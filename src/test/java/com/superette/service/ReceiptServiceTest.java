package com.superette.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.superette.model.Settings;
import com.superette.repo.SaleRepo;

public class ReceiptServiceTest {
    @Test
    void generatesPdf() throws Exception {
        Settings s = new Settings();
        s.setStoreName("Superette POS");
        s.setAddress("Tunis, Tunisia");
        s.setCurrency("TND");
        s.setReceiptFooter("Thank you for your purchase.");

        SaleRepo.SaleSummary sale = new SaleRepo.SaleSummary();
        sale.id = 123;
        sale.datetime = "2025-12-15 10:00:00";
        sale.paymentMethod = "CASH";
        sale.totalGross = 12.345;
        sale.totalVat = 1.234;

        SaleRepo.SaleLineDetail l1 = new SaleRepo.SaleLineDetail();
        l1.productName = "Sugar 1kg";
        l1.barcode = "61300001";
        l1.qty = 2.0;
        l1.unitPriceGross = 2.300;
        l1.vatRate = 19.0;

        SaleRepo.SaleLineDetail l2 = new SaleRepo.SaleLineDetail();
        l2.productName = "Milk 1L";
        l2.barcode = "61300002";
        l2.qty = 3.0;
        l2.unitPriceGross = 1.200;
        l2.vatRate = 19.0;

        ReceiptService svc = new ReceiptService();
        Path pdf = svc.generateReceiptPDF(saleSettingsFallback(s), sale, List.of(l1, l2));

        assertTrue(Files.exists(pdf), "PDF file should exist");
        assertTrue(Files.size(pdf) > 500, "PDF size should be > 500 bytes");
        // Cleanup optional:
        // Files.deleteIfExists(pdf);
    }

    // If settings fields are null, ReceiptService applies fallbacks; keep
    // consistent with your service logic
    private Settings saleSettingsFallback(Settings s) {
        return s;
    }
}
