package org.trafficSimulation.model.argumentation;

import net.sf.tweety.arg.dung.semantics.Extension;
import net.sf.tweety.arg.dung.syntax.*;
import org.trafficSimulation.model.environment.Weather;
import java.util.*;
import net.sf.tweety.arg.dung.reasoner.SimplePreferredReasoner;

public class TransportArgumentation {
    private DungTheory argumentationFramework;

    public TransportArgumentation() {
        this.argumentationFramework = new DungTheory();
        setupArguments();
    }

    private void setupArguments() {
        // Arguments pour les moyens de transport
        Argument voiture = new Argument("voiture");
        Argument moto = new Argument("moto");
        Argument velo = new Argument("velo");

        // Arguments pour la météo
        Argument beauTemps = new Argument("beau_temps");
        Argument pluie = new Argument("pluie");
        Argument orage = new Argument("orage");
        Argument ventFort = new Argument("vent_fort");
        Argument visibiliteReduite = new Argument("visibilite_reduite");

        // Arguments pour la distance
        Argument courteDistance = new Argument("courte_distance");
        Argument moyenneDistance = new Argument("moyenne_distance");
        Argument longueDistance = new Argument("longue_distance");

        // Arguments pour la santé
        Argument bonSante = new Argument("bonne_sante");
        Argument mauvaiseSante = new Argument("mauvaise_sante");

        // Ajouter tous les arguments
        argumentationFramework.add(voiture);
        argumentationFramework.add(moto);
        argumentationFramework.add(velo);
        argumentationFramework.add(beauTemps);
        argumentationFramework.add(pluie);
        argumentationFramework.add(orage);
        argumentationFramework.add(ventFort);
        argumentationFramework.add(visibiliteReduite);
        argumentationFramework.add(courteDistance);
        argumentationFramework.add(moyenneDistance);
        argumentationFramework.add(longueDistance);
        argumentationFramework.add(bonSante);
        argumentationFramework.add(mauvaiseSante);

        // Définir les attaques
        // Météo
        argumentationFramework.addAttack(pluie, moto);
        argumentationFramework.addAttack(pluie, velo);
        argumentationFramework.addAttack(orage, moto);
        argumentationFramework.addAttack(orage, velo);
        argumentationFramework.addAttack(ventFort, moto);
        argumentationFramework.addAttack(ventFort, velo);
        argumentationFramework.addAttack(visibiliteReduite, moto);
        argumentationFramework.addAttack(visibiliteReduite, velo);
        argumentationFramework.addAttack(beauTemps, voiture);

        // Distance
        argumentationFramework.addAttack(longueDistance, velo);
        argumentationFramework.addAttack(courteDistance, voiture);

        // Santé
        argumentationFramework.addAttack(mauvaiseSante, velo);
        argumentationFramework.addAttack(mauvaiseSante, moto);
        argumentationFramework.addAttack(bonSante, voiture);

        argumentationFramework.addAttack(beauTemps, pluie);      // Le beau temps défend contre la pluie
        argumentationFramework.addAttack(beauTemps, orage);      // Le beau temps défend contre l'orage
        argumentationFramework.addAttack(beauTemps, ventFort);   // Le beau temps défend contre le vent fort

        // Défenses liées à la santé
        argumentationFramework.addAttack(bonSante, mauvaiseSante);   // La bonne santé défend contre la mauvaise santé

        // Défenses liées à la distance
        argumentationFramework.addAttack(moyenneDistance, courteDistance);    // La moyenne distance défend contre la courte distance
        argumentationFramework.addAttack(longueDistance, moyenneDistance);    // La longue distance défend contre la moyenne distance

        // Défenses des moyens de transport
        argumentationFramework.addAttack(voiture, visibiliteReduite);    // La voiture défend contre la visibilité réduite
        argumentationFramework.addAttack(voiture, pluie);                // La voiture défend contre la pluie
        argumentationFramework.addAttack(voiture, orage);                // La voiture défend contre l'orage

        argumentationFramework.addAttack(moto, courteDistance);          // La moto défend contre la courte distance

        argumentationFramework.addAttack(velo, courteDistance);

    }

    public String chooseTransport(int distance, boolean bonneSante, Weather weather) {
        // Créer une nouvelle extension
        DungTheory theorieCourante = new DungTheory();

        // Ajouter les arguments de base
        Argument voiture = new Argument("voiture");
        Argument moto = new Argument("moto");
        Argument velo = new Argument("velo");

        theorieCourante.add(voiture);
        theorieCourante.add(moto);
        theorieCourante.add(velo);

        // Ajouter les arguments basés sur les conditions actuelles
        if (weather.getCurrentCondition() == Weather.Condition.SUNNY && weather.isGoodForCycling()) {
            Argument beauTemps = new Argument("beau_temps");
            theorieCourante.add(beauTemps);
            theorieCourante.addAttack(beauTemps, voiture);
        }
        if (weather.getCurrentCondition() == Weather.Condition.RAINY) {
            Argument pluie = new Argument("pluie");
            theorieCourante.add(pluie);
            theorieCourante.addAttack(pluie, moto);
            theorieCourante.addAttack(pluie, velo);
        }
        if (weather.getCurrentCondition() == Weather.Condition.STORMY) {
            Argument orage = new Argument("orage");
            theorieCourante.add(orage);
            theorieCourante.addAttack(orage, moto);
            theorieCourante.addAttack(orage, velo);
        }
        if (weather.getWindSpeed() > 50.0) {
            Argument ventFort = new Argument("vent_fort");
            theorieCourante.add(ventFort);
            theorieCourante.addAttack(ventFort, moto);
            theorieCourante.addAttack(ventFort, velo);
        }
        if (weather.getVisibility() < 500) {
            Argument visibiliteReduite = new Argument("visibilite_reduite");
            theorieCourante.add(visibiliteReduite);
            theorieCourante.addAttack(visibiliteReduite, moto);
            theorieCourante.addAttack(visibiliteReduite, velo);
        }

        // Arguments pour la distance
        if (distance < 5) {
            Argument courteDistance = new Argument("courte_distance");
            theorieCourante.add(courteDistance);
            theorieCourante.addAttack(courteDistance, voiture);
        } else if (distance > 15) {
            Argument longueDistance = new Argument("longue_distance");
            theorieCourante.add(longueDistance);
            theorieCourante.addAttack(longueDistance, velo);
        }

        // Arguments pour la santé
        if (bonneSante) {
            Argument bonSante = new Argument("bonne_sante");
            theorieCourante.add(bonSante);
            theorieCourante.addAttack(bonSante, voiture);
        } else {
            Argument mauvaiseSante = new Argument("mauvaise_sante");
            theorieCourante.add(mauvaiseSante);
            theorieCourante.addAttack(mauvaiseSante, velo);
            theorieCourante.addAttack(mauvaiseSante, moto);
        }

        SimplePreferredReasoner reasoner = new SimplePreferredReasoner();
        Collection<Extension> extensions = reasoner.getModels(theorieCourante);
        Extension extension = extensions.iterator().next();

        Argument veloArg = new Argument("velo");
        Argument motoArg = new Argument("moto");




        if (extension.contains(veloArg)) {
            return "velo";
        } else if (extension.contains(motoArg)) {
            return "moto";
        } else {
            return "voiture";
        }

    }






}