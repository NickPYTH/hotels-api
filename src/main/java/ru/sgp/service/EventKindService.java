package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.EventKindDTO;
import ru.sgp.model.EventKind;
import ru.sgp.model.EventType;
import ru.sgp.repository.EventKindRepository;
import ru.sgp.repository.EventTypeRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class EventKindService {
    @Autowired
    private EventKindRepository eventKindRepository;
    @Autowired
    private EventTypeRepository eventTypeRepository;

    @Transactional
    public EventKindDTO update(EventKindDTO eventKindDTO) throws ParseException {
        EventKind eventKind;
        if (eventKindDTO.getId() != null) eventKind = eventKindRepository.getById(eventKindDTO.getId());
        else eventKind = new EventKind();
        eventKind.setName(eventKindDTO.getName());
        eventKind.setDescription(eventKindDTO.getDescription());
        EventType eventType = eventTypeRepository.getById(eventKindDTO.getType().getId());
        eventKind.setType(eventType);
        eventKindRepository.save(eventKind);
        return eventKindDTO;
    }

    public EventKindDTO get(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(eventKindRepository.getById(id), EventKindDTO.class);
    }

    public List<EventKindDTO> getAll() {
        ModelMapper modelMapper = new ModelMapper();
        return eventKindRepository.findAll().stream().map(e -> modelMapper.map(e, EventKindDTO.class)).collect(Collectors.toList());
    }
}