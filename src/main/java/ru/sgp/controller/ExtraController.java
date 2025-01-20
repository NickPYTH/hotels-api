package ru.sgp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.sgp.dto.ExtraDTO;
import ru.sgp.model.Log;
import ru.sgp.repository.LogRepository;
import ru.sgp.service.ExtraService;
import ru.sgp.utils.SecurityManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/extra")
public class ExtraController {

    @Autowired
    LogRepository logsRepository;
    @Autowired
    private ExtraService extraService;
    Logger logger = LoggerFactory.getLogger(ExtraController.class);
    String loggerString = "DATE: {} | Status: {} | User: {} | PATH: {} | DURATION: {} | MESSAGE: {}";
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    @GetMapping(path = "/getAll")
    public ResponseEntity<List<ExtraDTO>> getAll() {
        return new ResponseEntity<>(extraService.getAll(), HttpStatus.OK);
    }

    @GetMapping(path = "/getAllByGuest")
    public ResponseEntity<List<ExtraDTO>> getAllByGuest(@RequestParam Long guestId) {
        return new ResponseEntity<>(extraService.getAllByGuest(guestId), HttpStatus.OK);
    }

    @PostMapping(path = "/create")
    public ResponseEntity<ExtraDTO> create(@RequestBody ExtraDTO ExtraDTO) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            ExtraDTO response = extraService.create(ExtraDTO);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/extra/create", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/extra/create");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/extra/create", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/extra/create");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(path = "/createGuestExtra")
    public ResponseEntity<ExtraDTO> createGuestExtra(@RequestParam Long guestId, @RequestParam Long extraId) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            ExtraDTO response = extraService.createGuestExtra(guestId, extraId);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/extra/createGuestExtra", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/extra/createGuestExtra");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/extra/createGuestExtra", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/extra/createGuestExtra");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(path = "/deleteGuestExtra")
    public ResponseEntity<ExtraDTO> deleteGuestExtra(@RequestParam Long guestId, @RequestParam Long extraId) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            ExtraDTO response = extraService.deleteGuestExtra(guestId, extraId);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/extra/createGuestExtra", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/extra/createGuestExtra");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/extra/createGuestExtra", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/extra/createGuestExtra");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(path = "/update")
    public ResponseEntity<ExtraDTO> update(@RequestBody ExtraDTO ExtraDTO) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            ExtraDTO response = extraService.update(ExtraDTO);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/extra/update", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/extra/update");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/extra/update", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/extra/update");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
