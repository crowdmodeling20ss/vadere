package org.vadere.state.types;

public enum EikonalSolverType {

	/**
	 * solvers on a regular grid
	 */
	FAST_MARCHING,
	HIGH_ACCURACY_FAST_MARCHING,
	FAST_MARCHING_ADAPTIVE,
	HIGH_ACCURACY_FAST_MARCHING_ADAPTIVE,
	HIGH_ACCURACY_FAST_MARCHING_DYNAMIC,
	HIGH_ACCURACY_FAST_MARCHING_DENSITY,
	FAST_ITERATIVE_METHOD,
	FAST_SWEEPING_METHOD,

	/**
	 * Solvers on an triangle mesh
	 */
	FAST_MARCHING_TRI,

	/**
	 * No solver at all
	 */
	NONE;

	public boolean isHighAccuracy() {
		return  this == HIGH_ACCURACY_FAST_MARCHING ||
				this == HIGH_ACCURACY_FAST_MARCHING_ADAPTIVE ||
				this == HIGH_ACCURACY_FAST_MARCHING_DENSITY ||
				this == HIGH_ACCURACY_FAST_MARCHING_DYNAMIC;
	}

	public boolean isUsingCellGrid() {
		return  this == FAST_MARCHING ||
				this == HIGH_ACCURACY_FAST_MARCHING ||
				this == FAST_MARCHING_ADAPTIVE ||
				this == HIGH_ACCURACY_FAST_MARCHING_ADAPTIVE ||
				this == HIGH_ACCURACY_FAST_MARCHING_DYNAMIC ||
				this == HIGH_ACCURACY_FAST_MARCHING_DENSITY ||
				this == FAST_ITERATIVE_METHOD ||
				this == FAST_SWEEPING_METHOD;
	}
}
