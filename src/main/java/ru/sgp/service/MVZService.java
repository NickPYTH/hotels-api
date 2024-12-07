package ru.sgp.service;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.sgp.dto.MVZDTO;
import ru.sgp.model.MVZ;
import ru.sgp.repository.EmployeeRepository;
import ru.sgp.repository.FilialRepository;
import ru.sgp.repository.MVZRepository;
import ru.sgp.repository.OrganizationRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MVZService {
    @Autowired
    MVZRepository mvzRepository;
    @Autowired
    FilialRepository filialRepository;
    ModelMapper modelMapper = new ModelMapper();

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

}