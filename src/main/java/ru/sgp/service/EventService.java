package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.EventDTO;
import ru.sgp.dto.EventTypeDTO;
import ru.sgp.model.Event;
import ru.sgp.repository.EventRepository;
import ru.sgp.repository.EventTypeRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    ModelMapper modelMapper = new ModelMapper();
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventTypeRepository eventTypeRepository;

    public List<EventDTO> getAll() {
        return eventRepository.findAll().stream().map(e -> modelMapper.map(e, EventDTO.class)).collect(Collectors.toList());
    }

    public List<EventTypeDTO> getAllTypes() {
        return eventTypeRepository.findAll().stream().map(e -> modelMapper.map(e, EventTypeDTO.class)).collect(Collectors.toList());
    }

    public EventDTO get(Long id) {
        return modelMapper.map(eventRepository.getById(id), EventDTO.class);
    }

    @Transactional
    public EventDTO update(EventDTO eventDTO) throws ParseException {
        Event event = modelMapper.map(eventDTO, Event.class);
        event.setDateStart(dateFormatter.parse(eventDTO.getDateStart()));
        event.setDateFinish(dateFormatter.parse(eventDTO.getDateFinish()));
        eventRepository.save(event);
        return eventDTO;
    }

    @Transactional
    public EventDTO create(EventDTO eventDTO) throws ParseException {
        Event event = modelMapper.map(eventDTO, Event.class);
        event.setDateStart(dateFormatter.parse(eventDTO.getDateStart()));
        event.setDateFinish(dateFormatter.parse(eventDTO.getDateFinish()));
        eventRepository.save(event);
        return eventDTO;
    }

}