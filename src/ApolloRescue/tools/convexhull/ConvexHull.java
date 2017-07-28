package ApolloRescue.tools.convexhull;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.Area;
//import apollo.partition.tools.MathUtils;
//import apollo.world.entities.Building;
import rescuecore2.standard.entities.Building;

public class ConvexHull {
	private EntityID ID;
	private List<Building> apexBuildings;
	private Point centerPoint;
	private Building centerBuilding;
	private int burningBuildingNum;
	private int area;
	private int groundArea; // mended
	private List<ConvexHullEdge> edges;
	private List<Building> allEdgeBuildings;
	private List<Building> allBuildingsOnHull;
	private double radius; // 凸包半径
	private static final int MaxValue = 100000000;
	private Polygon polygon = null;

	public ConvexHull() {
		apexBuildings = new ArrayList<Building>();
		edges = new ArrayList<ConvexHullEdge>();
		allEdgeBuildings = new ArrayList<Building>();
	}

	public void setBuildingsOnHull(List<Building> buildings) {
		this.allBuildingsOnHull = buildings;
	}

	public EntityID getId() {
		return this.ID;
	}

	public void setId(EntityID id) {
		this.ID = id;
	}

	public Point getCenterPoint() {
		return this.centerPoint;
	}

	public void setCenterPoint(Point point) {
		this.centerPoint = point;
	}

	public void setCenterPoint() {
		int maxX = 0, maxY = 0, minX = MaxValue, minY = MaxValue;
		if (!apexBuildings.isEmpty()) {
			Building p1 = null, p2 = null, p3 = null, p4 = null;
			for (Building building : apexBuildings) {
				if (maxX < building.getX()) { // p1X最大的建筑
					maxX = building.getX();
					p1 = building;
				}
				if (maxY < building.getY()) { // p2Y最大的建筑
					maxY = building.getY();
					p2 = building;
				}
				if (minX > building.getX()) { // p3X最大小的建筑
					minX = building.getX();
					p3 = building;
				}
				if (minY > building.getY()) { // p4Y最小的建筑
					minY = building.getY();
					p4 = building;
				}
			}
			if (p1 != null && p2 != null && p3 != null && p4 != null) {
				int x = (p1.getX()
						+ p2.getX()
						+ p3.getX() + p4
						.getX()) / 4;
				int y = (p1.getY()
						+ p2.getY()
						+ p3.getY() + p4
						.getY()) / 4;
				centerPoint = new Point(x, y);
				List<Building> buildings = new ArrayList<Building>();
				buildings.add(p1);
				buildings.add(p2);
				buildings.add(p3);
				buildings.add(p4);
				setRadius(buildings);
			}

		}
		// 计算中心建筑
		calcCenter();
	}

	private void calcCenter() {
		double x = 0, y = 0;
		for (Building a : apexBuildings) {
			x += a.getX();
			y += a.getY();
		}
		if (this.apexBuildings.size() != 0) {
			x = x / apexBuildings.size();
			y = y / apexBuildings.size();
		}

		int ix = (int) x;
		int iy = (int) y;
		double mindist = Double.MAX_VALUE;
		for (Building a : apexBuildings) {
			double temp = manhattanDistance(a, ix,
					iy);
			if (temp < mindist) {
				mindist = temp;
				centerBuilding = a;
			}
		}
	}
	
	public static double manhattanDistance(Area a, double x2, double y2) {
		return Math.abs(a.getX() - x2) + Math.abs(a.getY() - y2);
	}

	/**
	 * 火区中心建筑，由凸包边上建筑计算
	 * 
	 * @return
	 */
	public Building getCenter() {
		if (centerBuilding == null) {
			calcCenter();
		}
		return centerBuilding;
	}

	public void setRadius(List<Building> burningBuildings) {
		if (burningBuildings != null && !burningBuildings.isEmpty()) {
			double radius = 0;
			for (Building building : burningBuildings) {
				double buildingWidth = getBuildingWidth(building);
				double distance = getDistance(centerPoint.getX(),
						centerPoint.getY(), building.getX(),
						building.getY());
				radius += distance + buildingWidth / 2;
			}

			this.radius = radius / burningBuildings.size();
		}

	}

