package org.example;

import java.awt.*;
import java.util.*;
import java.util.List;


class Vehicle {
    // Constantes de comportement
    private static final double SAFE_FOLLOW_DISTANCE = 120.0;
    private static final int MAX_SPEED = 5;
    private static final double STOP_DISTANCE = 60.0;
    private static final int LANE_WIDTH = 20;
    public static final int MIN_SAFE_LANE_CHANGE_DISTANCE = 70;

    public Road currentRoad;
    private int position;
    private int baseSpeed = 2;
    private int speed;
    private boolean isInRightLane;
    public int laneOffset;
    private SimulationPanel environment;
    private Color color;

    // Système BDI
    private Map<String, Object> beliefs = new HashMap<>();
    private List<String> desires = new ArrayList<>();

    public Vehicle(Road road, int initialPos, int speed, SimulationPanel environment) {
        this.currentRoad = road;
        this.position = initialPos;
        this.speed = speed;
        this.environment = environment;
        this.color = new Color(
                (int)(Math.random() * 200 + 55),
                (int)(Math.random() * 200 + 55),
                (int)(Math.random() * 200 + 55)
        );
        this.isInRightLane = road.isRightLane();
    }

    public void update() {
        perceiveEnvironment();
        evaluateDesires();
        executeIntention();
        move();
    }

    private void perceiveEnvironment() {
        // Perception des éléments environnementaux
        beliefs.put("nearbyVehicles", environment.getNearbyVehicles(this));
        beliefs.put("nextTrafficLight", environment.getNextTrafficLight(this));
        beliefs.put("frontVehicle", environment.getVehiclesInLane(this).stream()
                .filter(this::isAheadOf)
                .min(Comparator.comparingDouble(this::distanceTo)));


    }


    private void evaluateDesires() {
        desires.clear();

        // Évaluation des véhicules précédents
        Optional<Vehicle> frontVehicle = (Optional<Vehicle>) beliefs.get("frontVehicle");
        frontVehicle.ifPresent(v -> {

            double distance = distanceTo(v);
            if(distance < STOP_DISTANCE) {
                desires.add("fullStop");
            }
            else if(distance < SAFE_FOLLOW_DISTANCE) {
                desires.add("decelerate");
            }
        });

        Optional<TrafficLight> lightOpt = (Optional<TrafficLight>) beliefs.get("nextTrafficLight");

        if(isAdjacentLaneClear() && !isInRightLane && !lightOpt.isPresent() ) {
            desires.add("changeLane");
        }
        else if(!isAdjacentLaneClear() && !isInRightLane && !lightOpt.isPresent()) {
            desires.add("accelerate");
        }

        lightOpt.ifPresent(light -> {
            TrafficLight.State lightState = light.getState();
            double distance = getDistanceToLight(light);
            boolean isApproaching = isApproachingLight(light);
            boolean isAtLight = distance <= 40.0;


            if (lightState == TrafficLight.State.RED || lightState == TrafficLight.State.ORANGE) {
                desires.remove("overtake");

                if (isApproaching || isAtLight) {
                    if (lightState == TrafficLight.State.RED) {
                        if (distance < STOP_DISTANCE) {
                            desires.add("fullStop");
                        } else if (distance < SAFE_FOLLOW_DISTANCE) {
                            desires.add("decelerate");
                        }
                    } else if (lightState == TrafficLight.State.ORANGE) {
                        desires.add("decelerate");
                        if (distance < STOP_DISTANCE) {
                            desires.add("fullStop");
                        }

                    }

                }

            }

            else if(lightState == TrafficLight.State.GREEN) {
                desires.add("maintainSpeed");
            }
        });

        System.out.println(desires);


        if(desires.isEmpty()) desires.add("maintainSpeed");
    }



