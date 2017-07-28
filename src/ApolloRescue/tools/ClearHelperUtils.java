/**
 * 
 */
package ApolloRescue.tools;

/**
 * @author YangJiedong
 *
 */
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;

public class ClearHelperUtils {
	
	/**
	 * 获得两个Point2D之间的距离
	 * 
	 * @param p1
	 * @param p2
	 * @return 返回距离为整数
	 */
	public static int getDistance(Point2D p1, Point2D p2) {
		double dx, dy;
		int d;

		dx = p1.getX() - p2.getX();
		dy = p1.getY() - p2.getY();
		d = (int) Math.hypot(dx, dy);// 勾股定理
		return d;
	}

	/**
	 * 获取一个Point2D到Building的距离，Building位置取中心
	 * 
	 * @param p
	 * @param b
	 * @return 返回距离为整数
	 */
	public static int getDistance(Point2D p, Building b) {
		Point2D center = new Point2D(b.getX(), b.getY());
		return ClearHelperUtils.getDistance(p, center);
	}

	/**
	 * 获取一个Point2D到Road的距离，Road的位置取中心
	 * 
	 * @param p
	 * @param r
	 * @return 返回距离为整数
	 */
	public static int getDistance(Point2D p, Road r) {
		Point2D center = new Point2D(r.getX(), r.getY());
		return ClearHelperUtils.getDistance(p, center);
	}

	/**
	 * 获取一个Point2D到Blockade的距离，Blockade的位置取中心
	 * 
	 * @param p
	 * @param b
	 * @return 返回距离为整数
	 */
	public static int getDistance(Point2D p, Blockade b) {
		Point2D center = new Point2D(b.getX(), b.getY());
		return ClearHelperUtils.getDistance(p, center);
	}

	/**
	 * 获取一个Point2D到Human的距离
	 * 
	 * @param p
	 * @param h
	 * @return 返回距离为整数
	 */
	public static int getDistance(Point2D p, Human h) {
		Point2D center = new Point2D(h.getX(), h.getY());
		return ClearHelperUtils.getDistance(p, center);
	}

	/**
	 * 获取一个Point2D到Edge的距离，获取方法为点对边作垂线，求垂线段的长
	 * 
	 * @param point
	 * @param edge
	 * @return
	 */
	public static int getDistance(Point2D point, Edge edge) {
		int d;
		java.awt.geom.Line2D l;

		l = ClearHelperUtils.getLine(edge);
		Param line = CalParam(l.getP1(), l.getP2());
		Param verticalLine = getVerticalLine(line, point);
		Point2D p = getIntersectPoint(line, verticalLine);
		d = getDistance(p, point);
		return d;
	}// 获得点到边的距离

	/**
	 * 用Edge获取Line2D
	 * 
	 * @param edge
	 * @return
	 */
	public static java.awt.geom.Line2D getLine(Edge edge) {
		Point p1, p2;
		int x, y;
		java.awt.geom.Line2D line;

		x = edge.getStartX();
		y = edge.getStartY();
		p1 = new Point(x, y);
		x = edge.getEndX();
		y = edge.getEndY();
		p2 = new Point(x, y);
		line = new java.awt.geom.Line2D.Double(p1, p2);
		return line;
	}

