package test_team.module.algorithm;

import adf.agent.action.common.ActionMove;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.topdown.CommandFire;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.PathPlanning;
//import apollo.agent.pf.ClearHelperUtils;
//import apollo.viewer.layers.ApolloRoadLineLayer;
//import apollo.agent.pf.ClearHelperUtils;
//import apollo.agent.pf.ClearHelperUtils;
import rescuecore2.misc.Pair;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;
import rescuecore2.components.AbstractComponent;

import ApolloRescue.tools.*;

import java.awt.Point;
import java.util.*;

public class AStarPathPlanning extends PathPlanning {

   private Map<EntityID, Set<EntityID>> graph;

   private EntityID from;
   private Collection<EntityID> targets;
   private List<EntityID> result;
   
   private List<Pair<Integer, Integer>> locations; //locations in 5 time steps
   private int slowMoveFlag;
   private int stuckPositionX ;
   private int stuckPositionY ;
   private List<Integer> timeSteps;

   public AStarPathPlanning(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
       super(ai, wi, si, moduleManager, developData);
       this.init();
       this.locations = new LinkedList<Pair<Integer, Integer>>();
       this.timeSteps = new LinkedList<Integer>();
   }
   
   @Override
   public PathPlanning updateInfo(MessageManager messageManager){
       super.updateInfo(messageManager);
       return this;
   }

   private void init() {
       Map<EntityID, Set<EntityID>> neighbours = new LazyMap<EntityID, Set<EntityID>>() {
           @Override
           public Set<EntityID> createValue() {
               return new HashSet<>();
           }
       };
       for (Entity next : this.worldInfo) {
           if (next instanceof Area) {
               Collection<EntityID> areaNeighbours = ((Area) next).getNeighbours();
               neighbours.get(next.getID()).addAll(areaNeighbours);
           }
       }
       this.graph = neighbours;
   }

   @Override
   public List<EntityID> getResult() {
       return this.result;
   }

   @Override
   public PathPlanning setFrom(EntityID id) {
       this.from = id;
       return this;
   }

   @Override
   public PathPlanning setDestination(Collection<EntityID> targets) {
     this.targets = targets;
     return this;
   }

   @Override
   public PathPlanning precompute(PrecomputeData precomputeData) {
       super.precompute(precomputeData);
       return this;
   }

   @Override
   public PathPlanning resume(PrecomputeData precomputeData) {
       super.resume(precomputeData);
       return this;
   }

   @Override
   public PathPlanning preparate() {
       super.preparate();
       return this;
   }

   @Override
   public PathPlanning calc() {
	//System.out.println("AStarPathPlanning");
	this.setLocationHistory(agentInfo, worldInfo, moduleManager);
	this.amIMoveSlow();
       //  1
       List<EntityID> open = new LinkedList<>();
       List<EntityID> close = new LinkedList<>();
       Map<EntityID, Node> nodeMap = new HashMap<>();
    
       //  3
       open.add(this.from);
       nodeMap.put(this.from, new Node(null, this.from));
       close.clear();
    
       while (true) {
           //  4
           if (open.size() < 0) {
               this.result = null;
               return this;
           }
    
           //  5
           Node n = null;
           for (EntityID id : open) {
               Node node = nodeMap.get(id);
    
               if (n == null) {
                   n = node;
               } else if (node.estimate() < n.estimate()) {
                   n = node;
               }
           }
    
           //  6
           if (targets.contains(n.getID())) {
               //  9
               List<EntityID> path = new LinkedList<>();
               while (n != null) {
                   path.add(0, n.getID());
                   n = nodeMap.get(n.getParent());
               }
    
               this.result = path;
               return this;
           }
           open.remove(n.getID());
           close.add(n.getID());
    
           //  7
           Collection<EntityID> neighbours = this.graph.get(n.getID());
           for (EntityID neighbour : neighbours) {
               Node m = new Node(n, neighbour);
    
               if (!open.contains(neighbour) && !close.contains(neighbour)
					   && !(worldInfo.getEntity(neighbour) instanceof Blockade)
					   && !(worldInfo.getEntity(neighbour) instanceof Building && ((Building) worldInfo.getEntity(neighbour)).isOnFire())) {
                   open.add(m.getID());
                   nodeMap.put(neighbour, m);
               }
               else if (open.contains(neighbour) && m.estimate() < nodeMap.get(neighbour).estimate()) {
                   nodeMap.put(neighbour, m);
               }
               else if (!close.contains(neighbour) && m.estimate() < nodeMap.get(neighbour).estimate()) {
                   nodeMap.put(neighbour, m);
               }
           }
       }
   }
   
