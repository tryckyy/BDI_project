package org.trafficSimulation;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Road {
    private List<RoadSegment> segments;
    private Color color;

    private boolean isRightLane;
    private Road pairedRoad;
    private boolean reverse;
    private List<Road> nextRoads = new ArrayList<>();

    public Road getPairedRoad() { return pairedRoad; }
    public void setPairedRoad(Road pairedRoad) { this.pairedRoad = pairedRoad; }

    public boolean isRightLane() {
        return isRightLane;
    }


    public void addNextRoad(Road nextRoad) {
        if (!nextRoads.contains(nextRoad)) {
            nextRoads.add(nextRoad);
        }
    }

    public List<Road> getNextRoads() {
        return Collections.unmodifiableList(nextRoads);
    }

    public Road(List<RoadSegment> segments, Color color, boolean reverse, boolean isRightLane) {
        this.segments = segments;
        this.color = color;
        this.reverse = reverse;
        this.isRightLane = isRightLane;
        connectSegments();
    }

    public boolean isReverse() {
        return reverse;
    }


    public Road(int startX, int startY, int length, Color color, boolean horizontal, boolean isRightLane) {
        this.segments = new ArrayList<>();
        segments.add(new RoadSegment(startX, startY, length, horizontal, false));
        this.color = color;
        this.isRightLane = isRightLane;
        connectSegments();
    }





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