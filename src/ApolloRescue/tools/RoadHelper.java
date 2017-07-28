//package ApolloRescue.tools;
//
//import java.awt.Point;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Set;
//
//import rescuecore2.misc.Pair;
//import rescuecore2.misc.geometry.Point2D;
//import rescuecore2.misc.geometry.Vector2D;
//import rescuecore2.standard.entities.Area;
//import rescuecore2.standard.entities.Blockade;
//import rescuecore2.standard.entities.Edge;
//import rescuecore2.standard.entities.StandardEntityURN;
//import rescuecore2.worldmodel.ChangeSet;
//import rescuecore2.worldmodel.EntityID;
//import rescuecore2.worldmodel.Property;
//import adf.agent.action.common.ActionMove;
//import adf.agent.develop.DevelopData;
//import adf.agent.info.AgentInfo;
//import adf.agent.info.ScenarioInfo;
//import adf.agent.info.WorldInfo;
//import adf.agent.module.ModuleManager;
//import adf.component.module.AbstractModule;
//
//public class RoadHelper {
//	private List<AbstractModule> subModules = new ArrayList<>();
//    protected ScenarioInfo scenarioInfo;
//    protected AgentInfo agentInfo;
//    protected WorldInfo worldInfo;
//    protected ModuleManager moduleManager;
//    protected DevelopData developData;
//
//    private int countPrecompute;
//    private int countResume;
//    private int countPreparate;
//    private int countUpdateInfo;
//    private int countUpdateInfoCurrentTime;
//
//	private List<Pair<Integer, Integer>> locations; //locations in 5 time steps
//	private int slowMoveFlag;
//	private Pair<Integer, Integer> lastStuckPosition ;
//	private int lastStuckTime;
////	private int stuckPositionY ;
////	private boolean
//	private List<Integer> timeSteps;
//
//	public RoadHelper(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
//		this.worldInfo = wi;
//        this.agentInfo = ai;
//        this.scenarioInfo = si;
//        this.moduleManager = moduleManager;
//        this.developData = developData;
//        this.countPrecompute = 0;
//        this.countResume = 0;
//        this.countPreparate = 0;
//        this.countUpdateInfo = 0;
//        this.countUpdateInfoCurrentTime = 0;
//
//        this.lastStuckTime = 0;
//
//        this.locations = new LinkedList<Pair<Integer, Integer>>();
//        this.timeSteps = new LinkedList<Integer>();
//	}
//
//	public boolean needToChangeTarget() {
//		if (locations.isEmpty()) {
//			return false;
//		}
////		if(this.amIMoveSlow()) {
////			return true;
////		}
////		else {
//		if(this.lastStuckPosition == null)
//			return false;
//		if (this.getDistance(this.agentInfo.getX(), this.agentInfo.getY(),
//				this.lastStuckPosition.first(), this.lastStuckPosition.second()) > 5000
//				&& agentInfo.getTime() - lastStuckTime > 3 && lastStuckTime > 5) {
//			return true;
//		}
////		}
//		return false;
//	}
//
//	public void setLocationHistory(AgentInfo ai, WorldInfo wi, ModuleManager mm) {
//
//		Pair<Integer, Integer> location = wi.getLocation(ai.me());
//		int time = ai.getTime();
//		if(time > 5 && this.agentInfo.getExecutedAction(time-1) instanceof ActionMove) {
//
//			if(this.locations.size() != 5) {
//				this.locations.add(location);
//				this.timeSteps.add(time);
//			}
//			if (this.locations.size() == 5) {
//				this.locations.remove(0);
//				this.timeSteps.remove(0);
//				this.locations.add(location);
//				this.timeSteps.add(time);
//			}
//		}
//   }
//
//   /**
//    * 自己身边一定距离没有被挡住的,且在自身area内的随机移动点 Note：如果所有方向都被挡住，则返回null
//    * @param length 移动距离
//    * @return
//    */
//   public Point2D randomMovePoint(int length) {
//	   List<Vector2D> possibleDirections = findUnblockedDirection(length);
//	   possibleDirections = findInsideAreaDirection(possibleDirections, length);
//	   Vector2D choosedDirection = getDirectionRandomly(possibleDirections);//TODO can we choose direction more wisely
//	   if (choosedDirection == null) {
//	   } else {
//		   Point2D selfPoint = new Point2D(agentInfo.getX(),agentInfo.getY());
//	   }
//	   return getMovePoint(choosedDirection, length);
//   }
//
//   public List<Vector2D> findInsideAreaDirection(List<Vector2D> directions,
//			int length) {
//		Area selfArea = (Area) agentInfo.getPositionArea();
//		Point2D selfPoint = new Point2D(agentInfo.getX(), agentInfo.getY());
//		if (selfArea == null || selfArea.getShape() == null) {// 自己所在area为null
//			return null;
//		}
//		if (directions == null || directions.size() == 0) {// 没有方向可选
//			return null;
//		}
//		List<Vector2D> result = new ArrayList<Vector2D>();
//		for (Vector2D direction : directions) {
//			Point2D point = selfPoint
//					.plus(direction.normalised().scale(length));
//			Edge edge = new Edge(selfPoint, point);
//			List<Edge> shape = selfArea.getEdges();
//			boolean hasIntercet = false;
//			for (Edge areaEdge : shape) {
//				if (!areaEdge.isPassable()) {
//					if (ClearHelperUtils
//							.getSegmentIntercetPoint(areaEdge, edge) != null) {
//						hasIntercet = true;
//					}
//				}
//			}
//			if (!hasIntercet) {
//				result.add(direction);
//			}
//		}
//		return result;
//   }
//
//   public Vector2D getDirectionRandomly(List<Vector2D> directions) {
//		if (directions == null || directions.size() == 0) {// 没有方向
//			return null;
//		} else if (directions.size() == 1) {// 待选方向只有一个
//			return directions.get(0);
//		} else {// 待选方向不只有一个
////			agentInfo.me().
//			int index = (int) (Math.random() * (directions.size() - 1));
//			// System.out.println("随机数字为："+index);
////			int i
//			return directions.get(index);
//
//		}
//	}
//
//   /**
//    * 给定方向和移动距离，生成移动点
//	*
//	* @param direction
//	*            给定方向
//	* @param length
//	*            移动距离
//	* @return 生成的移动点
//	*/
//   public Point2D getMovePoint(Vector2D direction, int length) {
//	   if (direction == null || length <= 0) {
//		   return null;
//	   } else {
//		   Point2D selfPoint = new Point2D(worldInfo.getLocation(this.agentInfo.me()).first(),
//				   worldInfo.getLocation(this.agentInfo.me()).second());
//		   return selfPoint.plus(direction.normalised().scale(length));
//	   }
//   }
//
//   /**
//	 * 返回在全方向中没有被障碍物阻挡的方向
//	 * 上层调用
//	 * @param length
//	 *            探测长度
//	 * @return 未被阻挡的方向
//	 */
//   public List<Vector2D> findUnblockedDirection(int length) {
//	   List<Vector2D> possibleDirections = getAllDirection();
//	   List<Vector2D> unblockedDirection = new ArrayList<Vector2D>();
//
//	   Point2D selfPoint = new Point2D(worldInfo.getLocation(this.agentInfo.me()).first(),
//			   							   worldInfo.getLocation(this.agentInfo.me()).second());
//	   for (Vector2D direction : possibleDirections) {
//		   Edge edge = new Edge(selfPoint, selfPoint.plus(direction.normalised().scale(length)));
//		   if (!isEdgeBlocked(edge)) {
//			   unblockedDirection.add(direction);
//		   }
//	   }
//	   return unblockedDirection;
//   }
//
//   /**
//    * 一个Edge是否和可见障碍物相交或被包含
//    * @param edge
//    * @return
//    */
//   public boolean isEdgeBlocked(Edge edge) {
//	   return blockadesDetecter(blockadeInSight(), edge);
//   }
//
//   /**
//    * 对多个障碍物判断一个Edge是否在障碍物内或有交点
//    * @param blockades
//    * @param e
//    * @return
//    */
//   private boolean blockadesDetecter(Set<Blockade> blockades, Edge e) {
//	   if (blockades == null || blockades.size() == 0 || e == null) {
//			return false;
//		} else {
//			for (Blockade b : blockades) {
//				if (blockadeDetecter(b, e)) {
//					return true;
//				}
//			}
//			return false;
//		}
//   }
//
//   /**
//    * 对单个障碍物判断一条Edge是否在障碍物内或有交点
//    * @param b
//    * @param e
//    * @return
//    */
//   private boolean blockadeDetecter(Blockade b, Edge e) {
//	   if (b == null || b.getApexes() == null || b.getApexes().length == 0
//				|| e == null) {
//			return false;
//		} else {
//			List<Edge> blockadeEdges = ClearHelperUtils
//					.getEdgesFromPoint2D(ClearHelperUtils.getPointsFromApexs(b
//							.getApexes()));
//			return
//			// ClearHelperUtils.insideShape(blockadeEdges, e.getStart())||
//			ClearHelperUtils.insideShape(blockadeEdges, e.getEnd())
//					|| ClearHelperUtils.isEdgeIntercetShape(e, blockadeEdges);
//		}
//   }
//
//   /**
//    * 选取视野内的障碍物
//    * @return
//    */
//   public Set<Blockade> blockadeInSight() {
//	   return getBlockadeSeen();
//   }
//
//   /**
//    * 获得看到的障碍物
//    * @return
//    */
//   public Set<Blockade> getBlockadeSeen() {
//	   Set<Blockade> blockadeSeen = new HashSet<Blockade>();
//	   ChangeSet changeSet = this.worldInfo.getChanged();
//	   for (EntityID entityID : changeSet.getChangedEntities()) {
//		   if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.BLOCKADE.toString())) {
//			   Blockade blockade = (Blockade) this.worldInfo.getEntity(entityID);
//			   for (Property p : changeSet.getChangedProperties(entityID)) {
//
//					blockade.getProperty(p.getURN()).takeValue(p);
////					propertyComponent.setPropertyTime(
////							blockade.getProperty(p.getURN()), time);
//				}
//				if (this.worldInfo.getEntity(blockade.getPosition()) != null) {
//					Area area = (Area) this.worldInfo.getEntity(blockade.getPosition());
//					if (area.getBlockades() == null) {
//						area.setBlockades(new ArrayList<EntityID>());
//					}
//					if (!area.getBlockades().contains(blockade.getID())) {
//						ArrayList<EntityID> blockades = new ArrayList<EntityID>(
//								area.getBlockades());
//						blockades.add(blockade.getID());
//						area.setBlockades(blockades);
//					}
//				}
//
////				if (getEntity(blockade.getID()) == null) {
////					addEntityImpl(blockade);
////					propertyComponent.addEntityProperty(blockade, time);
////				}
//
//				blockadeSeen.add(blockade);
//		   }
//	   }
//	   return blockadeSeen;
//   }
//
//   /**
//    * 获取全部方向的列表，以竖直向上为基础，不断顺时针旋转15度生成。
//    * @return
//    */
//   public List<Vector2D> getAllDirection() {
//		List<Vector2D> directions = new ArrayList<Vector2D>();
//		Vector2D v = new Vector2D(1, 0);
//		for (int angle = 0; angle < 360; angle += 15) {
//			directions.add(rotClockWise(v, angle));
//		}
//		return directions;
//   }
//   /**
//    * 将向量顺时针旋转一定角度
//    * @param v
//    * @param a
//    * @return
//    */
//   public static Vector2D rotClockWise(Vector2D v, double a) {// 向量顺时针旋转角度a，角度制
//		a = a / 180 * Math.PI;
//		return new Vector2D(v.getX() * Math.cos(a) + v.getY() * Math.sin(a),
//				-v.getX() * Math.sin(a) + v.getY() * Math.cos(a));
//	}
//
//
//   public enum StuckState {
//	    TOTALLY_BLOCKED,
//	    BLOCKED_IN_REGION,
//	    FREE,
//
//	}
//	/**
//	 * 判断是否在一个范围内移动 5周期
//	 *
//	 * @param distance
//	 *            移动距离
//	 * @return
//	 */
//	public boolean amIMoveSlow() {
//		int time = agentInfo.getTime();
//		if (time < 4) {
//			return false;
//		}
//		if (this.locations.isEmpty()) {
//			slowMoveFlag = 0;
//			return false;
//		}
//		//前提是上周发送的是移动命令
//		System.out.println("Time:" + this.agentInfo.getTime() + "\tAgent:(" + this.agentInfo.getID() + ") \t Last time Action type: " + this.agentInfo.getExecutedAction(time-1).toString());
//		if (this.agentInfo.getExecutedAction(time-1) instanceof ActionMove) {
//
//			Point position = new Point();
//			position.x = worldInfo.getLocation(this.agentInfo.me()).first(); //agentInfo.getX();
//			position.y = worldInfo.getLocation(this.agentInfo.me()).second();
//			int dis = (int)getDistance(getLastPositionXY().first(), getLastPositionXY().second(), position.x, position.y);
//			if(dis < 2000) {
//				slowMoveFlag++;
//				if (slowMoveFlag == 5){
//					lastStuckPosition = new Pair(position.x, position.y);
//					lastStuckTime = time;
//				} else if(slowMoveFlag > 5 && getDistance(position.x,position.y,lastStuckPosition.first(),lastStuckPosition.second()) > 4000) {
//					slowMoveFlag = 0;
//					return false;
//				}
//
//
//			} else {
//				slowMoveFlag = 0;
//            }
//		//非移动命令清零
//		} else {
//			slowMoveFlag = 0;
//		}
//		//5周期在同一位置
//		if(slowMoveFlag > 4) {
//			return true;
//		}
//		return false;
//	}
//
//	private double getDistance(double fromX, double fromY, double toX, double toY) {
//        double dx = fromX - toX;
//        double dy = fromY - toY;
//        return Math.hypot(dx, dy);
//    }
//
//	public Pair<Integer, Integer> getLastPositionXY() {
//		if (locations.size() == 0) {
//			return null;
//		}
//		return this.locations.get(locations.size()-1);
//	}
//
//
//}
