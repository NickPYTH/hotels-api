package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.PaymentTypeDTO;
import ru.sgp.model.PaymentType;
import ru.sgp.repository.PaymentTypeRepository;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentTypeService {
    @Autowired
    private PaymentTypeRepository paymentTypeRepository;

    @Transactional
    public PaymentTypeDTO update(PaymentTypeDTO PaymentTypeDTO) throws ParseException {
        PaymentType PaymentType;
        if (PaymentTypeDTO.getId() != null) PaymentType = paymentTypeRepository.getById(PaymentTypeDTO.getId());
        else PaymentType = new PaymentType();
        PaymentType.setName(PaymentTypeDTO.getName());
        paymentTypeRepository.save(PaymentType);
        return PaymentTypeDTO;
    }

    public PaymentTypeDTO get(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(paymentTypeRepository.getById(id), PaymentTypeDTO.class);
    }

    public List<PaymentTypeDTO> getAll() {
        ModelMapper modelMapper = new ModelMapper();
        return paymentTypeRepository.findAll().stream().map(e -> modelMapper.map(e, PaymentTypeDTO.class)).collect(Collectors.toList());
    }
}