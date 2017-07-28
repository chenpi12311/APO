package ApolloRescue.tools.convexhull;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;

//import apollo.tools.geometry.Planarizable;

/**
 * creat convex hull by Javis March
 */
public class JarvisMarch<T extends Area>{

	private List<T> points;
	private List<T> hull;
	private static int MAX_ANGLE = 4;   
	private double currentMinAngle = 0;

	public JarvisMarch(List<T> points) throws Exception  {
		this.points = points;
		this.hull = new ArrayList<T>();

		this.calculate();
	}
	
	private void calculate() throws Exception  {
		int firstIndex = getFirstPointIndex(this.points);
		this.hull.clear();
		this.hull.add(this.points.get(firstIndex));//向list(hull)中添加第一个点
		currentMinAngle = 0;
		for (int i = nextIndex(firstIndex, this.points); i != firstIndex; i = nextIndex(
				i, this.points)) {
			this.hull.add(this.points.get(i));
		}//向list(hull)中添加其他的点，这些点将构成一个convex hull
	}

	public void remove(T item) throws Exception  {

		if (!hull.contains(item)) {
			points.remove(item);
			return;
		}
		points.remove(item);
		// TODO
		calculate();
	}
	
	public void remove(List<T> items) throws Exception{
		points.removeAll(items);
		calculate();
	}
	

	public void add(T item) throws Exception {
		points.add(item);

		List<T> tmplist = new ArrayList<T>();

		tmplist.addAll(hull);
		tmplist.add(item);

		List<T> tmphull = new ArrayList<T>();
		int firstIndex = getFirstPointIndex(tmplist);
		tmphull.add(tmplist.get(firstIndex));
		currentMinAngle = 0;
		for (int i = nextIndex(firstIndex, tmplist); i != firstIndex; i = nextIndex(
				i, tmplist)) {
			tmphull.add(tmplist.get(i));
		}

		this.hull = tmphull;
	}

	public void add(List<T> items) throws Exception {
		points.addAll(items);
		List<T> tmplist = new ArrayList<T>();

		tmplist.addAll(hull);
		tmplist.addAll(items);

		List<T> tmphull = new ArrayList<T>();
		int firstIndex = getFirstPointIndex(tmplist);
		tmphull.add(tmplist.get(firstIndex));
		currentMinAngle = 0;
		for (int i = nextIndex(firstIndex, tmplist); i != firstIndex; i = nextIndex(
				i, tmplist)) {
			tmphull.add(tmplist.get(i));
		}

		this.hull = tmphull;
	}

	public List<T> getHull() {
		return this.hull;
	}

	private int nextIndex(int currentIndex, List<T> points) throws Exception {
		double minAngle = MAX_ANGLE;
		double pseudoAngle;
		int minIndex = 0;
		for (int i = 0; i < points.size(); i++) {
			if (i != currentIndex) {
//				//FIXME
//				if((points.get(i).getX() - points.get(currentIndex).getX()) == 0
//						&& (points.get(i).getY() - points.get(currentIndex).getY()) == 0) {
//					System.err.println("error jarvis");
//					continue;
//				}
//				//
				pseudoAngle = getPseudoAngle(
						points.get(i).getX() - points.get(currentIndex).getX(),
						points.get(i).getY() - points.get(currentIndex).getY());
				if (pseudoAngle >= currentMinAngle && pseudoAngle < minAngle) {
					minAngle = pseudoAngle;
					minIndex = i;
				} else if (pseudoAngle == minAngle) {
					if ((Math.abs(points.get(i).getX()
							- points.get(currentIndex).getX()) > Math.abs(points
							.get(minIndex).getX() - points.get(currentIndex).getX()))
							|| (Math.abs(points.get(i).getY()
									- points.get(currentIndex).getY()) > Math
										.abs(points.get(minIndex).getY()
												- points.get(currentIndex).getY()))) {
						minIndex = i;
					}
				}
			}

		}
		currentMinAngle = minAngle;
		return minIndex;
	}
	
	//获得起始点
	private int getFirstPointIndex(List<T> points) {
		int minIndex = 0;
		for (int i = 1; i < points.size(); i++) {
			if (points.get(i).getY() < points.get(minIndex).getY()) {
				minIndex = i;
			} else if ((points.get(i).getY() == points.get(minIndex).getY())
					&& (points.get(i).getX() < points.get(minIndex).getX())) {
				minIndex = i;
			}
		}
		return minIndex;
	}

	private double getPseudoAngle(double dx, double dy) throws Exception {
		if (dx > 0 && dy >= 0)
			return dy / (dx + dy);
		if (dx <= 0 && dy > 0)
			return 1 + (Math.abs(dx) / (Math.abs(dx) + dy));
		if (dx < 0 && dy <= 0)
			return 2 + (dy / (dx + dy));
		if (dx >= 0 && dy < 0)
			return 3 + (dx / (dx + Math.abs(dy)));
//		throw new Error("Impossible");
		throw new Exception("Impossible");
	}
}
