package org.example;

import javax.swing.text.Position;
import java.awt.*;

class TrafficLight {
    enum State { RED, GREEN, ORANGE }
    enum Direction { HORIZONTAL, VERTICAL }

    private int x;
    private int y;
    private final Direction direction;
    private final Point stopLinePosition;
    private State state;
    private int timer;

    Point getPosition() {
        return new Point (x, y);
    }

    public TrafficLight(int x, int y, Direction direction, State initialState) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.state = initialState;
        this.stopLinePosition = calculateStopLinePosition();
    }

    private Point calculateStopLinePosition() {
        if(direction == Direction.HORIZONTAL) {
            return new Point(x, y - 15); // 15px avant le feu sur l'axe vertical
        } else {
            return new Point(x - 15, y); // 15px avant le feu sur l'axe horizontal
        }
    }

    public void setState(State newState) { // Added setter
        this.state = newState;
    }

    public void update() {
        if(timer++ > 100) {
            state = (state == State.RED) ? State.GREEN : State.RED;
            timer = 0;
        }
    }

    public void draw(Graphics g) {
        // Dessiner le feu
        g.setColor(Color.BLACK);
        g.fillRect(x - 3, y - 3, 26, 26);
        if (state == State.RED) {
            g.setColor(Color.RED);
        } else if (state == State.GREEN) {
            g.setColor(Color.GREEN);
        } else {
            g.setColor(Color.ORANGE); // Orange for ORANGE state
        }
        g.fillRect(x, y, 20, 20);

        // Dessiner la ligne d'arrêt
        g.setColor(Color.WHITE);
        if(direction == Direction.HORIZONTAL) {
            g.drawLine(stopLinePosition.x, stopLinePosition.y - 2, stopLinePosition.x, stopLinePosition.y + 2);
        } else {
            g.drawLine(stopLinePosition.x - 2, stopLinePosition.y, stopLinePosition.x + 2, stopLinePosition.y);
        }
    }

    public boolean isInRange(int vehicleX, int vehicleY) {
        return switch(direction) {
            case HORIZONTAL ->
                    Math.abs(vehicleY - y) < 15 &&  // Alignement vertical
                            vehicleX >= x - 50 && vehicleX <= x + 50;  // Portée avant/après

            case VERTICAL ->
                    Math.abs(vehicleX - x) < 30 &&  // Couvre les 2 voies (15px chaque côté)
                            vehicleY >= y - 50 && vehicleY <= y + 50;
        };
    }
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isInPath(Point vehicleFront, boolean isVehicleHorizontal) {
        // Vérifier la cohérence de direction
        boolean directionMatch = (this.direction == Direction.HORIZONTAL && isVehicleHorizontal)
                || (this.direction == Direction.VERTICAL && !isVehicleHorizontal);

        // Vérifier la proximité spatiale
        int detectionRange = 50;
        return directionMatch &&
                Math.abs(vehicleFront.x - x) < detectionRange &&
                Math.abs(vehicleFront.y - y) < detectionRange;
    }



    public State getState() { return state; }
    public Direction getDirection() { return direction; }
}