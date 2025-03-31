package org.example;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

class SimulationPanel extends JPanel {
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<TrafficLight> trafficLights = new ArrayList<>();
    public List<Road> roads = new ArrayList<>();
    private enum Phase { HORIZONTAL_GREEN, HORIZONTAL_ORANGE, ALL_RED, VERTICAL_GREEN, VERTICAL_ORANGE }
    private Phase currentPhase = Phase.HORIZONTAL_GREEN;
    private int phaseTimer = 0;
    private static final int GREEN_DURATION = 200; // Adjust as needed
    private static final int ORANGE_DURATION = 50;
    private static final int ALL_RED_DURATION = 30;
    private final Map<Vehicle, Long> travelTimes = new HashMap<>(); // Temps de trajet par véhicule
    private final Map<Vehicle, Integer> laneChanges = new HashMap<>(); // Changements de voie par véhicule

    public SimulationPanel() {
        initializeComponents();
    }


    private void initializeComponents() {
        // Création des routes
        Road horizontalRoadLeft = new Road(0, 300, 700, Color.GRAY, true, false);
        Road horizontalRoadRight = new Road(0, 320, 700, Color.DARK_GRAY, true, true);
        Road verticalRoadRight = new Road(390, 0, 500, Color.GRAY, false, true);
        Road verticalRoadLeft = new Road(410, 0, 500, Color.DARK_GRAY, false, false);


        // Virage a gauche sur la route horizontal
        List<RoadSegment> horizontalLeftTurn = new ArrayList<>();
        horizontalLeftTurn.add(new RoadSegment(700, 200, 100, false, true));
        Road horizontalLeftTurnRoad = new Road(horizontalLeftTurn, Color.GRAY, true, false);

        // Virage a gauche sur la route vertical
        List<RoadSegment> verticalLeftTurn = new ArrayList<>();
        verticalLeftTurn.add(new RoadSegment(410, 500, 100, true, false));
        Road verticalLeftTurnRoad = new Road(verticalLeftTurn, Color.DARK_GRAY, false, false);


        Road afterHorizontalLeftTurn = new Road(700, 200, 200, Color.GRAY, true, false);
        Road afterVerticalRightTurn = new Road(290, 500, 200, Color.GRAY, false, true);
        Road afterHorizontalRightTurn = new Road(700, 420, 200, Color.DARK_GRAY, true, true);
        Road afterVerticalLeftTurn = new Road(510, 500, 200, Color.DARK_GRAY, false, false);

        // Virage a droite sur la route horizontal
        List<RoadSegment> horizontalRightTurn = new ArrayList<>();
        horizontalRightTurn.add(new RoadSegment(700, 320, 100, false, false));
        Road horizontalRightTurnRoad = new Road(horizontalRightTurn, Color.DARK_GRAY, false, true);

        List<RoadSegment> verticalRightTurn = new ArrayList<>();
        verticalRightTurn.add(new RoadSegment(290, 500, 100, true, true));
        Road verticalRightTurnRoad = new Road(verticalRightTurn, Color.GRAY, true, true);

        horizontalRoadLeft.addNextRoad(horizontalLeftTurnRoad);
        horizontalLeftTurnRoad.addNextRoad(afterHorizontalLeftTurn);
        afterHorizontalLeftTurn.addNextRoad(horizontalRoadLeft);
        // Connexion virage a droite route vertical
        verticalRoadRight.addNextRoad(verticalRightTurnRoad);
        verticalRightTurnRoad.addNextRoad(afterVerticalRightTurn);
        afterVerticalRightTurn.addNextRoad(verticalRoadRight);

        // Connexion virage a droite route horizontal
        horizontalRoadRight.addNextRoad(horizontalRightTurnRoad);
        horizontalRightTurnRoad.addNextRoad(afterHorizontalRightTurn);
        afterHorizontalRightTurn.addNextRoad(horizontalRoadRight);

        verticalRoadLeft.addNextRoad(verticalLeftTurnRoad);
        verticalLeftTurnRoad.addNextRoad(afterVerticalLeftTurn);
        afterVerticalLeftTurn.addNextRoad(verticalRoadLeft);

        verticalRoadRight.setPairedRoad(verticalRoadLeft);
        verticalRoadLeft.setPairedRoad(verticalRoadRight);
        horizontalRoadRight.setPairedRoad(horizontalRoadLeft);
        horizontalRoadLeft.setPairedRoad(horizontalRoadRight);



        // Ajouter toutes les routes à la liste
        roads.addAll(List.of(
                horizontalRoadRight, horizontalRoadLeft,
                verticalRoadRight, verticalRoadLeft,
                horizontalLeftTurnRoad, horizontalRightTurnRoad, afterHorizontalLeftTurn,
                verticalRightTurnRoad, afterVerticalRightTurn, afterVerticalLeftTurn, afterHorizontalRightTurn,
                verticalLeftTurnRoad
        ));

        List<Road> mainRoads = List.of(
                horizontalRoadLeft,
                horizontalRoadRight,
                verticalRoadRight,
                verticalRoadLeft
        );


        int carsPerLane = 3; // 3 voitures par voie
        int spawnDelay = 10000; // Délai de 2 secondes

        mainRoads.forEach(road -> {
            // Première voiture immédiate
            createAndAddVehicle(road);

            // Ajout des voitures suivantes avec délai
            for (int i = 1; i < carsPerLane; i++) {
                Timer timer = new Timer(i * spawnDelay, e -> createAndAddVehicle(road));
                timer.setRepeats(false);
                timer.start();
            }
        });


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


    }

