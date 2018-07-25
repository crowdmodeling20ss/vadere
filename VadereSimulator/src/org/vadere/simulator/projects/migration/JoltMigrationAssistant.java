package org.vadere.simulator.projects.migration;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.Diffy;
import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;

import org.apache.log4j.Logger;
import org.vadere.simulator.entrypoints.Version;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.LogBufferAppender;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

import static org.vadere.util.io.IOUtils.LEGACY_DIR;
import static org.vadere.util.io.IOUtils.OUTPUT_DIR;
import static org.vadere.util.io.IOUtils.SCENARIO_DIR;


public class JoltMigrationAssistant extends MigrationAssistant {

	private final static Logger logger = Logger.getLogger(JoltMigrationAssistant.class);
	private final static LogBufferAppender appender;

	private static HashMap<Version, Chainr> identityTransformation = new LinkedHashMap<>();
	private static HashMap<Version, Chainr> transformations = new LinkedHashMap<>();

	static {
		identityTransformation.put(Version.V0_1, Chainr.fromSpec(JsonUtils.classpathToList("/identity_v1.json")));
		identityTransformation.put(Version.V0_2, Chainr.fromSpec(JsonUtils.classpathToList("/identity_v2.json")));

		transformations.put(Version.V0_2, Chainr.fromSpec(JsonUtils.classpathToList("/transform_v1_to_v2.json")));
		appender = new LogBufferAppender();
	}

	private Diffy transformationDiff;

	public JoltMigrationAssistant(MigrationOptions options) {
		super(options);
		this.transformationDiff = new Diffy();
		logger.addAppender(appender);
	}

	public JoltMigrationAssistant() {
		this(MigrationOptions.defaultOptions());
	}

	@Override
	public String getLog() {
		return appender.getMigrationLog();
	}


	@Override
	public MigrationResult analyzeProject(String projectFolderPath) throws IOException {
		MigrationResult stats = new MigrationResult();

		Path scenarioDir = Paths.get(projectFolderPath, SCENARIO_DIR);
		if (Files.exists(scenarioDir)) {
			stats = analyzeDirectory(scenarioDir, SCENARIO_DIR);
		}

		Path outputDir = Paths.get(projectFolderPath, OUTPUT_DIR);
		if (Files.exists(outputDir)) {
			MigrationResult outputDirStats = analyzeDirectory(outputDir, OUTPUT_DIR);
			stats.add(outputDirStats);
		}
		return stats;
	}


	public MigrationResult analyzeDirectory(Path dir, String dirName) throws IOException {

		Path legacyDir = dir.getParent().resolve(LEGACY_DIR).resolve(dirName);

		File[] scenarioFiles = dirName.equals(SCENARIO_DIR) ? IOUtils.getFilesInScenarioDirectory(dir) : IOUtils.getScenarioFilesInOutputDirectory(dir);
		MigrationResult stats = new MigrationResult(scenarioFiles.length);

		for (File file : scenarioFiles) {

			if (dirName.equals(OUTPUT_DIR)) {
				String fileFolder = Paths.get(file.getParent()).getFileName().toString(); // the folder in which the .scenario and the .trajectories file lies
				legacyDir = dir.getParent().resolve(LEGACY_DIR).resolve(OUTPUT_DIR).resolve(fileFolder);
			}

			Path scenarioFilePath = Paths.get(file.getAbsolutePath());
			try {
				if (analyzeScenario(scenarioFilePath, legacyDir, dirName)) {
					stats.legacy++;
				} else {
					stats.upToDate++;
				}
			} catch (MigrationException e) {
				moveFileAddExtension(scenarioFilePath, legacyDir, migrationOptions.getNonmigratabelExtension(), !dirName.equals(SCENARIO_DIR));
				logger.error("!> Can't migrate the scenario to latest version, removed it from the directory (" +
						e.getMessage() + ") If you can fix this problem manually, do so and then remove ." +
						migrationOptions.getNonmigratabelExtension() + " from the file in the " + LEGACY_DIR + "-directory "
						+ "and move it back into the scenarios-directory, it will be checked again when the GUI restarts.");
				stats.notmigratable++;
			}
		}
		return stats;
	}

