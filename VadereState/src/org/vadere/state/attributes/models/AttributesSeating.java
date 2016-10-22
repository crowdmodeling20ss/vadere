package org.vadere.state.attributes.models;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.seating.SeatFacingDirection;
import org.vadere.state.attributes.models.seating.SeatRelativePosition;
import org.vadere.state.attributes.models.seating.SeatSide;
import org.vadere.state.attributes.models.seating.ValueWithProbabilityFraction;
import org.vadere.state.attributes.models.seating.model.SeatPosition;
import org.vadere.state.scenario.Et423Geometry;

/**
 * Parameters for the seating model.
 *
 */
public class AttributesSeating extends Attributes {

	/** The train geometry class name used to generate the scenario with Traingen. */
	private String trainGeometry = Et423Geometry.class.getName();
	
	/**
	 * Choices with probabilities for the seat group. <code>true</code> is
	 * choosing a seat group with the least number of other passengers.
	 */
	private List<ValueWithProbabilityFraction<Boolean>> seatGroupChoice;

	private List<ValueWithProbabilityFraction<SeatPosition>> seatChoice0;

	private List<ValueWithProbabilityFraction<SeatRelativePosition>> seatChoice1;

	private List<ValueWithProbabilityFraction<SeatSide>> seatChoice2Side;

	private List<ValueWithProbabilityFraction<SeatFacingDirection>> seatChoice2FacingDirection;
	
	{
		// initialize fields with values from data collection
		// TODO make this autogenerated
		
		seatGroupChoice = new ArrayList<>(2);
		addFraction(seatGroupChoice, true,  119);
		addFraction(seatGroupChoice, false, 23);
		
		seatChoice0 = new ArrayList<>(4);
		addFraction(seatChoice0, SeatPosition.WINDOW_BACKWARD, 5);
		addFraction(seatChoice0, SeatPosition.AISLE_BACKWARD, 1);
		addFraction(seatChoice0, SeatPosition.WINDOW_FORWARD, 25);
		addFraction(seatChoice0, SeatPosition.AISLE_FORWARD, 6);
		
		seatChoice1 = new ArrayList<>(3);
		addFraction(seatChoice1, SeatRelativePosition.DIAGONAL, 49);
		addFraction(seatChoice1, SeatRelativePosition.ACROSS, 14);
		addFraction(seatChoice1, SeatRelativePosition.NEXT, 5);
		
		seatChoice2Side = new ArrayList<>(2);
		addFraction(seatChoice2Side, SeatSide.AISLE, 4);
		addFraction(seatChoice2Side, SeatSide.WINDOW, 4);

		seatChoice2FacingDirection = new ArrayList<>(2);
		addFraction(seatChoice2FacingDirection, SeatFacingDirection.FORWARD, 7);
		addFraction(seatChoice2FacingDirection, SeatFacingDirection.BACKWARD, 6);

	}

	public String getTrainGeometry() {
		return trainGeometry;
	}

	public List<Pair<Boolean, Double>> getSeatGroupChoice() {
		return toPairListForEnumeratedDistribution(seatGroupChoice);
	}

	public List<Pair<SeatPosition, Double>> getSeatChoice0() {
		return toPairListForEnumeratedDistribution(seatChoice0);
	}

	public List<Pair<SeatRelativePosition, Double>> getSeatChoice1() {
		return toPairListForEnumeratedDistribution(seatChoice1);
	}

	public List<Pair<SeatSide, Double>> getSeatChoice2Side() {
		return toPairListForEnumeratedDistribution(seatChoice2Side);
	}

	public List<Pair<SeatFacingDirection, Double>> getSeatChoice2FacingDirection() {
		return toPairListForEnumeratedDistribution(seatChoice2FacingDirection);
	}
	
	public static <T> List<Pair<T, Double>> toPairListForEnumeratedDistribution(
			List<ValueWithProbabilityFraction<T>> list) {
		return list.stream().map(ValueWithProbabilityFraction::toPair).collect(Collectors.toList());
	}

	private <T> void addFraction(List<ValueWithProbabilityFraction<T>> list, T value, double fraction) {
		list.add(new ValueWithProbabilityFraction<>(value, fraction));
	}

}
