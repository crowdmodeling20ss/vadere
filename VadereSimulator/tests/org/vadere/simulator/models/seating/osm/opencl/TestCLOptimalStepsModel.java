package org.vadere.simulator.models.seating.osm.opencl;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.osm.opencl.CLOptimalStepsModel;
import org.vadere.simulator.models.potential.fields.ObstacleDistancePotential;
import org.vadere.simulator.models.potential.fields.PotentialFieldSingleTargetGrid;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.models.AttributesPotentialCompact;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.state.util.TextOutOfNodeException;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.opencl.OpenCLException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Benedikt Zoennchen
 */
public class TestCLOptimalStepsModel {

	private String topographyStringChicken = "{\n" +
			"  \"attributes\" : {\n" +
			"    \"bounds\" : {\n" +
			"      \"x\" : 0.0,\n" +
			"      \"y\" : 0.0,\n" +
			"      \"width\" : 35.0,\n" +
			"      \"height\" : 60.0\n" +
			"    },\n" +
			"    \"boundingBoxWidth\" : 0.5,\n" +
			"    \"bounded\" : true\n" +
			"  },\n" +
			"  \"obstacles\" : [ {\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 9.0,\n" +
			"      \"y\" : 21.0,\n" +
			"      \"width\" : 1.0,\n" +
			"      \"height\" : 20.0,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 25.0,\n" +
			"      \"y\" : 21.0,\n" +
			"      \"width\" : 1.0,\n" +
			"      \"height\" : 20.0,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  }, {\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 10.0,\n" +
			"      \"y\" : 40.0,\n" +
			"      \"width\" : 15.0,\n" +
			"      \"height\" : 1.0,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"id\" : -1\n" +
			"  } ],\n" +
			"  \"stairs\" : [ ],\n" +
			"  \"targets\" : [ {\n" +
			"    \"id\" : 1,\n" +
			"    \"absorbing\" : true,\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 10.0,\n" +
			"      \"y\" : 51.0,\n" +
			"      \"width\" : 15.0,\n" +
			"      \"height\" : 5.0,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"waitingTime\" : 0.0,\n" +
			"    \"waitingTimeYellowPhase\" : 0.0,\n" +
			"    \"parallelWaiters\" : 0,\n" +
			"    \"individualWaiting\" : true,\n" +
			"    \"deletionDistance\" : 0.1,\n" +
			"    \"startingWithRedLight\" : false,\n" +
			"    \"nextSpeed\" : -1.0\n" +
			"  } ],\n" +
			"  \"sources\" : [ {\n" +
			"    \"id\" : -1,\n" +
			"    \"shape\" : {\n" +
			"      \"x\" : 10.0,\n" +
			"      \"y\" : 6.0,\n" +
			"      \"width\" : 15.0,\n" +
			"      \"height\" : 5.0,\n" +
			"      \"type\" : \"RECTANGLE\"\n" +
			"    },\n" +
			"    \"interSpawnTimeDistribution\" : \"org.vadere.state.scenario.ConstantDistribution\",\n" +
			"    \"distributionParameters\" : [ 1.0 ],\n" +
			"    \"spawnNumber\" : 300,\n" +
			"    \"maxSpawnNumberTotal\" : -1,\n" +
			"    \"startTime\" : 0.0,\n" +
			"    \"endTime\" : 0.0,\n" +
			"    \"spawnAtRandomPositions\" : true,\n" +
			"    \"useFreeSpaceOnly\" : false,\n" +
			"    \"targetIds\" : [ 1 ],\n" +
			"    \"groupSizeDistribution\" : [ 0.0, 0.0, 1.0 ],\n" +
			"    \"dynamicElementType\" : \"PEDESTRIAN\"\n" +
			"  } ],\n" +
			"  \"dynamicElements\" : [ ],\n" +
			"  \"attributesPedestrian\" : {\n" +
			"    \"radius\" : 0.195,\n" +
			"    \"densityDependentSpeed\" : false,\n" +
			"    \"speedDistributionMean\" : 1.34,\n" +
			"    \"speedDistributionStandardDeviation\" : 0.26,\n" +
			"    \"minimumSpeed\" : 0.5,\n" +
			"    \"maximumSpeed\" : 2.2,\n" +
			"    \"acceleration\" : 2.0\n" +
			"  },\n" +
			"  \"attributesCar\" : null\n" +
			"}";

	private AttributesFloorField attributesFloorField;
	private AttributesOSM attributesOSM;
	private List<CLOptimalStepsModel.PedestrianOpenCL> pedestrians;
	private Topography topography;
	private ObstacleDistancePotential obstacleDistancePotential;
	private PotentialFieldSingleTargetGrid targetPotentialField;
	private AttributesAgent attributesAgent;
	private AttributesPotentialCompact attributesPotentialCompact;
	private Random random;
	private int numberOfElements;
	private VRectangle bound;

	/*
	AttributesOSM attributesOSM,
				  AttributesAgent attributesPedestrian, Topography topography,
				  Random random, IPotentialFieldTarget potentialFieldTarget,
				  PotentialFieldObstacle potentialFieldObstacle,
				  PotentialFieldAgent potentialFieldPedestrian,
				  List<SpeedAdjuster> speedAdjusters,
				  StepCircleOptimizer stepCircleOptimizer
	 */
	@Before
	public void setUp() throws IOException, TextOutOfNodeException {
		random = new Random();
		numberOfElements = 256;
		attributesOSM = new AttributesOSM();
		attributesFloorField = new AttributesFloorField();
		attributesAgent = new AttributesAgent();
		attributesPotentialCompact = new AttributesPotentialCompact();
		topography = StateJsonConverter.deserializeTopography(topographyStringChicken);
		bound = new VRectangle(topography.getBounds());
		obstacleDistancePotential = new ObstacleDistancePotential(
				topography.getObstacles().stream().map(obs -> obs.getShape()).collect(Collectors.toList()),
				bound, attributesFloorField);
		targetPotentialField = new PotentialFieldSingleTargetGrid(topography, attributesAgent, attributesFloorField, 1);
		targetPotentialField.preLoop(0.4f);
		pedestrians = new ArrayList<>();

		for(int i = 0; i < numberOfElements; i++) {
			VPoint randomPosition = new VPoint(
					(float)(bound.getMinX() + random.nextDouble() * bound.getWidth()),
					(float)(bound.getMinY() + random.nextDouble() * bound.getHeight()));
			CLOptimalStepsModel.PedestrianOpenCL pedestrian = new CLOptimalStepsModel.PedestrianOpenCL(randomPosition, 1.9f);
			pedestrians.add(pedestrian);
		}
	}


	@Test
	public void testIdentity() throws OpenCLException {
		CLOptimalStepsModel clOptimalStepsModel = new CLOptimalStepsModel(
				pedestrians.size(),
				new VRectangle(topography.getBounds()),
				1.5f + attributesPotentialCompact.getPedPotentialWidth(), // max step length + function width
				attributesFloorField,
				targetPotentialField.getEikonalSolver(),
				obstacleDistancePotential.getEikonalSolver());

		List<CLOptimalStepsModel.PedestrianOpenCL> result = clOptimalStepsModel.getNextSteps(pedestrians);

		for(int i = 0; i < numberOfElements; i++) {
			assertEquals("not equals for index = " + i, result.get(i).position, result.get(i).newPosition);
		}
	}
}
