package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

}

