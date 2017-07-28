package ApolloRescue.module.algorithm;

import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.StaticClustering;
import rescuecore2.misc.Pair;
//import apollo.agent.universal.ApolloConstants;
//import apollo.tools.MapTypes;
//import apollo.tools.debug.ConsoleDebug;
//import apollo.tools.debug.ConsoleDebug.OutputType;
//import rescuecore2.misc.Pair;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

//import static java.util.Comparator.comparing;
//import static java.util.Comparator.reverseOrder;

public class ApolloKMeans extends StaticClustering {
    private static final String KEY_CLUSTER_SIZE = "Apollo.clustering.size";
    private static final String KEY_CLUSTER_CENTER = "Apollo.clustering.centers";
    private static final String KEY_CLUSTER_ENTITY = "Apollo.clustering.entities.";
    private static final String KEY_ASSIGN_AGENT = "Apollo.clustering.assign";

    private int repeatPrecompute;
    private int repeatPreparate;

    private Collection<StandardEntity> entities;

    private List<StandardEntity> centerList;
    private List<EntityID> centerIDs;
    private Map<Integer, List<StandardEntity>> clusterEntitiesList;
    private List<List<EntityID>> clusterEntityIDsList;
    
    private List<Building> allBuildings;
    private List<Road> allRoads;

    private int clusterSize;

    private boolean assignAgentsFlag;

    private Map<EntityID, Set<EntityID>> shortestPathGraph;

    public ApolloKMeans(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.repeatPrecompute = developData.getInteger("Apollo.module.ApolloKMeans.repeatPrecompute", 7);
        this.repeatPreparate = developData.getInteger("Apollo.module.ApolloKMeans.repeatPreparate", 30);
        this.allBuildings = new ArrayList<>();
        this.allRoads = new ArrayList<>();
        this.clusterSize = developData.getInteger("Apollo.module.ApolloKMeans.clusterSize", this.calcPartNumUseMapInfo(worldInfo));
        this.assignAgentsFlag = developData.getBoolean("Apollo.module.ApolloKMeans.assignAgentsFlag", true);
        this.clusterEntityIDsList = new ArrayList<>();
        this.centerIDs = new ArrayList<>();
        this.clusterEntitiesList = new HashMap<>();
        this.centerList = new ArrayList<>();
        this.entities = wi.getEntitiesOfType(
                StandardEntityURN.ROAD,
                StandardEntityURN.HYDRANT,
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.GAS_STATION,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.POLICE_OFFICE
        );
    }
    
//    private int getPartitionNumber() {
//    	int num = 10;
//    	
//    	return num;
//    }
    
