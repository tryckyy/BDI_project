package org.trafficSimulation.model.graph;

import org.trafficSimulation.model.road.Road;

class Edge {
    private final Road road;
    private final double weight;

    public Edge(Road road, double weight) {
        this.road = road;
        this.weight = weight;
    }

    public Road getRoad() {
        return road;
    }

    public double getWeight() {
        return weight;
    }
}
