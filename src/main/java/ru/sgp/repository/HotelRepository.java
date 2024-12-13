package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Filial;
import ru.sgp.model.Hotel;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findAllByFilial(Filial filial);

    Hotel findByName(String hotel);

    Hotel findByNameAndFilial(String hotel, Filial filialModel);
}