   public void setLocationHistory(AgentInfo ai, WorldInfo wi, ModuleManager mm) {
	   
		Pair<Integer, Integer> location = wi.getLocation(ai.me());
		int time = ai.getTime();
		if(time > 5 && this.agentInfo.getExecutedAction(time-1) instanceof ActionMove) {
			
			if(this.locations.size() != 5) {
				this.locations.add(location);
				this.timeSteps.add(time);
			} 
			if (this.locations.size() == 5) {
				this.locations.remove(0);
				this.timeSteps.remove(0);
				this.locations.add(location);
				this.timeSteps.add(time);
			}
		}
   }
   
   /**
    * 自己身边一定距离没有被挡住的,且在自身area内的随机移动点 Note：如果所有方向都被挡住，则返回null
    * @param length 移动距离
    * @return
    */
   public Point2D randomMovePoint(int length) {
	   List<Vector2D> possibleDirections = findUnblockedDirection(length);
	   possibleDirections = findInsideAreaDirection(possibleDirections, length);
	   Vector2D choosedDirection = getDirectionRandomly(possibleDirections);
	   if (choosedDirection == null) {
	   } else {
		   Point2D selfPoint = new Point2D(agentInfo.getX(),agentInfo.getY());
	   }
	   return getMovePoint(choosedDirection, length);
   }
   
   public List<Vector2D> findInsideAreaDirection(List<Vector2D> directions,
			int length) {
		Area selfArea = (Area) agentInfo.getPositionArea();
		Point2D selfPoint = new Point2D(agentInfo.getX(), agentInfo.getY());
		if (selfArea == null || selfArea.getShape() == null) {// 自己所在area为null
			return null;
		}
		if (directions == null || directions.size() == 0) {// 没有方向可选
			return null;
		}
		List<Vector2D> result = new ArrayList<Vector2D>();
		for (Vector2D direction : directions) {
			Point2D point = selfPoint
					.plus(direction.normalised().scale(length));
			Edge edge = new Edge(selfPoint, point);
			List<Edge> shape = selfArea.getEdges();
			boolean hasIntercet = false;
			for (Edge areaEdge : shape) {
				if (!areaEdge.isPassable()) {
					if (ClearHelperUtils
							.getSegmentIntercetPoint(areaEdge, edge) != null) {
						hasIntercet = true;
					}
				}
			}
			if (!hasIntercet) {
				result.add(direction);
			}
		}
		return result;
   }
   
   public Vector2D getDirectionRandomly(List<Vector2D> directions) {
		if (directions == null || directions.size() == 0) {// 没有方向
			return null;
		} else if (directions.size() == 1) {// 待选方向只有一个
			return directions.get(0);
		} else {// 待选方向不只有一个
//			agentInfo.me().
			int index = (int) (Math.random() * (directions.size() - 1));
			// System.out.println("随机数字为："+index);
//			int i 
			return directions.get(index);

		}
	}
   
   /**
    * 给定方向和移动距离，生成移动点
	* 
	* @param direction
	*            给定方向
	* @param length
	*            移动距离
	* @return 生成的移动点
	*/
   public Point2D getMovePoint(Vector2D direction, int length) {
	   if (direction == null || length <= 0) {
		   return null;
	   } else {
		   Point2D selfPoint = new Point2D(worldInfo.getLocation(this.agentInfo.me()).first(),
				   worldInfo.getLocation(this.agentInfo.me()).second());
		   return selfPoint.plus(direction.normalised().scale(length));
	   }
   }
   
