package client;

import client.model.Graph;
import client.model.Node;

import java.util.*;

/**
 * AI class.
 * You should fill body of the method {@link #doTurn}.
 * Do not change name or modifiers of the methods or fields
 * and do not add constructor for this class.
 * You can add as many methods or fields as you want!
 * Use world parameter to access and modify game's
 * world!
 * See World interface for more details.
 */
public class AI {

    private boolean isFound;
    private List<Integer> frontEnd = new ArrayList<>();
    private HashMap<Integer, Integer> parents = new HashMap<>();

    public void doTurn(World world) {

        Node[] myNodes = world.getMyNodes();
        Node[] enemyNodes = world.getOpponentNodes();
        int myID = world.getMyID();
        int totalTurns = world.getTotalTurns();
        int turnNumber = world.getTurnNumber();
        long totalTurnTime = world.getTotalTurnTime();
        ArrayList<Integer> freedomPoints = getFreedomPoints(world);
        ArrayList<Integer> dangerPoints = getDangerPoints(world);
        ArrayList<Integer> frontLines = getFrontLines(world);

        for (Node node : myNodes) {
            Node[] neighbours = node.getNeighbours();
            boolean isMoved = false;

            //Logs
            /**
             System.out.print("My Node " + node.getArmyCount() + " (index" + node.getIndex()
             + "), Has " + getEnemiesNearbyCount(node, world)
             + " Enemy power(" + getEnemiesNearbyPower(node, world)
             + "), Has " + getFriendsNearbyCount(node, world)
             + " Friend power(" + getFriendsNearbyPower(node, world) + ")");
             System.out.println("");
             */

            //Move Section
            if (neighbours.length > 0) {
                //1.We Have Enemies Nearby, Good, That means we have stood up for something!
                // If myPow >= ePow/2, Attack the one with most danger point
                // if myPow < ePow/2 && i've got a friend with more danger point around, help him half of me
                // if myPow < ePow/2 && i've got no friend around, fill an empty with 0.2 if me
                // if non, isMoved = true, because i want to stay here and defend my land!
                if (!isMoved && getEnemiesNearbyCount(node, world) != 0) {
                    ArrayList<Node> sortedEnemies = getEnemiesNearby(node, world);
                    ArrayList<Node> sameSizeEnemies = new ArrayList<>();
                    Node keyEnemy = sortedEnemies.get(0);//Key enemy to find the big same size enemies

                    //Find biggest enemy that we can attack and assign it as key
                    for (Node sortedEnemy : sortedEnemies) {
                        if (node.getArmyCount() >= (2 * getNormalizedPower(sortedEnemy, world)) / 3) {
                            keyEnemy = sortedEnemy;
                            break;
                        }
                    }

                    //If we have big and same size enemies, add it to sameSizeEnemies
                    for (int i = 1; i < sortedEnemies.size(); i++) {
                        if (getNormalizedPower(sortedEnemies.get(i), world) == getNormalizedPower(keyEnemy, world)) {
                            sameSizeEnemies.add(sortedEnemies.get(i));
                        }
                    }

                    //Sort same size enemies by danger point in ascending order
                    for (int j = 0; j < sameSizeEnemies.size() - 1; j++) {
                        for (int i = 0; i < sameSizeEnemies.size() - 1; i++) {
                            if (dangerPoints.get(sameSizeEnemies.get(i).getIndex())
                                    >= dangerPoints.get(sameSizeEnemies.get(i + 1).getIndex())) {
                                Node temp = sameSizeEnemies.get(i);
                                sameSizeEnemies.set(i, sameSizeEnemies.get(i + 1));
                                sameSizeEnemies.set(i + 1, temp);
                            }
                        }
                    }

                    //If there are same sized enemies, attack the one with the least support
                    if (sameSizeEnemies.size() > 0 && node.getArmyCount() >= (2 * getNormalizedPower(sortedEnemies.get(0), world)) / 3) {
                        world.moveArmy(node.getIndex(), sameSizeEnemies.get(0).getIndex(), (int) (node.getArmyCount() * 0.8));
                        isMoved = true;
                    }

                    if (!isMoved) {
                        for (Node sortedEnemy : sortedEnemies) {
                            if (node.getArmyCount() >= (2 * getNormalizedPower(sortedEnemy, world)) / 3) {
                                world.moveArmy(node.getIndex(), sortedEnemy.getIndex(), (int) (node.getArmyCount() * 0.8));
                                isMoved = true;
                                break;
                            }
                        }
                    }
                    if (!isMoved) {
                        for (Node neighbour : neighbours) {
                            if (neighbour.getOwner() == myID && dangerPoints.get(neighbour.getIndex()) >
                                    dangerPoints.get(node.getIndex())) {
                                world.moveArmy(node.getIndex(), neighbour.getIndex(), (int) (node.getArmyCount() * 0.3));
                                isMoved = true;
                                break;
                            }
                        }
                    }
                }

                //2.No Enemy, Move to the most freedom, No freedom? Get out of my if!
                if (!isMoved && getEnemiesNearbyCount(node, world) == 0) {
                    int[] neighboursFreedomPoints = new int[neighbours.length];
                    int[] neighboursIndex = new int[neighbours.length];
                    for (int i = 0; i < neighbours.length; i++) {
                        neighboursFreedomPoints[i] = freedomPoints.get(neighbours[i].getIndex());
                        neighboursIndex[i] = neighbours[i].getIndex();
                    }
                    //Sort the freedom points
                    for (int j = 0; j < neighboursFreedomPoints.length - 1; j++) {
                        for (int i = 0; i < neighboursFreedomPoints.length - 1; i++) {
                            if (neighboursFreedomPoints[i] < neighboursFreedomPoints[i + 1]) {
                                int temp = neighboursFreedomPoints[i];
                                int tempIndex = neighboursIndex[i];

                                neighboursIndex[i] = neighboursIndex[i + 1];
                                neighboursIndex[i + 1] = tempIndex;
                                neighboursFreedomPoints[i] = neighboursFreedomPoints[i + 1];
                                neighboursFreedomPoints[i + 1] = temp;
                            }
                        }
                    }
                    boolean allFriend = true;
                    for (int i = 0; i < neighboursFreedomPoints.length; i++) {
                        if (world.getMap().getNodes()[neighboursIndex[i]].getOwner() != myID && neighboursIndex[i] > 0) {
                            world.moveArmy(node.getIndex(), neighboursIndex[i], (int) (node.getArmyCount() * 0.8));
                            isMoved = true;
                            allFriend = false;
                            break;
                        }
                    }
                    if (allFriend) {
                        if (frontLines.size() > 0) {
                            //Now help the nearest front line, if there is any
                            int[] dfsResults = new int[frontLines.size()];
                            ArrayList<ArrayList<Node>> dfsResaultsRoute = new ArrayList<>();
                            for (int i = 0; i < frontLines.size(); i++) {
                                //Find All possible DFS to front line
                                ArrayList<Node> temp = findSrc(node, world.getMap().getNodes()[frontLines.get(i)], world.getMap());
                                dfsResults[i] = temp.size();
                                dfsResaultsRoute.add(temp);
                            }
                            //Compare and choose the nearest front liner
                            for (int j = 0; j < dfsResults.length - 1; j++) {
                                for (int i = 0; i < dfsResults.length - 1; i++) {
                                    if (dfsResults[i] >= dfsResults[i + 1]) {
                                        int temp = dfsResults[i];
                                        ArrayList<Node> tempRoute = dfsResaultsRoute.get(i);

                                        dfsResults[i] = dfsResults[i + 1];
                                        dfsResults[i + 1] = temp;

                                        dfsResaultsRoute.set(i, dfsResaultsRoute.get(i + 1));
                                        dfsResaultsRoute.set(i + 1, tempRoute);
                                    }
                                }
                            }
                            world.moveArmy(node.getIndex(), dfsResaultsRoute.get(0).get(1).getIndex(),
                                    (int) (node.getArmyCount() * 0.9));
                            isMoved = true;
                        } else {
                            int smallestIndex = 0;
                            int biggest = 100000000;
                            for (Node neighbour : neighbours) {
                                if (biggest > neighbour.getArmyCount()) {
                                    biggest = neighbour.getArmyCount();
                                    smallestIndex = neighbour.getIndex();
                                }
                            }
                            world.moveArmy(node.getIndex(), smallestIndex, (int) (node.getArmyCount() * 0.5));
                            isMoved = true;
                        }
                    }
                }

                //3.Couldn't Do anything, fuck it! I decided to move random!
                if (!isMoved) {
                    Node destination = neighbours[(int) (neighbours.length * Math.random())];
                    world.moveArmy(node, destination, (int) (node.getArmyCount() * 0.8));
                }
            }
        }
        System.out.println("#################");

    }

