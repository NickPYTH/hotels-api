package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.*;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findAllByFilialAndHotelAndReasonAndYear(Filial filial, Hotel hotel, Reason reason, Integer year);

    List<Contract> findAllByFilialAndOrganizationAndHotelAndReason(Filial filial, Organization organization, Hotel hotel, Reason reason);

    List<Contract> findAllByFilialAndHotelAndReasonAndOrganization(Filial filial, Hotel hotel, Reason reason, Organization org);

    List<Contract> findAllByFilialAndHotelAndReasonAndOrganizationAndBilling(Filial filial, Hotel hotel, Reason reason, Organization org, String billing);

    List<Contract> findAllByFilialAndHotelAndOrganization(Filial filial, Hotel hotel, Organization orgTmp);
}