   /**
	 * 返回在全方向中没有被障碍物阻挡的方向
	 * 上层调用
	 * @param length
	 *            探测长度
	 * @return 未被阻挡的方向
	 */
   public List<Vector2D> findUnblockedDirection(int length) {
	   List<Vector2D> possibleDirections = getAllDirection();
	   List<Vector2D> unblockedDirection = new ArrayList<Vector2D>();
	   
	   Point2D selfPoint = new Point2D(worldInfo.getLocation(this.agentInfo.me()).first(),
			   							   worldInfo.getLocation(this.agentInfo.me()).second());
	   for (Vector2D direction : possibleDirections) {
		   Edge edge = new Edge(selfPoint, selfPoint.plus(direction.normalised().scale(length)));
		   if (!isEdgeBlocked(edge)) {
			   unblockedDirection.add(direction);
		   }
	   }
	   return unblockedDirection;
   }
   
   /**
    * 一个Edge是否和可见障碍物相交或被包含
    * @param edge
    * @return
    */
   public boolean isEdgeBlocked(Edge edge) {
	   return blockadesDetecter(blockadeInSight(), edge);
   }
   
   /**
    * 对多个障碍物判断一个Edge是否在障碍物内或有交点
    * @param blockades
    * @param e
    * @return
    */
   private boolean blockadesDetecter(Set<Blockade> blockades, Edge e) {
	   if (blockades == null || blockades.size() == 0 || e == null) {
			return false;
		} else {
			for (Blockade b : blockades) {
				if (blockadeDetecter(b, e)) {
					return true;
				}
			}
			return false;
		}
   }
   
   /**
    * 对单个障碍物判断一条Edge是否在障碍物内或有交点
    * @param b
    * @param e
    * @return
    */
   private boolean blockadeDetecter(Blockade b, Edge e) {
	   if (b == null || b.getApexes() == null || b.getApexes().length == 0
				|| e == null) {
			return false;
		} else {
			List<Edge> blockadeEdges = ClearHelperUtils
					.getEdgesFromPoint2D(ClearHelperUtils.getPointsFromApexs(b
							.getApexes()));
			return
			// ClearHelperUtils.insideShape(blockadeEdges, e.getStart())||
			ClearHelperUtils.insideShape(blockadeEdges, e.getEnd())
					|| ClearHelperUtils.isEdgeIntercetShape(e, blockadeEdges);
		}
   }
   
   /**
    * 选取视野内的障碍物
    * @return
    */
   public Set<Blockade> blockadeInSight() {
	   return getBlockadeSeen();
   }
   
