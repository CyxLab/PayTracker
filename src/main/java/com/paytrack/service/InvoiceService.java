package com.paytrack.service;

import com.paytrack.dto.InvoiceDto;
import com.paytrack.model.Invoice;
import com.paytrack.repository.InvoiceRepository;
import com.paytrack.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> listForCurrentTenant() {
        UUID tenantId = TenantContext.get();
        return invoiceRepository.findAllByTenantId(tenantId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceDto findByIdForCurrentTenant(Long id) {
        UUID tenantId = TenantContext.get();
        return invoiceRepository.findById(id)
                .filter(inv -> tenantId.equals(inv.getTenantId()))
                .map(this::toDto)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Invoice " + id + " not found"));
    }

    private InvoiceDto toDto(Invoice inv) {
        return new InvoiceDto(inv.getId(), inv.getTenantId(), inv.getCustomer(),
                inv.getAmount(), inv.getStatus());
    }
}
