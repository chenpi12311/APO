package ApolloRescue.module.complex;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.centralized.CommandAmbulance;
import adf.agent.communication.standard.bundle.centralized.MessageReport;
import adf.agent.communication.standard.bundle.information.MessageAmbulanceTeam;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.communication.standard.bundle.information.MessageCivilian;
import adf.agent.communication.standard.bundle.information.MessageFireBrigade;
import adf.agent.communication.standard.bundle.information.MessagePoliceForce;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.complex.AmbulanceTargetAllocator;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;


import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class ApolloAmbulanceTargetAllocator extends AmbulanceTargetAllocator {
    private Collection<EntityID> priorityHumans;
    private Collection<EntityID> targetHumans;
    private final static int DEFAULT_MAP_CYCLE = 800;
    private Map<EntityID, AmbulanceTeamInfo> ambulanceTeamInfoMap;

    public ApolloAmbulanceTargetAllocator(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.priorityHumans = new HashSet<>();
        this.targetHumans = new HashSet<>();
        this.ambulanceTeamInfoMap = new HashMap<>();
    }

    @Override
    public AmbulanceTargetAllocator resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if(this.getCountResume() >= 2) {
            return this;
        }
        for(EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.AMBULANCE_TEAM)) {
            this.ambulanceTeamInfoMap.put(id, new AmbulanceTeamInfo(id));
        }
        return this;
    }

    @Override
    public AmbulanceTargetAllocator preparate() {
        super.preparate();
        if(this.getCountPrecompute() >= 2) {
            return this;
        }
        for(EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.AMBULANCE_TEAM)) {
            this.ambulanceTeamInfoMap.put(id, new AmbulanceTeamInfo(id));
        }
        return this;
    }

    @Override
    public Map<EntityID, EntityID> getResult() {
        return this.convert(this.ambulanceTeamInfoMap);
    }

    @Override
    public AmbulanceTargetAllocator calc() {
        List<StandardEntity> agents = this.getActionAgents(this.ambulanceTeamInfoMap);
        Collection<EntityID> removes = new ArrayList<>();
        int currentTime = this.agentInfo.getTime();
        for(EntityID target : this.priorityHumans) {
            if(agents.size() > 0) {
                StandardEntity targetEntity = this.worldInfo.getEntity(target);
                if (targetEntity != null && targetEntity instanceof Human && ((Human)targetEntity).isPositionDefined()) {
                    agents.sort(new DistanceSorter(this.worldInfo, targetEntity));
                    StandardEntity result = agents.get(0);
                    agents.remove(0);
                    AmbulanceTeamInfo info = this.ambulanceTeamInfoMap.get(result.getID());
                    if (info != null) {
                        info.canNewAction = false;
                        info.target = target;
                        info.commandTime = currentTime;
                        this.ambulanceTeamInfoMap.put(result.getID(), info);
                        removes.add(target);
                    }
                }
            }
        }
        this.priorityHumans.removeAll(removes);
        removes.clear();
        for(EntityID target : this.targetHumans) {
            if(agents.size() > 0) {
                StandardEntity targetEntity = this.worldInfo.getEntity(target);
                if (targetEntity != null && targetEntity instanceof Human && ((Human)targetEntity).isPositionDefined()) {
                    agents.sort(new DistanceSorter(this.worldInfo, targetEntity));
                    StandardEntity result = agents.get(0);
                    agents.remove(0);
                    AmbulanceTeamInfo info = this.ambulanceTeamInfoMap.get(result.getID());
                    if(info != null) {
                        info.canNewAction = false;
                        info.target = target;
                        info.commandTime = currentTime;
                        this.ambulanceTeamInfoMap.put(result.getID(), info);
                        removes.add(target);
                    }
                }
            }
        }
        this.targetHumans.removeAll(removes);
        return this;
    }
    

    @Override
    public AmbulanceTargetAllocator updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if(this.getCountUpdateInfo() >= 2) {
            return this;
        }
        int currentTime = this.agentInfo.getTime();
        for(CommunicationMessage message : messageManager.getReceivedMessageList()) {
            Class<? extends CommunicationMessage> messageClass = message.getClass();
            if(messageClass == MessageCivilian.class) {
                MessageCivilian mc = (MessageCivilian) message;
                MessageUtil.reflectMessage(this.worldInfo, mc);
                if(mc.isBuriednessDefined() && mc.getBuriedness() > 0) {
                    this.targetHumans.add(mc.getAgentID());
                } else {
                    this.priorityHumans.remove(mc.getAgentID());
                    this.targetHumans.remove(mc.getAgentID());
                }
            } else if(messageClass == MessageFireBrigade.class) {
                MessageFireBrigade mfb = (MessageFireBrigade) message;
                MessageUtil.reflectMessage(this.worldInfo, mfb);
                if(mfb.isBuriednessDefined() && mfb.getBuriedness() > 0) {
                    this.priorityHumans.add(mfb.getAgentID());
                } else {
                    this.priorityHumans.remove(mfb.getAgentID());
                    this.targetHumans.remove(mfb.getAgentID());
                }
            } else if(messageClass == MessagePoliceForce.class) {
                MessagePoliceForce mpf = (MessagePoliceForce) message;
                MessageUtil.reflectMessage(this.worldInfo, mpf);
                if(mpf.isBuriednessDefined() && mpf.getBuriedness() > 0) {
                    this.priorityHumans.add(mpf.getAgentID());
                }else {
                    this.priorityHumans.remove(mpf.getAgentID());
                    this.targetHumans.remove(mpf.getAgentID());
                }
            }
        }
        for(CommunicationMessage message : messageManager.getReceivedMessageList(MessageAmbulanceTeam.class)) {
            MessageAmbulanceTeam mat = (MessageAmbulanceTeam) message;
            MessageUtil.reflectMessage(this.worldInfo, mat);
            if(mat.isBuriednessDefined() && mat.getBuriedness() > 0) {
                this.priorityHumans.add(mat.getAgentID());
            }else {
                this.priorityHumans.remove(mat.getAgentID());
                this.targetHumans.remove(mat.getAgentID());
            }
            AmbulanceTeamInfo info = this.ambulanceTeamInfoMap.get(mat.getAgentID());
            if(info == null) {
                info = new AmbulanceTeamInfo(mat.getAgentID());
            }
            if(currentTime >= info.commandTime + 2) {
                this.ambulanceTeamInfoMap.put(mat.getAgentID(), this.update(info, mat));
            }
        }
        for(CommunicationMessage message : messageManager.getReceivedMessageList(CommandAmbulance.class)) {
            CommandAmbulance command = (CommandAmbulance)message;
            if(command.getAction() == CommandAmbulance.ACTION_RESCUE && command.isBroadcast()) {
                this.priorityHumans.add(command.getTargetID());
                this.targetHumans.add(command.getTargetID());
            } else if(command.getAction() == CommandAmbulance.ACTION_LOAD && command.isBroadcast()) {
                this.priorityHumans.add(command.getTargetID());
                this.targetHumans.add(command.getTargetID());
            }
        }
        for(CommunicationMessage message : messageManager.getReceivedMessageList(MessageReport.class)) {
            MessageReport report = (MessageReport) message;
            AmbulanceTeamInfo info = this.ambulanceTeamInfoMap.get(report.getSenderID());
            if(info != null && report.isDone()) {
                info.canNewAction = true;
                this.priorityHumans.remove(info.target);
                this.targetHumans.remove(info.target);
                info.target = null;
                this.ambulanceTeamInfoMap.put(info.agentID, info);
            }
        }
        for(CommunicationMessage message:messageManager.getReceivedMessageList()){
        	//估算只有10个周期可以活得就不救了
        	float currentDamage;
        	int currentHP;
        	boolean flag=false;
        	Class<? extends CommunicationMessage> messageClass = message.getClass();
        	   if(messageClass == MessageCivilian.class) {
        		   flag=false;
                   MessageCivilian mc = (MessageCivilian) message;
                  
                   MessageUtil.reflectMessage(this.worldInfo, (MessageCivilian) message);
                   if((mc.isDamageDefined())&&(mc.isHPDefined())){
                   currentDamage=mc.getDamage();
                   currentHP=mc.getHP();
                   int lastLiveTime=-1;
                   int ttd=-1;
                   Iterator<EntityID> it=this.worldInfo.getFireBuildingIDs().iterator();
                   while(it.hasNext()){
                	   if(mc.getPosition().equals(it.next())){
                		   flag=true;
                		   this.priorityHumans.remove(mc.getAgentID());
                		   this.targetHumans.remove(mc.getAgentID());
                	   }
                   }
                   if(flag==false){
                   for(int i=this.agentInfo.getTime()+1;i<DEFAULT_MAP_CYCLE;i++){
           			if (currentDamage <= 37) {
           				currentDamage += 0.2;
           			} else if (currentDamage <= 63) {
           				currentDamage += 0.3;
           			} else if (currentDamage <= 88) {
           				currentDamage += 0.4;
           			} else {
           				currentDamage += 0.5;
           			}
           			currentHP -= Math.round(currentDamage);
           			if(currentHP<=0){
           				ttd=i-this.agentInfo.getTime();
           				lastLiveTime=i;
           			}
           			if(ttd<=10){
           				this.priorityHumans.remove(mc.getAgentID());
           			}
                   }
                   }
                   }
               } else if(messageClass == MessageFireBrigade.class) {
            	   flag=false;
                   MessageFireBrigade mfb = (MessageFireBrigade) message;
                   MessageUtil.reflectMessage(this.worldInfo, mfb);
                   if((mfb.isDamageDefined())&&(mfb.isHPDefined())){
                   currentDamage=mfb.getDamage();
                   currentHP=mfb.getHP();
                   int lastLiveTime=-1;
                   int ttd=-1;
                   Iterator<EntityID> it=this.worldInfo.getFireBuildingIDs().iterator();
                   while(it.hasNext()){
                	   if(mfb.getPosition().equals(it.next())){
                		   flag=true;
                		   this.priorityHumans.remove(mfb.getAgentID());
                		   this.targetHumans.remove(mfb.getAgentID());
                	   }
                   }if(flag==false){
                	   
 
                   for(int i=this.agentInfo.getTime()+1;i<DEFAULT_MAP_CYCLE;i++){
           			if (currentDamage <= 37) {
           				currentDamage += 0.2;
           			} else if (currentDamage <= 63) {
           				currentDamage += 0.3;
           			} else if (currentDamage <= 88) {
           				currentDamage += 0.4;
           			} else {
           				currentDamage += 0.5;
           			}
           			currentHP -= Math.round(currentDamage);
           			if(currentHP<=0){
           				ttd=i-this.agentInfo.getTime();
           				lastLiveTime=i;
           			}
           			if(ttd<=5){
           				this.priorityHumans.remove(mfb.getAgentID());
           			}
                   }
                   }
                   }
               } else if(messageClass == MessagePoliceForce.class) {
            	   flag=false;
                   MessagePoliceForce mpf = (MessagePoliceForce) message;
                   MessageUtil.reflectMessage(this.worldInfo, mpf);
                   if((mpf.isDamageDefined())&&(mpf.isHPDefined())){
                   currentDamage=mpf.getDamage();
                   currentHP=mpf.getHP();
                   int lastLiveTime=-1;
                   int ttd=-1;
                   Iterator<EntityID> it=this.worldInfo.getFireBuildingIDs().iterator();
                   while(it.hasNext()){
                	   if(mpf.getPosition().equals(it.next())){
                		   flag=true;
                		   this.priorityHumans.remove(mpf.getAgentID());
                		   this.targetHumans.remove(mpf.getAgentID());
                	   }
                   }if(flag==false){
                   for(int i=this.agentInfo.getTime()+1;i<DEFAULT_MAP_CYCLE;i++){
           			if (currentDamage <= 37) {
           				currentDamage += 0.2;
           			} else if (currentDamage <= 63) {
           				currentDamage += 0.3;
           			} else if (currentDamage <= 88) {
           				currentDamage += 0.4;
           			} else {
           				currentDamage += 0.5;
           			}
           			currentHP -= Math.round(currentDamage);
           			if(currentHP<=0){
           				ttd=i-this.agentInfo.getTime();
           				lastLiveTime=i;
           			}
           			if(ttd<=10){
           				this.priorityHumans.remove(mpf.getAgentID());
           			}
                   }
                   }
                   }
               }
        	
        }
        return this;
    }

    private Map<EntityID, EntityID> convert(Map<EntityID, AmbulanceTeamInfo> map) {
        Map<EntityID, EntityID> result = new HashMap<>();
        for(EntityID id : map.keySet()) {
            AmbulanceTeamInfo info = map.get(id);
            if(info != null && info.target != null) {
                result.put(id, info.target);
            }
        }
        return result;
    }

    private List<StandardEntity> getActionAgents(Map<EntityID, AmbulanceTeamInfo> map) {
        List<StandardEntity> result = new ArrayList<>();
        for(StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
            AmbulanceTeamInfo info = map.get(entity.getID());
            if(info != null && info.canNewAction && ((AmbulanceTeam)entity).isPositionDefined()) {
                result.add(entity);
            }
        }
        return result;
    }

    private AmbulanceTeamInfo update(AmbulanceTeamInfo info, MessageAmbulanceTeam message) {
        if(message.isBuriednessDefined() && message.getBuriedness() > 0) {
            info.canNewAction = false;
            if (info.target != null) {
                this.targetHumans.add(info.target);
                info.target = null;
            }
            return info;
        }
        if(message.getAction() == MessageAmbulanceTeam.ACTION_REST) {
            info.canNewAction = true;
            if (info.target != null) {
                this.targetHumans.add(info.target);
                info.target = null;
            }
        } else if(message.getAction() == MessageAmbulanceTeam.ACTION_MOVE) {
            if(message.getTargetID() != null) { 
                StandardEntity entity = this.worldInfo.getEntity(message.getTargetID());
                if(entity != null) {
                    if(entity instanceof Area) {
                        if(entity.getStandardURN() == REFUGE) {
                            info.canNewAction = false;
                            return info;
                        }
                        StandardEntity targetEntity = this.worldInfo.getEntity(info.target);
                        if (targetEntity != null) {
                            if(targetEntity instanceof Human) {
                                targetEntity = this.worldInfo.getPosition((Human)targetEntity);
                                if(targetEntity == null) {
                                    this.priorityHumans.remove(info.target);
                                    this.targetHumans.remove(info.target);
                                    info.canNewAction = true;
                                    info.target = null;
                                    return info;
                                }
                            }
                            if (targetEntity.getID().getValue() == entity.getID().getValue()) {
                                info.canNewAction = false;
                            } else {
                                info.canNewAction = true;
                                if (info.target != null) {
                                    this.targetHumans.add(info.target);
                                    info.target = null;
                                }
                            }
                            //防止门堵，如果目标仍然存在但10个周期没有救出目标判断目标门没清开，可以接受新目标
                            if((this.agentInfo.getTime()-info.commandTime>=10)&&(message.getAction()!=MessageAmbulanceTeam.ACTION_LOAD)){
                            	info.canNewAction=true;
                    
                            	}else{
                            		info.canNewAction=false;
                            }
                        }
                        else {
                            info.canNewAction = true;
                            info.target = null;
                        }
                        return info;
                    } else if(entity instanceof Human) {
                        if(entity.getID().getValue() == info.target.getValue()) {
                            info.canNewAction = false;
                        } else {
                            info.canNewAction = true;
                            this.targetHumans.add(info.target);
                            this.targetHumans.add(entity.getID());
                            info.target = null;
                        }
                        return info;
                    }
                }
            }
            info.canNewAction = true;
            if(info.target != null) {
                this.targetHumans.add(info.target);
                info.target = null;
            }
        } else if(message.getAction() == MessageAmbulanceTeam.ACTION_RESCUE) {
            info.canNewAction = true;
            if(info.target != null) {
                this.targetHumans.add(info.target);
                info.target = null;
            }
        } else if(message.getAction() == MessageAmbulanceTeam.ACTION_LOAD) {
            info.canNewAction = false;
        } else if(message.getAction() == MessageAmbulanceTeam.ACTION_UNLOAD) {
            info.canNewAction = true;
            this.priorityHumans.remove(info.target);
            this.targetHumans.remove(info.target);
            info.target = null;
        }
        return info;
    }


    private class AmbulanceTeamInfo {
        EntityID agentID;
        EntityID target;
        boolean canNewAction;
        int commandTime;

        AmbulanceTeamInfo(EntityID id) {
            agentID = id;
            target = null;
            canNewAction = true;
            commandTime = -1;
        }
    }

    private class DistanceSorter implements Comparator<StandardEntity> {
        private StandardEntity reference;
        private WorldInfo worldInfo;

        DistanceSorter(WorldInfo wi, StandardEntity reference) {
            this.reference = reference;
            this.worldInfo = wi;
        }
   	/**
 	 * 按距离优先，给智能体一个优先级
 	 * 先tm用着回头再改
  	 */
        public int compare(StandardEntity a, StandardEntity b) {
        	
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            if(this.reference.getURN().equals( StandardEntityURN.FIRE_BRIGADE)){
        		return (d1-d2)/4;
        	}else if(this.reference.getURN().equals( StandardEntityURN.POLICE_FORCE)){
        		return (d1-d2)/3;
        	}else if(this.reference.getURN().equals( StandardEntityURN.AMBULANCE_TEAM)){
        		return (d1-d2)/2;
        	}else{
            return d1 - d2;
        	}
        }
    }
}
// class RescueTarget {
//
//	private EntityID targetID;
//	private StandardEntity position;
//	private int distanceToPartition;
//	private int distanceToRefuge;
//	private int distanceToMe;
//	private int deathTime;
//	private int fireDeathTime;
//	private int buriedness;
//    private double victimSituation;
//    private Set<EntityID> nowWorkingOnMe = new HashSet<EntityID>();
//    private int ATneedToBeRescued;
//    private int timeToNearestRefuge;
//    private int priority;
//    private int damage;
//    private int HP;
//    private float cost;
//    private double deathTimeBasedWeight;   //基于不同身份的的处理过的死亡时间，用于优线救智能体
//
//	public RescueTarget(EntityID targetID) {
//		this.targetID = targetID;
//	}
//	
//	public void setDeathTimeBasedWeight(double deathTimeBasedWeight) {
//		this.deathTimeBasedWeight = deathTimeBasedWeight;
//	}
//	
//	public double getDeathTimeBasedWeight() {
//		return deathTimeBasedWeight;
//	}
//	
//	public void setTimeToNearestRefuge(int timeToNearestRefuge) {
//		this.timeToNearestRefuge = timeToNearestRefuge;
//	}
//	
//	public void setHP(int HP) {
//		this.HP = HP;
//	}
//	
//	public int getHP() {
//		return this.HP;
//	}
//	
//	public void setFireDeathTime(int fireDeathTime) {
//		this.fireDeathTime = fireDeathTime;
//	}
//	
//	public int getFireDeathTime() {
//		return this.fireDeathTime;
//	}
//	
//	public void setPriority(int priority) {
//		this.priority = priority;
//	}
//	
//	public int getPriority() {
//		return priority;
//	}
//	
//	public void setDamage(int damage) {
//		this.damage = damage;
//	}
//	
//	public int getDamage() {
//		return damage;
//	}
//	
//	public int getTimeToNearestRefuge() {
//		return timeToNearestRefuge;
//	}
//	
//	public Set<EntityID> getNowWorkingOnMe() {
//		return nowWorkingOnMe;
//	}
//	
//	public void setATneedToBeRescued(int ATneedToBeRescued) {
//		this.ATneedToBeRescued = ATneedToBeRescued;
//	}
//	
//	public int getATneedToBeRescued() {
//		return ATneedToBeRescued;
//	}
//
//	public EntityID getTarget1() {
//		return targetID;
//	}
//
//	public void setTarget(EntityID targetID) {
//		this.targetID = targetID;
//	}
//	
//	public int getBuriedness() {
//		return buriedness;
//	}
//
//	public void setBuriedness(int buriedness) {
//		this.buriedness = buriedness;
//	}
//
//	public StandardEntity getPosition() {
//		return position;
//	}
//	
//    public int getDistanceToRefuge() {
//        return distanceToRefuge;
//    }
//    
//    public void setDistanceToRefuge(int distanceToRefuge) {
//        this.distanceToRefuge = distanceToRefuge;
//    }
//
//	public void setPosition(StandardEntity position) {
//		this.position = position;
//	}
//	
//    public int getDistanceToPartition() {
//        return distanceToPartition;
//    }
//
//    public void setDistanceToPartition(int distanceToPartition) {
//        this.distanceToPartition = distanceToPartition;
//    }
//    
//    public int getDistanceToMe() {
//        return distanceToMe;
//    }
//    
//    public double getVictimSituation() {
//        return victimSituation;
//    }
//
//    public void setVictimSituation(double victimSituation) {
//        this.victimSituation = victimSituation;
//    }
//
//    public void setDistanceToMe(int distanceToMe) {
//        this.distanceToMe = distanceToMe;
//    }
//
//	public int getDeathTime() {
//		return deathTime;
//	}
//
//	public void setDeathTime(int deathTime) {
//		this.deathTime = deathTime;
//	}
//	
//    public float getCost() {
//        return cost;
//    }
//
//    public void setCost(float cost) {
//        this.cost = cost;
//    }
//    
//
//
//	@Override
//	public boolean equals(Object obj) {
//		if (obj == null) {
//			return false;
//		}
//		if (obj instanceof RescueTarget) {
//			RescueTarget r = (RescueTarget) obj;
//			if (this.targetID.equals(r.getTarget())) {
//				return true;
//			}
//		}
//		return false;
//	}
//	Set<StandardEntity> availableHumans = new FastSet<StandardEntity>();
//	/**
//	 * To get all available human IDs which need to rescue.</br>
//	 * @return {@code EntityID} set
//	 */
//	public Set<EntityID> getAvailableHumanIDs() {
//		Set<EntityID> result = new FastSet<EntityID>();
//		for(StandardEntity entity : availableHumans) {
//			result.add(entity.getID());
//		}
//		return result;
//	}
//	
// 
// public RescueTarget getTarget() {
//	     RescueTarget previousTarget;
//	     AmbulanceTeam me;
//		if (getVisibleTarget() != null) { // 先判断视野内的
//			previousTarget = getVisibleTarget();
//			return getVisibleTarget();
//		}
//
//		Set<EntityID> victims = me.getAvailableHumanIDs();
//		if (previousTarget != null
//				&& !victims.contains(previousTarget.getTarget())) { // 上一回合的目标已经被救
//			previousTarget = null;
//		}
//
//		validRescueTargets = me.getValidRescueTargets();
//		if (validRescueTargets == null || validRescueTargets.isEmpty()) { // 当前全图上都没有目标可供选择，执行遍历
//			return null;
//		}
//		RescueTarget bestTarget = null;
//		bestTarget = findBestVictimBasedDistance(validRescueTargets);
//		// bestTarget = findBestVictimBasedCost(validRescueTargets);
//
//		if (previousTarget != null
//				&& victims
//						.contains(world.getEntity(previousTarget.getTarget()))) {
//			if (bestTarget != null
//					&& !bestTarget.getTarget().equals(
//							previousTarget.getTarget())) {
//				Human bestHuman = (Human) world.getEntity(bestTarget
//						.getTarget());
//				Human previousHuman = (Human) world.getEntity(previousTarget
//						.getTarget());
//
//				pathPlanner.planMove((Area) world.getSelfPosition(),
//						(Area) world.getEntity(bestHuman.getPosition()), 0,
//						false);
//				int bestHumanCost = pathPlanner.getPathCost();
//				pathPlanner.planMove((Area) world.getSelfPosition(),
//						(Area) world.getEntity(previousHuman.getPosition()), 0,
//						false);
//				int previousHumanCost = pathPlanner.getPathCost();
//				if (previousHumanCost < (int) bestHumanCost * 3) { // TODO
//					bestTarget = previousTarget;
//					System.err.println("Time:" + world.getTime()
//							+ bestHumanCost + "改变目标！！！！！！！");
//				}
//			}
//		}
//
//		previousTarget = bestTarget;
//
//		if (bestTarget != null) {
//			return bestTarget;
//		} else {
//			if (shouldFindInMap()) { // 现在应该在全图找一个目标
//				RescueTarget bestTargetInMap = null;
//				bestTargetInMap = findBestVictimInMapBasedDistance(validRescueTargets);
//				// bestTargetInMap =
//				// findBestVictimInMapBasedCost(validRescueTargets);
//				return bestTargetInMap;
//			} else { // 现在还不需要在全图找目标，考虑遍历自己的分区
//				return null;
//			}
//		}
//	}
//	public <T extends StandardEntity> T getEntity(EntityID id, Class<T> c) {
//		StandardEntity entity;
//
//		entity = getEntity(id);
//		if (c.isInstance(entity)) {
//			T castedEntity;
//
//			castedEntity = c.cast(entity);
//			return castedEntity;
//		} else {
//			return null;
//		}
//	}
// /**
//	 * get best rescue target in view based death time
//	 * 
//	 * @return best rescue target in view
//	 */
//	public RescueTarget getVisibleTarget() {
//		RescueTarget target = null;
//		List<RescueTarget> lists = getVisibleRescueTarget();
//
//		if (lists.isEmpty()) {
//			return target;
//		}
//		for (RescueTarget h : lists) {
//			Human human = WorldInfo.getEntity(h.getTarget(), Human.class);
//			if (rescueHelper.canBeSaved(human, 8)) {
//				return h;
//			}
//			// System.out.println("无法救出来"); // XXX
//		}
//		return target;
//	}
// /**
//	 * get targets in viewer
//	 * 
//	 * @return targets in viewer
//	 */
//	private List<RescueTarget> getVisibleRescueTarget() {
//		List<RescueTarget> lists = new ArrayList<RescueTarget>();
//
//		for (EntityID cvID : world.getBuriedCivilianSeen()) { // 这里得到的在视野范围内的有掩埋值的人
//			RescueTarget rescueTarget = me.getValidRescueTargets().get(cvID);
//			if (rescueTarget != null && !lists.contains(rescueTarget)) {
//				lists.add(rescueTarget);
//			}
//		}
//
//		for (EntityID agentID : world.getBuriedAgentsSeen()) {
//			RescueTarget rescueTarget = me.getValidRescueTargets().get(agentID);
//			if (rescueTarget != null && !lists.contains(rescueTarget)) {
//				lists.add(rescueTarget);
//			}
//		}
//
//		Iterator<RescueTarget> itSeen = lists.iterator();
//		while (itSeen.hasNext()) {
//			RescueTarget rescueTarget = itSeen.next();
//			if (rescueTarget.getPosition() != null
//					&& !(rescueTarget.getPosition().equals(world
//							.getSelfPosition()))) { // 这里的伤员在建筑物中并有足够的人来救了！如果我和伤员不在同一节点就不把伤员当成目标！
//				itSeen.remove();
//			}
//		}
//
//		Collections.sort(lists,
//				new RescuePriorityWithAgentComparator(world.getSelfPosition(),
//						world));
//		return lists;
//
//	}
////
////public StandardEntity getTarget() {
////	StandardEntity previousTarget;
////	SampleTacticsAmbulanceTeam me;
////	if (getVisibleTarget() != null) { // 先判断视野内的
////		previousTarget = getVisibleTarget();
////		return getVisibleTarget();
////	}
////
////	Collection<EntityID> victims = this.targetHumans;
////	if (previousTarget != null
////			&& !victims.contains(previousTarget.getTarget())) { // 上一回合的目标已经被救
////		previousTarget = null;
////	}
////
////	validRescueTargets = me.getValidRescueTargets();
////	if (validRescueTargets == null || validRescueTargets.isEmpty()) { // 当前全图上都没有目标可供选择，执行遍历
////		return null;
////	}
////	RescueTarget bestTarget = null;
////	bestTarget = findBestVictimBasedDistance(validRescueTargets);
////	// bestTarget = findBestVictimBasedCost(validRescueTargets);
////
////	if (previousTarget != null
////			&& victims
////					.contains(world.getEntity(previousTarget.getTarget()))) {
////		if (bestTarget != null
////				&& !bestTarget.getTarget().equals(
////						previousTarget.getTarget())) {
////			Human bestHuman = (Human) world.getEntity(bestTarget
////					.getTarget());
////			Human previousHuman = (Human) world.getEntity(previousTarget
////					.getTarget());
////
////			pathPlanner.planMove((Area) world.getSelfPosition(),
////					(Area) world.getEntity(bestHuman.getPosition()), 0,
////					false);
////			int bestHumanCost = pathPlanner.getPathCost();
////			pathPlanner.planMove((Area) world.getSelfPosition(),
////					(Area) world.getEntity(previousHuman.getPosition()), 0,
////					false);
////			int previousHumanCost = pathPlanner.getPathCost();
////			if (previousHumanCost < (int) bestHumanCost * 3) { // TODO
////				bestTarget = previousTarget;
////				System.err.println("Time:" + world.getTime()
////						+ bestHumanCost + "改变目标！！！！！！！");
////			}
////		}
////	}
////
////	previousTarget = bestTarget;
////
////	if (bestTarget != null) {
////		return bestTarget;
////	} else {
////		if (shouldFindInMap()) { // 现在应该在全图找一个目标
////			RescueTarget bestTargetInMap = null;
////			bestTargetInMap = findBestVictimInMapBasedDistance(validRescueTargets);
////			// bestTargetInMap =
////			// findBestVictimInMapBasedCost(validRescueTargets);
////			return bestTargetInMap;
////		} else { // 现在还不需要在全图找目标，考虑遍历自己的分区
////			return null;
////		}
////	}
////}
///**
//Get all entities of a set of types.
//@param urns The type urns to look up.
//@return A new Collection of entities of the specified types.
//*/
//public Collection<StandardEntity> getEntitiesOfType(StandardEntityURN... urns) {
// Collection<StandardEntity> result = new HashSet<StandardEntity>();
// for (StandardEntityURN urn : urns) {
//     result.addAll(getEntitiesOfType(urn));
// }
// return result;
//}
///**
// * 返回指定位置的AT数量
// * 
// * @param position
// * @return
// */
//public int calATsNumOnArea(StandardEntity position) {
//	List<AmbulanceTeam> ambulanceTeamList = new ArrayList<AmbulanceTeam>();
//	for (StandardEntity standardEntity : getEntitiesOfType(
//			StandardEntityURN.AMBULANCE_TEAM))
//		 if (standardEntity instanceof AmbulanceTeam) {
//			ambulanceTeamList.add((AmbulanceTeam) standardEntity);
//		}
//	int ats = 0;
//	if (position == null) {
//		return 0;
//	}
//	for (AmbulanceTeam at : ambulanceTeamList) {
//		if (at.getPosition().equals(position.getID())) {
//			ats++;
//		}
//	}
//	return ats;
//}
//}
//
////public StandardEntity getVisibleTarget() {
////	StandardEntity target = null;
////	List<StandardEntity> lists = getVisibleRescueTarget();
////
////	if (lists.isEmpty()) {
////		return target;
////	}
////	for (StandardEntity h : lists) {
////		int atsThere = calATsNumOnArea(h.getPosition());
////		if (atsThere == 0) {
////			atsThere += 1;
////		}
////		Human human = world.getEntity(h.getTarget(), Human.class);
////		if (rescueHelper.canBeSaved(human, 8)) {
////			return h;
////		}
////	}
////	return target;
////}
////private List<StandardEntity> getVisibleRescueTarget() {
////	List<StandardEntity> lists = new ArrayList<StandardEntity>();
////
////	for (EntityID cvID : world.getBuriedCivilianSeen()) { // 这里得到的在视野范围内的有掩埋值的人
////		StandardEntity rescueTarget = me.getValidRescueTargets().get(cvID);
////		if (rescueTarget != null && !lists.contains(rescueTarget)) {
////			lists.add(rescueTarget);
////		}
////	}
////
////	for (EntityID agentID : world.getBuriedAgentsSeen()) {
////		StandardEntity rescueTarget = me.getValidRescueTargets().get(agentID);
////		if (rescueTarget != null && !lists.contains(rescueTarget)) {
////			lists.add(rescueTarget);
////		}
////	}
////
////	Iterator<StandardEntity> itSeen = lists.iterator();
////	while (itSeen.hasNext()) {
////		StandardEntity rescueTarget = itSeen.next();
////		if (rescueTarget.getPosition() != null
////				&& !(rescueTarget.getPosition().equals(world
////						.getSelfPosition()))) { // 这里的伤员在建筑物中并有足够的人来救了！如果我和伤员不在同一节点就不把伤员当成目标！
////			itSeen.remove();
////		}
////	}
////
////	Collections.sort(lists,
////			new RescuePriorityWithAgentComparator(world.getSelfPosition(),
////					world));
////	return lists;
////
////}
////}

