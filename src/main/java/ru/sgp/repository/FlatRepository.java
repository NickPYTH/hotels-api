package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Flat;
import ru.sgp.model.FlatLocks;
import ru.sgp.model.Hotel;

import java.util.Date;
import java.util.List;

@Repository
public interface FlatRepository extends JpaRepository<Flat, Long> {

    List<Flat> findAllByHotelOrderById(Hotel hotel);
    List<Flat> findAllByHotelAndFloorOrderById(Hotel hotel, Integer floor);

    Flat findByNameAndFloorAndHotel(String name, Integer floor, Hotel hotel);

    Integer countAllByHotel(Hotel hotel);

    void deleteAllByHotel(Hotel h);

    Flat findByNameAndHotel(String flatName, Hotel hotel);

    int countByHotel(Hotel hotel);
}
