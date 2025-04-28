package org.trafficSimulation.model.agents;

import org.trafficSimulation.model.road.Road;
import org.trafficSimulation.view.SimulationPanel;
import java.awt.*;

public class Moto extends Vehicle {
    // Constantes spécifiques aux motos
    public static final int MOTO_BASE_SPEED = 3;        // Vitesse de base plus élevée
    private static final int MOTO_WIDTH = 5;             // Largeur plus petite
    private static final int MOTO_LENGTH = 15;           // Longueur plus petite

    public Moto(Road road, int initialPos, int speed, SimulationPanel environment, Road destination) {
        super(road, initialPos, speed, environment, destination);
        this.baseSpeed = MOTO_BASE_SPEED;
        this.MAX_SPEED = MAX_SPEED_MOTO;
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
                g.fillRect(pos.x, pos.y - MOTO_WIDTH/2, MOTO_LENGTH, MOTO_WIDTH);
            } else {
                g.fillRect(pos.x - MOTO_LENGTH, pos.y - MOTO_WIDTH/2, MOTO_LENGTH, MOTO_WIDTH);
            }
        } else {
            if (isReverse) {
                g.fillRect(pos.x - MOTO_WIDTH/2, pos.y, MOTO_WIDTH, MOTO_LENGTH);
            } else {
                g.fillRect(pos.x - MOTO_WIDTH/2, pos.y - MOTO_LENGTH, MOTO_WIDTH, MOTO_LENGTH);
            }
        }
    }
}