package com.paytrack.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.paytrack.model.NumberSeries;
import com.paytrack.repository.NumberSeriesRepository;

import jakarta.transaction.Transactional;

@Service
public class InvoiceNumberGenerator {

    private final NumberSeriesRepository repository;

    public InvoiceNumberGenerator(NumberSeriesRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int nextNumber(UUID tenantId, String financialYear) {

        NumberSeries series =
                repository.findForUpdate(
                        tenantId,
                        financialYear)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No number series found for year "
                                        + financialYear));

        int next = series.getCurrentValue() + 1;

        series.setCurrentValue(next);

        repository.save(series);

        return next;
    }
}