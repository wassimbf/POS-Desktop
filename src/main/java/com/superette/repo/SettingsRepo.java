package com.superette.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import com.superette.db.Database;
import com.superette.model.Settings;

public class SettingsRepo {

    public void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS settings (" +
                "id INTEGER PRIMARY KEY CHECK (id = 1)," +
                "store_name TEXT," +
                "address TEXT," +
                "phone TEXT," +
                "tax_id TEXT," +
                "currency TEXT," +
                "default_vat_rate REAL," +
                "receipt_footer TEXT" +
                ")";
        try (Connection conn = Database.getConnection();
                Statement st = conn.createStatement()) {
            st.execute(sql);
            // Ensure one row exists
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR IGNORE INTO settings(id, currency, default_vat_rate) VALUES(1, 'TND', 19.0)")) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("ensureTable failed", e);
        }
    }

    public Settings load() {
        ensureTable();
        String sql = "SELECT store_name, address, phone, tax_id, currency, default_vat_rate, receipt_footer FROM settings WHERE id=1";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            Settings s = new Settings();
            if (rs.next()) {
                s.setStoreName(rs.getString(1));
                s.setAddress(rs.getString(2));
                s.setPhone(rs.getString(3));
                s.setTaxId(rs.getString(4));
                s.setCurrency(rs.getString(5));
                double vat = rs.getDouble(6);
                s.setDefaultVatRate(rs.wasNull() ? null : vat);
                s.setReceiptFooter(rs.getString(7));
            }
            return s;
        } catch (SQLException e) {
            throw new RuntimeException("load settings failed", e);
        }
    }

    public void save(Settings s) {
        ensureTable();
        String sql = "UPDATE settings SET store_name=?, address=?, phone=?, tax_id=?, currency=?, default_vat_rate=?, receipt_footer=? WHERE id=1";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nullToEmpty(s.getStoreName()));
            ps.setString(2, nullToEmpty(s.getAddress()));
            ps.setString(3, nullToEmpty(s.getPhone()));
            ps.setString(4, nullToEmpty(s.getTaxId()));
            ps.setString(5, s.getCurrency() == null ? "TND" : s.getCurrency());
            if (s.getDefaultVatRate() == null)
                ps.setNull(6, Types.REAL);
            else
                ps.setDouble(6, s.getDefaultVatRate());
            ps.setString(7, nullToEmpty(s.getReceiptFooter()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("save settings failed", e);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
