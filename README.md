# Superette POS Desktop

A lightweight JavaFX desktop Point-of-Sale for small stores. It manages products, stock entries, low‑stock alerts, and sales (cash register), with settings for store info. Data is persisted in SQLite.

## Features

- Products

  - Add products with name, barcode, price (gross, VAT‑inclusive), VAT rate, stock quantity, reorder threshold, active flag.
  - Edit products (double‑click or “Edit” button).
  - “Active” products appear in autocomplete and low‑stock; inactive are hidden but retained for history.

- Stock Entry (Receipts)

  - Increase stock by barcode or name, with quantity, reference, and note.
  - Low‑stock banner highlights items with `stock_qty <= reorder_threshold`.
  - Autocomplete suggestions by name or barcode.

- Sales (Cash Register)

  - Build a cart by product name/barcode with quantities.
  - Totals with VAT derived from gross price.
  - Payment methods: CASH, CARD.
  - Stock is decremented transactionally; stock movements recorded.

- Settings

  - Store name, address, phone, tax ID.
  - Currency (default: TND; amounts formatted to 3 decimals).
  - Default VAT (e.g., 19%).
  - Receipt footer text (for future receipt PDFs).

- UX details
  - Autocomplete in Stock Entry and Sales (substring on name, substring on barcode; only active products).
  - Cross‑screen refresh (products and low‑stock update after stock entry or sale).
  - UTF‑8 safe em dash in banners (`\u2014`) to avoid encoding issues.

## Tech Stack

- Java 17
- JavaFX 17+ (via Maven)
- SQLite (via org.xerial:sqlite-jdbc)
- SLF4J (optional binding; see Troubleshooting)

## Project Structure (key classes)

- UI
  - `ProductsController` — products table, add/edit, refresh hooks
  - `ProductEditDialog` — edit product fields
  - `StockEntryController` — receipts, low‑stock banner, autocomplete
  - `CashRegisterController` — cart, totals, checkout, autocomplete
  - `SettingsController` — settings form
  - `App` — tabs wiring and cross‑refresh
- Repositories
  - `ProductRepo` — CRUD, search, helpers
  - `StockRepo` — stock entry (receipts)
  - `SaleRepo` — transactional sales create/decrement/logging
  - `SettingsRepo` — single‑row settings table
- Model
  - `Product`, `Settings` (and others in your repo)

## Getting Started

Prerequisites

- JDK 17
- Internet access for Maven wrapper to download dependencies

Build and Run

- Windows:
  - `.\mvnw.cmd clean package`
  - `.\mvnw.cmd exec:java`
- macOS/Linux:
  - `./mvnw clean package`
  - `./mvnw exec:java`

First Run

- The app creates the SQLite DB at `data/superette.db`.
- Open the Settings tab to fill store information (currency TND, VAT 19, etc.).
- Add products on the Products tab.

## Usage Walkthrough

1. Add two products (example)

- Sugar 1kg: barcode 61300001, price gross 2.300, VAT 19, stock 0, threshold 5
- Milk 1L: barcode 61300002, price gross 1.200, VAT 19, stock 10, threshold 5

2. Low‑Stock

- Go to Stock Entry: banner shows “Sugar 1kg — stock: 0.00 (threshold: 5.00)”.

3. Receive stock

- Stock Entry: Sugar 1kg (or 61300001), quantity 6, reference INV‑TEST‑001 → Add Stock.
- Products: Sugar stock updates to 6; Low‑Stock banner clears.

4. Sell items

- Sales: add Sugar 1kg qty 2, Milk 1L qty 3 → Pay CASH.
- Products: Sugar 6→4, Milk 10→7; Low‑Stock updates if thresholds are crossed.

## Database

- File: `data/superette.db` (SQLite)
- Tables (non‑exhaustive): `product`, `stock_movement`, `sale`, `sale_item`, `settings`
- Useful checks (with sqlite3):
  - `SELECT name, stock_qty, reorder_threshold FROM product ORDER BY name;`
  - `SELECT type, product_id, qty, datetime, reference FROM stock_movement ORDER BY id DESC LIMIT 10;`
  - `SELECT id, datetime, total_gross, total_vat, payment_method FROM sale ORDER BY id DESC LIMIT 5;`

## Configuration and Defaults

- Currency: TND (display to 3 decimals)
- Default VAT: configurable in Settings (suggested 19%)
- Product fields validated to be non‑negative; quantities must be > 0
- Autocomplete returns up to 10 active products by substring

## Troubleshooting

- Encoding “mojibake” like `â€”` in banners:

  - Project uses UTF‑8; labels use Unicode escape `\u2014`. Ensure files are saved as UTF‑8.
  - In Maven, set:
    - `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`
    - `<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>`

- SLF4J warning (no binding):

  - Harmless. To enable logging, add one binding:
    - Quick: `org.slf4j:slf4j-simple:2.0.13` (runtime)
    - Full: `ch.qos.logback:logback-classic:1.5.6` (runtime)

- JavaFX startup info lines are normal.

## Roadmap

- Sales history view and receipt PDF (using Settings for store info)
- Product search filter on Products tab
- Packaging via `jpackage` (Windows installer)
- Reports (low‑stock CSV, sales per day, top products)

## License

Add your preferred license (e.g., MIT) or keep private.
