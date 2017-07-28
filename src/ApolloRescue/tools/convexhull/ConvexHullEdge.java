package ApolloRescue.tools.convexhull;

import java.util.ArrayList;
import java.util.List;

import ApolloRescue.tools.Ruler;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.worldmodel.EntityID;
//import apollo.tools.geometry.Ruler;
//import apollo.world.entities.Building;
//import apollo.world.entities.BurningBuilding;

public class ConvexHullEdge {

	private EntityID id;
	private Building start;
	private Building end;
	private List<Building> nearBuildings;
	private static final double value = 8000;

	ConvexHullEdge(Building start, Building end) {
		this.start = start;
		this.end = end;
		nearBuildings = new ArrayList<Building>();
	}

	public Building getStart() {
		return start;
	}

	public void setStart(Building start) {
		this.start = start;
	}

	public Building getEnd() {
		return end;
	}

	public void setEnd(Building end) {
		this.end = end;
	}

	public List<Building> getNearBuildings() {
		return nearBuildings;
	}

	public void setNearBuildings(List<Building> buildings) {
		this.nearBuildings = buildings;

	}

	public Boolean IsNearToMe(Building building) {
		if (start != null && end != null && building != null) {
			double k = (end.getY() - start.getY())
					/ (end.getX() - start.getX());
			double distance = (Math.abs(k * building.getX()
					- building.getY() - k
					* start.getX() + start.getY()))
					/ (Math.sqrt(1 + k * k));
			if (distance < value) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	public double getDistance(Building building) {
		if (start != null && end != null && building != null) {
			if (start.equals(end)) {
				return value * 10;
			}
			Point2D p1 = new Point2D(start.getX(), start
					.getY());
			Point2D p2 = new Point2D(end.getX(), end
					.getY());
			Point2D center = new Point2D(building.getX(),
					building.getY());
			Edge edge = new Edge(p1, p2);
			double distance = Ruler.getDistance(center, edge);
			// double
			// k=(end.getBuilding().getY()-start.getBuilding().getY())/(end.getBuilding().getX()-start.getBuilding().getX());
			// double
			// distance=(Math.abs(k*building.getBuilding().getX()-building.getBuilding().getY()-k*start.getBuilding().getX()+start.getBuilding().getY()))/(Math.sqrt(1+k*k));
			return distance;
		}
		return value * 10;
	}

	public EntityID getId() {
		return id;
	}

	public void setId(EntityID id) {
		this.id = id;
	}

	public String toString() {
		return "( " + start + " , " + end + " ) " + " ( " + nearBuildings
				+ " ) ";
	}

}
