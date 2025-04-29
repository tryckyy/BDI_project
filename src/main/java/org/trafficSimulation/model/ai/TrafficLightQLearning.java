package org.trafficSimulation.model.ai;

import java.util.*;

import org.trafficSimulation.model.agents.Vehicle;

import javax.swing.*;

public class TrafficLightQLearning{
    // Paramètres du Q-learning
    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.9;
    private static final double EXPLORATION_RATE = 0.1;
    private static final int NUM_ACTIONS = 4; // 4 phases possibles
    private static final int ORANGE_DURATION = 30;
    private static final int MIN_ORANGE_DURATION = 20;
    private static final int ALL_RED_DURATION = 20;
    private static final int MIN_GREEN_DURATION = 15;
    private static final int MAX_GREEN_DURATION = 60;
    private TrafficPhase lastGreenPhase;


    // États possibles du système de feux de circulation
    public enum TrafficPhase {
        HORIZONTAL_GREEN, HORIZONTAL_ORANGE,
        VERTICAL_GREEN, VERTICAL_ORANGE, ALL_RED, VERTICAL_RED,
        HORIZONTAL_RED
    }



    // Représentation de l'état du trafic
    public static class TrafficState {
        private final int horizontalTrafficDensity; // 0-10
        private final int verticalTrafficDensity;   // 0-10
        private final TrafficPhase currentPhase;
        private final int timeSinceLastChange;      // en secondes

        public TrafficState(int horizontalDensity, int verticalDensity,
                            TrafficPhase phase, int timeSinceChange) {
            this.horizontalTrafficDensity = Math.min(10, Math.max(0, horizontalDensity));
            this.verticalTrafficDensity = Math.min(10, Math.max(0, verticalDensity));
            this.currentPhase = phase;
            this.timeSinceLastChange = timeSinceChange;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TrafficState that = (TrafficState) o;
            return horizontalTrafficDensity == that.horizontalTrafficDensity &&
                    verticalTrafficDensity == that.verticalTrafficDensity &&
                    currentPhase == that.currentPhase &&
                    timeSinceLastChange == that.timeSinceLastChange;
        }

        @Override
        public int hashCode() {
            return Objects.hash(horizontalTrafficDensity, verticalTrafficDensity,
                    currentPhase, timeSinceLastChange);
        }

        @Override
        public String toString() {
            return String.format("H:%d V:%d Phase:%s Time:%d",
                    horizontalTrafficDensity, verticalTrafficDensity,
                    currentPhase, timeSinceLastChange);
        }
    }

    // Actions possibles
    public enum Action {
        MAINTAIN_PHASE,
        CHANGE_TO_HORIZONTAL,
        CHANGE_TO_VERTICAL,
        TOGGLE_PHASE
    }

    // Table Q pour stocker les valeurs d'action-état
    private final Map<TrafficState, double[]> qTable;

    // État actuel et action
    private TrafficState currentState;
    private Action currentAction;

    // Statistiques pour le calcul des récompenses
    private int totalVehiclesHorizontal;
    private int totalVehiclesVertical;
    private int waitingVehiclesHorizontal;
    private int waitingVehiclesVertical;

    public TrafficLightQLearning() {
        this.qTable = new HashMap<>();

        this.currentState = new TrafficState(0, 0, TrafficPhase.HORIZONTAL_GREEN, 0);
        this.currentAction = Action.MAINTAIN_PHASE;
    }

    // Mise à jour de l'état du trafic en fonction des véhicules présents
    public void updateTrafficState(List<Vehicle> horizontalVehicles, List<Vehicle> verticalVehicles,
                                   TrafficPhase currentPhase, int timeSinceLastChange) {
        int horizontalDensity = calculateTrafficDensity(horizontalVehicles);
        int verticalDensity = calculateTrafficDensity(verticalVehicles);



        totalVehiclesHorizontal = horizontalVehicles.size();
        totalVehiclesVertical = verticalVehicles.size();
        waitingVehiclesHorizontal = countWaitingVehicles(horizontalVehicles);
        waitingVehiclesVertical = countWaitingVehicles(verticalVehicles);

        TrafficState newState = new TrafficState(horizontalDensity, verticalDensity,
                currentPhase, timeSinceLastChange);

        // Si c'est un nouvel état, initialiser ses valeurs Q
        if (!qTable.containsKey(newState)) {
            qTable.put(newState, new double[NUM_ACTIONS]);
        }

        // Appliquer l'apprentissage si nous avons un état précédent
        if (currentState != null) {
            double reward = calculateReward();
            learn(currentState, currentAction, reward, newState);

        }




        currentState = newState;
    }

