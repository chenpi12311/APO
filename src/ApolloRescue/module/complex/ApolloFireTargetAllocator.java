package ApolloRescue.module.complex;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.centralized.CommandFire;
import adf.agent.communication.standard.bundle.centralized.MessageReport;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.communication.standard.bundle.information.MessageFireBrigade;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.complex.FireTargetAllocator;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import ApolloRescue.tools.*;
import ApolloRescue.tools.convexhull.*;

import java.util.*;

import ApolloRescue.module.algorithm.ApolloFireClustering;

public class ApolloFireTargetAllocator extends FireTargetAllocator {

    private Collection<EntityID> priorityBuildings;
    private Collection<EntityID> targetBuildings;

    private Map<EntityID, FireBrigadeInfo> agentInfoMap;

    private int maxWater;
    private int maxPower;
    
    /* Multi-Fire Situation */
    List<Building> outerBuildings; // building with fieryness one 
    List<Building> gapArea; // all building with fietyness 0
    
    List<ApolloFireClustering> fireZoneList; //所有火区
    
    List<List<EntityID>> gapPartitionList;   //所有空白分区
    List<Integer> gapCenterXList;
    List<Integer> gapCenterYList;
    List<Pair<Integer, Integer>> gapCenterList;
    
    
    List<Building> multiFireTargetList; //result target list
    List<Building> buildingList;

    public ApolloFireTargetAllocator(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.priorityBuildings = new HashSet<>();
        this.targetBuildings = new HashSet<>();
        this.agentInfoMap = new HashMap<>();
        
        this.fireZoneList = new ArrayList<ApolloFireClustering>();
        this.gapPartitionList = new ArrayList<List<EntityID>>();
        this.gapCenterXList = new ArrayList<Integer>();
        this.gapCenterYList = new ArrayList<Integer>();
        this.multiFireTargetList = new ArrayList<Building>();
        this.outerBuildings = new ArrayList<Building>();
        this.gapArea = new ArrayList<Building>();
        this.buildingList = new ArrayList<Building>();
        
        this.maxWater = si.getFireTankMaximum();
        this.maxPower = si.getFireExtinguishMaxSum();
        
    }

