package com.superette;

import com.superette.db.Database;
import com.superette.ui.CashRegisterController;
import com.superette.ui.ProductsController;
import com.superette.ui.SettingsController;
import com.superette.ui.StockEntryController;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        ProductsController products = new ProductsController();
        StockEntryController stockEntry = new StockEntryController();
        CashRegisterController sales = new CashRegisterController();
        SettingsController settings = new SettingsController();

        // Cross-refresh
        stockEntry.setOnStockChanged(products::refresh);
        products.setOnProductsChanged(stockEntry::refresh);
        sales.setOnSaleCompleted(() -> {
            products.refresh();
            stockEntry.refresh();
        });

        TabPane tabs = new TabPane();
        Tab productsTab = new Tab("Products", products.getRoot());
        productsTab.setClosable(false);
        Tab stockTab = new Tab("Stock Entry", stockEntry.getRoot());
        stockTab.setClosable(false);
        Tab salesTab = new Tab("Sales", sales.getRoot());
        salesTab.setClosable(false);
        Tab settingsTab = new Tab("Settings", settings.getRoot());
        settingsTab.setClosable(false);

        productsTab.setOnSelectionChanged(e -> {
            if (productsTab.isSelected())
                products.refresh();
        });
        stockTab.setOnSelectionChanged(e -> {
            if (stockTab.isSelected())
                stockEntry.refresh();
        });

        tabs.getTabs().addAll(productsTab, stockTab, salesTab, settingsTab);

        stage.setTitle("Superette POS");
        stage.setScene(new Scene(tabs, 1150, 780));
        stage.show();
    }

    public static void main(String[] args) {
        Database.init("data/superette.db");
        launch(args);
    }
}
