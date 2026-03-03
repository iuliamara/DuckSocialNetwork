package com.ubb.service;

import com.ubb.domain.User;
import java.util.List;

/**
 * Interfata care defineste contractul pentru serviciile legate de analiza structurii retelei sociale.
 * Analiza se bazeaza pe conceptul de graf (utilizatori = noduri, prietenii = muchii).
 */
public interface NetworkServiceInterface {

    /**
     * Calculeaza numarul de comunitati din reteaua sociala.
     * O comunitate este o componenta conexa a grafului de utilizatori.
     * @return Numarul total de componente conexe (comunitati).
     */
    int getNumberOfCommunities();

    /**
     * Identifica si returneaza membrii celei mai sociabile comunitati.
     * Cea mai sociabila comunitate este cea care are cel mai lung lant (drum)
     * intre oricare doi utilizatori din cadrul acelei comunitati (diametrul maxim).
     *
     * @return O lista de utilizatori care fac parte din cea mai sociabila comunitate.
     */
    List<User> getMostSociableCommunity();
}