    /**
     * return the approximate sum of nearby enemies power
     *
     * @param node  target node
     * @param world
     * @return normalized average power of surrounding enemies
     */
    private int getEnemiesNearbyPower(Node node, World world) {
        Node[] neighbours = node.getNeighbours();
        int myId = world.getMyID();
        int enemyId = 1 - myId;
        int enemiesPower = 0;
        for (Node neighbour : neighbours) {
            if (neighbour.getOwner() == enemyId)
                enemiesPower += neighbour.getArmyCount();
        }
        if (getEnemiesNearbyCount(node, world) == 0) {
            enemiesPower = -1;
        }
        //TODO should be normalized
        /** Use {@link World#getLowArmyBound()} & {@link World#getMediumArmyBound()} */
        int normalized = 0;
        Node[] myNodes = world.getMyNodes();
        int energySum = 0;
        for (Node myNode : myNodes) {
            energySum += myNode.getArmyCount();
        }
        int averageEnergy = energySum / myNodes.length;
        switch (enemiesPower) {
            case -1:
                normalized = 0;
                break;
            case 0:
                normalized = world.getLowArmyBound() / 2;
                break;
            case 1:
                if (averageEnergy <= (world.getMediumArmyBound() + world.getLowArmyBound()) / 2) {
                    normalized = averageEnergy;
                } else {
                    normalized = averageEnergy * (world.getOpponentNodes().length / myNodes.length);
                }
                break;
            case 2:
                if (averageEnergy <= world.getMediumArmyBound()) {
                    normalized = averageEnergy;
                } else {
                    normalized = averageEnergy * (world.getOpponentNodes().length / myNodes.length);
                }
                break;
        }

        return normalized;
    }