	/**
	 * 计算直线参数的方法
	 * 
	 * @param p1
	 *            直线上第一个点
	 * @param p2
	 *            直线上第二个点
	 * @return 直线方程的三个参数
	 */
	public static Param CalParam(java.awt.geom.Point2D p1,
			java.awt.geom.Point2D p2) {
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
	 * 计算直线参数的方法
	 * 
	 * @param p1
	 *            直线上第一个点
	 * @param p2
	 *            直线上第二个点
	 * @return 直线方程的三个参数
	 */
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
	 * 获取一个点到一个直线的垂线的方程参数
	 * 
	 * @param pm
	 *            直线方程参数
	 * @param p
	 *            点
	 * @return 点到直线的垂线的方程参数
	 */
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

	/**
	 * 获得两条直线的相交点
	 * 
	 * @param pm1
	 *            直线参数1
	 * @param pm2
	 *            直线参数2
	 * @return 交点
	 */
	public static Point2D getIntersectPoint(Param pm1, Param pm2) {
		return getIntersectPoint(pm1.a, pm1.b, pm1.c, pm2.a, pm2.b, pm2.c);
	}

	/**
	 * 获得两条直线的相交点
	 * 
	 * @param a1
	 *            第一条直线的参数a
	 * @param b1
	 *            第一条直线的参数b
	 * @param c1
	 *            第一条直线的参数c
	 * @param a2
	 *            第二条直线的参数a
	 * @param b2
	 *            第二条直线的参数b
	 * @param c2
	 *            第二条直线的参数c
	 * @return 交点
	 */
	public static Point2D getIntersectPoint(double a1, double b1, double c1,
			double a2, double b2, double c2) {
		Point2D p = null;
		double m = a1 * b2 - a2 * b1;
		if (m == 0) {
			// 两条线平行
			// System.out.println("返回0");
			return null;
		}
		double x = (c2 * b1 - c1 * b2) / m;
		double y = (c1 * a2 - c2 * a1) / m;
		p = new Point2D((int) x, (int) y);
		return p;
	}

	/**
	 * 从Apexs中获取所有坐标点的列表（有序的）
	 * 
	 * @param Apexs
	 * @return
	 */
	public static List<Point2D> getPointsFromApexs(int[] Apexs) {
		List<Point2D> points = new ArrayList<Point2D>();
		for (int i = 0; i < Apexs.length; i += 2) {
			points.add(new Point2D(Apexs[i], Apexs[i + 1]));
		}
		return points;
	}

	/**
	 * 从Edge的列表（必须有序，且闭环）中获取其中所有的点
	 * 
	 * @param edges
	 *            边的列表
	 * @return 点的列表
	 */
	public static List<Point2D> getPoint2DFromEdges(List<Edge> edges) {// TODO:test
		List<Point2D> points = new ArrayList<Point2D>();
		if (edges == null || edges.size() <= 2) {
			return null;
		} else {
			for (Edge e : edges) {
				points.add(e.getStart());
			}
			return points;
		}
	}

	/**
	 * 从点的数组中（有序）获取边的列表
	 * 
	 * @param points
	 *            点的数组
	 * @return
	 */
	public static List<Edge> getEdgesFromPoint2D(Point2D[] points) {
		List<Edge> edges = new ArrayList<Edge>();
		if (null != points && points.length > 0) {
			for (int i = 0; i < points.length; i++) {
				// 如果是最有一个元素
				if (i == (points.length - 1)) {
					Edge edge = new Edge((int) points[i].getX(),
							(int) points[i].getY(), (int) points[0].getX(),
							(int) points[0].getY());
					edges.add(edge);
				} else {
					Edge edge = new Edge((int) points[i].getX(),
							(int) points[i].getY(), (int) points[i + 1].getX(),
							(int) points[i + 1].getY());
					edges.add(edge);
				}
			}
		}
		return edges;
	}

	/**
	 * 从点的列表中获取边的列表
	 * 
	 * @param points
	 *            点的列表
	 * @return 边的列表
	 */
	public static List<Edge> getEdgesFromPoint2D(List<Point2D> points) {
		List<Edge> edges = new ArrayList<Edge>();
		if (null != points && points.size() > 0) {
			for (int i = 0; i < points.size(); i++) {
				// 如果是最有一个元素
				if (i == (points.size() - 1)) {
					Edge edge = new Edge((int) points.get(i).getX(),
							(int) points.get(i).getY(), (int) points.get(0)
									.getX(), (int) points.get(0).getY());
					edges.add(edge);
				} else {
					Edge edge = new Edge((int) points.get(i).getX(),
							(int) points.get(i).getY(), (int) points.get(i + 1)
									.getX(), (int) points.get(i + 1).getY());
					edges.add(edge);
				}
			}
		}
		return edges;
	}

	/**
	 * 判断一个点是否在一个区域内
	 * 
	 * @param shape
	 *            区域轮廓边的列表（有序且闭环）
	 * @param point
	 * @return 返回true则在图形内
	 */
//	public static boolean insideShape(List<Edge> shape, Point2D point) {
//		int x = (int) point.getX();
//		int y = (int) point.getY();
//		int left = 0;
//		int right = 0;
//		Edge testEdge = new Edge(0, y, Integer.MAX_VALUE, y);
//		for (Edge e : shape) {
//			if (e.getStartY() > y) {
//				if (e.getEndY() <= y) {
//					Point2D intercetPoint = ClearHelperUtils
//							.getSegmentIntercetPoint(e, testEdge);
//					if (intercetPoint != null) {
//						if (intercetPoint.getX() < x) {
//							left++;
//						} else if (intercetPoint.getX() > x) {
//							right++;
//						}
//					}
//				}
//			} else if (e.getEndY() > y) {
//				Point2D intercetPoint = ClearHelperUtils
//						.getSegmentIntercetPoint(e, testEdge);
//				if (intercetPoint != null) {
//					if (intercetPoint.getX() < x) {
//						left++;
//					} else if (intercetPoint.getX() > x) {
//						right++;
//					}
//				}
//			}
//		}
//		if (left % 2 == 1 && right % 2 == 1) {
//			return true;
//		} else {
//			return false;
//		}
//	}

	 public static boolean insideShape( List<Edge> shape,Point2D point){
	 java.awt.geom.Point2D.Double p = new
	 java.awt.geom.Point2D.Double(point.getX(),point.getY());
	 List<java.awt.geom.Point2D.Double> polygon = new
	 ArrayList<java.awt.geom.Point2D.Double>();
	 for (Edge e:shape){
	 int x = e.getStartX();
	 int y = e.getStartY();
	 polygon.add(new java.awt.geom.Point2D.Double(x,y));
	 }
	 return ClearHelperUtils.insideShape(p, polygon);
	
	 }

	/**
	 * 判断一个点是否在图形内（该方法调用java底层方法，暂时还未测试）
	 * 
	 * @param point
	 * @param polygon
	 * @return
	 */
	public static boolean insideShape(java.awt.geom.Point2D.Double point,
			List<java.awt.geom.Point2D.Double> polygon) {
		java.awt.Polygon p = new Polygon();

		// java.awt.geom.GeneralPath
		final int TIMES = 1;

		for (java.awt.geom.Point2D.Double d : polygon) {
			int x = (int) d.x * TIMES;
			int y = (int) d.y * TIMES;
			p.addPoint(x, y);
		}

		int x = (int) point.x * TIMES;
		int y = (int) point.y * TIMES;

		return p.contains(x, y);
	}

	// public static boolean insideShape(java.awt.geom.Point2D.Double point,
	// List<java.awt.geom.Point2D.Double> polygon){
	// java.awt.geom.GeneralPath p = new java.awt.geom.GeneralPath();
	// java.awt.geom.Point2D.Double first = polygon.get(0);
	// p.moveTo(first.x, first.y);
	//
	// for (java.awt.geom.Point2D.Double d : polygon) {
	// p.lineTo(d.x, d.y);
	// }
	//
	// p.lineTo(first.x, first.y);
	//
	// p.closePath();
	//
	// return p.contains(point);
	// }

	// public static boolean insideShape(List<Edge> shape,Point2D p){//
	// int count1=0,count2=0;
	// for(Edge e:shape){
	// long
	// s=linefunc(e.getStartX(),e.getStartY(),e.getEndX(),e.getEndY(),(int)p.getX(),(int)p.getY());
	// if (s>0){
	// count1++;
	// }
	// if (s<0){
	// count2++;
	// }
	// }
	// if (count1!=0&&count2!=0){
	// return false;
	// }
	// else {
	// return true;
	// }
	// }

	private static long linefunc(int p1x, int p1y, int p2x, int p2y, int nx,
			int ny) {
		long ret = (p2y - p1y) * (p2x - nx) - (p2y - ny) * (p2x - p1x);
		return ret;
	}

	/**
	 * 获取两条线段的交点（并不是直线）
	 * 
	 * @param e1
	 * @param e2
	 * @return 如果没有交点，返回null
	 */
	public static Point2D getSegmentIntercetPoint(Edge e1, Edge e2) {
		Point2D a = e1.getStart();
		Point2D b = e1.getEnd();
		Point2D c = e2.getStart();
		Point2D d = e2.getEnd();
		return ClearHelperUtils.getSegmentIntercetPoint(a, b, c, d);
	}

	/**
	 * 获取线段的交点
	 * 
	 * @param a
	 *            线段一的起始端
	 * @param b
	 *            线段一的终止端
	 * @param c
	 *            线段二的起始端
	 * @param d
	 *            线段二的终止端
	 * @return 如果没有交点，返回null
	 */
	public static Point2D getSegmentIntercetPoint(Point2D a, Point2D b,
			Point2D c, Point2D d) {
		double area_abc = (a.getX() - c.getX()) * (b.getY() - c.getY())
				- (a.getY() - c.getY()) * (b.getX() - c.getX());
		// double area_abc = (a.getX() - c.getX()) * (b.getY()- c.y) - (a.y -
		// c.y) * (b.x - c.x);
		double area_abd = (a.getX() - d.getX()) * (b.getY() - d.getY())
				- (a.getY() - d.getY()) * (b.getX() - d.getX());
		// 面积符号相同则两点在线段同侧,不相交 (对点在线段上的情况,本例当作不相交处理);
		if (area_abc * area_abd >= 0) {
			return null;
		}

		// 三角形cda 面积的2倍
		double area_cda = (c.getX() - a.getX()) * (d.getY() - a.getY())
				- (c.getY() - a.getY()) * (d.getX() - a.getX());
		// 三角形cdb 面积的2倍
		// 注意: 这里有一个小优化.不需要再用公式计算面积,而是通过已知的三个面积加减得出.
		double area_cdb = area_cda + area_abc - area_abd;
		if (area_cda * area_cdb >= 0) {
			return null;
		}

		// 计算交点坐标
		double t = area_cda / (area_abd - area_abc);
		double dx = t * (b.getX() - a.getX()), dy = t * (b.getY() - a.getY());
		Point2D p = new Point2D((int) (a.getX() + dx), (int) (a.getY() + dy));
		return p;
		// return { x: a.x + dx , y: a.y + dy };

	}

	/**
	 * 获取Edge的中点
	 * 
	 * @param e
	 * @return
	 */
	public static Point2D getEdgeCenter(Edge e) {// 求边的中点
		if (e == null) {
			return null;
		} else {
			double dx = (e.getStartX() + e.getEndX()) / 2;
			double dy = (e.getStartY() + e.getEndY()) / 2;
			return new Point2D(dx, dy);
		}
	}

	// public static double getTotalSurface(Area a){//以平方米为单位
	// Point2D areaCenter = new Point2D(a.getX(),a.getY());
	// List<Edge> edges = a.getEdges();
	//
	// double surface = 0;
	// for (Edge e:edges){
	// double edgeLength = ClearHelperUtils.getDistance(e.getStart(),
	// e.getEnd());
	// double height = ClearHelperUtils.getDistance(areaCenter, e);
	// surface += edgeLength*height/2;
	// }
	// return surface*0.001*0.001;
	//
	// }

	/**
	 * 获取一个Area的面积
	 * 
	 * @param a
	 * @return 面积为double，如果area为null也返回0
	 */
	public static double getTotalSurface(Area a) {
		if (a == null) {
			return 0;
		}
		if (a.getApexList() == null) {
			return 0;
		}
		int[] xPoints = new int[a.getApexList().length / 2];
		int[] yPoints = new int[a.getApexList().length / 2];
		for (int i = 0; i < a.getApexList().length; i += 2) {
			xPoints[i / 2] = a.getApexList()[i];
			yPoints[i / 2] = a.getApexList()[i + 1];
		}
		double surface = ClearHelperUtils.surface(new java.awt.geom.Area(
				new Polygon(xPoints, yPoints, xPoints.length))) * 0.001 * 0.001;
		return surface;
	}

	// 获得ClearＡrea
	/**
	 * 获取清理Area
	 * 
	 * @param agent
	 * @param targetX
	 *            目标X坐标
	 * @param targetY
	 *            目标Y坐标
	 * @param clearLength
	 *            清理长度
	 * @param clearRad
	 *            清理宽度
	 * @return 清理区域的java.awt.geom.Area
	 */
	public static java.awt.geom.Area getClearArea(Human agent, int targetX,
			int targetY, int clearLength, int clearRad) {
		Vector2D agentToTarget = new Vector2D(targetX - agent.getX(), targetY
				- agent.getY());

		if (agentToTarget.getLength() > clearLength) {
			agentToTarget = agentToTarget.normalised().scale(clearLength + 510); // TODO
																					// new
		}

		Vector2D backAgent = (new Vector2D(agent.getX(), agent.getY()))
				.add(agentToTarget.normalised().scale(-510)); // 新server中变成一个常量
		Line2D line = new Line2D(backAgent.getX(), backAgent.getY(),
				agentToTarget.getX(), agentToTarget.getY());

		Vector2D dir = agentToTarget.normalised().scale(clearRad);
		Vector2D perpend1 = new Vector2D(-dir.getY(), dir.getX());
		Vector2D perpend2 = new Vector2D(dir.getY(), -dir.getX());

		rescuecore2.misc.geometry.Point2D points[] = new rescuecore2.misc.geometry.Point2D[] {
				line.getOrigin().plus(perpend1),
				line.getEndPoint().plus(perpend1),
				line.getEndPoint().plus(perpend2),
				line.getOrigin().plus(perpend2) };
		int[] xPoints = new int[points.length];
		int[] yPoints = new int[points.length];
		for (int i = 0; i < points.length; i++) {
			xPoints[i] = (int) points[i].getX();
			yPoints[i] = (int) points[i].getY();
		}
		return new java.awt.geom.Area(new Polygon(xPoints, yPoints,
				points.length));
	}

	public static java.awt.geom.Area getClearArea(List<Edge> judgeArea) {
		if (judgeArea == null || judgeArea.size() <= 3) {
			return null;
		} else {
			int[] xPoints = new int[judgeArea.size()];
			int[] yPoints = new int[judgeArea.size()];
			for (int index = 0; index < judgeArea.size(); index++) {
				xPoints[index] = judgeArea.get(index).getStartX();
				yPoints[index] = judgeArea.get(index).getStartY();
			}
			return new java.awt.geom.Area(new Polygon(xPoints, yPoints,
					judgeArea.size()));
		}
	}

	/**
	 * 根据一块矩形 获得矩形内障碍物的面积
	 * 
	 * @param clearArea
	 *            清理区域java.awt.geom.Area
	 * @param blockades
	 *            可能的障碍物集合
	 * @return 矩形内的障碍物面积，为double
	 */
	public static double calcBlockedArea(java.awt.geom.Area clearArea,
			Set<Blockade> blockades) {
		List<java.awt.geom.Area> blockadesArea = new ArrayList<java.awt.geom.Area>();

		if (null != blockades && blockades.size() > 0) {
			for (Blockade b : blockades) {
				Shape shape = b.getShape();
				if (null != shape) {
					java.awt.geom.Area tempArea = new java.awt.geom.Area(shape);
					blockadesArea.add(tempArea);
				}
			}
		}

		double firstSurface = ClearHelperUtils.surface(clearArea); // 生成矩形的面积
		for (java.awt.geom.Area blockade : blockadesArea) {
			clearArea.subtract(blockade);
		}
		double surface = ClearHelperUtils.surface(clearArea); // 去除掉路障的面积
		double clearedSurface = firstSurface - surface; // 得到路障的面积

		return clearedSurface;
	}

	/**
	 * 计算图形的面积
	 * 
	 * @param area
	 *            java.awt.geom.Area
	 * @return 面积，为double
	 */
	public static double surface(java.awt.geom.Area area) {
		if (null == area) {
			return 0;
		}
		PathIterator iter = area.getPathIterator(null);
		if (null == iter) {
			System.out.println("iter is null");
			return 0;
		}
		double sum_all = 0;
		while (!iter.isDone()) {
			List<double[]> points = new ArrayList<double[]>();
			while (!iter.isDone()) {
				double point[] = new double[2];
				int type = iter.currentSegment(point);
				iter.next();
				if (type == PathIterator.SEG_CLOSE) {
					if (points.size() > 0)
						points.add(points.get(0));
					break;
				}
				points.add(point);
			}

			double sum = 0;
			for (int i = 0; i < points.size() - 1; i++) {
				sum += points.get(i)[0] * points.get(i + 1)[1]
						- points.get(i)[1] * points.get(i + 1)[0];
			}

			sum_all += Math.abs(sum) / 2;
		}

		return sum_all;
	}

	/**
	 * 将向量顺时针旋转一定角度
	 * 
	 * @param v
	 *            待旋转向量
	 * @param a
	 *            旋转的角度，角度制
	 * @return 旋转后的向量
	 */
	public static Vector2D rotClockWise(Vector2D v, double a) {// 向量顺时针旋转角度a，角度制
		a = a / 180 * Math.PI;
		return new Vector2D(v.getX() * Math.cos(a) + v.getY() * Math.sin(a),
				-v.getX() * Math.sin(a) + v.getY() * Math.cos(a));
	}

	/**
	 * 将向量逆时针旋转一定角度
	 * 
	 * @param v
	 *            待旋转向量
	 * @param a
	 *            旋转的角度，角度制
	 * @return 旋转后的向量
	 */
	public static Vector2D rotCounterClockWise(Vector2D v, double a) {// 向量逆时针旋转角度a，角度制
		a = a / 180 * Math.PI;
		return new Vector2D(v.getX() * Math.cos(a) - v.getY() * Math.sin(a),
				v.getX() * Math.sin(a) + v.getY() * Math.cos(a));
	}

	/**
	 * 判断一个Point2D是否在线段Edge上，（由于求距离忽略了小数，最后结果可能会有偏差，需要检测）
	 * 
	 * @param p
	 * @param e
	 * @return
	 */
	public static boolean isOnSegment(Point2D p, Edge e) {// TODO:test
		int totalDis = ClearHelperUtils.getDistance(e.getStart(), e.getEnd());
		int testDis = ClearHelperUtils.getDistance(p, e.getStart())
				+ ClearHelperUtils.getDistance(p, e.getEnd());
		if (totalDis == testDis) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 寻找一个点到线段上最近的点，如果垂足在线段上，则选择垂足，如果垂足不在线段上，则选择距离点最近的一端
	 * 
	 * @param p
	 * @param e
	 * @return
	 */
	public static Point2D closestPointOnSegment(Point2D p, Edge e) {
		if (ClearHelperUtils.isOnSegment(p, e)) {
			return p;
		}
		Param pm1 = ClearHelperUtils.CalParam(e.getStart(), e.getEnd());
		Param pm2 = getVerticalLine(pm1, p);
		Point2D intersectPoint = getIntersectPoint(pm1, pm2);
		if (ClearHelperUtils.isOnSegment(intersectPoint, e)) {// 垂足在线段上
			return intersectPoint;
		} else {// 垂足不在线段上
			int dis1 = ClearHelperUtils.getDistance(intersectPoint,
					e.getStart());
			int dis2 = ClearHelperUtils.getDistance(intersectPoint, e.getEnd());
			if (dis1 < dis2) {
				return e.getStart();
			} else {
				return e.getEnd();
			}
		}
	}
	
	/**
	 * 寻找线段两个端点哪个更接近点的位置
	 * @param p
	 * @param e
	 * @return
	 */
	public static Point2D closestSideOnSegment(Point2D p,Edge e){
		int dis1 = ClearHelperUtils.getDistance(p,
				e.getStart());
		int dis2 = ClearHelperUtils.getDistance(p, e.getEnd());
		if (dis1 <= dis2) {
			return e.getStart();
		} else {
			return e.getEnd();
		}
	}

	/**
	 * 这个交点是直线与线段的交点
	 * 
	 * @param e1
	 *            直线
	 * @param e2
	 *            线段
	 * @return
	 */
	public static Point2D getIntercetPoint(Edge e1, Edge e2) {
		Param pm1 = ClearHelperUtils.CalParam(e1.getStart(), e1.getEnd());
		Param pm2 = ClearHelperUtils.CalParam(e2.getStart(), e2.getEnd());
		Point2D intersectPoint = ClearHelperUtils.getIntersectPoint(pm1, pm2);
		if (intersectPoint == null) {
			return null;
		} else {
			if (ClearHelperUtils.isOnSegment(intersectPoint, e2)) {
				return intersectPoint;
			} else {
				return null;
			}
		}
	}

	/**
	 * 判断一条线段是否在多边形内部
	 * 
	 * @param edge
	 * @param shape
	 * @return
	 */
	public static boolean isEdgeInsideShape(Edge edge, List<Edge> shape) {
		if (shape == null || shape.size() == 0 || edge == null) {
			return false;
		} else {
			if (insideShape(shape, edge.getStart())){
//					&& insideShape(shape, edge.getEnd())) {// 两个端点都在图形内
				for (Edge e : shape) {
					if (e.isPassable()){
						continue;
					}
					if (getSegmentIntercetPoint(e, edge) != null) {// 发现交点
						return false;
					}
				}
				return true;
			} else {
				return false;
			}
		}
	}
	
	public static boolean isEdgeIntercetShape(Edge edge, List<Edge> shape){
		if (shape == null || shape.size() == 0 || edge == null) {
			return false;
		} else {
			for (Edge e : shape) {
				if (getSegmentIntercetPoint(e, edge) != null) {// 发现交点
					return true;
				}
		}
			return false;
	}
	}

	/**
	 * 生成一条边向左平移一段距离的边
	 * 
	 * @param edge
	 *            基准边
	 * @param width
	 *            平移距离
	 * @return 生成的一条边
	 */
	public static Edge getLeftSideEdge(Edge edge, int width) {
		if (edge == null) {
			return null;
		} else {
			Vector2D direction = edge.getEnd().minus(edge.getStart());
			direction = ClearHelperUtils.rotClockWise(direction, -90);
			direction = direction.normalised().scale(width);
			Point2D newStart = edge.getStart().plus(direction);
			Point2D newEnd = edge.getEnd().plus(direction);
			return new Edge(newStart, newEnd);
		}
	}

	/**
	 * 生成一条边向右平移一段距离的边
	 * 
	 * @param edge
	 *            基准边
	 * @param width
	 *            平移距离
	 * @return 生成的一条边
	 */
	public static Edge getRightSideEdge(Edge edge, int width) {
		if (edge == null) {
			return null;
		} else {
			Vector2D direction = edge.getEnd().minus(edge.getStart());
			direction = ClearHelperUtils.rotClockWise(direction, 90);
			direction = direction.normalised().scale(width);
			Point2D newStart = edge.getStart().plus(direction);
			Point2D newEnd = edge.getEnd().plus(direction);
			return new Edge(newStart, newEnd);
		}
	}

	public static List<Edge> getAreaShape(Area a) {
		if (a == null || a.getApexList().length == 0) {
			return null;
		} else {
			return ClearHelperUtils.getEdgesFromPoint2D(ClearHelperUtils
					.getPointsFromApexs(a.getApexList()));
		}
	}
}
