package com.paytrack;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.paytrack.service.InvoiceNumberGenerator;

public class InvoiceNumberSeriesTest extends AbstractIntegrationTest {

    @Autowired
    private InvoiceNumberGenerator generator;

    @Test
    void shouldGenerate20UniqueConsecutiveNumbers() throws Exception {

        UUID tenantId = UUID.fromString(
                "11111111-1111-1111-1111-111111111111");

        String financialYear = "2026";

        ExecutorService executor =
                Executors.newFixedThreadPool(20);

        List<Future<Integer>> futures =
                new ArrayList<>();

        for (int i = 0; i < 20; i++) {

            futures.add(
                    executor.submit(() ->
                            generator.nextNumber(
                                    tenantId,
                                    financialYear)));
        }

        List<Integer> generatedNumbers =
                new ArrayList<>();

        for (Future<Integer> future : futures) {

            generatedNumbers.add(
                    future.get());
        }

        executor.shutdown();

        /*
         * 20 appels
         */
        assertEquals(
                20,
                generatedNumbers.size());

        /*
         * aucun doublon
         */
        assertEquals(
                20,
                new HashSet<>(generatedNumbers).size());

        /*
         * tri pour vérifier la continuité
         */
        Collections.sort(generatedNumbers);

        int first = generatedNumbers.get(0);

        for (int i = 0; i < generatedNumbers.size(); i++) {

            assertEquals(
                    first + i,
                    generatedNumbers.get(i),
                    "Gap detected in invoice series");
        }
    }
}