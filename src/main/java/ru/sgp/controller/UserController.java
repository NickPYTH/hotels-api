package ru.sgp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.sgp.dto.UserDTO;
import ru.sgp.model.Log;
import ru.sgp.repository.LogRepository;
import ru.sgp.service.UserService;
import ru.sgp.utils.SecurityManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    LogRepository logsRepository;
    Logger logger = LoggerFactory.getLogger(UserController.class);
    String loggerString = "DATE: {} | Status: {} | User: {} | PATH: {} | DURATION: {} | MESSAGE: {}";
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    @PostMapping(path = "/create")
    public ResponseEntity<UserDTO> create(@RequestBody UserDTO userDTO) throws ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            UserDTO response = userService.create(userDTO);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", ru.sgp.utils.SecurityManager.getCurrentUser(), "/user/create", duration, "");
            record.setStatus("OK");
            record.setUser(ru.sgp.utils.SecurityManager.getCurrentUser());
            record.setPath("/user/create");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", ru.sgp.utils.SecurityManager.getCurrentUser(), "/user/create", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/user/create");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @PostMapping(path = "/update")
    public ResponseEntity<UserDTO> update(@RequestBody UserDTO userDTO) throws ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            UserDTO response = userService.update(userDTO);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", ru.sgp.utils.SecurityManager.getCurrentUser(), "/user/update", duration, "");
            record.setStatus("OK");
            record.setUser(ru.sgp.utils.SecurityManager.getCurrentUser());
            record.setPath("/user/update");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", ru.sgp.utils.SecurityManager.getCurrentUser(), "/user/update", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/user/update");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @PostMapping(path = "/updateRole")
    public ResponseEntity<UserDTO> update(@RequestParam Long roleId) throws ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            UserDTO response = userService.updateRole(roleId);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", ru.sgp.utils.SecurityManager.getCurrentUser(), "/user/update", duration, "");
            record.setStatus("OK");
            record.setUser(ru.sgp.utils.SecurityManager.getCurrentUser());
            record.setPath("/user/update");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", ru.sgp.utils.SecurityManager.getCurrentUser(), "/user/update", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/user/update");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getCurrent")
    public ResponseEntity<UserDTO> get() {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            UserDTO response = userService.getCurrent();
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", ru.sgp.utils.SecurityManager.getCurrentUser(), "/user/getCurrent", duration, "");
            record.setStatus("OK");
            record.setUser(ru.sgp.utils.SecurityManager.getCurrentUser());
            record.setPath("/user/getCurrent");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", ru.sgp.utils.SecurityManager.getCurrentUser(), "/user/getCurrent", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/user/getCurrent");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getAll")
    public ResponseEntity<List<UserDTO>> getAll() {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            List<UserDTO> response = userService.getAll();
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", ru.sgp.utils.SecurityManager.getCurrentUser(), "/user/getAll", duration, "");
            record.setStatus("OK");
            record.setUser(ru.sgp.utils.SecurityManager.getCurrentUser());
            record.setPath("/user/getAll");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", ru.sgp.utils.SecurityManager.getCurrentUser(), "/user/getAll", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/user/getAll");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}