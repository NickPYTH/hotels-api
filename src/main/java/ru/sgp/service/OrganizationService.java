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
import ru.sgp.dto.report.MonthReportDTO;
import ru.sgp.model.*;
import ru.sgp.repository.*;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class OrganizationService {
    @Autowired
    OrganizationRepository organizationRepository;

    public List<OrganizationDTO> getAll() {
        List<OrganizationDTO> response = new ArrayList<>();
        for (Organization organization : organizationRepository.findAll()) {
            OrganizationDTO organizationDTO = new OrganizationDTO();
            organizationDTO.setId(organization.getId());
            organizationDTO.setName(organization.getName());
            response.add(organizationDTO);
        }
        return response;
    }

    public OrganizationDTO get(Long id) {
        Organization organization = organizationRepository.getById(id);
        OrganizationDTO organizationDTO = new OrganizationDTO();
        organizationDTO.setId(organization.getId());
        organizationDTO.setName(organization.getName());
        return organizationDTO;
    }

    @Transactional
    public OrganizationDTO update(OrganizationDTO organizationDTO) {
        Organization organization = organizationRepository.getById(organizationDTO.getId());
        organization.setName(organizationDTO.getName());
        organizationRepository.save(organization);
        return organizationDTO;
    }

    @Transactional
    public OrganizationDTO create(OrganizationDTO organizationDTO) {
        Organization organization = new Organization();
        organization.setName(organizationDTO.getName());
        organizationRepository.save(organization);
        return organizationDTO;
    }

}