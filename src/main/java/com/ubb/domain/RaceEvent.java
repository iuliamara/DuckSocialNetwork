package com.ubb.domain;

import com.ubb.domain.capabilities.Inotator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Eveniment special pentru rate inotatoare (Cursa). Extinde Event.
 * Implementeaza logica de finalizare a cursei (Problema Natatie).
 */
public class RaceEvent extends Event {

    private final int numParticipants;
    protected List<Duck> eligibleParticipants;
    private final List<Double> beaconDistances;

    /**
     * Constructor pentru initializarea RaceEvent.
     */
    public RaceEvent(String name, int numParticipants, List<Duck> eligibleParticipants, List<Double> beaconDistances) {
        super(name);
        this.numParticipants = numParticipants;
        // Folosim o copie defensiva
        this.eligibleParticipants = new ArrayList<>(eligibleParticipants);
        this.beaconDistances = beaconDistances;
    }

    // --- Specificatii: Getters & Setters ---

    /**
     * Seteaza lista participantilor eligibili.
     * Acesta este un setter necesar pentru reconstructia din EventService.
     * @param participants Lista de rate.
     */
    public void setEligibleParticipants(List<Duck> participants) {
        this.eligibleParticipants.clear();
        this.eligibleParticipants.addAll(participants);
    }

    /**
     * Returneaza distanta pana la balize.
     * @return Lista de distante.
     */
    public List<Double> getBeaconDistances() {
        return beaconDistances;
    }

    // --- Specificatii: Logica de business (cu Stream-uri) ---

    /**
     * Implementeaza logica de finalizare a cursei, selecteaza ratele si calculeaza rezultatele.
     * Utilizeaza operatii pe stream-uri pentru filtrarea si sortarea initiala.
     *
     * @return Mesajul final cu rezultatele.
     */
    @Override
    public String finishEvent() {
        // 1. Filtrare (doar ratele care implementeaza Inotator)
        // OPERATIE PE STREAM-URI: Filtrare dupa capabilitate
        List<Duck> swimmingDucks = eligibleParticipants.stream()
                .filter(d -> d instanceof Inotator)
                .collect(Collectors.toList());

        // Verificare minima de participare
        if (swimmingDucks.size() < numParticipants || beaconDistances.size() < numParticipants) {
            String cancelMessage = String.format("Cursa anulatata: Necesita %d participanti si balize, dar au fost gasite doar %d rate inotatoare.",
                    numParticipants, swimmingDucks.size());
            notifySubscribers(cancelMessage);
            return cancelMessage;
        }

        // 2. Aplicarea regulilor Natatie: r1 >= r2 >= ... rm
        // Sortam ratele eligibile dupa rezistenta (descrescator)
        // OPERATIE PE STREAM-URI: Sortare
        swimmingDucks.sort(Comparator.comparingDouble(Duck::getRezistenta).reversed());

        // Selectia M rate (cele mai rezistente)
        List<Duck> finalParticipants = swimmingDucks.subList(0, numParticipants);

        // 3. Calculul Timpului Total (max(2 * d_j / v_i))
        double maxRaceTime = 0.0;
        StringBuilder raceDetails = new StringBuilder();
        raceDetails.append("\n--- CLASAMENT CURSA (detalii) ---\n");

        // Aici nu folosim stream-uri pentru a mentine logica indexata
        // si generarea secventiala a detaliilor (StringBuilder)
        for (int j = 0; j < numParticipants; j++) {
            // Balizele (d_j) sunt aliniate cu ratele sortate (r_i)
            double distance = beaconDistances.get(j);
            Duck participant = finalParticipants.get(j);
            double speed = participant.getViteza();

            // Timpul pentru rata i pe culoarul j (dus-intors)
            // Prevenim impartirea la zero
            double time = (speed > 0) ? (2.0 * distance / speed) : Double.MAX_VALUE;

            if (time > maxRaceTime) {
                maxRaceTime = time;
            }
            // Adauga detalii despre pozitie/culoar
            raceDetails.append(String.format("Pozitia %d: Duck %s pe Culoar %d (d=%.1f m). Timp: %.3f s\n",
                    j + 1, participant.getUsername(), j + 1, distance, time));
        }

        // 4. Setarea rezultatului si Notificarea
        String finalReport = String.format("CURSA S-A TERMINAT. DURATA MINIMA TOTALA: %.3f s.\n", maxRaceTime) +
                "Participanti: " + finalParticipants.size() + "\n" +
                raceDetails;

        setFinished(true); // Marcheaza evenimentul ca fiind terminat
        notifySubscribers(finalReport);
        return finalReport;
    }

    @Override
    public String toString(){
        return getName();
    }
}