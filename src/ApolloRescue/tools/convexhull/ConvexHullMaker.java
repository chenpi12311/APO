package ApolloRescue.tools.convexhull;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import ApolloRescue.tools.Ruler;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;
//import apollo.tools.geometry.Ruler;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;


public class ConvexHullMaker {
	private WorldInfo world;
	private ConvexHull convexHull;
	private AngleComparator angleComparator;
	private PriorityQueue<Building> priPoints;
//	private final static Log log = LogFactory.getLog(ConvexHullMaker.class);

	public ConvexHullMaker(WorldInfo world, List<Building> buildings) {
		// _______________________________-new
		long sTime = System.currentTimeMillis();

		this.world = world;
		// hulls.clear();
		convexHull = new ConvexHull();
		//清除重复ID 建筑
		Set<Building> temp = new HashSet<Building>(buildings);
		buildings.clear();
		buildings.addAll(temp);
		
		if (buildings != null && buildings.size() > 0) {
			if (buildings.size() <= 3) {
				convexHull.setApexBuildings(buildings);
				convexHull.setCenterPoint();
				return;
			} else {
				try {
					JarvisMarch<Building> jm = new JarvisMarch<Building>(
							buildings);
					List<Building> hull = jm.getHull();

					if (hull != null) {
						convexHull.setApexBuildings(hull);
						convexHull.setCenterPoint();
						// convexHull.setEdges(creatEdges(buildings));
						convexHull.setEdges(compositeEdges(convexHull));
					} else {
						String info = "";
						for (Building bb : buildings) {
							info += "\t" + bb.getID();
						}
					}
				} catch (Exception e) {
					// TODO: handle exception
					StringBuffer str = new StringBuffer();
					str.append("error: ");
					for(Building b : buildings) {
						str.append(" [B:"+b.getID()+" X:"+b.getX()+" Y:"+b.getY()+"]; ");
					}
//					log.debug(str);
					System.err.println(str);
					
				} 
				
			}

		}

		long dTime = System.currentTimeMillis() - sTime;
		if (dTime > 50) {
			// System.out.println("calHull cost : " + dTime + " ms");
		}
		

	}

	// TODO update buildings on hull
	public ConvexHull getConvexHull() {
		return convexHull;
	}

	public Building findBasicPoint(List<Building> buildings) {
		Building building = buildings.get(0);
		for (Building build : buildings) {
			if (build.getY() < building.getY()
					|| (build.getY() == building
							.getY() && build.getX() < building
							.getX())) {
				building = build;
			}

		}
		return building;
	}

	// 计算两个向量的叉积
	public int crossMultiply(Building p1, Building p2,
			Building p3) {
		int result;
		Point v1 = new Point(p2.getX() - p1.getX(),
				p2.getY() - p1.getY());
		Point v2 = new Point(p3.getX() - p2.getX(),
				p3.getY() - p2.getY());
		if (v1.equals(v2) && v1.x * v2.x > 0 && v1.y * v2.y > 0) {
			return -1;
		}
		result = v1.x * v2.y - v1.y * v2.x;
		return result;
	}

	public List<ConvexHullEdge> compositeEdges(ConvexHull hull) {
		if (hull != null) {
			List<ConvexHullEdge> edges = new ArrayList<ConvexHullEdge>();

			List<Building> apexBuildings = hull.getApexBuildings();
			int size = hull.getApexBuildings().size();

			// 1 composite edges
			for (int i = 0; i < size; i++) {
				Building start = apexBuildings.get(i);
				Building end = null;
				if (i == size - 1) {
					end = apexBuildings.get(0);
				} else {
					end = apexBuildings.get(i + 1);
				}
				ConvexHullEdge edge = new ConvexHullEdge(start, end);
				edge.setId(new EntityID(edges.size()));
				edges.add(edge);
			}

			// 2 get edge buildings
			if (edges != null) {
				for (ConvexHullEdge edge : edges) {
					int x = (edge.getStart().getX() + edge.getEnd().getX()) / 2;// edge中点x
					int y = (edge.getStart().getY() + edge.getEnd().getY()) / 2;// edge中点y
					int range = Ruler.getDistance(new Point(
							edge.getStart().getX(), edge.getStart().getY()),
							new Point(edge.getEnd().getX(), edge.getEnd().getY()));

					List<Building> history = new ArrayList<Building>();

					Collection<StandardEntity> entities = world
							.getObjectsInRange(x, y, range);// 某一edge周围的entity

					Iterator<StandardEntity> iter = entities.iterator();

					while (iter.hasNext()) {
						StandardEntity entity = iter.next();
						if (entity.getStandardURN().equals(
								StandardEntityURN.BUILDING)) {
							Building building = (Building) entity;
//							Building bb = new Building(building, world);

							if (apexBuildings.contains(building)
									|| history.contains(building)) {
								continue;
							}
							double min = 20000;// 20
							double distance = edge.getDistance(building);
							if (distance < min) {
								edge.getNearBuildings().add(building);
								history.add(building);
							}
						}
					}
				}
			}

			return edges;
		}

		return null;
	}

	// 创建凸包的边 old version
	public List<ConvexHullEdge> creatEdges(List<Building> buildings) {

		if (convexHull.getApexBuildings() != null && !buildings.isEmpty()) {

			List<ConvexHullEdge> edges = new ArrayList<ConvexHullEdge>();

			int size = convexHull.getApexBuildings().size();

			Building building = convexHull.getApexBuildings().get(0);
			convexHull.getApexBuildings().add(building);

			for (int i = 0; i < size; i++) {
				Building start = convexHull.getApexBuildings().get(i);
				Building end = null;
				if (i == size - 1) {
					end = convexHull.getApexBuildings().get(0);
				} else {
					end = convexHull.getApexBuildings().get(i + 1);
				}
				ConvexHullEdge edge = new ConvexHullEdge(start, end);
				edge.setId(new EntityID(edges.size()));
				edges.add(edge);
			}

			List<Building> history = new ArrayList<Building>();

			for (Building build : buildings) {
				if (convexHull.getApexBuildings().contains(build)
						|| history.contains(build)) {
					continue;
				}
				ConvexHullEdge nearestEdge = edges.get(0);
				double min = 20000;// 20
				for (ConvexHullEdge edge : edges) {

					// 如果此建筑到凸包边的距离小于十米时加进边中
					double distance = edge.getDistance(build);
					if (distance < min) {
						min = distance;
						nearestEdge = edge;
					}
				}
				nearestEdge.getNearBuildings().add(build);// 附近的建筑
				history.add(build);
			}
			convexHull.getApexBuildings().remove(building);
			convexHull.setAllEdgesBuildings(edges);

			return edges;
		}
		return null;
	}

	public String toString() {
		return "convexHull " + "(" + convexHull + " )";
	}

}
