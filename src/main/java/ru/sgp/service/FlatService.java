package ru.sgp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.BedDTO;
import ru.sgp.dto.FlatDTO;
import ru.sgp.dto.GuestDTO;
import ru.sgp.dto.RoomDTO;
import ru.sgp.model.*;
import ru.sgp.repository.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    private PostRepository postRepository;
    @Autowired
    StatusRepository statusRepository;
    @Autowired
    private BedRepository bedRepository;
    @Autowired
    private RoomLocksRepository roomLocksRepository;
    @Autowired
    private FlatLocksRepository flatLocksRepository;

    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH");

    @Transactional
    public List<FlatDTO> getAllByHotelId(Long hotelId, String dateStr) throws ParseException {
        Hotel hotel = hotelRepository.getById(hotelId);
        Date date = dateTimeFormatter.parse(dateStr);
        List<FlatDTO> response = new ArrayList<>();
        for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
            FlatDTO flatDTO = new FlatDTO();
            flatDTO.setId(flat.getId());
            flatDTO.setName(flat.getName());
            flatDTO.setFloor(flat.getFloor());
            flatDTO.setHotelName(hotel.getName());
            flatDTO.setRoomsCount(roomRepository.findAllByFlatOrderById(flat).size());
            flatDTO.setTech(flat.getTech());
            if (flat.getCategory() != null) {
                flatDTO.setCategory(flat.getCategory().getName());
                flatDTO.setCategoryId(flat.getCategory().getId());
            }
            int bedsCount = 0;
            int emptyBedsCount = 0;
            List<RoomDTO> roomDTOList = new ArrayList<>();
            for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(date, date, room);
                bedsCount += room.getBedsCount();
                emptyBedsCount += room.getBedsCount() - guestRepository.findAllByRoomAndDateStartLessThanEqualAndDateFinishGreaterThan(room, date, date).size();
                RoomDTO roomDTO = new RoomDTO();
                roomDTO.setId(room.getId());
                roomDTO.setName(room.getName());
                if (!roomLocksList.isEmpty()) {
                    roomDTO.setStatusId(roomLocksList.get(0).getStatus().getId());
                    roomDTO.setStatusName(roomLocksList.get(0).getStatus().getName());
                    roomDTO.setRoomLockId(roomLocksList.get(0).getId());
                } else {
                    Status vacant = statusRepository.getById(1L);
                    roomDTO.setStatusId(vacant.getId());
                    roomDTO.setStatusName(vacant.getName());
                }
                roomDTO.setBedsCount(room.getBedsCount());
                List<GuestDTO> guestDTOList = new ArrayList<>();
                for (Guest guest : guestRepository.findAllByRoomAndDateStartLessThanEqualAndDateFinishGreaterThan(room, date, date)) {
                    GuestDTO guestDTO = new GuestDTO();
                    guestDTO.setId(guest.getId());
                    guestDTO.setFirstname(guest.getFirstname());
                    guestDTO.setLastname(guest.getLastname());
                    guestDTO.setSecondName(guest.getSecondName());
                    guestDTO.setRoomId(room.getId());
                    guestDTO.setRoomName(room.getName());
                    guestDTO.setFlatId(room.getFlat().getId());
                    guestDTO.setFlatName(flat.getName());
                    guestDTO.setFilialId(flat.getHotel().getFilial().getId());
                    guestDTO.setFilialName(flat.getHotel().getFilial().getName());
                    guestDTO.setHotelId(flat.getHotel().getId());
                    guestDTO.setHotelName(flat.getHotel().getName());
                    guestDTO.setDateStart(dateTimeFormatter.format(guest.getDateStart()));
                    guestDTO.setDateFinish(dateTimeFormatter.format(guest.getDateFinish()));
                    guestDTO.setNote(guest.getNote());
                    guestDTO.setRegPoMestu(guest.getRegPoMestu());
                    guestDTO.setMemo(guest.getMemo());
                    guestDTO.setReason(guest.getReason().getName());
                    guestDTO.setBilling(guest.getBilling());
                    guestDTO.setMale(guest.getMale());
                    guestDTO.setCheckouted(guest.getCheckouted());
                    if (guest.getEmployee() != null) {
                        if (guest.getEmployee().getIdPoststaff() != null)
                            guestDTO.setPost(postRepository.getById(guest.getEmployee().getIdPoststaff().longValue()).getName());
                    }
                    if (guest.getEmployee() != null) {
                        Filial filial = filialRepository.findByCode(guest.getEmployee().getIdFilial());
                        guestDTO.setFilialEmployee(filial.getName());
                        guestDTO.setTabnum(guest.getEmployee().getTabnum());
                    } else {
                        if (guest.getOrganization() != null) {
                            guestDTO.setFilialEmployee(guest.getOrganization().getName());
                            guestDTO.setOrganization(guest.getOrganization().getName());
                        }
                    }
                    guestDTOList.add(guestDTO);
                }
                roomDTO.setGuests(guestDTOList);
                roomDTOList.add(roomDTO);
            }
            flatDTO.setRooms(roomDTOList);
            flatDTO.setBedsCount(bedsCount);
            flatDTO.setEmptyBedsCount(emptyBedsCount);
            List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(date, date, flat);
            if (!flatLocksList.isEmpty()) {
                flatDTO.setStatusId(flatLocksList.get(0).getStatus().getId());
                flatDTO.setStatus(flatLocksList.get(0).getStatus().getName());
                flatDTO.setFlatLockId(flatLocksList.get(0).getId());
            } else {
                Status vacant = statusRepository.getById(1L);
                flatDTO.setStatusId(vacant.getId());
                flatDTO.setStatus(vacant.getName());
            }
            flatDTO.setHotelId(hotelId);
            flatDTO.setNote(flat.getNote());
            response.add(flatDTO);
        }
        return response;
    }

    @Transactional
    public FlatDTO get(Long flatId, String dateStr) throws ParseException {
        Flat flat = flatRepository.getById(flatId);
        FlatDTO flatDTO = new FlatDTO();
        Date date = dateTimeFormatter.parse(dateStr);
        List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(date, date, flat);
        if (!flatLocksList.isEmpty()) {
            flatDTO.setStatusId(flatLocksList.get(0).getStatus().getId());
            flatDTO.setStatus(flatLocksList.get(0).getStatus().getName());
            flatDTO.setFlatLockId(flatLocksList.get(0).getId());
        } else {
            Status vacant = statusRepository.getById(1L);
            flatDTO.setStatusId(vacant.getId());
            flatDTO.setStatus(vacant.getName());
        }
        flatDTO.setId(flatId);
        flatDTO.setName(flat.getName());
        flatDTO.setFloor(flat.getFloor());
        flatDTO.setHotelId(flat.getHotel().getId());
        if (flat.getCategory() != null) {
            flatDTO.setCategory(flat.getCategory().getName());
            flatDTO.setCategoryId(flat.getCategory().getId());
        }
        flatDTO.setFilialId(flat.getHotel().getFilial().getId());
        flatDTO.setNote(flat.getNote());
        flatDTO.setTech(flat.getTech());
        List<RoomDTO> roomDTOList = new ArrayList<>();
        for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
            List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(date, date, room);
            RoomDTO roomDTO = new RoomDTO();
            roomDTO.setId(room.getId());
            roomDTO.setName(room.getName());
            if (!roomLocksList.isEmpty()) {
                roomDTO.setStatusId(roomLocksList.get(0).getStatus().getId());
                roomDTO.setStatusName(roomLocksList.get(0).getStatus().getName());
                roomDTO.setRoomLockId(roomLocksList.get(0).getId());
            } else {
                Status vacant = statusRepository.getById(1L);
                roomDTO.setStatusId(vacant.getId());
                roomDTO.setStatusName(vacant.getName());
            }
            roomDTO.setFilialId(flat.getHotel().getFilial().getId());
            roomDTO.setHotelId(flat.getHotel().getId());
            roomDTO.setFlatId(flatId);
            roomDTO.setBedsCount(room.getBedsCount());
            List<GuestDTO> guestDTOList = new ArrayList<>();
            for (Guest guest : guestRepository.findAllByRoomAndDateStartLessThanEqualAndDateFinishGreaterThan(room, date, date)) {
                GuestDTO guestDTO = new GuestDTO();
                guestDTO.setId(guest.getId());
                guestDTO.setFirstname(guest.getFirstname());
                guestDTO.setLastname(guest.getLastname());
                guestDTO.setSecondName(guest.getSecondName());
                guestDTO.setRoomId(room.getId());
                guestDTO.setRoomName(room.getName());
                guestDTO.setFlatId(flatId);
                guestDTO.setFlatName(flat.getName());
                guestDTO.setFilialId(flat.getHotel().getFilial().getId());
                guestDTO.setFilialName(flat.getHotel().getFilial().getName());
                guestDTO.setHotelId(flat.getHotel().getId());
                guestDTO.setHotelName(flat.getHotel().getName());
                guestDTO.setDateStart(dateTimeFormatter.format(guest.getDateStart()));
                guestDTO.setDateFinish(dateTimeFormatter.format(guest.getDateFinish()));
                guestDTO.setNote(guest.getNote());
                guestDTO.setRegPoMestu(guest.getRegPoMestu());
                guestDTO.setMemo(guest.getMemo());
                guestDTO.setReason(guest.getReason().getName());
                guestDTO.setBilling(guest.getBilling());
                guestDTO.setCheckouted(guest.getCheckouted());
                guestDTO.setBedName(guest.getBed().getName());
                guestDTO.setBedId(guest.getBed().getId());
                //String daysCount = String.valueOf(TimeUnit.HOURS.convert(guest.getDateFinish().getTime() - guest.getDateStart().getTime(), TimeUnit.MILLISECONDS) / 24);
                Date cuttedStartDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateStart()));
                Date cuttedFinishDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateFinish()));
                String daysCount = String.valueOf(TimeUnit.DAYS.convert(cuttedFinishDate.getTime() - cuttedStartDate.getTime(), TimeUnit.MILLISECONDS));
                guestDTO.setDaysCount(daysCount);
                if (guest.getContract() != null) {
                    guestDTO.setContractId(guest.getContract().getId());
                    guestDTO.setCostByNight(guest.getContract().getCost());
                    guestDTO.setNote(guest.getContract().getDocnum());
                    if (Integer.parseInt(guestDTO.getDaysCount()) > 0)
                        guestDTO.setCost(guestDTO.getCostByNight() * Integer.parseInt(guestDTO.getDaysCount()));
                    else
                        guestDTO.setCost(guestDTO.getCostByNight());
                }
                guestDTO.setMale(guest.getMale());
                if (guest.getEmployee() != null) {
                    Filial filial = filialRepository.findByCode(guest.getEmployee().getIdFilial());
                    String guestPost = "";
                    if (guest.getEmployee().getIdPoststaff() != null)
                        guestPost = postRepository.getById(guest.getEmployee().getIdPoststaff().longValue()).getName();
                    guestDTO.setFilialEmployee(filial.getName());
                    guestDTO.setPost(guestPost);
                    guestDTO.setTabnum(guest.getEmployee().getTabnum());
                } else {
                    if (guest.getOrganization() != null) {
                        guestDTO.setFilialEmployee(guest.getOrganization().getName());
                        guestDTO.setOrganization(guest.getOrganization().getName());
                    }
                }
                guestDTOList.add(guestDTO);
            }
            roomDTO.setGuests(guestDTOList);
            List<BedDTO> bedDTOList = new ArrayList<>();
            for (Bed bed : bedRepository.findAllByRoom(room)) {
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

        // Experiments
//        for (Bed bed: bedRepository.findAll()) {
//            //Bed bed = bedRepository.getById(639L);
//            List<Guest> guestOneBedList = guestRepository.findAllByBed(bed);
//            for (Guest guest : guestOneBedList) {  // Будем проходить по жильцам привзяанным к одной кровати и с пересекающимися диапазонами
//                List<Guest> guestOverlapList = guestRepository.findAllByDateStartGreaterThanEqualAndDateFinishLessThanEqualAndBedAndIdIsNot(guest.getDateStart(), guest.getDateFinish(), bed, guest.getId());
//                if (!guestOverlapList.isEmpty()) {
//                    guestOverlapList.add(guest);
//                    List<Bed> bedExp = bedRepository.findAllByRoom(bed.getRoom());
//                    for (int i = 0; i < 6; i++) {
//                        if (i < bedExp.size()) {
//                            if (bedExp.get(i) != null) {
//                                if (i < guestOverlapList.size()) {
//                                    if (guestOverlapList.get(i) != null) {
//                                        guestOverlapList.get(i).setBed(bedExp.get(i));
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }

        // -----

        return flatDTO;
    }

    @Transactional
    public FlatDTO updateTech(Long flatId) {
        Flat flat = flatRepository.getById(flatId);
        flat.setTech(!flat.getTech());
        flatRepository.save(flat);
        FlatDTO flatDTO = new FlatDTO();
        return flatDTO;
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
                for (Bed bed : bedRepository.findAllByRoom(room)) {
                    HashMap<String, String> record = new HashMap<>();
                    record.put("section", flat.getName());
                    record.put("sectionId", flat.getId().toString());
                    record.put("room", room.getId().toString());
                    record.put("roomName", room.getName());
                    record.put("bed", bed.getName());
                    for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBed(dateFinish, dateStart, bed)) {
                        LocalDateTime start = dateStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                        int daysCount = Integer.parseInt(String.valueOf(TimeUnit.DAYS.convert(dateFinish.getTime() - dateStart.getTime(), TimeUnit.MILLISECONDS)));
                        int count = 0;
                        while (count <= daysCount) {
                            LocalDateTime guestStart = dateFormatter.parse(dateTimeFormatter.format(guest.getDateStart())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                            LocalDateTime guestFinish = dateFormatter.parse(dateTimeFormatter.format(guest.getDateFinish())).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                            if ((start.isAfter(guestStart) || start.isEqual(guestStart)) && (start.isBefore(guestFinish) || start.isEqual(guestFinish))) {
                                Integer busyPercentStart = 100;
                                Integer busyPercentFinish = 100;
                                String guestDatesRange = "&" + dateTimeFormatter.format(guest.getDateStart()) + "-" + dateTimeFormatter.format(guest.getDateFinish());
                                if (start.isEqual(guestStart)) { // Начало совпало вычисляем процент занятого дня
                                    busyPercentStart = (int) ((24 - Double.parseDouble(timeFormatter.format(guest.getDateStart()))) / 24 * 100);
                                    if (record.get(start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))) != null) {
                                        record.put(start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), record.get(start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))) + "||" + guest.getLastname() + " " + guest.getFirstname().charAt(0) + ". " + guest.getSecondName().charAt(0) + "." + guestDatesRange + "#" + busyPercentStart);
                                    } else
                                        record.put(start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), guest.getLastname() + " " + guest.getFirstname().charAt(0) + ". " + guest.getSecondName().charAt(0) + "." + guestDatesRange + "#" + busyPercentStart);
                                } else if (start.isEqual(guestFinish)) { // Начало совпало вычисляем процент занятого дня
                                    busyPercentFinish = (int) ((24 - Double.parseDouble(timeFormatter.format(guest.getDateFinish()))) / 24 * 100);
                                    if (record.get(start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))) != null) {
                                        record.put(start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), record.get(start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))) + "||" + guest.getLastname() + " " + guest.getFirstname().charAt(0) + ". " + guest.getSecondName().charAt(0) + "." + guestDatesRange + "#-" + busyPercentFinish);
                                    } else
                                        record.put(start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), guest.getLastname() + " " + guest.getFirstname().charAt(0) + ". " + guest.getSecondName().charAt(0) + "." + guestDatesRange + "#-" + busyPercentFinish);
                                } else {
                                    record.put(start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), guest.getLastname() + " " + guest.getFirstname().charAt(0) + ". " + guest.getSecondName().charAt(0) + "." + guestDatesRange + "#100");
                                }
                                if (record.get("dates") == null)
                                    record.put("dates", dateTimeFormatter.format(guest.getDateStart()) + " - " + dateTimeFormatter.format(guest.getDateFinish()));
                                if (record.get("post") == null) {
                                    if (guest.getEmployee() != null)
                                        if (guest.getEmployee().getIdPoststaff() != null)
                                            record.put("post", postRepository.getById(guest.getEmployee().getIdPoststaff().longValue()).getName());
                                }
                            }
                            start = start.plusDays(1);
                            count++;
                        }
                    }

                    result.add(record);
                }
            }
        }
        return result;
    }

    @Transactional
    public List<GuestDTO> getAllNotCheckotedBeforeTodayByHotelId(Long hotelId, String dateStr) throws
            ParseException {
        Hotel hotel = hotelRepository.getById(hotelId);
        Date date = dateTimeFormatter.parse(dateStr);
        List<GuestDTO> response = new ArrayList<>();
        for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
            for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                for (Guest guest : guestRepository.findAllByRoomAndDateFinishLessThanEqualAndCheckouted(room, date, false)) {
                    GuestDTO guestDTO = new GuestDTO();
                    guestDTO.setId(guest.getId());
                    guestDTO.setFirstname(guest.getFirstname());
                    guestDTO.setLastname(guest.getLastname());
                    guestDTO.setSecondName(guest.getSecondName());
                    guestDTO.setRoomId(room.getId());
                    guestDTO.setRoomName(room.getName());
                    guestDTO.setFlatId(room.getFlat().getId());
                    guestDTO.setFlatName(flat.getName());
                    guestDTO.setFilialId(flat.getHotel().getFilial().getId());
                    guestDTO.setFilialName(flat.getHotel().getFilial().getName());
                    guestDTO.setHotelId(flat.getHotel().getId());
                    guestDTO.setHotelName(flat.getHotel().getName());
                    guestDTO.setDateStart(dateTimeFormatter.format(guest.getDateStart()));
                    guestDTO.setDateFinish(dateTimeFormatter.format(guest.getDateFinish()));
                    guestDTO.setNote(guest.getNote());
                    guestDTO.setRegPoMestu(guest.getRegPoMestu());
                    guestDTO.setMemo(guest.getMemo());
                    guestDTO.setReason(guest.getReason().getName());
                    guestDTO.setBilling(guest.getBilling());
                    guestDTO.setMale(guest.getMale());
                    guestDTO.setCheckouted(guest.getCheckouted());
                    Integer daysCount = Integer.parseInt(String.valueOf(TimeUnit.DAYS.convert(guest.getDateFinish().getTime() - guest.getDateStart().getTime(), TimeUnit.MILLISECONDS)));
                    guestDTO.setDaysCount(daysCount == 0 ? "1" : daysCount.toString());
                    if (guest.getContract() != null) {
                        guestDTO.setContractNumber(guest.getContract().getDocnum());
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
                    } else {
                        if (guest.getOrganization() != null) {
                            guestDTO.setFilialEmployee(guest.getOrganization().getName());
                            guestDTO.setOrganization(guest.getOrganization().getName());
                        }
                    }
                    response.add(guestDTO);
                }
            }
        }
        return response;
    }
}