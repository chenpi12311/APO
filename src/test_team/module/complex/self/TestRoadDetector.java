package test_team.module.complex.self;

import static rescuecore2.standard.entities.StandardEntityURN.AMBULANCE_TEAM;
import static rescuecore2.standard.entities.StandardEntityURN.CIVILIAN;
import static rescuecore2.standard.entities.StandardEntityURN.FIRE_BRIGADE;
import static rescuecore2.standard.entities.StandardEntityURN.GAS_STATION;
import static rescuecore2.standard.entities.StandardEntityURN.POLICE_FORCE;
import static rescuecore2.standard.entities.StandardEntityURN.REFUGE;

import java.util.*;

import adf.debug.TestLogger;
import org.apache.log4j.Logger;

import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.RoadDetector;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

public class TestRoadDetector extends RoadDetector {
	private Set<Area> openedAreas = new HashSet<>();
	private Clustering clustering;
	private PathPlanning pathPlanning;

	private EntityID result;
	private Logger logger;

	private List<Human> ignoreAgents = new ArrayList<>();
	private Collection<StandardEntity> allAgents;
	private List<Human> stuckAgents = new ArrayList<>();


	public TestRoadDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
		super(ai, wi, si, moduleManager, developData);
		logger = TestLogger.getLogger(agentInfo.me());
		this.pathPlanning = moduleManager.getModule("TestRoadDetector.PathPlanning", "ApolloRescue.module.algorithm.ApolloPathPlanning");
		this.clustering = moduleManager.getModule("TestRoadDetector.Clustering", "ApolloRescue.module.algorithm.ApolloKMeans");
		registerModule(this.clustering);
		registerModule(this.pathPlanning);
		this.allAgents = worldInfo.getAllEntities();
		this.result = null;

	}

	@Override
	public RoadDetector updateInfo(MessageManager messageManager) {
		logger.debug("Time:"+agentInfo.getTime());
		super.updateInfo(messageManager);
		return this;
	}

	@Override
	public RoadDetector calc() {
		EntityID positionID = this.agentInfo.getPosition();
		StandardEntity currentPosition = worldInfo.getEntity(positionID);
		openedAreas.add((Area) currentPosition);
		if (positionID.equals(result)) {
			logger.debug("reach to " + currentPosition + " reseting target");
			this.result = null;
		}

		if (this.result == null) {
			ArrayList<Area> currentTargets = calcTargets();
			logger.debug("Targets: " + currentTargets);
			if (currentTargets.isEmpty()) {
				this.result = null;
				return this;
			}
			this.pathPlanning.setFrom(positionID);
			this.pathPlanning.setDestination(toEntityIds(currentTargets));
			List<EntityID> path = this.pathPlanning.calc().getResult();
			if (path != null && path.size() > 0) {
				this.result = path.get(path.size() - 1);
			}
			logger.debug("Selected Target: " + this.result);
		}
		return this;
	}

	private Collection<EntityID> toEntityIds(Collection<? extends StandardEntity> entities) {
		ArrayList<EntityID> eids = new ArrayList<>();
		for (StandardEntity standardEntity : entities) {
			eids.add(standardEntity.getID());
		}
		return eids;
	}

	private ArrayList<Area> calcTargets() {
		ArrayList<Area> targetAreas = new ArrayList<>();
		for (StandardEntity e : this.worldInfo.getEntitiesOfType(REFUGE, GAS_STATION)) {
			targetAreas.add((Area) e);
		}

		for (StandardEntity e : this.worldInfo.getEntitiesOfType(FIRE_BRIGADE)) {
			if (isValidHuman(e)) {
				Human h = (Human) e;
				targetAreas.add((Area) worldInfo.getEntity(h.getPosition()));
			}
		}

		for (StandardEntity e : this.worldInfo.getEntitiesOfType(CIVILIAN, AMBULANCE_TEAM, POLICE_FORCE)) {
			if (isValidHuman(e)) {
				Human h = (Human) e;
				targetAreas.add((Area) worldInfo.getEntity(h.getPosition()));
			}
		}


//		while (ignoreAgents.size() < stuckAgents.size()) {
//			Human stuckAgent = getAgentTargetInPartition();
//			targetAreas.add((Area) worldInfo.getEntity(stuckAgent.getPosition()));
//		}

		ArrayList<Area> inClusterTarget = filterInCluster(targetAreas);
		inClusterTarget.removeAll(openedAreas);
		logger.debug("Cluster targets : " + inClusterTarget);
		return inClusterTarget;
	}

	private ArrayList<Area> filterInCluster(ArrayList<Area> targetAreas) {
		int clusterIndex = clustering.getClusterIndex(this.agentInfo.getID());
		ArrayList<Area> clusterTargets = new ArrayList<>();
		ArrayList<StandardEntity> inCluster = new ArrayList<>(clustering.getClusterEntities(clusterIndex));
		for (Area target : targetAreas) {
			if (inCluster.contains(target))
				clusterTargets.add(target);

		}

//		if (clusterTargets.size() < 1) {
//			ArrayList<Area> targets = targetAreas;
//			Collections.sort(targets, new Comparator<Area>() {
//				@Override
//				public int compare(Area o1, Area o2) {
//					int d1 = worldInfo.getDistance(agentInfo.me(), o1);
//					int d2 = worldInfo.getDistance(agentInfo.me(), o2);
//					return d1 - d2;
//				}
//			});
//			clusterTargets = targets;
//		}

		return clusterTargets;
	}

	@Override
	public EntityID getTarget() {
		return this.result;
	}

	private boolean isValidHuman(StandardEntity entity) {
		if (entity == null)
			return false;
		if (!(entity instanceof Human))
			return false;

		Human target = (Human) entity;
		if (!target.isHPDefined() || target.getHP() == 0)
			return false;
		if (!target.isPositionDefined())
			return false;
		if (!target.isDamageDefined() || target.getDamage() == 0)
			return false;
		if (!target.isBuriednessDefined())
			return false;

		StandardEntity position = worldInfo.getPosition(target);
		if (position == null)
			return false;

		StandardEntityURN positionURN = position.getStandardURN();
		if (positionURN == REFUGE || positionURN == AMBULANCE_TEAM)
			return false;

		return true;
	}


