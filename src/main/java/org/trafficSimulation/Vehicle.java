package org.trafficSimulation;

import java.awt.*;
import java.util.*;
import java.util.List;


public class Vehicle {
    // Constantes de comportement
    private static final double SAFE_FOLLOW_DISTANCE = 120.0;
    private static final int MAX_SPEED = 5;
    private static final double STOP_DISTANCE = 60.0;
    private static final int LANE_WIDTH = 20;
    public static final int MIN_SAFE_LANE_CHANGE_DISTANCE = 70;
    private long lastLaneChangeTime = 0;
    private static final long LANE_CHANGE_COOLDOWN = 1000;

    private final Road destination;
    private final int baseSpeed = 2;

    private Color color;
    private final SimulationPanel environment;
    private final long creationTime = System.currentTimeMillis(); // Temps de création
    private int laneChangesCount = 0;

    public Road currentRoad;
    private int position;
    private boolean reachedDestination = false;

    private List<Road> path;

    private int speed;
    private boolean isInRightLane;
    public int laneOffset;



    // Système BDI
    private final Map<String, Object> beliefs = new HashMap<>();
    private final List<String> desires = new ArrayList<>();

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

    public void setPath(List<Road> path) {
        this.path = new ArrayList<>(path);
        // Ajouter la destination comme dernière étape si elle n'y est pas
        if (!path.isEmpty() && !path.get(path.size() - 1).equals(destination)) {
            this.path.add(destination);
        }
    }



    public boolean hasReachedDestination() {
        return reachedDestination;
    }

    private void checkDestination() {
        // ONLY check if on the final destination road and at its end point
        if (currentRoad.equals(destination)) {
            boolean atEndOfRoad = currentRoad.isReverse()
                    ? position <= 0
                    : position >= currentRoad.getLength();

            if (atEndOfRoad) {
                reachedDestination = true;
            }
        }
    }


    public void update() {
        perceiveEnvironment();
        evaluateDesires();
        executeIntention();
        move();
        checkDestination();
    }

    private void perceiveEnvironment() {
        beliefs.put("nextTrafficLight", environment.getNextTrafficLight(this));
        beliefs.put("frontVehicle", environment.getVehiclesInLane(this).stream()
                .filter(this::isAheadOf)
                .min(Comparator.comparingDouble(this::distanceTo)));

        if(shouldPrepareForDestinationLane()) {
            beliefs.put("changeLane", true);
        }
        else {
            beliefs.remove("changeLane");
        }


    }

    private boolean shouldPrepareForDestinationLane() {
        if (path == null || path.size() < 2) {
            return false;
        }

        int currentIndex = path.indexOf(currentRoad);
        if (currentIndex < 0) {
            // Si la route actuelle n'est pas dans le chemin, vérifier la première route du chemin
            Road firstRoadInPath = path.get(0);
            Road pairedRoad = currentRoad.getPairedRoad();
            return pairedRoad != null && firstRoadInPath.equals(pairedRoad);
        }

        if (currentIndex == path.size() - 1) {
            return false; // Ne pas changer de voie si c'est la dernière route
        }

        // Ne changer de voie que si on est proche de la fin de la route actuelle
        boolean isNearEnd = currentRoad.isReverse()
                ? position <= MIN_SAFE_LANE_CHANGE_DISTANCE
                : position >= currentRoad.getLength() - MIN_SAFE_LANE_CHANGE_DISTANCE;

        if (!isNearEnd) {
            return false;
        }

        Road nextRoad = path.get(currentIndex + 1);
        return nextRoad.isRightLane() != currentRoad.isRightLane();
    }



