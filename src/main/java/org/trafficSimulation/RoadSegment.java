package org.trafficSimulation;

import java.awt.*;

public class RoadSegment {
    int startX;
    int startY;

    public boolean isReverse() {
        return reverse;
    }

    private int length;
    private boolean horizontal;
    private boolean reverse;

    public RoadSegment(int startX, int startY, int length, boolean horizontal, boolean reverse) {
        this.startX = startX;
        this.startY = startY;
        this.length = length;
        this.horizontal = horizontal;
        this.reverse = reverse;
    }

    public Point getPosition(int progression, int laneOffset) {
        if(horizontal) {
            return new Point(startX + progression, startY + laneOffset);
        } else {
            return new Point(startX + laneOffset, startY + progression);
        }
    }

    public void draw(Graphics g, Color color) {
        g.setColor(color);
        if(horizontal) {
            g.fillRect(startX, startY - 10, length, 20);
        } else {
            g.fillRect(startX - 10, startY, 20, length);
        }
    }

    public boolean contains(Point p) {
        return getBounds().contains(p);
    }

    public Rectangle getBounds() {
        if(horizontal) {
            return new Rectangle(startX, startY - 10, length, 20);
        } else {
            return new Rectangle(startX - 10, startY, 20, length);
        }
    }


    public void setNextSegment(RoadSegment next) {
        if(reverse) {
            next.startX = startX;

            next.startY = startY + length;
        }
        else {

            if (horizontal) {
                next.startX = startX + length;
                next.startY = startY;
            } else {
                next.startX = startX;
                next.startY = startY + length;
            }
        }
    }

    public int getLength() { return length; }
    public boolean isHorizontal() { return horizontal; }
}