   /**
    * 获得看到的障碍物
    * @return
    */
   public Set<Blockade> getBlockadeSeen() {
	   Set<Blockade> blockadeSeen = new HashSet<Blockade>();
	   ChangeSet changeSet = this.worldInfo.getChanged();
	   for (EntityID entityID : changeSet.getChangedEntities()) {
		   if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.BLOCKADE.toString())) {
			   Blockade blockade = (Blockade) this.worldInfo.getEntity(entityID);
			   for (Property p : changeSet.getChangedProperties(entityID)) {
	
					blockade.getProperty(p.getURN()).takeValue(p);
//					propertyComponent.setPropertyTime(
//							blockade.getProperty(p.getURN()), time);
				}
				if (this.worldInfo.getEntity(blockade.getPosition()) != null) {
					Area area = (Area) this.worldInfo.getEntity(blockade.getPosition());
					if (area.getBlockades() == null) {
						area.setBlockades(new ArrayList<EntityID>());
					}
					if (!area.getBlockades().contains(blockade.getID())) {
						ArrayList<EntityID> blockades = new ArrayList<EntityID>(
								area.getBlockades());
						blockades.add(blockade.getID());
						area.setBlockades(blockades);
					}
				}
	
//				if (getEntity(blockade.getID()) == null) {
//					addEntityImpl(blockade);
//					propertyComponent.addEntityProperty(blockade, time);
//				}
	
				blockadeSeen.add(blockade);
		   }
	   }
	   return blockadeSeen;
   }
   
   /**
    * 获取全部方向的列表，以竖直向上为基础，不断顺时针旋转15度生成。
    * @return
    */
   public List<Vector2D> getAllDirection() {
		List<Vector2D> directions = new ArrayList<Vector2D>();
		Vector2D v = new Vector2D(1, 0);
		for (int angle = 0; angle < 360; angle += 15) {
			directions.add(rotClockWise(v, angle));
		}
		return directions;
   }
   /**
    * 将向量顺时针旋转一定角度
    * @param v
    * @param a
    * @return
    */
   public static Vector2D rotClockWise(Vector2D v, double a) {// 向量顺时针旋转角度a，角度制
		a = a / 180 * Math.PI;
		return new Vector2D(v.getX() * Math.cos(a) + v.getY() * Math.sin(a),
				-v.getX() * Math.sin(a) + v.getY() * Math.cos(a));
	}
   
   
   public enum StuckState {
	    TOTALLY_BLOCKED,
	    BLOCKED_IN_REGION,
	    FREE,

	}
	/**
	 * 判断是否在一个范围内移动 5周期
	 * 
	 *            移动距离
	 * @return
	 */
	public boolean amIMoveSlow() {
		int time = agentInfo.getTime();
		if (time < 4) {
			return false;
		}
		if (this.locations.isEmpty()) {
			slowMoveFlag = 0;
			return false;
		}
		//前提是上周发送的是移动命令
		//System.out.println("Time:" + this.agentInfo.getTime() + "\tAgent:(" + this.agentInfo.getID() + ") \t Last time Action type: " + this.agentInfo.getExecutedAction(time-1).toString());
		if (this.agentInfo.getExecutedAction(time-1) instanceof ActionMove) {
			
			Point position = new Point();
			position.x = worldInfo.getLocation(this.agentInfo.me()).first(); //agentInfo.getX();
			position.y = worldInfo.getLocation(this.agentInfo.me()).second();
			int dis = (int)getDistance(getLastPositionXY().first(), getLastPositionXY().second(), position.x, position.y);
			if(dis < 2000) {
				slowMoveFlag++;
				if (slowMoveFlag == 5){
					stuckPositionX = position.x;
					stuckPositionY = position.y;
				} else if(slowMoveFlag > 5 && getDistance(position.x,position.y,stuckPositionX,stuckPositionY) > 4000) {
					slowMoveFlag = 0;
					return false;
				}
                                

			} else {
				slowMoveFlag = 0;
            }
		//非移动命令清零
		} else {
			slowMoveFlag = 0;
		}
		//5周期在同一位置
		if(slowMoveFlag > 4) {
			return true;
		}
		return false;
	}
	
	private double getDistance(double fromX, double fromY, double toX, double toY) {
        double dx = fromX - toX;
        double dy = fromY - toY;
        return Math.hypot(dx, dy);
    }
	
	public Pair<Integer, Integer> getLastPositionXY() {
		if (locations.size() == 0) {
			return null;
		}
		return this.locations.get(locations.size()-1);
	}


   private class Node {
	    EntityID id;
	    EntityID parent;
	 
	    double cost;
	    double heuristic;
	 
	    public Node(Node from, EntityID id) {
	        this.id = id;
	 
	        if (from == null) {
	            this.cost = 0;
	        } else {
	            this.parent = from.getID();
	            this.cost = from.getCost() + worldInfo.getDistance(from.getID(), id);
	        }
	 
	        this.heuristic = worldInfo.getDistance(id, targets.toArray(new EntityID[targets.size()])[0]);
	    }
	 
	    public EntityID getID() {
	        return id;
	    }
	 
	    public double getCost() {
	        return cost;
	    }
	 
	    public double estimate() {
	        return cost + heuristic;
	    }
	 
	    public EntityID getParent() {
	        return this.parent;
	    }
	}
}

