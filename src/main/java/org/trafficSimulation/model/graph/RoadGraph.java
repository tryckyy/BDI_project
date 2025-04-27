package org.trafficSimulation.model.graph;

import org.trafficSimulation.model.road.Road;
import org.trafficSimulation.model.road.RoadSegment;

import java.awt.*;
import java.util.*;
import java.util.List;



public class RoadGraph {
    private final Map<Point, Node> nodes;

    public RoadGraph() {
        this.nodes = new HashMap<>();
    }

    private Node getOrCreateNode(Point position) {
        return nodes.computeIfAbsent(position, Node::new);
    }

    public static RoadGraph buildFromRoads(List<Road> roads) {
        RoadGraph graph = new RoadGraph();
        final int LANE_CHANGE_PENALTY = 5; // Pénalité pour changement de voie

        // Première passe: créer tous les nœuds
        for (Road road : roads) {
            Point startPoint = getStartPoint(road);
            Point endPoint = getEndPoint(road);

            if (startPoint != null) graph.getOrCreateNode(startPoint);
            if (endPoint != null) graph.getOrCreateNode(endPoint);

            // Ajouter des points intermédiaires pour les changements de voie
            Road pairedRoad = road.getPairedRoad();
            if (pairedRoad != null) {
                // Créer des points intermédiaires le long de la route (par exemple, tous les 50 pixels)
                int step = 50;
                for (int pos = step; pos < road.getLength(); pos += step) {
                    Point intermediatePoint = road.getPosition(pos, 0);
                    if (intermediatePoint != null) {
                        graph.getOrCreateNode(intermediatePoint);
                    }
                }
            }
        }

        // Deuxième passe: créer toutes les arêtes
        for (Road road : roads) {
            Point startPoint = getStartPoint(road);
            Point endPoint = getEndPoint(road);

            if (startPoint == null || endPoint == null) continue;

            Node startNode = graph.getOrCreateNode(startPoint);
            Node endNode = graph.getOrCreateNode(endPoint);

            // Arête principale (route normale)
            Edge edge = new Edge(road, road.getLength());
            startNode.addNeighbor(endNode, edge);

            // Connexions aux routes jumelées (changements de voie)
            Road pairedRoad = road.getPairedRoad();
            if (pairedRoad != null) {
                Point pairedStart = getStartPoint(pairedRoad);
                Point pairedEnd = getEndPoint(pairedRoad);

                if (pairedStart != null && pairedEnd != null) {
                    Node pairedStartNode = graph.getOrCreateNode(pairedStart);
                    Node pairedEndNode = graph.getOrCreateNode(pairedEnd);

                    // Connexion bidirectionnelle entre les routes jumelées aux extrémités
                    startNode.addNeighbor(pairedStartNode, new Edge(pairedRoad, LANE_CHANGE_PENALTY));
                    pairedStartNode.addNeighbor(startNode, new Edge(road, LANE_CHANGE_PENALTY));

                    endNode.addNeighbor(pairedEndNode, new Edge(pairedRoad, LANE_CHANGE_PENALTY));
                    pairedEndNode.addNeighbor(endNode, new Edge(road, LANE_CHANGE_PENALTY));


                    int step = 50;
                    for (int pos = step; pos < road.getLength(); pos += step) {
                        Point point1 = road.getPosition(pos, 0);
                        Point point2 = pairedRoad.getPosition(pos, 0);

                        if (point1 != null && point2 != null) {
                            Node node1 = graph.getOrCreateNode(point1);
                            Node node2 = graph.getOrCreateNode(point2);

                            // Connexion bidirectionnelle aux points intermédiaires
                            node1.addNeighbor(node2, new Edge(pairedRoad, LANE_CHANGE_PENALTY));
                            node2.addNeighbor(node1, new Edge(road, LANE_CHANGE_PENALTY));
                        }
                    }
                }
            }

            // Connexions aux routes suivantes
            for (Road nextRoad : road.getNextRoads()) {
                Point nextStart = getStartPoint(nextRoad);
                if (nextStart != null) {
                    Node nextStartNode = graph.getOrCreateNode(nextStart);
                    Edge connectionEdge = new Edge(nextRoad, 0); // Pas de pénalité pour les connexions normales
                    endNode.addNeighbor(nextStartNode, connectionEdge);
                }
            }
        }

        return graph;
    }

