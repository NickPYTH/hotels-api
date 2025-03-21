package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.ExtraDTO;
import ru.sgp.model.Extra;
import ru.sgp.model.Guest;
import ru.sgp.model.GuestExtra;
import ru.sgp.repository.ExtraRepository;
import ru.sgp.repository.GuestExtraRepository;
import ru.sgp.repository.GuestRepository;

import java.text.ParseException;
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
    public List<ExtraDTO> getAll() {
        ModelMapper modelMapper = new ModelMapper();
        return extraRepository.findAll().stream().map(e -> modelMapper.map(e, ExtraDTO.class)).collect(Collectors.toList());
    }
    public List<ExtraDTO> getAllByGuest(Long guestId) {
        ModelMapper modelMapper = new ModelMapper();
        return guestExtraRepository.findAllByGuest(guestRepository.getById(guestId)).stream().map(e -> modelMapper.map(e.getExtra(), ExtraDTO.class)).collect(Collectors.toList());
    }
    public ExtraDTO get(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(extraRepository.getById(id), ExtraDTO.class);
    }
    @Transactional
    public ExtraDTO update(ExtraDTO ExtraDTO) throws ParseException {
        ModelMapper modelMapper = new ModelMapper();
        Extra Extra = modelMapper.map(ExtraDTO, Extra.class);
        extraRepository.save(Extra);
        return ExtraDTO;
    }
    @Transactional
    public ExtraDTO create(ExtraDTO ExtraDTO) throws ParseException {
        ModelMapper modelMapper = new ModelMapper();
        Extra Extra = modelMapper.map(ExtraDTO, Extra.class);
        extraRepository.save(Extra);
        return ExtraDTO;
    }
    public ExtraDTO createGuestExtra(Long guestId, Long extraId) {
        Guest guest = guestRepository.getById(guestId);
        Extra extra = extraRepository.getById(extraId);
        GuestExtra guestExtra = new GuestExtra();
        guestExtra.setGuest(guest);
        guestExtra.setExtra(extra);
        guestExtraRepository.save(guestExtra);
        return new ExtraDTO();
    }
    public ExtraDTO deleteGuestExtra(Long guestId, Long extraId) {
        Guest guest = guestRepository.getById(guestId);
        Extra extra = extraRepository.getById(extraId);
        List<GuestExtra> guestExtraList = guestExtraRepository.findAllByGuestAndExtra(guest, extra);
        if (!guestExtraList.isEmpty())
            guestExtraRepository.delete(guestExtraList.get(0));
        return new ExtraDTO();
    }
}