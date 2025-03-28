package org.example;

import javax.swing.*;

public class TrafficSimulator extends JFrame {
    private SimulationPanel simulationPanel;

    public TrafficSimulator() {
        setTitle("Traffic Simulator BDI");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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