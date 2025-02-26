package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Bed;
import ru.sgp.model.Filial;
import ru.sgp.model.Reservation;
import ru.sgp.model.Room;

import java.util.Date;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findAllByDateStartLessThanAndDateFinishGreaterThanAndTabnumAndBedIsNotAndBedRoomFlatHotelFilial(Date dateFinish, Date dateStart, Integer tabnum, Bed bed, Filial filial);

    List<Reservation> findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(Date dateFinish, Date dateStart, Bed bed);

    List<Reservation> findAllByDateStartLessThanAndDateFinishGreaterThanAndBedAndIdIsNot(Date dateFinish, Date dateStart, Bed bed, Long id);

    List<Reservation> findAllByBedRoomAndDateStartLessThanEqualAndDateFinishGreaterThan(Room room, Date date, Date date1);

    List<Reservation> findAllByDateStartBeforeAndDateFinishAfterAndBed(Date dateFinish, Date dateStart, Bed bed);
}