    @Override
    public FireTargetAllocator resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        for (EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.FIRE_BRIGADE)) {
            this.agentInfoMap.put(id, new FireBrigadeInfo(id));
        }
        return this;
    }

    @Override
    public FireTargetAllocator preparate() {
        super.preparate();
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        for (EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.FIRE_BRIGADE)) {
            this.agentInfoMap.put(id, new FireBrigadeInfo(id));
        }
        return this;
    }

    @Override
    public Map<EntityID, EntityID> getResult() {
        return this.convert(this.agentInfoMap);
    }

    @Override
    public FireTargetAllocator calc() {
        int currentTime = this.agentInfo.getTime();
        List<StandardEntity> agents = this.getActionAgents(this.agentInfoMap);
        Collection<EntityID> removes = new ArrayList<>();
        for (EntityID target : this.priorityBuildings) {
            if (agents.size() > 0) {
                StandardEntity targetEntity = this.worldInfo.getEntity(target);
                if (targetEntity != null) {
                    agents.sort(new DistanceSorter(this.worldInfo, targetEntity));
                    StandardEntity result = agents.get(0);
                    agents.remove(0);
                    FireBrigadeInfo info = this.agentInfoMap.get(result.getID());
                    if (info != null) {
                        info.canNewAction = false;
                        info.target = target;
                        info.commandTime = currentTime;
                        this.agentInfoMap.put(result.getID(), info);
                        removes.add(target);
                    }
                }
            }
        }
        this.priorityBuildings.removeAll(removes);
        removes.clear();
        for (EntityID target : this.targetBuildings) {
            if (agents.size() > 0) {
                StandardEntity targetEntity = this.worldInfo.getEntity(target);
                if (targetEntity != null) {
                    agents.sort(new DistanceSorter(this.worldInfo, targetEntity));
                    StandardEntity result = agents.get(0);
                    agents.remove(0);
                    FireBrigadeInfo info = this.agentInfoMap.get(result.getID());
                    if (info != null) {
                        info.canNewAction = false;
                        info.target = target;
                        info.commandTime = currentTime;
                        this.agentInfoMap.put(result.getID(), info);
                        removes.add(target);
                    }
                }
            }
        }
        this.targetBuildings.removeAll(removes);
        return this;
    }

    @Override
    public FireTargetAllocator updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        int currentTime = this.agentInfo.getTime();
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageBuilding.class)) {
            MessageBuilding mb = (MessageBuilding) message;
            Building building = MessageUtil.reflectMessage(this.worldInfo, mb);
            if (building.isOnFire()) {
                this.targetBuildings.add(building.getID());
            } else {
                this.priorityBuildings.remove(mb.getBuildingID());
                this.targetBuildings.remove(mb.getBuildingID());
            }
        }
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageFireBrigade.class)) {
            MessageFireBrigade mfb = (MessageFireBrigade) message;
            MessageUtil.reflectMessage(this.worldInfo, mfb);
            FireBrigadeInfo info = this.agentInfoMap.get(mfb.getAgentID());
            if (info == null) {
                info = new FireBrigadeInfo(mfb.getAgentID());
            }
            if (currentTime >= info.commandTime + 2) {
                this.agentInfoMap.put(mfb.getAgentID(), this.update(info, mfb));
            }
        }
        for (CommunicationMessage message : messageManager.getReceivedMessageList(CommandFire.class)) {
            CommandFire command = (CommandFire) message;
            if (command.getAction() == CommandFire.ACTION_EXTINGUISH && command.isBroadcast()) {
                this.priorityBuildings.add(command.getTargetID());
                this.targetBuildings.add(command.getTargetID());
            }
        }
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageReport.class)) {
            MessageReport report = (MessageReport) message;
            FireBrigadeInfo info = this.agentInfoMap.get(report.getSenderID());
            if (info != null && report.isDone()) {
                info.canNewAction = true;
                this.priorityBuildings.remove(info.target);
                this.targetBuildings.remove(info.target);
                info.target = null;
                this.agentInfoMap.put(report.getSenderID(), info);
            }
        }
        return this;
    }

    private Map<EntityID, EntityID> convert(Map<EntityID, FireBrigadeInfo> infoMap) {
        Map<EntityID, EntityID> result = new HashMap<>();
        for (EntityID id : infoMap.keySet()) {
            FireBrigadeInfo info = infoMap.get(id);
            if (info != null && info.target != null) {
                result.put(id, info.target);
            }
        }
        return result;
    }

    private List<StandardEntity> getActionAgents(Map<EntityID, FireBrigadeInfo> infoMap) {
        List<StandardEntity> result = new ArrayList<>();
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE)) {
            FireBrigadeInfo info = infoMap.get(entity.getID());
            if (info != null && info.canNewAction && ((FireBrigade) entity).isPositionDefined()) {
                result.add(entity);
            }
        }
        return result;
    }

    private FireBrigadeInfo update(FireBrigadeInfo info, MessageFireBrigade message) {
        if (message.isBuriednessDefined() && message.getBuriedness() > 0) {
            info.canNewAction = false;
            if (info.target != null) {
                this.targetBuildings.add(info.target);
                info.target = null;
            }
            return info;
        }
        if (message.getAction() == MessageFireBrigade.ACTION_REST) {
            info.canNewAction = true;
            if (info.target != null) {
                this.targetBuildings.add(info.target);
                info.target = null;
            }
        } else if (message.getAction() == MessageFireBrigade.ACTION_REFILL) {
            info.canNewAction = (message.getWater() + this.maxPower >= this.maxWater);
            if (info.target != null) {
                this.targetBuildings.add(info.target);
                info.target = null;
            }
        } else if (message.getAction() == MessageFireBrigade.ACTION_MOVE) {
            if (message.getTargetID() != null) {
                StandardEntity entity = this.worldInfo.getEntity(message.getTargetID());
                if (entity != null && entity instanceof Area) {
                    if (info.target != null) {
                        StandardEntity targetEntity = this.worldInfo.getEntity(info.target);
                        if (targetEntity != null && targetEntity instanceof Area) {
                            if (message.getTargetID().getValue() == info.target.getValue()) {
                                info.canNewAction = false;
                            } else {
                                info.canNewAction = true;
                                this.targetBuildings.add(info.target);
                                info.target = null;
                            }
                        } else {
                            info.canNewAction = true;
                            info.target = null;
                        }
                    } else {
                        info.canNewAction = true;
                    }
                } else {
                    info.canNewAction = true;
                    if (info.target != null) {
                        this.targetBuildings.add(info.target);
                        info.target = null;
                    }
                }
            } else {
                info.canNewAction = true;
                if (info.target != null) {
                    this.targetBuildings.add(info.target);
                    info.target = null;
                }
            }
        } else if (message.getAction() == MessageFireBrigade.ACTION_EXTINGUISH) {
            info.canNewAction = true;
            info.target = null;
            this.priorityBuildings.remove(message.getTargetID());
            this.targetBuildings.remove(message.getTargetID());
        }
        return info;
    }
    
    /**
     * multiple fire strategy
     * @author Yangjiedong
     */
    
    /**
     * 判断是否符合多火区策略应用情况 
     * @param fireZones
     * @return
     */
    public boolean isMultiFire(List<ApolloFireClustering> fireZones) {
    	boolean isMF = false;
    	//TODO 判别条件计算
    	//TODO 符合条件火区的筛选
    	List<ApolloFireClustering> multiFire = new ArrayList<ApolloFireClustering>();
    	for (ApolloFireClustering cluster : fireZones) {
    		if (cluster.calc().getAllClusterEntities().size() > 5) {
    			multiFire.add(cluster);
    		}
    	}
    	//火区数量 大于2; 大于5 火区太多，无可行有效方法
    	if (multiFire.size() > 2 && multiFire.size() < 6) {    	
    		//火区的联合面积（包含中间gapArea）
    		
    	}
    	
    	
    	
    	return isMF;
    }
    
    
    
    public boolean isInCenterGapArea(Building building, List<ApolloFireClustering> fireZones) {
    	boolean isIn = false;
    	Pair<Integer, Integer> location = this.worldInfo.getLocation(building);
    	//TODO 数学判定，计算building是否在中心区域内
    	
    	return isIn;
    }
    
    public void createMultiFireBounds() {
    	
    }
    
    /**
     * get result for multiple fire situation
     * @param gapPartitions
     * @param fireZones
     * @return target list for multi-fire
     * @author Yangjiedong
     */
    public List<EntityID> multiFireResult (List<List<EntityID>> gapPartitions, List<ApolloFireClustering> fireZones) {
    	List<EntityID> result = new ArrayList<EntityID>();
    	
    	
    	
    	
    	return result;
    }
    
    /**
     * 计算所有非着火分区的中心
     * @param gapPartitions
     * @return
     * @author Yangjiedong
     */
    public List<Pair<Integer, Integer>> calcGapCenters (List<List<EntityID>> gapPartitions) {
    	List<List<Building>> gapParts = this.getGapPartitionBuildings(gapPartitions);
    	List<Pair<Integer, Integer>> centers = new ArrayList<Pair<Integer, Integer>>();
    	for (List<Building> gapPartition : gapParts) {
    		Pair<Integer, Integer> center = this.calCenter(gapPartition);
    		centers.add(center);
    	}
    	return centers;
    }
    
    /**
     * 将EntityID的数据转化为Building的
     * @param gapPartitions
     * @return
     * @author Yangjiedong
     */
    public List<List<Building>> getGapPartitionBuildings(List<List<EntityID>> gapPartitions) {
    	List<List<Building>> results = new ArrayList<List<Building>>();
    	List<Building> result = new ArrayList<Building>();
    	for (List<EntityID> rs : gapPartitions) {
    		for (EntityID entityID : rs) {
    			Building building = (Building)this.worldInfo.getEntity(entityID);
    			if (building != null)
    				result.add(building);
    		}
    		results.add(result);
    	}
    	return results;
    }
    
