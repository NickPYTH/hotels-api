package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.EventDTO;
import ru.sgp.model.Event;
import ru.sgp.model.EventType;
import ru.sgp.repository.EventRepository;
import ru.sgp.repository.EventTypeRepository;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventTypeRepository eventTypeRepository;
    @Transactional
    public EventDTO update(EventDTO eventDTO) throws ParseException {
        Event event;
        if (eventDTO.getId() != null) event = eventRepository.getById(eventDTO.getId());
        else event = new Event();
        event.setName(eventDTO.getName());
        event.setDescription(eventDTO.getDescription());
        EventType eventType = eventTypeRepository.getById(eventDTO.getType().getId());
        event.setType(eventType);
        eventRepository.save(event);
        return eventDTO;
    }
    public EventDTO get(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(eventRepository.getById(id), EventDTO.class);
    }
    public List<EventDTO> getAll() {
        ModelMapper modelMapper = new ModelMapper();
        return eventRepository.findAll().stream().map(e -> modelMapper.map(e, EventDTO.class)).collect(Collectors.toList());
    }
}