package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Extra;
import ru.sgp.model.Guest;
import ru.sgp.model.GuestExtra;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuestExtraRepository extends JpaRepository<GuestExtra, Long> {

    List<GuestExtra> findAllByGuest(Guest byId);

    GuestExtra findByGuestAndExtra(Guest guest, Extra extra);

    List<GuestExtra> findAllByGuestAndExtra(Guest guest, Extra extra);
}
