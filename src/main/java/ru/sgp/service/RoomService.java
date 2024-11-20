package ru.sgp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.FlatDTO;
import ru.sgp.dto.GuestDTO;
import ru.sgp.dto.RoomDTO;
import ru.sgp.model.Flat;
import ru.sgp.model.Guest;
import ru.sgp.model.Room;
import ru.sgp.repository.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoomService {
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
    StatusRepository statusRepository;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");

    @Transactional
    public List<RoomDTO> getAllByFlatId(Long flatId) {
        Flat flat = flatRepository.getById(flatId);
        List<RoomDTO> response = new ArrayList<>();
        for (Room room: roomRepository.findAllByFlatOrderById(flat)) {
            if (guestRepository.findAllByRoomAndCheckouted(room, false).size() < room.getBedsCount()) {
                RoomDTO roomDTO = new RoomDTO();
                roomDTO.setId(room.getId());
                roomDTO.setName(room.getName());
                roomDTO.setBedsCount(room.getBedsCount());
                roomDTO.setStatusName(room.getStatus().getName());
                roomDTO.setStatusId(room.getStatus().getId());
                roomDTO.setFlatId(flatId);
                List<GuestDTO> guestDTOList = new ArrayList<>();
                for (Guest guest : guestRepository.findAllByRoomAndCheckouted(room, false)) {
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
                    guestDTO.setDateStart(dateFormatter.format(guest.getDateStart()));
                    guestDTO.setDateFinish(dateFormatter.format(guest.getDateFinish()));
                    guestDTO.setNote(guest.getNote());
                    guestDTOList.add(guestDTO);
                }
                roomDTO.setGuests(guestDTOList);
                response.add(roomDTO);
            }
        }
        return response;
    }

    @Transactional
    public RoomDTO updateStatus(Long roomId) {
        Room room = roomRepository.getById(roomId);
        if (room.getStatus().getId() == 1L) {
            room.setStatus(statusRepository.getById(2L));
        }
        else if (room.getStatus().getId() == 2L) {
            room.setStatus(statusRepository.getById(1L));
        }
        roomRepository.save(room);
        boolean isAllRoomBusy = true;
        for (Room roomTmp: roomRepository.findAllByFlatOrderById(room.getFlat())){
            if (roomTmp.getStatus().getId() == 1L) isAllRoomBusy = false;
        }
        if (isAllRoomBusy){
            Flat flat = room.getFlat();
            flat.setStatus(statusRepository.getById(2L)); // set busy if all rooms is busy
            flatRepository.save(flat);
        }
        return new RoomDTO();
    }
}