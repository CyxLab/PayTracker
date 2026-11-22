package com.paytrack.controller;

import com.paytrack.dto.InvoiceDto;
import com.paytrack.service.InvoiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public List<InvoiceDto> list() {
        return invoiceService.listForCurrentTenant();
    }

    @GetMapping("/{id}")
    public InvoiceDto getById(@PathVariable Long id) {
        return invoiceService.findByIdForCurrentTenant(id);
    }
}