    private void createAndAddVehicle(Road road) {
        List<Road> targetDestinations = roads.stream()
                .filter(r -> r.getLength() == 200)
                .collect(Collectors.toList());

        List<Road> possibleDestinations;

        if (road.isHorizontal()) {
            possibleDestinations = targetDestinations.stream()
                    .filter(r -> r.isHorizontal() && r != road)
                    .collect(Collectors.toList());
        } else {
            possibleDestinations = targetDestinations.stream()
                    .filter(r -> !r.isHorizontal() && r != road)
                    .collect(Collectors.toList());
        }

        if (!possibleDestinations.isEmpty()) {
            Road destination = possibleDestinations.get(
                    new Random().nextInt(possibleDestinations.size())
            );
            Vehicle v = new Vehicle(road, 0, 2, this, destination); // Position de départ à 0
            vehicles.add(v);
        }
    }




    public void updateSimulation() {
        manageTrafficLights();
        vehicles.forEach(Vehicle::update);

        List<Vehicle> arrived = vehicles.stream()
                .filter(Vehicle::hasReachedDestination)
                .collect(Collectors.toList());


        vehicles.removeAll(arrived);

        arrived.forEach(v -> {
                travelTimes.put(v, v.getTravelTime() );
                laneChanges.put(v, v.getLaneChangesCount());
        }
                );

        vehicles.removeAll(arrived);

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
        g.setColor(Color.RED);
        int yPos = 30;
        g.drawString("Métriques Globales :", 10, yPos);
        yPos += 15;
        g.drawString("Véhicules arrivés: " + travelTimes.size(), 10, yPos  );
        yPos += 15;


        if (!travelTimes.isEmpty()) {
            double avgTravelTime = travelTimes.values().stream().mapToLong(Long::longValue).average().orElse(0) / 1000;
            double avgLaneChanges = laneChanges.values().stream().mapToInt(Integer::intValue).average().orElse(0);

            g.drawString(String.format("Temps moyen de trajet: %.1f s", avgTravelTime), 10, yPos);
            yPos += 15;
            g.drawString(String.format("Changements de voie moyens: %.1f", avgLaneChanges), 10, yPos);
        }


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