    public static Point getStartPoint(Road road) {
        if (road.getSegments().isEmpty()) return null;

        RoadSegment segment = road.isReverse() ?
                road.getSegments().get(road.getSegments().size() - 1) :
                road.getSegments().get(0);

        if (road.isHorizontal()) {
            return new Point(
                    road.isReverse() ? segment.getX() + segment.getLength() : segment.getX(),
                    segment.getY()
            );
        } else {
            return new Point(
                    segment.getX() ,
                    road.isReverse() ? segment.getY() + segment.getLength() : segment.getY()
            );
        }
    }

    public static Point getEndPoint(Road road) {
        if (road.getSegments().isEmpty()) return null;

        RoadSegment segment = road.isReverse() ?
                road.getSegments().get(0) :
                road.getSegments().get(road.getSegments().size() - 1);

        if (road.isHorizontal()) {
            return new Point(
                    road.isReverse() ? segment.getX() : segment.getX() + segment.getLength(),
                    segment.getY()
            );
        } else {
            return new Point(
                    segment.getX() ,
                    road.isReverse() ? segment.getY() : segment.getY() + segment.getLength()
            );
        }
    }




    public List<Road> findShortestPath(Point start, Point goal) {
        Node startNode = getOrCreateNode(start);
        Node goalNode = getOrCreateNode(goal);

        // Initialisation
        Map<Node, Double> distances = new HashMap<>();
        Map<Node, Node> previousNodes = new HashMap<>();
        Map<Node, Road> incomingRoads = new HashMap<>(); // Pour suivre la route utilisée pour arriver à chaque nœud
        PriorityQueue<NodeWithDistance> queue = new PriorityQueue<>(Comparator.comparingDouble(NodeWithDistance::getDistance));
        Set<Node> visited = new HashSet<>();

        for (Node node : nodes.values()) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }
        distances.put(startNode, 0.0);
        queue.add(new NodeWithDistance(startNode, 0.0));

        // Algorithme de Dijkstra amélioré
        while (!queue.isEmpty()) {
            NodeWithDistance current = queue.poll();
            Node currentNode = current.getNode();

            if (currentNode.equals(goalNode)) {
                return reconstructPath(previousNodes, incomingRoads, goalNode);
            }

            if (visited.contains(currentNode)) continue;
            visited.add(currentNode);

            for (Map.Entry<Node, Edge> neighborEntry : currentNode.getNeighbors().entrySet()) {
                Node neighbor = neighborEntry.getKey();
                Edge edge = neighborEntry.getValue();
                Road road = edge.getRoad();

                if (visited.contains(neighbor)) continue;

                double newDistance = distances.get(currentNode) + edge.getWeight();

                // Pénalité supplémentaire si changement de voie
                Road incomingRoad = incomingRoads.get(currentNode);
                if (incomingRoad != null && road != null &&
                        !road.equals(incomingRoad) && road.getPairedRoad() != null &&
                        road.getPairedRoad().equals(incomingRoad)) {
                    newDistance += 10; // Pénalité pour changement de voie
                }

                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    previousNodes.put(neighbor, currentNode);
                    incomingRoads.put(neighbor, road); // Enregistrer la route utilisée
                    queue.add(new NodeWithDistance(neighbor, newDistance));
                }
            }
        }

        return Collections.emptyList(); // Aucun chemin trouvé
    }

    private List<Road> reconstructPath(Map<Node, Node> previousNodes, Map<Node, Road> incomingRoads, Node goalNode) {
        LinkedList<Road> path = new LinkedList<>();
        Node current = goalNode;

        while (current != null && previousNodes.containsKey(current)) {
            Road road = incomingRoads.get(current);
            if (road != null) {
                path.addFirst(road);
            }
            current = previousNodes.get(current);
        }

        // Simplifier le chemin en enlevant les doublons consécutifs
        List<Road> simplifiedPath = new ArrayList<>();
        Road lastRoad = null;
        for (Road road : path) {
            if (!road.equals(lastRoad)) {
                simplifiedPath.add(road);
                lastRoad = road;
            }
        }

        return simplifiedPath;
    }

    private static class NodeWithDistance {
        private final Node node;
        private final double distance;

        public NodeWithDistance(Node node, double distance) {
            this.node = node;
            this.distance = distance;
        }

        public Node getNode() { return node; }
        public double getDistance() { return distance; }
    }
}