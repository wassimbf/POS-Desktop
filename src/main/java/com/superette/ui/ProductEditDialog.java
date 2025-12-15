package com.superette.ui;

import com.superette.model.Product;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class ProductEditDialog extends Dialog<Product> {

    private final TextField nameField = new TextField();
    private final TextField barcodeField = new TextField();
    private final TextField priceGrossField = new TextField();
    private final TextField vatRateField = new TextField();
    private final TextField thresholdField = new TextField();
    private final CheckBox activeCheck = new CheckBox("Active");

    public ProductEditDialog(Product product) {
        setTitle("Edit Product");
        setHeaderText("Update product details");

        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        nameField.setText(nvl(product.getName()));
        barcodeField.setText(nvl(product.getBarcode()));
        priceGrossField.setText(doubleToText(product.getPriceGross()));
        vatRateField.setText(doubleToText(product.getVatRate()));
        thresholdField.setText(doubleToText(product.getReorderThreshold()));
        activeCheck.setSelected(product.isActive());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Barcode:"), 0, 1);
        grid.add(barcodeField, 1, 1);
        grid.add(new Label("Price (gross):"), 0, 2);
        grid.add(priceGrossField, 1, 2);
        grid.add(new Label("VAT rate (%):"), 0, 3);
        grid.add(vatRateField, 1, 3);
        grid.add(new Label("Reorder threshold:"), 0, 4);
        grid.add(thresholdField, 1, 4);
        grid.add(activeCheck, 1, 5);

        getDialogPane().setContent(grid);

        Node saveBtn = getDialogPane().lookupButton(saveBtnType);
        saveBtn.setDisable(true);
        Runnable validate = () -> saveBtn.setDisable(!isValid());
        nameField.textProperty().addListener((o, a, b) -> validate.run());
        barcodeField.textProperty().addListener((o, a, b) -> validate.run());
        priceGrossField.textProperty().addListener((o, a, b) -> validate.run());
        vatRateField.textProperty().addListener((o, a, b) -> validate.run());
        thresholdField.textProperty().addListener((o, a, b) -> validate.run());
        validate.run();

        setResultConverter(bt -> {
            if (bt != saveBtnType)
                return null;
            // parse after validation
            product.setName(nameField.getText().trim());
            product.setBarcode(blankToNull(barcodeField.getText()));
            product.setPriceGross(Double.parseDouble(priceGrossField.getText().trim()));
            product.setVatRate(Double.parseDouble(vatRateField.getText().trim()));
            product.setReorderThreshold(Double.parseDouble(thresholdField.getText().trim()));
            product.setActive(activeCheck.isSelected());
            return product;
        });
    }

    private boolean isValid() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty())
            return false;
        try {
            double pg = Double.parseDouble(priceGrossField.getText().trim());
            double vr = Double.parseDouble(vatRateField.getText().trim());
            double th = Double.parseDouble(thresholdField.getText().trim());
            return pg >= 0 && vr >= 0 && th >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String blankToNull(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String doubleToText(double v) {
        return String.format(java.util.Locale.US, "%.3f", v).replaceAll("\\.?0+$", ""); // trim trailing zeros
    }
}
