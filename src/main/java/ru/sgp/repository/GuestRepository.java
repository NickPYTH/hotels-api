package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Bed;
import ru.sgp.model.Guest;
import ru.sgp.model.Organization;
import ru.sgp.model.Room;

import java.util.Date;
import java.util.List;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {
    List<Guest> findAllByRoomAndCheckoutedAndDateStartLessThanEqualAndDateFinishGreaterThan(Room room, boolean checkouted, Date date, Date date1);

    List<Guest> findAllByRoomAndCheckouted(Room room, boolean checkouted);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndCheckoutedAndRoom(Date maxDate, Date minDate, boolean checkouted, Room room);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndCheckouted(Date maxDate, Date minDate, boolean checkouted);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndOrganizationAndLastnameAndFirstnameAndSecondNameAndCheckouted(Date maxDate, Date minDate, Organization organization, String lastName, String firstName, String secondName, boolean checkouted);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndRoom(Date dateFinish, Date dateStart, Room room);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfter(Date tmp, Date tmp1);

    List<Guest> findAllByRoomAndDateStartLessThanEqualAndDateFinishGreaterThan(Room room, Date date, Date date1);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndBed(Date date, Date date1, Bed bed);

    List<Guest> findAllByRoomAndDateFinishLessThanEqualAndCheckouted(Room room, Date date, Boolean checkouted);
}
