/**
 * 
 */
package ApolloRescue.tools;

/**
 * @author YangJiedong
 *
 */
import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.misc.geometry.GeometryTools2D;

public class Ruler {
	public static int getCenterDistance(Edge e1, Edge e2) {
		Point p1, p2;
		int d;

		p1 = Locator.getCenter(e1);
		p2 = Locator.getCenter(e2);
		d = getDistance(p1, p2);
		// System.out.print("$$$$$$$$$");
		return d;
	}

	// wq修改
	public static int getDistance(Edge e1, Edge e2) {
		Point p1, p2;
		double d1, d2;
		int d;
		p1 = Locator.getCenter(e1);
		p2 = Locator.getCenter(e2);
		d1 = getDistance(p1, e1);
		d2 = getDistance(p2, e2);
		d = (int) Math.min(d1, d2);
		return d;
	}// 求得是垂线段最小值，即道路的最窄处

	public static int getDistance(double x1, double y1, double x2, double y2) {// wq修改
		double dx = x1 - x2;
		double dy = y1 - y2;
		return (int) Math.hypot(dx, dy);
	}

	// public static int getDistance(Edge e1, Edge e2) {
	// Point p1, p2;
	// double d1, d2, d3, d4;
	// int d;
	// Line2D l1, l2;
	//
	// l1 = Locator.getLine(e1);
	// l2 = Locator.getLine(e2);
	// d1 = l1.ptSegDist(l2.getP1());
	// d2 = l1.ptSegDist(l2.getP2());
	// d3 = l2.ptSegDist(l1.getP1());
	// d4 = l2.ptSegDist(l1.getP2());
	// d = (int) Math.min(Math.min(d3, d4), Math.min(d1, d2));
	// return d;
	// }//求得是垂线段最小值，即道路的最窄处

	public static int getMaxDistance(Edge e1, Edge e2) {
		Point p1, p2;
		double d1, d2, d3, d4;
		int d;
		Line2D l1, l2;

		l1 = Locator.getLine(e1);
		l2 = Locator.getLine(e2);
		d1 = l1.ptSegDist(l2.getP1());
		d2 = l1.ptSegDist(l2.getP2());
		d3 = l2.ptSegDist(l1.getP1());
		d4 = l2.ptSegDist(l1.getP2());

		d = (int) Math.max(Math.max(d3, d4), Math.max(d1, d2));
		return d;

	}

	public static int getDistance(Point2D p1, Point2D p2) {
		double dx, dy;
		int d;

		dx = p1.getX() - p2.getX();
		dy = p1.getY() - p2.getY();
		d = (int) Math.hypot(dx, dy);// 勾股定理
		return d;
	}

	public static int getDistance(Point p1, Point p2) {
		double dx, dy;
		int d;

		dx = p1.getX() - p2.getX();
		dy = p1.getY() - p2.getY();
		d = (int) Math.hypot(dx, dy);// 勾股定理
		return d;
	}

	public static int getDistanceToBlock(Blockade b, int x, int y) {
		List<rescuecore2.misc.geometry.Line2D> lines = GeometryTools2D
				.pointsToLines(
						GeometryTools2D.vertexArrayToPoints(b.getApexes()),
						true);
		double best = Double.MAX_VALUE;
		rescuecore2.misc.geometry.Point2D origin = new rescuecore2.misc.geometry.Point2D(
				x, y);
		for (rescuecore2.misc.geometry.Line2D next : lines) {
			rescuecore2.misc.geometry.Point2D closest = GeometryTools2D
					.getClosestPointOnSegment(next, origin);
			double d = GeometryTools2D.getDistance(origin, closest);
			// LOG.debug("Next line: " + next + ", closest point: " + closest +
			// ", distance: " + d);
			if (d < best) {
				best = d;
				// LOG.debug("New best distance");
			}

		}
		return (int) best;
	}

	public static int getDistanceToBlock(Blockade block, Point point) {
		int x, y, d;

		x = point.x;
		y = point.y;
		d = getDistanceToBlock(block, x, y);
		return d;
	}

	public static int getLength(Line2D line) {
		Point2D p1, p2;
		int d;

		p1 = line.getP1();
		p2 = line.getP2();
		d = getDistance(p1, p2);
		return d;
	}

