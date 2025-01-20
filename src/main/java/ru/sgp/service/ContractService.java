package ru.sgp.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.ContractDTO;
import ru.sgp.dto.OrganizationDTO;
import ru.sgp.dto.report.MVZReportDTO;
import ru.sgp.dto.report.MonthReportDTO;
import ru.sgp.model.*;
import ru.sgp.repository.*;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ContractService {
    @Autowired
    ContractRepository contractRepository;
    @Autowired
    OrganizationRepository organizationRepository;
    @Autowired
    FilialRepository filialRepository;
    @Autowired
    HotelRepository hotelRepository;
    @Autowired
    private GuestRepository guestRepository;
    @Autowired
    private ReasonRepository reasonRepository;
    @Autowired
    private ResponsibilitiesRepository responsibilitiesRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private MVZRepository mvzRepository;

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat dateFormatterDot = new SimpleDateFormat("dd.MM.yyyy");
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    public byte[] export(JasperPrint jasperPrint) throws JRException {
        Exporter exporter;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exporter = new JRXlsxExporter();
        //exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        exporter.exportReport();
        return outputStream.toByteArray();
    }

    public List<ContractDTO> getAll() {
        List<ContractDTO> response = new ArrayList<>();
        for (Contract contract : contractRepository.findAll()) {
            ContractDTO contractDTO = new ContractDTO();
            contractDTO.setId(contract.getId());
            contractDTO.setFilial(contract.getFilial().getName());
            contractDTO.setFilialId(contract.getFilial().getId());
            if (contract.getHotel() != null) {
                contractDTO.setHotel(contract.getHotel().getName());
                contractDTO.setHotelId(contract.getHotel().getId());
            }
            if (contract.getOrganization() != null) {
                contractDTO.setOrganization(contract.getOrganization().getName());
                contractDTO.setOrganizationId(contract.getOrganization().getId());
            }
            contractDTO.setDocnum(contract.getDocnum());

            contractDTO.setCost(contract.getCost());
            contractDTO.setReasonId(contract.getReason().getId());
            contractDTO.setNote(contract.getNote());
            contractDTO.setOsnovanie(contract.getReason().getName());
            contractDTO.setBilling(contract.getBilling());
            contractDTO.setYear(contract.getYear());
            contractDTO.setRoomNumber(contract.getRoomNumber());
            response.add(contractDTO);
        }
        return response;
    }

    public ContractDTO get(Long id) {
        Contract contract = contractRepository.getById(id);
        ContractDTO contractDTO = new ContractDTO();
        contractDTO.setId(contract.getId());
        contractDTO.setFilial(contract.getFilial().getName());
        contractDTO.setFilialId(contract.getFilial().getId());
        if (contract.getHotel() != null) {
            contractDTO.setHotel(contract.getHotel().getName());
            contractDTO.setHotelId(contract.getHotel().getId());
        }
        contractDTO.setOrganization(contract.getOrganization().getName());
        contractDTO.setOrganizationId(contract.getOrganization().getId());
        contractDTO.setDocnum(contract.getDocnum());
        contractDTO.setCost(contract.getCost());
        contractDTO.setReasonId(contract.getReason().getId());
        contractDTO.setNote(contract.getNote());
        return contractDTO;
    }

    @Transactional
    public ContractDTO update(ContractDTO contractDTO) {
        Contract contract = contractRepository.getById(contractDTO.getId());
        Filial filial = filialRepository.getById(contractDTO.getFilialId());
        Hotel hotel = hotelRepository.getById(contractDTO.getHotelId());
        Reason reason = reasonRepository.getById(Long.parseLong(contractDTO.getOsnovanie()));
        Organization organization = organizationRepository.getById(contractDTO.getOrganizationId());
        contract.setBilling(contractDTO.getBilling());
        contract.setReason(reason);
        contract.setFilial(filial);
        contract.setHotel(hotel);
        contract.setOrganization(organization);
        contract.setDocnum(contractDTO.getDocnum());
        contract.setCost(contractDTO.getCost());
        contract.setNote(contractDTO.getNote());
        contract.setYear(contractDTO.getYear());
        contractRepository.save(contract);
        return contractDTO;
    }

    @Transactional
    public ContractDTO create(ContractDTO contractDTO) {
        Contract contract = new Contract();
        Filial filial = filialRepository.getById(contractDTO.getFilialId());
        Hotel hotel = hotelRepository.getById(contractDTO.getHotelId());
        Organization organization = organizationRepository.getById(contractDTO.getOrganizationId());
        Reason reason = reasonRepository.getById(Long.parseLong(contractDTO.getOsnovanie()));
        contract.setBilling(contractDTO.getBilling());
        contract.setReason(reason);
        contract.setFilial(filial);
        contract.setHotel(hotel);
        contract.setOrganization(organization);
        contract.setDocnum(contractDTO.getDocnum());
        contract.setCost(contractDTO.getCost());
        contract.setNote(contractDTO.getNote());
        contractRepository.save(contract);
        contractDTO.setId(contract.getId());
        return contractDTO;
    }

    public List<OrganizationDTO> getAllOrganizations() {
        List<OrganizationDTO> response = new ArrayList<>();
        for (Organization organization : organizationRepository.findAll()) {
            OrganizationDTO organizationDTO = new OrganizationDTO();
            organizationDTO.setId(organization.getId());
            organizationDTO.setName(organization.getName());
            response.add(organizationDTO);
        }
        return response;
    }

    @Transactional
    public byte[] getMonthReportByFilial(Long empFilialId, Long responsibilityId, Long reasonId, String dateStart, String dateFinish, String billing) throws JRException, ParseException {
        List<MonthReportDTO> reportData = new ArrayList<>();
        Filial empFilial = filialRepository.findById(empFilialId).orElse(null);
        Reason reason = reasonRepository.getById(reasonId);
        Responsibilities responsibilities = responsibilitiesRepository.getById(responsibilityId);
        Filial filial = responsibilities.getHotel().getFilial();
        DecimalFormat df = new DecimalFormat("#.##");
        Long daysCountSummary = 0L;
        Double costSummary = 0.0;
        Date minDate = dateTimeFormatter.parse(dateStart + " 23:59");
        Date maxDate = dateTimeFormatter.parse(dateFinish + " 23:59");
        int count = 1;
        for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfter(maxDate, minDate)) {
            Filial guestFilial = null;
            if (guest.getEmployee() != null) {
                guestFilial = filialRepository.findByCode(guest.getEmployee().getIdFilial());
            }
            Hotel guestHotel = guest.getRoom().getFlat().getHotel();
            if (empFilial != null) {
                if (guestFilial != null) {
                    if (guestFilial.getId() != empFilial.getId()) continue;
                } else continue;
            } else {
                if (guest.getEmployee() != null) continue; // Если работник то скипаем это только для сторонников
            }
            if (!guest.getBilling().equals(billing)) continue;
            if (guest.getReason().getId() != reason.getId()) continue;
            if (responsibilities.getHotel() != guestHotel) continue;

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(minDate);
            List<Contract> contracts = contractRepository.findAllByFilialAndHotelAndReasonAndYear(filial, guest.getRoom().getFlat().getHotel(), guest.getReason(), calendar.get(Calendar.YEAR));
            MonthReportDTO monthReportDTO = new MonthReportDTO();
            monthReportDTO.setId(String.valueOf(count));
            monthReportDTO.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
            if (guest.getDateStart().compareTo(minDate) > 0)
                monthReportDTO.setDateStart(dateFormatter.format(guest.getDateStart()));
            else
                monthReportDTO.setDateStart(dateFormatter.format(minDate));
            if (guest.getDateFinish().compareTo(maxDate) < 0)
                monthReportDTO.setDateFinish(dateFormatter.format(guest.getDateFinish()));
            else
                monthReportDTO.setDateFinish(dateFormatter.format(maxDate));
            Long daysCount = 1L;
            Date cuttedGuestStartDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateStart()));
            Date cuttedGuestFinishDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateFinish()));
            Date cuttedPeriodStartDate = dateFormatter.parse(dateTimeFormatter.format(minDate.getTime()));
            Date cuttedPeriodFinishDate = dateFormatter.parse(dateTimeFormatter.format(maxDate.getTime()));
            if (guest.getDateStart().before(minDate) && guest.getDateFinish().after(maxDate)) {  // Все дни заданного периода
                daysCount = TimeUnit.DAYS.convert(cuttedPeriodFinishDate.getTime() - cuttedPeriodStartDate.getTime(), TimeUnit.MILLISECONDS) + 1;
            } else if (guest.getDateStart().after(minDate) && guest.getDateFinish().before(maxDate)) {  // Все дни внутри периода
                daysCount = TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS);
            } else if (guest.getDateStart().before(minDate) && guest.getDateFinish().before(maxDate)) { // Если Дата начала не входит в период то сичтает с начала периода по дату выезда
                daysCount = TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedPeriodStartDate.getTime(), TimeUnit.MILLISECONDS);
            } else if (guest.getDateStart().after(minDate) && guest.getDateFinish().after(maxDate)) { // Если Дата выселения не входит в период то сичтает с заселения по конца периода
                daysCount = TimeUnit.DAYS.convert(cuttedPeriodFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS) + 1;
            } else {
                continue;
            }
            if (daysCount == 0) daysCount = 1L;
            daysCountSummary += daysCount;
            monthReportDTO.setDaysCount(Math.toIntExact(daysCount));
            if (!contracts.isEmpty()) {
                Float cost = Float.valueOf(df.format(contracts.get(0).getCost()).replace(',', '.'));
                monthReportDTO.setCostFromContract(cost);
                monthReportDTO.setCost(daysCount * cost);
                costSummary += daysCount * cost;
            }
            if (guest.getEmployee() != null) {
                monthReportDTO.setTabnum(guest.getEmployee().getTabnum().toString());
            } else {
                monthReportDTO.setTabnum("   ");
            }
            monthReportDTO.setMemo(guest.getMemo());
            reportData.add(monthReportDTO);
            count++;
        }
        reportData.sort((rec1, rec2) -> {
            return rec1.getDateStart().compareTo(rec2.getDateStart());
        });
        int i = 1;
        for (MonthReportDTO monthReportDTO : reportData) {
            monthReportDTO.setId(String.valueOf(i));
            i++;
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/MonthReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("periodStart", dateStart);
        parameters.put("periodFinish", dateFinish);
        parameters.put("filial", filial.getName());
        parameters.put("hotelLocation", responsibilities.getHotel().getName() + ", " + responsibilities.getHotel().getLocation());
        parameters.put("daysCountSummary", daysCountSummary.toString());
        parameters.put("costSummary", String.format("%.2f", costSummary));
        String respName = responsibilities.getEmployee().getFirstname().charAt(0) + ". " + responsibilities.getEmployee().getSecondName().charAt(0) + ". " + responsibilities.getEmployee().getLastname();
        String respPost = postRepository.getById(responsibilities.getEmployee().getIdPoststaff().longValue()).getName();
        parameters.put("respPost", respPost);
        parameters.put("respName", respName);
        if (empFilial != null) {
            parameters.put("filialFrom", empFilial.getName());
            String bossF = empFilial.getBoss().split(" ")[0];
            String bossN = empFilial.getBoss().split(" ")[1];
            String bossS = empFilial.getBoss().split(" ")[2];
            String bossPost = empFilial.getBoss().split(" ")[3] + " " + empFilial.getBoss().split(" ")[4];
            parameters.put("filialBossPost", bossPost);
            parameters.put("filialBossName", bossF + " " + bossN.charAt(0) + ". " + bossS.charAt(0) + ".");
        } else {
            parameters.put("filialBossPost", "");
            parameters.put("filialBossName", "");
        }
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }

    @Transactional
    public byte[] getMonthReportByUttist(Long empFilialId, Long responsibilityId, Long reasonId, String dateStart, String dateFinish, String billing, String ceh) throws JRException, ParseException {
        List<MonthReportDTO> reportData = new ArrayList<>();
        Filial empFilial = filialRepository.findById(empFilialId).orElse(null);
        Reason reason = reasonRepository.getById(reasonId);
        Responsibilities responsibilities = responsibilitiesRepository.getById(responsibilityId);
        Filial filial = responsibilities.getHotel().getFilial();
        DecimalFormat df = new DecimalFormat("#.##");
        Long daysCountSummary = 0L;
        Double costSummary = 0.0;
        Date minDate = dateTimeFormatter.parse(dateStart + " 23:59");
        Date maxDate = dateTimeFormatter.parse(dateFinish + " 23:59");
        int count = 1;
        for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfter(maxDate, minDate)) {
            Filial guestFilial = null;
            if (guest.getEmployee() != null) {
                guestFilial = filialRepository.findByCode(guest.getEmployee().getIdFilial());
            }
            Hotel guestHotel = guest.getRoom().getFlat().getHotel();
            if (empFilial != null) {
                if (guestFilial != null) {
                    if (guestFilial.getId() != empFilial.getId()) continue;
                } else continue;
            } else {
                if (guest.getEmployee() != null) continue; // Если работник то скипаем это только для сторонников
            }
            if (!guest.getBilling().equals(billing)) continue;
            if (guest.getReason().getId() != reason.getId()) continue;
            if (responsibilities.getHotel() != guestHotel) continue;
            if (guest.getEmployee() == null) continue;
            if (mvzRepository.findByIdAndNameIsContainingIgnoreCase(guest.getEmployee().getMvzId(), ceh).isEmpty())  // Проверка цеха
                continue;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(minDate);
            List<Contract> contracts = contractRepository.findAllByFilialAndHotelAndReasonAndYear(filial, guest.getRoom().getFlat().getHotel(), guest.getReason(), calendar.get(Calendar.YEAR));
            MonthReportDTO monthReportDTO = new MonthReportDTO();
            monthReportDTO.setId(String.valueOf(count));
            monthReportDTO.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
            if (guest.getDateStart().compareTo(minDate) > 0)
                monthReportDTO.setDateStart(dateFormatter.format(guest.getDateStart()));
            else
                monthReportDTO.setDateStart(dateFormatter.format(minDate));
            if (guest.getDateFinish().compareTo(maxDate) < 0)
                monthReportDTO.setDateFinish(dateFormatter.format(guest.getDateFinish()));
            else
                monthReportDTO.setDateFinish(dateFormatter.format(maxDate));
            Long daysCount = 1L;
            Date cuttedGuestStartDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateStart()));
            Date cuttedGuestFinishDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateFinish()));
            Date cuttedPeriodStartDate = dateFormatter.parse(dateTimeFormatter.format(minDate.getTime()));
            Date cuttedPeriodFinishDate = dateFormatter.parse(dateTimeFormatter.format(maxDate.getTime()));
            if (guest.getDateStart().before(minDate) && guest.getDateFinish().after(maxDate)) {  // Все дни заданного периода
                daysCount = TimeUnit.DAYS.convert(cuttedPeriodFinishDate.getTime() - cuttedPeriodStartDate.getTime(), TimeUnit.MILLISECONDS) + 1;
            } else if (guest.getDateStart().after(minDate) && guest.getDateFinish().before(maxDate)) {  // Все дни внутри периода
                daysCount = TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS);
            } else if (guest.getDateStart().before(minDate) && guest.getDateFinish().before(maxDate)) { // Если Дата начала не входит в период то сичтает с начала периода по дату выезда
                daysCount = TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedPeriodStartDate.getTime(), TimeUnit.MILLISECONDS);
            } else if (guest.getDateStart().after(minDate) && guest.getDateFinish().after(maxDate)) { // Если Дата выселения не входит в период то сичтает с заселения по конца периода
                daysCount = TimeUnit.DAYS.convert(cuttedPeriodFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS) + 1;
            } else {
                continue;
            }
            if (daysCount == 0) daysCount = 1L;
            daysCountSummary += daysCount;
            monthReportDTO.setDaysCount(Math.toIntExact(daysCount));
            if (!contracts.isEmpty()) {
                Float cost = Float.valueOf(df.format(contracts.get(0).getCost()).replace(',', '.'));
                monthReportDTO.setCostFromContract(cost);
                monthReportDTO.setCost(daysCount * cost);
                costSummary += daysCount * cost;
            }
            if (guest.getEmployee() != null) {
                monthReportDTO.setTabnum(guest.getEmployee().getTabnum().toString());
            } else {
                monthReportDTO.setTabnum("   ");
            }
            monthReportDTO.setMemo(guest.getMemo());
            reportData.add(monthReportDTO);
            count++;
        }
        reportData.sort((rec1, rec2) -> {
            return rec1.getDateStart().compareTo(rec2.getDateStart());
        });
        int i = 1;
        for (MonthReportDTO monthReportDTO : reportData) {
            monthReportDTO.setId(String.valueOf(i));
            i++;
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/MonthReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("periodStart", dateStart);
        parameters.put("periodFinish", dateFinish);
        parameters.put("filial", filial.getName());
        parameters.put("hotelLocation", responsibilities.getHotel().getName() + ", " + responsibilities.getHotel().getLocation());
        parameters.put("daysCountSummary", daysCountSummary.toString());
        parameters.put("costSummary", String.format("%.2f", costSummary));
        String respName = responsibilities.getEmployee().getFirstname().charAt(0) + ". " + responsibilities.getEmployee().getSecondName().charAt(0) + ". " + responsibilities.getEmployee().getLastname();
        String respPost = postRepository.getById(responsibilities.getEmployee().getIdPoststaff().longValue()).getName();
        parameters.put("respPost", respPost);
        parameters.put("respName", respName);
        if (empFilial != null) {
            parameters.put("filialFrom", empFilial.getName());
            String bossF = empFilial.getBoss().split(" ")[0];
            String bossN = empFilial.getBoss().split(" ")[1];
            String bossS = empFilial.getBoss().split(" ")[2];
            String bossPost = empFilial.getBoss().split(" ")[3] + " " + empFilial.getBoss().split(" ")[4];
            parameters.put("filialBossPost", bossPost);
            parameters.put("filialBossName", bossF + " " + bossN.charAt(0) + ". " + bossS.charAt(0) + ".");
        } else {
            parameters.put("filialBossPost", "");
            parameters.put("filialBossName", "");
        }
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }

    @Transactional
    public byte[] getMonthReportByOrganization(Long orgId, Long responsibilityId, Long reasonId, String dateStart, String dateFinish, String billing) throws JRException, ParseException {
        List<MonthReportDTO> reportData = new ArrayList<>();
        Reason reason = reasonRepository.getById(reasonId);
        Organization organization = organizationRepository.getById(orgId);
        Responsibilities responsibilities = responsibilitiesRepository.getById(responsibilityId);
        Filial filial = responsibilities.getHotel().getFilial();
        DecimalFormat df = new DecimalFormat("#.##");
        Long daysCountSummary = 0L;
        Double costSummary = 0.0;
        Date minDate = dateTimeFormatter.parse(dateStart + " 23:59");
        Date maxDate = dateTimeFormatter.parse(dateFinish + " 23:59");
        int count = 1;
        for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfter(maxDate, minDate)) {
            if (guest.getEmployee() != null) continue;
            if (guest.getOrganization() != organization) continue;
            if (guest.getReason().getId() != reason.getId()) continue;
            if (responsibilities.getHotel() != guest.getRoom().getFlat().getHotel()) continue;
            if (!guest.getBilling().equals(billing)) continue;
            List<Contract> contracts = contractRepository.findAllByFilialAndHotelAndReasonAndOrganization(filial, guest.getRoom().getFlat().getHotel(), guest.getReason(), guest.getOrganization());
            MonthReportDTO monthReportDTO = new MonthReportDTO();
            monthReportDTO.setId(String.valueOf(count));
            monthReportDTO.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
            if (guest.getDateStart().compareTo(minDate) > 0)
                monthReportDTO.setDateStart(dateFormatter.format(guest.getDateStart()));
            else
                monthReportDTO.setDateStart(dateFormatter.format(minDate));
            if (guest.getDateFinish().compareTo(maxDate) < 0)
                monthReportDTO.setDateFinish(dateFormatter.format(guest.getDateFinish()));
            else
                monthReportDTO.setDateFinish(dateFormatter.format(maxDate));
            Long daysCount = 1L;
            Date cuttedGuestStartDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateStart()));
            Date cuttedGuestFinishDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateFinish()));
            Date cuttedPeriodStartDate = dateFormatter.parse(dateTimeFormatter.format(minDate.getTime()));
            Date cuttedPeriodFinishDate = dateFormatter.parse(dateTimeFormatter.format(maxDate.getTime()));
            if (guest.getDateStart().before(minDate) && guest.getDateFinish().after(maxDate)) {  // Все дни заданного периода
                daysCount = TimeUnit.DAYS.convert(cuttedPeriodFinishDate.getTime() - cuttedPeriodStartDate.getTime(), TimeUnit.MILLISECONDS) + 1;
            } else if (guest.getDateStart().after(minDate) && guest.getDateFinish().before(maxDate)) {  // Все дни внутри периода
                daysCount = TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS);
            } else if (guest.getDateStart().before(minDate) && guest.getDateFinish().before(maxDate)) { // Если Дата начала не входит в период то сичтает с начала периода по дату выезда
                daysCount = TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedPeriodStartDate.getTime(), TimeUnit.MILLISECONDS);
            } else if (guest.getDateStart().after(minDate) && guest.getDateFinish().after(maxDate)) { // Если Дата выселения не входит в период то сичтает с заселения по конца периода
                daysCount = TimeUnit.DAYS.convert(cuttedPeriodFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS) + 1;
            }
            if (daysCount == 0) daysCount = 1L;
            daysCountSummary += daysCount;
            monthReportDTO.setDaysCount(Math.toIntExact(daysCount));
            if (!contracts.isEmpty()) {
                Float cost = Float.valueOf(df.format(contracts.get(0).getCost()).replace(',', '.'));
                monthReportDTO.setCostFromContract(Float.valueOf(String.format("%.2f", cost).replace(',', '.')));
                monthReportDTO.setCost(Float.valueOf(String.format("%.2f", daysCount * cost).replace(',', '.')));
                costSummary += daysCount * cost;
            }
            if (guest.getEmployee() != null) {
                monthReportDTO.setTabnum(guest.getEmployee().getTabnum().toString());
            } else {
                monthReportDTO.setTabnum("   ");
            }
            monthReportDTO.setMemo(guest.getMemo());
            reportData.add(monthReportDTO);
            count++;
        }
        reportData.sort((rec1, rec2) -> {
            return rec1.getDateStart().compareTo(rec2.getDateStart());
        });
        int i = 1;
        for (MonthReportDTO monthReportDTO : reportData) {
            monthReportDTO.setId(String.valueOf(i));
            i++;
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/MonthReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("filialFrom", organization.getName());
        parameters.put("periodStart", dateStart);
        parameters.put("periodFinish", dateFinish);
        parameters.put("filial", filial.getName());
        parameters.put("hotelLocation", responsibilities.getHotel().getName() + ", " + responsibilities.getHotel().getLocation());
        parameters.put("daysCountSummary", daysCountSummary.toString());
        parameters.put("costSummary", String.format("%.2f", costSummary));
        String respName = responsibilities.getEmployee().getFirstname().charAt(0) + ". " + responsibilities.getEmployee().getSecondName().charAt(0) + ". " + responsibilities.getEmployee().getLastname();
        String respPost = postRepository.getById(responsibilities.getEmployee().getIdPoststaff().longValue()).getName();
        parameters.put("respPost", respPost);
        parameters.put("respName", respName);
        parameters.put("filialBossPost", "");
        parameters.put("filialBossName", "");
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }

    @Transactional
    public byte[] getMVZReport(Long filialId, String dateStart, String dateFinish) throws JRException, ParseException {
        List<MVZReportDTO> reportData = new ArrayList<>();
        Filial filial = filialRepository.getById(filialId);
        Date minDate = dateTimeFormatter.parse(dateStart + " 23:59");
        Date maxDate = dateTimeFormatter.parse(dateFinish + " 23:59");
        int count = 1;
        for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfter(maxDate, minDate)) {
            if (guest.getEmployee() == null) continue; // только работников
            MVZ mvz = mvzRepository.findById(guest.getEmployee().getMvzId());
            if (mvz != null) {
                MVZReportDTO mvzReportDTO = new MVZReportDTO();
                mvzReportDTO.setId(String.valueOf(count));
                mvzReportDTO.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
                mvzReportDTO.setHotel(guest.getRoom().getFlat().getHotel().getName());
                Long daysCount = 1L;
                Date cuttedGuestStartDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateStart()));
                Date cuttedGuestFinishDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateFinish()));
                Date cuttedPeriodStartDate = dateFormatter.parse(dateTimeFormatter.format(minDate.getTime()));
                Date cuttedPeriodFinishDate = dateFormatter.parse(dateTimeFormatter.format(maxDate.getTime()));
                if (guest.getDateStart().before(minDate) && guest.getDateFinish().after(maxDate)) {  // Все дни заданного периода
                    daysCount = TimeUnit.DAYS.convert(cuttedPeriodFinishDate.getTime() - cuttedPeriodStartDate.getTime(), TimeUnit.MILLISECONDS) + 1;
                    mvzReportDTO.setDateStart(minDate);
                    mvzReportDTO.setDateFinish(maxDate);
                } else if (guest.getDateStart().after(minDate) && guest.getDateFinish().before(maxDate)) {  // Все дни внутри периода
                    daysCount = TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS);
                    mvzReportDTO.setDateStart(guest.getDateStart());
                    mvzReportDTO.setDateFinish(guest.getDateFinish());
                } else if (guest.getDateStart().before(minDate) && guest.getDateFinish().before(maxDate)) { // Если Дата начала не входит в период то сичтает с начала периода по дату выезда
                    daysCount = TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedPeriodStartDate.getTime(), TimeUnit.MILLISECONDS);
                    mvzReportDTO.setDateStart(minDate);
                    mvzReportDTO.setDateFinish(guest.getDateFinish());
                } else if (guest.getDateStart().after(minDate) && guest.getDateFinish().after(maxDate)) { // Если Дата выселения не входит в период то сичтает с заселения по конца периода
                    daysCount = TimeUnit.DAYS.convert(cuttedPeriodFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS) + 1;
                    mvzReportDTO.setDateStart(guest.getDateStart());
                    mvzReportDTO.setDateFinish(maxDate);
                }
                if (daysCount == 0) daysCount = 1L;
                mvzReportDTO.setDaysCount(Math.toIntExact(daysCount));
                mvzReportDTO.setTabnum(guest.getEmployee().getTabnum().toString());
                mvzReportDTO.setMvz(mvz.getId());
                mvzReportDTO.setMvzName(mvz.getName());
                mvzReportDTO.setGuestFilial(filialRepository.findByCode(guest.getEmployee().getIdFilial()).getName());
                mvzReportDTO.setFilial(filial.getName());
                mvzReportDTO.setOrgUnit(mvz.getOrganization());
                mvzReportDTO.setReason(guest.getReason().getName());
                mvzReportDTO.setBilling(guest.getBilling());
                reportData.add(mvzReportDTO);
                count++;
            }
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/MVZReport.jrxml"));
        reportData.sort((rec1, rec2) -> {
            return rec1.getDateStart().compareTo(rec2.getDateStart());
        });
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("periodStart", dateStart);
        parameters.put("periodFinish", dateFinish);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }

    public List<ContractDTO> getAllByFilialAndHotel(Long filialId, Long hotelId, Long reasonId, String orgStr, String billing) {
        List<ContractDTO> response = new ArrayList<>();
        Organization org = organizationRepository.findByName(orgStr);
        Filial filial = filialRepository.findById(filialId).orElse(null);
        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        Reason reason = reasonRepository.findById(reasonId).orElse(null);
        for (Contract contract : contractRepository.findAllByFilialAndHotelAndReasonAndOrganizationAndBilling(filial, hotel, reason, org, billing)) {
            ContractDTO contractDTO = new ContractDTO();
            contractDTO.setId(contract.getId());
            contractDTO.setFilial(contract.getFilial().getName());
            contractDTO.setFilialId(contract.getFilial().getId());
            if (contract.getHotel() != null) {
                contractDTO.setHotel(contract.getHotel().getName());
                contractDTO.setHotelId(contract.getHotel().getId());
            }
            if (contract.getOrganization() != null) {
                contractDTO.setOrganization(contract.getOrganization().getName());
                contractDTO.setOrganizationId(contract.getOrganization().getId());
            }
            contractDTO.setDocnum(contract.getDocnum());
            contractDTO.setCost(contract.getCost());
            contractDTO.setReasonId(contract.getReason().getId());
            contractDTO.setNote(contract.getNote());
            contractDTO.setOsnovanie(contract.getReason().getName());
            contractDTO.setBilling(contract.getBilling());
            contractDTO.setYear(contract.getYear());
            contractDTO.setRoomNumber(contract.getRoomNumber());
            response.add(contractDTO);
        }
        return response;
    }
}