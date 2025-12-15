package com.superette.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.superette.db.Database;
import com.superette.model.Product;

public class LowStockService {

    public List<Product> lowStock() {
        String sql = "SELECT id, barcode, name, category_id, price_gross, vat_rate, stock_qty, " +
                "reorder_threshold, cost_price, active " +
                "FROM product WHERE active = 1 AND stock_qty <= reorder_threshold ORDER BY stock_qty ASC, name";
        List<Product> out = new ArrayList<>();
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Product p = map(rs);
                out.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("lowStock query failed", e);
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