    // Calcule la densité du trafic (0-10) en fonction du nombre de véhicules
    private int calculateTrafficDensity(List<Vehicle> vehicles) {
        // La densité est proportionnelle au nombre de véhicules, avec un maximum de 10
        return Math.min(10, vehicles.size() / 2);
    }

    // Compte les véhicules à l'arrêt
    private int countWaitingVehicles(List<Vehicle> vehicles) {
        int count = 0;
        for (Vehicle v : vehicles) {
            if (v.getPosition() != null && v.distanceTo(v) < 10) {
                count++;
            }
        }
        return count;
    }



    // Calcule la récompense en fonction de l'état du trafic
    private double calculateReward() {
        // Facteurs de récompense
        double waitingPenalty = -2.0;
        double flowReward = 1.0;
        double changePhasePenalty = -5.0; // Pénalité augmentée
        double orangePhaseReward = 0.5; // Récompense pour respecter la phase orange

        double reward = 0;

        // Pénalité pour les véhicules en attente
        reward += waitingPenalty * (waitingVehiclesHorizontal + waitingVehiclesVertical);

        // Récompense pour le flux fluide
        int movingVehiclesHorizontal = totalVehiclesHorizontal - waitingVehiclesHorizontal;
        int movingVehiclesVertical = totalVehiclesVertical - waitingVehiclesVertical;

        if (currentState.currentPhase == TrafficPhase.ALL_RED) {
            reward -= 1.0; // Légère pénalité pour le temps d'arrêt total
        }

        // Récompense supplémentaire pour respecter la phase orange
        if (currentState.currentPhase == TrafficPhase.HORIZONTAL_ORANGE ||
                currentState.currentPhase == TrafficPhase.VERTICAL_ORANGE) {
            reward += orangePhaseReward * MIN_ORANGE_DURATION;
        }

        // Récompensons le mouvement dans la direction qui a le feu vert
        if (currentState.currentPhase == TrafficPhase.HORIZONTAL_GREEN) {
            reward += flowReward * movingVehiclesHorizontal;
        } else if (currentState.currentPhase == TrafficPhase.VERTICAL_GREEN) {
            reward += flowReward * movingVehiclesVertical;
        }

        // Pénalité plus forte pour les changements de phase trop fréquents
        if (currentAction != Action.MAINTAIN_PHASE && currentState.timeSinceLastChange < 30) {
            reward += changePhasePenalty * (30 - currentState.timeSinceLastChange);
        }

        return reward;
    }

