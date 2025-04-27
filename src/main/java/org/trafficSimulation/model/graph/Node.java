package org.trafficSimulation.model.graph;

import org.trafficSimulation.model.road.Road;

import java.awt.Point;
import java.util.*;

class Node {
    private final Point position;
    private Road road;
    private final Map<Node, Edge> neighbors;

    public Node(Point position) {
        this.position = position;
        this.neighbors = new HashMap<>();
        this.road = getRoad();
    }

    public Road getRoad () {
        return road;
    }


    public void addNeighbor(Node destination, Edge edge) {
        neighbors.put(destination, edge);
    }

    public Map<Node, Edge> getNeighbors() {
        return Collections.unmodifiableMap(neighbors);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return position.equals(node.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position);
    }

    @Override
    public String toString() {
        return "Node(" + position.x + "," + position.y + ")";
    }
}