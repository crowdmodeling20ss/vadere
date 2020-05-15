package org.vadere.simulator.models.groups.sir;


import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.groups.AbstractGroupModel;
import org.vadere.simulator.models.groups.Group;
import org.vadere.simulator.models.groups.GroupSizeDeterminator;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesSIRG;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.DynamicElementContainer;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Implementation of groups for a susceptible / infected / removed (SIR) model.
 */
@ModelClass
public class SIRGroupModel extends AbstractGroupModel<SIRGroup> {

    private Random random;
    private LinkedHashMap<Integer, SIRGroup> groupsById;
    private Map<Integer, LinkedList<SIRGroup>> sourceNextGroups;
    private AttributesSIRG attributesSIRG;
    private Topography topography;
    private IPotentialFieldTarget potentialFieldTarget;
    private int totalInfected = 0;
    private double totalTimeInSec = 0;
    public static int time = 0;

    public SIRGroupModel() {
        this.groupsById = new LinkedHashMap<>();
        this.sourceNextGroups = new HashMap<>();
    }

    @Override
    public void initialize(List<Attributes> attributesList, Domain domain,
                           AttributesAgent attributesPedestrian, Random random) {
        this.attributesSIRG = Model.findAttributes(attributesList, AttributesSIRG.class);
        this.topography = domain.getTopography();
        this.random = random;
        this.totalInfected = 0;
    }

    @Override
    public void setPotentialFieldTarget(IPotentialFieldTarget potentialFieldTarget) {
        this.potentialFieldTarget = potentialFieldTarget;
        // update all existing groups
        for (SIRGroup group : groupsById.values()) {
            group.setPotentialFieldTarget(potentialFieldTarget);
        }
    }

    @Override
    public IPotentialFieldTarget getPotentialFieldTarget() {
        return potentialFieldTarget;
    }

    private int getFreeGroupId() {
        if (this.random.nextDouble() < this.attributesSIRG.getInfectionRate()
                || this.totalInfected < this.attributesSIRG.getInfectionsAtStart()) {
            if (!getGroupsById().containsKey(SIRType.ID_INFECTED.ordinal())) {
                SIRGroup g = getNewGroup(SIRType.ID_INFECTED.ordinal(), Integer.MAX_VALUE / 2);
                getGroupsById().put(SIRType.ID_INFECTED.ordinal(), g);
            }
            this.totalInfected += 1;
            return SIRType.ID_INFECTED.ordinal();
        } else {
            if (!getGroupsById().containsKey(SIRType.ID_SUSCEPTIBLE.ordinal())) {
                SIRGroup g = getNewGroup(SIRType.ID_SUSCEPTIBLE.ordinal(), Integer.MAX_VALUE / 2);
                getGroupsById().put(SIRType.ID_SUSCEPTIBLE.ordinal(), g);
            }
            return SIRType.ID_SUSCEPTIBLE.ordinal();
        }
    }


    @Override
    public void registerGroupSizeDeterminator(int sourceId, GroupSizeDeterminator gsD) {
        sourceNextGroups.put(sourceId, new LinkedList<>());
    }

    @Override
    public int nextGroupForSource(int sourceId) {
        return Integer.MAX_VALUE / 2;
    }

    @Override
    public SIRGroup getGroup(final Pedestrian pedestrian) {
        SIRGroup group = groupsById.get(pedestrian.getGroupIds().getFirst());
        assert group != null : "No group found for pedestrian";
        return group;
    }

    @Override
    protected void registerMember(final Pedestrian ped, final SIRGroup group) {
        groupsById.putIfAbsent(ped.getGroupIds().getFirst(), group);
    }

    @Override
    public Map<Integer, SIRGroup> getGroupsById() {
        return groupsById;
    }

    @Override
    protected SIRGroup getNewGroup(final int size) {
        return getNewGroup(getFreeGroupId(), size);
    }

    @Override
    protected SIRGroup getNewGroup(final int id, final int size) {
        if (groupsById.containsKey(id)) {
            return groupsById.get(id);
        } else {
            return new SIRGroup(id, this);
        }
    }

    private void initializeGroupsOfInitialPedestrians() {
        // get all pedestrians already in topography
        DynamicElementContainer<Pedestrian> c = topography.getPedestrianDynamicElements();

        if (c.getElements().size() > 0) {
            // TODO: fill in code to assign pedestrians in the scenario at the beginning (i.e., not created by a source)
            //  to INFECTED or SUSCEPTIBLE groups.
            for (Pedestrian p : c.getElements()) {
                if (this.random.nextDouble() < attributesSIRG.getInfectionRate()) {
                    infectPedestrian(p);
                }
            }
        }
    }

