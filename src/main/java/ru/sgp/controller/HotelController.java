package ru.sgp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.sgp.dto.HotelDTO;
import ru.sgp.dto.report.HotelsStatsReportDTO;
import ru.sgp.model.Hotel;
import ru.sgp.model.Log;
import ru.sgp.repository.HotelRepository;
import ru.sgp.repository.LogRepository;
import ru.sgp.service.HotelService;
import ru.sgp.utils.SecurityManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/hotel")
public class HotelController {
    @Autowired
    private HotelService hotelService;

    @Autowired
    LogRepository logsRepository;

    Logger logger = LoggerFactory.getLogger(HotelController.class);
    String loggerString = "DATE: {} | Status: {} | User: {} | PATH: {} | DURATION: {} | MESSAGE: {}";
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    public MediaType getMediaType() {
        MediaType mediaType;
        mediaType = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return mediaType;
    }

    @Autowired
    private HotelRepository hotelRepository;

    @GetMapping(path = "/getHotelsStats")
    public ResponseEntity<List<HotelsStatsReportDTO>> getHotelsStats(@RequestParam Long idFilial, @RequestParam String dateStart, @RequestParam String dateFinish) throws ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        List<HotelsStatsReportDTO> response = hotelService.getHotelsStats(idFilial, dateStart, dateFinish);

        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/hotel/getHotelStats", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/hotel/getHotelStats");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/hotel/getHotelStats", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/hotel/getHotelStats");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getAllByFilialId")
    public ResponseEntity<List<HotelDTO>> getAllByFilialId(@RequestParam Long id, @RequestParam String date) throws ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        List<HotelDTO> response = hotelService.getAll(id, date);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/hotel/getAllByFilialId", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/hotel/getAllByFilialId");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/hotel/getAllByFilialId", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/hotel/getAllByFilialId");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getAllByCommendant")
    public ResponseEntity<List<HotelDTO>> getAllByCommendant(@RequestParam String date) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            List<HotelDTO> response = hotelService.getAllByCommendant(date);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/hotel/getAllByCommendant", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/hotel/getAllByFilialId");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/hotel/getAllByCommendant", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/hotel/getAllByFilialId");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getHotelReport")
    public ResponseEntity<byte[]> getHotelReport(@RequestParam(name = "id") Long hotelId, @RequestParam(name = "checkouted") boolean checkouted,
                                                 @RequestParam(name = "dateStart") String dateStart,
                                                 @RequestParam(name = "dateFinish") String dateFinish) throws ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = hotelService.getHotelReport(hotelId, checkouted, dateStart, dateFinish);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/hotel/getHotelReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/hotel/getHotelReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + hotel.getName() + ".xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/hotel/getHotelReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/hotel/getHotelReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getFloorReport")
    public ResponseEntity<byte[]> getHotelReport(@RequestParam(name = "hotelId") Long hotelId, @RequestParam(name = "floor") Integer floor, @RequestParam String date) throws ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = hotelService.getFloorReport(hotelId, floor, date);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/hotel/getFloorReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/hotel/getFloorReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + hotel.getName() + ".xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/hotel/getFloorReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/hotel/getFloorReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}