    /**
     * return the normalized power of enemies
     *
     * @param node  target node
     * @param world
     * @return power of enemies
     */
    private int getNormalizedPower(Node node, World world) {
        int normalized = 0;
        Node[] myNodes = world.getMyNodes();
        int energySum = 0;
        for (Node myNode : myNodes) {
            energySum += myNode.getArmyCount();
        }
        int averageEnergy = energySum / myNodes.length;
        switch (node.getArmyCount()) {
            case -1:
                normalized = 0;
                break;
            case 0:
                normalized = world.getLowArmyBound() / 2;
                break;
            case 1:
                if (averageEnergy <= (world.getMediumArmyBound() + world.getLowArmyBound()) / 2) {
                    normalized = averageEnergy;
                } else {
                    normalized = averageEnergy * (world.getOpponentNodes().length / myNodes.length);
                }
                break;
            case 2:
                if (averageEnergy <= world.getMediumArmyBound()) {
                    normalized = averageEnergy;
                } else {
                    normalized = averageEnergy * (world.getOpponentNodes().length / myNodes.length);
                }
                break;
        }
        return normalized;
    }


    /**
     * return the number of nearby enemies
     *
     * @param node  target node
     * @param world
     * @return number of enemies
     */
    private int getEnemiesNearbyCount(Node node, World world) {
        Node[] neighbours = node.getNeighbours();
        int myId = world.getMyID();
        int enemyId = 1 - myId;
        int enemiesCount = 0;
        for (Node neighbour : neighbours) {
            if (neighbour.getOwner() == enemyId)
                enemiesCount++;
        }
        return enemiesCount;
    }

    /**
     * return the number of friends enemies
     *
     * @param node  target node
     * @param world
     * @return number of enemies
     */
    private int getFriendsNearbyCount(Node node, World world) {
        Node[] neighbours = node.getNeighbours();
        int myId = world.getMyID();
        int friendsCount = 0;
        for (Node neighbour : neighbours) {
            if (neighbour.getOwner() == myId)
                friendsCount++;
        }
        return friendsCount;
    }


    /**
     * return the approximate sum of nearby friends power
     *
     * @param node  target node
     * @param world
     * @return sum of friedly power
     */
    private int getFriendsNearbyPower(Node node, World world) {
        Node[] neighbours = node.getNeighbours();
        int myId = world.getMyID();
        int friendlyPower = 0;
        for (Node neighbour : neighbours) {
            if (neighbour.getOwner() == myId)
                friendlyPower += neighbour.getArmyCount();
        }
        return friendlyPower;
    }


