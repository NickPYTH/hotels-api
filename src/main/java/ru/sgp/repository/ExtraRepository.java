package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Extra;
import ru.sgp.model.Guest;

import java.util.Optional;

@Repository
public interface ExtraRepository extends JpaRepository<Extra, Long> {

}
