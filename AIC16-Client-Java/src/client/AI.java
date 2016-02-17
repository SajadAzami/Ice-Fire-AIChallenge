package client;

import client.model.Node;

import java.util.ArrayList;

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

        for (Node node : myNodes) {
            Node[] neighbours = node.getNeighbours();

            System.out.print("My Node " + node.getArmyCount() + " (index" + node.getIndex()
                    + "), Has " + getEnemmiesNearbyCount(node, world)
                    + " Enemy power(" + getEnemmiesNearbyPower(node, world)
                    + "), Has " + getFriendsNearbyCount(node, world)
                    + " Friend power(" + getFriendsNearbyPower(node, world) + ")");

            System.out.println("");

            //Simple dummy random move
            if (neighbours.length > 0) {
                // select a random neighbour
                Node destination = neighbours[(int) (neighbours.length * Math.random())];
                // move half of the node's army to the neighbor node
                world.moveArmy(node, destination, node.getArmyCount() / 2);
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
    private int getEnemmiesNearbyPower(Node node, World world) {
        Node[] neighbours = node.getNeighbours();
        int myId = world.getMyID();
        int enemyId = 1 - myId;
        int enemiesPower = 0;
        for (Node neighbour : neighbours) {
            if (neighbour.getOwner() == enemyId)
                enemiesPower += neighbour.getArmyCount();
        }
        //TODO should be normalized
        return enemiesPower;
    }


    /**
     * return the number of nearby enemies
     *
     * @param node  target node
     * @param world
     * @return number of enemies
     */
    private int getEnemmiesNearbyCount(Node node, World world) {
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
        int enemyId = 1 - myId;
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
}
