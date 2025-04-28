package org.trafficSimulation.model.agents;

import org.trafficSimulation.model.road.Road;
import org.trafficSimulation.view.SimulationPanel;
import java.awt.*;

public class Velo extends Vehicle {
    // Constantes spécifiques aux motos
    public static final int VELO_BASE_SPEED = 1;        // Vitesse de base plus élevée
    private static final int VELO_WIDTH = 3;             // Largeur plus petite
    private static final int VELO_LENGTH = 10;           // Longueur plus petite

    public Velo(Road road, int initialPos, int speed, SimulationPanel environment, Road destination) {
        super(road, initialPos, speed, environment, destination);
        this.baseSpeed = VELO_BASE_SPEED;
        this.MAX_SPEED = MAX_SPEED_VELO;

        setColor(Color.BLACK);  // Couleur par défaut pour les motos
    }

    @Override
    public void draw(Graphics g) {
        Point pos = getPosition();
        g.setColor(getColor());

        boolean isHorizontal = currentRoad.getCurrentSegment(pos).isHorizontal();
        boolean isReverse = currentRoad.getCurrentSegment(pos).isReverse();

        if (isHorizontal) {
            if (isReverse) {
                g.fillRect(pos.x, pos.y - VELO_WIDTH/2, VELO_LENGTH, VELO_LENGTH);
            } else {
                g.fillRect(pos.x - VELO_LENGTH, pos.y - VELO_WIDTH/2, VELO_LENGTH, VELO_WIDTH);
            }
        } else {
            if (isReverse) {
                g.fillRect(pos.x - VELO_WIDTH/2, pos.y, VELO_WIDTH, VELO_LENGTH);
            } else {
                g.fillRect(pos.x - VELO_WIDTH/2, pos.y - VELO_LENGTH, VELO_WIDTH, VELO_LENGTH);
            }
        }
    }
}