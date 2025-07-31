package ru.sgp.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.fill.JRTemplatePrintText;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.integration.BookReportDTO;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
    @Autowired
    ReservationRepository reservationRepository;
    @Autowired
    EmployeeRepository employeeRepository;
    @Autowired
    private GuestExtraRepository guestExtraRepository;
    @Autowired
    private BookReportRepository bookReportRepository;
    @Autowired
    private RecordBookReportRepository recordBookReportRepository;

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat dateFormatterDot = new SimpleDateFormat("dd.MM.yyyy");
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    private final SimpleDateFormat dateTimeDotFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public Float simpleCalculateDaysCount(Guest guest) throws ParseException {
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
        Long daysCount = 0L;
        Date cuttedGuestStartDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateStart()));
        Date cuttedGuestFinishDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateFinish()));
        daysCount = TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS) + 1;
        if (daysCount == 0) daysCount = 1L;
        return daysCount.floatValue();
    }

    public Float calculateDaysCount(Date minDate, Date maxDate, Guest guest) throws ParseException {
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
        Date midday = timeFormatter.parse("12:00");
        Long daysCount = 0L;
        Float added = 0f;
        Date cuttedGuestStartDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateStart()));
        Date cuttedGuestFinishDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateFinish()));
        Date cuttedPeriodStartDate = dateFormatter.parse(dateTimeFormatter.format(minDate.getTime()));
        Date cuttedPeriodFinishDate = dateFormatter.parse(dateTimeFormatter.format(maxDate.getTime()));
        // Все дни заданного периода
        if (guest.getDateStart().before(minDate) && guest.getDateFinish().after(maxDate)) {
            daysCount = TimeUnit.DAYS.convert(cuttedPeriodFinishDate.getTime() - cuttedPeriodStartDate.getTime(), TimeUnit.MILLISECONDS) + 1;
            // Если ранний заезд
            if (timeFormatter.parse(timeFormatter.format(minDate)).before(midday))
                added += 0.5f;
            // Если поздний выезд
            if (timeFormatter.parse(timeFormatter.format(maxDate)).after(midday))
                added += 0.5f;
        } // Все дни внутри периода
        else if (guest.getDateStart().after(minDate) && guest.getDateFinish().before(maxDate)) {
            daysCount = TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS);
            // Если ранний заезд
            if (timeFormatter.parse(timeFormatter.format(guest.getDateStart())).before(midday))
                added += 0.5f;
            // Если поздний выезд
            if (timeFormatter.parse(timeFormatter.format(guest.getDateFinish())).after(midday))
                added += 0.5f;
        } // Если Дата начала не входит в период то считает с начала периода по дату выезда
        else if (guest.getDateStart().before(minDate) && guest.getDateFinish().before(maxDate)) {
            daysCount = TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedPeriodStartDate.getTime(), TimeUnit.MILLISECONDS);
            // Если ранний заезд
            if (timeFormatter.parse(timeFormatter.format(minDate.getTime())).before(midday))
                added += 0.5f;
            // Если поздний выезд
            if (timeFormatter.parse(timeFormatter.format(guest.getDateFinish())).after(midday))
                added += 0.5f;
        } // Если Дата выселения не входит в период то сичтает с заселения по конца периода
        else if (guest.getDateStart().after(minDate) && guest.getDateFinish().after(maxDate)) {
            daysCount = TimeUnit.DAYS.convert(cuttedPeriodFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS) + 1;
            // Если ранний заезд
            if (timeFormatter.parse(timeFormatter.format(guest.getDateStart())).before(midday))
                added += 0.5f;
            // Если поздний выезд
            if (timeFormatter.parse(timeFormatter.format(maxDate.getTime())).after(midday))
                added += 0.5f;
        }
        if (daysCount == 0) daysCount = 1L;
