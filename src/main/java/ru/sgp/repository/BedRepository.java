package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Bed;
import ru.sgp.model.Flat;
import ru.sgp.model.Hotel;
import ru.sgp.model.Room;

import java.util.List;
import java.util.Optional;

@Repository
public interface BedRepository extends JpaRepository<Bed, Long> {
    List<Bed> findAllByRoomAndIsExtra(Room room, Boolean isExtra);

    Integer countByRoomFlatAndIsExtra(Flat flat, Boolean isExtra);

    Integer countByRoomAndIsExtra(Room room, Boolean isExtra);

    List<Bed> findAllByRoomFlatHotelAndIsExtra(Hotel hotel, Boolean isExtra);

    List<Bed> findAllByRoomFlatAndIsExtra(Flat flat, Boolean isExtra);

    List<Bed> findAllByRoom(Room room);

    Integer countByRoomFlatHotelAndIsExtra(Hotel hotel, boolean b);

    List<Bed> findAllByRoomFlatHotelId(long l);
}
