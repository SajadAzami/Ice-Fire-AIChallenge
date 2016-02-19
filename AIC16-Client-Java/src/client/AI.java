package client;

import client.model.Node;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

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

    public void doTurn(World world) {

        Node[] myNodes = world.getMyNodes();
        Node[] enemyNodes = world.getOpponentNodes();
        int myID = world.getMyID();
        int totalTurns = world.getTotalTurns();
        int turnNumber = world.getTurnNumber();
        long totalTurnTime = world.getTotalTurnTime();
        ArrayList<Integer> freedomPoints = getFreedomPoints(world);
        System.out.println(myID);
        ArrayList<Integer> dangerPoints = getDangerPoints(world);

        for (Node node : myNodes) {
            Node[] neighbours = node.getNeighbours();
            boolean isMoved = false;
            System.out.print("My Node " + node.getArmyCount() + " (index" + node.getIndex()
                    + "), Has " + getEnemiesNearbyCount(node, world)
                    + " Enemy power(" + getEnemiesNearbyPower(node, world)
                    + "), Has " + getFriendsNearbyCount(node, world)
                    + " Friend power(" + getFriendsNearbyPower(node, world) + ")");
            System.out.println("");

            //Move Section
            if (neighbours.length > 0) {
                //1.We Have Enemies Nearby, Good, That means we have stood up for something!
                // If myPow >= ePow/2, Attack the one with most danger point
                // if myPow < ePow/2 && i've got a friend with more danger point around, help him half of me
                // if myPow < ePow/2 && i've got no friend around, fill an empty with 0.2 if me
                // if non, isMoved = true, because i want to stay here and defend my land!
                if (!isMoved && getEnemiesNearbyCount(node, world) != 0) {
                    ArrayList<Node> sortedEnemies = getEnemiesNearby(node, world);
                    ArrayList<Node> nearbyEnemiesDangerPoints = new ArrayList<>();

                    for (Node sortedEnemy : sortedEnemies) {
                        if (node.getArmyCount() >= getNormalizedPower(sortedEnemy, world) / 2) {
                            world.moveArmy(node.getIndex(), sortedEnemy.getIndex(), (int) (node.getArmyCount() * 0.8));
                            isMoved = true;
                            break;
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
                    for (int i = 0; i < neighboursFreedomPoints.length; i++) {
                        if (world.getMap().getNodes()[neighboursIndex[i]].getOwner() != myID && neighboursIndex[i] > 0) {
                            world.moveArmy(node.getIndex(), neighboursIndex[i], (int) (node.getArmyCount() * 0.8));
                            isMoved = true;
                            break;
                        }
                    }
                }

                //3.Couldn't Do anything, fuck it! I decided to move random!
                if (!isMoved) {
                    Node destination = neighbours[(int) (neighbours.length * Math.random())];
                    world.moveArmy(node, destination, 1);
                    System.out.println("Randomeddddddddddddd");
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
     * @return 0 = weak, 1 medium, 2 strong
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
                normalized = 5;
                break;
            case 1:
                if (averageEnergy <= 10) {
                    normalized = averageEnergy;
                } else {
                    normalized = averageEnergy * (world.getOpponentNodes().length / myNodes.length);
                }
                break;
            case 2:
                if (averageEnergy <= 30) {
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
            case 0:
                normalized = 5;
                break;
            case 1:
                if (averageEnergy <= 10) {
                    normalized = averageEnergy;
                } else {
                    normalized = averageEnergy * (world.getOpponentNodes().length / myNodes.length);
                }
                break;
            case 2:
                if (averageEnergy <= 30) {
                    normalized = averageEnergy;
                } else {
                    normalized = averageEnergy * (world.getOpponentNodes().length / myNodes.length);
                }
                break;
            default:
                normalized = 0;
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


}