    private void handleTrafficAndObstacles() {
        Optional<Vehicle> frontVehicle = (Optional<Vehicle>) beliefs.get("frontVehicle");
        Optional<TrafficLight> lightOpt = (Optional<TrafficLight>) beliefs.get("nextTrafficLight");


        // Gestion des véhicules devant
        frontVehicle.ifPresent(v -> {
            double distance = distanceTo(v);
            if (distance < STOP_DISTANCE) {
                desires.add("fullStop");
            } else if (distance < SAFE_FOLLOW_DISTANCE) {
                desires.add("decelerate");
            }
        });

        // Gestion des feux
        lightOpt.ifPresent(light -> {
            TrafficLight.State lightState = light.getState();
            double distance = getDistanceToLight(light);

            if ((lightState == TrafficLight.State.RED || lightState == TrafficLight.State.ORANGE) &&
                    isApproachingLight(light)) {
                if (distance < STOP_DISTANCE) {
                    desires.add("fullStop");
                } else if (distance < SAFE_FOLLOW_DISTANCE) {
                    desires.add("decelerate");
                }
            }
        });
    }




    private void evaluateDesires() {
        desires.clear();
        handleTrafficAndObstacles();

        if(!beliefs.containsKey("changeLane")) {
            desires.add("accelerate");
        }

        if(beliefs.containsKey("changeLane")) {
            desires.add("changeLane");
        }


        Optional<Vehicle> frontVehicle = (Optional<Vehicle>) beliefs.get("frontVehicle");
        frontVehicle.ifPresent(v -> {

            double distance = distanceTo(v);
            if(currentRoad.isReverse()) {
                if(distance > STOP_DISTANCE) {
                    desires.add("fullStop");
                }
                else if(distance > SAFE_FOLLOW_DISTANCE) {
                    desires.add("decelerate");
                }
            } else {
                if(distance < STOP_DISTANCE) {
                    desires.add("fullStop");
                }
                else if(distance < SAFE_FOLLOW_DISTANCE) {
                    desires.add("decelerate");
                }
            }
        });

        Optional<TrafficLight> lightOpt = (Optional<TrafficLight>) beliefs.get("nextTrafficLight");

        lightOpt.ifPresent(light -> {
            TrafficLight.State lightState = light.getState();
            double distance = getDistanceToLight(light);
            boolean isApproaching = isApproachingLight(light);
            boolean isAtLight = distance <= 40.0;


            if (lightState == TrafficLight.State.RED || lightState == TrafficLight.State.ORANGE) {

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
        if (paired == null) return false;

        List<Vehicle> adjacentVehicles = environment.getVehiclesOnRoad(paired);

        // Vérifiez si la route est dans le même sens ou dans le sens inverse
        boolean isSameDirection = paired.isReverse() == currentRoad.isReverse();

        for (Vehicle v : adjacentVehicles) {
            int relativePosInAdjacentLane;

            // Ajustez la position relative en fonction de l'orientation des routes
            if (isSameDirection) {
                relativePosInAdjacentLane = v.position;
            } else {
                // Si les routes sont dans des directions opposées, inversez la position relative
                relativePosInAdjacentLane = paired.getLength() - v.position;
            }

            // Calculez la distance entre les véhicules en tenant compte de la direction
            int positionDifference = Math.abs(relativePosInAdjacentLane - this.position);

            // Vérifiez si la distance est inférieure à la distance minimale sécuritaire
            if (positionDifference < MIN_SAFE_LANE_CHANGE_DISTANCE) {
                return false;
            }
        }

        return true;
    }

    public int getLaneChangesCount() { return laneChangesCount; }

    public long getTravelTime() {
        return System.currentTimeMillis() - creationTime;
    }

    void changeLane() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLaneChangeTime < LANE_CHANGE_COOLDOWN) {
            return; // Ignore le changement si le cooldown n'est pas écoulé
        }

        Road pairedRoad = currentRoad.getPairedRoad();
        if (pairedRoad == null || !isAdjacentLaneClear()) {
            return;
        }

        currentRoad = pairedRoad;
        isInRightLane = currentRoad.isRightLane();
        laneOffset = currentRoad.isHorizontal() ? LANE_WIDTH / 8 : -LANE_WIDTH / 8;
        laneChangesCount++;
        lastLaneChangeTime = currentTime; // Met à jour le temps du dernier changement
    }


    public Color getColor() {
        return color;
    }

    public Road getDestination() {
        return destination;
    }

    private void executeIntention() {
        if (desires.contains("changeLane")) {
            changeLane();
            desires.add("accelerate");
        }
        if (desires.contains("maintainSpeed")) {
            speed = baseSpeed;
        }


        else if(desires.contains("decelerate")) {
            speed = Math.max(speed - 1, 1);
        }

        else if(desires.contains("accelerate") && !isInRightLane) {
            speed = MAX_SPEED + 1;
        }
        else if(desires.contains("accelerate") && isInRightLane) {
            speed = MAX_SPEED - 1;
        }




        if(desires.contains("fullStop")) {
            speed = 0;
        }
    }


    private void move() {
        if (reachedDestination) return;

        position += currentRoad.isReverse() ? -speed : speed;

        boolean shouldTransition = currentRoad.isReverse()
                ? position <= 0
                : position >= currentRoad.getLength();

        if (shouldTransition) {
            if (currentRoad.equals(destination)) {
                reachedDestination = true;
                return;
            }



            int currentIndex = path.indexOf(currentRoad);
            if (currentIndex >= 0 && currentIndex < path.size() - 1) {
                Road nextRoad = path.get(currentIndex + 1);
                currentRoad = nextRoad;
                position = nextRoad.isReverse() ? nextRoad.getLength() : 0;
                isInRightLane = nextRoad.isRightLane();
            }
        }
    }




    private void recalculatePath() {
        Point start = getPosition();
        Point end = RoadGraph.getEndPoint(destination);

        if (currentRoad.isReverse() ? position <= 0 : position >= currentRoad.getLength()) {
            start = RoadGraph.getEndPoint(currentRoad);
        }

        List<Road> newPath = RoadGraph.buildFromRoads(environment.roads)
                .findShortestPath(start, end);

        if (!newPath.isEmpty()) {
            this.path = newPath;
            Road firstRoad = path.get(0);
            Road pairedRoad = currentRoad.getPairedRoad();

            if (!firstRoad.equals(currentRoad) && pairedRoad != null && firstRoad.equals(pairedRoad)) {
                currentRoad = firstRoad;
                position = currentRoad.isReverse() ? currentRoad.getLength() : 0;
                isInRightLane = currentRoad.isRightLane();
            }
        } else {
            reachedDestination = true;
        }
    }


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
            boolean sameLane = Math.abs(frontPos.y - lightPos.y) <= LANE_WIDTH;
            return sameLane && (
                    (speed > 0 && frontPos.x < lightPos.x) ||
                            (speed < 0 && frontPos.x > lightPos.x)
            );
        } else {
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
        Point pos = getPosition();
        int frontOffset = speed > 0 ? 15 : -15;

        if (currentRoad.isHorizontal()) {
            return new Point(pos.x + frontOffset, pos.y);
        } else {
            return new Point(pos.x, pos.y + frontOffset);
        }
    }


    public void draw(Graphics g) {
        Point pos = getPosition();
        g.setColor(color);

        RoadSegment currentSegment = currentRoad.getCurrentSegment(pos);
        boolean isHorizontal = currentSegment.isHorizontal();

        if(currentSegment.isReverse() && !isHorizontal) {
            g.fillRect(pos.x - 5, pos.y , 10, 20);
        } else if (currentSegment.isReverse() && isHorizontal) {
            g.fillRect(pos.x, pos.y - 5 , 20, 10);
        } else {

            if (isHorizontal) {
                g.fillRect(pos.x - 10, pos.y - 5, 20, 10);
            } else {
                g.fillRect(pos.x - 5, pos.y - 10, 10, 20);
            }
        }

    }
}