    protected void assignToGroup(Pedestrian ped, int groupId) {
        SIRGroup currentGroup = getNewGroup(groupId, Integer.MAX_VALUE / 2);
        currentGroup.addMember(ped);
        ped.getGroupIds().clear();
        ped.getGroupSizes().clear();
        ped.addGroupId(currentGroup.getID(), currentGroup.getSize());
        registerMember(ped, currentGroup);
    }

    protected void assignToGroup(Pedestrian ped) {
        int groupId = getFreeGroupId();
        assignToGroup(ped, groupId);
    }


    /* DynamicElement Listeners */

    @Override
    public void elementAdded(Pedestrian pedestrian) {
        assignToGroup(pedestrian);
    }

    @Override
    public void elementRemoved(Pedestrian pedestrian) {
        Group group = groupsById.get(pedestrian.getGroupIds().getFirst());
        if (group.removeMember(pedestrian)) { // if true pedestrian was last member.
            groupsById.remove(group.getID());
        }
    }

    /* Model Interface */

    @Override
    public void preLoop(final double simTimeInSec) {
        initializeGroupsOfInitialPedestrians();
        topography.addElementAddedListener(Pedestrian.class, this);
        topography.addElementRemovedListener(Pedestrian.class, this);
    }

    @Override
    public void postLoop(final double simTimeInSec) {
    }

    @Override
    public void update(final double simTimeInSec) {
        // check the positions of all pedestrians and switch groups to INFECTED (or REMOVED).
        DynamicElementContainer<Pedestrian> c = topography.getPedestrianDynamicElements();
        //System.out.println("time: " + ++time + " simTimeInSec:" + simTimeInSec + " infected peds:" + c.getElements().stream().filter(pedestrian -> getGroup(pedestrian).getID() == SIRType.ID_INFECTED.ordinal()).count());

        /**
         *  The fixed time step 1-sec is used for infecting the pedestrians
         *  to decouple the infection rate and the time step,
         *
         *  This 'update' method is called depending on the simTimeStepLength.
         *  Let's say we have a simulation with finishTime=20.
         *  If simTimeStepLength is given 0.5, the update function is called 40 times
         *  If simTimeStepLength is given 0.1, the update function is called 200 times
         *
         *  Because of that for the same simulation time infection rate differs.
         *
         * !!! Attention !!!
         *  TODO: If the simulation time less than 1, nobody infected!
         */
        if (simTimeInSec - totalTimeInSec < 1) {
            return;
        }

        totalTimeInSec++;
        if (c.getElements().size() > 0) {
            for (Pedestrian p : c.getElements()) {
                List<DynamicElement> neighbours = getDynElementsAtPosition(topography, p.getPosition(), attributesSIRG.getInfectionMaxDistance());

                List<Pedestrian> infectedNeighbours = neighbours
                        .parallelStream()
                        .map(dynamicElement -> (Pedestrian) dynamicElement)
                        .filter(p_neighbor -> p != p_neighbor && getGroup(p_neighbor).getID() == SIRType.ID_INFECTED.ordinal())
                        // we don't need line below. It should be checked already by getDynElementsAtPosition()
                        //.filter(p_neighbor -> p.getPosition().distance(p_neighbor.getPosition()) < attributesSIRG.getInfectionMaxDistance())
                        .collect(Collectors.toList());

                for (Pedestrian ignored : infectedNeighbours) {
                    if (this.random.nextDouble() < attributesSIRG.getInfectionRate()) {
                        infectPedestrian(p);
                        break;
                    }
                }
            }
        }
    }

    private void infectPedestrian(Pedestrian p) {
        SIRGroup g = getGroup(p);
        if (g.getID() == SIRType.ID_SUSCEPTIBLE.ordinal()) {
            elementRemoved(p);
            assignToGroup(p, SIRType.ID_INFECTED.ordinal());
        }
    }

    private List<DynamicElement> getDynElementsAtPosition(final Topography topography, VPoint sourcePosition, double radius) {
        LinkedCellsGrid<DynamicElement> dynElements = topography.getSpatialMap(DynamicElement.class);
        return dynElements.getObjects(sourcePosition, radius);
    }
}
