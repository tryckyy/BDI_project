package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

class SimulationPanel extends JPanel {
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<TrafficLight> trafficLights = new ArrayList<>();
    private List<Road> roads = new ArrayList<>();

    private enum Phase { HORIZONTAL_GREEN, HORIZONTAL_ORANGE, ALL_RED, VERTICAL_GREEN, VERTICAL_ORANGE }
    private Phase currentPhase = Phase.HORIZONTAL_GREEN;
    private int phaseTimer = 0;
    private static final int GREEN_DURATION = 200; // Adjust as needed
    private static final int ORANGE_DURATION = 50;
    private static final int ALL_RED_DURATION = 30;

    public SimulationPanel() {
        initializeComponents();
    }


    private void initializeComponents() {
        // Création des routes
        Road horizontalRoad = new Road(100, 300, 600, Color.GRAY, true, false);
        Road horizontalRoad2 = new Road(100, 320, 600, Color.DARK_GRAY, true, true);
        Road verticalRoad = new Road(390, 100, 400, Color.GRAY, false, true);
        Road verticalRoad2 = new Road(410, 100, 400, Color.DARK_GRAY, false, false);
        System.out.println("Vertical road " + verticalRoad);

        // Création des routes avec virages
        List<RoadSegment> rightTurnSegments = new ArrayList<>();
        rightTurnSegments.add(new RoadSegment(700, 300, 100, true));
        rightTurnSegments.add(new RoadSegment(800, 300, 100, false));
        Road rightTurnRoad = new Road(rightTurnSegments, Color.GRAY);

        List<RoadSegment> sTurnSegments = new ArrayList<>();
        sTurnSegments.add(new RoadSegment(410, 500, 100, false));
        sTurnSegments.add(new RoadSegment(410, 600, 100, true));
        Road sTurnRoad = new Road(sTurnSegments, Color.DARK_GRAY);

        // Lier les routes principales aux routes avec virages
        horizontalRoad.setNextRoad(rightTurnRoad);   // Route horizontale 1 → Virage à droite
        verticalRoad2.setNextRoad(sTurnRoad);         // Route verticale 1 → Virage en S

        // Lier les routes avec virages aux routes principales (pour boucler)
        rightTurnRoad.setNextRoad(horizontalRoad);   // Après le virage, retour à la route horizontale
        sTurnRoad.setNextRoad(verticalRoad2);         // Après le virage, retour à la route verticale

        verticalRoad.setPairedRoad(verticalRoad2);
        verticalRoad2.setPairedRoad(verticalRoad);
        horizontalRoad.setPairedRoad(horizontalRoad2);
        horizontalRoad2.setPairedRoad(horizontalRoad);

        // Ajouter toutes les routes à la liste
        roads.addAll(List.of(
                horizontalRoad, horizontalRoad2,
                verticalRoad, verticalRoad2,
                rightTurnRoad, sTurnRoad
        ));

        int offset = 50;

        // Feux verticaux (Nord/Sud) placés avant l'intersection
        trafficLights.add(new TrafficLight(
                370, 300 - offset, // Nord sur route verticale gauche
                TrafficLight.Direction.VERTICAL,
                TrafficLight.State.RED
        ));

        trafficLights.add(new TrafficLight(
                430, 300 - offset,
                TrafficLight.Direction.VERTICAL,
                TrafficLight.State.RED
        ));




        // Feux horizontaux (Est/Ouest) placés avant l'intersection
        trafficLights.add(new TrafficLight(
                400 - offset, 280, // Ouest sur route horizontale haute
                TrafficLight.Direction.HORIZONTAL,
                TrafficLight.State.GREEN
        ));

        // Feux horizontaux (Est/Ouest) placés avant l'intersection
        trafficLights.add(new TrafficLight(
                400 - offset, 340, // Ouest sur route horizontale haute
                TrafficLight.Direction.HORIZONTAL,
                TrafficLight.State.GREEN
        ));


        // Création des véhicules
        roads.forEach(road -> {
            for(int i = 0; i < 1; i++) {
                Vehicle v = new Vehicle(road, i * 80, 2, this);
                vehicles.add(v);
            }
        });
    }


