package org.trafficSimulation.view;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.trafficSimulation.model.ai.TrafficLightQLearning;
import org.trafficSimulation.model.argumentation.TransportArgumentation;
import org.trafficSimulation.model.environment.Weather;
import org.trafficSimulation.model.graph.RoadGraph;
import org.trafficSimulation.model.road.Road;
import org.trafficSimulation.model.road.RoadSegment;
import org.trafficSimulation.model.agents.Moto;
import org.trafficSimulation.model.agents.TrafficLight;
import org.trafficSimulation.model.agents.Vehicle;
import org.trafficSimulation.model.agents.Velo;

import static org.trafficSimulation.model.graph.RoadGraph.getEndPoint;
import static org.trafficSimulation.model.graph.RoadGraph.getStartPoint;


public class SimulationPanel extends JPanel {
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<TrafficLight> trafficLights = new ArrayList<>();
    private List<Road> endpointRoads = new ArrayList<>();
    public List<Road> roads = new ArrayList<>();
    private TrafficLightQLearning qLearning;
    private int timeSinceLastChange = 0;
    private TrafficLightQLearning.TrafficPhase currentTrafficPhase = TrafficLightQLearning.TrafficPhase.HORIZONTAL_GREEN;;
    private final Map<Vehicle, Long> travelTimes = new HashMap<>();
    private final Map<Vehicle, Integer> laneChanges = new HashMap<>();
    private static final double CONNECTION_THRESHOLD = 10;
    private boolean showGraph = false;
    private RoadGraph roadGraph;
    private Map<Point, Set<Point>> graphEdges = new HashMap<>();
    private JCheckBox showGraphCheckbox;
    private boolean showDestinations = false;
    private JCheckBox showDestinationsCheckbox;
    private int carsPerLane = 8;
    private int spawnDelay = 5000;
    private JSpinner carsPerLaneSpinner;
    private JSpinner spawnDelaySpinner;
    private JButton startButton;
    private boolean simulationStarted = false;
    private JButton resetButton;
    private final List<Timer> timers = new ArrayList<>();
    private List<Moto> motos = new ArrayList<Moto>();
    private List<Velo> velos = new ArrayList<>();
    private Weather weather;
    private TransportArgumentation transportArgumentation;
    private Random random;
    private JComboBox<Weather.Condition> weatherConditionComboBox;
    private JSlider temperatureSlider;
    private JSlider windSpeedSlider;
    private JSlider visibilitySlider;
    private JPanel controlPanel;