    private boolean isAdjacentLaneClear() {
        Road paired = currentRoad.getPairedRoad();
        if(paired == null) return false;
        List<Vehicle> adjacentVehicles = environment.getVehiclesOnRoad(paired);
        return adjacentVehicles.stream().noneMatch(v ->
                Math.abs(v.position - this.position) < MIN_SAFE_LANE_CHANGE_DISTANCE
        );
    }


    void changeLane() {
        if(isAdjacentLaneClear()) {
            currentRoad = currentRoad.getPairedRoad();
            System.out.println("Road : " + currentRoad + "vehicles" + environment.getVehiclesOnRoad(currentRoad));
            isInRightLane = !isInRightLane;
            laneOffset += currentRoad.isHorizontal() ? LANE_WIDTH/8 : -LANE_WIDTH/8;
        }
    }


    private void executeIntention() {
        if (desires.contains("maintainSpeed")) {
            speed = baseSpeed;
        }

        else if(desires.contains("decelerate")) {
            speed = Math.max(speed - 1, 1);
        }

        else if(desires.contains("accelerate")) {
            speed = MAX_SPEED;
        }

        else if(desires.contains("changeLane")) {
            changeLane();
        }
        if(desires.contains("fullStop")) {
            speed = 0;
        }
    }


    private void move() {
        position += speed;
        if (position > currentRoad.getLength()) {
            if (currentRoad.getNextRoad() != null) {
                // Passer à la prochaine route et réinitialiser la position
                currentRoad = currentRoad.getNextRoad();
                position = 0;
            } else {
                // Si aucune route suivante, boucler sur la même route
                position %= currentRoad.getLength();
            }
        }
    }


    // Méthodes utilitaires
    private boolean isAheadOf(Vehicle other) {
        if(currentRoad.isHorizontal()) {
            return this.getFrontPosition().x < other.getFrontPosition().x;
        } else {
            return this.getFrontPosition().y < other.getFrontPosition().y;
        }
    }

    private boolean isApproachingLight(TrafficLight light) {
        Point lightPos = light.getPosition();
        Point frontPos = getFrontPosition();

        if (currentRoad.isHorizontal()) {
            // Vérifier l'alignement sur la même voie verticale (Y)
            boolean sameLane = Math.abs(frontPos.y - lightPos.y) <= LANE_WIDTH;
            return sameLane && (
                    (speed > 0 && frontPos.x < lightPos.x) ||
                            (speed < 0 && frontPos.x > lightPos.x)
            );
        } else {
            // Vérifier l'alignement sur la même voie horizontale (X)
            boolean sameLane = Math.abs(frontPos.x - lightPos.x) <= LANE_WIDTH;
            return sameLane && (
                    (speed > 0 && frontPos.y < lightPos.y) ||
                            (speed < 0 && frontPos.y > lightPos.y)
            );
        }
    }

    private double getDistanceToLight(TrafficLight light) {
        return getFrontPosition().distance(light.getPosition());
    }

    public double distanceTo(Vehicle other) {
        return getFrontPosition().distance(other.getFrontPosition());
    }

    public Point getPosition() {
        return currentRoad.getPosition(position, laneOffset);
    }

    public Point getFrontPosition() {
        Point pos = getPosition(); // Contient déjà le laneOffset
        int frontOffset = speed > 0 ? 15 : -15; // Déplacement selon la direction

        if (currentRoad.isHorizontal()) {
            return new Point(pos.x + frontOffset, pos.y); // Ne pas ajouter laneOffset ici
        } else {
            return new Point(pos.x, pos.y + frontOffset); // Ne pas ajouter laneOffset ici
        }
    }


    public void draw(Graphics g) {
        Point pos = getPosition();
        g.setColor(color);

        RoadSegment currentSegment = currentRoad.getCurrentSegment(pos);
        boolean isHorizontal = currentSegment.isHorizontal();

        if(isHorizontal) {
            g.fillRect(pos.x - 10, pos.y - 5, 20, 10);
        } else {
            g.fillRect(pos.x - 5, pos.y - 10, 10, 20);
        }
    }
}