//    public int calcGapCenterX (List<StandardEntity> gapArea) {
//    	int centX = 0;
//    	int sum = 0;
//    	for(StandardEntity entity : gapArea) {
////    		sum += 
//    	}
//    	
//    	
//    	return centX;
//    }
//    public int calcGapCenterY (List<EntityID> gapArea) {
//    	int centY = 0;
//    	List<Building> fireBuildings = (ArrayList<Building>)worldInfo.getFireBuildings();
//    	List<Building> buildings = (ArrayList) getBuildings();
//    	
//    	return centY;
//    }
    /**
     * 为任意分区或者建筑列表计算中心
     * @param partition
     * @return
     */
    public Pair<Integer, Integer> calCenter(List<Building> partition) {
    	if (partition == null || partition.size() == 0) {
    		return null;
    	}
    	int sumX = 0;
    	int sumY = 0;
    	int centerX = 0;
    	int centerY = 0;
    	for (Building building : partition) {
    		sumX += this.worldInfo.getLocation(building).first();
    		sumY += this.worldInfo.getLocation(building).second();
    	}
    	centerX = sumX / partition.size();
    	centerY = sumY / partition.size();
    	return new Pair(centerX, centerY);
    }
    
    /**
     * 返回多火区中心坐标
     * @return centerLocation 
     */
    public Pair<Integer, Integer> calMultiFireCenter() {
//    	int sum = 0;
    	int sumX = 0;
    	int sumY = 0;
    	int centerX = 0;
    	int centerY = 0;
    	List<Building> fringe = this.getFringeBurning();
    	for (Building b : fringe) {
    		Pair<Integer, Integer> location = this.worldInfo.getLocation(b);
    		sumX += location.first();
    		sumY += location.second();
    	}
    	centerX = sumX/fringe.size();
    	centerY = sumY/fringe.size();
    	return new Pair(centerX, centerY);
    }
    
    /**
     * get burning building on fire zone fringe(fieryness 1 and 2)
     * @return
     */
    
    public List<Building> getFringeBurning() {
    	Set<Building> result = new HashSet<Building>();
    	Set<StandardEntity> buildings = (Set<StandardEntity>) this.getBuildings();
    	for (StandardEntity s : buildings) {
    		Building b = (Building) s;
    		if (b.getFieryness() < 3 && b.getFieryness() >= 1)
    			result.add(b);
    	}
    	List<Building> r = new ArrayList<Building>();
    	r.addAll(result);
    	return r;
    }
    
    /**
     * 获得小于3级火情的所有建筑 
     * @param allBuildings
     * @return
     */
    public List<StandardEntity> getCandidateBuildings() {
    	List<StandardEntity> result = new ArrayList<StandardEntity>();
    	List<StandardEntity> buildings = (List<StandardEntity>) this.getBuildings();
    	for (StandardEntity s : buildings) {
    		Building b = (Building) s;
    		if (b.getFieryness() < 3)
    			result.add(b);
    	}
    	return result;
    }
    
    public Collection<StandardEntity> getBuildings() {
		return this.worldInfo.getEntitiesOfType(StandardEntityURN.BUILDING,
				StandardEntityURN.REFUGE, StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.FIRE_STATION,
				StandardEntityURN.POLICE_OFFICE, StandardEntityURN.GAS_STATION);
	}

    private class FireBrigadeInfo {
        EntityID agentID;
        EntityID target;
        boolean canNewAction;
        int commandTime;

        FireBrigadeInfo(EntityID id) {
            agentID = id;
            target = null;
            canNewAction = true;
            commandTime = -1;
        }
    }

    private class DistanceSorter implements Comparator<StandardEntity> {
        private StandardEntity reference;
        private WorldInfo worldInfo;

        DistanceSorter(WorldInfo wi, StandardEntity reference)
        {
            this.reference = reference;
            this.worldInfo = wi;
        }

        public int compare(StandardEntity a, StandardEntity b)
        {
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            return d1 - d2;
        }
    }
    
//    @SuppressWarnings("hiding")
//	private class Pair<Integer a,Integer b> {
//    	int x;
//    	int y;
//    	
//    }
}