	public JsonNode transform(JsonNode currentJson, Version targetVersion) throws MigrationException {
		return transform(StateJsonConverter.convertJsonNodeToObject(currentJson), targetVersion);
	}

	private JsonNode transform(Object currentJson, Version targetVersion) throws MigrationException {
		Chainr t = transformations.get(targetVersion);
		if (t == null) {
			logger.error("No Transformation defined for Version " + targetVersion.toString());
			throw new MigrationException("No Transformation defined for Version " + targetVersion.toString());
		}

		Object scenarioTransformed = t.transform(currentJson);

		Chainr identity = identityTransformation.get(targetVersion);
		if (identity == null) {
			logger.error("No IdentityTransformation defined for Version " + targetVersion.toString());
			throw new MigrationException("No IdentityTransformation definde for Version " + targetVersion.toString());
		}

		Object scenarioTransformedTest = identity.transform(scenarioTransformed);
		Diffy.Result diffResult = transformationDiff.diff(scenarioTransformed, scenarioTransformedTest);
		if (diffResult.isEmpty()) {
			return StateJsonConverter.deserializeToNode(scenarioTransformed);
		} else {
			logger.error("Error in Transformation " + diffResult.toString());
			throw new MigrationException("Error in Transformation " + diffResult.toString());
		}
	}

	private boolean analyzeScenario(Path scenarioFilePath, Path legacyDir, String dirName)
			throws IOException, MigrationException {
		String json = IOUtils.readTextFile(scenarioFilePath);
		JsonNode node = StateJsonConverter.deserializeToNode(json);

		String parentPath = dirName.equals(SCENARIO_DIR) ? SCENARIO_DIR + "/" : OUTPUT_DIR + "/" + scenarioFilePath.getParent().getFileName().toString() + "/";

		logger.info(">> analyzing JSON tree of scenario <" + parentPath + node.get("name").asText() + ">");

		Version version = Version.UNDEFINED;

		if (node.get("release") != null) {
			version = Version.fromString(node.get("release").asText());

			if (version == null || version.equalOrSamller(Version.UNDEFINED)) {
				logger.error("release version " + node.get("release").asText() + " is unknown or not " +
						"supported. If this is a valid release create a version transformation and a new idenity transformation");
				throw new MigrationException("release version " + node.get("release").asText() + " is unknown or not " +
						"supported. If this is a valid releasecreate a version transformation and a new idenity transformation");
			}

			// if enforced migration should be done from baseVersion to latestVersion
			if (migrationOptions.isReapplyLatestMigrationFlag() && migrationOptions.getBaseVersion() != null) {
				version = migrationOptions.getBaseVersion();

			} else if (migrationOptions.isReapplyLatestMigrationFlag()) { // if enforced migration should be done from prev version to latest
				Optional<Version> optVersion = Version.getPrevious(version);
				if (optVersion.isPresent()) {
					version = optVersion.get();
				} else {
					return false;
				}
			} // if no enforced migration should be done and we are at the latest version, no migration is required.
			else if (version == Version.latest()) {
				return false;
			}
		} else {
			logger.error("Version is unknown");
			throw new MigrationException("Version is unknown");
		}

		JsonNode transformedNode = node;
		// apply all transformation from current to latest version.
		for (Version v : Version.listToLatest(version)) {
			transformedNode = transform(transformedNode, v);
		}
		if (legacyDir != null) {
			logger.info("Scenario Migrated. Move olde version to legacyDir");
			moveFileAddExtension(scenarioFilePath, legacyDir, migrationOptions.getLegacyExtension(), false);
		}
		IOUtils.writeTextFile(scenarioFilePath.toString(), StateJsonConverter.serializeJsonNode(transformedNode));
		return true;
	}

	private void moveFileAddExtension(Path scenarioFilePath, Path legacyDir, String additionalExtension, boolean moveOutputFolder)
			throws IOException {
		Path source = scenarioFilePath;
		Path target = legacyDir.resolve(source.getFileName() + "." + additionalExtension);

		if (moveOutputFolder) {
			source = source.getParent();
			target = Paths.get(legacyDir.toAbsolutePath() + "." + additionalExtension);
		}

		IOUtils.createDirectoryIfNotExisting(target);
		Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); // ensure potential existing files aren't overwritten?
	}
}