	public static int getDistance(Building b1, Building b2) {
		int min;
		boolean first;

		first = true;
		min = 0;
		for (Edge edge : b1.getEdges()) {
			int d;

			d = getDistance(b2, edge);
			if (first || d < min) {
				min = d;
				first = false;
			}
		}
		return min;
	}

	public static int getDistance(Building building, Edge edge) {
		int min;
		boolean first;

		first = true;
		min = 0;
		for (Edge e : building.getEdges()) {
			int d;

			d = getDistance(e, edge);
			if (first || d < min) {
				min = d;
				first = false;
			}
		}
		return min;
	}

	public static int getDistanceToEdges(Point point, Building building) {
		int min;
		boolean first;

		first = true;
		min = 0;
		for (Edge e : building.getEdges()) {
			int d;

			d = getDistance(point, e);
			if (first || d < min) {
				min = d;
				first = false;
			}
		}
		return min;
	}

	public static int getMaxDistanceToEdges(Point point, Building building) {
		int max;
		boolean first;

		max = 0;
		first = true;
		for (Edge e : building.getEdges()) {
			int d;

			d = getDistance(point, e);
			if (first || d > max) {
				max = d;
				first = false;
			}
		}
		return max;
	}

	public static int getDistanceToWall(Building building) {
		Point center = new Point(building.getX(), building.getY());
		int max = getMaxDistanceToEdges(center, building);
		return max;
	}

	// public static int getDistance(Point point, Edge edge) {
	// int d;
	// Line2D l;

	// l = Locator.getLine(edge);
	// d = (int) l.ptSegDist(point);
	// return d;
	// }
	// wq修改
	public static int getDistance(Point2D point, Edge edge) {
		int d;
		Line2D l;

		l = Locator.getLine(edge);
		Param line = CalParam(l.getP1(), l.getP2());
		Param verticalLine = getVerticalLine(line, point);
		Point2D p = getIntersectPoint(line, verticalLine);
		d = getDistance(p, point);
		return d;
	}// 获得点到边的距离

	public static int getDistance(rescuecore2.misc.geometry.Point2D p1,
			rescuecore2.misc.geometry.Point2D p2) {
		double dx, dy;
		int d;

		dx = p1.getX() - p2.getX();
		dy = p1.getY() - p2.getY();
		d = (int) Math.hypot(dx, dy);
		return d;
	}

	public static int getDistance(rescuecore2.misc.geometry.Point2D edgeCenter,
			Edge edge) {
		// TODO Auto-generated method stub
		int d;
		Line2D l;
		Point point = new Point((int) edgeCenter.getX(),
				(int) edgeCenter.getY());

		l = Locator.getLine(edge);
		d = (int) l.ptSegDist(point);
		return d;
	}

	public static Point getCenterPoint(Edge edge) {
		int x1 = edge.getStartX();
		int x2 = edge.getEndX();

		int y1 = edge.getStartY();
		int y2 = edge.getEndY();

		int x = (x1 + x2) / 2;
		int y = (y1 + y2) / 2;

		Point p = new Point(x, y);
		return p;
	}

	public static rescuecore2.misc.geometry.Point2D getCenterPoint2D(Edge edge) {
		int x1 = edge.getStartX();
		int x2 = edge.getEndX();

		int y1 = edge.getStartY();
		int y2 = edge.getEndY();

		int x = (x1 + x2) / 2;
		int y = (y1 + y2) / 2;

		rescuecore2.misc.geometry.Point2D  p = new rescuecore2.misc.geometry.Point2D (x, y);    
		return p;
	}
	
	
	
	// 两条直线的交点
	// ax + by + c = 0;
	public static double[] getIntersection(double a1, double b1, double c1,
			double a2, double b2, double c2) {
		double[] res = new double[2];
		res[0] = (c1 * b2 - c2 * b1) / (a1 * b2 - a2 * b1);
		res[1] = (a1 * c2 - a2 * c1) / (a1 * b2 - a2 * b1);

		return res;
	}

	public static double[] getIntersection(double[] fun1, double[] fun2) {
		if (fun1.length != 3 || fun2.length != 3)
			return null;
		else
			return getIntersection(fun1[0], fun1[1], fun1[2], fun2[0], fun2[1],
					fun2[2]);
	}

