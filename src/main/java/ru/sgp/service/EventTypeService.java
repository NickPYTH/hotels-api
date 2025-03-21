package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.EventTypeDTO;
import ru.sgp.model.EventType;
import ru.sgp.repository.EventTypeRepository;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventTypeService {
    @Autowired
    private EventTypeRepository eventTypeRepository;
    @Transactional
    public EventTypeDTO update(EventTypeDTO eventTypeDTO) throws ParseException {
        EventType eventType;
        if (eventTypeDTO.getId() != null) eventType = eventTypeRepository.getById(eventTypeDTO.getId());
        else eventType = new EventType();
        eventType.setName(eventTypeDTO.getName());
        eventTypeRepository.save(eventType);
        return eventTypeDTO;
    }
    public EventTypeDTO get(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(eventTypeRepository.getById(id), EventTypeDTO.class);
    }
    public List<EventTypeDTO> getAll() {
        ModelMapper modelMapper = new ModelMapper();
        return eventTypeRepository.findAll().stream().map(e -> modelMapper.map(e, EventTypeDTO.class)).collect(Collectors.toList());
    }
}