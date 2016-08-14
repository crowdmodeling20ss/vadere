package org.vadere.simulator.projects.migration.incidents.specialized;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.vadere.simulator.models.potential.PotentialFieldObstacleCompact;
import org.vadere.simulator.models.potential.PotentialFieldObstacleOSM;
import org.vadere.simulator.models.potential.PotentialFieldPedestrianCompact;
import org.vadere.simulator.models.potential.PotentialFieldPedestrianOSM;
import org.vadere.simulator.projects.migration.Graph;
import org.vadere.simulator.projects.migration.MigrationException;
import org.vadere.simulator.projects.migration.incidents.Incident;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.models.AttributesPotentialCompact;
import org.vadere.state.attributes.models.AttributesPotentialOSM;

import static org.vadere.simulator.projects.migration.IncidentDatabase.path;

/**
 * For one particular case, not generalizable.
 */
public class AttributesPotentialCompactVSosmIncident extends Incident {

	@Override
	public boolean applies(Graph graph) {
		return true;
	}

	@Override
	public void resolve(Graph graph, StringBuilder log) throws MigrationException {

		Graph.Node attributesOSMnode = graph.getNodeByPath(path("vadere", "attributesModel", AttributesOSM.class.getName()));

		if (attributesOSMnode != null) {
			JsonNode attributesOSMjsonNode = attributesOSMnode.getJsonNode();

			Graph.Node attributesPotentialCompactNode = graph.getNodeByPath(path("vadere", "attributesModel", AttributesPotentialCompact.class.getName()));
			Graph.Node attributesPotentialOSMnode = graph.getNodeByPath(path("vadere", "attributesModel", AttributesPotentialOSM.class.getName()));

			if (attributesPotentialCompactNode != null && attributesPotentialOSMnode != null) {
				throw new MigrationException(this, "[AttributesPotentialCompact] and [AttributesPotentialOSM] are both present, that is not allowed.");
			}

			String beforeChange = attributesOSMjsonNode.toString();

			if (attributesPotentialCompactNode != null) {
				((ObjectNode) attributesOSMjsonNode).put("pedestrianPotentialModel", PotentialFieldPedestrianCompact.class.getName());
				((ObjectNode) attributesOSMjsonNode).put("obstaclePotentialModel", PotentialFieldObstacleCompact.class.getName());

				if (!beforeChange.equals(attributesOSMjsonNode.toString())) {
					log.append("\t- AttributesOSM: since AttributesPotentialCompact is present, set [pedestrianPotentialModel] to PotentialFieldPedestrianCompact " +
							"and [obstaclePotentialModel] to PotentialFieldObstacleCompact" + "\n");
				}
			}

			if (attributesPotentialOSMnode != null) {
				((ObjectNode) attributesOSMjsonNode).put("pedestrianPotentialModel", PotentialFieldPedestrianOSM.class.getName());
				((ObjectNode) attributesOSMjsonNode).put("obstaclePotentialModel", PotentialFieldObstacleOSM.class.getName());

				if (!beforeChange.equals(attributesOSMjsonNode.toString())) {
					log.append("\t- AttributesOSM: since AttributesPotentialOSM is present, set [pedestrianPotentialModel] to PotentialFieldPedestrianOSM " +
							"and [obstaclePotentialModel] to PotentialFieldObstacleOSM" + "\n");
				}
			}
		}
	}
}
