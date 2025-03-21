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
import ru.sgp.dto.report.*;
import ru.sgp.model.*;
import ru.sgp.repository.*;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ReportService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    GuestRepository guestRepository;
    @Autowired
    FilialRepository filialRepository;
    @Autowired
    ContractRepository contractRepository;
    @Autowired
    OrganizationRepository organizationRepository;
    @Autowired
    HotelRepository hotelRepository;
    @Autowired
    ReasonRepository reasonRepository;
    @Autowired
    ResponsibilitiesRepository responsibilitiesRepository;
    @Autowired
    PostRepository postRepository;
    @Autowired
    MVZRepository mvzRepository;
    @Autowired
    RoomLocksRepository roomLocksRepository;
    @Autowired
    FlatLocksRepository flatLocksRepository;
    @Autowired
    FlatRepository flatRepository;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    BedRepository bedRepository;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat dateFormatterDot = new SimpleDateFormat("dd.MM.yyyy");
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    private final SimpleDateFormat dateTimeDotFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
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
    public byte[] getGuestReport() throws JRException {
        List<GuestReportDTO> guestReportDTOS = new ArrayList<>();
        String username = ru.sgp.utils.SecurityManager.getCurrentUser();
        User user = userRepository.findByUsername(username);
        List<Guest> guests;

        // Если роль Администратор или Работник ОСР, отдаем все записи
        if (user.getRole().getId() == 1L || user.getRole().getId() == 5L) {
            guests = guestRepository.findAll();
        } else {
            // Если дежурный или работник филиала, то отдаем записи по филиалу работника/дежурного по скольку их место работы 99% совпадает с филиалом общежития
            Filial filial = filialRepository.findByCode(user.getEmployee().getIdFilial());
            guests = guestRepository.findAllByBedRoomFlatHotelFilial(filial);
        }
        for (Guest guest : guests) {
            GuestReportDTO guestReportDTO = new GuestReportDTO();
            guestReportDTO.setId(guest.getId().toString());
            guestReportDTO.setSurname(guest.getLastname());
            guestReportDTO.setSecondName(guest.getSecondName());
            guestReportDTO.setName(guest.getFirstname());
            guestReportDTO.setDateStart(dateTimeFormatter.format(guest.getDateStart()));
            guestReportDTO.setDateFinish(dateTimeFormatter.format(guest.getDateFinish()));
            guestReportDTO.setRoom(guest.getBed().getRoom().getFlat().getName());
            guestReportDTO.setHotel(guest.getBed().getRoom().getFlat().getHotel().getName());
            guestReportDTO.setFilial(guest.getBed().getRoom().getFlat().getHotel().getFilial().getName());
            if (guest.getOrganization() != null)
                guestReportDTO.setOrg(guest.getOrganization().getName());
            guestReportDTO.setRepPoMestu(guest.getRegPoMestu() ? "+" : "-");
            guestReportDTO.setCz(guest.getMemo());
            guestReportDTO.setMale(guest.getMale() ? "man" : "woman");
            if (guest.getCheckouted() != null)
                guestReportDTO.setCheckouted(guest.getCheckouted() ? "+" : "-");
            else guestReportDTO.setCheckouted("");
            if (guest.getContract() != null) {
                guestReportDTO.setContract(guest.getContract().getDocnum());
                guestReportDTO.setBilling(guest.getContract().getBilling());
                guestReportDTO.setReason(guest.getContract().getReason().getName());
            } else {
                guestReportDTO.setContract("");
                guestReportDTO.setBilling("");
                guestReportDTO.setReason("");
            }
            guestReportDTO.setBed(guest.getBed().getRoom().getName());
            guestReportDTOS.add(guestReportDTO);
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/GuestReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(guestReportDTOS);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }
    @Transactional
    public byte[] getMonthReportByFilial(Long empFilialId, Long responsibilityId, Long reasonId, String dateStart, String dateFinish, String billing) throws JRException, ParseException {
        List<MonthReportDTO> reportData = new ArrayList<>();
        Filial empFilial = filialRepository.findById(empFilialId).orElse(null);
        Reason reason = reasonRepository.getById(reasonId);
        Responsibilities responsibilities = responsibilitiesRepository.getById(responsibilityId);
        Filial filial = responsibilities.getHotel().getFilial();
        Long daysCountSummary = 0L;
        Double costSummary = 0.0;
        Date minDate = dateTimeFormatter.parse(dateStart + " 23:59");
        Date maxDate = dateTimeFormatter.parse(dateFinish + " 23:59");
        int count = 1;
        for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotel(maxDate, minDate, responsibilities.getHotel())) {
            Filial guestFilial = null;
            if (guest.getContract() == null) continue;
            if (guest.getEmployee() != null) {
                guestFilial = filialRepository.findByCode(guest.getEmployee().getIdFilial());
            }
            if (empFilial != null) {
                if (guestFilial != null) {
                    if (!Objects.equals(guestFilial.getId(), empFilial.getId())) continue;
                } else continue;
            } else {
                if (guest.getEmployee() != null) continue; // Если работник, то скипаем это только для сторонников
            }
            if (guest.getContract() != null){
                if (!guest.getContract().getBilling().equals(billing)) continue;
                if (!Objects.equals(guest.getContract().getReason().getId(), reason.getId())) continue;
            } else continue;

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(minDate);
            List<Contract> contracts = contractRepository.findAllByFilialAndHotelAndReasonAndYear(filial, guest.getBed().getRoom().getFlat().getHotel(), guest.getContract().getReason(), calendar.get(Calendar.YEAR));
            MonthReportDTO monthReportDTO = new MonthReportDTO();
            monthReportDTO.setId(String.valueOf(count));
            monthReportDTO.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
            if (guest.getDateStart().compareTo(minDate) > 0)
                monthReportDTO.setDateStart(dateFormatterDot.format(guest.getDateStart()));
            else
                monthReportDTO.setDateStart(dateFormatterDot.format(minDate));
            if (guest.getDateFinish().compareTo(maxDate) < 0)
                monthReportDTO.setDateFinish(dateFormatterDot.format(guest.getDateFinish()));
            else
                monthReportDTO.setDateFinish(dateFormatterDot.format(maxDate));
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
                if (contracts.get(0).getCost() % 1 == 0)
                    monthReportDTO.setCostFromContract(String.valueOf(contracts.get(0).getCost().intValue()));
                else
                    monthReportDTO.setCostFromContract(contracts.get(0).getCost().toString().replace('.', ','));

                monthReportDTO.setCost(daysCount * contracts.get(0).getCost());
                costSummary += daysCount * contracts.get(0).getCost();
            }
            if (guest.getEmployee() != null) {
                monthReportDTO.setTabnum(guest.getEmployee().getTabnum().toString());
            } else {
                monthReportDTO.setTabnum("   ");
            }
            monthReportDTO.setMemo(guest.getMemo());
            reportData.add(monthReportDTO);

            // Добавляем членов семьи, если есть
            List<Guest> family = guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotelAndFamilyMemberOfEmployee(maxDate, minDate, responsibilities.getHotel(), guest.getEmployee());
            for(Guest member : family) {
                List<Contract> contractsMember = contractRepository.findAllByFilialAndHotelAndReasonAndYear(filial, member.getBed().getRoom().getFlat().getHotel(), member.getContract().getReason(), calendar.get(Calendar.YEAR));
                MonthReportDTO memberDTO = new MonthReportDTO();
                memberDTO.setId(String.valueOf(member.getId()));
                memberDTO.setFio(member.getLastname() + " " + member.getFirstname() + " " + member.getSecondName());
                memberDTO.setDateStart(dateFormatter.format(member.getDateStart()));
                memberDTO.setDateFinish(dateFormatter.format(member.getDateFinish()));
                if (member.getDateStart().compareTo(minDate) > 0)
                    memberDTO.setDateStart(dateFormatterDot.format(member.getDateStart()));
                else
                    memberDTO.setDateStart(dateFormatterDot.format(minDate));
                if (member.getDateFinish().compareTo(maxDate) < 0)
                    memberDTO.setDateFinish(dateFormatterDot.format(member.getDateFinish()));
                else
                    memberDTO.setDateFinish(dateFormatterDot.format(maxDate));
                Long daysCountMember = 1L;
                Date cuttedGuestStartDateMember = dateFormatter.parse(dateTimeFormatter.format(member.getDateStart()));
                Date cuttedGuestFinishDateMember = dateFormatter.parse(dateTimeFormatter.format(member.getDateFinish()));
                Date cuttedPeriodStartDateMember = dateFormatter.parse(dateTimeFormatter.format(minDate.getTime()));
                Date cuttedPeriodFinishDateMember = dateFormatter.parse(dateTimeFormatter.format(maxDate.getTime()));
                if (member.getDateStart().before(minDate) && member.getDateFinish().after(maxDate)) {  // Все дни заданного периода
                    daysCountMember = TimeUnit.DAYS.convert(cuttedPeriodFinishDateMember.getTime() - cuttedPeriodStartDateMember.getTime(), TimeUnit.MILLISECONDS) + 1;
                } else if (member.getDateStart().after(minDate) && member.getDateFinish().before(maxDate)) {  // Все дни внутри периода
                    daysCountMember = TimeUnit.DAYS.convert(cuttedGuestFinishDateMember.getTime() - cuttedGuestStartDateMember.getTime(), TimeUnit.MILLISECONDS);
                } else if (member.getDateStart().before(minDate) && member.getDateFinish().before(maxDate)) { // Если Дата начала не входит в период то сичтает с начала периода по дату выезда
                    daysCountMember = TimeUnit.DAYS.convert(cuttedGuestFinishDateMember.getTime() - cuttedPeriodStartDateMember.getTime(), TimeUnit.MILLISECONDS);
                } else if (member.getDateStart().after(minDate) && member.getDateFinish().after(maxDate)) { // Если Дата выселения не входит в период то сичтает с заселения по конца периода
                    daysCountMember = TimeUnit.DAYS.convert(cuttedPeriodFinishDateMember.getTime() - cuttedGuestStartDateMember.getTime(), TimeUnit.MILLISECONDS) + 1;
                } else {
                    continue;
                }
                if (daysCountMember == 0) daysCountMember = 1L;
                daysCountSummary += daysCountMember;
                memberDTO.setDaysCount(Math.toIntExact(daysCountMember));
                if (!contractsMember.isEmpty()) {
                    if (contractsMember.get(0).getCost() % 1 == 0)
                        memberDTO.setCostFromContract(String.valueOf(contractsMember.get(0).getCost().intValue()));
                    else
                        memberDTO.setCostFromContract(contractsMember.get(0).getCost().toString().replace('.', ','));

                    memberDTO.setCost(daysCountMember * contractsMember.get(0).getCost());
                    costSummary += daysCountMember * contractsMember.get(0).getCost();
                }
                memberDTO.setTabnum("   ");
                memberDTO.setMemo(member.getMemo());
                reportData.add(memberDTO);
                count++;
            }
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
        parameters.put("periodStart", dateFormatterDot.format(dateFormatter.parse(dateStart)));
        parameters.put("periodFinish", dateFormatterDot.format(dateFormatter.parse(dateFinish)));
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
            if (guest.getContract() == null) continue;
            if (guest.getEmployee() != null) {
                guestFilial = filialRepository.findByCode(guest.getEmployee().getIdFilial());
            }
            Hotel guestHotel = guest.getBed().getRoom().getFlat().getHotel();
            if (empFilial != null) {
                if (guestFilial != null) {
                    if (!Objects.equals(guestFilial.getId(), empFilial.getId())) continue;
                } else continue;
            } else {
                if (guest.getEmployee() != null) continue; // Если работник то скипаем это только для сторонников
            }
            if (guest.getContract() != null){
                if (!guest.getContract().getBilling().equals(billing)) continue;
                if (!Objects.equals(guest.getContract().getReason().getId(), reason.getId())) continue;
            } else continue;
            if (responsibilities.getHotel() != guestHotel) continue;
            if (guest.getEmployee() == null) continue;
            if (mvzRepository.findByIdAndNameIsContainingIgnoreCase(guest.getEmployee().getMvzId(), ceh).isEmpty())  // Проверка цеха
                continue;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(minDate);
            List<Contract> contracts = contractRepository.findAllByFilialAndHotelAndReasonAndYear(filial, guest.getBed().getRoom().getFlat().getHotel(), guest.getContract().getReason(), calendar.get(Calendar.YEAR));
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
                if (contracts.get(0).getCost() % 1 == 0)
                    monthReportDTO.setCostFromContract(String.valueOf(contracts.get(0).getCost().intValue()));
                else
                    monthReportDTO.setCostFromContract(contracts.get(0).getCost().toString().replace('.', ','));

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
        for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotelAndOrganization(maxDate, minDate, responsibilities.getHotel(), organization)) {
            if (guest.getEmployee() != null) continue;
            if (guest.getContract() == null) continue;
            if (!Objects.equals(guest.getContract().getReason().getId(), reason.getId())) continue;
            if (!guest.getContract().getBilling().equals(billing)) continue;
            List<Contract> contracts = contractRepository.findAllByFilialAndHotelAndReasonAndOrganization(filial, guest.getBed().getRoom().getFlat().getHotel(), guest.getContract().getReason(), guest.getOrganization());
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
                if (contracts.get(0).getCost() % 1 == 0)
                    monthReportDTO.setCostFromContract(String.valueOf(contracts.get(0).getCost().intValue()));
                else
                    monthReportDTO.setCostFromContract(contracts.get(0).getCost().toString().replace('.', ','));
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
        for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotelFilial(maxDate, minDate, filial)) { // тут должен быть фильтр гостя по заднному филиалу
            if (guest.getEmployee() == null) continue; // только работников
            MVZ mvz = mvzRepository.findById(guest.getEmployee().getMvzId());
            if (mvz != null) {
                MVZReportDTO mvzReportDTO = new MVZReportDTO();
                mvzReportDTO.setId(String.valueOf(count));
                mvzReportDTO.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
                mvzReportDTO.setHotel(guest.getBed().getRoom().getFlat().getHotel().getName());
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
                } else continue;
                if (daysCount == 0) daysCount = 1L;
                mvzReportDTO.setDaysCount(Math.toIntExact(daysCount));
                mvzReportDTO.setTabnum(guest.getEmployee().getTabnum().toString());
                mvzReportDTO.setMvz(mvz.getId());
                mvzReportDTO.setMvzName(mvz.getName());
                mvzReportDTO.setGuestFilial(filialRepository.findByCode(guest.getEmployee().getIdFilial()).getName());
                mvzReportDTO.setFilial(filial.getName());
                mvzReportDTO.setOrgUnit(mvz.getOrganization());
                if (guest.getContract() != null) {
                    mvzReportDTO.setReason(guest.getContract().getReason().getName());
                    mvzReportDTO.setBilling(guest.getContract().getBilling());
                } else {
                    mvzReportDTO.setReason("");
                    mvzReportDTO.setBilling("");
                }
                reportData.add(mvzReportDTO);
                count++;
            }
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/MVZReport.jrxml"));
        reportData.sort((rec1, rec2) -> {
            if (rec1.getDateStart() == null || rec2.getDateStart() == null) {
                int a = 1;
            }
            return rec1.getDateStart().compareTo(rec2.getDateStart());
        });
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("periodStart", dateFormatterDot.format(dateFormatter.parse(dateStart)));
        parameters.put("periodFinish", dateFormatterDot.format(dateFormatter.parse(dateFinish)));
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }
    @Transactional
    public byte[] getMVZReportOnlyLPU(String lpu, String dateStart, String dateFinish) throws JRException, ParseException {
        List<MVZReportDTO> reportData = new ArrayList<>();
        Date minDate = dateTimeFormatter.parse(dateStart + " 23:59");
        Date maxDate = dateTimeFormatter.parse(dateFinish + " 23:59");
        int count = 1;
        for (Filial filial : filialRepository.findByNameIsContainingIgnoreCase(lpu)) {
            for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotelFilial(maxDate, minDate, filial)) {
                if (guest.getEmployee() == null) continue; // только работников
                MVZ mvz = mvzRepository.findById(guest.getEmployee().getMvzId());
                if (mvz != null) {
                    MVZReportDTO mvzReportDTO = new MVZReportDTO();
                    mvzReportDTO.setId(String.valueOf(count));
                    mvzReportDTO.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
                    mvzReportDTO.setHotel(guest.getBed().getRoom().getFlat().getHotel().getName());
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
                    } else continue;
                    if (daysCount == 0) daysCount = 1L;
                    mvzReportDTO.setDaysCount(Math.toIntExact(daysCount));
                    mvzReportDTO.setTabnum(guest.getEmployee().getTabnum().toString());
                    mvzReportDTO.setMvz(mvz.getId());
                    mvzReportDTO.setMvzName(mvz.getName());
                    mvzReportDTO.setGuestFilial(filialRepository.findByCode(guest.getEmployee().getIdFilial()).getName());
                    mvzReportDTO.setFilial(filial.getName());
                    mvzReportDTO.setOrgUnit(mvz.getOrganization());
                    if (guest.getContract() != null) {
                        mvzReportDTO.setReason(guest.getContract().getReason().getName());
                        mvzReportDTO.setBilling(guest.getContract().getBilling());
                    } else {
                        mvzReportDTO.setReason("");
                        mvzReportDTO.setBilling("");
                    }
                    reportData.add(mvzReportDTO);
                    count++;
                }
            }
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/MVZReport.jrxml"));
        reportData.sort((rec1, rec2) -> {
            if (rec1.getDateStart() == null || rec2.getDateStart() == null) {
                int a = 1;
            }
            return rec1.getDateStart().compareTo(rec2.getDateStart());
        });
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("periodStart", dateFormatterDot.format(dateFormatter.parse(dateStart)));
        parameters.put("periodFinish", dateFormatterDot.format(dateFormatter.parse(dateFinish)));
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }
    public byte[] getAllContractReport() throws JRException {
        List<ContractReportDTO> reportData = new ArrayList<>();
        for (Contract contract: contractRepository.findAllByOrderById()) {
            ContractReportDTO contractDTO = new ContractReportDTO();
            contractDTO.setId(contract.getId());
            contractDTO.setFilial(contract.getFilial().getName());
            contractDTO.setHotel(contract.getHotel().getName());
            contractDTO.setCompany(contract.getOrganization().getName());
            contractDTO.setContractNumber(contract.getDocnum());
            contractDTO.setCost(contract.getCost());
            contractDTO.setReason(contract.getReason().getName());
            contractDTO.setBilling(contract.getBilling());
            if (contract.getNote() != null) contractDTO.setNote(contract.getNote());
            else contractDTO.setNote("");
            if (contract.getRoomNumber() != null) contractDTO.setRoomName(contract.getRoomNumber().toString());
            else contractDTO.setRoomName("");
            contractDTO.setYear(contract.getYear());
            reportData.add(contractDTO);
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/Contracts.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }
    public byte[] getFilialReport(Long filialId, boolean checkouted, String dateStartStr, String dateFinishStr) throws JRException, ParseException {
        List<FilialReportDTO> reportData = new ArrayList<>();
        Date dateStart = dateFormatter.parse(dateStartStr);
        Date dateFinish = dateFormatter.parse(dateFinishStr);
        Filial filial = filialRepository.findById(filialId).orElse(null);
        for (Hotel hotel : hotelRepository.findAllByFilial(filial)) {
            for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
                for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                    List<Guest> guests = new ArrayList<>();
                    if (checkouted)
                        guests = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBedRoom(dateFinish, dateStart, room);
                    else // DateStartLessThanAndDateFinishGreaterThan
                        guests = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndCheckoutedAndBedRoom(dateFinish, dateStart, checkouted, room);
                    for (Guest guest : guests) {
                        FilialReportDTO record = new FilialReportDTO();
                        record.setFilial(filial.getName());
                        record.setHotel(hotel.getName());
                        record.setFlat(flat.getName());
                        record.setFloor(flat.getFloor());
                        record.setRoom(room.getName());
                        record.setDateStart(dateFormatter.format(guest.getDateStart()));
                        record.setDateFinish(dateFormatter.format(guest.getDateFinish()));
                        if (guest.getCheckouted() != null)
                            record.setCheckouted(guest.getCheckouted() ? "+" : "-");
                        else
                            record.setCheckouted("-");


                        Long daysCount = 1L;
                        Date cuttedGuestStartDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateStart()));
                        Date cuttedGuestFinishDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateFinish()));
                        Date cuttedPeriodStartDate = dateFormatter.parse(dateTimeFormatter.format(dateStart.getTime()));
                        Date cuttedPeriodFinishDate = dateFormatter.parse(dateTimeFormatter.format(dateFinish.getTime()));
                        if (guest.getDateStart().before(dateStart) && guest.getDateFinish().after(dateFinish)) {  // Все дни заданного периода
                            daysCount = TimeUnit.DAYS.convert(cuttedPeriodFinishDate.getTime() - cuttedPeriodStartDate.getTime(), TimeUnit.MILLISECONDS) + 1;
                            //record.setDateStart(dateTimeFormatter.format(dateStart));
                            //record.setDateFinish(dateTimeFormatter.format(dateFinish));
                        } else if (guest.getDateStart().after(dateStart) && guest.getDateFinish().before(dateFinish)) {  // Все дни внутри периода
                            daysCount = TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS);
                            //record.setDateStart(dateTimeFormatter.format(guest.getDateStart()));
                            //record.setDateFinish(dateTimeFormatter.format(guest.getDateFinish()));
                        } else if (guest.getDateStart().before(dateStart) && guest.getDateFinish().before(dateFinish)) { // Если Дата начала не входит в период то сичтает с начала периода по дату выезда
                            daysCount = TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedPeriodStartDate.getTime(), TimeUnit.MILLISECONDS);
                            //record.setDateStart(dateTimeFormatter.format(dateStart));
                            //record.setDateFinish(dateTimeFormatter.format(guest.getDateFinish()));
                        } else if (guest.getDateStart().after(dateStart) && guest.getDateFinish().after(dateFinish)) { // Если Дата выселения не входит в период то сичтает с заселения по конца периода
                            daysCount = TimeUnit.DAYS.convert(cuttedPeriodFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS) + 1;
                            //record.setDateStart(dateTimeFormatter.format(guest.getDateStart()));
                            //record.setDateFinish(dateTimeFormatter.format(dateFinish));
                        } else continue;
                        record.setDateStart(dateTimeDotFormatter.format(guest.getDateStart()));
                        record.setDateFinish(dateTimeDotFormatter.format(guest.getDateFinish()));
                        if (daysCount == 0) daysCount = 1L;



                        record.setNights(Math.toIntExact(daysCount));
                        if (guest.getContract() != null) {
                            record.setReason(guest.getContract().getReason().getName());
                            record.setBilling(guest.getContract().getBilling());
                            record.setContract(guest.getContract().getDocnum());
                            record.setContractPrice(guest.getContract().getCost());
                            record.setPeriodPrice(daysCount * guest.getContract().getCost());
                        } else {
                            record.setReason("");
                            record.setBilling("");
                            record.setContract("");
                            record.setContractPrice(0f);
                            record.setPeriodPrice(0f);
                        }
                        if (guest.getEmployee() != null) {
                            record.setTabnum(guest.getEmployee().getTabnum());
                            record.setGuestFilial(filialRepository.findByCode(guest.getEmployee().getIdFilial()).getName());
                        } else {
                            record.setTabnum(0);
                            if (guest.getOrganization() != null)
                                record.setGuestFilial(guest.getOrganization().getName());
                            else record.setGuestFilial("");
                        }
                        record.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
                        reportData.add(record);
                    }
                }
            }
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/FilialReport.jrxml"));
        reportData.sort(new Comparator<FilialReportDTO>() {
            @Override
            public int compare(FilialReportDTO o1, FilialReportDTO o2) {
                char c1 = o1.getFio().charAt(0);
                char c2 = o2.getFio().charAt(0);
                return c1 - c2;
            }
        });
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }
    public byte[] getFilialReportByFIO(String lastName, String dateStartStr, String dateFinishStr) throws JRException, ParseException {
        List<FilialReportDTO> reportData = new ArrayList<>();
        Date dateStart = dateFormatter.parse(dateStartStr);
        Date dateFinish = dateFormatter.parse(dateFinishStr);
        guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndLastname(dateFinish, dateStart, lastName).forEach(guest -> {
            FilialReportDTO record = new FilialReportDTO();
            record.setFilial(guest.getBed().getRoom().getFlat().getHotel().getFilial().getName());
            record.setHotel(guest.getBed().getRoom().getFlat().getHotel().getName());
            record.setFlat(guest.getBed().getRoom().getFlat().getName());
            record.setFloor(guest.getBed().getRoom().getFlat().getFloor());
            record.setRoom(guest.getBed().getRoom().getName());
            record.setDateStart(dateFormatterDot.format(guest.getDateStart()));
            record.setDateFinish(dateFormatterDot.format(guest.getDateFinish()));
            if (guest.getCheckouted() == null) record.setCheckouted("-");
            else record.setCheckouted(guest.getCheckouted() ? "+" : "-");
            Date cuttedStartDate = null;
            Date cuttedFinishDate = null;
            int daysCount = 0;
            try {
                cuttedStartDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateStart()));
                cuttedFinishDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateFinish()));
                daysCount = Integer.parseInt(String.valueOf(TimeUnit.DAYS.convert(cuttedFinishDate.getTime() - cuttedStartDate.getTime(), TimeUnit.MILLISECONDS)));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            if (daysCount == 0) daysCount = 1;
            record.setNights(daysCount);
            if (guest.getContract() != null) {
                record.setReason(guest.getContract().getReason().getName());
                record.setBilling(guest.getContract().getBilling());
                record.setContract(guest.getContract().getDocnum());
                record.setContractPrice(guest.getContract().getCost());
                record.setPeriodPrice(daysCount * guest.getContract().getCost());
            } else {
                record.setReason("");
                record.setBilling("");
                record.setContract("");
                record.setContractPrice(0f);
                record.setPeriodPrice(0f);
            }
            if (guest.getEmployee() != null) {
                record.setTabnum(guest.getEmployee().getTabnum());
                record.setGuestFilial(filialRepository.findByCode(guest.getEmployee().getIdFilial()).getName());
            } else {
                record.setTabnum(0);
                if (guest.getOrganization() != null)
                    record.setGuestFilial(guest.getOrganization().getName());
                else record.setGuestFilial("");
            }
            record.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
            reportData.add(record);
        });
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/FilialReport.jrxml"));
        reportData.sort(new Comparator<FilialReportDTO>() {
            @Override
            public int compare(FilialReportDTO o1, FilialReportDTO o2) {
                char c1 = o1.getFio().charAt(0);
                char c2 = o2.getFio().charAt(0);
                return c1 - c2;
            }
        });
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }
    public byte[] getShortReport(Long filialId) throws JRException {
        List<ShortReportDTO> reportData = new ArrayList<>();
        Filial filial = filialRepository.findById(filialId).get();
        for (Hotel hotel : hotelRepository.findAllByFilial(filial)) {
            ShortReportDTO record = new ShortReportDTO();
            record.setFilial(filial.getName());
            record.setHotel(hotel.getName());
            AtomicReference<Integer> bedsCount = new AtomicReference<>(0);
            flatRepository.findAllByHotelOrderById(hotel).forEach(flat -> {
                if (!flat.getTech())
                    roomRepository.findAllByFlatOrderById(flat).forEach(room -> {
                        bedsCount.updateAndGet(v -> v + room.getBedsCount());
                    });
            });
            record.setAll(bedsCount.get());
            Integer countBusyBeds = 0;
            List<Long> flatsExcludeList = new ArrayList<>();
            List<Long> roomExcludeList = new ArrayList<>();
            Date date = new Date();
            for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotel(date, date, hotel)) {
                if (roomExcludeList.contains(guest.getBed().getRoom().getId())) continue;
                if (flatsExcludeList.contains(guest.getBed().getRoom().getFlat().getId())) continue;
                Hotel guestHotel = guest.getBed().getRoom().getFlat().getHotel();
                if (Objects.equals(guestHotel.getId(), hotel.getId())) {
                    Room guestRoom = guest.getBed().getRoom();
                    Flat guestFlat = guestRoom.getFlat();
                    List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(date, date, guestRoom);
                    List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(date, date, guestFlat);
                    if (!flatLocksList.isEmpty()) { // Посчитать кол-во мест во всей секции и указать что они заняты
                        if (flatLocksList.get(0).getStatus().getId() == 4L || flatLocksList.get(0).getStatus().getId() == 2L) { // указывать что секции заняты только если они выкупалены организацией (ИД 4)
                            countBusyBeds += bedRepository.countByRoomFlatAndIsExtra(guestFlat, false);
                            flatsExcludeList.add(guestFlat.getId());
                        }
                    } else if (!roomLocksList.isEmpty()) { // Посчитать кол-во мест в комнате и указать что они заняты
                        if (roomLocksList.get(0).getStatus().getId() == 3L || roomLocksList.get(0).getStatus().getId() == 2L) { // указывать что комнаты заняты только если они выкупалены организацией (ИД 3)
                            countBusyBeds += bedRepository.countByRoomAndIsExtra(guestRoom, false);
                            roomExcludeList.add(guestRoom.getId());
                        }
                    } else countBusyBeds += 1;  // Просто указываем что гость занимает одно место
                }
            }
            if (record.getAll() > 0) {
                record.setBusy(countBusyBeds);
                record.setVacant(record.getAll() - record.getBusy());
                record.setPercent((int) (((float) record.getBusy() / (float) record.getAll()) * 100));
            } else {
                record.setBusy(0);
                record.setVacant(0);
                record.setPercent(0);
            }
            reportData.add(record);
        }

        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/ShortReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("date", dateTimeFormatter.format(new Date()));
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }
    @Transactional
    public byte[] getLoadStatsReport(Long hotelId, String dateStartStr, String dateFinishStr) throws JRException, ParseException {
        LoadStatsReportDTO reportData = new LoadStatsReportDTO();
        Date dateStartD = dateFormatter.parse(dateStartStr);
        LocalDate dateStart = dateStartD.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Date dateFinishD = dateFormatter.parse(dateFinishStr);
        LocalDate dateFinish = dateFinishD.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Hotel hotel = hotelRepository.getById(hotelId);
        AtomicReference<Integer> allBeds = new AtomicReference<>(0);
        flatRepository.findAllByHotelOrderById(hotel).forEach(flat -> {
            roomRepository.findAllByFlatOrderById(flat).forEach(room -> {
                allBeds.updateAndGet(v -> v + room.getBedsCount());
            });
        });

        reportData.setFilial(hotel.getFilial().getName());
        reportData.setHotel(hotel.getName());

        int busyBeds = 0;
        int daysCount = 0;
        while (dateStart.isBefore(dateFinish) || dateStart.isEqual(dateFinish)) {
            daysCount += 1;
            Date start = Date.from(dateStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
            busyBeds += guestRepository.countAllByDateStartLessThanEqualAndDateFinishGreaterThanEqualAndBedRoomFlatHotel(start, start, hotel);
            dateStart = dateStart.plusDays(1);
        }

        reportData.setAllBeds(allBeds.get() * daysCount);
        reportData.setBusyBeds(busyBeds);
        Float percent = ((float) busyBeds / (float) reportData.getAllBeds()) * 100;
        BigDecimal bd = new BigDecimal(Float.toString(percent));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        reportData.setPercent(bd.toString());
        reportData.setDates(dateFormatterDot.format(dateStartD) + " " + dateFormatterDot.format(dateFinishD));

        List<LoadStatsReportDTO> data = new ArrayList<>();
        data.add(reportData);
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/LoadStats.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }
    public byte[] getHotelReport(Long hotelId, boolean checkouted, String dateStartStr, String dateFinishStr) throws JRException, ParseException {
        List<FilialReportDTO> reportData = new ArrayList<>();
        Date dateStart = dateFormatter.parse(dateStartStr);
        Date dateFinish = dateFormatter.parse(dateFinishStr);
        Hotel hotel = hotelRepository.findById(hotelId).get();
        Filial filial = hotel.getFilial();
        for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
            for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                List<Guest> guests = new ArrayList<>();
                if (checkouted)
                    guests = guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoom(dateFinish, dateStart, room);
                else
                    guests = guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndCheckoutedAndBedRoom(dateFinish, dateStart, checkouted, room);
                for (Guest guest : guests) {
                    FilialReportDTO record = new FilialReportDTO();
                    record.setFilial(filial.getName());
                    record.setHotel(hotel.getName());
                    record.setFlat(flat.getName());
                    record.setFloor(flat.getFloor());
                    record.setRoom(room.getId().toString());
                    if (guest.getCheckouted() != null)
                        record.setCheckouted(guest.getCheckouted() ? "+" : "-");
                    else record.setCheckouted("-");
                    record.setDateStart(dateFormatterDot.format(guest.getDateStart()));
                    record.setDateFinish(dateFormatterDot.format(guest.getDateFinish()));
                    if (guest.getEmployee() != null)
                        record.setTabnum(guest.getEmployee().getTabnum());
                    record.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
                    reportData.add(record);
                }
            }
        }

        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/FilialReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }
    public byte[] getFloorReport(Long hotelId, Integer floor, String dateStr) throws JRException, ParseException {
        Date date = dateTimeFormatter.parse(dateStr);
        List<FilialReportDTO> reportData = new ArrayList<>();
        Hotel hotel = hotelRepository.findById(hotelId).get();
        Filial filial = hotel.getFilial();
        for (Flat flat : flatRepository.findAllByHotelAndFloorOrderById(hotel, floor)) {
            for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                guestRepository.findAllByBedRoomAndCheckoutedAndDateStartLessThanEqualAndDateFinishGreaterThan(room, false, date, date).forEach(guest -> {
                    FilialReportDTO record = new FilialReportDTO();
                    record.setFilial(filial.getName());
                    record.setHotel(hotel.getName());
                    record.setFlat(flat.getName());
                    record.setFloor(flat.getFloor());
                    record.setRoom(room.getId().toString());
                    record.setDateStart(dateFormatterDot.format(guest.getDateStart()));
                    record.setDateFinish(dateFormatterDot.format(guest.getDateFinish()));
                    if (guest.getEmployee() != null)
                        record.setTabnum(guest.getEmployee().getTabnum());
                    record.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
                    reportData.add(record);
                });
            }
        }

        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/FilialReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }
    @Transactional
    public byte[] getMVZReportShort(Long empFilialId, Long filialId, String dateStart, String dateFinish) throws JRException, ParseException {
        List<MVZReportDTO> mvzReportDTOList = new ArrayList<>();
        List<MVZReportShortDTO> mvzReportShortDTOList = new ArrayList<>();
        Filial empFilial = filialRepository.getById(empFilialId);
        Filial filial = filialRepository.getById(filialId);
        Date minDate = dateTimeFormatter.parse(dateStart + " 23:59");
        Date maxDate = dateTimeFormatter.parse(dateFinish + " 23:59");
        int count = 1;
        for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndEmployeeNotNull(maxDate, minDate)) {
            if (guest.getContract() != null) {
                if (guest.getContract().getReason().getId() == 3 || guest.getContract().getReason().getId() == 4) {
                    if (!Objects.equals(guest.getEmployee().getIdFilial(), empFilial.getCode())) continue;
                    MVZ mvz = mvzRepository.findById(guest.getEmployee().getMvzId());
                    if (mvz != null) {
                        MVZReportDTO mvzReportDTO = new MVZReportDTO();
                        mvzReportDTO.setId(String.valueOf(count));
                        mvzReportDTO.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
                        mvzReportDTO.setHotel(guest.getBed().getRoom().getFlat().getHotel().getName());
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
                        mvzReportDTO.setDaysCount(Math.toIntExact(daysCount));
                        mvzReportDTO.setTabnum(guest.getEmployee().getTabnum().toString());
                        mvzReportDTO.setMvz(mvz.getId());
                        mvzReportDTO.setMvzName(mvz.getName());
                        mvzReportDTO.setGuestFilial(filialRepository.findByCode(guest.getEmployee().getIdFilial()).getName());
                        mvzReportDTO.setFilial(filial.getName());
                        mvzReportDTO.setOrgUnit(mvz.getOrganization());
                        mvzReportDTO.setReason(guest.getContract().getReason().getName());
                        mvzReportDTO.setBilling(guest.getContract().getBilling());
                        mvzReportDTOList.add(mvzReportDTO);
                        count++;
                    }
                }
            }
        }

        List<MVZReportDTO> uniqMvzReportDTOList = new ArrayList<>();
        for (MVZReportDTO mvzReportDTO : mvzReportDTOList) {
            boolean exist = false;
            for (MVZReportDTO uniqMvzReportDTO : uniqMvzReportDTOList) {
                if (uniqMvzReportDTO.getMvz().equals(mvzReportDTO.getMvz())) {
                    exist = true;
                }
            }
            if (!exist)
                uniqMvzReportDTOList.add(mvzReportDTO);
        }

        for (Hotel hotel : hotelRepository.findAllByFilial(filial)) {
            for (MVZReportDTO mvzReportDTO : uniqMvzReportDTOList) {
                MVZReportShortDTO mvzReportShortDTO = new MVZReportShortDTO();
                mvzReportShortDTO.setHotel(hotel.getName());
                mvzReportShortDTO.setMvz(mvzReportDTO.getMvz());
                mvzReportShortDTO.setMvzName(mvzReportDTO.getMvzName());
                AtomicInteger days = new AtomicInteger();
                mvzReportDTOList.forEach(mvzReportDTO1 -> {
                    if (mvzReportDTO1.getHotel().equals(hotel.getName()) && mvzReportDTO1.getMvz().equals(mvzReportDTO.getMvz())) {
                        days.addAndGet(mvzReportDTO1.getDaysCount());
                    }
                });
                if (days.get() != 0) {
                    mvzReportShortDTO.setDays(days.get());
                    mvzReportShortDTOList.add(mvzReportShortDTO);
                }
            }
        }


        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/MVZReportShort.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(mvzReportShortDTOList);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("periodStart", dateStart);
        parameters.put("periodFinish", dateFinish);
        parameters.put("filial", filial.getName());
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }
    @Transactional
    public byte[] getCheckoutReport(Long guestId, Integer roomNumber, String periodStartStr, String periodEndStr) throws JRException, ParseException {
        Date dateStart = dateTimeFormatter.parse(periodStartStr);
        Date dateFinish = dateTimeFormatter.parse(periodEndStr);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        Guest guest = guestRepository.getById(guestId);
        Room room = guest.getBed().getRoom();
        Flat flat = room.getFlat();
        Hotel hotel = flat.getHotel();
        Filial filial = hotel.getFilial();
        JasperReport jasperReport;
        if (filial.getId() == 930L)
            jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/CheckoutReportUEZS.jrxml"));
        else
            jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/CheckoutReportGeneral.jrxml"));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("filial", filial.getName());
        parameters.put("hotelName", hotel.getName());
        parameters.put("hotelLocation", hotel.getLocation());
        parameters.put("reportNumber", filial.getCode().toString() + "/" + guest.getId().toString()); // 1930 + id guest
        parameters.put("reportDate", dateFormat.format(new Date()));
        parameters.put("fioFull", guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
        parameters.put("fio", guest.getLastname() + " " + guest.getFirstname().charAt(0) + ". " + guest.getSecondName().charAt(0) + ".");
        if (guest.getEmployee() != null) {
            parameters.put("tabnum", guest.getEmployee().getTabnum().toString());
        } else parameters.put("tabnum", "");
        if (guest.getEmployee() != null) {
            Filial guestFilial = filialRepository.findByCode(guest.getEmployee().getIdFilial());
            String guestJob = guestFilial.getName();
            parameters.put("guestJob", guestJob);
        } else {
            if (guest.getOrganization() != null)
                parameters.put("guestJob", guest.getOrganization().getName());
            else parameters.put("guestJob", "");
        }
        String username = ru.sgp.utils.SecurityManager.getCurrentUser();
        User user = userRepository.findByUsername(username);
        Employee respEmp = user.getEmployee();
        parameters.put("respName", respEmp.getLastname() + " " + respEmp.getFirstname().charAt(0) + ". " + respEmp.getSecondName().charAt(0) + ".");
        parameters.put("roomNumber", flat.getName());
        parameters.put("korpusNumber", "");
        parameters.put("checkInDate", dateFormat.format(dateStart));
        parameters.put("checkInTime", timeFormat.format(dateStart));
        parameters.put("checkOutDate", dateFormat.format(dateFinish));
        parameters.put("checkOutTime", timeFormat.format(dateFinish));
        if (dateFormat.format(guest.getDateFinish()).equals(dateFormat.format(dateFinish))) {
            Date cuttedGuestStartDate = dateFormatter.parse(dateTimeFormatter.format(dateStart));
            Date cuttedGuestFinishDate = dateFormatter.parse(dateTimeFormatter.format(dateFinish));
            String daysCount = String.valueOf(TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS));
            if (filial.getId() == 930L) {
                Double hoursCount = (double) TimeUnit.MINUTES.convert(dateFinish.getTime() - dateStart.getTime(), TimeUnit.MILLISECONDS) / 60;
                parameters.put("daysCount", String.valueOf((int)(hoursCount /6)*0.25));
            } else
                parameters.put("daysCount", daysCount.equals("0") ? "1" : daysCount);
        } else {
            Date cuttedGuestStartDate = dateFormatter.parse(dateTimeFormatter.format(dateStart));
            Date cuttedGuestFinishDate = dateFormatter.parse(dateTimeFormatter.format(dateFinish));
            String daysCount = String.valueOf(TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS) + 1);
            if (filial.getId() == 930L) {
                Long hoursCount = TimeUnit.MINUTES.convert(dateFinish.getTime() - dateStart.getTime(), TimeUnit.MILLISECONDS);
                parameters.put("daysCount", String.valueOf((int)((double)hoursCount /6)*0.25));
            } else
                parameters.put("daysCount", daysCount.equals("0") ? "1" : daysCount);
        }
        parameters.put("date", dateFormat.format(new Date()));
        if (filial.getId() == 930L) {
            parameters.put("date", dateFormat.format(dateFinish));
            parameters.put("reportDate", dateFormat.format(dateFinish));
        }
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters);
        return export(jasperPrint);
    }

}
