package ru.sgp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.ContractDTO;
import ru.sgp.dto.OrganizationDTO;
import ru.sgp.model.*;
import ru.sgp.repository.*;

import java.util.ArrayList;
import java.util.List;

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
    private ReasonRepository reasonRepository;
    @Transactional
    public ContractDTO create(ContractDTO contractDTO) {
        Contract contract = new Contract();
        Filial filial = filialRepository.getById(contractDTO.getFilialId());
        Hotel hotel = hotelRepository.getById(contractDTO.getHotelId());
        Organization organization = organizationRepository.getById(contractDTO.getOrganizationId());
        Reason reason = reasonRepository.getById(Long.parseLong(contractDTO.getReason()));
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
        contractDTO.setId(contract.getId());
        return contractDTO;
    }
    @Transactional
    public ContractDTO update(ContractDTO contractDTO) {
        Contract contract = contractRepository.getById(contractDTO.getId());
        Filial filial = filialRepository.getById(contractDTO.getFilialId());
        Hotel hotel = hotelRepository.getById(contractDTO.getHotelId());
        Reason reason = reasonRepository.getById(Long.parseLong(contractDTO.getReason()));
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
            contractDTO.setReason(contract.getReason().getName());
            contractDTO.setBilling(contract.getBilling());
            contractDTO.setYear(contract.getYear());
            contractDTO.setRoomNumber(contract.getRoomNumber());
            response.add(contractDTO);
        }
        return response;
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
            contractDTO.setReason(contract.getReason().getName());
            contractDTO.setBilling(contract.getBilling());
            contractDTO.setYear(contract.getYear());
            contractDTO.setRoomNumber(contract.getRoomNumber());
            response.add(contractDTO);
        }
        return response;
    }
}