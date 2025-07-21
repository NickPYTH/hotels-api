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
import ru.sgp.model.Log;
import ru.sgp.repository.LogRepository;
import ru.sgp.repository.ReportNamesRepository;
import ru.sgp.service.ReportService;
import ru.sgp.utils.SecurityManager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/report")
public class ReportController {
    @Autowired
    LogRepository logsRepository;
    @Autowired
    ReportNamesRepository reportNamesRepository;
    @Autowired
    ReportService reportService;
    Logger logger = LoggerFactory.getLogger(ReportController.class);
    String loggerString = "DATE: {} | Status: {} | User: {} | PATH: {} | DURATION: {} | MESSAGE: {}";
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    public static MediaType getMediaType(String fileType) {
        String type = fileType.toLowerCase(Locale.ROOT);
        if (type.equals("xlsx") || type.equals("xls"))
            return new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        if (type.equals("docx") || type.equals("doc"))
            return new MediaType("application", "vnd.openxmlformats-officedocument.wordprocessingml.document");
        if (type.equals("rar"))
            return new MediaType("application", "vnd.rar");
        if (type.equals("pdf"))
            return MediaType.APPLICATION_PDF;
        if (type.equals("jpeg") || type.equals("jpg"))
            return new MediaType("image", "jpeg");
        return new MediaType("application", type);
    }

    public String getContentDisposition(String fileName) {
        // закодируем имя файла для корректного отображения русских буков
        fileName = URLEncoder
                .encode(fileName, StandardCharsets.UTF_8)
                // URLEncoder.encode согласно документации преобразует пробелы в "+"
                .replace("+", "%20");
        //return "attachment; filename=\"" + fileName + '\"';
        return "inline; filename=\"" + fileName + ".xlsx" + '\"';
    }

    @GetMapping(path = "/getGuestReport")
    public ResponseEntity<byte[]> getGuestReport() throws JRException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getGuestReport();
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getGuestReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getGuestReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("Guests").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
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

