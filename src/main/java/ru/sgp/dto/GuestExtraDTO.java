package ru.sgp.dto;

import lombok.Data;

@Data
public class GuestExtraDTO {
    private Long id;
    private Long guestId;
    private Long extraId;
    private Boolean isPaid;
    private PaymentTypeDTO paymentType;
}
