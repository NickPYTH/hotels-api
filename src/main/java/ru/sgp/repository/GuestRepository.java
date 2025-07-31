package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.sgp.model.*;

import java.util.Date;
import java.util.List;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {
    List<Guest> findAllByBedRoomAndCheckoutedAndDateStartLessThanEqualAndDateFinishGreaterThan(Room room, boolean checkouted, Date date, Date date1);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndCheckoutedAndBedRoom(Date maxDate, Date minDate, boolean checkouted, Room room);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndBedRoom(Date dateFinish, Date dateStart, Room room);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndEmployeeNotNull(Date tmp, Date tmp1);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfter(Date tmp, Date tmp1);

    List<Guest> findAllByBedRoomAndDateStartLessThanEqualAndDateFinishGreaterThan(Room room, Date date, Date date1);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndBed(Date date, Date date1, Bed bed);

    List<Guest> findAllByOrganization(Organization byId);

    @Query("SELECT DISTINCT guest.lastname FROM Guest guest")
    List<String> findDistinctLastname();

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndLastname(Date dateFinish, Date dateStart, String lastName);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlat(Date dateFinish, Date dateStart, Flat flat);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotelFilial(Date tmp, Date tmp1, Filial filial);


    List<Guest> findAllByBed(Bed bed);

    List<Guest> findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(Date date, Date date1, Bed bed); // Для поиска пересечения дат проживания по месту при создании


    List<Guest> findAllByBedRoomAndDateStartLessThanEqualAndDateFinishGreaterThanEqual(Room room, Date date, Date date1);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotel(Date date, Date date1, Hotel hotel);

    List<Guest> findAllByBedRoomFlatHotelFilial(Filial filial);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotelAndOrganization(Date maxDate, Date minDate, Hotel hotel, Organization organization);

    List<Guest> findAllByDateStartLessThanAndDateFinishGreaterThanAndCheckoutedAndBedRoom(Date dateFinish, Date dateStart, boolean checkouted, Room room);

    List<Guest> findAllByDateStartLessThanAndDateFinishGreaterThanAndBedRoom(Date dateFinish, Date dateStart, Room room);

    int countAllByDateStartLessThanEqualAndDateFinishGreaterThanEqualAndBedRoomFlatHotel(Date dateFinish, Date dateStart, Hotel hotel);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotelAndFamilyMemberOfEmployee(Date maxDate, Date minDate, Hotel hotel, Employee employee);

    List<Guest> findAllByBedRoomFlatHotelAndDateFinishLessThanEqual(Hotel hotel, Date date);

    List<Guest> findAllByDateStartGreaterThanEqualAndDateStartLessThanEqualAndBedRoomFlatHotel(Date dayStart, Date dayFinish, Hotel hotel);

    List<Guest> findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotelAndContractReasonAndEmployeeIsNotNull(Date dateFinish, Date dateStart, Hotel hotel, Reason reasonAVD);

    int countAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotelAndFamilyMemberOfEmployee(Date dateFinish, Date dateStart, Hotel hotel, Employee guest);

    Boolean existsByDateStartLessThanAndDateFinishGreaterThanAndBed(Date dateFinish, Date dateStart, Bed bed);

    List<Guest> findAllByBedRoomFlatHotelAndDateStartLessThanEqualAndDateFinishGreaterThanEqual(Hotel hotel, Date dateFinish, Date dateStart);

    List<Guest> findAllByDateFinishGreaterThanEqual(Date dateStart);

    List<Guest> findAllByDateStartLessThanAndDateFinishGreaterThanAndBedAndIdIsNot(Date dateFinish, Date dateStart, Bed bed, Long id);
}
