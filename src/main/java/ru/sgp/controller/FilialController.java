package ru.sgp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.sgp.dto.FilialDTO;
import ru.sgp.model.Log;
import ru.sgp.repository.FilialRepository;
import ru.sgp.repository.LogRepository;
import ru.sgp.service.FilialService;
import ru.sgp.utils.SecurityManager;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/filial")
public class FilialController {
    @Autowired
    LogRepository logsRepository;
    @Autowired
    FilialRepository filialRepository;
    @Autowired
    FilialService filialService;
    Logger logger = LoggerFactory.getLogger(FilialController.class);
    String loggerString = "DATE: {} | Status: {} | User: {} | PATH: {} | DURATION: {} | MESSAGE: {}";
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    @GetMapping(path = "/getAll")
    public ResponseEntity<List<FilialDTO>> getAll() throws ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            List<FilialDTO> response = filialService.getAll();
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/filial/getAll", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/filial/getAll");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/filial/getAll", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/filial/getAll");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getWithStats")
    public ResponseEntity<FilialDTO> getWithStats(@RequestParam String date, @RequestParam Long filialId) throws ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            FilialDTO response = filialService.getWithStats(date, filialId);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/filial/getWithStats", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/filial/getWithStats");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/filial/getWithStats", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/filial/getWithStats");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getAllWithStats")
    public ResponseEntity<List<FilialDTO>> getAllWithStats(@RequestParam String date) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            List<FilialDTO> response = filialService.getAllWithStats(date);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/filial/getAllWithStats", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/filial/getAllWithStats");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/filial/getAllWithStats", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/filial/getAllWithStats");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @PostMapping(path = "/load", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public List<String> dataLoad(@RequestParam("file") MultipartFile file) throws IOException {
        return filialService.dataLoad(file);
    }
    @PostMapping(path = "/loadContracts", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public List<String> loadContracts(@RequestParam("file") MultipartFile file) throws IOException {
        return filialService.dataLoadContracts(file);
    }
    @PostMapping(path = "/loadResponsibilities", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public List<String> loadResponsibilities(@RequestParam("file") MultipartFile file) throws IOException {
        return filialService.loadResponsibilities(file);
    }
    @PostMapping(path = "/loadUsers", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public List<String> loadUsers(@RequestParam("file") MultipartFile file) throws IOException {
        return filialService.loadUsers(file);
    }
}