package org.vadere.meshing.mesh.triangulation;

import org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen.GenEikMesh;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.function.Function;

/**
 * The edge-length function used in {@link GenEikMesh},
 * {@link org.vadere.meshing.mesh.triangulation.improver.distmesh.Distmesh} and some
 * {@link ITriangulator} gives for every position
 * in the 2D Euclidean space the desired relative length of an edge of a mesh. Relative in the sense
 * that if the function is a constant equals to c edges will be of approximately the same length and
 * the actual length does not rely on c.
 *
 * @author Benedikt Zoennchen
 */
@FunctionalInterface
public interface IEdgeLengthFunction extends Function<IPoint,Double> {}
