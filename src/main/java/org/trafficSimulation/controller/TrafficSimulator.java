package org.trafficSimulation.controller;

import javax.swing.*;

import org.trafficSimulation.view.SimulationPanel;

import java.awt.*;

public class TrafficSimulator extends JFrame {
    private SimulationPanel simulationPanel;


    public TrafficSimulator() {
        setTitle("Traffic Simulator BDI");
        setSize(1920, 1080);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        simulationPanel = new SimulationPanel();
        add(simulationPanel);

        new Thread(() -> {
            while(true) {
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                simulationPanel.updateSimulation();
                simulationPanel.repaint();
            }
        }).start();



    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TrafficSimulator().setVisible(true));
    }
}