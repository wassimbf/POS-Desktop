package com.superette.ui;

import com.superette.model.Settings;
import com.superette.repo.SettingsRepo;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class SettingsController {
    private final VBox root = new VBox(10);
    private final SettingsRepo repo = new SettingsRepo();

    private final TextField storeName = new TextField();
    private final TextArea address = new TextArea();
    private final TextField phone = new TextField();
    private final TextField taxId = new TextField();
    private final TextField currency = new TextField("TND");
    private final TextField defaultVat = new TextField("19");
    private final TextArea receiptFooter = new TextArea();

    public SettingsController() {
        root.setPadding(new Insets(10));

        Label title = new Label("Settings");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        address.setPrefRowCount(2);
        receiptFooter.setPrefRowCount(3);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);

        form.add(new Label("Store name:"), 0, 0);
        form.add(storeName, 1, 0);
        form.add(new Label("Address:"), 0, 1);
        form.add(address, 1, 1);
        form.add(new Label("Phone:"), 0, 2);
        form.add(phone, 1, 2);
        form.add(new Label("Tax ID:"), 0, 3);
        form.add(taxId, 1, 3);
        form.add(new Label("Currency (e.g., TND):"), 0, 4);
        form.add(currency, 1, 4);
        form.add(new Label("Default VAT rate (%):"), 0, 5);
        form.add(defaultVat, 1, 5);
        form.add(new Label("Receipt footer:"), 0, 6);
        form.add(receiptFooter, 1, 6);

        Button save = new Button("Save");
        save.setOnAction(e -> onSave());

        root.getChildren().addAll(title, form, save);
        load();
    }

    private void load() {
        Settings s = repo.load();
        storeName.setText(nvl(s.getStoreName()));
        address.setText(nvl(s.getAddress()));
        phone.setText(nvl(s.getPhone()));
        taxId.setText(nvl(s.getTaxId()));
        currency.setText(nvl(s.getCurrency()));
        defaultVat.setText(s.getDefaultVatRate() == null ? "" : trimZeros(s.getDefaultVatRate()));
        receiptFooter.setText(nvl(s.getReceiptFooter()));
    }

    private void onSave() {
        try {
            Settings s = new Settings();
            s.setStoreName(trim(storeName.getText()));
            s.setAddress(trim(address.getText()));
            s.setPhone(trim(phone.getText()));
            s.setTaxId(trim(taxId.getText()));
            s.setCurrency(trim(currency.getText()));
            Double vat = null;
            String vatTxt = defaultVat.getText() == null ? "" : defaultVat.getText().trim();
            if (!vatTxt.isEmpty()) {
                vat = Double.parseDouble(vatTxt);
                if (vat < 0) {
                    alert("Validation", "VAT must be >= 0");
                    return;
                }
            }
            s.setDefaultVatRate(vat);
            s.setReceiptFooter(trim(receiptFooter.getText()));

            repo.save(s);
            alert("Saved", "Settings updated.");
        } catch (NumberFormatException ex) {
            alert("Validation", "Default VAT must be a number.");
        } catch (Exception ex) {
            alert("Error", ex.getMessage());
        }
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String trimZeros(double v) {
        return String.format(java.util.Locale.US, "%.3f", v).replaceAll("\\.?0+$", "");
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
