package com.superette.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.superette.model.Settings;
import com.superette.repo.SaleRepo;

public class ReceiptService {

    public Path generateReceiptPDF(Settings settings, SaleRepo.SaleSummary sale,
            List<SaleRepo.SaleLineDetail> lines) throws Exception {
        String currency = settings.getCurrency() == null || settings.getCurrency().isBlank() ? "TND"
                : settings.getCurrency();
        String storeName = settings.getStoreName() == null ? "Superette POS" : settings.getStoreName();
        String addr = settings.getAddress() == null ? "" : settings.getAddress();
        String phone = settings.getPhone() == null ? "" : settings.getPhone();
        String taxId = settings.getTaxId() == null ? "" : settings.getTaxId();
        String footer = settings.getReceiptFooter() == null ? "" : settings.getReceiptFooter();

        Path outDir = Path.of("receipts");
        Files.createDirectories(outDir);
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path outFile = outDir.resolve(String.format("receipt_%d_%s.pdf", sale.id, ts));

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50f;
                float y = page.getMediaBox().getHeight() - margin;

                // Header
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.newLineAtOffset(margin, y);
                cs.showText(storeName);
                cs.endText();

                y -= 20;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText(addr);
                cs.endText();

                if (!phone.isBlank()) {
                    y -= 12;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Phone: " + phone);
                    cs.endText();
                }
                if (!taxId.isBlank()) {
                    y -= 12;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Tax ID: " + taxId);
                    cs.endText();
                }

                // Sale info
                y -= 20;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText(String.format("Sale #%d — %s — %s", sale.id, sale.datetime, sale.paymentMethod));
                cs.endText();

                // Table headers
                y -= 18;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Item");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.newLineAtOffset(page.getMediaBox().getWidth() - margin - 200, y);
                cs.showText("Qty");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.newLineAtOffset(page.getMediaBox().getWidth() - margin - 140, y);
                cs.showText("Unit");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.newLineAtOffset(page.getMediaBox().getWidth() - margin - 70, y);
                cs.showText("Total");
                cs.endText();

                // Lines
                y -= 14;
                cs.setFont(PDType1Font.HELVETICA, 10);
                for (SaleRepo.SaleLineDetail d : lines) {
                    if (y < 100)
                        break; // simple pagination guard
                    String itemName = d.productName == null ? "" : d.productName;
                    String barcode = d.barcode == null ? "" : (" [" + d.barcode + "]");
                    String itemText = itemName + barcode;

                    double lineTotal = d.qty * d.unitPriceGross;

                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText(truncate(itemText, 40));
                    cs.endText();

                    cs.beginText();
                    cs.newLineAtOffset(page.getMediaBox().getWidth() - margin - 200, y);
                    cs.showText(fmtQty(d.qty));
                    cs.endText();

                    cs.beginText();
                    cs.newLineAtOffset(page.getMediaBox().getWidth() - margin - 140, y);
                    cs.showText(fmtMoney(currency, d.unitPriceGross));
                    cs.endText();

                    cs.beginText();
                    cs.newLineAtOffset(page.getMediaBox().getWidth() - margin - 70, y);
                    cs.showText(fmtMoney(currency, lineTotal));
                    cs.endText();

                    y -= 12;
                }

                // Totals
                y -= 10;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText(String.format("Total VAT: %s", fmtMoney(currency, sale.totalVat)));
                cs.endText();

                y -= 16;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
                cs.newLineAtOffset(margin, y);
                cs.showText(String.format("Total: %s", fmtMoney(currency, sale.totalGross)));
                cs.endText();

                // Footer
                if (!footer.isBlank()) {
                    y = 80;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(footer);
                    cs.endText();
                }
            }

            doc.save(outFile.toFile());
        }

        return outFile;
    }

    private String fmtMoney(String cur, double v) {
        return cur + " " + String.format(java.util.Locale.US, "%.3f", v);
    }

    private String fmtQty(double v) {
        return String.format(java.util.Locale.US, "%.3f", v).replaceAll("\\.?0+$", "");
    }

    private String truncate(String s, int n) {
        if (s == null)
            return "";
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }
}
