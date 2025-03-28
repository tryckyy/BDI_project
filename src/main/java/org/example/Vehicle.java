package org.example;

import java.awt.*;
import java.util.*;
import java.util.List;

import static java.lang.Integer.MAX_VALUE;

class Vehicle {
    // Constantes de comportement
    private static final double SAFE_FOLLOW_DISTANCE = 40.0;
    private static final double EMERGENCY_BRAKE_DISTANCE = 20.0;
    private static final int MAX_SPEED = 5;
    private static final int STOP_DISTANCE = 30;
    private static final int LANE_WIDTH = 20;
    private static final int LOOK_AHEAD = 80;
    public static final int MIN_SAFE_LANE_CHANGE_DISTANCE = 30;
    private static final int LANE_CHANGE_COOLDOWN = 100;
    private int laneChangeTimer = 0;

    public Road currentRoad;
    private int position;
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

    private int currentMaxSpeed() {
        return currentRoad.getSpeedLimit();
    }

    private void evaluateDesires() {
        desires.clear();

        Optional<Road> nextRoad = detectUpcomingSpeedLimitChange();
        nextRoad.ifPresent(road -> {
            if(road.getSpeedLimit() < currentRoad.getSpeedLimit()) {
                desires.add("decelerate");
            }
        });



        // Évaluation des feux rouges
        Optional<TrafficLight> lightOpt = (Optional<TrafficLight>) beliefs.get("nextTrafficLight");
        lightOpt.ifPresent(light -> {
            TrafficLight.State lightState = light.getState();
            if (lightState == TrafficLight.State.RED || lightState == TrafficLight.State.ORANGE) {
                double distance = getDistanceToLight(light);
                boolean isApproaching = isApproachingLight(light);
                boolean isAtLight = distance <= 40.0;

                if (isApproaching || isAtLight) {
                    if(lightState == TrafficLight.State.RED) {
                        if (distance < STOP_DISTANCE) {
                            desires.add("fullStop");
                        } else if (distance < STOP_DISTANCE * 2) {
                            desires.add("decelerate");
                        }
                    }
                    else if (lightState == TrafficLight.State.ORANGE) {
                        desires.add("decelerate");
                        if (distance < STOP_DISTANCE) {
                            desires.add("emergencyBrake");
                        }
                    }

                }
            }
        });

        // Évaluation des véhicules précédents
        Optional<Vehicle> frontVehicle = (Optional<Vehicle>) beliefs.get("frontVehicle");
        frontVehicle.ifPresent(v -> {
            double distance = distanceTo(v);
            if (distance < EMERGENCY_BRAKE_DISTANCE) {
                desires.add("emergencyBrake");
            } else if (distance < SAFE_FOLLOW_DISTANCE) {
                desires.add("decelerate");
            }
        });

        if(desires.isEmpty()) desires.add("maintainSpeed");
    }

    private void executeIntention() {
        // Priorité: arrêt d'urgence > feu rouge > décélération > maintien vitesse
        speed = Math.min(speed + 1, currentMaxSpeed());

        if(desires.contains("emergencyBrake")) {
            speed = Math.max(0, speed - 3);
        }
        else if(desires.contains("fullStop")) {
            speed = 0;
        }
        else if(desires.contains("decelerate")) {
            speed = Math.max(speed - 1, 0);
        }

        else {
            speed = Math.min(speed + 1, MAX_SPEED);
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