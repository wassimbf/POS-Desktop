package com.superette.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.superette.model.Product;
import com.superette.repo.ProductRepo;
import com.superette.repo.SaleRepo;

import javafx.animation.PauseTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.converter.DoubleStringConverter;

public class CashRegisterController {
    private final BorderPane root = new BorderPane();
    private final ProductRepo productRepo = new ProductRepo();
    private final SaleRepo saleRepo = new SaleRepo();
    private Runnable onSaleCompleted;

    // Inputs
    private final TextField productField = new TextField(); // barcode or name
    private final TextField qtyField = new TextField("1");

    // Autocomplete
    private final ContextMenu suggestions = new ContextMenu();
    private final PauseTransition suggestDebounce = new PauseTransition(Duration.millis(200));

    // Cart
    private final TableView<CartLine> table = new TableView<>();
    private final ObservableList<CartLine> cart = FXCollections.observableArrayList();

    // Totals
    private final Label totalGrossLbl = new Label("TND 0.000");
    private final Label totalVatLbl = new Label("TND 0.000");

    public CashRegisterController() {
        root.setPadding(new Insets(10));

        Label title = new Label("Sales (Cash Register)");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        productField.setPromptText("Barcode or Product name");
        qtyField.setPromptText("Qty");

        Button addBtn = new Button("Add to cart");
        addBtn.setOnAction(e -> onAddToCart());
        productField.setOnAction(e -> onAddToCart());
        qtyField.setOnAction(e -> onAddToCart());

        GridPane top = new GridPane();
        top.setHgap(10);
        top.setVgap(8);
        top.add(new Label("Product:"), 0, 0);
        top.add(productField, 1, 0);
        top.add(new Label("Qty:"), 2, 0);
        top.add(qtyField, 3, 0);
        top.add(addBtn, 4, 0);

        // Table columns
        TableColumn<CartLine, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProduct().getName()));

        TableColumn<CartLine, Number> priceCol = new TableColumn<>("Unit (gross)");
        priceCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getProduct().getPriceGross()));

        TableColumn<CartLine, Double> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(c -> c.getValue().qtyProperty().asObject());
        qtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        qtyCol.setOnEditCommit(evt -> {
            double v = evt.getNewValue();
            if (v <= 0) {
                alert("Validation", "Quantity must be > 0");
                evt.getRowValue().setQty(1.0);
            } else {
                evt.getRowValue().setQty(v);
            }
            recalcTotals();
        });

        TableColumn<CartLine, Number> totalCol = new TableColumn<>("Line Total");
        totalCol.setCellValueFactory(
                c -> new SimpleDoubleProperty(c.getValue().getQty() * c.getValue().getProduct().getPriceGross()));

        TableColumn<CartLine, CartLine> removeCol = new TableColumn<>("Remove");
        removeCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        removeCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("X");

            @Override
            protected void updateItem(CartLine line, boolean empty) {
                super.updateItem(line, empty);
                if (empty || line == null) {
                    setGraphic(null);
                } else {
                    btn.setOnAction(e -> {
                        cart.remove(line);
                        recalcTotals();
                    });
                    setGraphic(btn);
                }
            }
        });

        table.setItems(cart);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(List.of(nameCol, priceCol, qtyCol, totalCol, removeCol));

        // Bottom: totals + actions
        HBox totals = new HBox(20, new Label("Total Gross:"), totalGrossLbl, new Label("Total VAT:"), totalVatLbl);
        Button payCashBtn = new Button("Pay CASH");
        payCashBtn.setOnAction(e -> checkout("CASH"));
        Button payCardBtn = new Button("Pay CARD");
        payCardBtn.setOnAction(e -> checkout("CARD"));

        HBox actions = new HBox(10, payCashBtn, payCardBtn);
        VBox bottom = new VBox(10, totals, actions);

        // Autocomplete wiring
        productField.textProperty().addListener((obs, ov, nv) -> {
            suggestions.hide();
            suggestDebounce.stop();
            if (nv == null || nv.trim().length() < 1)
                return;
            suggestDebounce.setOnFinished(e -> showSuggestions(nv.trim()));
            suggestDebounce.playFromStart();
        });
        productField.focusedProperty().addListener((o, oldV, newV) -> {
            if (!newV)
                suggestions.hide();
        });

        root.setTop(new VBox(10, title, top));
        root.setCenter(table);
        root.setBottom(bottom);

        recalcTotals();
    }

    private void showSuggestions(String q) {
        List<Product> matches = productRepo.searchByQuery(q, 10);
        if (matches.isEmpty()) {
            suggestions.hide();
            return;
        }
        List<CustomMenuItem> items = new ArrayList<>();
        for (Product p : matches) {
            String label = (p.getName() == null ? "" : p.getName()) +
                    (p.getBarcode() == null ? "" : " [" + p.getBarcode() + "]");
            Label l = new Label(label);
            CustomMenuItem cmi = new CustomMenuItem(l, true);
            cmi.setOnAction(e -> {
                productField
                        .setText(p.getName() != null ? p.getName() : (p.getBarcode() != null ? p.getBarcode() : ""));
                productField.positionCaret(productField.getText().length());
                suggestions.hide();
            });
            items.add(cmi);
        }
        suggestions.getItems().setAll(items);
        if (!suggestions.isShowing()) {
            suggestions.show(productField, Side.BOTTOM, 0, 0);
        }
    }

    private void onAddToCart() {
        String key = productField.getText().trim();
        if (key.isEmpty()) {
            alert("Validation", "Enter a barcode or product name.");
            return;
        }
        double qty;
        try {
            qty = Double.parseDouble(qtyField.getText().trim());
        } catch (NumberFormatException ex) {
            alert("Validation", "Qty must be a number.");
            return;
        }
        if (qty <= 0) {
            alert("Validation", "Qty must be > 0.");
            return;
        }

        Product p = productRepo.findByBarcodeOrName(key);
        if (p == null) {
            alert("Not found", "No product with that barcode or name.");
            return;
        }

        Optional<CartLine> existing = cart.stream().filter(cl -> cl.getProduct().getId().equals(p.getId())).findFirst();
        if (existing.isPresent()) {
            existing.get().setQty(existing.get().getQty() + qty);
        } else {
            cart.add(new CartLine(p, qty));
        }
        productField.clear();
        qtyField.setText("1");
        recalcTotals();
    }

    private void checkout(String method) {
        if (cart.isEmpty()) {
            alert("Cart empty", "Add at least one item.");
            return;
        }
        // Build sale items
        List<SaleRepo.SaleItem> items = new ArrayList<>();
        for (CartLine cl : cart) {
            Product p = cl.getProduct();
            items.add(new SaleRepo.SaleItem(
                    p.getId(),
                    cl.getQty(),
                    p.getPriceGross(),
                    p.getVatRate()));
        }
        try {
            int saleId = saleRepo.createSale(items, method);
            alert("Sale completed", "Sale #" + saleId + " registered.");
            cart.clear();
            recalcTotals();
            if (onSaleCompleted != null)
                onSaleCompleted.run();
        } catch (IllegalStateException ex) {
            alert("Insufficient stock", ex.getMessage());
        } catch (Exception ex) {
            alert("Error", ex.getMessage());
        }
    }

    private void recalcTotals() {
        double gross = 0.0;
        double vat = 0.0;
        for (CartLine cl : cart) {
            double lineGross = cl.getQty() * cl.getProduct().getPriceGross();
            gross += lineGross;
            double unitNet = cl.getProduct().getPriceGross() / (1.0 + cl.getProduct().getVatRate() / 100.0);
            vat += cl.getQty() * (cl.getProduct().getPriceGross() - unitNet);
        }
        totalGrossLbl.setText(fmtTND(gross));
        totalVatLbl.setText(fmtTND(vat));
    }

    private String fmtTND(double v) {
        return "TND " + String.format(java.util.Locale.US, "%.3f", v);
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(title);
        a.showAndWait();
    }

    public Parent getRoot() {
        return root;
    }

    public void setOnSaleCompleted(Runnable r) {
        this.onSaleCompleted = r;
    }

    // Cart line model
    public static class CartLine {
        private final Product product;
        private final DoubleProperty qty = new SimpleDoubleProperty(1.0);

        public CartLine(Product product, double qty) {
            this.product = product;
            this.qty.set(qty);
        }

        public Product getProduct() {
            return product;
        }

        public double getQty() {
            return qty.get();
        }

        public void setQty(double v) {
            qty.set(v);
        }

        public DoubleProperty qtyProperty() {
            return qty;
        }
    }
}
