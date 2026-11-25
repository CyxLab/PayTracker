package com.paytrack;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.paytrack.model.Invoice;
import com.paytrack.repository.InvoiceRepository;

public class InvoiceNumberSeriesTest extends AbstractIntegrationTest{
	
	@Autowired
	private InvoiceRepository invoiceRepository;
	
	@Test
	void shouldGetLastInvoiceNumber() {

	    Optional<Invoice> invoice =
	            invoiceRepository.findTopByOrderByInvoiceNumberDesc();

	    assertTrue(invoice.isPresent());

	    Invoice lastInvoice = invoice.get();

	    assertEquals(2, lastInvoice.getInvoiceNumber());
	}	
}
