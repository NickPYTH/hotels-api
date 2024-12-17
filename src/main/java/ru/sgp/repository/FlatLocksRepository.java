package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Flat;
import ru.sgp.model.FlatLocks;

import java.util.Date;
import java.util.List;

@Repository
public interface FlatLocksRepository extends JpaRepository<FlatLocks, Long> {

    List<FlatLocks> findAllByDateStartBeforeAndDateFinishAfterAndFlat(Date dateStart, Date dateFinish, Flat flat);

    List<FlatLocks> findAllByFlat(Flat flat);
}
