package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.*;
import ru.sgp.model.*;
import ru.sgp.repository.*;
import ru.sgp.utils.MyMapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class FlatService {
    @Autowired
    FilialRepository filialRepository;
    @Autowired
    HotelRepository hotelRepository;
    @Autowired
    FlatRepository flatRepository;
    @Autowired
    GuestRepository guestRepository;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    PostRepository postRepository;
    @Autowired
    StatusRepository statusRepository;
    @Autowired
    BedRepository bedRepository;
    @Autowired
    RoomLocksRepository roomLocksRepository;
    @Autowired
    FlatLocksRepository flatLocksRepository;
    @Autowired
    ReservationRepository reservationRepository;
    @Autowired
    EmployeeRepository employeeRepository;
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH");
    @Transactional
    public List<FlatDTO> getAllByHotelId(Long hotelId, String dateStr) throws ParseException {
        Hotel hotel = hotelRepository.getById(hotelId);
        Date date = dateTimeFormatter.parse(dateStr);
        List<FlatDTO> response = new ArrayList<>();
        for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
            FlatDTO flatDTO = MyMapper.FlatToFlatDTO(flat);
            flatDTO.setRoomsCount(roomRepository.findAllByFlatOrderById(flat).size());
            int bedsCount = 0;
            int emptyBedsCount = 0;
            List<RoomDTO> roomDTOList = new ArrayList<>();
            for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(date, date, room);
                bedsCount += room.getBedsCount();
                emptyBedsCount += room.getBedsCount() - guestRepository.findAllByBedRoomAndDateStartLessThanEqualAndDateFinishGreaterThan(room, date, date).size();
                RoomDTO roomDTO = MyMapper.RoomToRoomDTO(room);
                if (!roomLocksList.isEmpty()) {
                    roomDTO.setStatus(MyMapper.StatusToStatusDTO(roomLocksList.get(0).getStatus()));
                    roomDTO.setRoomLockId(roomLocksList.get(0).getId());
                } else {
                    roomDTO.setStatus(MyMapper.StatusToStatusDTO(statusRepository.getById(1L)));
                }
                roomDTO.setBedsCount(room.getBedsCount());
                List<GuestDTO> guestDTOList = new ArrayList<>();

                // Получаем записи о проживании
                for (Guest guest : guestRepository.findAllByBedRoomAndDateStartLessThanEqualAndDateFinishGreaterThan(room, date, date)) {
                    GuestDTO guestDTO = MyMapper.GuestToGuestDTO(guest);
                    guestDTO.setIsReservation(false);
                    guestDTOList.add(guestDTO);
                }
                // -----

                // Получаем брони
                for (Reservation reservation : reservationRepository.findAllByBedRoomAndDateStartLessThanEqualAndDateFinishGreaterThanEqual(room, date, date)) {
                    GuestDTO guestDTO = new GuestDTO();
                    guestDTO.setId(reservation.getId());
                    Employee employee = employeeRepository.findByTabnum(reservation.getTabnum());
                    guestDTO.setFirstname(employee!=null?employee.getFirstname():reservation.getFirstname());
                    guestDTO.setLastname(employee!=null?employee.getLastname():reservation.getLastname());
                    guestDTO.setSecondName(employee!=null?employee.getSecondName():reservation.getSecondname());
                    guestDTO.setBed(MyMapper.BedToBedDTO(reservation.getBed()));
                    guestDTO.setDateStart(dateTimeFormatter.format(reservation.getDateStart()));
                    guestDTO.setDateFinish(dateTimeFormatter.format(reservation.getDateFinish()));
                    guestDTO.setNote(reservation.getNote());
                    guestDTO.setIsReservation(true);
                    guestDTOList.add(guestDTO);
                }
                // -----

                roomDTO.setGuests(guestDTOList);
                roomDTOList.add(roomDTO);
            }
            flatDTO.setRooms(roomDTOList);
            flatDTO.setBedsCount(bedsCount);
            flatDTO.setEmptyBedsCount(emptyBedsCount);
            List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(date, date, flat);
            if (!flatLocksList.isEmpty()) {
                flatDTO.setStatus(MyMapper.StatusToStatusDTO(flatLocksList.get(0).getStatus()));
                flatDTO.setFlatLockId(flatLocksList.get(0).getId());
            } else {
                flatDTO.setStatus(MyMapper.StatusToStatusDTO(statusRepository.getById(1L)));
            }
            response.add(flatDTO);
        }
        return response;
    }
    @Transactional
    public FlatDTO get(Long flatId, String dateStr) throws ParseException {
        Flat flat = flatRepository.getById(flatId);
        FlatDTO flatDTO = MyMapper.FlatToFlatDTO(flat);
        Date date = dateTimeFormatter.parse(dateStr);
        List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(date, date, flat);
        if (!flatLocksList.isEmpty()) {
            flatDTO.setStatus(MyMapper.StatusToStatusDTO(flatLocksList.get(0).getStatus()));
            flatDTO.setFlatLockId(flatLocksList.get(0).getId());
        } else {
            flatDTO.setStatus(MyMapper.StatusToStatusDTO(statusRepository.getById(1L)));
        }
        List<RoomDTO> roomDTOList = new ArrayList<>();
        for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
            List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(date, date, room);
            RoomDTO roomDTO = MyMapper.RoomToRoomDTO(room);
            if (!roomLocksList.isEmpty()) {
                roomDTO.setStatus(MyMapper.StatusToStatusDTO(roomLocksList.get(0).getStatus()));
                roomDTO.setRoomLockId(roomLocksList.get(0).getId());
            } else {
                roomDTO.setStatus(MyMapper.StatusToStatusDTO(statusRepository.getById(1L)));
            }
            roomDTO.setBedsCount(room.getBedsCount());
            List<GuestDTO> guestDTOList = new ArrayList<>();

            // Получаем записи о проживании
            for (Guest guest : guestRepository.findAllByBedRoomAndDateStartLessThanEqualAndDateFinishGreaterThanEqual(room, date, date)) {
                GuestDTO guestDTO = MyMapper.GuestToGuestDTO(guest);
                // Остальные поля вычисляются и не заполняются MyMapper
                Date cuttedStartDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateStart()));
                Date cuttedFinishDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateFinish()));
                String daysCount = String.valueOf(TimeUnit.DAYS.convert(cuttedFinishDate.getTime() - cuttedStartDate.getTime(), TimeUnit.MILLISECONDS));
                guestDTO.setDaysCount(daysCount);
                if (guest.getContract() != null) {
                    guestDTO.setCostByNight(guest.getContract().getCost());
                    if (Integer.parseInt(guestDTO.getDaysCount()) > 0)
                        guestDTO.setCost(guestDTO.getCostByNight() * Integer.parseInt(guestDTO.getDaysCount()));
                    else
                        guestDTO.setCost(guestDTO.getCostByNight());
                }
                if (guest.getEmployee() != null) {
                    Filial filial = filialRepository.findByCode(guest.getEmployee().getIdFilial());
                    String guestPost = "";
                    if (guest.getEmployee().getIdPoststaff() != null) {
                        Optional<Post> post = postRepository.findById(guest.getEmployee().getIdPoststaff().longValue());
                        if (post.isPresent())
                            guestPost = post.get().getName();
                    }
                    guestDTO.setFilialEmployee(filial.getName());
                    guestDTO.setPost(guestPost);
                }
                guestDTO.setIsReservation(false);
                guestDTOList.add(guestDTO);
            }
            // -----

            // Получаем брони
            for (Reservation reservation : reservationRepository.findAllByBedRoomAndDateStartLessThanEqualAndDateFinishGreaterThanEqual(room, date, date)) {
                GuestDTO guestDTO = new GuestDTO();
                Employee employee = employeeRepository.findByTabnum(reservation.getTabnum());
                guestDTO.setId(reservation.getId());
                guestDTO.setFirstname(employee!=null?employee.getFirstname():reservation.getFirstname());
                guestDTO.setLastname(employee!=null?employee.getLastname():reservation.getLastname());
                guestDTO.setSecondName(employee!=null?employee.getSecondName():reservation.getSecondname());
                ModelMapper modelMapper = new ModelMapper();
                guestDTO.setBed(modelMapper.map(reservation.getBed(), BedDTO.class));
                guestDTO.setEvent(modelMapper.map(reservation.getEvent(), EventDTO.class));
                guestDTO.setFromFilial(modelMapper.map(reservation.getFromFilial(), FilialDTO.class));
                guestDTO.setBed(MyMapper.BedToBedDTO(reservation.getBed()));
                guestDTO.setDateStart(dateTimeFormatter.format(reservation.getDateStart()));
                guestDTO.setDateFinish(dateTimeFormatter.format(reservation.getDateFinish()));
                guestDTO.setNote(reservation.getNote());
                guestDTO.setIsReservation(true);
                guestDTO.setMale(reservation.getMale());
                guestDTOList.add(guestDTO);
            }
            // -----

            roomDTO.setGuests(guestDTOList);
            List<BedDTO> bedDTOList = new ArrayList<>();
            for (Bed bed : bedRepository.findAllByRoomAndIsExtra(room, false)) {
                BedDTO bedDTO = new BedDTO();
                bedDTO.setId(bed.getId());
                bedDTO.setName(bed.getName());
                bedDTOList.add(bedDTO);
            }
            roomDTO.setBeds(bedDTOList);
            roomDTOList.add(roomDTO);
        }
        flatDTO.setRooms(roomDTOList);
        flatDTO.setRoomsCount(roomDTOList.size());

        return flatDTO;
    }
    @Transactional
    public FlatDTO updateTech(Long flatId) {
        Flat flat = flatRepository.getById(flatId);
        flat.setTech(!flat.getTech());
        flatRepository.save(flat);
        return new FlatDTO();
    }
    @Transactional
    public FlatDTO updateNote(FlatDTO flatDTO) {
        Flat flat = flatRepository.getById(flatDTO.getId());
        flat.setNote(flatDTO.getNote());
        flatRepository.save(flat);
        return flatDTO;
    }
    @Transactional
    public List<HashMap<String, String>> getAllByHotelIdChess(Long hotelId, String dateStartStr, String
            dateFinishStr) throws ParseException {
        List<HashMap<String, String>> result = new ArrayList<>();
        Hotel hotel = hotelRepository.getById(hotelId);
        Date dateStart = dateFormatter.parse(dateStartStr);
        Date dateFinish = dateFormatter.parse(dateFinishStr);
        for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
            for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                for (Bed bed : bedRepository.findAllByRoomAndIsExtra(room, false)) {
                    HashMap<String, String> record = new HashMap<>();
                    record.put("hotelId", flat.getHotel().getId().toString());
                    record.put("filialId", flat.getHotel().getFilial().getId().toString());
                    record.put("section", flat.getName());
                    record.put("sectionId", flat.getId().toString());
                    record.put("room", room.getId().toString());
                    record.put("roomName", room.getName());
                    record.put("bed", bed.getName());
                    record.put("bedId", bed.getId().toString());

                    // Заполнение из таблицы ЗАПИСИ О ПРОЖИВАНИИ
                    for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBed(dateFinish, dateStart, bed)) {
                        LocalDateTime start = dateStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                        int daysCount = Integer.parseInt(String.valueOf(TimeUnit.DAYS.convert(dateFinish.getTime() - dateStart.getTime(), TimeUnit.MILLISECONDS)));
                        int count = 0;
                        while (count <= daysCount) {
                            LocalDateTime guestStart = dateFormatter.parse(dateTimeFormatter.format(guest.getDateStart())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                            LocalDateTime guestFinish = dateFormatter.parse(dateTimeFormatter.format(guest.getDateFinish())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                            if ((start.isAfter(guestStart) || start.isEqual(guestStart)) && (start.isBefore(guestFinish) || start.isEqual(guestFinish))) {
                                int busyPercentStart = 100;
                                int busyPercentFinish = 100;
                                String guestDatesRange =  dateTimeFormatter.format(guest.getDateStart()) + " :: " + dateTimeFormatter.format(guest.getDateFinish());
                                String startStr = start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                                String fio = guest.getLastname() + " ";
                                if (guest.getFirstname().length() > 0) fio += guest.getFirstname().charAt(0) + ". ";
                                if (guest.getSecondName().length() > 0) fio += guest.getSecondName().charAt(0) + ".";
                                if (fio.length() > 16) fio = fio.substring(0, 15); // Обрезаем иначе не поместится в ячейку минимальную
                                String post = "";
                                String filial = "emptyF";
                                if (guest.getEmployee() != null) {
                                    filial = filialRepository.findByCode(guest.getEmployee().getIdFilial()).getName();
                                    if (guest.getEmployee().getIdPoststaff() != null) {
                                        Optional<Post> optionalPost = postRepository.findById(guest.getEmployee().getIdPoststaff().longValue());
                                        if (optionalPost.isPresent())
                                            post = optionalPost.get().getName();
                                    }
                                }
                                String guestInfo = fio + "&" + guestDatesRange + "&" + guest.getMale() + "&"+ guest.getNote() + "&" + post + "&" + filial + "&" + false;  // Последний параметр это бронь
                                if (start.isEqual(guestStart)) { // Начало совпало вычисляем процент занятого дня
                                    busyPercentStart = (int) ((24 - Double.parseDouble(timeFormatter.format(guest.getDateStart()))) / 24 * 100);
                                    if (record.get(startStr) != null) // Если есть запись, но новый жилец будет СЛЕВА в ячейке
                                        record.put(startStr, record.get(startStr) + "||" + guestInfo + "#" + busyPercentStart);
                                    else
                                        record.put(startStr, guestInfo + "#" + busyPercentStart); // Частично занимает ЛЕВУЮ часть
                                } else if (start.isEqual(guestFinish)) { // Начало совпало вычисляем процент занятого дня
                                    busyPercentFinish = (int) ((Double.parseDouble(timeFormatter.format(guest.getDateFinish()))) / 24 * 100);
                                    if (record.get(startStr) != null) // Если есть запись, но новый жилец будет СПРАВА в ячейке
                                        record.put(startStr, record.get(startStr) + "||" + guestInfo + "#-" + busyPercentFinish);
                                    else
                                        record.put(startStr, guestInfo + "#-" + busyPercentFinish); // Частично занимает ПРАВУЮ часть
                                } else // Если жилец полностью занимает весь день, всю ячейку
                                    record.put(startStr, guestInfo + "#100");
                                if (record.get("dates") == null)
                                    record.put("dates", dateTimeFormatter.format(guest.getDateStart()) + " - " + dateTimeFormatter.format(guest.getDateFinish()));
                            }
                            start = start.plusDays(1);
                            count++;
                        }
                    }
                    // -----

                    // Заполнение из таблицы БРОНИРОВАНИЕ
                    for (Reservation reservation : reservationRepository.findAllByDateStartBeforeAndDateFinishAfterAndBed(dateFinish, dateStart, bed)) {
                        LocalDateTime start = dateStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                        int daysCount = Integer.parseInt(String.valueOf(TimeUnit.DAYS.convert(dateFinish.getTime() - dateStart.getTime(), TimeUnit.MILLISECONDS)));
                        int count = 0;
                        while (count <= daysCount) {
                            LocalDateTime guestStart = dateFormatter.parse(dateTimeFormatter.format(reservation.getDateStart())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                            LocalDateTime guestFinish = dateFormatter.parse(dateTimeFormatter.format(reservation.getDateFinish())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                            if ((start.isAfter(guestStart) || start.isEqual(guestStart)) && (start.isBefore(guestFinish) || start.isEqual(guestFinish))) {
                                int busyPercentStart = 100;
                                int busyPercentFinish = 100;
                                String guestDatesRange =  dateTimeFormatter.format(reservation.getDateStart()) + " :: " + dateTimeFormatter.format(reservation.getDateFinish());
                                String startStr = start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                                Employee employee = employeeRepository.findByTabnum(reservation.getTabnum());
                                String fio = "";
                                if (employee != null) {
                                    fio = employee.getLastname() + " " + employee.getFirstname().charAt(0) + ". " + employee.getSecondName().charAt(0) + ".";
                                } else {
                                    if (reservation.getLastname() != null) fio += reservation.getLastname();
                                    if (reservation.getFirstname() != null && !reservation.getFirstname().isEmpty()) fio += reservation.getFirstname().charAt(0) + ". ";
                                    if (reservation.getSecondname() != null && !reservation.getSecondname().isEmpty()) fio += reservation.getSecondname().charAt(0) + ". ";
                                }
                                if (fio.length() > 16) fio = fio.substring(0, 15); // Обрезаем иначе не поместится в ячейку минимальную
                                String post = "";
                                String filial = "";
                                if (employee != null) {
                                    filial = filialRepository.findByCode(employee.getIdFilial()).getName();
                                    if (employee.getIdPoststaff() != null)
                                        post = postRepository.getById(employee.getIdPoststaff().longValue()).getName();
                                }
                                String guestInfo = fio + "&" + guestDatesRange + "&" + true + "&"+ reservation.getNote() + "&" + post + "&" + filial + "&" + true; // Последний параметр это бронь
                                if (start.isEqual(guestStart)) { // Начало совпало вычисляем процент занятого дня
                                    busyPercentStart = (int) ((24 - Double.parseDouble(timeFormatter.format(reservation.getDateStart()))) / 24 * 100);
                                    if (record.get(startStr) != null) // Если есть запись, но новый жилец будет СЛЕВА в ячейке
                                        record.put(startStr, record.get(startStr) + "||" + guestInfo + "#" + busyPercentStart);
                                    else
                                        record.put(startStr, guestInfo + "#" + busyPercentStart); // Частично занимает ЛЕВУЮ часть
                                } else if (start.isEqual(guestFinish)) { // Начало совпало вычисляем процент занятого дня
                                    busyPercentFinish = (int) ((Double.parseDouble(timeFormatter.format(reservation.getDateFinish()))) / 24 * 100);
                                    if (record.get(startStr) != null) // Если есть запись, но новый жилец будет СПРАВА в ячейке
                                        record.put(startStr, record.get(startStr) + "||" + guestInfo + "#-" + busyPercentFinish);
                                    else
                                        record.put(startStr, guestInfo + "#-" + busyPercentFinish); // Частично занимает ПРАВУЮ часть
                                } else // Если жилец полностью занимает весь день, всю ячейку
                                    record.put(startStr, guestInfo + "#100");
                                if (record.get("dates") == null)
                                    record.put("dates", dateTimeFormatter.format(reservation.getDateStart()) + " - " + dateTimeFormatter.format(reservation.getDateFinish()));
                            }
                            start = start.plusDays(1);
                            count++;
                        }
                    }
                    // -----

                    result.add(record);
                }
            }
        }
        result.sort((a, b) -> {
            String sectionNameA = a.get("section");
            String sectionNameB = b.get("section");
            if (sectionNameA.contains("#"))
                sectionNameA = sectionNameA.substring(0, sectionNameA.indexOf("#"));
            if (sectionNameB.contains("#"))
                sectionNameB = sectionNameB.substring(0, sectionNameB.indexOf("#"));
            Integer sectionNumberA = Integer.parseInt(sectionNameA);
            Integer sectionNumberB = Integer.parseInt(sectionNameB);
            return sectionNumberA - sectionNumberB;
        });
        return result;
    }
    @Transactional
    public List<GuestDTO> getAllNotCheckoutedBeforeTodayByHotelId(Long hotelId, String dateStr) throws
            ParseException {
        Hotel hotel = hotelRepository.getById(hotelId);
        Date date = dateTimeFormatter.parse(dateStr);
        List<GuestDTO> response = new ArrayList<>();
        for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
            for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                for (Guest guest : guestRepository.findAllByBedRoomAndDateFinishLessThanEqualAndCheckouted(room, date, false)) {
                    GuestDTO guestDTO = MyMapper.GuestToGuestDTO(guest);
                    // Остальные поля вычисляются и не заполняются MyMapper
                    Integer daysCount = Integer.parseInt(String.valueOf(TimeUnit.DAYS.convert(guest.getDateFinish().getTime() - guest.getDateStart().getTime(), TimeUnit.MILLISECONDS)));
                    guestDTO.setDaysCount(daysCount == 0 ? "1" : daysCount.toString());
                    if (guest.getContract() != null) {
                        guestDTO.setContract(MyMapper.ContractToContractDTO(guest.getContract()));
                        guestDTO.setCostByNight(guest.getContract().getCost());
                        if (daysCount == 0)
                            guestDTO.setCost(guest.getContract().getCost());
                        else
                            guestDTO.setCost(guest.getContract().getCost() * daysCount);
                    }
                    if (guest.getEmployee() != null) {
                        Filial filial = filialRepository.findByCode(guest.getEmployee().getIdFilial());
                        String guestPost = postRepository.getById(guest.getEmployee().getIdPoststaff().longValue()).getName();
                        guestDTO.setPost(guestPost);
                        guestDTO.setFilialEmployee(filial.getName());
                        guestDTO.setTabnum(guest.getEmployee().getTabnum());
                    }
                    response.add(guestDTO);
                }
            }
        }
        return response;
    }
    public List<FlatDTO> getAll(Long hotelId, String dateStartStr, String dateFinishStr) throws ParseException {
        Hotel hotel = hotelRepository.getById(hotelId);
        List<FlatDTO> response = new ArrayList<>();
        Date dateStart = null;
        Date dateFinish = null;
        if (!dateStartStr.equals("null") && !dateFinishStr.equals("null")) {
            dateStart = dateTimeFormatter.parse(dateStartStr);
            dateFinish = dateTimeFormatter.parse(dateFinishStr);
        }
        for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
            Boolean isVacant = null;
            String additionalInfo = "";
            if (!dateStartStr.equals("null") && !dateFinishStr.equals("null")) {
                for (Room room: roomRepository.findAllByFlatOrderById(flat)) {
                    for (Bed bed : bedRepository.findAllByRoomAndIsExtra(room, false)) {
                        isVacant = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bed).isEmpty();
                        if (isVacant)
                            isVacant = reservationRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bed).isEmpty();
                        if (isVacant) break;
                    }
                    if (Boolean.TRUE.equals(isVacant)) break;
                }
            }
            if (!dateStartStr.equals("null") && !dateFinishStr.equals("null")) {
                List<FlatLocks> flatLocks = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(dateFinish, dateStart, flat);
                if (!flatLocks.isEmpty()) {
                    isVacant = false;
                    additionalInfo = flatLocks.get(0).getStatus().getId() == 4L ? "flatOrg" : "flatLock";
                }
            }
            FlatDTO flatDTO = new FlatDTO();
            flatDTO.setId(flat.getId());
            if (isVacant == null) flatDTO.setName(flat.getName());
            else flatDTO.setName(flat.getName() + " " + isVacant + " " + additionalInfo);
            response.add(flatDTO);
        }
        return response;
    }
}