//	public EntityID getTarget() {
//		StandardEntity target = null;
//		target = getAgentTargetInPartition();
//		return target.getID();
//	}


	private Human getAgentTargetInPartition() {
		Human target = null;

		// choose the nearest stuck agent which is in own partition
		logger.debug("stuckAgents size:"+stuckAgents.size());

		for (Human human : stuckAgents) {
			logger.debug("human id:"+human.getID());
			if (human instanceof FireBrigade) {
				Area position = (Area) worldInfo.getEntity(human.getPosition());
				if (isInCluster(position) && !ignoreAgents.contains(human)) {
					target = human;
					ignoreAgents.add(human);
					return target;
				}
			}
		}

		for (Human human : stuckAgents) {
			Area position = (Area) worldInfo.getEntity(human.getPosition());
			if (isInCluster(position) && !ignoreAgents.contains(human)) {
				target = human;
				ignoreAgents.add(human);
				break;
			}
		}
		return target;
	}

	private boolean isInCluster(StandardEntity entity) {
		int clusterIndex = clustering.getClusterIndex(this.agentInfo.getID());
		HashSet<StandardEntity> inCluster = new HashSet<>(clustering.getClusterEntities(clusterIndex));
		if (inCluster.contains(entity)) {
			return true;
		}
		return false;
	}

	private List<Human> getStuckAgentsInPartition() {
		for (StandardEntity standardEntity : allAgents) {
			if (standardEntity != null && standardEntity instanceof Human && isValidHuman(standardEntity)) {
				stuckAgents.add((Human) standardEntity);
			}
		}
		if(stuckAgents != null && stuckAgents.size() > 0) {
			Collections.sort(stuckAgents,
					new DistanceSorter(worldInfo, agentInfo.me()));
			return stuckAgents;
		} else {
			return null;
		}
	}

	private class DistanceSorter implements Comparator<Human> {
		private StandardEntity reference;
		private WorldInfo worldInfo;

		DistanceSorter(WorldInfo wi, StandardEntity reference) {
			this.reference = reference;
			this.worldInfo = wi;
		}

		@Override
		public int compare(Human a, Human b) {
			int d1 = this.worldInfo.getDistance(this.reference, a);
			int d2 = this.worldInfo.getDistance(this.reference, b);
			return d1 - d2;
		}
	}

}
