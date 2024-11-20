package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Reason;
import ru.sgp.model.Responsibilities;

@Repository
public interface ReasonRepository extends JpaRepository<Reason, Long> {
    Reason findByName(String reason);
}
