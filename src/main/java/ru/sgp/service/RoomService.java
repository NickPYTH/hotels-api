package ru.sgp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.BedDTO;
import ru.sgp.dto.RoomDTO;
import ru.sgp.model.Bed;
import ru.sgp.model.Flat;
import ru.sgp.model.Room;
import ru.sgp.repository.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    @Autowired
    private BedRepository bedRepository;

    @Transactional
    public List<BedDTO> getAllBeds(Long roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        List<BedDTO> beds = new ArrayList<>();
        for (Bed bed : bedRepository.findAllByRoom(room)) {
            BedDTO bedDTO = new BedDTO();
            bedDTO.setId(bed.getId());
            bedDTO.setName(bed.getName());
            beds.add(bedDTO);
        }
        return beds;
    }


    @Transactional
    public List<RoomDTO> getAllByFlatId(Long flatId) {
        Flat flat = flatRepository.getById(flatId);
        List<RoomDTO> response = new ArrayList<>();
        for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
            RoomDTO roomDTO = new RoomDTO();
            roomDTO.setId(room.getId());
            roomDTO.setName(room.getName());
            roomDTO.setBedsCount(room.getBedsCount());
            roomDTO.setFlatId(flatId);
            response.add(roomDTO);
//            if (guestRepository.findAllByRoomAndCheckouted(room, false).size() < room.getBedsCount()) {
//                RoomDTO roomDTO = new RoomDTO();
//                roomDTO.setId(room.getId());
//                roomDTO.setName(room.getName());
//                roomDTO.setBedsCount(room.getBedsCount());
//                roomDTO.setFlatId(flatId);
//                List<GuestDTO> guestDTOList = new ArrayList<>();
//                for (Guest guest : guestRepository.findAllByRoomAndCheckouted(room, false)) {
//                    GuestDTO guestDTO = new GuestDTO();
//                    guestDTO.setId(guest.getId());
//                    guestDTO.setFirstname(guest.getFirstname());
//                    guestDTO.setLastname(guest.getLastname());
//                    guestDTO.setSecondName(guest.getSecondName());
//                    guestDTO.setRoomId(room.getId());
//                    guestDTO.setRoomName(room.getName());
//                    guestDTO.setFlatId(flatId);
//                    guestDTO.setFlatName(flat.getName());
//                    guestDTO.setFilialId(flat.getHotel().getFilial().getId());
//                    guestDTO.setFilialName(flat.getHotel().getFilial().getName());
//                    guestDTO.setHotelId(flat.getHotel().getId());
//                    guestDTO.setHotelName(flat.getHotel().getName());
//                    guestDTO.setDateStart(dateFormatter.format(guest.getDateStart()));
//                    guestDTO.setDateFinish(dateFormatter.format(guest.getDateFinish()));
//                    guestDTO.setNote(guest.getNote());
//                    guestDTOList.add(guestDTO);
//                }
//                roomDTO.setGuests(guestDTOList);
//                response.add(roomDTO);
//            }
        }
        return response;
    }

}