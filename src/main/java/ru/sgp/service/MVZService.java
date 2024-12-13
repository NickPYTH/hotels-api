package ru.sgp.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.sgp.dto.MVZDTO;
import ru.sgp.dto.report.MVZReportDTO;
import ru.sgp.dto.report.MVZReportShortDTO;
import ru.sgp.model.Filial;
import ru.sgp.model.Guest;
import ru.sgp.model.Hotel;
import ru.sgp.model.MVZ;
import ru.sgp.repository.FilialRepository;
import ru.sgp.repository.GuestRepository;
import ru.sgp.repository.HotelRepository;
import ru.sgp.repository.MVZRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class MVZService {
    @Autowired
    MVZRepository mvzRepository;
    @Autowired
    FilialRepository filialRepository;
    @Autowired
    GuestRepository guestRepository;
    ModelMapper modelMapper = new ModelMapper();

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    @Autowired
    private HotelRepository hotelRepository;

    public byte[] export(JasperPrint jasperPrint) throws JRException {
        Exporter exporter;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        exporter.exportReport();
        return outputStream.toByteArray();
    }

    public List<String> loadMVZ(MultipartFile file) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        XSSFSheet worksheet = workbook.getSheetAt(0);
        for (int i = 1; i <= 10000; i++) {
            MVZ mvz = new MVZ();
            XSSFRow row = worksheet.getRow(i);
            if (row == null)
                break;
            for (int j = 0; j < 6; j++) {
                if (row.getCell(j) != null) {
                    if (j == 0) {
                        mvz.setEmployeeTab(String.valueOf((int) row.getCell(j).getNumericCellValue()));
                    }
                    if (j == 1) {
                        mvz.setEmployeeFio(row.getCell(j).getStringCellValue());
                    }
                    if (j == 2) {
                        mvz.setMvz(row.getCell(j).getStringCellValue());
                    }
                    if (j == 3) {
                        mvz.setMvzName(row.getCell(j).getStringCellValue());
                    }
                    if (j == 4) {
                        mvz.setFilial(filialRepository.findByName(row.getCell(j).getStringCellValue()));
                    }
                    if (j == 5) {
                        mvz.setOrganization(row.getCell(j).getStringCellValue());
                    }
                }
            }
            mvzRepository.save(mvz);
        }
        List<String> response = new ArrayList<>();
        return response;
    }

    public List<MVZDTO> getAll() {
        return mvzRepository.findAll().stream().map(mvz -> modelMapper.map(mvz, MVZDTO.class)).collect(Collectors.toList());
    }

    public MVZDTO get(Long id) {
        return modelMapper.map(mvzRepository.getById(id), MVZDTO.class);
    }

    @Transactional
    public MVZDTO update(MVZDTO MVZDTO) {
        mvzRepository.save(modelMapper.map(MVZDTO, MVZ.class));
        return MVZDTO;
    }

    @Transactional
    public MVZDTO create(MVZDTO MVZDTO) {
        mvzRepository.save(modelMapper.map(MVZDTO, MVZ.class));
        return MVZDTO;
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
            if (guest.getReason().getId() == 3 || guest.getReason().getId() == 4) {
                if (!Objects.equals(guest.getEmployee().getIdFilial(), empFilial.getCode())) continue;
                MVZ mvz = mvzRepository.findByEmployeeTab(guest.getEmployee().getTabnum().toString());
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
                mvzReportDTO.setMvz(mvz.getMvz());
                mvzReportDTO.setMvzName(mvz.getMvzName());
                mvzReportDTO.setGuestFilial(filialRepository.findByCode(guest.getEmployee().getIdFilial()).getName());
                mvzReportDTO.setFilial(filial.getName());
                mvzReportDTO.setOrgUnit(mvz.getOrganization());
                mvzReportDTO.setReason(guest.getReason().getName());
                mvzReportDTO.setBilling(guest.getBilling());
                mvzReportDTOList.add(mvzReportDTO);
                count++;
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
}