package com.superette.ui;

import java.awt.Desktop;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import com.superette.model.Settings;
import com.superette.repo.SaleRepo;
import com.superette.repo.SettingsRepo;
import com.superette.service.ReceiptService;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class SalesHistoryController {
    private final BorderPane root = new BorderPane();
    private final SaleRepo saleRepo = new SaleRepo();
    private final SettingsRepo settingsRepo = new SettingsRepo();
    private final ReceiptService receiptService = new ReceiptService();

    // Filters
    private final DatePicker fromDate = new DatePicker(LocalDate.now());
    private final DatePicker toDate = new DatePicker(LocalDate.now());
    private final Button refreshBtn = new Button("Refresh");

    // Sales list
    private final TableView<SaleRepo.SaleSummary> table = new TableView<>();

    // Details
    private final TableView<SaleRepo.SaleLineDetail> details = new TableView<>();
    private final Button pdfBtn = new Button("Generate Receipt (PDF)");

    public SalesHistoryController() {
        // Filters bar
        HBox filters = new HBox(10,
                new Label("From:"), fromDate,
                new Label("To:"), toDate,
                refreshBtn);
        filters.setPadding(new Insets(10));
        refreshBtn.setOnAction(e -> refresh());

        // Sales table
        TableColumn<SaleRepo.SaleSummary, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().id));
        TableColumn<SaleRepo.SaleSummary, String> dtCol = new TableColumn<>("Date/Time");
        dtCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().datetime));
        TableColumn<SaleRepo.SaleSummary, String> payCol = new TableColumn<>("Payment");
        payCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().paymentMethod));
        TableColumn<SaleRepo.SaleSummary, Number> vatCol = new TableColumn<>("VAT");
        vatCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().totalVat));
        TableColumn<SaleRepo.SaleSummary, Number> totCol = new TableColumn<>("Total");
        totCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().totalGross));
        table.getColumns().addAll(List.of(idCol, dtCol, payCol, vatCol, totCol));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> loadDetails(nv));

        // Details table
        TableColumn<SaleRepo.SaleLineDetail, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().productName));
        TableColumn<SaleRepo.SaleLineDetail, String> bcCol = new TableColumn<>("Barcode");
        bcCol.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().barcode == null ? "" : c.getValue().barcode));
        TableColumn<SaleRepo.SaleLineDetail, Number> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().qty));
        TableColumn<SaleRepo.SaleLineDetail, Number> unitCol = new TableColumn<>("Unit (gross)");
        unitCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().unitPriceGross));
        TableColumn<SaleRepo.SaleLineDetail, Number> lineCol = new TableColumn<>("Line Total");
        lineCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().qty * c.getValue().unitPriceGross));
        details.getColumns().addAll(List.of(nameCol, bcCol, qtyCol, unitCol, lineCol));
        details.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        pdfBtn.setOnAction(e -> onGeneratePDF());

        SplitPane split = new SplitPane(table, new BorderPane(details, null, null, pdfBtn, null));
        split.setDividerPositions(0.55);

        root.setTop(filters);
        root.setCenter(split);

        refresh();
    }

    private void refresh() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from == null || to == null) {
            alert("Validation", "Select both From and To dates.");
            return;
        }
        if (to.isBefore(from)) {
            alert("Validation", "To date cannot be before From date.");
            return;
        }
        table.getItems().setAll(saleRepo.listSales(from, to));
        details.getItems().clear();
    }

    private void loadDetails(SaleRepo.SaleSummary sale) {
        if (sale == null) {
            details.getItems().clear();
            return;
        }
        List<SaleRepo.SaleLineDetail> lines = saleRepo.saleDetails(sale.id);
        details.getItems().setAll(lines);
    }

    private void onGeneratePDF() {
        SaleRepo.SaleSummary sale = table.getSelectionModel().getSelectedItem();
        if (sale == null) {
            alert("Selection", "Select a sale first.");
            return;
        }
        try {
            Settings s = settingsRepo.load();
            List<SaleRepo.SaleLineDetail> lines = saleRepo.saleDetails(sale.id);
            Path path = receiptService.generateReceiptPDF(s, sale, lines);
            alert("Receipt generated", "Saved to: " + path.toAbsolutePath());
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(path.toFile());
                }
            } catch (Exception ignore) {
            }
        } catch (Exception ex) {
            alert("Error", ex.getMessage());
        }
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(title);
        a.showAndWait();
    }

    public Parent getRoot() {
        return root;
    }
}
