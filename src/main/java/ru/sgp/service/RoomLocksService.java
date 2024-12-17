package ru.sgp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.RoomLocksDTO;
import ru.sgp.model.Room;
import ru.sgp.model.RoomLocks;
import ru.sgp.repository.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class RoomLocksService {
    @Autowired
    FlatRepository flatRepository;
    @Autowired
    GuestRepository guestRepository;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    StatusRepository statusRepository;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    @Autowired
    private RoomLocksRepository roomLocksRepository;

    public List<RoomLocksDTO> getAllByRoom(Long roomId) throws ParseException {
        Room room = roomRepository.getById(roomId);
        List<RoomLocksDTO> response = new ArrayList<>();
        for (RoomLocks roomLocks : roomLocksRepository.findAllByRoom(room)) {
            RoomLocksDTO roomLocksDTO = new RoomLocksDTO();
            roomLocksDTO.setId(roomLocks.getId());
            roomLocksDTO.setDateStart(dateFormatter.format(roomLocks.getDateStart()));
            roomLocksDTO.setDateFinish(dateFormatter.format(roomLocks.getDateFinish()));
            roomLocksDTO.setStatusId(roomLocks.getStatus().getId());
            roomLocksDTO.setRoomId(roomId);
            response.add(roomLocksDTO);
        }
        return response;
    }

    @Transactional
    public RoomLocksDTO create(RoomLocksDTO roomLocksDTO) throws ParseException {
        if (roomLocksDTO.getId() == 1L) return roomLocksDTO;
        Date s1 = dateFormatter.parse(roomLocksDTO.getDateStart());
        Date f1 = dateFormatter.parse(roomLocksDTO.getDateFinish());
        Room room = roomRepository.getById(roomLocksDTO.getRoomId());
        RoomLocks roomLocks = new RoomLocks();
        List<RoomLocks> roomLocksList = roomLocksRepository.findAllByRoom(room);
        for (RoomLocks roomLock : roomLocksList) {
            if (f1.before(roomLock.getDateStart()) || roomLock.getDateFinish().before(s1)) {

            } else {
                roomLocksDTO.setError("Date range error");
                return roomLocksDTO;
            }
        }
        roomLocks.setDateStart(dateFormatter.parse(roomLocksDTO.getDateStart()));
        roomLocks.setDateFinish(dateFormatter.parse(roomLocksDTO.getDateFinish()));
        roomLocks.setRoom(roomRepository.getById(roomLocksDTO.getRoomId()));
        roomLocks.setStatus(statusRepository.getById(roomLocksDTO.getStatusId()));
        roomLocksRepository.save(roomLocks);
        roomLocksDTO.setId(roomLocks.getId());
        return roomLocksDTO;
    }

    @Transactional
    public RoomLocksDTO update(RoomLocksDTO roomLocksDTO) throws ParseException {
        if (roomLocksDTO.getStatusId() == 1L) {
            roomLocksRepository.deleteById(roomLocksDTO.getId());
            return roomLocksDTO;
        }
        RoomLocks roomLocks = roomLocksRepository.getById(roomLocksDTO.getId());
        roomLocks.setDateStart(dateFormatter.parse(roomLocksDTO.getDateStart()));
        roomLocks.setDateFinish(dateTimeFormatter.parse(roomLocksDTO.getDateFinish() + " 23:59"));
        roomLocks.setRoom(roomRepository.getById(roomLocksDTO.getRoomId()));
        roomLocks.setStatus(statusRepository.getById(roomLocksDTO.getStatusId()));
        roomLocksRepository.save(roomLocks);
        return roomLocksDTO;
    }

    public Long delete(Long roomLockId) throws ParseException {
        roomLocksRepository.deleteById(roomLockId);
        return roomLockId;
    }

    @Transactional
    public RoomLocksDTO get(Long id) {
        RoomLocks roomLocks = roomLocksRepository.getById(id);
        RoomLocksDTO roomLocksDTO = new RoomLocksDTO();
        roomLocksDTO.setId(roomLocks.getId());
        roomLocksDTO.setDateStart(dateFormatter.format(roomLocks.getDateStart()));
        roomLocksDTO.setDateFinish(dateFormatter.format(roomLocks.getDateFinish()));
        roomLocksDTO.setRoomId(roomLocks.getRoom().getId());
        roomLocksDTO.setStatusId(roomLocks.getStatus().getId());
        return roomLocksDTO;
    }
}