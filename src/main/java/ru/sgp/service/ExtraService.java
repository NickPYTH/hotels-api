package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.ExtraDTO;
import ru.sgp.model.Extra;
import ru.sgp.model.Guest;
import ru.sgp.model.GuestExtra;
import ru.sgp.model.PaymentType;
import ru.sgp.repository.ExtraRepository;
import ru.sgp.repository.GuestExtraRepository;
import ru.sgp.repository.GuestRepository;
import ru.sgp.repository.PaymentTypeRepository;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExtraService {
    @Autowired
    private ExtraRepository extraRepository;
    @Autowired
    private GuestRepository guestRepository;
    @Autowired
    private GuestExtraRepository guestExtraRepository;
    @Autowired
    private PaymentTypeRepository paymentTypeRepository;

    public List<ExtraDTO> getAll() {
        ModelMapper modelMapper = new ModelMapper();
        return extraRepository.findAll().stream().map(e -> modelMapper.map(e, ExtraDTO.class)).collect(Collectors.toList());
    }

    public List<ExtraDTO> getAllByGuest(Long guestId) {
        ModelMapper modelMapper = new ModelMapper();
        List<ExtraDTO> response = new ArrayList<>();
        for (GuestExtra guestExtra : guestExtraRepository.findAllByGuest(guestRepository.getById(guestId))) {
            ExtraDTO extraDTO = modelMapper.map(guestExtra, ExtraDTO.class);
            extraDTO.setIsPaid(guestExtra.getIsPaid());
            extraDTO.setName(guestExtra.getExtra().getName());
            extraDTO.setDescription(guestExtra.getExtra().getDescription());
            extraDTO.setCost(guestExtra.getExtra().getCost());
            response.add(extraDTO);
        }
        return response;
    }

    public ExtraDTO get(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(extraRepository.getById(id), ExtraDTO.class);
    }

    @Transactional
    public ExtraDTO update(ExtraDTO ExtraDTO) throws ParseException {
        ModelMapper modelMapper = new ModelMapper();
        Extra extra = modelMapper.map(ExtraDTO, Extra.class);
        extraRepository.save(extra);
        return ExtraDTO;
    }

    @Transactional
    public ExtraDTO confirmPayment(Long guestId, Long extraId, Long paymentTypeId) {
        Guest guest = guestRepository.getById(guestId);
        GuestExtra guestExtra = guestExtraRepository.getById(extraId);
        PaymentType paymentType = paymentTypeRepository.getById(paymentTypeId);
        for (GuestExtra ge : guestExtraRepository.findAllByGuestAndExtra(guest, guestExtra.getExtra())) {
            ge.setIsPaid(true);
            ge.setPaymentType(paymentType);
            guestExtraRepository.save(ge);
        }
        return new ExtraDTO();
    }

    @Transactional
    public ExtraDTO create(ExtraDTO ExtraDTO) throws ParseException {
        ModelMapper modelMapper = new ModelMapper();
        Extra Extra = modelMapper.map(ExtraDTO, Extra.class);
        extraRepository.save(Extra);
        return ExtraDTO;
    }

    public ExtraDTO createGuestExtra(Long guestId, Long extraId) throws ParseException {
        Guest guest = guestRepository.getById(guestId);
        Extra extra = extraRepository.getById(extraId);
        GuestExtra guestExtra = new GuestExtra();
        guestExtra.setGuest(guest);
        guestExtra.setExtra(extra);
        guestExtra.setIsPaid(false);
        guestExtraRepository.save(guestExtra);
        return new ExtraDTO();
    }

    public ExtraDTO deleteGuestExtra(Long guestId, Long extraId) {
        guestExtraRepository.deleteById(extraId);
        return new ExtraDTO();
    }
}