package com.paytrack.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceDto(
        Long id,
        UUID tenantId,
        String customer,
        BigDecimal amount,
        String status) {}
