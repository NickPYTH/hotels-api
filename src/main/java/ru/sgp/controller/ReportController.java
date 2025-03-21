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
import ru.sgp.model.Filial;
import ru.sgp.model.Log;
import ru.sgp.repository.FilialRepository;
import ru.sgp.repository.LogRepository;
import ru.sgp.service.ReportService;
import ru.sgp.utils.SecurityManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/report")
public class ReportController {
    @Autowired
    LogRepository logsRepository;
    @Autowired
    FilialRepository filialRepository;
    @Autowired
    ReportService reportService;
    Logger logger = LoggerFactory.getLogger(ReportController.class);
    String loggerString = "DATE: {} | Status: {} | User: {} | PATH: {} | DURATION: {} | MESSAGE: {}";
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    public MediaType getMediaType() {
        MediaType mediaType;
        mediaType = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return mediaType;
    }
    @GetMapping(path = "/getGuestReport")
    public ResponseEntity<byte[]> getGuestReport() {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            byte[] reportData = reportService.getGuestReport();
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getGuestReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getGuestReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=GuestReport.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getGuestReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getGuestReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getMonthReportByFilial")
    public ResponseEntity<byte[]> getMonthReportByFilial(@RequestParam Long empFilialId, @RequestParam Long responsibilityId, @RequestParam Long reasonId, @RequestParam String dateStart, @RequestParam String dateFinish, @RequestParam String billing) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getMonthReportByFilial(empFilialId, responsibilityId, reasonId, dateStart, dateFinish, billing);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getMonthReportByFilial", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getMonthReportByFilial");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=ReestrByFilial.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getMonthReportByFilial", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getMonthReportByFilial");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getMonthReportByUttist")
    public ResponseEntity<byte[]> getMonthReportByUttist(@RequestParam Long empFilialId, @RequestParam Long responsibilityId, @RequestParam Long reasonId, @RequestParam String dateStart, @RequestParam String dateFinish, @RequestParam String billing, @RequestParam String ceh) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getMonthReportByUttist(empFilialId, responsibilityId, reasonId, dateStart, dateFinish, billing, ceh);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getMonthReportByUttist", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getMonthReportByUttist");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=ReestrByUttist.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getMonthReportByUttist", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getMonthReportByUttist");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getMonthReportByOrganization")
    public ResponseEntity<byte[]> getMonthReport(@RequestParam Long organizationId, @RequestParam Long responsibilityId, @RequestParam Long reasonId, @RequestParam String dateStart, @RequestParam String dateFinish, @RequestParam String billing) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getMonthReportByOrganization(organizationId, responsibilityId, reasonId, dateStart, dateFinish, billing);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getMonthReportByOrganization", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getMonthReportByOrganization");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=ReestrByOrganization.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getMonthReportByOrganization", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getMonthReportByOrganization");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getMVZReport")
    public ResponseEntity<byte[]> getMVZReport(@RequestParam Long filialId, @RequestParam String dateStart, @RequestParam String dateFinish) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            byte[] reportData = reportService.getMVZReport(filialId, dateStart, dateFinish);
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getMVZReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getMVZReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=MVZreport.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getMVZReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getMVZReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getMVZReportOnlyLPU")
    public ResponseEntity<byte[]> getMVZReportOnlyLPU(@RequestParam String lpu, @RequestParam String dateStart, @RequestParam String dateFinish) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            byte[] reportData = reportService.getMVZReportOnlyLPU(lpu, dateStart, dateFinish);
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getMVZReportOnlyLPU", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getMVZReportOnlyLPU");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=MVZReportOnlyLPU.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getMVZReportOnlyLPU", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getMVZReportOnlyLPU");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getAllContractReport")
    public ResponseEntity<byte[]> getAllReport() {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            byte[] reportData = reportService.getAllContractReport();
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getAllReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getAllReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=Contracts.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getAllReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getAllReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getFilialReport")
    public ResponseEntity<byte[]> getFilialReport(@RequestParam(name = "id") Long filialId,
                                                  @RequestParam(name = "checkouted") boolean checkouted,
                                                  @RequestParam(name = "dateStart") String dateStart,
                                                  @RequestParam(name = "dateFinish") String dateFinish) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getFilialReport(filialId, checkouted, dateStart, dateFinish);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getFilialReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getFilialReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            Filial filial = filialRepository.findById(filialId).orElse(null);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + filial.getName() + ".xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getFilialReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getFilialReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getFilialReportByFIO")
    public ResponseEntity<byte[]> getFilialReportByFIO(@RequestParam(name = "lastName") String lastName,
                                                       @RequestParam(name = "dateStart") String dateStart,
                                                       @RequestParam(name = "dateFinish") String dateFinish) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = reportService.getFilialReportByFIO(lastName, dateStart, dateFinish);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getFilialReportByFIO", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getFilialReportByFIO");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=GuestReport.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getFilialReportByFIO", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getFilialReportByFIO");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getShortReport")
    public ResponseEntity<byte[]> getShortReport(@RequestParam Long filialId) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getShortReport(filialId);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getShortReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getShortReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=ShortReport.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getShortReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getShortReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getLoadStatsReport")
    public ResponseEntity<byte[]> getLoadStatsReport(@RequestParam Long hotelId, @RequestParam String dateStart, @RequestParam String dateFinish) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getLoadStatsReport(hotelId, dateStart, dateFinish);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getLoadStatsReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getLoadStatsReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=LoadStats.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getLoadStatsReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getLoadStatsReport");
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
                                                 @RequestParam(name = "dateFinish") String dateFinish) throws ParseException, JRException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getHotelReport(hotelId, checkouted, dateStart, dateFinish);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getHotelReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getHotelReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=Report.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getHotelReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getHotelReport");
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
            byte[] reportData = reportService.getFloorReport(hotelId, floor, date);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getFloorReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getFloorReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=Report.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getFloorReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getFloorReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getMVZReportShort")
    public ResponseEntity<byte[]> getMVZReportShort(@RequestParam Long empFilialId, @RequestParam Long filialId, @RequestParam String dateStart, @RequestParam String dateFinish) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getMVZReportShort(empFilialId, filialId, dateStart, dateFinish);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getMVZReportShort", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getMVZReportShort");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=MVZreport.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getMVZReportShort", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getMVZReportShort");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping(path = "/getCheckoutReport")
    public ResponseEntity<byte[]> getCheckoutReport(@RequestParam Long id, @RequestParam Integer roomNumber, @RequestParam String periodStart, @RequestParam String periodEnd) {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getCheckoutReport(id, roomNumber, periodStart, periodEnd);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getCheckoutReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getCheckoutReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=Checkout.xlsx");
            return ResponseEntity.ok().headers(headers).contentType(getMediaType()).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getCheckoutReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getCheckoutReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
