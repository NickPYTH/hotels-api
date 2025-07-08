package ru.sgp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.sgp.dto.EventKindDTO;
import ru.sgp.model.Log;
import ru.sgp.repository.LogRepository;
import ru.sgp.service.EventKindService;
import ru.sgp.utils.SecurityManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/eventKind")
public class EventKindController {
    @Autowired
    LogRepository logsRepository;
    @Autowired
    private EventKindService eventKindService;
    Logger logger = LoggerFactory.getLogger(EventKindController.class);
    String loggerString = "DATE: {} | Status: {} | User: {} | PATH: {} | DURATION: {} | MESSAGE: {}";
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    @PostMapping(path = "/create")
    public ResponseEntity<EventKindDTO> create(@RequestBody EventKindDTO EventKindDTO) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            EventKindDTO response = eventKindService.update(EventKindDTO);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", ru.sgp.utils.SecurityManager.getCurrentUser(), "/eventKind/create", duration, "");
            record.setStatus("OK");
            record.setUser(ru.sgp.utils.SecurityManager.getCurrentUser());
            record.setPath("/eventKind/create");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", ru.sgp.utils.SecurityManager.getCurrentUser(), "/eventKind/create", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/eventKind/create");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(path = "/update")
    public ResponseEntity<EventKindDTO> update(@RequestBody EventKindDTO EventKindDTO) throws ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            EventKindDTO response = eventKindService.update(EventKindDTO);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", ru.sgp.utils.SecurityManager.getCurrentUser(), "/eventKind/update", duration, "");
            record.setStatus("OK");
            record.setUser(ru.sgp.utils.SecurityManager.getCurrentUser());
            record.setPath("/eventKind/update");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", ru.sgp.utils.SecurityManager.getCurrentUser(), "/eventKind/update", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/eventKind/update");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getAll")
    public ResponseEntity<List<EventKindDTO>> getAll() {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            List<EventKindDTO> response = eventKindService.getAll();
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", ru.sgp.utils.SecurityManager.getCurrentUser(), "/eventKind/getAll", duration, "");
            record.setStatus("OK");
            record.setUser(ru.sgp.utils.SecurityManager.getCurrentUser());
            record.setPath("/eventKind/getAll");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", ru.sgp.utils.SecurityManager.getCurrentUser(), "/eventKind/getAll", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/eventKind/getAll");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