//        if (guest.getOrganization() != null && guest.getContract() != null)
//            if (guest.getOrganization().getId() == 11L && guest.getContract().getReason().getId() == 4L) // Расчет только для Адм и основания Командировка
//                return daysCount.floatValue() + added;
        return daysCount.floatValue();
    }

    private static byte[] export(final JasperPrint print, String type) throws JRException {
        if (print == null)
            return null;
        final Exporter exporter;
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean html = false;
        switch (type) {
            case "xlsx":
                exporter = new JRXlsxExporter();
                break;
            case "pdf":
                exporter = new JRPdfExporter();
                break;
            default:
                throw new JRException("Unknown report format: " + type);
        }

        if (!html) {
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        }

        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.exportReport();

        return out.toByteArray();
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
            if (guest.getDateStart() != null)
                guestReportDTO.setDateStart(dateTimeFormatter.format(guest.getDateStart()));
            if (guest.getDateFinish() != null)
                guestReportDTO.setDateFinish(dateTimeFormatter.format(guest.getDateFinish()));
            guestReportDTO.setRoom(guest.getBed().getRoom().getFlat().getName());
            guestReportDTO.setHotel(guest.getBed().getRoom().getFlat().getHotel().getName());
            guestReportDTO.setFilial(guest.getBed().getRoom().getFlat().getHotel().getFilial().getName());
            if (guest.getOrganization() != null)
                guestReportDTO.setOrg(guest.getOrganization().getName());
            if (guest.getRegPoMestu() != null) guestReportDTO.setRepPoMestu(guest.getRegPoMestu() ? "+" : "-");
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
        return export(jasperPrint, "xlsx");
    }

    @Transactional
    public byte[] getMonthReportByFilial(Long empFilialId, Long responsibilityId, Long reasonId, String dateStart, String dateFinish, String billing) throws JRException, ParseException {
        List<MonthReportDTO> reportData = new ArrayList<>();
        Filial empFilial = filialRepository.findById(empFilialId).orElse(null);
        Reason reason = reasonRepository.getById(reasonId);
        Responsibilities responsibilities = responsibilitiesRepository.getById(responsibilityId);
        Filial filial = responsibilities.getHotel().getFilial();
        Float daysCountSummary = 0f;
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
            if (guest.getContract() != null) {
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
            Float daysCount = calculateDaysCount(minDate, maxDate, guest);
            daysCountSummary += daysCount;
            monthReportDTO.setDaysCount(daysCount);
            if (!contracts.isEmpty()) {
                if (contracts.get(0).getCost() % 1 == 0)
                    monthReportDTO.setCostFromContract(String.valueOf(contracts.get(0).getCost().intValue()));
                else
                    monthReportDTO.setCostFromContract(contracts.get(0).getCost().toString().replace('.', ','));

                // Установка цены в правильном формате обязательное два знака после зпт
                String cost = String.valueOf(daysCount * contracts.get(0).getCost());
                if (cost.split("\\.").length > 1) {
                    String intPart = cost.split("\\.")[0];
                    String doublePart = cost.split("\\.")[1];
                    if (doublePart.length() == 1) doublePart += "0";
                    cost = intPart + "," + doublePart;
                    monthReportDTO.setCost(cost);
                } else {
                    monthReportDTO.setCost(cost);
                }
                // -----

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
            for (Guest member : family) {
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
                Float daysCountMember = calculateDaysCount(minDate, maxDate, member);
                if (daysCountMember == 0) daysCountMember = 1f;
                daysCountSummary += daysCountMember;
                memberDTO.setDaysCount(daysCountMember);
                if (!contractsMember.isEmpty()) {
                    if (contractsMember.get(0).getCost() % 1 == 0)
                        memberDTO.setCostFromContract(String.valueOf(contractsMember.get(0).getCost().intValue()));
                    else
                        memberDTO.setCostFromContract(contractsMember.get(0).getCost().toString().replace('.', ','));

                    memberDTO.setCost(String.valueOf(daysCountMember * contractsMember.get(0).getCost()));
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
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/monthReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ceh", "");
        parameters.put("date", dateFormatterDot.format(new Date()));
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
            parameters.put("filialBossPost", empFilial.getId() == 925L ? "Начальник центра" : bossPost);
            parameters.put("filialBossName", bossF + " " + bossN.charAt(0) + ". " + bossS.charAt(0) + ".");
        } else {
            parameters.put("filialBossPost", "");
            parameters.put("filialBossName", "");
        }
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint, "xlsx");
    }

    @Transactional
    public byte[] getReestrRabotReport(Long hotelId, Long responsibilityId, String dateStart, String dateFinish, String ukgBoss, String workType, Boolean isOrganization, List<Long> reasonList) throws JRException, ParseException {
        Responsibilities responsibility = responsibilitiesRepository.getById(responsibilityId);
        Hotel hotelM = hotelRepository.getById(hotelId);
        Date minDate = dateTimeFormatter.parse(dateStart + " 23:59");
        Date maxDate = dateTimeFormatter.parse(dateFinish + " 23:59");
        Filial filial = hotelM.getFilial();
        AtomicReference<Float> summaryCost = new AtomicReference<>(0f);
        List<ReestrRabotReportDTO> data = new ArrayList<>();
        for (Hotel hotel : hotelRepository.findAllByMvz(hotelM.getMvz())) {
            for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotel(maxDate, minDate, hotel)) {
                if (guest.getContract() == null) continue;
                if (reasonList.stream().noneMatch(r -> Objects.equals(r, guest.getContract().getReason().getId())))
                    continue; // Пропускаем если нет такого основания
                if ((guest.getEmployee() == null && guest.getFamilyMemberOfEmployee() == null) && !isOrganization)
                    continue;  // Пропускаем если работник организации в режиме работников ГТС
                if (guest.getEmployee() != null && isOrganization)
                    continue; // Пропускаем если работник в режиме организаций
                if (guest.getOrganization() == null && isOrganization)
                    continue; // Пропускаем если какого-то хуя организация не указана и не работник ГТС
                if (guest.getOrganization() != null && isOrganization && guest.getOrganization().getId() == 11L)
                    continue; // Пропускаем это работник ГТС у которого не заполнено поле isEmployee, но организация стоит ГТС
                if (guest.getEmployee() != null)
                    if (Objects.equals(guest.getEmployee().getIdFilial(), filial.getCode()))
                        continue; // Пропускаем если работник ГТС из того же филиала что и филиал отправитель
                if (guest.getContract() == null) continue;
                if (isOrganization && guest.getOrganization().getId() == 21L && guest.getContract().getBilling().length() > 15)
                    continue;
                Filial guestFilial = (!isOrganization && guest.getFamilyMemberOfEmployee() == null) ? filialRepository.findByCode(guest.getEmployee().getIdFilial()) : null;
                if (guestFilial == null && guest.getFamilyMemberOfEmployee() != null)
                    guestFilial = filialRepository.findByCode(guest.getFamilyMemberOfEmployee().getIdFilial());
                boolean isRecordNew;
                if (!isOrganization) {
                    Filial finalGuestFilial = guestFilial;
                    isRecordNew = data.stream().noneMatch(item -> item.getBs().equals(finalGuestFilial.getCode().toString()));
                } else
                    isRecordNew = data.stream().noneMatch(item -> item.getOrganization().equals(guest.getOrganization().getName()));
                if (isRecordNew) {
                    ReestrRabotReportDTO record = new ReestrRabotReportDTO();
                    Float daysCount = calculateDaysCount(minDate, maxDate, guest);
                    if (daysCount == 0) daysCount = 1f;
                    record.setWorkSize(daysCount);
                    if (isOrganization) {
                        record.setOrganization(guest.getOrganization().getName());
                        record.setBilling(guest.getContract().getBilling());
                    } else {
                        record.setBs(guestFilial.getCode().toString());
                        record.setFilial(guestFilial.getName());
                    }
                    record.setCost(daysCount * guest.getContract().getCost());
                    summaryCost.updateAndGet(v -> v + record.getCost());
                    data.add(record);
                } else {
                    Filial finalGuestFilial1 = guestFilial;
                    data = data.stream().map(item -> {
                        boolean isRecordSame;
                        if (isOrganization)
                            isRecordSame = item.getOrganization().equals(guest.getOrganization().getName());
                        else isRecordSame = item.getBs().equals(finalGuestFilial1.getCode().toString());
                        if (isRecordSame) {
                            Float daysCount = null;
                            try {
                                daysCount = calculateDaysCount(minDate, maxDate, guest);
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            item.setWorkSize(item.getWorkSize() + daysCount);
                            item.setCost(item.getCost() + (daysCount * guest.getContract().getCost()));
                            summaryCost.updateAndGet(v -> v + item.getCost());
                        }
                        return item;
                    }).collect(Collectors.toList());
                }
            }
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(isOrganization ? JRLoader.getResourceInputStream("reports/ReestrRabotReportOrganization.jrxml") : JRLoader.getResourceInputStream("reports/ReestrRabotReport.jrxml"));
        data.sort((rec1, rec2) -> {
            if (rec1.getBilling() != null && rec2.getBilling() != null) {
                if (rec1.getBilling().charAt(0) == rec2.getBilling().charAt(0)) return 0;
                return rec1.getBilling().charAt(0) > rec2.getBilling().charAt(0) ? 1 : -1;
            } else return 0;
        });
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("filialSender", filial.getName());
        parameters.put("period", dateFormatterDot.format(dateFormatter.parse(dateStart)) + " - " + dateFormatterDot.format(dateFormatter.parse(dateFinish)));
        parameters.put("mvzSender", hotelM.getMvz());
        parameters.put("month", minDate.getMonth());
        parameters.put("workType", "733000");
        parameters.put("costSum", summaryCost.get());
        String bossF = hotelM.getFilial().getBoss().split(" ")[0];
        String bossN = hotelM.getFilial().getBoss().split(" ")[1];
        String bossS = hotelM.getFilial().getBoss().split(" ")[2];
        String bossPost = hotelM.getFilial().getBoss().split(" ")[3] + " " + hotelM.getFilial().getBoss().split(" ")[4];
        parameters.put("bossPost", bossPost);
        parameters.put("boss", bossF + " " + bossN.charAt(0) + ". " + bossS.charAt(0) + ".");
        parameters.put("ukgBoss", ukgBoss);
        parameters.put("commendant", responsibility.getEmployee().getLastname() + " " + responsibility.getEmployee().getFirstname().charAt(0) + ". " + responsibility.getEmployee().getSecondName().charAt(0) + ".");
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        List<JRPrintElement> workTypeList = new ArrayList<>();
        for (JRPrintElement jrPrintElement : jasperPrint.getPages().get(0).getElements()) {
            if (((JRTemplatePrintText) jrPrintElement).getFullText().length() > 1 && workType.toLowerCase().contains(((JRTemplatePrintText) jrPrintElement).getFullText().toLowerCase())) {
                workTypeList.add(jrPrintElement);
            }
        }
        List<JRPrintElement> workTypeGroup = new ArrayList<>();
        for (JRPrintElement el : workTypeList) {
            if (workTypeGroup.isEmpty()) {
                workTypeGroup.add(el);
                continue;
            }
            if (el.getY() - workTypeGroup.get(workTypeGroup.size() - 1).getY() == 20) {
                workTypeGroup.add(el);
                if (el.getPrintElementId() == workTypeList.get(workTypeList.size() - 1).getPrintElementId()) { // if last
                    // group completed
                    workTypeGroup.get(0).setHeight(workTypeGroup.size() * 20);
                    for (JRPrintElement jrPrintElement : workTypeGroup) {
                        if (jrPrintElement.getPrintElementId() != workTypeGroup.get(0).getPrintElementId()) {
                            jrPrintElement.setHeight(0);
                            ((JRTemplatePrintText) jrPrintElement).setText("");
                            ((JRTemplatePrintText) jrPrintElement).setX(5000);
                        }
                    }
                }
            } else {
                // group completed
                workTypeGroup.get(0).setHeight(workTypeGroup.size() * 20);
                for (JRPrintElement jrPrintElement : workTypeGroup) {
                    if (jrPrintElement.getPrintElementId() != workTypeGroup.get(0).getPrintElementId()) {
                        jrPrintElement.setHeight(0);
                        ((JRTemplatePrintText) jrPrintElement).setText("");
                        ((JRTemplatePrintText) jrPrintElement).setX(5000);
                    }
                }
                workTypeGroup = new ArrayList<>();
                workTypeGroup.add(el);
                if (el.getPrintElementId() == workTypeList.get(workTypeList.size() - 1).getPrintElementId()) { // if last
                    // group completed
                    workTypeGroup.get(0).setHeight(workTypeGroup.size() * 20);
                    for (JRPrintElement jrPrintElement : workTypeGroup) {
                        if (jrPrintElement.getPrintElementId() != workTypeGroup.get(0).getPrintElementId()) {
                            jrPrintElement.setHeight(0);
                            ((JRTemplatePrintText) jrPrintElement).setText("");
                            ((JRTemplatePrintText) jrPrintElement).setX(5000);
                        }
                    }
                }
            }
        }
        return export(jasperPrint, "xlsx");
    }

    @Transactional
    public byte[] getMonthReportByUttist(Long empFilialId, Long responsibilityId, Long reasonId, String dateStart, String dateFinish, String billing, String ceh) throws JRException, ParseException {
        List<MonthReportDTO> reportData = new ArrayList<>();
        Filial empFilial = filialRepository.findById(empFilialId).orElse(null);
        Reason reason = reasonRepository.getById(reasonId);
        Responsibilities responsibilities = responsibilitiesRepository.getById(responsibilityId);
        Filial filial = responsibilities.getHotel().getFilial();
        DecimalFormat df = new DecimalFormat("#.##");
        Float daysCountSummary = 0f;
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
            if (guest.getContract() != null) {
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
            Float daysCount = calculateDaysCount(minDate, maxDate, guest);
            daysCountSummary += daysCount;
            monthReportDTO.setDaysCount(daysCount);
            if (!contracts.isEmpty()) {
                Float cost = Float.valueOf(df.format(contracts.get(0).getCost()).replace(',', '.'));
                if (contracts.get(0).getCost() % 1 == 0)
                    monthReportDTO.setCostFromContract(String.valueOf(contracts.get(0).getCost().intValue()));
                else
                    monthReportDTO.setCostFromContract(contracts.get(0).getCost().toString().replace('.', ','));

                monthReportDTO.setCost(String.valueOf(daysCount * cost));
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
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/monthReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("date", dateFormatterDot.format(new Date()));
        parameters.put("periodStart", dateStart);
        parameters.put("ceh", ceh);
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
        return export(jasperPrint, "xlsx");
    }

    @Transactional
    public byte[] getMonthReportByOrganization(Long orgId, Long responsibilityId, Long reasonId, String dateStart, String dateFinish, String billing) throws JRException, ParseException {
        List<MonthReportDTO> reportData = new ArrayList<>();
        Reason reason = reasonRepository.getById(reasonId);
        Organization organization = organizationRepository.getById(orgId);
        Responsibilities responsibilities = responsibilitiesRepository.getById(responsibilityId);
        Filial filial = responsibilities.getHotel().getFilial();
        DecimalFormat df = new DecimalFormat("#.##");
        Float daysCountSummary = 0f;
        Double costSummary = 0.0;
        Date minDate = dateTimeFormatter.parse(dateStart + " 23:59");
        Date maxDate = dateTimeFormatter.parse(dateFinish + " 23:59");
        int count = 1;
        for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotelAndOrganization(maxDate, minDate, responsibilities.getHotel(), organization)) {
            if (guest.getEmployee() != null) continue;
            if (guest.getContract() == null) continue;
            if (!Objects.equals(guest.getContract().getReason().getId(), reason.getId())) continue;
            if (!guest.getContract().getBilling().equals(billing)) continue;
            //List<Contract> contracts = contractRepository.findAllByFilialAndHotelAndReasonAndOrganization(filial, guest.getBed().getRoom().getFlat().getHotel(), guest.getContract().getReason(), guest.getOrganization());
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
            Float daysCount = calculateDaysCount(minDate, maxDate, guest);
            daysCountSummary += daysCount;
            monthReportDTO.setDaysCount(daysCount);
//            if (!contracts.isEmpty()) {
//                Float cost = Float.valueOf(df.format(contracts.get(0).getCost()).replace(',', '.'));
//                if (contracts.get(0).getCost() % 1 == 0)
//                    monthReportDTO.setCostFromContract(String.valueOf(contracts.get(0).getCost().intValue()));
//                else
//                    monthReportDTO.setCostFromContract(contracts.get(0).getCost().toString().replace('.', ','));
//                monthReportDTO.setCost(String.valueOf(Float.valueOf(String.format("%.2f", daysCount * cost).replace(',', '.'))));
//                costSummary += daysCount * cost;
//            } else {
//                // Тут если нету договора с организацией ставим ее как сторонюю и ищем договор с ней (только налик)
//                Reason other = reasonRepository.getById(5L);
//                Organization podryadchiki = organizationRepository.getById(2L);
//                Optional<Contract> contractOptional = contractRepository.findByHotelAndOrganizationAndReasonAndYear(guest.getBed().getRoom().getFlat().getHotel(), podryadchiki, other, 2025); // TODO get current year
//                if (contractOptional.isPresent()) {
//                    Contract contract = contractOptional.get();
//                    Float cost = Float.valueOf(df.format(contract.getCost()).replace(',', '.'));
//                    if (contract.getCost() % 1 == 0)
//                        monthReportDTO.setCostFromContract(String.valueOf(contract.getCost().intValue()));
//                    else
//                        monthReportDTO.setCostFromContract(contract.getCost().toString().replace('.', ','));
//                    monthReportDTO.setCost(String.valueOf(Float.valueOf(String.format("%.2f", daysCount * cost).replace(',', '.'))));
//                    costSummary += daysCount * cost;
//                }
//            }

            if (guest.getContract() != null) {
                Float cost = guest.getContract().getCost();
                if (cost % 1 == 0)
                    monthReportDTO.setCostFromContract(String.valueOf(cost.intValue()));
                else
                    monthReportDTO.setCostFromContract(cost.toString().replace('.', ','));
                monthReportDTO.setCost(String.valueOf(Float.valueOf(String.format("%.2f", daysCount * cost).replace(',', '.'))));
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
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/monthReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ceh", "");
        parameters.put("date", dateFormatterDot.format(new Date()));
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
        return export(jasperPrint, "xlsx");
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
        return export(jasperPrint, "xlsx");
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
        return export(jasperPrint, "xlsx");
    }

    public byte[] getAllContractReport() throws JRException {
        List<ContractReportDTO> reportData = new ArrayList<>();
        for (Contract contract : contractRepository.findAllByOrderById()) {
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
            contractDTO.setYear(contract.getYear());
            reportData.add(contractDTO);
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/Contracts.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint, "xlsx");
    }

    public byte[] getFilialReport(Long filialId, boolean checkouted, String dateStartStr, String dateFinishStr, Boolean fullReport) throws JRException, ParseException {
        List<FilialReportDTO> reportData = new ArrayList<>();
        List<Filial> filialList = new ArrayList<>();
        Date dateStart = dateFormatter.parse(dateStartStr);
        Date dateFinish = dateFormatter.parse(dateFinishStr);
        if (fullReport) {
            filialList = filialRepository.findAll();
        } else {
            Filial filial = filialRepository.findById(filialId).orElse(null);
            filialList.add(filial);
        }
        for (Filial filial : filialList) {
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
                            record.setPeriodStart(dateFormatterDot.format(dateStart));
                            record.setPeriodFinish(dateFormatterDot.format(dateFinish));
                            record.setDateStart(dateTimeDotFormatter.format(guest.getDateStart()));
                            record.setDateFinish(dateTimeDotFormatter.format(guest.getDateFinish()));
                            if (guest.getCheckouted() != null)
                                record.setCheckouted(guest.getCheckouted() ? "+" : "-");
                            else
                                record.setCheckouted("-");

                            Float daysCount = calculateDaysCount(dateStart, dateFinish, guest);

                            record.setNights(daysCount);
                            if (guest.getContract() != null) {
                                record.setReason(guest.getContract().getReason().getName());
                                record.setBilling(guest.getContract().getBilling());
                                record.setContract(guest.getContract().getDocnum());
                                record.setContractPrice(guest.getContract().getCost());
                                record.setPeriodPrice(daysCount * guest.getContract().getCost());
                                record.setCreditCard(guest.getCreditCard() == null ? "-" : guest.getCreditCard() ? "+" : "-");
                            } else {
                                record.setCreditCard("-");
                                record.setReason("");
                                record.setBilling("");
                                record.setContract("");
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
        return export(jasperPrint, "xlsx");
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
            record.setPeriodStart(dateFormatter.format(dateStart));
            record.setPeriodFinish(dateFormatter.format(dateFinish));
            record.setDateStart(dateFormatterDot.format(guest.getDateStart()));
            record.setDateFinish(dateFormatterDot.format(guest.getDateFinish()));
            if (guest.getCheckouted() == null) record.setCheckouted("-");
            else record.setCheckouted(guest.getCheckouted() ? "+" : "-");
            Float daysCount = null;
            try {
                daysCount = calculateDaysCount(dateStart, dateFinish, guest);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
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
        return export(jasperPrint, "xlsx");
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
        return export(jasperPrint, "xlsx");
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
        return export(jasperPrint, "xlsx");
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
        return export(jasperPrint, "xlsx");
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
        return export(jasperPrint, "xlsx");
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
        return export(jasperPrint, "xlsx");
    }

    @Transactional
    public byte[] getCheckoutReport(Long guestId, String periodStartStr, String periodEndStr, String format) throws JRException, ParseException {
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
        else if (filial.getId() == 917L)
            jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/CheckoutReportSamsonovskoe.jrxml"));
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
        Employee respEmp = null;
        if (user != null)
            respEmp = user.getEmployee();
        else {
            respEmp = new Employee();
            respEmp.setLastname("null");
            respEmp.setSecondName("null");
            respEmp.setFirstname("null");
        }

        // Устанавливаем должность из справочника должностей
        String comendantPost = "";
        if (respEmp.getIdPoststaff() != null) {
            Optional<Post> post = postRepository.findById(respEmp.getIdPoststaff().longValue());
            if (post.isPresent()) {
                comendantPost = post.get().getName();
                parameters.put("comendantPost", comendantPost);
            }
        }
        // -----

        // Устанавливаем должность из альт должностей
        if (user.getCustomPost() != null) {
            if (user.getCustomPost().trim().length() > 0) {
                parameters.put("comendantPost", user.getCustomPost());
            }
        }
        // -----

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
                parameters.put("daysCount", String.valueOf((int) (hoursCount / 6) * 0.25));
            } else
                parameters.put("daysCount", daysCount.equals("0") ? "1" : daysCount);
        } else {
            Date cuttedGuestStartDate = dateFormatter.parse(dateTimeFormatter.format(dateStart));
            Date cuttedGuestFinishDate = dateFormatter.parse(dateTimeFormatter.format(dateFinish));
            String daysCount = String.valueOf(TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS) + 1);
            if (filial.getId() == 930L) {
                Double hoursCount = (double) TimeUnit.MINUTES.convert(dateFinish.getTime() - dateStart.getTime(), TimeUnit.MILLISECONDS) / 60;
                parameters.put("daysCount", String.valueOf((int) (hoursCount / 6) * 0.25));
            } else
                parameters.put("daysCount", daysCount.equals("0") ? "1" : daysCount);
        }
        parameters.put("date", dateFormat.format(new Date()));
        if (filial.getId() == 930L) {
            parameters.put("date", dateFormat.format(dateFinish));
            parameters.put("reportDate", dateFormat.format(dateFinish));
        }
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters);
        return export(jasperPrint, format);
    }

    // Отчеты для Ермака
    @Transactional
    public byte[] getReestrRabotErmakReport(Long hotelId, Long responsibilityId, String dateStart, String dateFinish, String ukgBoss, String workType, Boolean isOrganization, List<Long> reasonList) throws JRException, ParseException {
        Responsibilities responsibility = responsibilitiesRepository.getById(responsibilityId);
        Hotel hotelM = hotelRepository.getById(hotelId);
        Date minDate = dateTimeFormatter.parse(dateStart + " 23:59");
        Date maxDate = dateTimeFormatter.parse(dateFinish + " 23:59");
        Filial filial = hotelM.getFilial();
        List<ReestrRabotReportDTO> data = new ArrayList<>();
        for (Hotel hotel : hotelRepository.findAllByMvz(hotelM.getMvz())) {
            for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotel(maxDate, minDate, hotel)) {
                if (guest.getContract() == null) continue;
                if (reasonList.stream().noneMatch(r -> Objects.equals(r, guest.getContract().getReason().getId())))
                    continue; // Пропускаем если нет такого основания
                if (guest.getEmployee() == null && !isOrganization)
                    continue;  // Пропускаем если работник организации в режиме работников ГТС
                if (guest.getEmployee() != null && isOrganization)
                    continue; // Пропускаем если работник в режиме организаций
                if (guest.getOrganization() == null && isOrganization)
                    continue; // Пропускаем если какого-то хуя организация не указана и не работник ГТС
                if (guest.getOrganization() != null && isOrganization && guest.getOrganization().getId() == 11L)
                    continue; // Пропускаем это работник ГТС у которого не заполнено поле isEmployee, но организация стоит ГТС
                if (guest.getEmployee() != null)
                    if (Objects.equals(guest.getEmployee().getIdFilial(), filial.getCode()))
                        continue; // Пропускаем если работник ГТС из того же филиала что и филиал отправитель
                Filial guestFilial = !isOrganization ? filialRepository.findByCode(guest.getEmployee().getIdFilial()) : null;
                boolean isRecordNew;
                if (!isOrganization)
                    isRecordNew = data.stream().noneMatch(item -> item.getBs().equals(guestFilial.getCode().toString()));
                else
                    isRecordNew = data.stream().noneMatch(item -> item.getOrganization().equals(guest.getOrganization().getName()));
                if (isRecordNew) {
                    ReestrRabotReportDTO record = new ReestrRabotReportDTO();
                    if (isOrganization) {
                        record.setOrganization(guest.getOrganization().getName());
                        if (guest.getContract() != null) record.setBilling(guest.getContract().getBilling());
                    } else {
                        record.setBs(guestFilial.getCode().toString());
                        record.setFilial(guestFilial.getName());
                    }
                    record.setWorkSize(1f);
                    data.add(record);
                } else {
                    data = data.stream().map(item -> {
                        boolean isRecordSame;
                        if (isOrganization)
                            isRecordSame = item.getOrganization().equals(guest.getOrganization().getName());
                        else isRecordSame = item.getBs().equals(guestFilial.getCode().toString());
                        if (isRecordSame) {
                            item.setWorkSize(item.getWorkSize() + 1);
                        }
                        return item;
                    }).collect(Collectors.toList());
                }
            }
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(isOrganization ? JRLoader.getResourceInputStream("reports/ReestrRabotErmakReportOrganization.jrxml") : JRLoader.getResourceInputStream("reports/ReestrRabotErmakReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("filialSender", filial.getName());
        parameters.put("period", dateFormatterDot.format(dateFormatter.parse(dateStart)) + " - " + dateFormatterDot.format(dateFormatter.parse(dateFinish)));
        parameters.put("mvzSender", hotelM.getMvz());
        parameters.put("month", minDate.getMonth());
        parameters.put("workType", "733000");
        String bossF = hotelM.getFilial().getBoss().split(" ")[0];
        String bossN = hotelM.getFilial().getBoss().split(" ")[1];
        String bossS = hotelM.getFilial().getBoss().split(" ")[2];
        String bossPost = hotelM.getFilial().getBoss().split(" ")[3] + " " + hotelM.getFilial().getBoss().split(" ")[4];
        parameters.put("bossPost", bossPost);
        parameters.put("boss", bossF + " " + bossN.charAt(0) + ". " + bossS.charAt(0) + ".");
        parameters.put("ukgBoss", ukgBoss);
        parameters.put("commendant", responsibility.getEmployee().getLastname() + " " + responsibility.getEmployee().getFirstname().charAt(0) + ". " + responsibility.getEmployee().getSecondName().charAt(0) + ".");

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        List<JRPrintElement> workTypeList = new ArrayList<>();
        for (JRPrintElement jrPrintElement : jasperPrint.getPages().get(0).getElements()) {
            if (((JRTemplatePrintText) jrPrintElement).getFullText().length() > 1 && workType.toLowerCase().contains(((JRTemplatePrintText) jrPrintElement).getFullText().toLowerCase())) {
                workTypeList.add(jrPrintElement);
            }
        }
        List<JRPrintElement> workTypeGroup = new ArrayList<>();
        for (JRPrintElement el : workTypeList) {
            if (workTypeGroup.isEmpty()) {
                workTypeGroup.add(el);
                continue;
            }
            if (el.getY() - workTypeGroup.get(workTypeGroup.size() - 1).getY() == 15) {
                workTypeGroup.add(el);
                if (el.getPrintElementId() == workTypeList.get(workTypeList.size() - 1).getPrintElementId()) { // if last
                    // group completed
                    workTypeGroup.get(0).setHeight(workTypeGroup.size() * 15);
                    for (JRPrintElement jrPrintElement : workTypeGroup) {
                        if (jrPrintElement.getPrintElementId() != workTypeGroup.get(0).getPrintElementId()) {
                            jrPrintElement.setHeight(0);
                            ((JRTemplatePrintText) jrPrintElement).setText("");
                            ((JRTemplatePrintText) jrPrintElement).setX(5000);
                        }
                    }
                }
            } else {
                // group completed
                workTypeGroup.get(0).setHeight(workTypeGroup.size() * 15);
                for (JRPrintElement jrPrintElement : workTypeGroup) {
                    if (jrPrintElement.getPrintElementId() != workTypeGroup.get(0).getPrintElementId()) {
                        jrPrintElement.setHeight(0);
                        ((JRTemplatePrintText) jrPrintElement).setText("");
                        ((JRTemplatePrintText) jrPrintElement).setX(5000);
                    }
                }
                workTypeGroup = new ArrayList<>();
                workTypeGroup.add(el);
                if (el.getPrintElementId() == workTypeList.get(workTypeList.size() - 1).getPrintElementId()) { // if last
                    // group completed
                    workTypeGroup.get(0).setHeight(workTypeGroup.size() * 15);
                    for (JRPrintElement jrPrintElement : workTypeGroup) {
                        if (jrPrintElement.getPrintElementId() != workTypeGroup.get(0).getPrintElementId()) {
                            jrPrintElement.setHeight(0);
                            ((JRTemplatePrintText) jrPrintElement).setText("");
                            ((JRTemplatePrintText) jrPrintElement).setX(5000);
                        }
                    }
                }
            }
        }

        return export(jasperPrint, "xlsx");
    }

    public byte[] getPlanZaezdReport(Long hotelId, String dateStartStr, String dateFinishStr) throws ParseException, JRException {
        Date dayStart = dateTimeFormatter.parse(dateStartStr + " 00:01");
        Date dayFinish = dateTimeFormatter.parse(dateFinishStr + " 23:59");
        Hotel hotel = hotelRepository.getById(hotelId);
        JasperReport jasperReport;
        jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/PlanZaezd.jrxml"));
        List<PlanZaezdReportDTO> data = new ArrayList<>();
        for (Reservation reservation : reservationRepository.findAllByDateStartGreaterThanEqualAndDateStartLessThanEqualAndBedRoomFlatHotel(dayStart, dayFinish, hotel)) {
            PlanZaezdReportDTO record = new PlanZaezdReportDTO();
            record.setId(reservation.getId().intValue());
            record.setStatus("0");
            String lastname = reservation.getLastname() != null ? reservation.getLastname() : " ";
            String firstname = reservation.getFirstname() != null ? reservation.getFirstname() : " ";
            String secondname = reservation.getSecondName() != null ? reservation.getSecondName() : " ";
            record.setGuest(lastname + " " + firstname + " " + secondname);
            record.setDateStart(dateTimeFormatter.format(reservation.getDateStart()));
            record.setDateFinish(dateTimeFormatter.format(reservation.getDateFinish()));
            record.setFlatName(reservation.getBed().getRoom().getFlat().getName());
            record.setBilling(" ");
            data.add(record);
        }
        for (Guest guest : guestRepository.findAllByDateStartGreaterThanEqualAndDateStartLessThanEqualAndBedRoomFlatHotel(dayStart, dayFinish, hotel)) {
            PlanZaezdReportDTO record = new PlanZaezdReportDTO();
            record.setId(guest.getId().intValue());
            record.setStatus("1");
            String lastname = guest.getLastname() != null ? guest.getLastname() : " ";
            String firstname = guest.getFirstname() != null ? guest.getFirstname() : " ";
            String secondname = guest.getSecondName() != null ? guest.getSecondName() : " ";
            record.setGuest(lastname + " " + firstname + " " + secondname);
            record.setDateStart(dateTimeFormatter.format(guest.getDateStart()));
            record.setDateFinish(dateTimeFormatter.format(guest.getDateFinish()));
            record.setFlatName(guest.getBed().getRoom().getFlat().getName());
            if (guest.getContract() != null)
                record.setBilling(guest.getContract().getBilling());
            data.add(record);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("date", dateStartStr + " - " + dateFinishStr);
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint, "xlsx");
    }

    public byte[] getWeekReport(Long hotelId, String dateStartStr, String dateFinishStr) throws ParseException, JRException {
        Date monday = dateFormatter.parse(dateStartStr);
        LocalDate mondayLD = monday.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        LocalDate tuesdayLD = mondayLD.plusDays(1);
        Date tuesday = dateFormatter.parse(tuesdayLD.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        LocalDate wednesdayLD = mondayLD.plusDays(2);
        Date wednesday = dateFormatter.parse(wednesdayLD.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        LocalDate thursdayLD = mondayLD.plusDays(3);
        Date thursday = dateFormatter.parse(thursdayLD.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        LocalDate fridayLD = mondayLD.plusDays(4);
        Date friday = dateFormatter.parse(fridayLD.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        LocalDate saturdayLD = mondayLD.plusDays(5);
        Date saturday = dateFormatter.parse(saturdayLD.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        LocalDate sundayLD = mondayLD.plusDays(6);
        Date sunday = dateFormatter.parse(sundayLD.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        Hotel hotel = hotelRepository.getById(hotelId);
        JasperReport jasperReport;
        jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/WeekReport.jrxml"));
        List<WeekReportDTO> data = new ArrayList<>();
        int count = 1;
        Integer beds = bedRepository.countByRoomFlatHotelAndIsExtra(hotel, false);
        Integer guestCountMonday = 0;
        Integer guestCountTuesday = 0;
        Integer guestCountWednesday = 0;
        Integer guestCountThursday = 0;
        Integer guestCountFriday = 0;
        Integer guestCountSaturday = 0;
        Integer guestCountSunday = 0;
        for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
            WeekReportDTO record = new WeekReportDTO();
            record.setNumber(count);
            record.setFlatName(flat.getName());
            String guestListMonday = "";
            String reasonListMonday = "";
            String guestListTuesday = "";
            String reasonListTuesday = "";
            String guestListWednesday = "";
            String reasonListWednesday = "";
            String guestListThursday = "";
            String reasonListThursday = "";
            String guestListFriday = "";
            String reasonListFriday = "";
            String guestListSaturday = "";
            String reasonListSaturday = "";
            String guestListSunday = "";
            String reasonListSunday = "";
            Integer summary = 0;
            for (Reservation reservation : reservationRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlat(sunday, monday, flat)) {
                if (monday.getTime() >= reservation.getDateStart().getTime() && monday.getTime() <= reservation.getDateFinish().getTime()) { // Добавляем запись о броне в колонку ПН
                    summary += 1;
                    guestCountMonday += 1;
                    guestListMonday += reservation.getLastname() + " " + reservation.getFirstname() + " " + reservation.getSecondName() + "; ";
                    if (reservation.getNote() != null && !reservation.getNote().isEmpty())
                        reasonListMonday += reservation.getNote() + ";  ";
                }
                if (tuesday.getTime() >= reservation.getDateStart().getTime() && tuesday.getTime() <= reservation.getDateFinish().getTime()) { // Добавляем запись о броне в колонку ВТ
                    summary += 1;
                    guestCountTuesday += 1;
                    guestListTuesday += reservation.getLastname() + " " + reservation.getFirstname() + " " + reservation.getSecondName() + "; ";
                    if (reservation.getNote() != null && !reservation.getNote().isEmpty())
                        reasonListTuesday += reservation.getNote() + ";  ";
                }
                if (tuesday.getTime() >= reservation.getDateStart().getTime() && tuesday.getTime() <= reservation.getDateFinish().getTime()) { // Добавляем запись о броне в колонку СР
                    summary += 1;
                    guestCountWednesday += 1;
                    guestListWednesday += reservation.getLastname() + " " + reservation.getFirstname() + " " + reservation.getSecondName() + "; ";
                    if (reservation.getNote() != null && !reservation.getNote().isEmpty())
                        reasonListWednesday += reservation.getNote() + ";  ";
                }
                if (thursday.getTime() >= reservation.getDateStart().getTime() && thursday.getTime() <= reservation.getDateFinish().getTime()) { // Добавляем запись о броне в колонку ЧТ
                    summary += 1;
                    guestCountThursday += 1;
                    guestListThursday += reservation.getLastname() + " " + reservation.getFirstname() + " " + reservation.getSecondName() + "; ";
                    if (reservation.getNote() != null && !reservation.getNote().isEmpty())
                        reasonListThursday += reservation.getNote() + ";  ";
                }
                if (friday.getTime() >= reservation.getDateStart().getTime() && friday.getTime() <= reservation.getDateFinish().getTime()) { // Добавляем запись о броне в колонку ПТ
                    summary += 1;
                    guestCountFriday += 1;
                    guestListFriday += reservation.getLastname() + " " + reservation.getFirstname() + " " + reservation.getSecondName() + "; ";
                    if (reservation.getNote() != null && !reservation.getNote().isEmpty())
                        reasonListFriday += reservation.getNote() + ";  ";
                }
                if (saturday.getTime() >= reservation.getDateStart().getTime() && saturday.getTime() <= reservation.getDateFinish().getTime()) { // Добавляем запись о броне в колонку СБ
                    summary += 1;
                    guestCountSaturday += 1;
                    guestListSaturday += reservation.getLastname() + " " + reservation.getFirstname() + " " + reservation.getSecondName() + "; ";
                    if (reservation.getNote() != null && !reservation.getNote().isEmpty())
                        reasonListSaturday += reservation.getNote() + ";  ";
                }
                if (sunday.getTime() >= reservation.getDateStart().getTime() && sunday.getTime() <= reservation.getDateFinish().getTime()) { // Добавляем запись о броне в колонку ВС
                    summary += 1;
                    guestCountSunday += 1;
                    guestListSunday += reservation.getLastname() + " " + reservation.getFirstname() + " " + reservation.getSecondName() + "; ";
                    if (reservation.getNote() != null && !reservation.getNote().isEmpty())
                        reasonListSunday += reservation.getNote() + ";  ";
                }
            }
            for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlat(sunday, monday, flat)) {
                if (monday.getTime() >= guest.getDateStart().getTime() && monday.getTime() <= guest.getDateFinish().getTime()) { // Добавляем запись о броне в колонку ПН
                    summary += 1;
                    guestCountMonday += 1;
                    guestListMonday += guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName() + "; ";
                    if (guest.getNote() != null && !guest.getNote().isEmpty())
                        reasonListMonday += guest.getNote() + ";  ";
                }
                if (tuesday.getTime() >= guest.getDateStart().getTime() && tuesday.getTime() <= guest.getDateFinish().getTime()) { // Добавляем запись о броне в колонку ВТ
                    summary += 1;
                    guestCountTuesday += 1;
                    guestListTuesday += guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName() + "; ";
                    if (guest.getNote() != null && !guest.getNote().isEmpty())
                        reasonListTuesday += guest.getNote() + ";  ";
                }
                if (tuesday.getTime() >= guest.getDateStart().getTime() && tuesday.getTime() <= guest.getDateFinish().getTime()) { // Добавляем запись о броне в колонку СР
                    summary += 1;
                    guestCountWednesday += 1;
                    guestListWednesday += guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName() + "; ";
                    if (guest.getNote() != null && !guest.getNote().isEmpty())
                        reasonListWednesday += guest.getNote() + ";  ";
                }
                if (thursday.getTime() >= guest.getDateStart().getTime() && thursday.getTime() <= guest.getDateFinish().getTime()) { // Добавляем запись о броне в колонку ЧТ
                    summary += 1;
                    guestCountThursday += 1;
                    guestListThursday += guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName() + "; ";
                    if (guest.getNote() != null && !guest.getNote().isEmpty())
                        reasonListThursday += guest.getNote() + ";  ";
                }
                if (friday.getTime() >= guest.getDateStart().getTime() && friday.getTime() <= guest.getDateFinish().getTime()) { // Добавляем запись о броне в колонку ПТ
                    summary += 1;
                    guestCountFriday += 1;
                    guestListFriday += guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName() + "; ";
                    if (guest.getNote() != null && !guest.getNote().isEmpty())
                        reasonListFriday += guest.getNote() + ";  ";
                }
                if (saturday.getTime() >= guest.getDateStart().getTime() && saturday.getTime() <= guest.getDateFinish().getTime()) { // Добавляем запись о броне в колонку СБ
                    summary += 1;
                    guestCountSaturday += 1;
                    guestListSaturday += guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName() + "; ";
                    if (guest.getNote() != null && !guest.getNote().isEmpty())
                        reasonListSaturday += guest.getNote() + ";  ";
                }
                if (sunday.getTime() >= guest.getDateStart().getTime() && sunday.getTime() <= guest.getDateFinish().getTime()) { // Добавляем запись о броне в колонку ВС
                    summary += 1;
                    guestCountSunday += 1;
                    guestListSunday += guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName() + "; ";
                    if (guest.getNote() != null && !guest.getNote().isEmpty())
                        reasonListSunday += guest.getNote() + ";  ";
                }
            }
            record.setGuestListMonday(guestListMonday);
            record.setReasonListMonday(reasonListMonday);
            record.setGuestListTuesday(guestListTuesday);
            record.setReasonListTuesday(reasonListTuesday);
            record.setGuestListWednesday(guestListWednesday);
            record.setReasonListWednesday(reasonListWednesday);
            record.setGuestListThursday(guestListThursday);
            record.setReasonListThursday(reasonListThursday);
            record.setGuestListFriday(guestListFriday);
            record.setReasonListFriday(reasonListFriday);
            record.setGuestListSaturday(guestListSaturday);
            record.setReasonListSaturday(reasonListSaturday);
            record.setGuestListSunday(guestListSunday);
            record.setReasonListSunday(reasonListSunday);
            record.setSummary(summary);
            data.add(record);
            count++;
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dateStart", dateFormatterDot.format(monday));
        parameters.put("dateFinish", dateFormatterDot.format(sunday));
        parameters.put("hotelBeds", String.valueOf(beds));
        parameters.put("beds", String.valueOf(beds * 7));
        BigDecimal bd = new BigDecimal(Float.toString((float) (guestCountMonday + guestCountThursday + guestCountWednesday + guestCountTuesday + guestCountFriday + guestCountSaturday + guestCountSunday) / (beds * 7) * 100));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        parameters.put("percent", String.valueOf(bd.toString()));
        parameters.put("dateMonday", dateFormatterDot.format(monday));
        parameters.put("dateTuesday", dateFormatterDot.format(tuesday));
        parameters.put("dateWednesday", dateFormatterDot.format(wednesday));
        parameters.put("dateThursday", dateFormatterDot.format(thursday));
        parameters.put("dateFriday", dateFormatterDot.format(friday));
        parameters.put("dateSaturday", dateFormatterDot.format(saturday));
        parameters.put("dateSunday", dateFormatterDot.format(sunday));
        parameters.put("countMondayReservation", guestCountMonday);
        parameters.put("countTuesdayReservation", guestCountTuesday);
        parameters.put("countWednesdayReservation", guestCountWednesday);
        parameters.put("countThursdayReservation", guestCountThursday);
        parameters.put("countFridayReservation", guestCountFriday);
        parameters.put("countSaturdayReservation", guestCountSaturday);
        parameters.put("countSundayReservation", guestCountSunday);
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint, "xlsx");
    }

    public byte[] getExtrasReport(Long hotelId, String dateStartStr, String dateFinishStr) throws JRException, ParseException {
        Date dateStart = dateFormatter.parse(dateStartStr);
        Date dateFinish = dateFormatter.parse(dateFinishStr);
        List<ExtrasReportDTO> reportData = new ArrayList<>();
        Hotel hotel = hotelRepository.findById(hotelId).get();
        int count = 1;
        for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotel(dateFinish, dateStart, hotel)) {
            for (GuestExtra extraGuest : guestExtraRepository.findAllByGuest(guest)) {
                ExtrasReportDTO reportDTO = new ExtrasReportDTO();
                reportDTO.setNumber(count);
                reportDTO.setName(extraGuest.getExtra().getName());
                reportDTO.setGuest(extraGuest.getGuest().getLastname() + " " + extraGuest.getGuest().getFirstname() + " " + extraGuest.getGuest().getSecondName());
                reportDTO.setDateStart(dateFormatterDot.format(extraGuest.getGuest().getDateStart()));
                reportDTO.setDateFinish(dateFormatterDot.format(extraGuest.getGuest().getDateFinish()));
                reportDTO.setBilling("");
                reportDTO.setCount(1);
                reportDTO.setCost(extraGuest.getExtra().getCost());
                reportData.add(reportDTO);
                count++;
            }
        }

        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/ExtrasReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        String username = ru.sgp.utils.SecurityManager.getCurrentUser();
        User user = userRepository.findByUsername(username);
        Employee respEmp = user.getEmployee();
        String comendantPost = "";
        if (respEmp.getIdPoststaff() != null) {
            Optional<Post> post = postRepository.findById(respEmp.getIdPoststaff().longValue());
            if (post.isPresent())
                comendantPost = post.get().getName();
        }
        parameters.put("comendant", respEmp.getLastname() + " " + respEmp.getFirstname().charAt(0) + ". " + respEmp.getSecondName().charAt(0));
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint, "xlsx");
    }

    @Transactional
    public byte[] getAvdReport(Long hotelId, String dateStartStr, String dateFinishStr, Long responsibilityId) throws JRException, ParseException {
        Date dateStart = dateFormatter.parse(dateStartStr);
        Date dateFinish = dateFormatter.parse(dateFinishStr);
        Reason reasonAVD = reasonRepository.getById(16L);
        Responsibilities responsibility = responsibilitiesRepository.getById(responsibilityId);
        List<AvdReportDTO> reportData = new ArrayList<>();
        Hotel hotel = hotelRepository.findById(hotelId).get();
        int count = 1;
        for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotelAndContractReasonAndEmployeeIsNotNull(dateFinish, dateStart, hotel, reasonAVD)) {
            AvdReportDTO record = new AvdReportDTO();
            record.setNumber(count);
            record.setPeriod(dateFormatterDot.format(guest.getDateStart()) + "-" + dateFormatterDot.format(guest.getDateFinish()));
            record.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
            Filial filial = filialRepository.findByCode(guest.getEmployee().getIdFilial());
            if (filial != null) record.setContragent(filial.getName());
            record.setCountDays(calculateDaysCount(dateStart, dateFinish, guest));
            int countFamilyMembers = guestRepository.countAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotelAndFamilyMemberOfEmployee(dateFinish, dateStart, hotel, guest.getEmployee());
            record.setFamilyMembersCount(countFamilyMembers);
            reportData.add(record);
            count++;
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/AVD.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hotel", hotel.getName());
        parameters.put("period", dateFormatterDot.format(dateStart) + "-" + dateFormatterDot.format(dateFinish));
        parameters.put("responsibility", responsibility.getEmployee().getLastname() + " " + responsibility.getEmployee().getFirstname() + " " + responsibility.getEmployee().getSecondName());
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint, "xlsx");
    }

    public byte[] getReservationConfirmReport(Long filialEmployeeId, Long hotelId, String dateStartStr, String dateFinishStr, String format) throws ParseException, JRException {
        ModelMapper modelMapper = new ModelMapper();
        Date dateStart = dateFormatter.parse(dateStartStr);
        Date dateFinish = dateFormatter.parse(dateFinishStr);
        Hotel hotel = hotelRepository.findById(hotelId).get();
        Filial filial = filialRepository.findById(filialEmployeeId).get();
        List<ReservationConfirmReportDTO> reportData = new ArrayList<>();
        int count = 1;
        for (Reservation reservation : reservationRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotel(dateFinish, dateStart, hotel)) {
            if (reservation.getTabnum() == null) continue;
            ReservationConfirmReportDTO record = new ReservationConfirmReportDTO();
            Employee employee = employeeRepository.findByTabnumAndEndDate(reservation.getTabnum(), null);
            if (!Objects.equals(employee.getIdFilial(), filial.getCode())) continue;
            record.setN(count);
            record.setNumber(reservation.getId().intValue());
            record.setFio(employee.getLastname() + " " + employee.getFirstname() + " " + employee.getSecondName());
            record.setPeriod(dateFormatterDot.format(reservation.getDateStart()) + " - " + dateFormatterDot.format(reservation.getDateFinish()));
            record.setFlat(reservation.getBed().getRoom().getFlat().getName());
            record.setDays(calculateDaysCount(dateStart, dateFinish, modelMapper.map(reservation, Guest.class)));
            if (reservation.getContract() != null) {
                record.setCost(reservation.getContract().getCost());
                record.setSummary(record.getDays() * record.getCost());
                record.setType(reservation.getContract().getNote());
            } else {
                record.setCost(0f);
                record.setSummary(0f);
                record.setType("");
            }
            reportData.add(record);
            count++;
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/ReservationConfirm.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hotel", hotel.getName());
        parameters.put("location", hotel.getLocation());
        parameters.put("filial", filial.getName());
        parameters.put("period", dateFormatterDot.format(dateStart) + "-" + dateFormatterDot.format(dateFinish));
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint, format);
    }

    public byte[] getReservationConfirmReport(Long reservationId, String format) throws ParseException, JRException {
        ModelMapper modelMapper = new ModelMapper();
        Reservation reservation = reservationRepository.findById(reservationId).get();
        List<ReservationConfirmReportDTO> reportData = new ArrayList<>();
        ReservationConfirmReportDTO record = new ReservationConfirmReportDTO();
        Employee employee = employeeRepository.findByTabnumAndEndDate(reservation.getTabnum(), null);

        record.setN(1);
        record.setNumber(reservation.getId().intValue());
        if (employee != null) {
            record.setFio(employee.getLastname() + " " + employee.getFirstname() + " " + employee.getSecondName());
        } else {
            record.setFio(reservation.getLastname() + " " + reservation.getFirstname() + " " + reservation.getSecondName());
        }
        record.setPeriod(dateFormatterDot.format(reservation.getDateStart()) + " - " + dateFormatterDot.format(reservation.getDateFinish()));
        record.setFlat(reservation.getBed().getRoom().getFlat().getName());
        record.setDays(simpleCalculateDaysCount(modelMapper.map(reservation, Guest.class)));
        if (reservation.getContract() != null) {
            record.setCost(reservation.getContract().getCost());
            record.setSummary(record.getDays() * record.getCost());
            record.setType(reservation.getContract().getNote());
        } else {
            record.setCost(0f);
            record.setSummary(0f);
            record.setType("");
        }
        reportData.add(record);

        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/ReservationConfirm.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hotel", reservation.getBed().getRoom().getFlat().getHotel().getName());
        parameters.put("location", reservation.getBed().getRoom().getFlat().getHotel().getLocation());
        parameters.put("filial", reservation.getBed().getRoom().getFlat().getHotel().getFilial().getName());
        parameters.put("period", dateFormatterDot.format(reservation.getDateStart()) + "-" + dateFormatterDot.format(reservation.getDateFinish()));
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint, format);
    }
    // -----

    int calculateDays(long monthStart, long monthEnd, long rangeStart, long rangeEnd) {

        long latestStart = Math.max(monthStart, rangeStart);
        long earliestEnd = Math.min(monthEnd, rangeEnd);

        if (latestStart > earliestEnd) return 0;

        long overlap_days = earliestEnd - latestStart;

        if (overlap_days > 0) {
            return (int) TimeUnit.DAYS.convert(overlap_days, TimeUnit.MILLISECONDS);
        }
        return 0;
    }

    public byte[] getYearReservationReport(Long hotelId, Integer yearInt, String format) throws ParseException, JRException {
        SimpleDateFormat yearFormatter = new SimpleDateFormat("yyyy");
        Hotel hotel = hotelRepository.getById(hotelId);
        Date year = yearFormatter.parse(yearInt.toString());
        Date nextYear = yearFormatter.parse(Integer.toString(yearInt + 1));
        int hotelRooms = flatRepository.countByHotel(hotel) * 365;

        List<YearReservationReportDTO> reportData = new ArrayList<>();
        int count = 1;
        Date januaryStart = dateFormatter.parse("01-01-" + yearInt);
        Date januaryEnd = dateFormatter.parse("31-01-" + yearInt);
        Date februaryStart = dateFormatter.parse("01-02-" + yearInt);
        Date februaryEnd = dateFormatter.parse("28-02-" + yearInt);
        Date marchStart = dateFormatter.parse("01-03-" + yearInt);
        Date marchEnd = dateFormatter.parse("31-03-" + yearInt);
        Date aprilStart = dateFormatter.parse("01-04-" + yearInt);
        Date aprilEnd = dateFormatter.parse("30-04-" + yearInt);
        Date mayStart = dateFormatter.parse("01-05-" + yearInt);
        Date mayEnd = dateFormatter.parse("31-05-" + yearInt);
        Date juneStart = dateFormatter.parse("01-06-" + yearInt);
        Date juneEnd = dateFormatter.parse("30-06-" + yearInt);
        Date julyStart = dateFormatter.parse("01-07-" + yearInt);
        Date julyEnd = dateFormatter.parse("31-07-" + yearInt);
        Date augustStart = dateFormatter.parse("01-08-" + yearInt);
        Date augustEnd = dateFormatter.parse("31-08-" + yearInt);
        Date septemberStart = dateFormatter.parse("01-09-" + yearInt);
        Date septemberEnd = dateFormatter.parse("30-09-" + yearInt);
        Date octoberStart = dateFormatter.parse("01-10-" + yearInt);
        Date octoberEnd = dateFormatter.parse("31-10-" + yearInt);
        Date novemberStart = dateFormatter.parse("01-11-" + yearInt);
        Date novemberEnd = dateFormatter.parse("30-11-" + yearInt);
        Date decemberStart = dateFormatter.parse("01-12-" + yearInt);
        Date decemberEnd = dateFormatter.parse("31-12-" + yearInt);
        for (Reservation reservation : reservationRepository.findAllByDateStartAfterAndDateStartBeforeAndBedRoomFlatHotel(year, nextYear, hotel)) {
            YearReservationReportDTO record = null;
            int daysCount = (int) TimeUnit.DAYS.convert(reservation.getDateFinish().getTime() - reservation.getDateStart().getTime(), TimeUnit.MILLISECONDS);
            if (reportData.stream().anyMatch(r -> Objects.equals(r.getFilialNumber(), reservation.getFromFilial().getCode()))) { // Если нет такого филила создаем иначе работаем с существующим
                record = reportData.stream().filter(r -> Objects.equals(r.getFilialNumber(), reservation.getFromFilial().getCode())).collect(Collectors.toList()).get(0);
                record.setCount(record.getCount() + daysCount);
                record.setJanuary(record.getJanuary() + calculateDays(januaryStart.getTime(), januaryEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setFebruary(record.getFebruary() + calculateDays(februaryStart.getTime(), februaryEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setMarch(record.getMarch() + calculateDays(marchStart.getTime(), marchEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setApril(record.getApril() + calculateDays(aprilStart.getTime(), aprilEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setMay(record.getMay() + calculateDays(mayStart.getTime(), mayEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setJune(record.getJune() + calculateDays(juneStart.getTime(), juneEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setJuly(record.getJuly() + calculateDays(julyStart.getTime(), julyEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setAugust(record.getAugust() + calculateDays(augustStart.getTime(), augustEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setSeptember(record.getSeptember() + calculateDays(septemberStart.getTime(), septemberEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setOctober(record.getOctober() + calculateDays(octoberStart.getTime(), octoberEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setNovember(record.getNovember() + calculateDays(novemberStart.getTime(), novemberEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setDecember(record.getDecember() + calculateDays(decemberStart.getTime(), decemberEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
            } else {
                record = new YearReservationReportDTO();
                record.setNumber(count);
                record.setFilialName(reservation.getFromFilial().getName());
                record.setFilialNumber(reservation.getFromFilial().getCode());
                record.setCount(daysCount);
                record.setJanuary(calculateDays(januaryStart.getTime(), januaryEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setFebruary(calculateDays(februaryStart.getTime(), februaryEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setMarch(calculateDays(marchStart.getTime(), marchEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setApril(calculateDays(aprilStart.getTime(), aprilEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setMay(calculateDays(mayStart.getTime(), mayEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setJune(calculateDays(juneStart.getTime(), juneEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setJuly(calculateDays(julyStart.getTime(), julyEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setAugust(calculateDays(augustStart.getTime(), augustEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setSeptember(calculateDays(septemberStart.getTime(), septemberEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setOctober(calculateDays(octoberStart.getTime(), octoberEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setNovember(calculateDays(novemberStart.getTime(), novemberEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));
                record.setDecember(calculateDays(decemberStart.getTime(), decemberEnd.getTime(), reservation.getDateStart().getTime(), reservation.getDateFinish().getTime()));


                reportData.add(record);
            }
            count++;
        }

        for (YearReservationReportDTO record : reportData)
            record.setPercent(((float) (record.getCount() / (float) hotelRooms)) * 100);

        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/YearReservationReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint, format);
    }

    @Transactional
    public byte[] bookReport(Long bookReportId) throws ParseException, JRException {
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
        List<BookReportDTO> reportData = new ArrayList<>();
        BookReport bookReport = bookReportRepository.getById(bookReportId);
        for (RecordBookReport recordBookReport : recordBookReportRepository.findAllByBookReport(bookReport)) {
            BookReportDTO bookReportDTO = new BookReportDTO();
            bookReportDTO.setBookId(bookReport.getId().toString());
            bookReportDTO.setDate(dateTimeFormatter.format(bookReport.getDate()));
            bookReportDTO.setUsername(bookReport.getUsername());
            bookReportDTO.setDuration(bookReport.getDuration().toString());
            bookReportDTO.setBookStatus(bookReport.getStatus());
            bookReportDTO.setTabnumber(recordBookReport.getTabnumber().toString());
            Employee employee = employeeRepository.findByTabnumAndEndDate(recordBookReport.getTabnumber(), null);
            if (employee != null)
                bookReportDTO.setFio(employee.getLastname() + " " + employee.getFirstname() + " " + employee.getSecondName());
            else bookReportDTO.setFio(recordBookReport.getFio());
            bookReportDTO.setDateStart(dateFormatter.format(recordBookReport.getDateStart()));
            bookReportDTO.setDateFinish(dateFormatter.format(recordBookReport.getDateFinish()));
            bookReportDTO.setStatus(recordBookReport.getStatus());
            reportData.add(bookReportDTO);
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/BookReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint, "xlsx");
    }

}
