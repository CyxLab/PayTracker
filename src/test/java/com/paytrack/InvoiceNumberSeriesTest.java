package com.paytrack;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.paytrack.model.Invoice;

public class InvoiceNumberSeriesTest extends AbstractIntegrationTest{
	@Test
    void testInvoiceNumberSeries() {
		int n = 5;
        // Arrange
        List<Integer> list = IntStream.range(1, n + 1)
                .boxed()
                .collect(Collectors.toList());
        
        Invoice invoice = new Invoice();
        
        // Act
        for (Integer item : list) {
			invoice.setInvoiceNumber(item);
		}
        
        // Assert
        assertTrue(isConsecutive(list), "The list must be composed by consecutive number");
    }
	
    private boolean isConsecutive(List<Integer> list) {
        if (list.size() <= 1) return true;
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i) != list.get(i - 1) + 1) {
                return false;
            }
        }
        return true;
    }	
}
