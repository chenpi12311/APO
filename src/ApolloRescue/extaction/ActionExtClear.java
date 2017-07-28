package ApolloRescue.extaction;

import ApolloRescue.tools.ClearHelperUtils;
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.common.ActionRest;
import adf.agent.action.police.ActionClear;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.PathPlanning;
import com.google.common.collect.Lists;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

import java.util.*;
import java.util.stream.Collectors;

public class ActionExtClear extends ExtAction
{
    private PathPlanning pathPlanning;

    private int clearDistance;
    private int forcedMove;
    private int thresholdRest;
    private int kernelTime;

    private EntityID target;
    private Map<EntityID, Set<Point2D>> movePointCache;
    private int oldClearX;
    private int oldClearY;
    private int count;

    private int DefaultLength = 8000;

    public ActionExtClear(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)
    {
        super(ai, wi, si, moduleManager, developData);
        this.clearDistance = si.getClearRepairDistance();
        this.forcedMove = developData.getInteger("ActionExtClear.forcedMove", 3);
        this.thresholdRest = developData.getInteger("ActionExtClear.rest", 100);

        this.target = null;
        this.movePointCache = new HashMap<>();
        this.oldClearX = 0;
        this.oldClearY = 0;
        this.count = 0;

        switch (si.getMode())
        {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "ApolloRescue.module.algorithm.ApolloPathPlanning");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "ApolloRescue.module.algorithm.ApolloPathPlanning");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "ApolloRescue.module.algorithm.ApolloPathPlanning");
                break;
        }
    }

    @Override
    public ExtAction precompute(PrecomputeData precomputeData)
    {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
        try
        {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        }
        catch (NoSuchConfigOptionException e)
        {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2)
        {
            return this;
        }
        this.pathPlanning.resume(precomputeData);
        try
        {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        }
        catch (NoSuchConfigOptionException e)
        {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction preparate()
    {
        super.preparate();
        if (this.getCountPreparate() >= 2)
        {
            return this;
        }
        this.pathPlanning.preparate();
        try
        {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        }
        catch (NoSuchConfigOptionException e)
        {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
        {
            return this;
        }
        this.pathPlanning.updateInfo(messageManager);
        return this;
    }

    @Override
    public ExtAction setTarget(EntityID target)
    {
        this.target = null;
        StandardEntity entity = this.worldInfo.getEntity(target);
        if (entity != null)
        {
            if (entity instanceof Road)
            {
                this.target = target;
            }
            else if (entity.getStandardURN().equals(StandardEntityURN.BLOCKADE))
            {
                this.target = ((Blockade) entity).getPosition();
            }
            else if (entity instanceof Building)
            {
                this.target = target;
            }
        }
        return this;
    }

    @Override
    public ExtAction calc()
    {
        this.result = null;
        PoliceForce policeForce = (PoliceForce) this.agentInfo.me();

        if (this.needRest(policeForce))
        {
            List<EntityID> list = new ArrayList<>();
            if (this.target != null)
            {
                list.add(this.target);
            }
            this.result = this.calcRest(policeForce, this.pathPlanning, list);
            if (this.result != null)
            {
                return this;
            }
        }

        if (this.target == null)
        {
            return this;
        }
        EntityID agentPosition = policeForce.getPosition();
        StandardEntity targetEntity = this.worldInfo.getEntity(this.target);
        StandardEntity positionEntity = Objects.requireNonNull(this.worldInfo.getEntity(agentPosition));
        //貌似pf不会选择人为目标
        if (targetEntity == null || !(targetEntity instanceof Area))
        {
            return this;
        }
        if (positionEntity instanceof Road)
        {
            this.result = this.getRescueAction(policeForce, (Road) positionEntity);
            if (this.result != null)
            {
                return this;
            }
        }
        if (agentPosition.equals(this.target))
        {
            this.result = this.getAreaClearAction(policeForce, targetEntity);
        }
        // else if (((Area) targetEntity).getEdgeTo(agentPosition) != null)
        // {
        //     this.result = this.getNeighbourPositionAction(policeForce, (Area) targetEntity);
        // }
        else
        {
            List<EntityID> path = this.pathPlanning.getResult(agentPosition, this.target);
            if (path != null && path.size() > 0)
            {
                Point2D position = clearForRoad(path);
                if (position == null)
                    this.result = new ActionMove(path);
                else
                    this.result = new ActionClear((int) position.getX(), (int) position.getY());
            }
        }
        return this;
    }

    public Point2D clearForRoad(List<EntityID> pathToGo) {
        PoliceForce policeForce = (PoliceForce) this.agentInfo.me();
        Point2D agentPoint = new Point2D(policeForce.getX(), policeForce.getY());
        if (pathToGo == null || pathToGo.size() <= 1) {// 如果道路方向无法生成
            return null;
        } else {
            Area selfArea = (Area) worldInfo.getPosition(policeForce);
            Point2D areaCenter = new Point2D(selfArea.getX(), selfArea.getY());
            Edge nextEdge = selfArea.getEdgeTo(pathToGo.get(1));
            Point2D nextPoint = ClearHelperUtils.getEdgeCenter(nextEdge);
            Vector2D referenceDirection = nextPoint.minus(areaCenter);
            if (directionChecker(referenceDirection, agentPoint,
                    blockadeInSight(), DefaultLength,
                    1000)) {// 发现障碍物
                Point2D best = findClearPoint(referenceDirection, agentPoint);
                return best;
            } else {// 没有发现障碍物
                return null;
            }
        }
    }

    private boolean directionChecker(Vector2D direction, Point2D agentPoint,
                                     Set<Blockade> blockades, int length, int rad) {
        if (direction == null) {
            return false;
        } else {
            if (blockadesDetecter(blockades,
                    createJudgeArea(direction, agentPoint, length, rad))) {
                return true;
            } else {
                return false;
            }
        }
    }

    private Point2D findClearPoint(Vector2D direction, Point2D agentPoint) {
        int maxlen = 10000;
        direction = direction.normalised().scale(maxlen);
        Point2D targetPoint = agentPoint.plus(direction);
        return targetPoint;
    }

    /**
     * 选取视野内的障碍物
     * @return
     */
    private Set<Blockade> blockadeInSight() {
        return getBlockadeSeen();
    }

    /**
     * 获得看到的障碍物
     * @return
     */
    private Set<Blockade> getBlockadeSeen() {
        Set<Blockade> blockadeSeen = new HashSet<Blockade>();
        ChangeSet changeSet = this.worldInfo.getChanged();
        for (EntityID entityID : changeSet.getChangedEntities()) {
            if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.BLOCKADE.toString())) {
                Blockade blockade = (Blockade) this.worldInfo.getEntity(entityID);
                for (Property p : changeSet.getChangedProperties(entityID)) {

                    blockade.getProperty(p.getURN()).takeValue(p);
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

                blockadeSeen.add(blockade);
            }
        }
        return blockadeSeen;
    }

    private boolean blockadesDetecter(Set<Blockade> blockades,
                                      List<Edge> judgeArea) {// 多个障碍物探测器
        if (blockades == null || blockades.size() == 0 || judgeArea == null) {
            return false;
        } else {
            for (Blockade b : blockades) {
                if (blockadeDetecter(b, judgeArea)) {
                    return true;
                }
            }
            return false;
        }
    }

    private List<Edge> createJudgeArea(Vector2D direction, Point2D selfPoint,
                                       int length, int rad) {
        Vector2D dir = direction.normalised().scale(rad);
        Vector2D perpend1 = new Vector2D(-dir.getY(), dir.getX());
        Vector2D perpend2 = new Vector2D(dir.getY(), -dir.getX());
        Point2D targetPoint = selfPoint.plus(direction.normalised().scale(
                length));
        Point2D points[] = new Point2D[] { selfPoint.plus(perpend1),
                targetPoint.plus(perpend1), targetPoint.plus(perpend2),
                selfPoint.plus(perpend2) };
        List<Edge> edges = ClearHelperUtils.getEdgesFromPoint2D(points);
        return edges;
    }

    private boolean blockadeDetecter(Blockade b, List<Edge> judgeArea) {// 单个障碍物判断，true是发现障碍，false未发现障碍
        if (b == null || b.getApexes() == null || b.getApexes().length == 0) {
            return false;
        } else {
            List<Point2D> blockadePoints = ClearHelperUtils
                    .getPointsFromApexs(b.getApexes());
            List<Edge> blockadeEdges = ClearHelperUtils
                    .getEdgesFromPoint2D(blockadePoints);
            if (null != blockadeEdges && blockadeEdges.size() > 0) {
                for (Edge be : blockadeEdges) {
                    for (Edge agentEdge : judgeArea) {
                        Point2D p = ClearHelperUtils.getSegmentIntercetPoint(
                                agentEdge, be);
                        if (null != p) {
                            return true;
                        }
                    }
                }
            } else {
                return false;
            }// 第一步检查所有边都与判断边不相交，结束
            Point2D testPoint = blockadePoints.get(0);
            if (ClearHelperUtils.insideShape(judgeArea, testPoint)) {
                return true;
            }// 第二步检查是否障碍物完全在内部，结束
            Point2D testPoint2 = judgeArea.get(0).getStart();
            if (ClearHelperUtils.insideShape(blockadeEdges, testPoint2)) {
                return true;
            }
            // 第三步检查是否处在超大障碍物内部，结束
        }
        return false;
    }

    public List<Vector2D> RoadDirection(Area a) {// 道路方向生成器
        List<EntityID> neighboursID = a.getNeighbours();
        List<Vector2D> directions = new ArrayList<>();
        Point2D areaCenter = new Point2D(a.getX(), a.getY());
        int roadNeighbourNum = roadNeighbourNum(a);
        switch (roadNeighbourNum) {
            case -1:
                return directions;// FIXME:对于完全没有neighbour的Area
            case 0:// 该情况为建筑物之间的通道或是建筑物内部，选取area中点到所有neighbour公共边中点的正负方向为参考方向
                for (EntityID id : neighboursID) {
                    Edge commonEdge = a.getEdgeTo(id);
                    Point2D edgeCenter = ClearHelperUtils.getEdgeCenter(commonEdge);
                    Vector2D v = edgeCenter.minus(areaCenter);
                    directions.add(v);
                    v = areaCenter.minus(edgeCenter);
                    directions.add(v);
                }
                return directions;
            case 1:// 该情况为路的末端或者入口，选取area中点和相邻路的中点的两个方向方向作为路的两个参考方向
                for (EntityID id : neighboursID) {
                    if (worldInfo.getEntity(id) instanceof Road) {
                        Edge commonEdge = a.getEdgeTo(id);
                        Point2D edgeCenter = ClearHelperUtils
                                .getEdgeCenter(commonEdge);
                        Vector2D v = edgeCenter.minus(areaCenter);
                        directions.add(v);
                        v = areaCenter.minus(edgeCenter);
                        directions.add(v);
                    }
                }
                return directions;

            case 2:// 该情况为直路或拐弯，选取area中点和相邻路的公共边中点的两个方向作为路的参考方向
                for (EntityID id : neighboursID) {
                    if (worldInfo.getEntity(id) instanceof Road && !isEntrance(id)) {
                        Edge commonEdge = a.getEdgeTo(id);
                        Point2D edgeCenter = ClearHelperUtils
                                .getEdgeCenter(commonEdge);
                        Vector2D v = edgeCenter.minus(areaCenter);
                        directions.add(v);
                    }
                }
                return directions;
            default:// 该情况为十字路口或三叉路口，选取area中点和各不是入口公共边中点方向为路的若干个参考方向。
                for (EntityID id : neighboursID) {
                    if (worldInfo.getEntity(id) instanceof Road && !isEntrance(id)) {
                        Edge commonEdge = a.getEdgeTo(id);
                        Point2D edgeCenter = ClearHelperUtils
                                .getEdgeCenter(commonEdge);
                        Vector2D v = edgeCenter.minus(areaCenter);
                        directions.add(v);
                    }
                }
                return directions;

        }
    }

    private int roadNeighbourNum(Area a) {// 统计一个节点与多少条路相连
        int num = 0;
        List<EntityID> neighbourID = a.getNeighbours();
        if (neighbourID == null || neighbourID.size() == 0) {
            return -1;
        } else {
            for (EntityID id : neighbourID) {
                if (worldInfo.getEntity(id) instanceof Road && !isEntrance(id)) {
                    num++;
                }
            }
            return num;
        }
    }

    public boolean isEntrance(EntityID id) {
        if (worldInfo.getEntity(id) instanceof Area) {
            Area a = (Area) worldInfo.getEntity(id);
            return isEntrance(a);
        } else {
            return false;
        }
    }

    private boolean isEntrance(Area a) {// 和建筑物直接相连,且面积不大于20的是入口
        List<EntityID> neighbourID = a.getNeighbours();
        if (neighbourID == null || neighbourID.size() == 0) {
            return false;
        } else {
            // if (ClearHelperUtils.getTotalSurface(a) > 20) {
            // return false;
            // }
            boolean buildingFlag = false;
            int roadNum = 0;
            for (EntityID id : neighbourID) {
                if (worldInfo.getEntity(id) instanceof Building) {
                    buildingFlag = true;
                    // return true;
                } else if (worldInfo.getEntity(id) instanceof Road) {
                    roadNum++;
                }
            }
            if (!buildingFlag) {
                return false;
            } else {
                if (roadNum == 1) {
                    return true;
                } else if (ClearHelperUtils.getTotalSurface(a) > 20) {
                    return false;
                } else {
                    return true;
                }
            }
        }
    }

    //###############################################################################
    //###############################################################################
    //###############################################################################
    //###############################################################################
    //###############################################################################

    private Action getRescueAction(PoliceForce police, Road road)
    {
        if (!road.isBlockadesDefined())
        {
            return null;
        }
        Collection<Blockade> blockades = this.worldInfo.getBlockades(road)
                .stream()
                .filter(Blockade::isApexesDefined)
                .collect(Collectors.toSet());
        Collection<StandardEntity> agents = this.worldInfo.getEntitiesOfType(
                StandardEntityURN.AMBULANCE_TEAM,
                StandardEntityURN.FIRE_BRIGADE
        );

        double policeX = police.getX();
        double policeY = police.getY();
        double minDistance = Double.MAX_VALUE;
        Action moveAction = null;
        for (StandardEntity entity : agents)
        {
            Human human = (Human) entity;
            if (!human.isPositionDefined() || human.getPosition().getValue() != road.getID().getValue())
            {
                continue;
            }
            double humanX = human.getX();
            double humanY = human.getY();
            ActionClear actionClear = null;
            for (Blockade blockade : blockades)
            {
                if (!this.isInside(humanX, humanY, blockade.getApexes()))
                {
                    continue;
                }
                double distance = this.getDistance(policeX, policeY, humanX, humanY);
                if (this.intersect(policeX, policeY, humanX, humanY, road))
                {
                    Action action = this.getIntersectEdgeAction(policeX, policeY, humanX, humanY, road);
                    if (action == null)
                    {
                        continue;
                    }
                    if (action.getClass() == ActionClear.class)
                    {
                        if (actionClear == null)
                        {
                            actionClear = (ActionClear) action;
                            continue;
                        }
                        if (actionClear.getTarget() != null)
                        {
                            Blockade another = (Blockade) this.worldInfo.getEntity(actionClear.getTarget());
                            if (another != null && this.intersect(blockade, another))
                            {
                                return new ActionClear(another);
                            }
                            int anotherDistance = this.worldInfo.getDistance(police, another);
                            int blockadeDistance = this.worldInfo.getDistance(police, blockade);
                            if (anotherDistance > blockadeDistance)
                            {
                                return action;
                            }
                        }
                        return actionClear;
                    }
                    else if (action.getClass() == ActionMove.class && distance < minDistance)
                    {
                        minDistance = distance;
                        moveAction = action;
                    }
                }
                else if (this.intersect(policeX, policeY, humanX, humanY, blockade))
                {
                    Vector2D vector = this.scaleClear(this.getVector(policeX, policeY, humanX, humanY));
                    int clearX = (int) (policeX + vector.getX());
                    int clearY = (int) (policeY + vector.getY());
                    vector = this.scaleBackClear(vector);
                    int startX = (int) (policeX + vector.getX());
                    int startY = (int) (policeY + vector.getY());
                    if (this.intersect(startX, startY, clearX, clearY, blockade))
                    {
                        if (actionClear == null)
                        {
                            actionClear = new ActionClear(clearX, clearY, blockade);
                        }
                        else
                        {
                            if (actionClear.getTarget() != null)
                            {
                                Blockade another = (Blockade) this.worldInfo.getEntity(actionClear.getTarget());
                                if (another != null && this.intersect(blockade, another))
                                {
                                    return new ActionClear(another);
                                }
                                int distance1 = this.worldInfo.getDistance(police, another);
                                int distance2 = this.worldInfo.getDistance(police, blockade);
                                if (distance1 > distance2)
                                {
                                    return new ActionClear(clearX, clearY, blockade);
                                }
                            }
                            return actionClear;
                        }
                    }
                    else if (distance < minDistance)
                    {
                        minDistance = distance;
                        moveAction = new ActionMove(Lists.newArrayList(road.getID()), (int) humanX, (int) humanY);
                    }
                }
            }
            if (actionClear != null)
            {
                return actionClear;
            }
        }
        return moveAction;
    }

    private Action getAreaClearAction(PoliceForce police, StandardEntity targetEntity)
    {
        if (targetEntity instanceof Building)
        {
            return null;
        }
        Road road = (Road) targetEntity;
        if (!road.isBlockadesDefined() || road.getBlockades().isEmpty())
        {
            return null;
        }
        Collection<Blockade> blockades = this.worldInfo.getBlockades(road)
                .stream()
                .filter(Blockade::isApexesDefined)
                .collect(Collectors.toSet());
        int minDistance = Integer.MAX_VALUE;
        Blockade clearBlockade = null;
        for (Blockade blockade : blockades)
        {
            for (Blockade another : blockades)
            {
                if (!blockade.getID().equals(another.getID()) && this.intersect(blockade, another))
                {
                    int distance1 = this.worldInfo.getDistance(police, blockade);
                    int distance2 = this.worldInfo.getDistance(police, another);
                    if (distance1 <= distance2 && distance1 < minDistance)
                    {
                        minDistance = distance1;
                        clearBlockade = blockade;
                    }
                    else if (distance2 < minDistance)
                    {
                        minDistance = distance2;
                        clearBlockade = another;
                    }
                }
            }
        }
        if (clearBlockade != null)
        {
            if (minDistance < this.clearDistance)
            {
                return new ActionClear(clearBlockade);
            }
            else
            {
                return new ActionMove(
                        Lists.newArrayList(police.getPosition()),
                        clearBlockade.getX(),
                        clearBlockade.getY()
                );
            }
        }
        double agentX = police.getX();
        double agentY = police.getY();
        clearBlockade = null;
        Double minPointDistance = Double.MAX_VALUE;
        int clearX = 0;
        int clearY = 0;
        for (Blockade blockade : blockades)
        {
            int[] apexes = blockade.getApexes();
            for (int i = 0; i < (apexes.length - 2); i += 2)
            {
                double distance = this.getDistance(agentX, agentY, apexes[i], apexes[i + 1]);
                if (distance < minPointDistance)
                {
                    clearBlockade = blockade;
                    minPointDistance = distance;
                    clearX = apexes[i];
                    clearY = apexes[i + 1];
                }
            }
        }
        if (clearBlockade != null)
        {
            if (minPointDistance < this.clearDistance)
            {
                Vector2D vector = this.scaleClear(this.getVector(agentX, agentY, clearX, clearY));
                clearX = (int) (agentX + vector.getX());
                clearY = (int) (agentY + vector.getY());
                return new ActionClear(clearX, clearY, clearBlockade);
            }
            return new ActionMove(Lists.newArrayList(police.getPosition()), clearX, clearY);
        }
        return null;
    }

    private Action getNeighbourPositionAction(PoliceForce police, Area target)
    {
        double agentX = police.getX();
        double agentY = police.getY();
        StandardEntity position = Objects.requireNonNull(this.worldInfo.getPosition(police));
        Edge edge = target.getEdgeTo(position.getID());
        if (edge == null)
        {
            return null;
        }
        if (position instanceof Road)
        {
            Road road = (Road) position;
            if (road.isBlockadesDefined() && road.getBlockades().size() > 0)
            {
                double midX = (edge.getStartX() + edge.getEndX()) / 2;
                double midY = (edge.getStartY() + edge.getEndY()) / 2;
                if (this.intersect(agentX, agentY, midX, midY, road))
                {
                    return this.getIntersectEdgeAction(agentX, agentY, edge, road);
                }
                ActionClear actionClear = null;
                ActionMove actionMove = null;
                Vector2D vector = this.scaleClear(this.getVector(agentX, agentY, midX, midY));
                int clearX = (int) (agentX + vector.getX());
                int clearY = (int) (agentY + vector.getY());
                vector = this.scaleBackClear(vector);
                int startX = (int) (agentX + vector.getX());
                int startY = (int) (agentY + vector.getY());
                for (Blockade blockade : this.worldInfo.getBlockades(road))
                {
                    if (blockade == null || !blockade.isApexesDefined())
                    {
                        continue;
                    }
                    if (this.intersect(startX, startY, midX, midY, blockade))
                    {
                        if (this.intersect(startX, startY, clearX, clearY, blockade))
                        {
                            if (actionClear == null)
                            {
                                actionClear = new ActionClear(clearX, clearY, blockade);
                                if (this.equalsPoint(this.oldClearX, this.oldClearY, clearX, clearY))
                                {
                                    if (this.count >= this.forcedMove)
                                    {
                                        this.count = 0;
                                        return new ActionMove(Lists.newArrayList(road.getID()), clearX, clearY);
                                    }
                                    this.count++;
                                }
                                this.oldClearX = clearX;
                                this.oldClearY = clearY;
                            }
                            else
                            {
                                if (actionClear.getTarget() != null)
                                {
                                    Blockade another = (Blockade) this.worldInfo.getEntity(actionClear.getTarget());
                                    if (another != null && this.intersect(blockade, another))
                                    {
                                        return new ActionClear(another);
                                    }
                                }
                                return actionClear;
                            }
                        }
                        else if (actionMove == null)
                        {
                            actionMove = new ActionMove(Lists.newArrayList(road.getID()), (int) midX, (int) midY);
                        }
                    }
                }
                if (actionClear != null)
                {
                    return actionClear;
                }
                else if (actionMove != null)
                {
                    return actionMove;
                }
            }
        }
        if (target instanceof Road)
        {
            Road road = (Road) target;
            if (!road.isBlockadesDefined() || road.getBlockades().isEmpty())
            {
                return new ActionMove(Lists.newArrayList(position.getID(), target.getID()));
            }
            Blockade clearBlockade = null;
            Double minPointDistance = Double.MAX_VALUE;
            int clearX = 0;
            int clearY = 0;
            for (EntityID id : road.getBlockades())
            {
                Blockade blockade = (Blockade) this.worldInfo.getEntity(id);
                if (blockade != null && blockade.isApexesDefined())
                {
                    int[] apexes = blockade.getApexes();
                    for (int i = 0; i < (apexes.length - 2); i += 2)
                    {
                        double distance = this.getDistance(agentX, agentY, apexes[i], apexes[i + 1]);
                        if (distance < minPointDistance)
                        {
                            clearBlockade = blockade;
                            minPointDistance = distance;
                            clearX = apexes[i];
                            clearY = apexes[i + 1];
                        }
                    }
                }
            }
            if (clearBlockade != null && minPointDistance < this.clearDistance)
            {
                Vector2D vector = this.scaleClear(this.getVector(agentX, agentY, clearX, clearY));
                clearX = (int) (agentX + vector.getX());
                clearY = (int) (agentY + vector.getY());
                if (this.equalsPoint(this.oldClearX, this.oldClearY, clearX, clearY))
                {
                    if (this.count >= this.forcedMove)
                    {
                        this.count = 0;
                        return new ActionMove(Lists.newArrayList(road.getID()), clearX, clearY);
                    }
                    this.count++;
                }
                this.oldClearX = clearX;
                this.oldClearY = clearY;
                return new ActionClear(clearX, clearY, clearBlockade);
            }
        }
        return new ActionMove(Lists.newArrayList(position.getID(), target.getID()));
    }

    private Action getIntersectEdgeAction(double agentX, double agentY, Edge edge, Road road)
    {
        double midX = (edge.getStartX() + edge.getEndX()) / 2;
        double midY = (edge.getStartY() + edge.getEndY()) / 2;
        return this.getIntersectEdgeAction(agentX, agentY, midX, midY, road);
    }

    private Action getIntersectEdgeAction(double agentX, double agentY, double pointX, double pointY, Road road)
    {
        Set<Point2D> movePoints = this.getMovePoints(road);
        Point2D bestPoint = null;
        double bastDistance = Double.MAX_VALUE;
        for (Point2D p : movePoints)
        {
            if (!this.intersect(agentX, agentY, p.getX(), p.getY(), road))
            {
                if (!this.intersect(pointX, pointY, p.getX(), p.getY(), road))
                {
                    double distance = this.getDistance(pointX, pointY, p.getX(), p.getY());
                    if (distance < bastDistance)
                    {
                        bestPoint = p;
                        bastDistance = distance;
                    }
                }
            }
        }
        if (bestPoint != null)
        {
            double pX = bestPoint.getX();
            double pY = bestPoint.getY();
            if (!road.isBlockadesDefined())
            {
                return new ActionMove(Lists.newArrayList(road.getID()), (int) pX, (int) pY);
            }
            ActionClear actionClear = null;
            ActionMove actionMove = null;
            Vector2D vector = this.scaleClear(this.getVector(agentX, agentY, pX, pY));
            int clearX = (int) (agentX + vector.getX());
            int clearY = (int) (agentY + vector.getY());
            vector = this.scaleBackClear(vector);
            int startX = (int) (agentX + vector.getX());
            int startY = (int) (agentY + vector.getY());
            for (Blockade blockade : this.worldInfo.getBlockades(road))
            {
                if (this.intersect(startX, startY, pX, pY, blockade))
                {
                    if (this.intersect(startX, startY, clearX, clearY, blockade))
                    {
                        if (actionClear == null)
                        {
                            actionClear = new ActionClear(clearX, clearY, blockade);
                        }
                        else
                        {
                            if (actionClear.getTarget() != null)
                            {
                                Blockade another = (Blockade) this.worldInfo.getEntity(actionClear.getTarget());
                                if (another != null && this.intersect(blockade, another))
                                {
                                    return new ActionClear(another);
                                }
                            }
                            return actionClear;
                        }
                    }
                    else if (actionMove == null)
                    {
                        actionMove = new ActionMove(Lists.newArrayList(road.getID()), (int) pX, (int) pY);
                    }
                }
            }
            if (actionClear != null)
            {
                return actionClear;
            }
            else if (actionMove != null)
            {
                return actionMove;
            }
        }
        Action action = this.getAreaClearAction((PoliceForce) this.agentInfo.me(), road);
        if (action == null)
        {
            action = new ActionMove(Lists.newArrayList(road.getID()), (int) pointX, (int) pointY);
        }
        return action;
    }

    private boolean equalsPoint(double p1X, double p1Y, double p2X, double p2Y)
    {
        return this.equalsPoint(p1X, p1Y, p2X, p2Y, 1000.0D);
    }

    private boolean equalsPoint(double p1X, double p1Y, double p2X, double p2Y, double range)
    {
        return (p2X - range < p1X && p1X < p2X + range) && (p2Y - range < p1Y && p1Y < p2Y + range);
    }

    private boolean isInside(double pX, double pY, int[] apex)
    {
        Point2D p = new Point2D(pX, pY);
        Vector2D v1 = (new Point2D(apex[apex.length - 2], apex[apex.length - 1])).minus(p);
        Vector2D v2 = (new Point2D(apex[0], apex[1])).minus(p);
        double theta = this.getAngle(v1, v2);

        for (int i = 0; i < apex.length - 2; i += 2)
        {
            v1 = (new Point2D(apex[i], apex[i + 1])).minus(p);
            v2 = (new Point2D(apex[i + 2], apex[i + 3])).minus(p);
            theta += this.getAngle(v1, v2);
        }
        return Math.round(Math.abs((theta / 2) / Math.PI)) >= 1;
    }

    private boolean intersect(double agentX, double agentY, double pointX, double pointY, Area area)
    {
        for (Edge edge : area.getEdges())
        {
            double startX = edge.getStartX();
            double startY = edge.getStartY();
            double endX = edge.getEndX();
            double endY = edge.getEndY();
            if (java.awt.geom.Line2D.linesIntersect(
                    agentX, agentY, pointX, pointY,
                    startX, startY, endX, endY
            ))
            {
                double midX = (edge.getStartX() + edge.getEndX()) / 2;
                double midY = (edge.getStartY() + edge.getEndY()) / 2;
                if (!equalsPoint(pointX, pointY, midX, midY) && !equalsPoint(agentX, agentY, midX, midY))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean intersect(Blockade blockade, Blockade another)
    {
        if (blockade.isApexesDefined() && another.isApexesDefined())
        {
            int[] apexes0 = blockade.getApexes();
            int[] apexes1 = another.getApexes();
            for (int i = 0; i < (apexes0.length - 2); i += 2)
            {
                for (int j = 0; j < (apexes1.length - 2); j += 2)
                {
                    if (java.awt.geom.Line2D.linesIntersect(
                            apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
                            apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3]
                    ))
                    {
                        return true;
                    }
                }
            }
            for (int i = 0; i < (apexes0.length - 2); i += 2)
            {
                if (java.awt.geom.Line2D.linesIntersect(
                        apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
                        apexes1[apexes1.length - 2], apexes1[apexes1.length - 1], apexes1[0], apexes1[1]
                ))
                {
                    return true;
                }
            }
            for (int j = 0; j < (apexes1.length - 2); j += 2)
            {
                if (java.awt.geom.Line2D.linesIntersect(
                        apexes0[apexes0.length - 2], apexes0[apexes0.length - 1], apexes0[0], apexes0[1],
                        apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3]
                ))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean intersect(double agentX, double agentY, double pointX, double pointY, Blockade blockade)
    {
        List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(blockade.getApexes()), true);
        for (Line2D line : lines)
        {
            Point2D start = line.getOrigin();
            Point2D end = line.getEndPoint();
            double startX = start.getX();
            double startY = start.getY();
            double endX = end.getX();
            double endY = end.getY();
            if (java.awt.geom.Line2D.linesIntersect(
                    agentX, agentY, pointX, pointY,
                    startX, startY, endX, endY
            ))
            {
                return true;
            }
        }
        return false;
    }

    private double getDistance(double fromX, double fromY, double toX, double toY)
    {
        double dx = toX - fromX;
        double dy = toY - fromY;
        return Math.hypot(dx, dy);
    }

    private double getAngle(Vector2D v1, Vector2D v2)
    {
        double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
        double angle = Math.acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
        if (flag > 0)
        {
            return angle;
        }
        if (flag < 0)
        {
            return -1 * angle;
        }
        return 0.0D;
    }

    private Vector2D getVector(double fromX, double fromY, double toX, double toY)
    {
        return (new Point2D(toX, toY)).minus(new Point2D(fromX, fromY));
    }

    private Vector2D scaleClear(Vector2D vector)
    {
        return vector.normalised().scale(this.clearDistance);
    }

    private Vector2D scaleBackClear(Vector2D vector)
    {
        return vector.normalised().scale(-510);
    }

    private Set<Point2D> getMovePoints(Road road)
    {
        Set<Point2D> points = this.movePointCache.get(road.getID());
        if (points == null)
        {
            points = new HashSet<>();
            int[] apex = road.getApexList();
            for (int i = 0; i < apex.length; i += 2)
            {
                for (int j = i + 2; j < apex.length; j += 2)
                {
                    double midX = (apex[i] + apex[j]) / 2;
                    double midY = (apex[i + 1] + apex[j + 1]) / 2;
                    if (this.isInside(midX, midY, apex))
                    {
                        points.add(new Point2D(midX, midY));
                    }
                }
            }
            for (Edge edge : road.getEdges())
            {
                double midX = (edge.getStartX() + edge.getEndX()) / 2;
                double midY = (edge.getStartY() + edge.getEndY()) / 2;
                points.remove(new Point2D(midX, midY));
            }
            this.movePointCache.put(road.getID(), points);
        }
        return points;
    }

    private boolean needRest(Human agent)
    {
        int hp = agent.getHP();
        int damage = agent.getDamage();
        if (damage == 0 || hp == 0)
        {
            return false;
        }
        int activeTime = (hp / damage) + ((hp % damage) != 0 ? 1 : 0);
        if (this.kernelTime == -1)
        {
            try
            {
                this.kernelTime = this.scenarioInfo.getKernelTimesteps();
            }
            catch (NoSuchConfigOptionException e)
            {
                this.kernelTime = -1;
            }
        }
        return damage >= this.thresholdRest || (activeTime + this.agentInfo.getTime()) < this.kernelTime;
    }

    private Action calcRest(Human human, PathPlanning pathPlanning, Collection<EntityID> targets)
    {
        EntityID position = human.getPosition();
        Collection<EntityID> refuges = this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE);
        int currentSize = refuges.size();
        if (refuges.contains(position))
        {
            return new ActionRest();
        }
        List<EntityID> firstResult = null;
        while (refuges.size() > 0)
        {
            pathPlanning.setFrom(position);
            pathPlanning.setDestination(refuges);
            List<EntityID> path = pathPlanning.calc().getResult();
            if (path != null && path.size() > 0)
            {
                if (firstResult == null)
                {
                    firstResult = new ArrayList<>(path);
                    if (targets == null || targets.isEmpty())
                    {
                        break;
                    }
                }
                EntityID refugeID = path.get(path.size() - 1);
                pathPlanning.setFrom(refugeID);
                pathPlanning.setDestination(targets);
                List<EntityID> fromRefugeToTarget = pathPlanning.calc().getResult();
                if (fromRefugeToTarget != null && fromRefugeToTarget.size() > 0)
                {
                    return new ActionMove(path);
                }
                refuges.remove(refugeID);
                //remove failed
                if (currentSize == refuges.size())
                {
                    break;
                }
                currentSize = refuges.size();
            }
            else
            {
                break;
            }
        }
        return firstResult != null ? new ActionMove(firstResult) : null;
    }
}
