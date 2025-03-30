package org.example;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Road {
    private List<RoadSegment> segments;
    private Color color;
    private Road nextRoad;
    private boolean isRightLane;
    private Road pairedRoad;

    public Road getPairedRoad() { return pairedRoad; }
    public void setPairedRoad(Road pairedRoad) { this.pairedRoad = pairedRoad; }

    public boolean isRightLane() {
        return isRightLane;
    }



    public Road(List<RoadSegment> segments, Color color) {
        this.segments = segments;
        this.color = color;
        connectSegments();
    }

    public Road(int startX, int startY, int length, Color color, boolean horizontal, boolean isRightLane) {
        this.segments = new ArrayList<>();
        segments.add(new RoadSegment(startX, startY, length, horizontal));
        this.color = color;
        this.isRightLane = isRightLane;
        connectSegments();
    }

    public Road getNextRoad() { return nextRoad; }
    public void setNextRoad(Road nextRoad) { this.nextRoad = nextRoad; }

    private void connectSegments() {
        for(int i = 1; i < segments.size(); i++) {
            segments.get(i-1).setNextSegment(segments.get(i));
        }
    }

    public int getLength() {
        return segments.stream().mapToInt(RoadSegment::getLength).sum();
    }


    public Point getPosition(int progression, int laneOffset) {
        int accumulated = 0;
        for(RoadSegment segment : segments) {
            if(progression <= accumulated + segment.getLength()) {
                return segment.getPosition(progression - accumulated, laneOffset);
            }
            accumulated += segment.getLength();
        }
        return segments.get(segments.size()-1).getPosition(segments.get(segments.size()-1).getLength(), laneOffset);
    }

    public void draw(Graphics g) {
        g.setColor(color);
        for(RoadSegment segment : segments) {
            segment.draw(g, color);
        }
    }

    public RoadSegment getCurrentSegment(Point position) {
        for(RoadSegment segment : segments) {
            if(segment.contains(position)) {
                return segment;
            }
        }
        return segments.get(0);
    }

    public boolean isHorizontal() { return !segments.isEmpty() && segments.get(0).isHorizontal(); }


}