    /**
     * 利用地图信息计算分区数量
     * @param world
     * @return
     * @author Yangjiedong
     */
    private int calcPartNumUseMapInfo(WorldInfo world) {
    	int num = 10;
    	int agentNum = this.getAgentNumber();
    	for (StandardEntity entity : world.getAllEntities()) {
			if (entity instanceof Refuge) {
				allBuildings.add((Building) entity);
			}
			if (entity instanceof Building) {
				Building b = (Building) entity;
				allBuildings.add(b);
			}
			if (entity instanceof Road) {
				Road r = (Road) entity;
				allRoads.add(r);
			}

		}
    	Pair<Integer, Integer> topLeft = world.getWorldBounds().first();
    	Pair<Integer, Integer> bottomRight = world.getWorldBounds().second();
		int mapWidth = bottomRight.first() - topLeft.first();
		int mapHeight = bottomRight.second() - bottomRight.second();
		
		//TODO 地图与分区数目关系判定 
		// mapWidth & mapHeight 的误差设为20000
		//medium    mapTypes = MapTypes.LD;
		if((973 <= allBuildings.size() && allBuildings.size() <= 993 && 2995 <= allRoads.size() && allRoads.size() <= 3015) //added  2016 08 24
				|| (999086 <= mapWidth && mapWidth <= 1039086 && 922802 <= mapHeight && mapHeight <= 962802))
			if (agentNum > 25 && this.agentInfo.me().getStandardURN() != StandardEntityURN.AMBULANCE_TEAM) {
				num = agentNum / 2; 
			} else {
				num = agentNum;
			}
		
		//small    mapTypes = MapTypes.VC;
		else if ((1253 <= allBuildings.size() && allBuildings.size() <= 1273 && 1944 <= allRoads.size() && allRoads.size() <= 1964)
				|| (413100 <= mapWidth && mapWidth <= 453100 && 422800 <= mapHeight && mapHeight <= 462800))
			if (agentNum > 25 && this.agentInfo.me().getStandardURN() != StandardEntityURN.AMBULANCE_TEAM) {
				num = agentNum / 2; 
			} else {
				num = agentNum;
			}
		
		//small    mapTypes = MapTypes.Kobe;
		else if ((747 <= allBuildings.size() && allBuildings.size() <= 767 && 1592 <= allRoads.size() && allRoads.size() <= 1612)
				|| (448520 <= mapWidth && mapWidth <= 488520 && 344080 <= mapHeight && mapHeight <= 384080))
			if (agentNum > 20 && this.agentInfo.me().getStandardURN() != StandardEntityURN.AMBULANCE_TEAM) {
				num = agentNum / 2; 
			} else {
				num = agentNum;
			}
		
		//small but condense   mapTypes = MapTypes.Mexico;
		else if ((1557 <= allBuildings.size() && allBuildings.size() <= 1577 && 5098 <= allRoads.size() && allRoads.size() <= 5118)
				|| (609391 <= mapWidth && mapWidth <= 649391 && 651489 <= mapHeight && mapHeight <= 691489))
			if (agentNum > 36 && this.agentInfo.me().getStandardURN() != StandardEntityURN.AMBULANCE_TEAM) {
				num = agentNum / 2; 
			} else {
				num = agentNum;
			}

		//medium   mapTypes = MapTypes.Paris;
		else if ((1608 <= allBuildings.size() && allBuildings.size() <= 1628 && 3015 <= allRoads.size() && allRoads.size() <= 3035)
				|| (936653 <= mapWidth && mapWidth <= 976653 && 976981 <= mapHeight && mapHeight <= 1016981))
			if (agentNum > 30 && this.agentInfo.me().getStandardURN() != StandardEntityURN.AMBULANCE_TEAM) {
				num = agentNum / 2; 
			} else {
				num = agentNum;
			}
		
		
		//medium   mapTypes = MapTypes.Istanbul;
		else if ((1234 <= allBuildings.size() && allBuildings.size() <= 1254 && 3327 <= allRoads.size() && allRoads.size() <= 3347)
				|| (1279638 <= mapWidth && mapWidth <= 1319638 && 959313 <= mapHeight && mapHeight <= 999313))
			if (agentNum > 30 && this.agentInfo.me().getStandardURN() != StandardEntityURN.AMBULANCE_TEAM) {
				num = agentNum / 2; 
			} else {
				num = agentNum;
			}
		
		//large    mapTypes = MapTypes.Berlin;
		else if ((1416 <= allBuildings.size() && allBuildings.size() <= 1436 && 3375 <= allRoads.size() && allRoads.size() <= 3395)
				|| (2167484 <= mapWidth && mapWidth <= 2207484 && 1617291 <= mapHeight && mapHeight <= 1657291))
			if (agentNum > 40 && this.agentInfo.me().getStandardURN() != StandardEntityURN.AMBULANCE_TEAM) {
				num = agentNum / 2; 
			} else {
				num = agentNum;
			}
	
		//large   mapTypes = MapTypes.NY;
		else if ((2901 <= allBuildings.size() && allBuildings.size() <= 2921 && 7729 <= allRoads.size() && allRoads.size() <= 7749)
				|| (1914114 <= mapWidth && mapWidth <= 1954114 && 1507788 <= mapHeight && mapHeight <= 1547788))
			if (agentNum > 40 && this.agentInfo.me().getStandardURN() != StandardEntityURN.AMBULANCE_TEAM) {
				num = agentNum / 2; 
			} else {
				num = agentNum;
			}
			
		//large and scarce   mapTypes = MapTypes.Joao;
		else if ((873 <= allBuildings.size() && allBuildings.size() <= 893 && 3457 <= allRoads.size() && allRoads.size() <= 3477)
				|| (1639464 <= mapWidth && mapWidth <= 1679464 && 1407167 <= mapHeight && mapHeight <= 1447167))
			if (agentNum > 30 && this.agentInfo.me().getStandardURN() != StandardEntityURN.AMBULANCE_TEAM) {
				num = agentNum / 2; 
			} else {
				num = agentNum;
			}
		
		//large mapTypes = MapTypes.Eindhoven;
		else if ((1298 <= allBuildings.size() && allBuildings.size() <= 1318 && 5162 <= allRoads.size() && allRoads.size() <= 5182)
				|| (2058918 <= mapWidth && mapWidth <= 2098918 && 1736160 <= mapHeight && mapHeight <= 1776160))
			if (agentNum > 30 && this.agentInfo.me().getStandardURN() != StandardEntityURN.AMBULANCE_TEAM) {
				num = agentNum / 2; 
			} else {
				num = agentNum;
			}
		//unknown type
		else {
			//small
			if (mapWidth <= 700000 && mapHeight <= 700000) {
				if (agentNum > 25 && this.agentInfo.me().getStandardURN() != StandardEntityURN.AMBULANCE_TEAM) {
					num = agentNum / 2; 
				} else {
					num = agentNum;
				}
			} else if (mapWidth <= 1400000 && mapHeight <= 1400000) { //medium
				if (agentNum > 36 && this.agentInfo.me().getStandardURN() != StandardEntityURN.AMBULANCE_TEAM) {
					num = agentNum / 2; 
				} else {
					num = agentNum;
				}
			} else { // large 
				if (agentNum > 40 && this.agentInfo.me().getStandardURN() != StandardEntityURN.AMBULANCE_TEAM) {
					num = agentNum / 2; 
				} else {
					num = agentNum;
				}
			}
		}
		return num;
    }
    
