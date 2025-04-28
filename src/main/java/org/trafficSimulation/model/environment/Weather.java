package org.trafficSimulation.model.environment;

public class Weather {
    public enum Condition {
        SUNNY,      // Ensoleillé
        CLOUDY,     // Nuageux
        RAINY,      // Pluvieux
        STORMY      // Orageux
    }

    private Condition currentCondition;
    private double temperature;     // en Celsius
    private double windSpeed;       // en km/h
    private double visibility;      // en mètres

    public Weather() {
        this.currentCondition = Condition.SUNNY;
        this.temperature = 20.0;
        this.windSpeed = 0.0;
        this.visibility = 1000.0;
    }

    public void updateWeather(Condition condition, double temperature, double windSpeed, double visibility) {
        this.currentCondition = condition;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.visibility = visibility;
    }

    public boolean isDangerousForTwoWheels() {
        return windSpeed > 50.0 || currentCondition == Condition.STORMY ||
                (currentCondition == Condition.RAINY && visibility < 500);
    }

    public boolean isGoodForCycling() {
        return temperature >= 10 && temperature <= 30 &&
                windSpeed < 30 &&
                (currentCondition == Condition.SUNNY || currentCondition == Condition.CLOUDY);
    }

    // Getters
    public Condition getCurrentCondition() { return currentCondition; }
    public double getTemperature() { return temperature; }
    public double getWindSpeed() { return windSpeed; }
    public double getVisibility() { return visibility; }
}