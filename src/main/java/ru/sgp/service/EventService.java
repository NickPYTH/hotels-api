package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.EventDTO;
import ru.sgp.dto.EventTypeDTO;
import ru.sgp.model.Event;
import ru.sgp.model.EventType;
import ru.sgp.model.Hotel;
import ru.sgp.repository.EventRepository;
import ru.sgp.repository.EventTypeRepository;
import ru.sgp.repository.HotelRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventTypeRepository eventTypeRepository;
    @Autowired
    private HotelRepository hotelRepository;

    public List<EventDTO> getAll() {
        ModelMapper modelMapper = new ModelMapper();
        return eventRepository.findAll().stream().map(e -> modelMapper.map(e, EventDTO.class)).collect(Collectors.toList());
    }

    public List<EventTypeDTO> getAllTypes() {
        ModelMapper modelMapper = new ModelMapper();
        return eventTypeRepository.findAll().stream().map(e -> modelMapper.map(e, EventTypeDTO.class)).collect(Collectors.toList());
    }

    public EventDTO get(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(eventRepository.getById(id), EventDTO.class);
    }

    @Transactional
    public EventDTO update(EventDTO eventDTO) throws ParseException {
        Event event;
        if (eventDTO.getId() != null) event = eventRepository.getById(eventDTO.getId());
        else event = new Event();
        event.setName(eventDTO.getName());
        event.setDescription(eventDTO.getDescription());
        event.setDateStart(dateFormatter.parse(eventDTO.getDateStart()));
        event.setDateFinish(dateFormatter.parse(eventDTO.getDateFinish()));
        EventType eventType = eventTypeRepository.getById(eventDTO.getType().getId());
        event.setType(eventType);
        Hotel hotel = hotelRepository.getById(eventDTO.getHotel().getId());
        event.setHotel(hotel);
        event.setManCount(eventDTO.getManCount());
        event.setWomenCount(eventDTO.getWomenCount());
        eventRepository.save(event);
        return eventDTO;
    }

}