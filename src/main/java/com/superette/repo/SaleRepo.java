package com.superette.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.superette.db.Database;

public class SaleRepo {

    public static class SaleItem {
        public final int productId;
        public final double qty;
        public final double unitPriceGross;
        public final double vatRate;

        public SaleItem(int productId, double qty, double unitPriceGross, double vatRate) {
            this.productId = productId;
            this.qty = qty;
            this.unitPriceGross = unitPriceGross;
            this.vatRate = vatRate;
        }
    }

    public static class SaleSummary {
        public int id;
        public String datetime;
        public double totalGross;
        public double totalVat;
        public String paymentMethod;
    }

    public static class SaleLineDetail {
        public String productName;
        public String barcode;
        public double qty;
        public double unitPriceGross;
        public double vatRate;
    }

    public int createSale(List<SaleItem> items, String paymentMethod) {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Cart is empty");
        if (!"CASH".equals(paymentMethod) && !"CARD".equals(paymentMethod)) {
            throw new IllegalArgumentException("Invalid payment method");
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1) Stock checks
                for (SaleItem it : items) {
                    double stock = getCurrentStock(conn, it.productId);
                    if (it.qty <= 0)
                        throw new IllegalArgumentException("Quantity must be > 0");
                    if (stock < it.qty) {
                        throw new IllegalStateException("Insufficient stock for product ID " + it.productId +
                                ": have " + stock + ", need " + it.qty);
                    }
                }

                // 2) Totals
                double totalGross = 0.0;
                double totalVat = 0.0;
                for (SaleItem it : items) {
                    double lineGross = it.qty * it.unitPriceGross;
                    totalGross += lineGross;
                    double net = it.unitPriceGross / (1.0 + it.vatRate / 100.0);
                    totalVat += it.qty * (it.unitPriceGross - net);
                }

                // 3) Insert sale
                int saleId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO sale(datetime, cashier_id, total_gross, total_vat, payment_method, status) " +
                                "VALUES(datetime('now'), NULL, ?, ?, ?, 'COMPLETED')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setDouble(1, totalGross);
                    ps.setDouble(2, totalVat);
                    ps.setString(3, paymentMethod);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            saleId = keys.getInt(1);
                        } else {
                            try (Statement st = conn.createStatement();
                                    ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                                rs.next();
                                saleId = rs.getInt(1);
                            }
                        }
                    }
                }

                // 4) Insert sale items, decrement stock, log movements
                try (PreparedStatement insItem = conn.prepareStatement(
                        "INSERT INTO sale_item(sale_id, product_id, qty, unit_price_gross, vat_rate) VALUES(?,?,?,?,?)");
                        PreparedStatement decStock = conn.prepareStatement(
                                "UPDATE product SET stock_qty = stock_qty - ? WHERE id = ?");
                        PreparedStatement insMov = conn.prepareStatement(
                                "INSERT INTO stock_movement(product_id, type, qty, datetime, reference, note) " +
                                        "VALUES(?, 'SALE', ?, datetime('now'), ?, ?)")) {
                    for (SaleItem it : items) {
                        insItem.setInt(1, saleId);
                        insItem.setInt(2, it.productId);
                        insItem.setDouble(3, it.qty);
                        insItem.setDouble(4, it.unitPriceGross);
                        insItem.setDouble(5, it.vatRate);
                        insItem.addBatch();

                        decStock.setDouble(1, it.qty);
                        decStock.setInt(2, it.productId);
                        decStock.addBatch();

                        insMov.setInt(1, it.productId);
                        insMov.setDouble(2, -it.qty);
                        insMov.setString(3, "SALE");
                        insMov.setString(4, null);
                        insMov.addBatch();
                    }
                    insItem.executeBatch();
                    decStock.executeBatch();
                    insMov.executeBatch();
                }

                conn.commit();
                return saleId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("createSale failed", e);
        }
    }

    private double getCurrentStock(Connection conn, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT stock_qty FROM product WHERE id = ?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new IllegalArgumentException("Product not found id=" + productId);
                return rs.getDouble(1);
            }
        }
    }

    // History: list sales by date range (inclusive)
    public List<SaleSummary> listSales(LocalDate from, LocalDate to) {
        String sql = "SELECT id, datetime, total_gross, total_vat, payment_method " +
                "FROM sale WHERE date(datetime) BETWEEN ? AND ? " +
                "ORDER BY datetime DESC, id DESC";
        List<SaleSummary> out = new ArrayList<>();
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SaleSummary s = new SaleSummary();
                    s.id = rs.getInt("id");
                    s.datetime = rs.getString("datetime");
                    s.totalGross = rs.getDouble("total_gross");
                    s.totalVat = rs.getDouble("total_vat");
                    s.paymentMethod = rs.getString("payment_method");
                    out.add(s);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("listSales failed", e);
        }
        return out;
    }

    // Details: lines for a given sale
    public List<SaleLineDetail> saleDetails(int saleId) {
        String sql = "SELECT p.name AS product_name, p.barcode, si.qty, si.unit_price_gross, si.vat_rate " +
                "FROM sale_item si JOIN product p ON p.id = si.product_id " +
                "WHERE si.sale_id = ? ORDER BY si.rowid";
        List<SaleLineDetail> out = new ArrayList<>();
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SaleLineDetail d = new SaleLineDetail();
                    d.productName = rs.getString("product_name");
                    d.barcode = rs.getString("barcode");
                    d.qty = rs.getDouble("qty");
                    d.unitPriceGross = rs.getDouble("unit_price_gross");
                    d.vatRate = rs.getDouble("vat_rate");
                    out.add(d);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("saleDetails failed", e);
        }
        return out;
    }

    public SaleSummary getSale(int saleId) {
        String sql = "SELECT id, datetime, total_gross, total_vat, payment_method FROM sale WHERE id=?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SaleSummary s = new SaleSummary();
                    s.id = rs.getInt("id");
                    s.datetime = rs.getString("datetime");
                    s.totalGross = rs.getDouble("total_gross");
                    s.totalVat = rs.getDouble("total_vat");
                    s.paymentMethod = rs.getString("payment_method");
                    return s;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("getSale failed", e);
        }
        return null;
    }
}