    /**
     * return the number of edges of a node
     *
     * @param node  target node
     * @param world
     * @return number of edges
     */
    private int getEdgesCount(Node node, World world) {
        return node.getNeighbours().length;
    }

    /**
     * return the enemies nearby in a descending sorted ArrayList by their power
     *
     * @param node  target node
     * @param world
     * @return enemies list
     */
    private ArrayList<Node> getEnemiesNearby(Node node, World world) {
        ArrayList<Node> enemies = new ArrayList<>();
        Node[] neighbours = node.getNeighbours();
        int myId = world.getMyID();
        for (Node neighbour : neighbours) {
            if (neighbour.getOwner() != myId)
                enemies.add(neighbour);
        }
        for (int j = 0; j < enemies.size() - 1; j++) {
            for (int i = 0; i < enemies.size() - 1; i++) {
                if (enemies.get(i).getArmyCount() < enemies.get(i + 1).getArmyCount()) {
                    Node temp = enemies.get(i);
                    enemies.set(i, enemies.get(i + 1));
                    enemies.set(i + 1, temp);
                }
            }
        }
        return enemies;
    }

    /**
     * return list of freedom points
     *
     * @param world
     * @return ArrayList of points
     */
    private ArrayList<Integer> getFreedomPoints(World world) {
        ArrayList<Integer> points = new ArrayList<>();
        int myId = world.getMyID();
        int enemyId = 1 - myId;
        Node[] allNodes = world.getMap().getNodes();
        for (Node allNode : allNodes) {
            Node[] temp = allNode.getNeighbours();
            int count = 0;
            for (Node aTemp : temp) {
                if (aTemp.getOwner() == -1) {
                    count++;
                }
            }
            points.add(count);
        }
        return points;
    }


    /**
     * return list of danger points
     *
     * @param world
     * @return ArrayList of points
     */
    private ArrayList<Integer> getDangerPoints(World world) {
        ArrayList<Integer> points = new ArrayList<>();
        int myId = world.getMyID();
        int enemyId = 1 - myId;
        Node[] allNodes = world.getMap().getNodes();
        for (Node allNode : allNodes) {
            if (getEnemiesNearbyCount(allNode, world) == 0) {
                points.add(0);
            } else {
                points.add((getEnemiesNearbyPower(allNode, world) / getEnemiesNearbyCount(allNode, world)));
            }
        }
        return points;
    }

    /**
     * Created by Sajad Azami
     * return index list of nodes in front line
     *
     * @return ArrayList of front line nodes
     */
    public ArrayList<Integer> getFrontLines(World world) {
        ArrayList<Integer> frontLines = new ArrayList<>();
        Node[] myNodes = world.getMyNodes();
        int myId = world.getMyID();
        int enemyId = 1 - myId;
        for (Node myNode : myNodes) {
            Node[] neighbours = myNode.getNeighbours();
            for (Node neighbour : neighbours) {
                if (neighbour.getOwner() == enemyId || neighbour.getOwner() == -1) {
                    frontLines.add(myNode.getIndex());
                    break;
                }
            }
        }
        return frontLines;
    }


    /**
     * Created by Sina Baharlouie
     * return list of nodes to reach a point
     *
     * @param map
     * @return ArrayList of node
     */
    private ArrayList<Node> findSrc(Node src, Node dest, Graph map) {

        frontEnd.clear();
        parents.clear();
        isFound = false;
        frontEnd.add(src.getIndex());

        ArrayList<Integer> Q = new ArrayList<>();
        int head = 0;
        int tail = 0;

        Q.add(src.getIndex());
        tail++;
        int current;
        while (tail != head) {
            current = Q.get(head);
            head++;
            if (current == dest.getIndex()) {
                isFound = true;
                break;
            }

            Node[] neighbours = map.getNode(current).getNeighbours();
            for (Node neighbour : neighbours) {
                if (frontEnd.contains(neighbour.getIndex()))
                    continue;
                else {
                    parents.put(neighbour.getIndex(), current);
                    frontEnd.add(neighbour.getIndex());
                    Q.add(neighbour.getIndex());
                    tail++;
                }
            }
        }
        if (!isFound)
            return null;

        ArrayList<Node> path = new ArrayList<>();
        path.add(dest);
        int cur = dest.getIndex();
        while (cur != src.getIndex()) {
            cur = parents.get(cur);
            path.add(map.getNode(cur));
        }
        Collections.reverse(path);
        return path;

    }
}
