package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.sgp.model.*;

import java.util.Date;
import java.util.List;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {
    List<Guest> findAllByRoomAndCheckoutedAndDateStartLessThanEqualAndDateFinishGreaterThan(Room room, boolean checkouted, Date date, Date date1);

    Integer countAllByRoomAndCheckoutedAndDateStartLessThanEqualAndDateFinishGreaterThan(Room room, boolean checkouted, Date date, Date date1);

    List<Guest> findAllByRoomAndCheckouted(Room room, boolean checkouted);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndCheckoutedAndRoom(Date maxDate, Date minDate, boolean checkouted, Room room);


    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndOrganizationAndLastnameAndFirstnameAndSecondNameAndCheckouted(Date maxDate, Date minDate, Organization organization, String lastName, String firstName, String secondName, boolean checkouted);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndRoom(Date dateFinish, Date dateStart, Room room);

    List<Guest>  findAllByDateStartBeforeAndDateFinishAfterAndEmployeeNotNull(Date tmp, Date tmp1);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfter(Date tmp, Date tmp1);

    List<Guest> findAllByRoomAndDateStartLessThanEqualAndDateFinishGreaterThan(Room room, Date date, Date date1);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndBed(Date date, Date date1, Bed bed);

    List<Guest> findAllByRoomAndDateFinishLessThanEqualAndCheckouted(Room room, Date date, Boolean checkouted);

    List<Guest> findAllByOrganization(Organization byId);

    @Query("SELECT DISTINCT guest.lastname FROM Guest guest")
    List<String> findDistinctLastname();

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndRoomFlatHotel(Date tmp, Date tmp1, Hotel hotel);


    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndLastname(Date dateFinish, Date dateStart, String lastName);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlat(Date dateFinish, Date dateStart, Flat flat);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndRoomFlatHotelFilial(Date tmp, Date tmp1, Filial filial);


    List<Guest> findAllByBed(Bed bed);

    List<Guest> findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(Date date, Date date1, Bed bed); // Для поиска пересечения дат проживания по месту при создании

    List<Guest> findAllByDateStartLessThanAndDateFinishGreaterThanAndBedAndIdIsNot(Date date, Date date1, Bed bed, Long id); // Для поиска пересечения дат проживания по месту при обновлении

    List<Guest> findAllByRoomAndDateStartLessThanEqualAndDateFinishGreaterThanEqual(Room room, Date date, Date date1);

    List<Guest> findAllByDateStartLessThanAndDateFinishGreaterThanAndFirstnameAndLastnameAndSecondNameAndBedIsNot(Date dateFinish, Date dateStart, String firstname, String lastname, String secondName, Bed bed);
}
