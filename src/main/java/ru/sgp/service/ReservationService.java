package ru.sgp.service;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.sgp.dto.BedDTO;
import ru.sgp.dto.ReservationDTO;
import ru.sgp.model.*;
import ru.sgp.repository.*;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service
public class ReservationService {
    @Autowired
    ReservationRepository reservationRepository;
    @Autowired
    BedRepository bedRepository;
    @Autowired
    EventKindRepository eventKindRepository;
    @Autowired
    FilialRepository filialRepository;
    @Autowired
    GuestRepository guestRepository;
    @Autowired
    ReservationStatusRepository reservationStatusRepository;
    @Autowired
    EmployeeRepository employeeRepository;
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private ContractRepository contractRepository;
    @Autowired
    private HotelRepository hotelRepository;

    @Transactional
    public List<ReservationDTO> update(ReservationDTO reservationDTO) throws ParseException {
        ModelMapper modelMapper = new ModelMapper();
        List<ReservationDTO> response = new ArrayList<>();
        ReservationDTO reservationBeforeState;
        Reservation reservation;
        if (reservationDTO.getId() != null) {
            reservation = reservationRepository.getById(reservationDTO.getId());
            reservationBeforeState = modelMapper.map(reservation, ReservationDTO.class);
        } else {
            reservation = new Reservation();
            reservationBeforeState = new ReservationDTO();
        }
        response.add(reservationBeforeState);

        Date dateStart = dateTimeFormatter.parse(reservationDTO.getDateStart());
        Date dateFinish = dateTimeFormatter.parse(reservationDTO.getDateFinish());
        reservation.setTabnum(reservationDTO.getTabnum());
        reservation.setFirstname(reservationDTO.getFirstname());
        reservation.setLastname(reservationDTO.getLastname());
        reservation.setSecondName(reservationDTO.getSecondName());
        reservation.setMale(reservationDTO.getMale());
        reservation.setDateStart(dateStart);
        reservation.setDateFinish(dateFinish);

        // Проверка на договор
        if (reservationDTO.getContract() != null) {
            Contract contract = contractRepository.getById(reservationDTO.getContract().getId());
            reservation.setContract(contract);
        } else {
            reservation.setContract(null);
        }
        // -----

        // Проверка на доп. место
        if (reservationDTO.getBed().getId() == null) {
            Room room = roomRepository.getById(reservationDTO.getRoomId());
            Bed extraBed = new Bed();
            extraBed.setIsExtra(true);
            extraBed.setRoom(room);
            extraBed.setName("Extra");
            bedRepository.save(extraBed);
            reservation.setBed(extraBed);
        } else {
            reservation.setBed(bedRepository.getById(reservationDTO.getBed().getId()));
        }
        // -----
        reservation.setEventKind(eventKindRepository.getById(reservationDTO.getEvent().getId()));
        reservation.setFromFilial(filialRepository.getById(reservationDTO.getFromFilial().getId()));
        reservation.setNote(reservationDTO.getNote());
        reservation.setFamilyMemberOfEmployee(reservationDTO.getFamilyMemberOfEmployee());
        if (reservationDTO.getGuest() != null)
            reservation.setGuest(guestRepository.getById(reservationDTO.getGuest().getId()));
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
            response.add(tmp);
            return response;
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
            response.add(tmp);
            return response;
        }
        // -----
        reservationRepository.save(reservation);
        response.add(modelMapper.map(reservation, ReservationDTO.class));
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
    public List<ReservationDTO> getAll() {
        ModelMapper modelMapper = new ModelMapper();
        List<ReservationDTO> response = new ArrayList<>();
        for (Reservation reservation : reservationRepository.findAll()) {
            ReservationDTO reservationDTO;
            try {
                reservationDTO = modelMapper.map(reservation, ReservationDTO.class);
                if (reservation.getDateStart() != null)
                    reservationDTO.setDateStart(dateTimeFormatter.format(reservation.getDateStart()));
                else {
                    if (reservation.getDateStartConfirmed() != null)
                        reservationDTO.setDateStart(dateTimeFormatter.format(reservation.getDateStartConfirmed()));
                }
                if (reservation.getDateFinish() != null)
                    reservationDTO.setDateFinish(dateTimeFormatter.format(reservation.getDateFinish()));
                else {
                    if (reservation.getDateFinishConfirmed() != null)
                        reservationDTO.setDateFinish(dateTimeFormatter.format(reservation.getDateFinishConfirmed()));
                }
                response.add(reservationDTO);
            } catch (Exception e) {
                reservationDTO = new ReservationDTO();
            }
        }
        return response;
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
    public ReservationDTO confirm(Long id) {
        Reservation reservation = reservationRepository.getById(id);
        Guest guest = new Guest();
        guest.setFirstname(reservation.getFirstname() == null ? " " : reservation.getFirstname());
        guest.setLastname(reservation.getLastname() == null ? " " : reservation.getLastname());
        guest.setSecondName(reservation.getSecondName() == null ? " " : reservation.getSecondName());
        guest.setNote(reservation.getNote());
        guest.setDateStart(reservation.getDateStart());
        guest.setDateFinish(reservation.getDateFinish());
        guest.setBed(reservation.getBed());
        guest.setContract(reservation.getContract());
        guest.setMemo("-");
        guest.setMale(true);
        if (reservation.getTabnum() != null) {
            Organization gtsOrganization = organizationRepository.getById(11L);
            guest.setOrganization(gtsOrganization);
            Employee employee = employeeRepository.findByTabnumAndEndDate(reservation.getTabnum(), null);
            guest.setEmployee(employee);
        } else {
            if (reservation.getFamilyMemberOfEmployee() != null) {
                Organization familyOrganization = organizationRepository.getById(72L);
                guest.setOrganization(familyOrganization);
                Employee fEmployee = employeeRepository.findByTabnumAndEndDate(reservation.getFamilyMemberOfEmployee(), null);
                if (fEmployee != null) guest.setFamilyMemberOfEmployee(fEmployee);
            }
        }
        guestRepository.save(guest);
        reservation.setDateStartConfirmed(reservation.getDateStart());
        reservation.setDateFinishConfirmed(reservation.getDateFinish());
        reservation.setDateStart(null);
        reservation.setDateFinish(null);
        reservation.setGuest(guest);
        reservationRepository.save(reservation);
        ModelMapper modelMapper = new ModelMapper();
        return new ReservationDTO();
    }

    public List<ReservationDTO> manyReservationUpload(MultipartFile file, Boolean mode) throws IOException {
        List<ReservationDTO> reservations = new ArrayList<>();
        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        XSSFSheet worksheet = workbook.getSheetAt(0);
        for (int i = 1; i <= 1000; i++) {
            XSSFRow row = worksheet.getRow(i);
            if (row == null)
                break;
            if (mode) {  // Поиск по таб. номеру
                Employee employee = null;
                if (row.getCell(0).getCellType() == CellType.STRING) {
                    Integer tabnum = Integer.valueOf(row.getCell(0).getStringCellValue());
                    employee = employeeRepository.findByTabnumAndEndDate(tabnum, null);
                } else {
                    Integer tabnum = (int) row.getCell(0).getNumericCellValue();
                    employee = employeeRepository.findByTabnumAndEndDate(tabnum, null);
                }
                if (employee != null) {
                    ReservationDTO reservationDTO = new ReservationDTO();
                    reservationDTO.setLastname(employee.getLastname());
                    reservationDTO.setFirstname(employee.getFirstname());
                    reservationDTO.setSecondName(employee.getSecondName());
                    reservationDTO.setTabnum(employee.getTabnum());
                    reservations.add(reservationDTO);
                }
            } else { // Поиск по ФИО
                String lastname = row.getCell(0).getStringCellValue().trim();
                String firstname = row.getCell(1).getStringCellValue().trim();
                String secondName = row.getCell(2).getStringCellValue().trim();
                List<Employee> employees = employeeRepository.findAllByLastnameAndFirstnameAndSecondNameAndEndDate(lastname, firstname, secondName, null);
                if (!employees.isEmpty()) {
                    ReservationDTO reservationDTO = new ReservationDTO();
                    reservationDTO.setLastname(lastname);
                    reservationDTO.setFirstname(firstname);
                    reservationDTO.setSecondName(secondName);
                    reservationDTO.setTabnum(employees.get(0).getTabnum());
                    reservations.add(reservationDTO);
                }
            }
        }
        return reservations;
    }

    @Transactional
    public HashMap<String, String> checkSpaces(Integer peopleCount, Long dateStartTS, Long dateFinishTS, Long eventId, Long hotelId, Boolean needReserve, Boolean soloMode) {
        HashMap<String, String> response = new HashMap<>();
        Hotel hotel = hotelRepository.getById(hotelId);
        EventKind eventKind = eventKindRepository.getById(eventId);
        Date dateStart = new Date(dateStartTS * 1000);
        Date dateFinish = new Date(dateFinishTS * 1000);
        List<Reservation> tmpReservations = new ArrayList<>();
        List<Bed> beds = bedRepository.findAllByRoomFlatHotelAndIsExtra(hotel, false);
        for (int i = 0; i < peopleCount; i++) {
            if (!tryCheckIn(dateStart, dateFinish, beds, tmpReservations, eventKind, needReserve, soloMode)) {
                // Очистка временных записей
                for (Reservation r : tmpReservations) reservationRepository.delete(r);
                // -----
                response.put("Status", "Error");
                return response;
            }
        }
        // Очистка временных записей, если мы просто проверяем
        if (!needReserve)
            for (Reservation r : tmpReservations) reservationRepository.delete(r);
        // -----
        if (needReserve) response.put("Status", "RESERVED");
        else response.put("Status", "OK");
        return response;
    }

    private boolean tryCheckIn(Date dateStart, Date dateFinish, List<Bed> beds, List<Reservation> tmpReservations, EventKind eventKind, Boolean needReserve, Boolean soloMode) {
        for (Bed bed : beds) {
            // Проверяем свободно ли место от броней или гостей на эти даты
            boolean bedBusyReservation = reservationRepository.existsByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bed);
            boolean bedBusyGuest = guestRepository.existsByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bed);
            if (bedBusyReservation || bedBusyGuest) continue;
            // -----

            // Если включен режим одиночки то не селим в комнаты с жильцами
            if (soloMode) {
                boolean neighborhoodExist = false;
                Flat flat = bed.getRoom().getFlat();
                for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                    for (Bed bedRoom : bedRepository.findAllByRoom(room)) {
                        List<Guest> neighborhoodsGuest = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bedRoom);
                        if (!neighborhoodsGuest.isEmpty())
                            neighborhoodExist = true;
                        List<Reservation> neighborhoodsReservation = reservationRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bedRoom);
                        if (!neighborhoodsReservation.isEmpty())
                            neighborhoodExist = true;
                        if (neighborhoodExist) break;
                    }
                }
                if (neighborhoodExist) continue;
            }
            // -----

            // Создаем временную бронь и добавляем в список веремнных броней, после они будут удалены
            Reservation reservation = new Reservation();
            reservation.setDateStart(dateStart);
            reservation.setDateFinish(dateFinish);
            reservation.setBed(bed);
            reservation.setMale(true);
            reservation.setFromFilial(filialRepository.getById(2L));
            if (needReserve)
                reservation.setNote("RESERVE");
            else
                reservation.setNote("RESERVATION CHECK! NEED TO DELETE if U SEE THIS RECORD!");
            reservation.setEventKind(eventKind);
            reservationRepository.save(reservation);
            reservation.setFirstname(reservation.getId().toString());
            reservation.setLastname(eventKind.getName());
            reservation.setSecondName(reservation.getId().toString());
            reservationRepository.save(reservation);
            tmpReservations.add(reservation);
            return true;
            // -----
        }
        return false;
    }
}