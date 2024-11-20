package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Status;

@Repository
public interface StatusRepository extends JpaRepository<Status, Long> {

}
