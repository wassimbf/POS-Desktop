package com.superette.model;

public class Settings {
    private String storeName;
    private String address;
    private String phone;
    private String taxId;
    private String currency; // e.g., TND
    private Double defaultVatRate; // e.g., 19.0
    private String receiptFooter;

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getDefaultVatRate() {
        return defaultVatRate;
    }

    public void setDefaultVatRate(Double defaultVatRate) {
        this.defaultVatRate = defaultVatRate;
    }

    public String getReceiptFooter() {
        return receiptFooter;
    }

    public void setReceiptFooter(String receiptFooter) {
        this.receiptFooter = receiptFooter;
    }
}
