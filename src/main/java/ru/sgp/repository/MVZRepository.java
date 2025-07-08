package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.MVZ;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MVZRepository extends JpaRepository<MVZ, Long> {

    MVZ findById(String mvzId);

    Collection<Object> findByIdAndNameIsContainingIgnoreCase(String mvzId, String ceh);
}
