package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.BedDTO;
import ru.sgp.dto.ReservationDTO;
import ru.sgp.model.Employee;
import ru.sgp.model.Guest;
import ru.sgp.model.Reservation;
import ru.sgp.repository.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

    @Autowired
    ReservationRepository reservationRepository;
    @Autowired
    BedRepository bedRepository;
    @Autowired
    EventRepository eventRepository;
    @Autowired
    FilialRepository filialRepository;
    @Autowired
    GuestRepository guestRepository;
    @Autowired
    ReservationStatusRepository reservationStatusRepository;
    @Autowired
    EmployeeRepository employeeRepository;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    @Transactional
    public List<ReservationDTO> getAll() {
        ModelMapper modelMapper = new ModelMapper();
        List<ReservationDTO> response = new ArrayList<>();
        for (Reservation reservation: reservationRepository.findAll()) {
            ReservationDTO reservationDTO = modelMapper.map(reservation, ReservationDTO.class);
            if (reservation.getDateStart() != null) reservationDTO.setDateStart(dateTimeFormatter.format(reservation.getDateStart()));
            else reservationDTO.setDateStart(dateTimeFormatter.format(reservation.getDateStartConfirmed()));
            if (reservation.getDateFinish() != null) reservationDTO.setDateFinish(dateTimeFormatter.format(reservation.getDateFinish()));
            else reservationDTO.setDateFinish(dateTimeFormatter.format(reservation.getDateFinishConfirmed()));
            response.add(reservationDTO);
        }
        return response;
    }

    @Transactional
    public ReservationDTO get(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        Reservation reservation = reservationRepository.getById(id);
        ReservationDTO reservationDTO = modelMapper.map(reservation, ReservationDTO.class);
        reservationDTO.setDateStart(dateTimeFormatter.format(reservation.getDateStart()));
        reservationDTO.setDateFinish(dateTimeFormatter.format(reservation.getDateFinish()));
        return reservationDTO;
    }

    @Transactional
    public ReservationDTO delete(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        Reservation reservation = reservationRepository.getById(id);
        ReservationDTO reservationDTO = modelMapper.map(reservation, ReservationDTO.class);
        reservationRepository.delete(reservation);
        return reservationDTO;
    }

    @Transactional
    public ReservationDTO update(ReservationDTO reservationDTO) throws ParseException {
        ModelMapper modelMapper = new ModelMapper();
        Reservation reservation;
        if (reservationDTO.getId() != null) reservation = reservationRepository.getById(reservationDTO.getId());
        else reservation = new Reservation();
        Date dateStart = dateTimeFormatter.parse(reservationDTO.getDateStart());
        Date dateFinish = dateTimeFormatter.parse(reservationDTO.getDateFinish());
        reservation.setTabnum(reservationDTO.getTabnum());
        reservation.setFirstname(reservationDTO.getFirstname());
        reservation.setLastname(reservationDTO.getLastname());
        reservation.setSecondname(reservationDTO.getSecondname());
        reservation.setMale(reservationDTO.getMale());
        reservation.setDateStart(dateStart);
        reservation.setDateFinish(dateFinish);
        reservation.setBed(bedRepository.getById(reservationDTO.getBed().getId()));
        reservation.setEvent(eventRepository.getById(reservationDTO.getEvent().getId()));
        reservation.setFromFilial(filialRepository.getById(reservationDTO.getFromFilial().getId()));
        reservation.setNote(reservationDTO.getNote());
        if (reservationDTO.getGuest() != null) reservation.setGuest(guestRepository.getById(reservationDTO.getGuest().getId()));
        else reservation.setGuest(null);
        //reservation.setStatus(reservationStatusRepository.getById(reservationDTO.getStatus().getId()));

        // Проверяем не пересекается ли дата проживания с кем-то на выбранном месте БРОНИ
        List<Reservation> reservations = new ArrayList<>();

        if (reservationDTO.getId() == null)
            reservations = reservationRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, reservation.getBed());
        else
            reservations = reservationRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBedAndIdIsNot(dateFinish, dateStart, reservation.getBed(), reservation.getId());
        if (!reservations.isEmpty()) {
            Reservation reservationTmp = reservations.get(0);
            ReservationDTO tmp = new ReservationDTO();
            tmp.setError("Dates range error");
            tmp.setTabnum(reservationTmp.getTabnum());
            tmp.setBed(modelMapper.map(reservationTmp.getBed(), BedDTO.class));
            tmp.setDateStart(dateTimeFormatter.format(reservationTmp.getDateStart()));
            tmp.setDateFinish(dateTimeFormatter.format(reservationTmp.getDateFinish()));
            return tmp;
        }
        // -----

        // Проверяем не пересекается ли дата проживания с кем-то на выбранном месте ЗАПИСИ О ПРОЖИВАНИИ
        List<Guest> guests = new ArrayList<>();

        guests = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, reservation.getBed());
        if (!guests.isEmpty()) {
            Guest guestTmp = guests.get(0);
            ReservationDTO tmp = new ReservationDTO();
            tmp.setError("Dates range error");
            tmp.setFio(guestTmp.getLastname() + " " + guestTmp.getFirstname() + " " + guestTmp.getSecondName());
            tmp.setBed(modelMapper.map(guestTmp.getBed(), BedDTO.class));
            tmp.setDateStart(dateTimeFormatter.format(guestTmp.getDateStart()));
            tmp.setDateFinish(dateTimeFormatter.format(guestTmp.getDateFinish()));
            return tmp;
        }
        // -----
        reservationRepository.save(reservation);
        return modelMapper.map(reservation, ReservationDTO.class);
    }

    @Transactional
    public ReservationDTO confirm(Long id) {
        Reservation reservation = reservationRepository.getById(id);
        Guest guest = new Guest();
        guest.setFirstname(reservation.getFirstname() == null ? " ":reservation.getFirstname());
        guest.setLastname(reservation.getLastname() == null ? " ":reservation.getLastname());
        guest.setSecondName(reservation.getSecondname() == null ? " " :reservation.getSecondname());
        guest.setNote(reservation.getNote());
        guest.setDateStart(reservation.getDateStart());
        guest.setDateFinish(reservation.getDateFinish());
        guest.setBed(reservation.getBed());
        guest.setMemo("-");
        guest.setMale(true);
        if (reservation.getTabnum() != null){
            Employee employee = employeeRepository.findByTabnum(reservation.getTabnum());
            guest.setEmployee(employee);
        }
        guestRepository.save(guest);
        reservation.setDateStartConfirmed(reservation.getDateStart());
        reservation.setDateFinishConfirmed(reservation.getDateFinish());
        reservation.setDateStart(null);
        reservation.setDateFinish(null);
        reservation.setGuest(guest);
        reservationRepository.save(reservation);
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(reservation, ReservationDTO.class);
    }
}