    public void updateSimulation() {
        manageTrafficLights();
        vehicles.forEach(Vehicle::update);
    }

    public List<Vehicle> getVehiclesOnRoad(Road road) {
        return Collections.unmodifiableList(
                vehicles.stream()
                        .filter(v -> v.currentRoad.equals(road)) // Utilisez equals() si les routes ont une identité logique
                        .collect(Collectors.toList())
        );
    }

    private void manageTrafficLights() {
        phaseTimer++;
        switch (currentPhase) {
            case HORIZONTAL_GREEN:
                if (phaseTimer >= GREEN_DURATION) {
                    setHorizontalLights(TrafficLight.State.ORANGE);
                    currentPhase = Phase.HORIZONTAL_ORANGE;
                    phaseTimer = 0;
                }
                break;
            case HORIZONTAL_ORANGE:
                if (phaseTimer >= ORANGE_DURATION) {
                    setAllLights(TrafficLight.State.RED);
                    currentPhase = Phase.ALL_RED;
                    phaseTimer = 0;
                }
                break;
            case ALL_RED:
                if (phaseTimer >= ALL_RED_DURATION) {
                    setVerticalLights(TrafficLight.State.GREEN);
                    currentPhase = Phase.VERTICAL_GREEN;
                    phaseTimer = 0;
                }
                break;
            case VERTICAL_GREEN:
                if (phaseTimer >= GREEN_DURATION) {
                    setVerticalLights(TrafficLight.State.ORANGE);
                    currentPhase = Phase.VERTICAL_ORANGE;
                    phaseTimer = 0;
                }
                break;
            case VERTICAL_ORANGE:
                if (phaseTimer >= ORANGE_DURATION) {
                    setAllLights(TrafficLight.State.RED);
                    currentPhase = Phase.HORIZONTAL_GREEN;
                    phaseTimer = 0;
                    setHorizontalLights(TrafficLight.State.GREEN);
                }
                break;
        }
    }

    private void setHorizontalLights(TrafficLight.State state) {
        trafficLights.stream()
                .filter(light -> light.getDirection() == TrafficLight.Direction.HORIZONTAL)
                .forEach(light -> light.setState(state));
    }

    private void setVerticalLights(TrafficLight.State state) {
        trafficLights.stream()
                .filter(light -> light.getDirection() == TrafficLight.Direction.VERTICAL)
                .forEach(light -> light.setState(state));
    }

    private void setAllLights(TrafficLight.State state) {
        trafficLights.forEach(light -> light.setState(state));
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.WHITE);

        // Dessin des éléments
        roads.forEach(r -> r.draw(g));
        trafficLights.forEach(t -> t.draw(g));
        vehicles.forEach(v -> v.draw(g));
    }

    public List<Vehicle> getNearbyVehicles(Vehicle requester) {
        Point requesterPos = requester.getPosition();
        return vehicles.stream()
                .filter(v -> v != requester)
                .filter(v -> v.getPosition().distance(requesterPos) < 50)
                .collect(Collectors.toList());
    }

    public List<Vehicle> getVehiclesInLane(Vehicle requester) {
        return vehicles.stream()
                .filter(v -> v != requester)
                .filter(v -> v.currentRoad == requester.currentRoad)
                .filter(v -> Math.abs(v.laneOffset - requester.laneOffset) < 15)
                .collect(Collectors.toList());
    }


    public Optional<TrafficLight> getNextTrafficLight(Vehicle vehicle) {
        return trafficLights.stream()
                // Filtrer les feux pertinents pour le véhicule
                .filter(light ->
                        light.isInPath(vehicle.getFrontPosition(), vehicle.currentRoad.isHorizontal())
                )
                // Trier par distance croissante
                .min(Comparator.comparingDouble(light ->
                        vehicle.getPosition().distance(light.getPosition())
                ));
    }
}