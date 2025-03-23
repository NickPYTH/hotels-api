package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.BedDTO;
import ru.sgp.dto.RoomDTO;
import ru.sgp.model.*;
import ru.sgp.repository.*;
import ru.sgp.utils.MyMapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class RoomService {
    @Autowired
    FlatRepository flatRepository;
    @Autowired
    GuestRepository guestRepository;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    StatusRepository statusRepository;
    @Autowired
    RoomLocksRepository roomLocksRepository;
    @Autowired
    BedRepository bedRepository;
    @Autowired
    ReservationRepository reservationRepository;
    @Autowired
    FlatLocksRepository flatLocksRepository;
    @Transactional
    public List<BedDTO> getAllBeds(Long roomId, String dateStartStr, String dateFinishStr) throws ParseException {
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        Room room = roomRepository.findById(roomId).orElse(null);
        List<BedDTO> beds = new ArrayList<>();
        Date dateStart = null;
        Date dateFinish = null;
        if (!dateStartStr.equals("null") && !dateFinishStr.equals("null")) {
            dateStart = dateTimeFormatter.parse(dateStartStr);
            dateFinish = dateTimeFormatter.parse(dateFinishStr);
        }
        for (Bed bed : bedRepository.findAllByRoom(room)) {
            Boolean isVacant = null;
            String additionalInfo = "";
            if (!dateStartStr.equals("null") && !dateFinishStr.equals("null")){
                isVacant = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bed).isEmpty();
                if (isVacant)
                    isVacant = reservationRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bed).isEmpty();
            }

            if (!dateStartStr.equals("null") && !dateFinishStr.equals("null")) {
                List<FlatLocks> flatLocks = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(dateFinish, dateStart, room.getFlat());
                if (!flatLocks.isEmpty()) {
                    isVacant = false;
                    additionalInfo = flatLocks.get(0).getStatus().getId() == 4L ? "flatOrg" : "flatLock";
                }
                List<RoomLocks> roomLocks = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(dateFinish, dateStart, room);
                if (!roomLocks.isEmpty()) {
                    isVacant = false;
                    additionalInfo = roomLocks.get(0).getStatus().getId() == 3L ? "roomOrg" : "roomLock";
                }
            }
            BedDTO bedDTO = MyMapper.BedToBedDTO(bed);
            if (isVacant == null) bedDTO.setName(bed.getName());
            else bedDTO.setName(bed.getName() + " " + isVacant + " " + additionalInfo);
            beds.add(bedDTO);
        }
        return beds;
    }
    @Transactional
    public List<RoomDTO> getAllByFlatId(Long flatId, String dateStartStr, String dateFinishStr) throws ParseException {
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        Flat flat = flatRepository.getById(flatId);
        List<RoomDTO> response = new ArrayList<>();
        Date dateStart = null;
        Date dateFinish = null;
        if (!dateStartStr.equals("null") && !dateFinishStr.equals("null")) {
            dateStart = dateTimeFormatter.parse(dateStartStr);
            dateFinish = dateTimeFormatter.parse(dateFinishStr);
        }
        String additionalInfo = "";
        for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
            Boolean isVacant = null;
            if (!dateStartStr.equals("null") && !dateFinishStr.equals("null")) {
                for (Bed bed : bedRepository.findAllByRoomAndIsExtra(room, false)) {
                    isVacant = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bed).isEmpty();
                    if (isVacant)
                        isVacant = reservationRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bed).isEmpty();
                    if (isVacant) break;
                }
            }
            if (!dateStartStr.equals("null") && !dateFinishStr.equals("null")) {
                List<FlatLocks> flatLocks = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(dateFinish, dateStart, flat);
                if (!flatLocks.isEmpty()) {
                    isVacant = false;
                    additionalInfo = flatLocks.get(0).getStatus().getId() == 4L ? "flatOrg" : "flatLock";
                }
                List<RoomLocks> roomLocks = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(dateFinish, dateStart, room);
                if (!roomLocks.isEmpty()) {
                    isVacant = false;
                    additionalInfo = roomLocks.get(0).getStatus().getId() == 3L ? "roomOrg" : "roomLock";
                }
            }
            RoomDTO roomDTO = new RoomDTO();
            roomDTO.setId(room.getId());
            if (isVacant == null) roomDTO.setName(room.getName());
            else roomDTO.setName(room.getName() + " " + isVacant + " " + additionalInfo);
            roomDTO.setBedsCount(room.getBedsCount());
            roomDTO.setFlatId(flatId);
            response.add(roomDTO);
        }
        return response;
    }
    @Transactional
    public BedDTO getAvailableBedWithRoomByFlatId(Long flatId, String dateStartStr, String dateFinishStr) throws ParseException {
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        Flat flat = flatRepository.getById(flatId);
        Date dateStart = dateTimeFormatter.parse(dateStartStr);
        Date dateFinish = dateTimeFormatter.parse(dateFinishStr);

        // Проходим по всем местам в секции пок не найдем первое сводное на запрашиваемом диапазоне
        for (Room room: roomRepository.findAllByFlatOrderById(flat)){
            for (Bed bed: bedRepository.findAllByRoomAndIsExtra(room, false)){
                List<Guest> guests = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bed);
                if (guests.isEmpty()){
                    ModelMapper modelMapper = new ModelMapper();
                    return modelMapper.map(bed, BedDTO.class);
                }
            }
        }
        // -----

        return new BedDTO();
    }
}