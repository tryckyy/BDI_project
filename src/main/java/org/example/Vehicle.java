package org.example;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


class Vehicle {
    // Constantes de comportement
    private static final double SAFE_FOLLOW_DISTANCE = 120.0;
    private static final int MAX_SPEED = 5;
    private static final double STOP_DISTANCE = 60.0;
    private static final int LANE_WIDTH = 20;
    public static final int MIN_SAFE_LANE_CHANGE_DISTANCE = 70;
    private int laneChangeCooldown = 0;
    private static final int LANE_CHANGE_DELAY = 30;

    public Road currentRoad;
    private Road destination;
    private int position;
    private boolean reachedDestination = false;
    private int baseSpeed = 2;
    private int speed;
    private boolean isInRightLane;
    public int laneOffset;
    private SimulationPanel environment;
    private Color color;

    // Système BDI
    private Map<String, Object> beliefs = new HashMap<>();
    private List<String> desires = new ArrayList<>();

    public Vehicle(Road road, int initialPos, int speed, SimulationPanel environment, Road destination) {
        this.currentRoad = road;
        this.position = initialPos;
        this.speed = speed;
        this.environment = environment;
        this.destination = destination;
        this.color = new Color(
                (int)(Math.random() * 200 + 55),
                (int)(Math.random() * 200 + 55),
                (int)(Math.random() * 200 + 55)
        );
        this.isInRightLane = currentRoad.isRightLane();
    }

    public boolean hasReachedDestination() {
        return reachedDestination;
    }

    private void checkDestination() {
        if (currentRoad == destination) {
            if (currentRoad.isReverse()) {
                if (position <= 0) { // Fin de route en mode reverse
                    reachedDestination = true;
                }
            } else {
                if (position >= currentRoad.getLength()) { // Fin de route normale
                    reachedDestination = true;
                }
            }
        }
    }


    public void update() {
        perceiveEnvironment();
        evaluateDesires();
        executeIntention();
        move();
        checkDestination();
        if (laneChangeCooldown > 0) laneChangeCooldown--;
    }

    private void perceiveEnvironment() {
        // Perception des éléments environnementaux
        beliefs.put("nearbyVehicles", environment.getNearbyVehicles(this));
        beliefs.put("nextTrafficLight", environment.getNextTrafficLight(this));
        beliefs.put("frontVehicle", environment.getVehiclesInLane(this).stream()
                .filter(this::isAheadOf)
                .min(Comparator.comparingDouble(this::distanceTo)));

        if(destination.isRightLane() && !currentRoad.isRightLane()) {
            beliefs.put("shouldChangeLane", true);
            isInRightLane = true;
        }
        else if(!destination.isRightLane() && currentRoad.isRightLane()) {
            beliefs.put("shouldChangeLane", true);
            isInRightLane = false;
        }
        else {
            beliefs.remove("shouldChangeLane");
        }
        // Condition stricte : changer de voie uniquement si la destination est dans la voie jumelle
    }


    private void evaluateDesires() {
        desires.clear();

        if (beliefs.containsKey("shouldChangeLane") && !isInRightLane && !isAdjacentLaneClear()) {
            desires.add("decelerate");
        }
        else if(beliefs.containsKey("shouldChangeLane") && !isInRightLane && isAdjacentLaneClear()) {
            desires.add("changeLane");
        }
        else if(beliefs.containsKey("shouldChangeLane") && isInRightLane && !isAdjacentLaneClear()) {
            desires.add("accelerate");
        }
        else if(beliefs.containsKey("shouldChangeLane") && isInRightLane && isAdjacentLaneClear()) {
            desires.add("changeLane");
        }

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
        Road pairedRoad = currentRoad.getPairedRoad();
        if (pairedRoad == null) return;


        if (isAdjacentLaneClear()) {
            currentRoad = pairedRoad;
            isInRightLane = currentRoad.isRightLane();
            laneOffset = currentRoad.isHorizontal() ? LANE_WIDTH / 8 : -LANE_WIDTH / 8;
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

        if (currentRoad.isReverse() && position >= -currentRoad.getLength()) {
            position -= speed;
        } else if (position < -currentRoad.getLength()) {
            if (!currentRoad.getNextRoads().isEmpty()) {
                currentRoad = currentRoad.getNextRoads().get(0); // Prend la première route suivante
                isInRightLane = currentRoad.isRightLane();
                position = 0;
            }
        } else {
            position += speed;
            if (position > currentRoad.getLength()) {
                if (!currentRoad.getNextRoads().isEmpty()) {
                    currentRoad = currentRoad.getNextRoads().get(0);
                    position = 0;
                } else {
                    position %= currentRoad.getLength();
                }
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

        if(currentSegment.isReverse() && !isHorizontal) {
            g.fillRect(pos.x - 5, pos.y + currentSegment.getLength() , 10, 20);
        } else if (currentSegment.isReverse() && isHorizontal) {
            g.fillRect(pos.x + currentSegment.getLength(), pos.y - 5 , 20, 10);
        } else {

            if (isHorizontal) {
                g.fillRect(pos.x - 10, pos.y - 5, 20, 10);
            } else {
                g.fillRect(pos.x - 5, pos.y - 10, 10, 20);
            }
        }
    }
}