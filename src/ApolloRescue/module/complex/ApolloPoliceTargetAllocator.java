package ApolloRescue.module.complex;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.centralized.MessageReport;
import adf.agent.communication.standard.bundle.information.MessagePoliceForce;
import adf.agent.communication.standard.bundle.information.MessageRoad;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.complex.PoliceTargetAllocator;
import javafx.collections.transformation.SortedList;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class ApolloPoliceTargetAllocator extends PoliceTargetAllocator
{

    private Collection<EntityID> priorityAreas;
    private Collection<EntityID> targetAreas;

    private Collection<EntityID> targetAgents;


    private Map<EntityID, PoliceForceInfo> agentInfoMap;

    public ApolloPoliceTargetAllocator(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)
    {
        super(ai, wi, si, moduleManager, developData);
        this.priorityAreas = new HashSet<>();
        this.targetAreas = new HashSet<>();
        this.agentInfoMap = new HashMap<>();
        this.targetAgents = new HashSet<>();

    }

    @Override
    public PoliceTargetAllocator resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2)
        {
            return this;
        }
        for (EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.POLICE_FORCE))
        {
            this.agentInfoMap.put(id, new PoliceForceInfo(id));
        }
        for (StandardEntity e : this.worldInfo.getEntitiesOfType(REFUGE, BUILDING, GAS_STATION))
        {
            for (EntityID id : ((Building) e).getNeighbours())
            {
                StandardEntity neighbour = this.worldInfo.getEntity(id);
                if (neighbour instanceof Road)
                {
                    this.targetAreas.add(id);
                }
            }
        }
        for (StandardEntity e : this.worldInfo.getEntitiesOfType(REFUGE))
        {
            for (EntityID id : ((Building) e).getNeighbours())
            {
                StandardEntity neighbour = this.worldInfo.getEntity(id);
                if (neighbour instanceof Road)
                {
                    this.priorityAreas.add(id);
                }
            }
        }
        return this;
    }

    @Override
    public PoliceTargetAllocator preparate()
    {
        super.preparate();
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        for (EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.POLICE_FORCE))
        {
            this.agentInfoMap.put(id, new PoliceForceInfo(id));
        }
        for (StandardEntity e : this.worldInfo.getEntitiesOfType(REFUGE, BUILDING, GAS_STATION))
        {
            for (EntityID id : ((Building) e).getNeighbours())
            {
                StandardEntity neighbour = this.worldInfo.getEntity(id);
                if (neighbour instanceof Road)
                {
                    this.targetAreas.add(id);
                }
            }
        }
        for (StandardEntity e : this.worldInfo.getEntitiesOfType(REFUGE))
        {
            for (EntityID id : ((Building) e).getNeighbours())
            {
                StandardEntity neighbour = this.worldInfo.getEntity(id);
                if (neighbour instanceof Road)
                {
                    this.priorityAreas.add(id);
                }
            }
        }
        return this;
    }

    @Override
    public Map<EntityID, EntityID> getResult()
    {
        return this.convert(this.agentInfoMap);
    }

    @Override
    public PoliceTargetAllocator calc()
    {
        List<StandardEntity> agents = this.getActionAgents(this.agentInfoMap);
        Collection<EntityID> removes = new ArrayList<>();
        int currentTime = this.agentInfo.getTime();

        for(EntityID target : this.priorityAreas) {
            if(agents.size() > 0) {
                StandardEntity targetEntity = this.worldInfo.getEntity(target);
                if (targetEntity != null) {
                    agents.sort(new DistanceSorter(this.worldInfo, targetEntity));
                    StandardEntity result = agents.get(0);
                    agents.remove(0);
                    PoliceForceInfo info = this.agentInfoMap.get(result.getID());
                    if(info != null) {
                        info.canNewAction = false;
                        info.target = target;
                        info.commandTime = currentTime;
                        this.agentInfoMap.put(result.getID(), info);
                        removes.add(target);
                    }
                }
            }
        }
        this.priorityAreas.removeAll(removes);
        this.targetAreas = calcTargetWeight(targetAreas);
        List<StandardEntity> areas = new ArrayList<>();
        for(EntityID target : this.targetAreas) {
            StandardEntity targetEntity = this.worldInfo.getEntity(target);
            if (targetEntity != null) {
                areas.add(targetEntity);
            }
        }
        for(StandardEntity agent : agents) {
            if(areas.size() > 0) {
                areas.sort(new DistanceSorter(this.worldInfo, agent));
                StandardEntity result = areas.get(0);
                areas.remove(0);
                this.targetAreas.remove(result.getID());
                PoliceForceInfo info = this.agentInfoMap.get(agent.getID());
                if(info != null) {
                    info.canNewAction = false;
                    info.target = result.getID();
                    info.commandTime = currentTime;
                    this.agentInfoMap.put(agent.getID(), info);
                }
            }
        }



        return this;
    }

    private List<EntityID> calcTargetWeight(Collection<EntityID> targetAreas) {
        Map<EntityID, Integer> targetAreasWithWeight = new HashMap<>();
        for (EntityID area : targetAreas) {
            int weight = getTargetWeight(worldInfo.getEntity(area)).second();
            targetAreasWithWeight.put(area, weight);
        }
        List<EntityID> results = new ArrayList<>(targetAreas);
        Collections.sort(results, new Comparator<EntityID>() {
            @Override
            public int compare(EntityID o1, EntityID o2) {
                int weight1 = targetAreasWithWeight.get(o1);
                int weight2 = targetAreasWithWeight.get(o2);
                return weight2 - weight1;
            }
        });
        return results;
    }

    private Pair<TargetWeight,Integer> getTargetWeight(StandardEntity entity) {
        Building building;
        TargetWeight targetWeightType;
        int weightValue = 0;
        StandardEntity pos;
        Human human = null;
        if (entity instanceof Human) {
            human = (Human) entity;
            pos = worldInfo.getEntity(human.getID());
        } else {
            pos = entity;
        }

        if (!(entity instanceof Building) && entity instanceof Civilian && pos instanceof Building) {
            Human h = (Human) entity;
            if (h.isDamageDefined() && h.getDamage() == 0) {
                targetWeightType = TargetWeight.BUILDING_WITH_HEALTHY_HUMAN;
                weightValue += targetWeightType.getWeight();
            } else if (h.isDamageDefined() && h.getDamage() != 0) {
                targetWeightType = TargetWeight.BUILDING_WITH_DAMAGED_CIVILIAN;
                weightValue += targetWeightType.getWeight();
            } else {// undefined properties
                targetWeightType = TargetWeight.DEFAULT;
                weightValue = targetWeightType.getWeight();
            }
            return new Pair<TargetWeight, Integer>(targetWeightType, weightValue);
        }

        if (entity instanceof Refuge) {
            targetWeightType = TargetWeight.REFUGE_ENTRANCE;
            weightValue += targetWeightType.getWeight();
            return new Pair<TargetWeight, Integer>(targetWeightType, weightValue);

            // checking Fiery Building
        } else if (entity instanceof Building) {
            building = (Building) entity;

            if (!building.isFierynessDefined() || building.getFieryness() == 0) {
                int i = 0;
                for (Human h : worldInfo.getBuriedHumans(building)) {

                    if (h.isBuriednessDefined() && h.getBuriedness() == 0) {
                        i++;
                    }
                }
                targetWeightType = TargetWeight.BUILDING_WITH_HEALTHY_HUMAN;
                weightValue += targetWeightType.getWeight() * i;
                return new Pair<TargetWeight, Integer>(targetWeightType, weightValue);

            } else if (building.isFierynessDefined()) {
                if (building.getFieryness() == 1) {
                    targetWeightType = TargetWeight.FIERY_BUILDING_1;
                    weightValue += targetWeightType.getWeight();
                    return new Pair<TargetWeight, Integer>(targetWeightType, weightValue);
                } else if (building.getFieryness() == 2) {
                    targetWeightType = TargetWeight.FIERY_BUILDING_2;
                    weightValue += targetWeightType.getWeight();
                    return new Pair<TargetWeight, Integer>(targetWeightType, weightValue);
                } else if (building.getFieryness() == 3
                        || (building.isTemperatureDefined() && building.getTemperature() > 0)) {
                    targetWeightType = TargetWeight.FIERY_BUILDING_3;
                    weightValue += targetWeightType.getWeight();
                    return new Pair<TargetWeight, Integer>(targetWeightType, weightValue);
                }
            }

        } else if (entity instanceof Road) {
            targetWeightType = TargetWeight.DEFAULT;
            weightValue = targetWeightType.getWeight();
            return new Pair<TargetWeight, Integer>(targetWeightType, weightValue);
        }

        if (entity instanceof FireBrigade) {
            if (human.isBuriednessDefined()) {
                targetWeightType = TargetWeight.BURIED_FIRE_BRIGADE;
                weightValue += targetWeightType.getWeight();
                return new Pair<TargetWeight, Integer>(targetWeightType, weightValue);

            } else {
                targetWeightType = TargetWeight.BLOCKED_FIRE_BRIGADE;
                weightValue += targetWeightType.getWeight();
                return new Pair<TargetWeight, Integer>(targetWeightType, weightValue);

            }

        } else if (entity instanceof PoliceForce) {
            targetWeightType = TargetWeight.BURIED_POLICE_FORCE;
            weightValue += targetWeightType.getWeight();
            return new Pair<TargetWeight, Integer>(targetWeightType, weightValue);

        } else if (entity instanceof AmbulanceTeam) {
            if (human.isBuriednessDefined()) {
                targetWeightType = TargetWeight.BURIED_AMBULANCE_TEAM;
                weightValue += targetWeightType.getWeight();
                return new Pair<TargetWeight, Integer>(targetWeightType, weightValue);

            } else {
                targetWeightType = TargetWeight.BLOCKED_AMBULANCE_TEAM;
                weightValue += targetWeightType.getWeight();
                return new Pair<TargetWeight, Integer>(targetWeightType, weightValue);

            }

        } else if (entity instanceof Civilian) {
            if (human.isBuriednessDefined()) {
                targetWeightType = TargetWeight.BUILDING_WITH_DAMAGED_CIVILIAN;
                weightValue += targetWeightType.getWeight();
                return new Pair<TargetWeight, Integer>(targetWeightType, weightValue);

            } else {
                targetWeightType = TargetWeight.DEFAULT;
                weightValue += targetWeightType.getWeight();
                return new Pair<TargetWeight, Integer>(targetWeightType, weightValue);

            }

        }
        targetWeightType = TargetWeight.DEFAULT;
        return new Pair<TargetWeight, Integer>(targetWeightType, targetWeightType.getWeight());

    }

    @Override
    public PoliceTargetAllocator updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
        {
            return this;
        }
        int currentTime = this.agentInfo.getTime();
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageRoad.class))
        {
            MessageRoad mpf = (MessageRoad) message;
            MessageUtil.reflectMessage(this.worldInfo, mpf);
        }
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessagePoliceForce.class))
        {
            MessagePoliceForce mpf = (MessagePoliceForce) message;
            MessageUtil.reflectMessage(this.worldInfo, mpf);
            PoliceForceInfo info = this.agentInfoMap.get(mpf.getAgentID());
            if (info == null)
            {
                info = new PoliceForceInfo(mpf.getAgentID());
            }
            if (currentTime >= info.commandTime + 2)
            {
                this.agentInfoMap.put(mpf.getAgentID(), this.update(info, mpf));
            }
        }
        for (CommunicationMessage message : messageManager.getReceivedMessageList(CommandPolice.class))
        {
            CommandPolice command = (CommandPolice) message;
            if (command.getAction() == CommandPolice.ACTION_CLEAR && command.isBroadcast())
            {
                this.priorityAreas.add(command.getTargetID());
                this.targetAreas.add(command.getTargetID());
            }
        }
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageReport.class))
        {
            MessageReport report = (MessageReport) message;
            PoliceForceInfo info = this.agentInfoMap.get(report.getSenderID());
            if (info != null && report.isDone())
            {
                info.canNewAction = true;
                this.priorityAreas.remove(info.target);
                this.targetAreas.remove(info.target);
                info.target = null;
                this.agentInfoMap.put(info.agentID, info);
            }
        }
        return this;
    }

    private PoliceForceInfo update(PoliceForceInfo info, MessagePoliceForce message)
    {
        if (message.isBuriednessDefined() && message.getBuriedness() > 0)
        {
            info.canNewAction = false;
            if (info.target != null)
            {
                this.targetAreas.add(info.target);
                info.target = null;
            }
            return info;
        }
        if (message.getAction() == MessagePoliceForce.ACTION_REST)
        {
            info.canNewAction = true;
            if (info.target != null)
            {
                this.targetAreas.add(info.target);
                info.target = null;
            }
        }
        else if (message.getAction() == MessagePoliceForce.ACTION_MOVE)
        {
            if (message.getTargetID() != null)
            {
                StandardEntity entity = this.worldInfo.getEntity(message.getTargetID());
                if (entity != null && entity instanceof Area)
                {
                    if (info.target != null)
                    {
                        StandardEntity targetEntity = this.worldInfo.getEntity(info.target);
                        if (targetEntity != null && targetEntity instanceof Area)
                        {
                            if (message.getTargetID().getValue() == info.target.getValue())
                            {
                                info.canNewAction = false;
                            }
                            else
                            {
                                info.canNewAction = true;
                                this.targetAreas.add(info.target);
                                info.target = null;
                            }
                        }
                        else
                        {
                            info.canNewAction = true;
                            info.target = null;
                        }
                    }
                    else
                    {
                        info.canNewAction = true;
                    }
                }
                else
                {
                    info.canNewAction = true;
                    if (info.target != null)
                    {
                        this.targetAreas.add(info.target);
                        info.target = null;
                    }
                }
            }
            else
            {
                info.canNewAction = true;
                if (info.target != null)
                {
                    this.targetAreas.add(info.target);
                    info.target = null;
                }
            }
        }
        else if (message.getAction() == MessagePoliceForce.ACTION_CLEAR)
        {
            info.canNewAction = false;
        }
        return info;
    }

    private List<StandardEntity> getActionAgents(Map<EntityID, PoliceForceInfo> infoMap)
    {
        List<StandardEntity> result = new ArrayList<>();
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE))
        {
            PoliceForceInfo info = infoMap.get(entity.getID());
            if (info != null && info.canNewAction && ((PoliceForce) entity).isPositionDefined())
            {
                result.add(entity);
            }
        }
        return result;
    }

    private Map<EntityID, EntityID> convert(Map<EntityID, PoliceForceInfo> infoMap)
    {
        Map<EntityID, EntityID> result = new HashMap<>();
        for (EntityID id : infoMap.keySet())
        {
            PoliceForceInfo info = infoMap.get(id);
            if (info != null && info.target != null)
            {
                result.put(id, info.target);
            }
        }
        return result;
    }

    private class PoliceForceInfo
    {
        EntityID agentID;
        EntityID target;
        boolean canNewAction;
        int commandTime;

        PoliceForceInfo(EntityID id)
        {
            agentID = id;
            target = null;
            canNewAction = true;
            commandTime = -1;
        }
    }

    private class DistanceSorter implements Comparator<StandardEntity>
    {
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

    enum TargetWeight {

        BLOCKED_FIRE_BRIGADE(250),
        REFUGE_ENTRANCE(179),
        FIERY_BUILDING_1(190),
        BLOCKED_AMBULANCE_TEAM(187),
        BUILDING_WITH_HEALTHY_HUMAN(186),
        FIERY_BUILDING_2(185),
        FIERY_BUILDING_3(180),
        BURIED_FIRE_BRIGADE(177),
        BURIED_POLICE_FORCE(175),
        BURIED_AMBULANCE_TEAM(140),
        BUILDING_WITH_DAMAGED_CIVILIAN(110),

        SEEN_AGENT(1000), DEFAULT(1), ;

        private int weight;

        private TargetWeight(int weight) {
            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }
    }

    private class ClearTarget {
        private EntityID id;
        private EntityID positionID;
        private int weight;
        private TargetWeight targetWeight;
        private EntityID nearestRoadID;
        private Map<EntityID, Boolean> roadsToMove; // road and its openness
        private int distanceToIt;

        public ClearTarget(EntityID id, EntityID positionID, int weight, TargetWeight targetWeight) {
            this.id = id;
            this.positionID = positionID;
            this.weight = weight;
            this.targetWeight = targetWeight;
        }

        public ClearTarget(EntityID id, int importance) {
            this.id = id;
            this.weight = importance;
        }

        public EntityID getId() {
            return id;
        }

        public EntityID getPositionID() {
            return positionID;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int importance) {
            this.weight = importance;
        }

        public TargetWeight getImportanceType() {
            return targetWeight;
        }

        public EntityID getNearestRoadID() {
            return nearestRoadID;
        }

        public void setNearestRoadID(EntityID nearestRoadID) {
            this.nearestRoadID = nearestRoadID;
        }

        public int getDistanceToIt() {
            return distanceToIt;
        }

        public void setDistanceToIt(int distanceToIt) {
            this.distanceToIt = distanceToIt;
        }

        public Map<EntityID, Boolean> getRoadsToMove() {
            return roadsToMove;
        }

        public void setRoadsToMove(Map<EntityID, Boolean> roadsToMove) {
            this.roadsToMove = roadsToMove;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ClearTarget)) {
                return false;
            }
            ClearTarget target = (ClearTarget) obj;
            if (target.getId().equals(getId()) && target.getPositionID().equals(getPositionID())
                    && target.getImportanceType().equals(getImportanceType())) {
                return true;
            }
            return false;
        }
    }

}



