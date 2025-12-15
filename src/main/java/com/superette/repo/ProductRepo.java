package com.superette.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.superette.db.Database;
import com.superette.model.Product;

public class ProductRepo {

    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT id, barcode, name, category_id, price_gross, vat_rate, stock_qty, " +
                "reorder_threshold, cost_price, active FROM product ORDER BY name";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed", e);
        }
        return list;
    }

    public Product findById(int id) {
        String sql = "SELECT id, barcode, name, category_id, price_gross, vat_rate, stock_qty, " +
                "reorder_threshold, cost_price, active FROM product WHERE id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return map(rs);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
    }

    public void insert(Product p) {
        String sql = "INSERT INTO product(barcode, name, category_id, price_gross, vat_rate, stock_qty, " +
                "reorder_threshold, cost_price, active) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getBarcode());
            ps.setString(2, p.getName());
            if (p.getCategoryId() == null)
                ps.setNull(3, Types.INTEGER);
            else
                ps.setInt(3, p.getCategoryId());
            ps.setDouble(4, p.getPriceGross());
            ps.setDouble(5, p.getVatRate());
            ps.setDouble(6, p.getStockQty());
            ps.setDouble(7, p.getReorderThreshold());
            if (p.getCostPrice() == null)
                ps.setNull(8, Types.REAL);
            else
                ps.setDouble(8, p.getCostPrice());
            ps.setInt(9, p.isActive() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insert failed", e);
        }
    }

    public void update(Product p) {
        if (p.getId() == null)
            throw new IllegalArgumentException("Product id is null");
        String sql = "UPDATE product SET barcode=?, name=?, price_gross=?, vat_rate=?, reorder_threshold=?, active=? WHERE id=?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getBarcode());
            ps.setString(2, p.getName());
            ps.setDouble(3, p.getPriceGross());
            ps.setDouble(4, p.getVatRate());
            ps.setDouble(5, p.getReorderThreshold());
            ps.setInt(6, p.isActive() ? 1 : 0);
            ps.setInt(7, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update failed", e);
        }
    }

    // Quick finder by barcode or exact name (used elsewhere)
    public Product findByBarcodeOrName(String key) {
        String sql = "SELECT id, barcode, name, category_id, price_gross, vat_rate, stock_qty, " +
                "reorder_threshold, cost_price, active FROM product WHERE barcode = ? OR name = ? LIMIT 1";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return map(rs);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByBarcodeOrName failed", e);
        }
    }

    // Substring search for autocomplete (case-insensitive on name; barcode
    // exact/substring)
    public List<Product> searchByQuery(String q, int limit) {
        String like = "%" + q.toLowerCase() + "%";
        String sql = "SELECT id, barcode, name, category_id, price_gross, vat_rate, stock_qty, " +
                "reorder_threshold, cost_price, active " +
                "FROM product " +
                "WHERE active = 1 AND (LOWER(name) LIKE ? OR barcode LIKE ?) " +
                "ORDER BY name LIMIT ?";
        List<Product> out = new ArrayList<>();
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, "%" + q + "%");
            ps.setInt(3, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("searchByQuery failed", e);
        }
        return out;
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setBarcode(rs.getString("barcode"));
        p.setName(rs.getString("name"));
        int cat = rs.getInt("category_id");
        p.setCategoryId(rs.wasNull() ? null : cat);
        p.setPriceGross(rs.getDouble("price_gross"));
        p.setVatRate(rs.getDouble("vat_rate"));
        p.setStockQty(rs.getDouble("stock_qty"));
        p.setReorderThreshold(rs.getDouble("reorder_threshold"));
        double cp = rs.getDouble("cost_price");
        p.setCostPrice(rs.wasNull() ? null : cp);
        p.setActive(rs.getInt("active") == 1);
        return p;
    }
}
