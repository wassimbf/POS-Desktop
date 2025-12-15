package com.superette.ui;

import java.util.ArrayList;
import java.util.List;

import com.superette.model.Product;
import com.superette.repo.ProductRepo;
import com.superette.repo.StockRepo;
import com.superette.service.LowStockService;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class StockEntryController {
    private final VBox root = new VBox(10);
    private final StockRepo stockRepo = new StockRepo();
    private final LowStockService lowStockService = new LowStockService();
    private final ProductRepo productRepo = new ProductRepo();
    private Runnable onStockChanged;

    // Form inputs
    private final TextField productField = new TextField(); // barcode or name
    private final TextField qtyField = new TextField();
    private final TextField refField = new TextField();
    private final TextArea noteArea = new TextArea();

    // Autocomplete
    private final ContextMenu suggestions = new ContextMenu();
    private final PauseTransition suggestDebounce = new PauseTransition(Duration.millis(200));

    // Low stock banner
    private final VBox lowStockBox = new VBox(5);

    public StockEntryController() {
        root.setPadding(new Insets(10));

        Label title = new Label("Stock Entry (Receipt)");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        productField.setPromptText("Barcode or Product name");
        qtyField.setPromptText("Quantity (+)");
        refField.setPromptText("Supplier ref / document");
        noteArea.setPromptText("Note (optional)");
        noteArea.setPrefRowCount(3);

        Button addBtn = new Button("Add Stock");
        addBtn.setOnAction(e -> onAddStock());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.add(new Label("Barcode/Name:"), 0, 0);
        form.add(productField, 1, 0);
        form.add(new Label("Quantity:"), 0, 1);
        form.add(qtyField, 1, 1);
        form.add(new Label("Reference:"), 0, 2);
        form.add(refField, 1, 2);
        form.add(new Label("Note:"), 0, 3);
        form.add(noteArea, 1, 3);
        form.add(addBtn, 1, 4);

        Label lowTitle = new Label("Low Stock Alerts");
        lowTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        lowStockBox.setPadding(new Insets(10));
        lowStockBox.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #ffeeba;");

        // Initial load
        refreshLowStock();

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
        productField.setOnAction(e -> onAddStock()); // press Enter to submit

        root.getChildren().addAll(title, form, lowTitle, lowStockBox);
    }

    // Call this when the tab is shown
    public void refresh() {
        refreshLowStock();
    }

    private void onAddStock() {
        try {
            String key = productField.getText().trim();
            if (key.isEmpty()) {
                alert("Validation", "Enter a barcode or product name.");
                return;
            }
            Integer productId = stockRepo.findProductIdByBarcodeOrName(key);
            if (productId == null) {
                alert("Not found", "No product with that barcode or name.");
                return;
            }
            double qty = Double.parseDouble(qtyField.getText().trim());
            if (qty <= 0) {
                alert("Validation", "Quantity must be > 0.");
                return;
            }

            stockRepo.addReceipt(productId, qty, refField.getText().trim(), noteArea.getText().trim());
            alert("Done", "Stock updated.");
            productField.clear();
            qtyField.clear();
            refField.clear();
            noteArea.clear();
            refreshLowStock();
            if (onStockChanged != null)
                onStockChanged.run();
        } catch (NumberFormatException ex) {
            alert("Validation", "Quantity must be a number.");
        } catch (Exception ex) {
            alert("Error", ex.getMessage());
        }
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

    private void refreshLowStock() {
        lowStockBox.getChildren().clear();
        List<Product> low = lowStockService.lowStock();
        if (low.isEmpty()) {
            lowStockBox.getChildren().add(new Label("All good. No low stock items."));
            return;
        }
        for (Product p : low) {
            Label item = new Label(String.format("%s \u2014 stock: %.2f (threshold: %.2f)",
                    p.getName(), p.getStockQty(), p.getReorderThreshold()));
            item.setStyle("-fx-text-fill: #856404;");
            lowStockBox.getChildren().add(item);
        }
    }

    public void setOnStockChanged(Runnable r) {
        this.onStockChanged = r;
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
