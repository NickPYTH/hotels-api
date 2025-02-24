package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.BedDTO;
import ru.sgp.dto.RoomDTO;
import ru.sgp.model.Bed;
import ru.sgp.model.Flat;
import ru.sgp.model.Guest;
import ru.sgp.model.Room;
import ru.sgp.repository.*;

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
    private BedRepository bedRepository;
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    @Transactional
    public List<BedDTO> getAllBeds(Long roomId, String dateStartStr, String dateFinishStr) throws ParseException {
        Room room = roomRepository.findById(roomId).orElse(null);
        List<BedDTO> beds = new ArrayList<>();
        for (Bed bed : bedRepository.findAllByRoom(room)) {
            Boolean isVacant = null;
            if (!dateStartStr.equals("null") && !dateFinishStr.equals("null")){
                Date dateStart = dateTimeFormatter.parse(dateStartStr);
                Date dateFinish = dateTimeFormatter.parse(dateFinishStr);
                isVacant = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bed).isEmpty();
            }
            BedDTO bedDTO = new BedDTO();
            bedDTO.setId(bed.getId());
            if (isVacant == null) bedDTO.setName(bed.getName());
            else bedDTO.setName(bed.getName() + " " + isVacant);
            beds.add(bedDTO);
        }
        return beds;
    }
    @Transactional
    public List<RoomDTO> getAllByFlatId(Long flatId, String dateStartStr, String dateFinishStr) throws ParseException {
        Flat flat = flatRepository.getById(flatId);
        List<RoomDTO> response = new ArrayList<>();
        for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
            Boolean isVacant = null;
            if (!dateStartStr.equals("null") && !dateFinishStr.equals("null")) {
                Date dateStart = dateTimeFormatter.parse(dateStartStr);
                Date dateFinish = dateTimeFormatter.parse(dateFinishStr);
                for (Bed bed : bedRepository.findAllByRoom(room)) {
                    isVacant = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bed).isEmpty();
                    if (isVacant) break;
                }
            }
            RoomDTO roomDTO = new RoomDTO();
            roomDTO.setId(room.getId());
            if (isVacant == null) roomDTO.setName(room.getName());
            else roomDTO.setName(room.getName() + " " + isVacant);
            roomDTO.setBedsCount(room.getBedsCount());
            roomDTO.setFlatId(flatId);
            response.add(roomDTO);
        }
        return response;
    }
    @Transactional
    public BedDTO getAvailableBedWithRoomByFlatId(Long flatId, String dateStartStr, String dateFinishStr) throws ParseException {
        Flat flat = flatRepository.getById(flatId);
        Date dateStart = dateTimeFormatter.parse(dateStartStr);
        Date dateFinish = dateTimeFormatter.parse(dateFinishStr);

        // Проходим по всем местам в секции пок не найдем первое сводное на запрашиваемом диапазоне
        for (Room room: roomRepository.findAllByFlatOrderById(flat)){
            for (Bed bed: bedRepository.findAllByRoom(room)){
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