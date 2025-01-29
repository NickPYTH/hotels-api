package ru.sgp.controller;

import net.sf.jasperreports.engine.JRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.sgp.dto.ContractDTO;
import ru.sgp.dto.OrganizationDTO;
import ru.sgp.model.Log;
import ru.sgp.repository.LogRepository;
import ru.sgp.service.ContractService;
import ru.sgp.utils.SecurityManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/contract")
public class ContractController {

    @Autowired
    private ContractService contractService;
    @Autowired
    LogRepository logsRepository;

    public MediaType getMediaType() {
        MediaType mediaType;
        mediaType = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return mediaType;
    }

    Logger logger = LoggerFactory.getLogger(ContractController.class);
    String loggerString = "DATE: {} | Status: {} | User: {} | PATH: {} | DURATION: {} | MESSAGE: {}";
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    @GetMapping(path = "/getAll")
    public ResponseEntity<List<ContractDTO>> getAll() {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            List<ContractDTO> response = contractService.getAll();
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/contract/getAll", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getAll");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/contract/getAll", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getAll");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getAllByFilialAndHotel")
    public ResponseEntity<List<ContractDTO>> getAllByFilialAndHotel(@RequestParam Long filialId, @RequestParam Long hotelId, @RequestParam Long reasonId, @RequestParam String org, @RequestParam String billing) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            List<ContractDTO> response = contractService.getAllByFilialAndHotel(filialId, hotelId, reasonId, org, billing);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/contract/getAll", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getAll");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/contract/getAll", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getAll");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/get")
    public ResponseEntity<ContractDTO> get(@RequestParam Long id) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            ContractDTO response = contractService.get(id);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/contract/get", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/get");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/contract/get", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/get");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(path = "/update")
    public ResponseEntity<ContractDTO> update(@RequestBody ContractDTO contractDTO) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            ContractDTO response = contractService.update(contractDTO);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/contract/update", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/update");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/contract/update", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/update");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(path = "/create")
    public ResponseEntity<ContractDTO> create(@RequestBody ContractDTO contractDTO) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            ContractDTO response = contractService.create(contractDTO);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/contract/create", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/create");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/contract/create", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/create");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getAllOrganizations")
    public ResponseEntity<List<OrganizationDTO>> getAllOrganizations() {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            List<OrganizationDTO> response = contractService.getAllOrganizations();
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/contract/getAllOrganizations", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getAllOrganizations");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/contract/getAllOrganizations", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getAllOrganizations");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getMonthReportByFilial")
    public ResponseEntity<byte[]> getMonthReportByFilial(@RequestParam Long empFilialId, @RequestParam Long responsibilityId, @RequestParam Long reasonId, @RequestParam String dateStart, @RequestParam String dateFinish, @RequestParam String billing) throws ParseException, JRException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = contractService.getMonthReportByFilial(empFilialId, responsibilityId, reasonId, dateStart, dateFinish, billing);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/contract/getMonthReportByFilial", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getMonthReportByFilial");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=ReestrByFilial.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/contract/getMonthReportByFilial", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getMonthReportByFilial");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getMonthReportByUttist")
    public ResponseEntity<byte[]> getMonthReportByUttist(@RequestParam Long empFilialId, @RequestParam Long responsibilityId, @RequestParam Long reasonId, @RequestParam String dateStart, @RequestParam String dateFinish, @RequestParam String billing, @RequestParam String ceh) throws ParseException, JRException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = contractService.getMonthReportByUttist(empFilialId, responsibilityId, reasonId, dateStart, dateFinish, billing, ceh);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/contract/getMonthReportByFilial", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getMonthReportByFilial");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=ReestrByFilial.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/contract/getMonthReportByFilial", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getMonthReportByFilial");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getMonthReportByOrganization")
    public ResponseEntity<byte[]> getMonthReport(@RequestParam Long organizationId, @RequestParam Long responsibilityId, @RequestParam Long reasonId, @RequestParam String dateStart, @RequestParam String dateFinish, @RequestParam String billing) throws ParseException, JRException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = contractService.getMonthReportByOrganization(organizationId, responsibilityId, reasonId, dateStart, dateFinish, billing);

        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/contract/getMonthReportByOrganization", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getMonthReportByOrganization");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=ReestrByOrganization.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/contract/getMonthReportByOrganization", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getMonthReportByOrganization");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getMVZReport")
    public ResponseEntity<byte[]> getMVZReport(@RequestParam Long filialId, @RequestParam String dateStart, @RequestParam String dateFinish) throws ParseException, JRException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = contractService.getMVZReport(filialId, dateStart, dateFinish);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;

            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/contract/getMVZReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getMVZReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=MVZreport.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/contract/getMVZReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getMVZReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getMVZReportOnlyLPU")
    public ResponseEntity<byte[]> getMVZReportOnlyLPU(@RequestParam String lpu, @RequestParam String dateStart, @RequestParam String dateFinish) throws ParseException, JRException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = contractService.getMVZReportOnlyLPU(lpu, dateStart, dateFinish);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;

            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/contract/getMVZReportOnlyLPU", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getMVZReportOnlyLPU");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=MVZReportOnlyLPU.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/contract/getMVZReportOnlyLPU", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/contract/getMVZReportOnlyLPU");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}