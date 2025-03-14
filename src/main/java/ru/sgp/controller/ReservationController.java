package ru.sgp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.sgp.dto.ReservationDTO;
import ru.sgp.model.Log;
import ru.sgp.repository.LogRepository;
import ru.sgp.service.ReservationService;
import ru.sgp.utils.SecurityManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/reservation")
public class ReservationController {

    @Autowired
    LogRepository logsRepository;
    @Autowired
    private ReservationService reservationService;
    Logger logger = LoggerFactory.getLogger(ReservationController.class);
    String loggerString = "DATE: {} | Status: {} | User: {} | PATH: {} | DURATION: {} | MESSAGE: {}";
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    @GetMapping(path = "/getAll")
    public ResponseEntity<List<ReservationDTO>> getAll() {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            List<ReservationDTO> response = reservationService.getAll();
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/reservation/getAll", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/reservation/getAll");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/reservation/getAll", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/reservation/getAll");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/get")
    public ResponseEntity<ReservationDTO> get(@RequestParam Long id) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            ReservationDTO response = reservationService.get(id);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/reservation/get", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/reservation/get");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/reservation/get", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/reservation/get");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(path = "/delete")
    public ResponseEntity<ReservationDTO> delete(@RequestParam Long id) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            ReservationDTO response = reservationService.delete(id);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/reservation/delete", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/reservation/delete");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/reservation/delete", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/reservation/delete");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @PostMapping(path = "/update")
    public ResponseEntity<ReservationDTO> update(@RequestBody ReservationDTO reservationDTO) throws ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        ReservationDTO response = reservationService.update(reservationDTO);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/reservation/update", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/reservation/update");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/reservation/update", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/reservation/update");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/confirm")
    public ResponseEntity<ReservationDTO> confirm(@RequestParam Long id) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            ReservationDTO response = reservationService.confirm(id);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/reservation/confirm", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/reservation/confirm");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/reservation/confirm", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/reservation/confirm");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