    // Fonction d'apprentissage Q
    private void learn(TrafficState state, Action action, double reward, TrafficState nextState) {
        if (!qTable.containsKey(state)) {
            qTable.put(state, new double[NUM_ACTIONS]);
        }

        if (!qTable.containsKey(nextState)) {
            qTable.put(nextState, new double[NUM_ACTIONS]);
        }
        int actionIndex = action.ordinal();

        // Obtenir les valeurs Q actuelles
        double[] qValues = qTable.get(state);
        double[] nextQValues = qTable.get(nextState);

        // Calcul de la valeur maximale de Q pour l'état suivant
        double maxNextQ = getMaxQValue(nextQValues);

        // Mise à jour de la valeur Q selon la formule du Q-learning
        qValues[actionIndex] = qValues[actionIndex] +
                LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxNextQ - qValues[actionIndex]);
    }

    // Obtient la valeur maximale de Q pour un ensemble de valeurs
    private double getMaxQValue(double[] qValues) {
        if (qValues == null || qValues.length == 0) {
            return 0.0; // Or some default value
        }

        double max = Double.NEGATIVE_INFINITY;
        for (double value : qValues) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    // Choisit la meilleure action selon la politique epsilon-greedy
    public Action chooseAction() {
        // Forcer le changement si durée maximale atteinte
        if (currentState != null) {
            if ((currentState.currentPhase == TrafficPhase.HORIZONTAL_GREEN
                    && currentState.timeSinceLastChange >= MAX_GREEN_DURATION) ||
                    (currentState.currentPhase == TrafficPhase.VERTICAL_GREEN
                            && currentState.timeSinceLastChange >= MAX_GREEN_DURATION)) {
                return Action.TOGGLE_PHASE;
            }
        }

        // Si nous sommes en phase orange ou rouge, seule l'action MAINTAIN_PHASE est valide
        if (currentState != null &&
                (currentState.currentPhase == TrafficPhase.HORIZONTAL_ORANGE ||
                        currentState.currentPhase == TrafficPhase.VERTICAL_ORANGE ||
                        currentState.currentPhase == TrafficPhase.HORIZONTAL_RED ||
                        currentState.currentPhase == TrafficPhase.VERTICAL_RED ||
                        currentState.currentPhase == TrafficPhase.ALL_RED)) {
            return Action.MAINTAIN_PHASE;
        }

        // Logique normale d'exploration/exploitation
        if (Math.random() < EXPLORATION_RATE) {
            return getRandomValidAction();
        }

        return getBestActionFromQTable(currentState);
    }

    private Action getRandomValidAction() {
        List<Action> validActions = new ArrayList<>();

        if (currentState == null) {
            return Action.MAINTAIN_PHASE;
        }

        // Toujours autoriser MAINTAIN_PHASE
        validActions.add(Action.MAINTAIN_PHASE);

        // Ajouter d'autres actions selon l'état actuel
        switch (currentState.currentPhase) {
            case HORIZONTAL_GREEN:
                if (currentState.timeSinceLastChange >= MIN_GREEN_DURATION) {
                    validActions.add(Action.CHANGE_TO_VERTICAL);
                    validActions.add(Action.TOGGLE_PHASE);
                }
                break;

            case VERTICAL_GREEN:
                if (currentState.timeSinceLastChange >= MIN_GREEN_DURATION) {
                    validActions.add(Action.CHANGE_TO_HORIZONTAL);
                    validActions.add(Action.TOGGLE_PHASE);
                }
                break;

            // Pour les autres phases, seule MAINTAIN_PHASE est valide
            default:
                break;
        }

        return validActions.get(new Random().nextInt(validActions.size()));
    }



    private Action getBestActionFromQTable(TrafficState state) {
        if (!qTable.containsKey(state)) {
            return Action.MAINTAIN_PHASE; // Action par défaut si l'état n'existe pas
        }

        double[] qValues = qTable.get(state);
        int bestActionIndex = 0;
        double maxQValue = qValues[0];

        for (int i = 1; i < qValues.length; i++) {
            if (qValues[i] > maxQValue) {
                maxQValue = qValues[i];
                bestActionIndex = i;
            }
        }

        return Action.values()[bestActionIndex];
    }

    // Exécute l'action sur les feux de circulation
    public TrafficPhase executeAction(TrafficPhase currentPhase, int timeSinceLastChange) {
        switch (currentPhase) {
            // Feu vert horizontal
            case HORIZONTAL_GREEN:
                if (shouldChangeToOrange(timeSinceLastChange)) {
                    lastGreenPhase = TrafficPhase.HORIZONTAL_GREEN;
                    return TrafficPhase.HORIZONTAL_ORANGE;
                }
                return TrafficPhase.HORIZONTAL_GREEN;

            // Feu orange horizontal
            case HORIZONTAL_ORANGE:
                if (timeSinceLastChange >= ORANGE_DURATION) {
                    return TrafficPhase.HORIZONTAL_RED;
                }
                return TrafficPhase.HORIZONTAL_ORANGE;

            // Feu rouge horizontal (attente avant tout rouge)
            case HORIZONTAL_RED:
                if (timeSinceLastChange >= ALL_RED_DURATION) {
                    return TrafficPhase.ALL_RED;
                }
                return TrafficPhase.HORIZONTAL_RED;

            // Tous feux rouges (transition)
            case ALL_RED:
                if (timeSinceLastChange >= ALL_RED_DURATION) {
                    return getNextGreenPhase();
                }
                return TrafficPhase.ALL_RED;

            // Même logique pour la direction verticale
            case VERTICAL_GREEN:
                if (shouldChangeToOrange(timeSinceLastChange)) {
                    lastGreenPhase = TrafficPhase.VERTICAL_GREEN;
                    return TrafficPhase.VERTICAL_ORANGE;
                }
                return TrafficPhase.VERTICAL_GREEN;

            case VERTICAL_ORANGE:
                if (timeSinceLastChange >= ORANGE_DURATION) {
                    return TrafficPhase.VERTICAL_RED;
                }
                return TrafficPhase.VERTICAL_ORANGE;

            case VERTICAL_RED:
                if (timeSinceLastChange >= ALL_RED_DURATION) {
                    return TrafficPhase.ALL_RED;
                }
                return TrafficPhase.VERTICAL_RED;

            default:
                return currentPhase;
        }
    }

    private boolean shouldChangeToOrange(int timeSinceLastChange) {
        // Changement si durée maximale atteinte OU action demandée avec durée minimale respectée
        return timeSinceLastChange >= MAX_GREEN_DURATION ||
                ((currentAction == Action.CHANGE_TO_VERTICAL || currentAction == Action.TOGGLE_PHASE)
                        && timeSinceLastChange >= MIN_GREEN_DURATION);
    }

    private TrafficPhase getNextGreenPhase() {
        if (lastDirectionWasHorizontal()) {
            return TrafficPhase.VERTICAL_GREEN;
        }
        return TrafficPhase.HORIZONTAL_GREEN;
    }

    private boolean lastDirectionWasHorizontal() {
        return lastGreenPhase == TrafficPhase.HORIZONTAL_GREEN;
    }



}
