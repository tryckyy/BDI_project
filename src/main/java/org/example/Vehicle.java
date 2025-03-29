package org.example;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


class Vehicle {
    // Constantes de comportement
    private static final double SAFE_FOLLOW_DISTANCE = 60.0;
    private static final double EMERGENCY_BRAKE_DISTANCE = 10.0;
    private static final int MAX_SPEED = 5;
    private static final double STOP_DISTANCE = 30.0;
    private static final int LANE_WIDTH = 20;
    private static final int LOOK_AHEAD = 80;
    public static final int MIN_SAFE_LANE_CHANGE_DISTANCE = 70;
    private static final int LANE_CHANGE_COOLDOWN = 100;
    private int laneChangeTimer = 0;
    private boolean isOvertaking = false;
    private int maxSpeed = 8;

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
        if (laneChangeTimer > 0) laneChangeTimer--;
    }

    private void perceiveEnvironment() {
        // Perception des éléments environnementaux
        beliefs.put("nearbyVehicles", environment.getNearbyVehicles(this));
        beliefs.put("nextTrafficLight", environment.getNextTrafficLight(this));
        beliefs.put("frontVehicle", environment.getVehiclesInLane(this).stream()
                .filter(this::isAheadOf)
                .min(Comparator.comparingDouble(this::distanceTo)));


    }

    private int currentMaxSpeed() {
        return currentRoad.getSpeedLimit();
    }

    private void evaluateDesires() {
        desires.clear();



        // Évaluation des feux rouges


        // Évaluation des véhicules précédents
        Optional<Vehicle> frontVehicle = (Optional<Vehicle>) beliefs.get("frontVehicle");
        frontVehicle.ifPresent(v -> {

            double distance = distanceTo(v);
            if(distance < 60.0) {
                desires.add("fullStop");
            }
            else if(distance < 120.0) {
                desires.add("decelerate");
            }
        });

        Optional<TrafficLight> lightOpt = (Optional<TrafficLight>) beliefs.get("nextTrafficLight");
        if (canOvertake() && isInRightLane && !lightOpt.isPresent() && beliefs.containsKey("frontVehicle")) {
            desires.add("maintainSpeed");
            desires.add("overtake");
        }

        else if(isAdjacentLaneClear() && !isInRightLane && !lightOpt.isPresent() ) {
            desires.add("changeLane");
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

    private boolean canOvertake() {
        if(isAdjacentLaneClear()){
            return true;
        }
        if (isOvertaking || currentRoad.isRightLane() || laneChangeTimer > 0)
            return false;

        Optional<Vehicle> frontVehicleOpt = (Optional<Vehicle>) beliefs.get("frontVehicle");
        if (!frontVehicleOpt.isPresent() || speed <= frontVehicleOpt.get().speed)
            return false;

        // Vérifier la distance de sécurité
        double distance = distanceTo(frontVehicleOpt.get());
        if (distance > SAFE_FOLLOW_DISTANCE)
            return false;

        return true;
    }

    private boolean isAdjacentLaneClear() {
        Road paired = currentRoad.getPairedRoad();
        if(paired == null) return false;
        List<Vehicle> adjacentVehicles = environment.getVehiclesOnRoad(paired);
        return adjacentVehicles.stream().noneMatch(v ->
                Math.abs(v.position - this.position) < MIN_SAFE_LANE_CHANGE_DISTANCE
        );
    }

    void overtake() {
        if (!isInRightLane) {
            speed = Math.min(speed + 2, maxSpeed); // Accelerate during overtake
            isOvertaking = true;
            isInRightLane = !isInRightLane; // Switch lanes
            laneOffset += currentRoad.isHorizontal() ? LANE_WIDTH/8 : -LANE_WIDTH/8;
            currentRoad = currentRoad.getPairedRoad();

        }
        isOvertaking = false;
        desires.remove("overtake");
    }

    void changeLane() {
        if(isAdjacentLaneClear()) {
            currentRoad = currentRoad.getPairedRoad();
            System.out.println("Road : " + currentRoad + "vehicles" + environment.getVehiclesOnRoad(currentRoad));
            isInRightLane = !isInRightLane;
            laneOffset += currentRoad.isHorizontal() ? LANE_WIDTH/8 : -LANE_WIDTH/8;
        }
    }



    private double getPositionDistance(Vehicle other) {
        return other.position - this.position;
    }

    private void executeIntention() {
        if (desires.contains("maintainSpeed")) {
            speed = baseSpeed;
        }

        else if(desires.contains("decelerate")) {
            speed = Math.max(speed - 1, 1);
        }


        else if(desires.contains("accelerate")) {
            speed = Math.max(speed + 1, 6);
        }
        else if(desires.contains("overtake")) {
            overtake();
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

    private Optional<Road> detectUpcomingSpeedLimitChange() {
        final int LOOK_AHEAD = 80; // Distance de détection en pixels
        Point front = getFrontPosition();

        return environment.getRoads().stream()
                .filter(road -> road != currentRoad)
                .filter(road -> isRoadInPath(road, front))
                .filter(road -> road.getSpeedLimit() != currentRoad.getSpeedLimit())
                .findFirst();
    }

    public int getRoadPosition() {
        return position;
    }



    private boolean isRoadInPath(Road road, Point vehicleFront) {
        boolean sameDirection = (currentRoad.isHorizontal() == road.isHorizontal());
        Point roadStart = road.getStartPoint();

        if(currentRoad.isHorizontal()) {
            return sameDirection &&
                    Math.abs(roadStart.y - vehicleFront.y) < 30 &&
                    roadStart.x > vehicleFront.x &&
                    roadStart.x - vehicleFront.x < LOOK_AHEAD;
        } else {
            return sameDirection &&
                    Math.abs(roadStart.x - vehicleFront.x) < 30 &&
                    roadStart.y > vehicleFront.y &&
                    roadStart.y - vehicleFront.y < LOOK_AHEAD;
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