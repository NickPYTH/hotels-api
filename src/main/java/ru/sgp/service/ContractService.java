package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.ContractDTO;
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
    public ContractDTO update(ContractDTO contractDTO) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Contract contract = modelMapper.map(contractDTO, Contract.class);
        contractRepository.save(contract);
        contractDTO.setId(contract.getId());
        return contractDTO;
    }
    public ContractDTO get(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(contractRepository.getById(id), ContractDTO.class);
    }
    public List<ContractDTO> getAll() {
        ModelMapper modelMapper = new ModelMapper();
        List<ContractDTO> response = new ArrayList<>();
        for (Contract contract : contractRepository.findAll()) {
            response.add(modelMapper.map(contract, ContractDTO.class));
        }
        return response;
    }
    public List<ContractDTO> getAllByFilialAndHotel(Long filialId, Long hotelId, Long reasonId, String orgStr, String billing) {
        ModelMapper modelMapper = new ModelMapper();
        List<ContractDTO> response = new ArrayList<>();
        Organization org = organizationRepository.findByName(orgStr);
        Filial filial = filialRepository.findById(filialId).orElse(null);
        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        Reason reason = reasonRepository.findById(reasonId).orElse(null);
        for (Contract contract : contractRepository.findAllByFilialAndHotelAndReasonAndOrganizationAndBilling(filial, hotel, reason, org, billing)) {
            response.add(modelMapper.map(contract, ContractDTO.class));
        }
        return response;
    }
}