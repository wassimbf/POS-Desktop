package com.superette.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.superette.db.Database;

public class StockRepo {

    // Adds stock (receipt) and logs the movement
    public void addReceipt(int productId, double qty, String reference, String note) {
        if (qty <= 0)
            throw new IllegalArgumentException("Quantity must be > 0");
        String update = "UPDATE product SET stock_qty = stock_qty + ? WHERE id = ?";
        String movement = "INSERT INTO stock_movement(product_id, type, qty, datetime, reference, note) " +
                "VALUES(?, 'RECEIPT', ?, datetime('now'), ?, ?)";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(update);
                    PreparedStatement ps2 = conn.prepareStatement(movement)) {
                ps1.setDouble(1, qty);
                ps1.setInt(2, productId);
                ps1.executeUpdate();

                ps2.setInt(1, productId);
                ps2.setDouble(2, qty);
                ps2.setString(3, reference);
                ps2.setString(4, note);
                ps2.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("addReceipt failed", e);
        }
    }

    // Helper to find product by barcode or name for quick entry
    public Integer findProductIdByBarcodeOrName(String barcodeOrName) {
        String sql = "SELECT id FROM product WHERE barcode = ? OR name = ? LIMIT 1";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcodeOrName);
            ps.setString(2, barcodeOrName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("id");
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findProductId failed", e);
        }
    }
}
