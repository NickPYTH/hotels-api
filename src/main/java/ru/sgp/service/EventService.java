package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.EventDTO;
import ru.sgp.dto.chess.ChessEvent;
import ru.sgp.model.Event;
import ru.sgp.model.Hotel;
import ru.sgp.repository.EventKindRepository;
import ru.sgp.repository.EventRepository;
import ru.sgp.repository.HotelRepository;

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
public class EventService {
    @Autowired
    private EventKindRepository eventKindRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private HotelRepository hotelRepository;

    @Transactional
    public EventDTO update(EventDTO eventDTO) throws ParseException {
        Event event;
        if (eventDTO.getId() != null) event = eventRepository.getById(eventDTO.getId());
        else event = new Event();
        event.setKind(eventKindRepository.getById(eventDTO.getKind().getId()));
        event.setHotel(hotelRepository.getById(eventDTO.getHotel().getId()));
        event.setDateStart(new Date(eventDTO.getDateStart() * 1000));
        event.setDateFinish(new Date(eventDTO.getDateFinish() * 1000));
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

    public List<ChessEvent> getAllByDateRange(Long dateStartTS, Long dateFinishTS, Long hotelId) throws ParseException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
        ModelMapper modelMapper = new ModelMapper();
        Hotel hotel = hotelRepository.getById(hotelId);
        Date dateStart = new Date(dateStartTS * 1000);
        Date dateFinish = new Date(dateFinishTS * 1000);
        int daysCount = Integer.parseInt(String.valueOf(TimeUnit.DAYS.convert(dateFinish.getTime() - dateStart.getTime(), TimeUnit.MILLISECONDS)));
        List<ChessEvent> response = new ArrayList<>();
        LocalDateTime start = dateStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        int count = 0;
        while (count <= daysCount + 1) {
            ChessEvent chessEvent = new ChessEvent();
            String dateStr = start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            Date date = dateFormatter.parse(dateStr);
            chessEvent.setDate(dateStr);
            List<EventDTO> dateEvents = new ArrayList<>();
            for (Event event : eventRepository.findAllByHotelAndDateStartLessThanEqualAndDateFinishGreaterThanEqual(hotel, date, date)) {
                EventDTO eventDTO = modelMapper.map(event, EventDTO.class);
                dateEvents.add(eventDTO);
            }
            chessEvent.setEvents(dateEvents);
            start = start.plusDays(1);
            count++;
            response.add(chessEvent);
        }
        return response;
    }
}