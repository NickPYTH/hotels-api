package ru.sgp.dto;

import lombok.Data;
import ru.sgp.model.PaymentType;

@Data
public class ExtraDTO {
    private Long id;
    private String name;
    private String description;
    private Float cost;

    // Значения из таблицы guests_extras
    private Boolean isPaid;
    private PaymentTypeDTO paymentType;
}
