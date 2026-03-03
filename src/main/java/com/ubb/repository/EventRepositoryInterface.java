// repository/EventRepositoryInterface.java

package com.ubb.repository;

import com.ubb.domain.Event;
import java.util.List;

public interface EventRepositoryInterface extends Repository<Long, Event> {

    // Metoda specifica necesara in EventService pentru reconstructia abonatilor
    List<Long> getSubscriberIdsForEvent(Long eventId);
    List<Event> findFinishedEventsByUserId(Long userId);
}