package ru.sgp.controller;

import net.sf.jasperreports.engine.JRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.sgp.dto.GuestDTO;
import ru.sgp.dto.integration.AddGuestsForEventDTO;
import ru.sgp.model.Log;
import ru.sgp.repository.LogRepository;
import ru.sgp.service.GuestService;
import ru.sgp.service.HistoryService;
import ru.sgp.utils.SecurityManager;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/guest")
public class GuestController {
    @Autowired
    private GuestService guestService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    LogRepository logsRepository;

    Logger logger = LoggerFactory.getLogger(GuestController.class);
    String loggerString = "DATE: {} | Status: {} | User: {} | PATH: {} | DURATION: {} | MESSAGE: {}";
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    @GetMapping(path = "/getAll")
    public ResponseEntity<List<GuestDTO>> getAll() {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            List<GuestDTO> response = guestService.getAll();
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/guest/getAll", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/getAll");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/guest/getAll", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/getAll");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getAllByOrganizationId")
    public ResponseEntity<List<GuestDTO>> getAllByOrganizationId(@RequestParam Long id) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            List<GuestDTO> response = guestService.getAllByOrganizationId(id);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/guest/getAllByOrganizationId", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/getAllByOrganizationId");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/guest/getAllByOrganizationId", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/getAllByOrganizationId");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(path = "/checkout")
    public ResponseEntity<GuestDTO> checkout(@RequestParam Long id) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            GuestDTO response = guestService.checkout(id);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/guest/checkout", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/checkout");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/guest/checkout", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/checkout");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    @PostMapping(path = "/create")
    public ResponseEntity<GuestDTO> create(@RequestBody GuestDTO guestDTO) throws Exception {
        long startTime = System.nanoTime();
        Log record = new Log();
        List<GuestDTO> response = guestService.update(guestDTO);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/guest/create", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/create");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            historyService.updateGuest(record, response.get(0), response.get(1));
            return new ResponseEntity<>(response.get(1), HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/guest/create", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/create");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    @PostMapping(path = "/update")
    public ResponseEntity<GuestDTO> update(@RequestBody GuestDTO guestDTO) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            List<GuestDTO> response = guestService.update(guestDTO);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/guest/update", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/update");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            historyService.updateGuest(record, response.get(0), response.get(1));
            return new ResponseEntity<>(response.get(1), HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/guest/update", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/update");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(path = "/delete")
    public ResponseEntity<GuestDTO> delete(@RequestParam Long id) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            GuestDTO response = guestService.delete(id);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/guest/delete/" + id, duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/delete/" + id);
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/guest/delete/" + id, duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/delete/" + id);
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getFioByTabnum")
    public ResponseEntity<GuestDTO> getFioByTabnum(@RequestParam Integer tabnum) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            GuestDTO response = guestService.getFioByTabnum(tabnum);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/guest/getFioByTabnum", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/getFioByTabnum");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/guest/getFioByTabnum", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/getFioByTabnum");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getTabnumByFio")
    public ResponseEntity<GuestDTO> getTabnumByFio(@RequestParam String lastname, @RequestParam String firstname, @RequestParam String secondName) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            GuestDTO response = guestService.getTabnumByFio(lastname, firstname, secondName);
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/guest/getTabnumByFio", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/getTabnumByFio");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/guest/getTabnumByFio", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/getTabnumByFio");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getGuestsLastnames")
    public ResponseEntity<List<String>> getGuestsLastnames() {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            List<String> response = guestService.getGuestsLastnames();
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/guest/getGuestsLastnames", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/getGuestsLastnames");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/guest/getGuestsLastnames", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guest/getGuestsLastnames");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    public MediaType getMediaType() {
        MediaType mediaType;
        mediaType = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return mediaType;
    }

    @GetMapping(path = "/getGuestReport")
    public ResponseEntity<byte[]> getGuestReport() throws JRException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            byte[] reportData = guestService.getGuestReport();
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/guests/getGuestReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guests/getGuestReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=GuestReport.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/guests/getGuestReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guests/getGuestReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(path = "/integration/addGuestsForEvent")
    public ResponseEntity<List<GuestDTO>> addGuestsForEvent(@RequestBody AddGuestsForEventDTO body) throws Exception {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            List<GuestDTO> response = guestService.addGuestsForEvent(body);
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/guests/integration/addGuestsForEvent", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guests/integration/addGuestsForEvent");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/guests/integration/addGuestsForEvent", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/guests/integration/addGuestsForEvent");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(path = "/integration/loadGuestsFrom1C", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public List<String> loadGuestsFrom1C(@RequestParam("file") MultipartFile file) throws IOException, ParseException {
        return guestService.loadGuestsFrom1C(file);
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @PostMapping(path = "/manyGuestUpload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public List<GuestDTO> manyGuestUpload(@RequestParam Boolean mode, @RequestParam("file") MultipartFile file) throws IOException, ParseException {
        return guestService.manyGuestUpload(file, mode);
    }

}