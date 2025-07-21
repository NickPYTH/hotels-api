package ru.sgp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.sgp.dto.GuestDTO;
import ru.sgp.dto.HistoryDTO;
import ru.sgp.dto.ReservationDTO;
import ru.sgp.model.History;
import ru.sgp.model.Log;
import ru.sgp.repository.HistoryRepository;
import ru.sgp.repository.ReservationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoryService {

    @Autowired
    HistoryRepository historyRepository;
    @Autowired
    private ReservationRepository reservationRepository;

    public void updateGuest(Log request, GuestDTO guestBefore, GuestDTO guestAfter) throws JsonProcessingException {
        History history = new History();
        ObjectMapper mapper = new ObjectMapper();
        history.setRequest(request);
        history.setEntityType("guest");
        history.setEntityId(guestAfter.getId());
        history.setStateBefore(mapper.writeValueAsString(guestBefore));
        history.setStateAfter(mapper.writeValueAsString(guestAfter));
        historyRepository.save(history);
    }

    public void updateReservation(Log request, ReservationDTO reservationBefore, ReservationDTO reservationAfter) throws JsonProcessingException {
        History history = new History();
        ObjectMapper mapper = new ObjectMapper();
        history.setRequest(request);
        history.setEntityType("reservation");
        history.setEntityId(reservationAfter.getId());
        history.setStateBefore(mapper.writeValueAsString(reservationBefore));
        history.setStateAfter(mapper.writeValueAsString(reservationAfter));
        historyRepository.save(history);
    }

    public List<HistoryDTO> getGuestHistory(Long entityId, String entityType) {
        ModelMapper modelMapper = new ModelMapper();
        return historyRepository.findAllByEntityIdAndEntityType(entityId, entityType).stream().map(h -> modelMapper.map(h, HistoryDTO.class)).collect(Collectors.toList());
    }
}