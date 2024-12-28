package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.MVZ;

import java.util.Optional;

@Repository
public interface MVZRepository extends JpaRepository<MVZ, Long> {

    MVZ findByEmployeeTab(String string);

    Optional<MVZ> findByEmployeeTabAndMvzNameIsContainingIgnoreCase(String string, String mvzName);
}