	/*
	 * 计算 直线的系数
	 */
	public static Param CalParam(rescuecore2.misc.geometry.Point2D p1,
			rescuecore2.misc.geometry.Point2D p2) {
		double a, b, c;
		double x1 = p1.getX(), y1 = p1.getY(), x2 = p2.getX(), y2 = p2.getY();
		a = y2 - y1;
		b = x1 - x2;
		c = (x2 - x1) * y1 - (y2 - y1) * x1;
		if (b < 0) {
			a *= -1;
			b *= -1;
			c *= -1;
		} else if (b == 0 && a < 0) {
			a *= -1;
			c *= -1;
		}
		return new Param(a, b, c);
	}

	public static Param CalParam(Point p1, Point p2) {
		double a, b, c;
		double x1 = p1.getX(), y1 = p1.getY(), x2 = p2.getX(), y2 = p2.getY();
		a = y2 - y1;
		b = x1 - x2;
		c = (x2 - x1) * y1 - (y2 - y1) * x1;
		if (b < 0) {
			a *= -1;
			b *= -1;
			c *= -1;
		} else if (b == 0 && a < 0) {
			a *= -1;
			c *= -1;
		}
		return new Param(a, b, c);
	}

	public static Param CalParam(Point2D p1, Point2D p2) {
		double a, b, c;
		double x1 = p1.getX(), y1 = p1.getY(), x2 = p2.getX(), y2 = p2.getY();
		a = y2 - y1;
		b = x1 - x2;
		c = (x2 - x1) * y1 - (y2 - y1) * x1;
		if (b < 0) {
			a *= -1;
			b *= -1;
			c *= -1;
		} else if (b == 0 && a < 0) {
			a *= -1;
			c *= -1;
		}
		return new Param(a, b, c);
	}

	/**
	 * 计算两条直线的交点
	 */
	public static Point getIntersectPoint(Param pm1, Param pm2) {
		return getIntersectPoint(pm1.a, pm1.b, pm1.c, pm2.a, pm2.b, pm2.c);
	}

	public static Point getIntersectPoint(double a1, double b1, double c1,
			double a2, double b2, double c2) {
		Point p = null;
		double m = a1 * b2 - a2 * b1;
		if (m == 0) {
			// 两条线平行
			// System.out.println("返回0");
			return null;
		}
		double x = (c2 * b1 - c1 * b2) / m;
		double y = (c1 * a2 - c2 * a1) / m;
		p = new Point((int) x, (int) y);
		return p;
	}

	// 根据一条直线 获得一个点
	// 获得平行线
	public static Param getParrelLine(Param pm, Point2D p) {
		double a = pm.a;
		double b = pm.b;
		// double c = pm.c;

		double c1 = -a * p.getX() - b * p.getY();

		return new Param(a, b, c1);
	}

	// 根据一条直线和一个交点(垂足)获得垂线
	public static Param getVerticalLine(Param pm, Point2D p) {
		double a = pm.a;
		double b = pm.b;
		// double c = pm.c;

		// double c1 = a*p.getY()
		double temp = a;
		a = b;
		b = -temp;
		double c1 = -a * p.getX() - b * p.getY();
		return new Param(a, b, c1);
	}

	public static Param getVerticalLine(Param pm, Point p) {
		double a = pm.a;
		double b = pm.b;
		// double c = pm.c;

		// double c1 = a*p.getY()
		double temp = a;
		a = b;
		b = -temp;
		double c1 = -a * p.getX() - b * p.getY();
		return new Param(a, b, c1);
	}

	public static boolean isInLine(Point p, Edge edge) {
		double x = p.getX();
		double y = p.getY();

		int minX = 0;
		int maxX = 0;
		int minY = 0;
		int maxY = 0;

		if (edge.getStartX() <= edge.getEndX()) {
			minX = edge.getStartX();
			maxX = edge.getEndX();
		} else {
			minX = edge.getEndX();
			maxX = edge.getStartX();
		}

		if (edge.getStartY() <= edge.getEndY()) {
			minY = edge.getStartY();
			maxY = edge.getEndY();
		} else {
			minY = edge.getEndY();
			maxY = edge.getStartY();
		}

		if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isParallel(Param e1, Param e2) {
		double a1 = e1.a;
		double b1 = e1.b;
		double a2 = e2.a;
		double b2 = e2.b;
		double val = Math.abs(a2 * b1 - a1 * b2);
		if (val == 0) {
			return true;
		} else {
			return false;
		}

	}

}