	protected int getDistance(double x1, double y1, double x2, double y2) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		return (int) Math.hypot(dx, dy);
	}

	public double getBuildingWidth(Building burningBuilding) {

		double w_1 = burningBuilding.getShape().getBounds()
				.getHeight();
		double w_2 = burningBuilding.getShape().getBounds()
				.getWidth();
		double width = ((w_1 + w_2) / 2);
		return width;
	}

	public List<Building> getApexBuildings() {
		return apexBuildings;
	}

	public void setApexBuildings(List<Building> buildings) {
		this.apexBuildings = buildings;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ID == null) ? 0 : ID.hashCode());
		result = prime * result
				+ ((apexBuildings == null) ? 0 : apexBuildings.hashCode());
		result = prime * result
				+ ((centerPoint == null) ? 0 : centerPoint.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConvexHull other = (ConvexHull) obj;
		if (ID == null) {
			if (other.ID != null)
				return false;
		} else if (!ID.equals(other.ID))
			return false;
		if (apexBuildings == null) {
			if (other.apexBuildings != null)
				return false;
		} else if (!apexBuildings.equals(other.apexBuildings))
			return false;
		if (centerPoint == null) {
			if (other.centerPoint != null)
				return false;
		} else if (!centerPoint.equals(other.centerPoint))
			return false;
		return true;
	}

	public double getArea() {
		return area;
	}

	public void setArea(int area) {
		this.area = area;
	}

	public double getGroundArea() { // mended
		return groundArea;
	}

	public void setGroundArea(int area) { // mended
		this.groundArea = area;
	}

	public int getBurningBuildingNum() {
		return burningBuildingNum;
	}

	/**
	 * for Viewer
	 * 
	 * @return Point
	 */
	public List<Point> getBurningBuildingPoint() {
		List<Point> result = new ArrayList<Point>();
		for (Building bb : apexBuildings) {
			if (bb != null && bb != null) {
				result.add(new Point(bb.getX(), bb
						.getY()));
			}
		}
		return result;
	}

	public void setBurningBuildingNum(int burningBuildingNum) {
		this.burningBuildingNum = burningBuildingNum;
	}

	public List<ConvexHullEdge> getEdges() {
		return edges;
	}

	public void setEdges(List<ConvexHullEdge> edges) {
		this.edges = edges;
	}

	public void setAllEdgesBuildings(List<ConvexHullEdge> edges) {
		if (!edges.isEmpty()) {
			for (ConvexHullEdge edge : edges) {
				if (edge == null) {
					continue;
				}
				for (Building build : edge.getNearBuildings()) {
					if (!this.allEdgeBuildings.contains(build)) {
						this.allEdgeBuildings.add(build);
					}
				}
			}
		}
	}

	/**
	 * @author dyg XXX
	 * @return
	 */
	public List<Building> getAllNearBuildings() {
		List<Building> buildings = new ArrayList<Building>();
		if (!this.getEdges().isEmpty()) {
			for (ConvexHullEdge edge : this.getEdges()) {
				buildings.addAll(edge.getNearBuildings());
			}
		}
		return buildings;
	}
	
	public List<Building> getAllEdgeBuildings() {
		return this.allEdgeBuildings;

	}

	public void clear() {
		this.allEdgeBuildings.clear();
		this.apexBuildings.clear();
		this.edges.clear();
	}

	public List<Building> getHullBuildings() {
		List<Building> hullBuildings = new ArrayList<Building>();
		hullBuildings.addAll(apexBuildings);
		hullBuildings.addAll(allEdgeBuildings);
		return hullBuildings;
	}

	public List<Building> getHullBuildingsNew() {
		this.allBuildingsOnHull.addAll(apexBuildings);
		this.allBuildingsOnHull.addAll(allEdgeBuildings);
		return this.allBuildingsOnHull;
	}

	public boolean isSmallHull() {
		if (!isEmpty()) {
			if (this.getHullBuildings().size() != 0
					&& this.getHullBuildings().size() < 3) {
				return true;
			}
			return false;
		}
		return false;
	}

	public boolean isEmpty() {
		if (this.allEdgeBuildings.size() == 0 && this.apexBuildings.size() == 0) {
			return true;
		}
		return false;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public String toString() {
		return "convexHull " + "(" + "ID " + ID + " buildings " + apexBuildings
				+ " )";
	}

	/**
	 * 凸包的几何图形 由Apexs构成
	 * 
	 * @return
	 */
	public Polygon getPolygon() {
		setPolygon();
		return polygon;
	}

	public void setPolygon() {
		Polygon p = new Polygon();
		for (Building apex : getApexBuildings()) {
			p.addPoint(apex.getX(), apex.getY());
		}
		this.polygon = p;
	}

	/**
	 * 缩放凸包
	 * 
	 * @param convex
	 * @param scale
	 * @return
	 */
	public Polygon getScaleConvexHull(Polygon convex, float scale) {
		Polygon p = new Polygon();
		int size = convex.npoints;
		int[][] convexArray = new int[2][size];
		int[] xpoints = new int[size];
		int[] ypoints = new int[size];
		for (int i = 0; i < size; i++) {
			xpoints[i] = convex.xpoints[i];
			ypoints[i] = convex.ypoints[i];
		}
		convexArray[0] = xpoints;
		convexArray[1] = ypoints;
		scaleConvex(convexArray, scale);
		p.xpoints = convexArray[0];
		p.ypoints = convexArray[1];
		p.npoints = size;
		return p;
	}

	/**
	 * 对凸包进行缩放
	 * 
	 * @param convex
	 * @param scale
	 */
	public static void scaleConvex(int[][] convex, float scale) {
		int size = convex[0].length;
		double Cx, Cy;
		Cx = Cy = 0d;
		for (int i = 0; i < size; i++) {
			Cx += convex[0][i];
			Cy += convex[1][i];
		}
		Cx /= size;
		Cy /= size;

		for (int i = 0; i < size; i++) {
			convex[0][i] = (int) ((convex[0][i] - Cx) * scale + Cx);
			convex[1][i] = (int) ((convex[1][i] - Cy) * scale + Cy);
		}
	}
	
	//--------------------------------------------------------------------------------
	// TODO for direction 
    private Polygon triangle;
    public Point CENTER_POINT;
    public java.awt.Point FIRST_POINT;
    public java.awt.Point SECOND_POINT;
    public java.awt.Point CONVEX_POINT;
    //-------------
    public java.awt.Point OTHER_POINT1;
    public java.awt.Point OTHER_POINT2;
    public Set<Point2D> CONVEX_INTERSECT_POINTS;
    public Set<Line2D> CONVEX_INTERSECT_LINES;
    public Polygon DIRECTION_POLYGON;

    public void setTrianglePolygon(Polygon shape) {
        int xs[] = new int[shape.npoints];
        int ys[] = new int[shape.npoints];
        for (int i = 0; i < shape.npoints; i++) {
            xs[i] = shape.xpoints[i];
            ys[i] = shape.ypoints[i];
        }
        triangle = new Polygon(xs, ys, shape.npoints);
    }

    public Polygon getTriangle() {
        return triangle;
    }
	

}
