package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.EventType;

@Repository
public interface EventTypeRepository extends JpaRepository<EventType, Long> {

}

