package ru.sgp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.sgp.dto.GuestDTO;
import ru.sgp.dto.HistoryDTO;
import ru.sgp.model.History;
import ru.sgp.model.Log;
import ru.sgp.repository.HistoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoryService {

    @Autowired
    HistoryRepository historyRepository;

    public void updateGuest(Log request, GuestDTO guestBefore, GuestDTO guestAfter) throws JsonProcessingException {
        History history = new History();
        ObjectMapper mapper = new ObjectMapper();
        history.setRequest(request);
        history.setEntityType("guest");
        history.setEntityId(guestBefore.getId());
        history.setStateBefore(mapper.writeValueAsString(guestBefore));
        history.setStateAfter(mapper.writeValueAsString(guestAfter));
        historyRepository.save(history);
    }

    public List<HistoryDTO> getGuestHistory(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        return historyRepository.findAllByEntityId(id).stream().map(h -> modelMapper.map(h, HistoryDTO.class)).collect(Collectors.toList());
    }
}