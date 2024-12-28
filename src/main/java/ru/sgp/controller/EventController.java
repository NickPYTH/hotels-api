package ru.sgp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.sgp.dto.EventDTO;
import ru.sgp.dto.EventTypeDTO;
import ru.sgp.model.Log;
import ru.sgp.repository.LogRepository;
import ru.sgp.service.EventService;
import ru.sgp.utils.SecurityManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/event")
public class EventController {

    @Autowired
    LogRepository logsRepository;
    @Autowired
    private EventService eventService;
    Logger logger = LoggerFactory.getLogger(MVZController.class);
    String loggerString = "DATE: {} | Status: {} | User: {} | PATH: {} | DURATION: {} | MESSAGE: {}";
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    @GetMapping(path = "/getAll")
    public ResponseEntity<List<EventDTO>> getAll() {
        return new ResponseEntity<>(eventService.getAll(), HttpStatus.OK);
    }

    @GetMapping(path = "/getAllTypes")
    public ResponseEntity<List<EventTypeDTO>> getAllTypes() {
        return new ResponseEntity<>(eventService.getAllTypes(), HttpStatus.OK);
    }

    @PostMapping(path = "/create")
    public ResponseEntity<EventDTO> create(@RequestBody EventDTO EventDTO) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            EventDTO response = eventService.create(EventDTO);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", ru.sgp.utils.SecurityManager.getCurrentUser(), "/event/create", duration, "");
            record.setStatus("OK");
            record.setUser(ru.sgp.utils.SecurityManager.getCurrentUser());
            record.setPath("/event/create");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", ru.sgp.utils.SecurityManager.getCurrentUser(), "/event/create", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/event/create");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(path = "/update")
    public ResponseEntity<EventDTO> update(@RequestBody EventDTO EventDTO) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            EventDTO response = eventService.update(EventDTO);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", ru.sgp.utils.SecurityManager.getCurrentUser(), "/event/update", duration, "");
            record.setStatus("OK");
            record.setUser(ru.sgp.utils.SecurityManager.getCurrentUser());
            record.setPath("/event/update");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", ru.sgp.utils.SecurityManager.getCurrentUser(), "/event/update", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/event/update");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
