package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Log;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {

}