    private int getAgentNumber() {
    	List<StandardEntity> agentList = new ArrayList<>(this.worldInfo.getEntitiesOfType(this.agentInfo.me().getStandardURN()));
    	return agentList.size();
    }

    @Override
    public Clustering updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if(this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.centerList.clear();
        this.clusterEntitiesList.clear();
        return this;
    }

    @Override
    public Clustering precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if(this.getCountPrecompute() >= 2) {
            return this;
        }
        this.calcPathBased(this.repeatPrecompute);
        this.entities = null;
        // write
        precomputeData.setInteger(KEY_CLUSTER_SIZE, this.clusterSize);
        precomputeData.setEntityIDList(KEY_CLUSTER_CENTER, this.centerIDs);
        for(int i = 0; i < this.clusterSize; i++) {
            precomputeData.setEntityIDList(KEY_CLUSTER_ENTITY + i, this.clusterEntityIDsList.get(i));
        }
        precomputeData.setBoolean(KEY_ASSIGN_AGENT, this.assignAgentsFlag);
        return this;
    }

    @Override
    public Clustering resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if(this.getCountResume() >= 2) {
            return this;
        }
        this.entities = null;
        // read
        this.clusterSize = precomputeData.getInteger(KEY_CLUSTER_SIZE);
        this.centerIDs = new ArrayList<>(precomputeData.getEntityIDList(KEY_CLUSTER_CENTER));
        this.clusterEntityIDsList = new ArrayList<>(this.clusterSize);
        for(int i = 0; i < this.clusterSize; i++) {
            this.clusterEntityIDsList.add(i, precomputeData.getEntityIDList(KEY_CLUSTER_ENTITY + i));
        }
        this.assignAgentsFlag = precomputeData.getBoolean(KEY_ASSIGN_AGENT);
        return this;
    }

    @Override
    public Clustering preparate() {
        super.preparate();
        if(this.getCountPreparate() >= 2) {
            return this;
        }
        this.calcStandard(this.repeatPreparate);
        this.entities = null;
        return this;
    }

    @Override
    public int getClusterNumber() {
        //The number of clusters
        return this.clusterSize;
    }

    @Override
    public int getClusterIndex(StandardEntity entity) {
        return this.getClusterIndex(entity.getID());
    }

    @Override
    public int getClusterIndex(EntityID id) {
        for(int i = 0; i < this.clusterSize; i++) {
            if(this.clusterEntityIDsList.get(i).contains(id)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Collection<StandardEntity> getClusterEntities(int index) {
        List<StandardEntity> result = this.clusterEntitiesList.get(index);
        if(result == null || result.isEmpty()) {
            List<EntityID> list = this.clusterEntityIDsList.get(index);
            result = new ArrayList<>(list.size());
            for(int i = 0; i < list.size(); i++) {
                result.add(i, this.worldInfo.getEntity(list.get(i)));
            }
            this.clusterEntitiesList.put(index, result);
        }
        return result;
    }

    @Override
    public Collection<EntityID> getClusterEntityIDs(int index) {
        return this.clusterEntityIDsList.get(index);
    }

    @Override
    public Clustering calc() {
        return this;
    }

    private void calcStandard(int repeat) {
        this.initShortestPath(this.worldInfo);
        Random random = new Random();

        List<StandardEntity> entityList = new ArrayList<>(this.entities);
        this.centerList = new ArrayList<>(this.clusterSize);
        this.clusterEntitiesList = new HashMap<>(this.clusterSize);

        //init list
        for (int index = 0; index < this.clusterSize; index++) {
            this.clusterEntitiesList.put(index, new ArrayList<>());
            this.centerList.add(index, entityList.get(0));
        }
        System.out.println("[" + this.getClass().getSimpleName() + "] Cluster : " + this.clusterSize);
        //init center
        for (int index = 0; index < this.clusterSize; index++) {
            StandardEntity centerEntity;
            do {
                centerEntity = entityList.get(Math.abs(random.nextInt()) % entityList.size());
            } while (this.centerList.contains(centerEntity));
            this.centerList.set(index, centerEntity);
        }
        //calc center
        for (int i = 0; i < repeat; i++) {
            this.clusterEntitiesList.clear();
            for (int index = 0; index < this.clusterSize; index++) {
                this.clusterEntitiesList.put(index, new ArrayList<>());
            }
            for (StandardEntity entity : entityList) {
                StandardEntity tmp = this.getNearEntityByLine(this.worldInfo, this.centerList, entity);
                this.clusterEntitiesList.get(this.centerList.indexOf(tmp)).add(entity);
            }
            for (int index = 0; index < this.clusterSize; index++) {
                int sumX = 0, sumY = 0;
                for (StandardEntity entity : this.clusterEntitiesList.get(index)) {
                    Pair<Integer, Integer> location = this.worldInfo.getLocation(entity);
                    sumX += location.first();
                    sumY += location.second();
                }
                int centerX = sumX / this.clusterEntitiesList.get(index).size();
                int centerY = sumY / this.clusterEntitiesList.get(index).size();
                StandardEntity center = this.getNearEntityByLine(this.worldInfo, this.clusterEntitiesList.get(index), centerX, centerY);
                if(center instanceof Area) {
                    this.centerList.set(index, center);
                }
                else if(center instanceof Human) {
                    this.centerList.set(index, this.worldInfo.getEntity(((Human) center).getPosition()));
                }
                else if(center instanceof Blockade) {
                    this.centerList.set(index, this.worldInfo.getEntity(((Blockade) center).getPosition()));
                }
            }
            if  (scenarioInfo.isDebugMode()) { System.out.print("*"); }
        }

        if  (scenarioInfo.isDebugMode()) { System.out.println(); }

        //set entity
        this.clusterEntitiesList.clear();
        for (int index = 0; index < this.clusterSize; index++) {
            this.clusterEntitiesList.put(index, new ArrayList<>());
        }
        for (StandardEntity entity : entityList) {
            StandardEntity tmp = this.getNearEntityByLine(this.worldInfo, this.centerList, entity);
            this.clusterEntitiesList.get(this.centerList.indexOf(tmp)).add(entity);
        }

        //this.clusterEntitiesList.sort(comparing(List::size, reverseOrder()));

        if(this.assignAgentsFlag) {
            List<StandardEntity> firebrigadeList = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));
            List<StandardEntity> policeforceList = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE));
            List<StandardEntity> ambulanceteamList = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM));

            this.assignAgents(this.worldInfo, firebrigadeList);
            this.assignAgents(this.worldInfo, policeforceList);
            this.assignAgents(this.worldInfo, ambulanceteamList);
        }

        this.centerIDs = new ArrayList<>();
        for(int i = 0; i < this.centerList.size(); i++) {
            this.centerIDs.add(i, this.centerList.get(i).getID());
        }
        for (int index = 0; index < this.clusterSize; index++) {
            List<StandardEntity> entities = this.clusterEntitiesList.get(index);
            List<EntityID> list = new ArrayList<>(entities.size());
            for(int i = 0; i < entities.size(); i++) {
                list.add(i, entities.get(i).getID());
            }
            this.clusterEntityIDsList.add(index, list);
        }
    }

    private void calcPathBased(int repeat) {
        this.initShortestPath(this.worldInfo);
        Random random = new Random();
        List<StandardEntity> entityList = new ArrayList<>(this.entities);
        this.centerList = new ArrayList<>(this.clusterSize);
        this.clusterEntitiesList = new HashMap<>(this.clusterSize);

        for (int index = 0; index < this.clusterSize; index++) {
            this.clusterEntitiesList.put(index, new ArrayList<>());
            this.centerList.add(index, entityList.get(0));
        }
        for (int index = 0; index < this.clusterSize; index++) {
            StandardEntity centerEntity;
            do {
                centerEntity = entityList.get(Math.abs(random.nextInt()) % entityList.size());
            } while (this.centerList.contains(centerEntity));
            this.centerList.set(index, centerEntity);
        }
        for (int i = 0; i < repeat; i++) {
            this.clusterEntitiesList.clear();
            for (int index = 0; index < this.clusterSize; index++) {
                this.clusterEntitiesList.put(index, new ArrayList<>());
            }
            for (StandardEntity entity : entityList) {
                StandardEntity tmp = this.getNearEntity(this.worldInfo, this.centerList, entity);
                this.clusterEntitiesList.get(this.centerList.indexOf(tmp)).add(entity);
            }
            for (int index = 0; index < this.clusterSize; index++) {
                int sumX = 0, sumY = 0;
                for (StandardEntity entity : this.clusterEntitiesList.get(index)) {
                    Pair<Integer, Integer> location = this.worldInfo.getLocation(entity);
                    sumX += location.first();
                    sumY += location.second();
                }
                int centerX = sumX / clusterEntitiesList.get(index).size();
                int centerY = sumY / clusterEntitiesList.get(index).size();

                //this.centerList.set(index, getNearEntity(this.worldInfo, this.clusterEntitiesList.get(index), centerX, centerY));
                StandardEntity center = this.getNearEntity(this.worldInfo, this.clusterEntitiesList.get(index), centerX, centerY);
                if (center instanceof Area) {
                    this.centerList.set(index, center);
                } else if (center instanceof Human) {
                    this.centerList.set(index, this.worldInfo.getEntity(((Human) center).getPosition()));
                } else if (center instanceof Blockade) {
                    this.centerList.set(index, this.worldInfo.getEntity(((Blockade) center).getPosition()));
                }
            }
            if  (scenarioInfo.isDebugMode()) { System.out.print("*"); }
        }

        if  (scenarioInfo.isDebugMode()) { System.out.println(); }

        this.clusterEntitiesList.clear();
        for (int index = 0; index < this.clusterSize; index++) {
            this.clusterEntitiesList.put(index, new ArrayList<>());
        }
        for (StandardEntity entity : entityList) {
            StandardEntity tmp = this.getNearEntity(this.worldInfo, this.centerList, entity);
            this.clusterEntitiesList.get(this.centerList.indexOf(tmp)).add(entity);
        }
        //this.clusterEntitiesList.sort(comparing(List::size, reverseOrder()));
        if (this.assignAgentsFlag) {
            List<StandardEntity> fireBrigadeList = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));
            List<StandardEntity> policeForceList = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE));
            List<StandardEntity> ambulanceTeamList = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM));
            this.assignAgents(this.worldInfo, fireBrigadeList);
            this.assignAgents(this.worldInfo, policeForceList);
            this.assignAgents(this.worldInfo, ambulanceTeamList);
        }

        this.centerIDs = new ArrayList<>();
        for(int i = 0; i < this.centerList.size(); i++) {
            this.centerIDs.add(i, this.centerList.get(i).getID());
        }
        for (int index = 0; index < this.clusterSize; index++) {
            List<StandardEntity> entities = this.clusterEntitiesList.get(index);
            List<EntityID> list = new ArrayList<>(entities.size());
            for(int i = 0; i < entities.size(); i++) {
                list.add(i, entities.get(i).getID());
            }
            this.clusterEntityIDsList.add(index, list);
        }
    }

    private void assignAgents(WorldInfo world, List<StandardEntity> agentList) {
        int clusterIndex = 0;
        while (agentList.size() > 0) {
            StandardEntity center = this.centerList.get(clusterIndex);
            StandardEntity agent = this.getNearAgent(world, agentList, center);
            this.clusterEntitiesList.get(clusterIndex).add(agent);
            agentList.remove(agent);
            clusterIndex++;
            if (clusterIndex >= this.clusterSize) {
                clusterIndex = 0;
            }
        }
    }

    private StandardEntity getNearEntityByLine(WorldInfo world, List<StandardEntity> srcEntityList, StandardEntity targetEntity) {
        Pair<Integer, Integer> location = world.getLocation(targetEntity);
        return this.getNearEntityByLine(world, srcEntityList, location.first(), location.second());
    }

    private StandardEntity getNearEntityByLine(WorldInfo world, List<StandardEntity> srcEntityList, int targetX, int targetY) {
        StandardEntity result = null;
        for(StandardEntity entity : srcEntityList) {
            result = ((result != null) ? this.compareLineDistance(world, targetX, targetY, result, entity) : entity);
        }
        return result;
    }

    private StandardEntity getNearAgent(WorldInfo worldInfo, List<StandardEntity> srcAgentList, StandardEntity targetEntity) {
        StandardEntity result = null;
        for (StandardEntity agent : srcAgentList) {
            Human human = (Human)agent;
            if (result == null) {
                result = agent;
            }
            else {
                if (this.comparePathDistance(worldInfo, targetEntity, result, worldInfo.getPosition(human)).equals(worldInfo.getPosition(human))) {
                    result = agent;
                }
            }
        }
        return result;
    }

    private StandardEntity getNearEntity(WorldInfo worldInfo, List<StandardEntity> srcEntityList, int targetX, int targetY) {
        StandardEntity result = null;
        for (StandardEntity entity : srcEntityList) {
            result = (result != null) ? this.compareLineDistance(worldInfo, targetX, targetY, result, entity) : entity;
        }
        return result;
    }

    private Point2D getEdgePoint(Edge edge) {
        Point2D start = edge.getStart();
        Point2D end = edge.getEnd();
        return new Point2D(((start.getX() + end.getX()) / 2.0D), ((start.getY() + end.getY()) / 2.0D));
    }


    private double getDistance(double fromX, double fromY, double toX, double toY) {
        double dx = fromX - toX;
        double dy = fromY - toY;
        return Math.hypot(dx, dy);
    }

    private double getDistance(Pair<Integer, Integer> from, Point2D to) {
        return getDistance(from.first(), from.second(), to.getX(), to.getY());
    }

    private double getDistance(Pair<Integer, Integer> from, Edge to) {
        return getDistance(from, getEdgePoint(to));
    }

    private double getDistance(Point2D from, Point2D to) {
        return getDistance(from.getX(), from.getY(), to.getX(), to.getY());
    }

    private double getDistance(Edge from, Edge to) {
        return getDistance(getEdgePoint(from), getEdgePoint(to));
    }

    private StandardEntity compareLineDistance(WorldInfo worldInfo, int targetX, int targetY, StandardEntity first, StandardEntity second) {
        Pair<Integer, Integer> firstLocation = worldInfo.getLocation(first);
        Pair<Integer, Integer> secondLocation = worldInfo.getLocation(second);
        double firstDistance = getDistance(firstLocation.first(), firstLocation.second(), targetX, targetY);
        double secondDistance = getDistance(secondLocation.first(), secondLocation.second(), targetX, targetY);
        return (firstDistance < secondDistance ? first : second);
    }

    private StandardEntity getNearEntity(WorldInfo worldInfo, List<StandardEntity> srcEntityList, StandardEntity targetEntity) {
        StandardEntity result = null;
        for (StandardEntity entity : srcEntityList) {
            result = (result != null) ? this.comparePathDistance(worldInfo, targetEntity, result, entity) : entity;
        }
        return result;
    }

    private StandardEntity comparePathDistance(WorldInfo worldInfo, StandardEntity target, StandardEntity first, StandardEntity second) {
        double firstDistance = getPathDistance(worldInfo, shortestPath(target.getID(), first.getID()));
        double secondDistance = getPathDistance(worldInfo, shortestPath(target.getID(), second.getID()));
        return (firstDistance < secondDistance ? first : second);
    }

    private double getPathDistance(WorldInfo worldInfo, List<EntityID> path) {
        if (path == null) return Double.MAX_VALUE;
        if (path.size() <= 1) return 0.0D;

        double distance = 0.0D;
        int limit = path.size() - 1;

        Area area = (Area)worldInfo.getEntity(path.get(0));
        distance += getDistance(worldInfo.getLocation(area), area.getEdgeTo(path.get(1)));
        area = (Area)worldInfo.getEntity(path.get(limit));
        distance += getDistance(worldInfo.getLocation(area), area.getEdgeTo(path.get(limit - 1)));

        for(int i = 1; i < limit; i++) {
            area = (Area)worldInfo.getEntity(path.get(i));
            distance += getDistance(area.getEdgeTo(path.get(i - 1)), area.getEdgeTo(path.get(i + 1)));
        }
        return distance;
    }

    private void initShortestPath(WorldInfo worldInfo) {
        Map<EntityID, Set<EntityID>> neighbours = new LazyMap<EntityID, Set<EntityID>>() {
            @Override
            public Set<EntityID> createValue() {
                return new HashSet<>();
            }
        };
        for (Entity next : worldInfo) {
            if (next instanceof Area) {
                Collection<EntityID> areaNeighbours = ((Area) next).getNeighbours();
                neighbours.get(next.getID()).addAll(areaNeighbours);
            }
        }
        for (Map.Entry<EntityID, Set<EntityID>> graph : neighbours.entrySet()) {// fix graph
            for (EntityID entityID : graph.getValue()) {
                neighbours.get(entityID).add(graph.getKey());
            }
        }
        this.shortestPathGraph = neighbours;
    }

    private List<EntityID> shortestPath(EntityID start, EntityID... goals) {
        return shortestPath(start, Arrays.asList(goals));
    }

    private List<EntityID> shortestPath(EntityID start, Collection<EntityID> goals) {
        List<EntityID> open = new LinkedList<>();
        Map<EntityID, EntityID> ancestors = new HashMap<>();
        open.add(start);
        EntityID next;
        boolean found = false;
        ancestors.put(start, start);
        do {
            next = open.remove(0);
            if (isGoal(next, goals)) {
                found = true;
                break;
            }
            Collection<EntityID> neighbours = shortestPathGraph.get(next);
            if (neighbours.isEmpty()) continue;

            for (EntityID neighbour : neighbours) {
                if (isGoal(neighbour, goals)) {
                    ancestors.put(neighbour, next);
                    next = neighbour;
                    found = true;
                    break;
                }
                else if (!ancestors.containsKey(neighbour)) {
                    open.add(neighbour);
                    ancestors.put(neighbour, next);
                }
            }
        } while (!found && !open.isEmpty());
        if (!found) {
            // No path
            return null;
        }
        // Walk back from goal to start
        EntityID current = next;
        List<EntityID> path = new LinkedList<>();
        do {
            path.add(0, current);
            current = ancestors.get(current);
            if (current == null) throw new RuntimeException("Found a node with no ancestor! Something is broken.");
        } while (current != start);
        return path;
    }

    private boolean isGoal(EntityID e, Collection<EntityID> test) {
        return test.contains(e);
    }
}