    @GetMapping(path = "/getReestrRabotReport")
    public ResponseEntity<byte[]> getReestrRabotReport(@RequestParam List<Long> reasonList, @RequestParam Long hotelId, @RequestParam Long responsibilityId, @RequestParam String dateStart, @RequestParam String dateFinish, @RequestParam String ukgBoss, @RequestParam String workType, @RequestParam Boolean isOrganization) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getReestrRabotReport(hotelId, responsibilityId, dateStart, dateFinish, ukgBoss, workType, isOrganization, reasonList);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getReestrRabotReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getReestrRabotReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("ReestrRabot").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getReestrRabotReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getReestrRabotReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getReestrRabotErmakReport")
    public ResponseEntity<byte[]> getReestrRabotErmakReport(@RequestParam List<Long> reasonList, @RequestParam Long hotelId, @RequestParam Long responsibilityId, @RequestParam String dateStart, @RequestParam String dateFinish, @RequestParam String ukgBoss, @RequestParam String workType, @RequestParam Boolean isOrganization) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getReestrRabotErmakReport(hotelId, responsibilityId, dateStart, dateFinish, ukgBoss, workType, isOrganization, reasonList);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getReestrRabotErmakReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getReestrRabotErmakReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("ReestrRabot").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getReestrRabotErmakReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getReestrRabotErmakReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping(path = "/getMonthReportByFilial")
    public ResponseEntity<byte[]> getMonthReportByFilial(@RequestParam Long empFilialId, @RequestParam Long responsibilityId, @RequestParam Long reasonId, @RequestParam String dateStart, @RequestParam String dateFinish, @RequestParam String billing) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = reportService.getMonthReportByFilial(empFilialId, responsibilityId, reasonId, dateStart, dateFinish, billing);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getMonthReportByFilial", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getMonthReportByFilial");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("Reestr").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
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
            String reportName = reportNamesRepository.findByEnName("ReestrUtt").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName + "_" + ceh));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
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
    public ResponseEntity<byte[]> getMonthReport(@RequestParam Long organizationId, @RequestParam Long responsibilityId, @RequestParam Long reasonId, @RequestParam String dateStart, @RequestParam String dateFinish, @RequestParam String billing) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = reportService.getMonthReportByOrganization(organizationId, responsibilityId, reasonId, dateStart, dateFinish, billing);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getMonthReportByOrganization", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getMonthReportByOrganization");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("ReestrOrg").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
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
            String reportName = reportNamesRepository.findByEnName("Mvz").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
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
            String reportName = reportNamesRepository.findByEnName("Mvz").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
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
            String reportName = reportNamesRepository.findByEnName("Contracts").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
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
                                                  @RequestParam(name = "dateFinish") String dateFinish,
                                                  @RequestParam(name = "fullReport") Boolean fullReport) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = reportService.getFilialReport(filialId, checkouted, dateStart, dateFinish, fullReport);

        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getFilialReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getFilialReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("Filial").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
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
        try {
            byte[] reportData = reportService.getFilialReportByFIO(lastName, dateStart, dateFinish);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getFilialReportByFIO", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getFilialReportByFIO");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("Filial").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
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
            String reportName = reportNamesRepository.findByEnName("ShortFilial").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
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
            String reportName = reportNamesRepository.findByEnName("LoadStats").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
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
            String reportName = reportNamesRepository.findByEnName("Hotel").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
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
            String reportName = reportNamesRepository.findByEnName("Floor").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
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
            String reportName = reportNamesRepository.findByEnName("Mvz").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
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
    public ResponseEntity<byte[]> getCheckoutReport(@RequestParam Long id, @RequestParam String periodStart, @RequestParam String periodEnd, @RequestParam String format) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = reportService.getCheckoutReport(id, periodStart, periodEnd, format);

        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getCheckoutReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getCheckoutReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("Checkout").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType(format)).body(reportData);
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

    @GetMapping(path = "/getPlanZaezdReport")
    public ResponseEntity<byte[]> getPlanZaezdReport(@RequestParam Long hotelId, @RequestParam String date) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getPlanZaezdReport(hotelId, date);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getPlanZaezdReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getPlanZaezdReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("PlanZaezd").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getPlanZaezdReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getPlanZaezdReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getWeekReport")
    public ResponseEntity<byte[]> getWeekReport(@RequestParam Long hotelId, @RequestParam String dateStart, @RequestParam String dateFinish) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getWeekReport(hotelId, dateStart, dateFinish);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getWeekReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getWeekReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("WeekReport").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getWeekReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getWeekReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getExtrasReport")
    public ResponseEntity<byte[]> getExtrasReport(@RequestParam Long hotelId, @RequestParam String dateStart, @RequestParam String dateFinish) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        try {
            byte[] reportData = reportService.getExtrasReport(hotelId, dateStart, dateFinish);
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getExtrasReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getExtrasReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("ExtrasReport").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getExtrasReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getExtrasReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getAvdReport")
    public ResponseEntity<byte[]> getAvdReport(@RequestParam Long hotelId, @RequestParam String dateStart, @RequestParam String dateFinish, @RequestParam Long responsibilityId) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = reportService.getAvdReport(hotelId, dateStart, dateFinish, responsibilityId);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getAvdReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getAvdReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("AVD").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getAvdReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getAvdReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getReservationConfirmReport")
    public ResponseEntity<byte[]> getReservationConfirmReport(@RequestParam Long filialEmployeeId, @RequestParam Long hotelId, @RequestParam String dateStart, @RequestParam String dateFinish, @RequestParam String format) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = reportService.getReservationConfirmReport(filialEmployeeId, hotelId, dateStart, dateFinish, format);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getReservationConfirmReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getReservationConfirmReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("AVD").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType(format)).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getReservationConfirmReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getReservationConfirmReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getReservationConfirmReportSingle")
    public ResponseEntity<byte[]> getReservationConfirmReportSingle(@RequestParam Long reservationId, @RequestParam String format) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = reportService.getReservationConfirmReport(reservationId, format);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getReservationConfirmReportSingle", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getReservationConfirmReportSingle");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("AVD").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType(format)).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getReservationConfirmReportSingle", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getReservationConfirmReportSingle");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getYearReservationReport")
    public ResponseEntity<byte[]> getYearReservationReport(@RequestParam Long hotelId, @RequestParam Integer year, @RequestParam String format) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = reportService.getYearReservationReport(hotelId, year, format);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getYearReservationReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getYearReservationReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("YEAR_RESERVATION").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType(format)).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getYearReservationReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getYearReservationReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/getBookReport")
    public ResponseEntity<byte[]> getBookReport(@RequestParam Long bookReportId) throws JRException, ParseException {
        long startTime = System.nanoTime();
        Log record = new Log();
        byte[] reportData = reportService.bookReport(bookReportId);
        try {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "OK", SecurityManager.getCurrentUser(), "/report/getBookReport", duration, "");
            record.setStatus("OK");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getBookReport");
            record.setDuration(duration);
            record.setDate(new Date());
            logsRepository.save(record);
            HttpHeaders headers = new HttpHeaders();
            String reportName = reportNamesRepository.findByEnName("YEAR_RESERVATION").getRuName();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(reportName));
            return ResponseEntity.ok().headers(headers).contentType(getMediaType("xlsx")).body(reportData);
        } catch (Exception e) {
            Double duration = (System.nanoTime() - startTime) / 1E9;
            logger.info(loggerString, dateTimeFormatter.format(new Date()), "ERROR", SecurityManager.getCurrentUser(), "/report/getBookReport", duration, e.getMessage());
            record.setStatus("ERROR");
            record.setUser(SecurityManager.getCurrentUser());
            record.setPath("/report/getBookReport");
            record.setDuration(duration);
            record.setMessage(e.getMessage());
            record.setDate(new Date());
            logsRepository.save(record);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }


}