    public SimulationPanel() {
        setLayout(new BorderLayout());
        weather = new Weather();
        transportArgumentation = new TransportArgumentation();
        random = new Random();

        // Créer et configurer le panneau principal de simulation
        JPanel simulationArea = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                SimulationPanel.this.paintComponent(g);
            }
        };
        simulationArea.setPreferredSize(new Dimension(1600, 900));

        // Créer le panneau de contrôle
        createControlPanel();

        // Ajouter les composants au panneau principal
        add(simulationArea, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);

        initializeComponents();


    }

    private void createControlPanel() {
        controlPanel = new JPanel();
        controlPanel.setPreferredSize(new Dimension(300, getHeight()));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Contrôles"));
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        // Panneau pour les contrôles de simulation existants
        JPanel simulationControls = new JPanel();
        simulationControls.setBorder(BorderFactory.createTitledBorder("Simulation"));
        simulationControls.setLayout(new GridLayout(0, 2, 5, 5));

        // Ajouter les contrôles de simulation existants
        carsPerLaneSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 20, 1));
        spawnDelaySpinner = new JSpinner(new SpinnerNumberModel(5000, 1000, 10000, 500));

        simulationControls.add(new JLabel("Véhicules par voie:"));
        simulationControls.add(carsPerLaneSpinner);
        simulationControls.add(new JLabel("Délai d'apparition (ms):"));
        simulationControls.add(spawnDelaySpinner);

        // Panneau pour les contrôles météo
        JPanel weatherPanel = new JPanel();
        weatherPanel.setBorder(BorderFactory.createTitledBorder("Météo"));
        weatherPanel.setLayout(new GridLayout(0, 2, 5, 5));

        // Créer les contrôles météo
        weatherConditionComboBox = new JComboBox<>(Weather.Condition.values());
        temperatureSlider = new JSlider(JSlider.HORIZONTAL, -10, 40, 20);
        windSpeedSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        visibilitySlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, 1000);

        // Configurer les sliders
        configureSlider(temperatureSlider, "°C", 10, 5);
        configureSlider(windSpeedSlider, "", 20, 5);
        configureSlider(visibilitySlider, "m", 200, 50);

        // Ajouter les contrôles météo
        weatherPanel.add(new JLabel("Condition:"));
        weatherPanel.add(weatherConditionComboBox);
        weatherPanel.add(new JLabel("Température:"));
        weatherPanel.add(temperatureSlider);
        weatherPanel.add(new JLabel("Vitesse du vent:"));
        weatherPanel.add(windSpeedSlider);
        weatherPanel.add(new JLabel("Visibilité:"));
        weatherPanel.add(visibilitySlider);

        // Panneau pour les boutons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        startButton = new JButton("Démarrer");
        resetButton = new JButton("Réinitialiser");
        showGraphCheckbox = new JCheckBox("Afficher le graphe");
        showDestinationsCheckbox = new JCheckBox("Afficher les destinations");




        buttonPanel.add(startButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(showGraphCheckbox);
        buttonPanel.add(showDestinationsCheckbox);


        // Ajouter les listeners
        addControlListeners();

        // Ajouter tous les panneaux au panneau de contrôle
        controlPanel.add(simulationControls);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        controlPanel.add(weatherPanel);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        controlPanel.add(buttonPanel);
    }






    private void configureSlider(JSlider slider, String unit, int majorTick, int minorTick) {
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(majorTick);
        slider.setMinorTickSpacing(minorTick);
        slider.setPreferredSize(new Dimension(600, 50));  // Augmenté à 400 pixels de large

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();

        // Valeur minimale
        JLabel minLabel = new JLabel(slider.getMinimum() + unit);
        minLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
        minLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));  // Marges augmentées
        labelTable.put(slider.getMinimum(), minLabel);

        // Valeur médiane
        int median = (slider.getMaximum() + slider.getMinimum()) / 2;
        JLabel medLabel = new JLabel(median + unit);
        medLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
        medLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));  // Marges augmentées
        labelTable.put(median, medLabel);

        // Valeur maximale
        JLabel maxLabel = new JLabel(slider.getMaximum() + unit);
        maxLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 12));
        maxLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));  // Marges augmentées
        labelTable.put(slider.getMaximum(), maxLabel);

        slider.setLabelTable(labelTable);


    }

    private void addControlListeners() {
        startButton.addActionListener(e -> {
            if (!simulationStarted) {
                startSimulation();
                startButton.setText("Démarrer");
            }
            simulationStarted = !simulationStarted;
        });

        resetButton.addActionListener(e -> resetSimulation());

        showGraphCheckbox.addActionListener(e -> {
            showGraph = showGraphCheckbox.isSelected();
            repaint();
        });

        showDestinationsCheckbox.addActionListener(e -> {
            showDestinations = showDestinationsCheckbox.isSelected();
            repaint();
        });

        // Listener pour les changements météo
        ActionListener weatherUpdateListener = e -> updateWeatherConditions();
        ChangeListener sliderListener = e -> updateWeatherConditions();

        weatherConditionComboBox.addActionListener(weatherUpdateListener);
        temperatureSlider.addChangeListener(sliderListener);
        windSpeedSlider.addChangeListener(sliderListener);
        visibilitySlider.addChangeListener(sliderListener);
    }



    private void updateWeatherConditions() {
        Weather.Condition condition = (Weather.Condition) weatherConditionComboBox.getSelectedItem();
        double temperature = temperatureSlider.getValue();
        double windSpeed = windSpeedSlider.getValue();
        double visibility = visibilitySlider.getValue();

        weather.updateWeather(condition, temperature, windSpeed, visibility);
    }



    private void resetSimulation() {
        // Arrêter tous les timers actifs
        for (Timer timer : timers) {
            timer.stop();
        }
        timers.clear();

        // Réinitialiser les variables
        vehicles.clear();
        motos.clear();
        velos.clear();
        travelTimes.clear();
        laneChanges.clear();
        simulationStarted = false;
        timeSinceLastChange = 0;
        currentTrafficPhase = TrafficLightQLearning.TrafficPhase.HORIZONTAL_GREEN;

        // Réinitialiser les feux de circulation
        setHorizontalLights(TrafficLight.State.GREEN);
        setVerticalLights(TrafficLight.State.RED);

        // Réactiver les contrôles
        startButton.setEnabled(true);
        carsPerLaneSpinner.setEnabled(true);
        spawnDelaySpinner.setEnabled(true);

        // Rafraîchir l'affichage
        repaint();
    }

    private void startSimulation() {
        carsPerLane = (int) carsPerLaneSpinner.getValue();
        spawnDelay = (int) spawnDelaySpinner.getValue();

        // Liste des routes principales
        List<Road> mainRoads = List.of(
                roads.get(0),  // horizontalRoadRight
                roads.get(1),  // horizontalRoadLeft
                roads.get(2),  // verticalRoadRight
                roads.get(3)   // verticalRoadLeft
        );

        // Créer et ajouter les véhicules pour chaque route principale
        mainRoads.forEach(road -> {
            // Créer le premier véhicule immédiatement
            createAndAddVehicle(road);


            // Programmer la création des véhicules suivants avec le délai spécifié
            for (int i = 1; i < carsPerLane; i++) {
                Timer timer = new Timer(i * spawnDelay, e -> createAndAddVehicle(road));
                timer.setRepeats(false);
                timers.add(timer); // Garder une référence au timer
                timer.start();
            }
        });

    }



    private void generateDijkstraGraph() {
        roadGraph = RoadGraph.buildFromRoads(roads);
        graphEdges.clear();


        // Pour assurer que toutes les routes sont considérées
        Set<Road> processedRoads = new HashSet<>();

        // 1. D'abord, ajouter tous les points de départ et d'arrivée au graphe
        for (Road road : roads) {
            Point startPoint = getStartPoint(road);
            Point endPoint = getEndPoint(road);

            if (startPoint != null) {
                graphEdges.putIfAbsent(startPoint, new HashSet<>());
            }

            if (endPoint != null) {
                graphEdges.putIfAbsent(endPoint, new HashSet<>());
            }
        }

        // 2. Ensuite, connecter les routes explicitement reliées
        for (Road road : roads) {
            Point startPoint = getStartPoint(road);
            Point endPoint = getEndPoint(road);

            if (startPoint == null || endPoint == null) {
                continue;
            }

            // Ajouter une connexion du point de départ au point d'arrivée
            Set<Point> connections = graphEdges.get(startPoint);
            if (connections != null) {
                connections.add(endPoint);
            }

            // Traiter les connexions explicites (nextRoads)
            for (Road nextRoad : road.getNextRoads()) {
                Point nextStartPoint = getStartPoint(nextRoad);
                if (nextStartPoint != null) {
                    connections = graphEdges.get(endPoint);
                    if (connections != null) {
                        connections.add(nextStartPoint);
                    }
                }
            }

            processedRoads.add(road);
        }

        // 3. Enfin, détecter les connexions implicites par proximité si elles n'ont pas déjà été établies
        for (Road road1 : roads) {
            Point endPoint1 = getEndPoint(road1);
            if (endPoint1 == null) continue;

            for (Road road2 : roads) {
                if (road1 == road2 || processedRoads.contains(road2)) continue;

                Point startPoint2 = getStartPoint(road2);
                if (startPoint2 == null) continue;

                // Calculer la distance entre le point final de road1 et le point initial de road2
                double distance = endPoint1.distance(startPoint2);

                // Si les points sont suffisamment proches, connecter les routes
                if (distance <= CONNECTION_THRESHOLD) {
                    Set<Point> connections = graphEdges.get(endPoint1);
                    if (connections != null && !connections.contains(startPoint2)) {
                        connections.add(startPoint2);
                    }
                }
            }
        }



        // Log pour vérification
        int totalConnections = 0;
        for (Set<Point> connections : graphEdges.values()) {
            totalConnections += connections.size();
        }


        // Vérification des routes isolées
        List<Road> isolatedRoads = new ArrayList<>();
        for (Road road : roads) {
            Point startPoint = getStartPoint(road);
            Point endPoint = getEndPoint(road);

            boolean isConnected = false;

            if (startPoint != null && graphEdges.containsKey(startPoint) && !graphEdges.get(startPoint).isEmpty()) {
                isConnected = true;
            }

            if (endPoint != null && graphEdges.containsKey(endPoint) && !graphEdges.get(endPoint).isEmpty()) {
                isConnected = true;
            }

            if (!isConnected) {
                isolatedRoads.add(road);
            }
        }

        if (!isolatedRoads.isEmpty()) {
            System.out.println("ATTENTION: " + isolatedRoads.size() + " routes isolées détectées");
            for (Road road : isolatedRoads) {
                System.out.println("  - Route isolée: " + road);
            }
        }
    }




    private void initializeComponents() {
        qLearning = new TrafficLightQLearning();
        // Création des routes principales
        Road horizontalRoadLeft = new Road(0, 300, 700, Color.GRAY, true, false);
        Road horizontalRoadRight = new Road(0, 320, 700, Color.DARK_GRAY, true, true);
        Road verticalRoadRight = new Road(390, 0, 600, Color.GRAY, false, true);
        Road verticalRoadLeft = new Road(410, 0, 600, Color.DARK_GRAY, false, false);


        // Création des virages
        List<RoadSegment> horizontalLeftTurn = new ArrayList<>();
        horizontalLeftTurn.add(new RoadSegment(700, 200, 100, false, true));
        Road horizontalLeftTurnRoad = new Road(horizontalLeftTurn, Color.GRAY, true, false);


        List<RoadSegment> verticalLeftTurn = new ArrayList<>();
        verticalLeftTurn.add(new RoadSegment(410, 600, 100, true, false));
        Road verticalLeftTurnRoad = new Road(verticalLeftTurn, Color.DARK_GRAY, false, false);

        List<RoadSegment> horizontalRightTurn = new ArrayList<>();
        horizontalRightTurn.add(new RoadSegment(700, 320, 100, false, false));
        Road horizontalRightTurnRoad = new Road(horizontalRightTurn, Color.DARK_GRAY, false, true);

        List<RoadSegment> verticalRightTurn = new ArrayList<>();
        verticalRightTurn.add(new RoadSegment(290, 600, 100, true, true));
        Road verticalRightTurnRoad = new Road(verticalRightTurn, Color.GRAY, true, true);

        // Créations des routes apres les virages
        Road afterHorizontalLeftTurn = new Road(700, 200, 400, Color.GRAY, true, false);
        Road afterVerticalRightTurn = new Road(290, 600, 300, Color.GRAY, false, true);
        Road afterHorizontalRightTurn = new Road(700, 420, 400, Color.DARK_GRAY, true, true);
        Road afterVerticalLeftTurn = new Road(510, 600, 300, Color.DARK_GRAY, false, false);


        List<RoadSegment> verticalLeftTurn2 = new ArrayList<>();
        verticalLeftTurn2.add(new RoadSegment(290, 900, 100, true, false));
        Road verticalLeftTurnRoad2 = new Road(verticalLeftTurn2, Color.GRAY, false, false);

        List<RoadSegment> verticalRightTurn3 = new ArrayList<>();
        verticalRightTurn3.add(new RoadSegment(410, 900, 100, true, true));
        Road verticalRightTurnRoad3 = new Road(verticalRightTurn3, Color.DARK_GRAY, true, true);

        Road horizontalRoad2 = new Road(400, 900, 50, Color.DARK_GRAY, false, false);

        List<RoadSegment> verticalLeftTurn3 = new ArrayList<>();
        verticalLeftTurn3.add(new RoadSegment(500, 900, 200, true, false));
        Road verticalLeftTurnRoad3 = new Road(verticalLeftTurn3, Color.DARK_GRAY, false, false);



        List<RoadSegment> horizontalLeftTurn3 = new ArrayList<>();
        horizontalLeftTurn3.add(new RoadSegment(1100, 320, 100, false, false));
        Road horizontalLeftTurnRoad3 = new Road(horizontalLeftTurn3, Color.DARK_GRAY, true, true);

        List<RoadSegment> horizontalRightTurn3 = new ArrayList<>();
        horizontalRightTurn3.add(new RoadSegment(1100, 200, 100, false, false));
        Road horizontalRightTurnRoad3 = new Road(horizontalRightTurn3, Color.GRAY, false, false);

        List<RoadSegment> horizontalRightTurn4 = new ArrayList<>();
        horizontalRightTurn4.add(new RoadSegment(1100, 410, 100, false, false));
        Road horizontalRightTurnRoad4 = new Road(horizontalRightTurn4, Color.DARK_GRAY, false, true);

        List<RoadSegment> horizontalLeftTurn4 = new ArrayList<>();
        horizontalLeftTurn4.add(new RoadSegment(1100, 100, 100, false, false));
        Road horizontalLeftTurnRoad4 = new Road(horizontalLeftTurn4, Color.GRAY, true, false);

        List<RoadSegment> verticalRightTurn4 = new ArrayList<>();
        verticalRightTurn4.add(new RoadSegment(190, 900, 100, true, true));
        Road verticalRightTurnRoad4 = new Road(verticalRightTurn4, Color.GRAY, true, true);

        Road horizontalRoad4 = new Road(1100, 310, 200, Color.GRAY, true, false);

        List<RoadSegment> horizontalLeftTurn5 = new ArrayList<>();
        horizontalLeftTurn5.add(new RoadSegment(1300, 210, 100, false, true));
        Road horizontalLeftTurnRoad5 = new Road(horizontalLeftTurn5, Color.GRAY, true, false);

        List<RoadSegment> horizontalRightTurn5 = new ArrayList<>();
        horizontalRightTurn5.add(new RoadSegment(1450, 330, 150, false, false));
        Road horizontalRightTurnRoad5 = new Road(horizontalRightTurn5, Color.DARK_GRAY, false, true);

        Road horizontalRoad5 = new Road(1100, 330, 350, Color.DARK_GRAY, true, true);

        // Appairer les routes adjacentes
        verticalRoadRight.setPairedRoad(verticalRoadLeft);
        verticalRoadLeft.setPairedRoad(verticalRoadRight);
        horizontalRoadRight.setPairedRoad(horizontalRoadLeft);
        horizontalRoadLeft.setPairedRoad(horizontalRoadRight);

        horizontalRoad4.setPairedRoad(horizontalRoad5);
        horizontalRoad5.setPairedRoad(horizontalRoad4);


        roads.addAll(List.of(
                horizontalRoadRight, horizontalRoadLeft,
                verticalRoadRight, verticalRoadLeft,
                horizontalLeftTurnRoad, horizontalRightTurnRoad, afterHorizontalLeftTurn,
                verticalRightTurnRoad, afterVerticalRightTurn, afterVerticalLeftTurn, afterHorizontalRightTurn,
                verticalLeftTurnRoad, verticalLeftTurnRoad2, verticalRightTurnRoad3, horizontalRoad2,
                verticalLeftTurnRoad3, horizontalRightTurnRoad3,horizontalLeftTurnRoad3,
                horizontalRoad4, horizontalRightTurnRoad4, horizontalLeftTurnRoad4, verticalRightTurnRoad4,
                horizontalRoad5, horizontalLeftTurnRoad5, horizontalRightTurnRoad5
        ));


        connectRoads();
        identifyTrueEndpoints();
        generateDijkstraGraph();




        int offset = 50;

        trafficLights.add(new TrafficLight(
                370, 300 - offset,
                TrafficLight.Direction.VERTICAL,
                TrafficLight.State.RED
        ));

        trafficLights.add(new TrafficLight(
                430, 300 - offset,
                TrafficLight.Direction.VERTICAL,
                TrafficLight.State.RED
        ));





        trafficLights.add(new TrafficLight(
                400 - offset, 280,
                TrafficLight.Direction.HORIZONTAL,
                TrafficLight.State.GREEN
        ));


        trafficLights.add(new TrafficLight(
                400 - offset, 340,
                TrafficLight.Direction.HORIZONTAL,
                TrafficLight.State.GREEN
        ));

    }





    private void connectRoads() {
        for (Road road1 : roads) {
            Point endPoint1 = getEndPoint(road1);


            if (endPoint1 == null) continue;

            for (Road road2 : roads) {
                if (road1 == road2) continue;

                Point startPoint2 = getStartPoint(road2);
                if (startPoint2 == null) continue;

                double distance = endPoint1.distance(startPoint2);

                if (distance <= CONNECTION_THRESHOLD) {
                    // Vérification de la cohérence de direction
                    boolean shouldConnect = true;

                    if (road1.isHorizontal() && road2.isHorizontal()) {
                        // Pour les routes horizontales, vérifier que road2 est bien à droite de road1
                        shouldConnect = (startPoint2.x >= endPoint1.x);
                    } else if (!road1.isHorizontal() && !road2.isHorizontal()) {
                        // Pour les routes verticales, vérifier que road2 est bien en dessous de road1
                        shouldConnect = (startPoint2.y >= endPoint1.y);
                    }


                    if (shouldConnect) {
                        road1.addNextRoad(road2);
                    } else {
                        // Si la connexion est dans le mauvais sens, inverser les routes
                        Point endPoint2 = getEndPoint(road2);
                        Point startPoint1 = getStartPoint(road1);

                        if (endPoint2 != null && startPoint1 != null &&
                                endPoint2.distance(startPoint1) <= CONNECTION_THRESHOLD) {
                            road2.addNextRoad(road1);
                        }
                    }
                }
            }


        }
    }





    /**
     * Identifie les routes qui sont des points d'entrée et de sortie
     */
    private void identifyTrueEndpoints() {
        endpointRoads = roads.stream()
                .filter(road -> road.getNextRoads().isEmpty())
                .collect(Collectors.toList());

    }


    public Road selectRandomDestination(Road startRoad) {
        roadGraph = RoadGraph.buildFromRoads(roads);
        Point startPoint = getStartPoint(startRoad);

        if (startPoint == null) return null;

        List<Road> possibleDestinations = new ArrayList<>();

        for (Road road : endpointRoads) {
            possibleDestinations.add(road);
        }



        Collections.shuffle(possibleDestinations);

        for (Road dest : possibleDestinations) {
            Point endPoint = getEndPoint(dest);
            if (endPoint == null) continue;

            List<Road> path = roadGraph.findShortestPath(startPoint, endPoint);
            if (!path.isEmpty()) {
                return dest;
            }
        }

        return null;
    }


    public Vehicle createAndAddVehicle(Road road) {
        Road destination = selectRandomDestination(road);

        if (destination == null) {
            System.out.println("Aucune destination valide trouvée pour la route: " + road);
            return null;
        }

        Point start = getStartPoint(road);
        Point end = getEndPoint(destination);
        List<Road> path = roadGraph.findShortestPath(start, end);
        int distance = calculatePathDistance(path);


        boolean bonneSante = random.nextBoolean();

        // Utiliser l'argumentation pour choisir le type de véhicule
        String typeTransport = transportArgumentation.chooseTransport(distance, bonneSante, weather);

        Vehicle vehicle;
        switch (typeTransport) {
            case "velo":
                vehicle = new Velo(road, 0, Velo.VELO_BASE_SPEED, this, destination);
                break;
            case "moto":
                vehicle = new Moto(road, 0, Moto.MOTO_BASE_SPEED, this, destination);
                break;
            default:
                vehicle = new Vehicle(road, 0, Vehicle.MAX_SPEED_CAR, this, destination);
        }

        vehicle.setPath(path);
        vehicles.add(vehicle);
        return vehicle;

    }

    private int calculatePathDistance(List<Road> path) {
        int distance = 0;
        for (Road road : path) {
            distance += road.getLength();
        }
        return distance / 100; // Conversion en kilomètres approximatifs
    }








    public void updateSimulation() {
        manageTrafficLights();
        vehicles.forEach(Vehicle::update);
        motos.forEach(Moto::update);
        velos.forEach(Velo::update);

        // Gestion des véhicules arrivés
        List<Vehicle> arrived = vehicles.stream()
                .filter(Vehicle::hasReachedDestination)
                .collect(Collectors.toList());

        List<Moto> arrivedMotos = motos.stream()
                .filter(Moto::hasReachedDestination)
                .collect(Collectors.toList());

        List<Velo> arrivedVelos = velos.stream()
                .filter(Velo::hasReachedDestination)
                .collect(Collectors.toList());

        // Mise à jour des statistiques
        arrived.forEach(v -> {
            travelTimes.put(v, v.getTravelTime());
            laneChanges.put(v, v.getLaneChangesCount());
        });

        arrivedMotos.forEach(m -> {
            travelTimes.put(m, m.getTravelTime());
            laneChanges.put(m, m.getLaneChangesCount());
        });

        arrivedVelos.forEach(v -> {
            travelTimes.put(v, v.getTravelTime());
            laneChanges.put(v, v.getLaneChangesCount());
        });

        // Suppression des véhicules arrivés
        vehicles.removeAll(arrived);
        motos.removeAll(arrivedMotos);
        velos.removeAll(arrivedVelos);



    }

    public List<Vehicle> getVehiclesOnRoad(Road road) {
        List<Vehicle> allVehicles = new ArrayList<>();

        allVehicles.addAll(
                vehicles.stream()
                        .filter(v -> v.currentRoad.equals(road))
                        .collect(Collectors.toList())
        );

        allVehicles.addAll(
                motos.stream()
                        .filter(m -> m.currentRoad.equals(road))
                        .collect(Collectors.toList())
        );

        allVehicles.addAll(
                velos.stream()
                        .filter(v -> v.currentRoad.equals(road))
                        .collect(Collectors.toList())
        );

        return Collections.unmodifiableList(allVehicles);


    }

    private void manageTrafficLights() {
        timeSinceLastChange++;

        // Classifier les véhicules selon leur orientation
        List<Vehicle> horizontalVehicles = vehicles.stream()
                .filter(v -> v.currentRoad.isHorizontal())
                .collect(Collectors.toList());

        List<Vehicle> verticalVehicles = vehicles.stream()
                .filter(v -> !v.currentRoad.isHorizontal())
                .collect(Collectors.toList());

        // Mettre à jour l'état du trafic dans le système Q-learning
        qLearning.updateTrafficState(horizontalVehicles, verticalVehicles,
                currentTrafficPhase, timeSinceLastChange);

        // Choisir l'action
        TrafficLightQLearning.Action action = qLearning.chooseAction();

        // Exécuter l'action
        TrafficLightQLearning.TrafficPhase newPhase = qLearning.executeAction(currentTrafficPhase, timeSinceLastChange);

        // Si la phase a changé
        if (newPhase != currentTrafficPhase) {
            currentTrafficPhase = newPhase;
            timeSinceLastChange = 0;

            // Appliquer la nouvelle phase aux feux de circulation
            switch (currentTrafficPhase) {
                case HORIZONTAL_GREEN:
                    setHorizontalLights(TrafficLight.State.GREEN);
                    setVerticalLights(TrafficLight.State.RED);
                    break;
                case HORIZONTAL_ORANGE:
                    setHorizontalLights(TrafficLight.State.ORANGE);
                    setVerticalLights(TrafficLight.State.RED);
                    break;
                case VERTICAL_GREEN:
                    setHorizontalLights(TrafficLight.State.RED);
                    setVerticalLights(TrafficLight.State.GREEN);
                    break;
                case VERTICAL_ORANGE:
                    setHorizontalLights(TrafficLight.State.RED);
                    setVerticalLights(TrafficLight.State.ORANGE);
                    break;
            }
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



    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.WHITE);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.BLACK);
        g2d.drawString("Météo: " + weather.getCurrentCondition(), 10, getHeight() - 60);
        g2d.drawString("Température: " + weather.getTemperature() + "°C", 10, getHeight() - 45);
        g2d.drawString("Vent: " + weather.getWindSpeed() + " km/h", 10, getHeight() - 30);
        g2d.drawString("Visibilité: " + weather.getVisibility() + " m", 10, getHeight() - 15);


        roads.forEach(r -> r.draw(g));
        g.setColor(Color.BLACK);
        trafficLights.forEach(t -> t.draw(g));
        vehicles.forEach(v -> v.draw(g));
        motos.forEach(m -> m.draw(g));
        velos.forEach(v -> v.draw(g));

        g.setColor(Color.RED);
        AtomicInteger yPos = new AtomicInteger(30);
        g.drawString("Métriques Globales :", 10, yPos.get());
        yPos.addAndGet(15);
        g.drawString("Véhicules arrivés: " + travelTimes.size(), 10, yPos.get());
        yPos.addAndGet(15);


        if (!travelTimes.isEmpty()) {
            double avgTravelTime = travelTimes.values().stream().mapToLong(Long::longValue).average().orElse(0) / 1000;
            double avgLaneChanges = laneChanges.values().stream().mapToInt(Integer::intValue).average().orElse(0);

            g.drawString(String.format("Temps moyen de trajet: %.1f s", avgTravelTime), 10, yPos.get());
            yPos.addAndGet(15);
            g.drawString(String.format("Changements de voie moyens: %.1f", avgLaneChanges), 10, yPos.get());
        }

        if (showGraph) {
            drawDijkstraGraph(g);
        }

        if (showDestinations) {
            drawDestinations(g);
            drawValidatedPaths(g);
        }


    }



    private void drawDestinations(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // Configuration du style de dessin
        Stroke originalStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(2.0f));

        // Dessiner les destinations pour les véhicules
        for (Vehicle vehicle : vehicles) {
            drawVehicleDestination(g2d, vehicle);
        }

        // Dessiner les destinations pour les motos
        for (Moto moto : motos) {
            drawVehicleDestination(g2d, moto);
        }

        for (Velo velo : velos) {
            drawVehicleDestination(g2d, velo);
        }

        // Dessiner les points de destination possibles
        g.setColor(new Color(0, 255, 0, 100));
        for (Road dest : endpointRoads) {
            Point p = getEndPoint(dest);
            g.fillOval(p.x-8, p.y-8, 16, 16);
        }

        // Restaurer le style original
        g2d.setStroke(originalStroke);
    }

    private void drawVehicleDestination(Graphics2D g2d, Vehicle vehicle) {
        Road destinationRoad = vehicle.getDestination();
        if (destinationRoad != null) {
            Point vehiclePos = vehicle.getPosition();
            Point destinationPoint = getEndPoint(destinationRoad);

            if (vehiclePos != null && destinationPoint != null) {
                // Utiliser une couleur différente pour les motos
                Color vehicleColor = vehicle instanceof Moto ?
                        new Color(100, 100, 100) : // Gris pour les motos
                        vehicle.getColor();

                // Dessiner la ligne vers la destination
                g2d.setColor(new Color(vehicleColor.getRed(), vehicleColor.getGreen(),
                        vehicleColor.getBlue(), 100)); // Semi-transparent
                g2d.drawLine(vehiclePos.x, vehiclePos.y, destinationPoint.x, destinationPoint.y);

                // Dessiner le marqueur de destination
                int destinationSize = vehicle instanceof Moto ? 8 : 12; // Plus petit pour les motos
                g2d.setColor(new Color(vehicleColor.getRed(), vehicleColor.getGreen(),
                        vehicleColor.getBlue(), 180)); // Plus opaque
                g2d.fillOval(destinationPoint.x - destinationSize/2,
                        destinationPoint.y - destinationSize/2,
                        destinationSize, destinationSize);
            }
        }
    }



    public Map<Road, Map<Road, List<Road>>> validateAllPaths() {
        Map<Road, Map<Road, List<Road>>> validPathsMap = new HashMap<>();
        RoadGraph graph = RoadGraph.buildFromRoads(roads);

        // Pour chaque route de départ possible
        for (Road startRoad : roads) {
            Point startPoint = getStartPoint(startRoad);
            if (startPoint == null) continue;

            Map<Road, List<Road>> destinationsWithPaths = new HashMap<>();

            // Vérifier chaque destination possible
            for (Road endRoad : endpointRoads) {
                if (startRoad.equals(endRoad)) continue;

                Point endPoint = getEndPoint(endRoad);
                if (endPoint == null) continue;

                List<Road> path = graph.findShortestPath(startPoint, endPoint);
                if (!path.isEmpty()) {
                    destinationsWithPaths.put(endRoad, path);
                }
            }

            if (!destinationsWithPaths.isEmpty()) {
                validPathsMap.put(startRoad, destinationsWithPaths);
            }
        }

        return validPathsMap;
    }

    // Ajoutez cette méthode à votre SimulationPanel
    private void drawValidatedPaths(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Stroke originalStroke = g2d.getStroke();

        // Récupérer tous les chemins valides
        Map<Road, Map<Road, List<Road>>> allPaths = validateAllPaths();

        // Paramètres visuels
        float[] dashPattern = {5, 5};
        BasicStroke dashedStroke = new BasicStroke(2, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 1, dashPattern, 0);

        int colorIndex = 0;
        Color[] palette = {new Color(255,0,0,100), new Color(0,255,0,100),
                new Color(0,0,255,100), new Color(255,255,0,100)};

        // Dessiner chaque chemin
        for (Map.Entry<Road, Map<Road, List<Road>>> entry : allPaths.entrySet()) {
            Road startRoad = entry.getKey();
            Point startPoint = getStartPoint(startRoad);

            for (Map.Entry<Road, List<Road>> pathEntry : entry.getValue().entrySet()) {
                Road endRoad = pathEntry.getKey();
                List<Road> path = pathEntry.getValue();

                // Choisir une couleur
                Color pathColor = palette[colorIndex % palette.length];
                g2d.setColor(pathColor);
                g2d.setStroke(dashedStroke);

                // Dessiner le chemin
                Point currentPoint = startPoint;
                for (Road road : path) {
                    Point nextPoint = getEndPoint(road);
                    g2d.drawLine(currentPoint.x, currentPoint.y, nextPoint.x, nextPoint.y);
                    currentPoint = nextPoint;
                }

                // Marquage des points
                g2d.setStroke(new BasicStroke(3));
                g2d.fillOval(startPoint.x-4, startPoint.y-4, 8, 8); // Départ
                g2d.fillOval(currentPoint.x-6, currentPoint.y-6, 12, 12); // Arrivée

                colorIndex++;
            }
        }

        g2d.setStroke(originalStroke);

    }




    private void drawDijkstraGraph(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // Configuration du style de dessin
        Stroke originalStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
        g2d.setColor(new Color(50, 150, 255, 180)); // Bleu semi-transparent

        // Dessiner chaque connexion du graphe
        for (Map.Entry<Point, Set<Point>> entry : graphEdges.entrySet()) {
            Point from = entry.getKey();

            for (Point to : entry.getValue()) {
                // Dessiner la ligne représentant une arête
                g2d.drawLine(from.x, from.y, to.x, to.y);

                // Dessiner une flèche pour montrer la direction
                drawArrowHead(g2d, from, to);
            }

            // Dessiner le point (nœud)
            int nodeSize = 8;
            g2d.fillOval(from.x - nodeSize/2, from.y - nodeSize/2, nodeSize, nodeSize);
        }

        // Restaurer le style original
        g2d.setStroke(originalStroke);
    }

    private void drawArrowHead(Graphics2D g2d, Point from, Point to) {
        int arrowSize = 10;
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double angle = Math.atan2(dy, dx);

        // Calculer la position de la flèche (90% du chemin pour ne pas toucher le nœud)
        int x = (int) (from.x + 0.9 * dx);
        int y = (int) (from.y + 0.9 * dy);

        // Dessiner la pointe de flèche
        int[] xPoints = {
                x,
                (int) (x - arrowSize * Math.cos(angle - Math.PI/6)),
                (int) (x - arrowSize * Math.cos(angle + Math.PI/6))
        };

        int[] yPoints = {
                y,
                (int) (y - arrowSize * Math.sin(angle - Math.PI/6)),
                (int) (y - arrowSize * Math.sin(angle + Math.PI/6))
        };

        g2d.fillPolygon(xPoints, yPoints, 3);
    }


    public List<Vehicle> getVehiclesInLane(Vehicle requester) {
        List<Vehicle> allVehiclesInLane = new ArrayList<>();

        // Ajouter les véhicules dans la même voie
        allVehiclesInLane.addAll(
                vehicles.stream()
                        .filter(v -> v != requester)
                        .filter(v -> v.currentRoad == requester.currentRoad)
                        .filter(v -> Math.abs(v.laneOffset - requester.laneOffset) < 15)
                        .collect(Collectors.toList())
        );

        // Ajouter les motos dans la même voie
        allVehiclesInLane.addAll(
                motos.stream()
                        .filter(m -> m != requester)
                        .filter(m -> m.currentRoad == requester.currentRoad)
                        .filter(m -> Math.abs(m.laneOffset - requester.laneOffset) < 15)
                        .collect(Collectors.toList())
        );

        allVehiclesInLane.addAll(
                velos.stream()
                        .filter(m -> m != requester)
                        .filter(m -> m.currentRoad == requester.currentRoad)
                        .filter(m -> Math.abs(m.laneOffset - requester.laneOffset) < 15)
                        .collect(Collectors.toList())
        );

        return allVehiclesInLane;

    }


    public Optional<TrafficLight> getNextTrafficLight(Vehicle vehicle) {
        return trafficLights.stream()

                .filter(light ->
                        light.isInPath(vehicle.getFrontPosition(), vehicle.currentRoad.isHorizontal())
                )
                .min(Comparator.comparingDouble(light ->
                        vehicle.getPosition().distance(light.getPosition())
                ));
    }
}