package com.automation.domain.invoice.model;

import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public final class InvoiceReferenceTest {

    @Test
    public void shouldCreateInvoiceReference() {
        InvoiceReference invoice = new InvoiceReference(
                "466031434",
                "123456789012345",
                new BigDecimal("1000.00"),
                new BigDecimal("200.00")
        );

        assertEquals(invoice.getTicketId(), "466031434");
        assertEquals(invoice.getNossoNumero(), "123456789012345");
        assertEquals(
                invoice.getTotalAmount(),
                new BigDecimal("1000.00")
        );
        assertEquals(
                invoice.getMinimumAmount(),
                new BigDecimal("200.00")
        );
    }

    @Test
    public void shouldNormalizeTicketId() {
        InvoiceReference invoice = new InvoiceReference(
                " 466031434 ",
                " 123456789012345 ",
                new BigDecimal("1000.00"),
                new BigDecimal("200.00")
        );

        assertEquals(invoice.getTicketId(), "466031434");
        assertEquals(invoice.getNossoNumero(), "123456789012345");
    }

    @Test
    public void shouldRejectBlankTicketId() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new InvoiceReference(
                        " ",
                        "123456789012345",
                        new BigDecimal("1000.00"),
                        new BigDecimal("200.00")
                )
        );

        assertEquals(
                exception.getMessage(),
                "Ticket ID cannot be blank."
        );
    }

    @Test
    public void shouldRejectZeroTotalAmount() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new InvoiceReference(
                        "466031434",
                        "123456789012345",
                        BigDecimal.ZERO,
                        new BigDecimal("200.00")
                )
        );

        assertEquals(
                exception.getMessage(),
                "Invoice total amount must be greater than zero."
        );
    }

    @Test
    public void shouldRejectMinimumGreaterThanTotal() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new InvoiceReference(
                        "466031434",
                        "123456789012345",
                        new BigDecimal("1000.00"),
                        new BigDecimal("1200.00")
                )
        );

        assertEquals(
                exception.getMessage(),
                "Invoice minimum amount cannot be greater than total amount."
        );
    }
}