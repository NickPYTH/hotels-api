package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.EventKind;

@Repository
public interface EventKindRepository extends JpaRepository<EventKind, Long> {

}

