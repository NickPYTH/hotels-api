package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.*;
import ru.sgp.dto.chess.ChessDate;
import ru.sgp.dto.chess.ChessGuest;
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
import java.util.stream.Collectors;

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
                    Employee employee = employeeRepository.findByTabnumAndEndDate(reservation.getTabnum(), null);
                    guestDTO.setFirstname(employee != null ? employee.getFirstname() : reservation.getFirstname());
                    guestDTO.setLastname(employee != null ? employee.getLastname() : reservation.getLastname());
                    guestDTO.setSecondName(employee != null ? employee.getSecondName() : reservation.getSecondName());
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
                    else guestDTO.setCost(guestDTO.getCostByNight());
                }
                if (guest.getEmployee() != null) {
                    Filial filial = filialRepository.findByCode(guest.getEmployee().getIdFilial());
                    String guestPost = "";
                    if (guest.getEmployee().getIdPoststaff() != null) {
                        Optional<Post> post = postRepository.findById(guest.getEmployee().getIdPoststaff().longValue());
                        if (post.isPresent()) guestPost = post.get().getName();
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
                Employee employee = employeeRepository.findByTabnumAndEndDate(reservation.getTabnum(), null);
                guestDTO.setId(reservation.getId());
                guestDTO.setFirstname(employee != null ? employee.getFirstname() : reservation.getFirstname());
                guestDTO.setLastname(employee != null ? employee.getLastname() : reservation.getLastname());
                guestDTO.setSecondName(employee != null ? employee.getSecondName() : reservation.getSecondName());
                guestDTO.setTabnum(reservation.getTabnum());
                ModelMapper modelMapper = new ModelMapper();
                guestDTO.setBed(modelMapper.map(reservation.getBed(), BedDTO.class));
                guestDTO.setEvent(modelMapper.map(reservation.getEventKind(), EventKindDTO.class));
                guestDTO.setFromFilial(modelMapper.map(reservation.getFromFilial(), FilialDTO.class));
                guestDTO.setBed(MyMapper.BedToBedDTO(reservation.getBed()));
                guestDTO.setDateStart(dateTimeFormatter.format(reservation.getDateStart()));
                guestDTO.setDateFinish(dateTimeFormatter.format(reservation.getDateFinish()));
                guestDTO.setNote(reservation.getNote());
                guestDTO.setIsReservation(true);
                if (reservation.getContract() != null)
                    guestDTO.setContract(modelMapper.map(reservation.getContract(), ContractDTO.class));
                guestDTO.setMale(reservation.getMale());
                if (reservation.getFamilyMemberOfEmployee() != null)
                    guestDTO.setFamilyMemberOfEmployee(reservation.getFamilyMemberOfEmployee());
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
    public List<HashMap<String, String>> getAllByHotelIdChess(Long hotelId, String dateStartStr, String dateFinishStr) throws ParseException {
        List<HashMap<String, String>> result = new ArrayList<>();
        Hotel hotel = hotelRepository.getById(hotelId);
        Date dateStart = dateFormatter.parse(dateStartStr);
        Date dateFinish = dateFormatter.parse(dateFinishStr);
        for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
            for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
//                for (Bed bed : bedRepository.findAllByRoomAndIsExtra(room, true)) {
//                    if (guestRepository.findAllByBed(bed).isEmpty() && reservationRepository.findAllByBed(bed).isEmpty()) {
//                        bedRepository.delete(bed);
//                    }
//                }
                for (Bed bed : bedRepository.findAllByRoom(room)) {

                    if (bed.getIsExtra())
                        if (guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBed(dateFinish, dateStart, bed).isEmpty() && reservationRepository.findAllByDateStartBeforeAndDateFinishAfterAndBed(dateFinish, dateStart, bed).isEmpty())
                            continue;

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
                                String guestDatesRange = dateTimeFormatter.format(guest.getDateStart()) + " :: " + dateTimeFormatter.format(guest.getDateFinish());
                                String startStr = start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                                String fio = guest.getLastname() + " ";
                                if (guest.getFirstname().length() > 0) fio += guest.getFirstname().charAt(0) + ". ";
                                if (guest.getSecondName().length() > 0) fio += guest.getSecondName().charAt(0) + ".";
                                if (fio.length() > 16)
                                    fio = fio.substring(0, 15); // Обрезаем иначе не поместится в ячейку минимальную
                                String post = "";
                                String filial = "emptyF";
                                if (guest.getEmployee() != null) {
                                    filial = filialRepository.findByCode(guest.getEmployee().getIdFilial()).getName();
                                    if (guest.getEmployee().getIdPoststaff() != null) {
                                        Optional<Post> optionalPost = postRepository.findById(guest.getEmployee().getIdPoststaff().longValue());
                                        if (optionalPost.isPresent()) post = optionalPost.get().getName();
                                    }
                                }
                                String guestInfo = fio + "&" + guestDatesRange + "&" + guest.getMale() + "&" + guest.getNote() + "&" + post + "&" + filial + "&" + false + "&" + guest.getCheckouted();  // PredПоследний параметр это бронь
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
                                String guestDatesRange = dateTimeFormatter.format(reservation.getDateStart()) + " :: " + dateTimeFormatter.format(reservation.getDateFinish());
                                String startStr = start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                                Employee employee = employeeRepository.findByTabnumAndEndDate(reservation.getTabnum(), null);
                                String fio = "";
                                if (employee != null) {
                                    fio = employee.getLastname() + " ";
                                    if (!employee.getFirstname().isEmpty())
                                        fio += employee.getFirstname().charAt(0) + ". ";
                                    if (!employee.getLastname().isEmpty())
                                        fio += employee.getLastname().charAt(0) + ".";
                                } else {
                                    if (reservation.getLastname() != null) fio += reservation.getLastname();
                                    if (reservation.getFirstname() != null && !reservation.getFirstname().isEmpty())
                                        fio += reservation.getFirstname().charAt(0) + ". ";
                                    if (reservation.getSecondName() != null && !reservation.getSecondName().isEmpty())
                                        fio += reservation.getSecondName().charAt(0) + ". ";
                                }
                                if (fio.length() > 16)
                                    fio = fio.substring(0, 15); // Обрезаем иначе не поместится в ячейку минимальную
                                String post = "";
                                String filial = "";
                                if (employee != null) {
                                    filial = filialRepository.findByCode(employee.getIdFilial()).getName();
                                    if (employee.getIdPoststaff() != null) {
                                        Optional<Post> postOpt = postRepository.findById(employee.getIdPoststaff().longValue());
                                        post = postOpt.isPresent() ? postOpt.get().getName() : "NotFound";
                                    }
                                }
                                String guestInfo = fio + "&" + guestDatesRange + "&" + reservation.getMale() + "&" + reservation.getNote() + "&" + post + "&" + filial + "&" + true; // Последний параметр это бронь
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
            if (sectionNameA.contains("#")) sectionNameA = sectionNameA.substring(0, sectionNameA.indexOf("#"));
            if (sectionNameB.contains("#")) sectionNameB = sectionNameB.substring(0, sectionNameB.indexOf("#"));
            try {
                Integer sectionNumberA = Integer.parseInt(sectionNameA);
                Integer sectionNumberB = Integer.parseInt(sectionNameB);
                return sectionNumberA - sectionNumberB;
            } catch (NumberFormatException e) {
                return 0;
            }

        });
        return result;
    }

    @Transactional
    public List<GuestDTO> getAllNotCheckoutedBeforeTodayByHotelId(Long hotelId, String dateStr) throws ParseException {
        Hotel hotel = hotelRepository.getById(hotelId);
        Date date = dateTimeFormatter.parse(dateStr);
        List<GuestDTO> response = new ArrayList<>();
        for (Guest guest : guestRepository.findAllByBedRoomFlatHotelAndDateFinishLessThanEqual(hotel, date)) {
            if (guest.getCheckouted() == null || !guest.getCheckouted()) {
                GuestDTO guestDTO = MyMapper.GuestToGuestDTO(guest);
                // Остальные поля вычисляются и не заполняются MyMapper
                Integer daysCount = Integer.parseInt(String.valueOf(TimeUnit.DAYS.convert(guest.getDateFinish().getTime() - guest.getDateStart().getTime(), TimeUnit.MILLISECONDS)));
                guestDTO.setDaysCount(daysCount == 0 ? "1" : daysCount.toString());
                if (guest.getContract() != null) {
                    guestDTO.setContract(MyMapper.ContractToContractDTO(guest.getContract()));
                    guestDTO.setCostByNight(guest.getContract().getCost());
                    if (daysCount == 0) guestDTO.setCost(guest.getContract().getCost());
                    else guestDTO.setCost(guest.getContract().getCost() * daysCount);
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
                for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
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

    public List<FlatDTO> getNewChess(Long hotelId, Long dateStartTS, Long dateFinishTS) throws ParseException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
        Hotel hotel = hotelRepository.getById(hotelId);
        Date dateStart = new Date(dateStartTS * 1000);
        Date dateFinish = new Date(dateFinishTS * 1000);
        int daysCount = Integer.parseInt(String.valueOf(TimeUnit.DAYS.convert(dateFinish.getTime() - dateStart.getTime(), TimeUnit.MILLISECONDS)));
        List<FlatDTO> response = new ArrayList<>();
        List<Guest> guests = guestRepository.findAllByBedRoomFlatHotelAndDateStartLessThanEqualAndDateFinishGreaterThanEqual(hotel, dateFinish, dateStart);
        List<Reservation> reservations = reservationRepository.findAllByBedRoomFlatHotelAndDateStartLessThanEqualAndDateFinishGreaterThanEqual(hotel, dateFinish, dateStart);
        for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
            FlatDTO flatDTO = MyMapper.FlatToFlatDTO(flat);
            flatDTO.setRoomsCount(roomRepository.findAllByFlatOrderById(flat).size());
            int bedsCount = 0;
            int emptyBedsCount = 0;
            List<RoomDTO> roomDTOList = new ArrayList<>();
            for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                int roomBedsCount = 0;  // Так считаем из-за extra
                List<BedDTO> bedDTOList = new ArrayList<>();
                RoomDTO roomDTO = MyMapper.RoomToRoomDTO(room);
                for (Bed bed : bedRepository.findAllByRoom(room)) {
                    if (bed.getIsExtra()) continue;
                    BedDTO bedDTO = MyMapper.BedToBedDTO(bed);
                    List<ChessDate> chessDateList = new ArrayList<>();
                    int count = 0;
                    LocalDateTime start = dateStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    while (count <= daysCount + 1) {
                        ChessDate chessDate = new ChessDate();
                        String dateStrForFinish = start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                        Date dateForFinish = dateFormatter.parse(dateStrForFinish);
                        String dateStrForStart = start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
                        Date dateForStart = dateTimeFormatter.parse(dateStrForStart);
                        List<ChessGuest> chessGuests = new ArrayList<>();
                        List<Guest> guestsOfTheDay = guests.stream().filter(g -> g.getBed().equals(bed) && dateForFinish.getTime() <= g.getDateFinish().getTime() && dateForStart.getTime() >= g.getDateStart().getTime()).collect(Collectors.toList());
                        List<Reservation> reservationsOfTheDay = reservations.stream().filter(r -> r.getBed().equals(bed) && dateForFinish.getTime() <= r.getDateFinish().getTime() && dateForStart.getTime() >= r.getDateStart().getTime()).collect(Collectors.toList());
                        for (Guest guest : guestsOfTheDay) {
                            ChessGuest chessGuest = new ChessGuest();
                            chessGuest.setId(guest.getId());
                            chessGuest.setIsCheckouted(guest.getCheckouted());
                            chessGuest.setIsReservation(false);
                            if (guest.getEmployee() != null) {
                                if (guest.getEmployee().getIdPoststaff() != null) {
                                    Optional<Post> post = postRepository.findById(guest.getEmployee().getIdPoststaff().longValue());
                                    if (post.isPresent()) chessGuest.setPost(post.get().getName());
                                }
                            }
                            chessGuest.setName(guest.getFirstname());
                            chessGuest.setLastname(guest.getLastname());
                            chessGuest.setSecondName(guest.getSecondName());
                            chessGuest.setDateStart(guest.getDateStart().getTime() / 1000);
                            chessGuest.setDateFinish(guest.getDateFinish().getTime() / 1000);
                            chessGuest.setMale(guest.getMale());
                            chessGuest.setNote(guest.getNote());
                            chessGuests.add(chessGuest);
                        }
                        for (Reservation reservation : reservationsOfTheDay) {
                            ChessGuest chessGuest = new ChessGuest();
                            chessGuest.setId(reservation.getId());
                            chessGuest.setIsReservation(true);
                            chessGuest.setName(reservation.getFirstname());
                            chessGuest.setLastname(reservation.getLastname());
                            chessGuest.setSecondName(reservation.getSecondName());
                            chessGuest.setDateStart(reservation.getDateStart().getTime() / 1000);
                            chessGuest.setDateFinish(reservation.getDateFinish().getTime() / 1000);
                            chessGuest.setMale(reservation.getMale());
                            chessGuest.setNote(reservation.getNote());
                            chessGuests.add(chessGuest);
                        }
                        chessDate.setGuests(chessGuests);
                        chessDate.setDate(dateStrForStart);
                        chessDateList.add(chessDate);
                        start = start.plusDays(1);
                        count++;
                    }
                    bedDTO.setDates(chessDateList);
                    bedDTOList.add(bedDTO);
                    roomBedsCount++;
                }
                roomDTO.setBedsCount(roomBedsCount);
                roomDTO.setBeds(bedDTOList);
                roomDTOList.add(roomDTO);
                bedsCount += roomBedsCount;
            }
            flatDTO.setRooms(roomDTOList);
            flatDTO.setBedsCount(bedsCount);
            flatDTO.setEmptyBedsCount(emptyBedsCount);
            response.add(flatDTO);
        }
        return response;
    }
}