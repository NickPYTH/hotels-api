package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Event;
import ru.sgp.model.EventKind;
import ru.sgp.model.Hotel;

import java.util.Date;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByHotelAndDateStartLessThanEqualAndDateFinishGreaterThanEqual(Hotel hotel, Date dateFinish, Date dateStart);
}

