package ApolloRescue.tools.convexhull;

import java.awt.Point;
import java.util.Comparator;

import rescuecore2.standard.entities.Building;

public class AngleComparator implements Comparator<Building> {

	private Building basicBuilding;
	private static final int parameter = 10000;

	public AngleComparator(Building basicBuilding) {
		this.basicBuilding = basicBuilding;
	}

	@Override
	public int compare(Building p1, Building p2) {
		int result = 0;
		Point pp1 = new Point(p1.getX()
				- basicBuilding.getX(), p1.getY()
				- basicBuilding.getY());
		Point pp2 = new Point(p2.getX()
				- basicBuilding.getX(), p2.getY()
				- basicBuilding.getY());

		double m1 = Math.sqrt(pp1.x * pp1.x + pp1.y * pp1.y);
		double m2 = Math.sqrt(pp2.x * pp2.x + pp2.y * pp2.y);
		Double v1 = pp1.x / m1 * parameter;
		Double v2 = pp2.x / m2 * parameter;
		if (v1.equals(v2)) {
			result = (int) (m1 - m2);
			return result;
		}
		result = (int) (v2 - v1);
		return result;
	}

}
