package com.example.demo.dto;

import com.example.demo.entities.PaymentMethod;
import com.example.demo.entities.PaymentStatus;
import lombok.Data;

@Data

public class OrderPaymentDto {
    private Long id;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String transactionId;
}
