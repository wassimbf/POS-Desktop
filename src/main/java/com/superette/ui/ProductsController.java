package com.superette.ui;

import java.util.List;

import com.superette.model.Product;
import com.superette.repo.ProductRepo;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

public class ProductsController {
    private final BorderPane root = new BorderPane();
    private final TableView<Product> table = new TableView<>();
    private final ProductRepo repo = new ProductRepo();
    private Runnable onProductsChanged;

    public ProductsController() {
        TableColumn<Product, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));

        TableColumn<Product, Number> priceCol = new TableColumn<>("Price (gross)");
        priceCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getPriceGross()));

        TableColumn<Product, Number> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getStockQty()));

        TableColumn<Product, String> barcodeCol = new TableColumn<>("Barcode");
        barcodeCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBarcode() == null ? "" : c.getValue().getBarcode()));

        TableColumn<Product, Product> editCol = new TableColumn<>("Edit");
        editCol.setCellFactory(editCellFactory());

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(List.of(nameCol, barcodeCol, priceCol, stockCol, editCol));

        // Double-click row to edit
        table.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    openEdit(row.getItem());
                }
            });
            return row;
        });

        // Add form (unchanged)
        TextField name = new TextField();
        name.setPromptText("Product name");
        TextField price = new TextField();
        price.setPromptText("Price gross");
        TextField vat = new TextField("19");
        TextField stock = new TextField("0");
        TextField threshold = new TextField("0");
        threshold.setPromptText("Reorder threshold");
        TextField barcode = new TextField();
        barcode.setPromptText("Barcode");

        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            try {
                Product p = new Product();
                p.setName(name.getText().trim());
                p.setBarcode(barcode.getText().trim().isEmpty() ? null : barcode.getText().trim());
                p.setPriceGross(Double.parseDouble(price.getText().trim()));
                p.setVatRate(Double.parseDouble(vat.getText().trim()));
                p.setStockQty(Double.parseDouble(stock.getText().trim()));
                p.setReorderThreshold(Double.parseDouble(threshold.getText().trim()));
                p.setActive(true);
                if (p.getName().isEmpty() || p.getPriceGross() < 0 || p.getVatRate() < 0 || p.getStockQty() < 0
                        || p.getReorderThreshold() < 0) {
                    alert("Validation", "Check fields: name not empty, price/vat/stock/threshold >= 0");
                    return;
                }
                repo.insert(p);
                refresh();
                if (onProductsChanged != null)
                    onProductsChanged.run();
                name.clear();
                price.clear();
                stock.clear();
                threshold.clear();
                barcode.clear();
            } catch (Exception ex) {
                alert("Error", ex.getMessage());
            }
        });

        HBox form = new HBox(10, name, barcode, price, vat, stock, threshold, addBtn);
        form.setPadding(new Insets(10));
        form.setPrefHeight(60);

        root.setTop(form);
        root.setCenter(table);
        refresh();
    }

    private Callback<TableColumn<Product, Product>, TableCell<Product, Product>> editCellFactory() {
        return col -> new TableCell<>() {
            private final Button btn = new Button("Edit");
            {
                btn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    openEdit(p);
                });
            }

            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    private void openEdit(Product p) {
        ProductEditDialog dialog = new ProductEditDialog(p);
        Product updated = dialog.showAndWait().orElse(null);
        if (updated != null) {
            try {
                repo.update(updated);
                refresh();
                if (onProductsChanged != null)
                    onProductsChanged.run();
            } catch (Exception ex) {
                alert("Error", ex.getMessage());
            }
        }
    }

    public void refresh() {
        table.getItems().setAll(repo.findAll());
    }

    public void setOnProductsChanged(Runnable r) {
        this.onProductsChanged = r;
    }

    private void alert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setHeaderText(title);
        a.showAndWait();
    }

    public Parent getRoot() {
        return root;
    }
}
