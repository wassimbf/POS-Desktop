PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS category (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS product (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  barcode TEXT,
  name TEXT NOT NULL,
  category_id INTEGER,
  price_gross REAL NOT NULL CHECK (price_gross >= 0),
  vat_rate REAL NOT NULL CHECK (vat_rate >= 0),
  stock_qty REAL NOT NULL DEFAULT 0 CHECK (stock_qty >= 0),
  reorder_threshold REAL NOT NULL DEFAULT 0,
  cost_price REAL,
  active INTEGER NOT NULL DEFAULT 1,
  FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE TABLE IF NOT EXISTS sale (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  datetime TEXT NOT NULL,
  cashier_id INTEGER,
  total_gross REAL NOT NULL,
  total_vat REAL NOT NULL,
  payment_method TEXT NOT NULL CHECK (payment_method IN ('CASH','CARD')),
  status TEXT NOT NULL CHECK (status IN ('COMPLETED','CANCELLED'))
);

CREATE TABLE IF NOT EXISTS sale_item (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sale_id INTEGER NOT NULL,
  product_id INTEGER NOT NULL,
  qty REAL NOT NULL CHECK (qty > 0),
  unit_price_gross REAL NOT NULL CHECK (unit_price_gross >= 0),
  vat_rate REAL NOT NULL CHECK (vat_rate >= 0),
  discount REAL NOT NULL DEFAULT 0 CHECK (discount >= 0),
  FOREIGN KEY (sale_id) REFERENCES sale(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE IF NOT EXISTS stock_movement (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  product_id INTEGER NOT NULL,
  type TEXT NOT NULL CHECK (type IN ('RECEIPT','ADJUST','SALE')),
  qty REAL NOT NULL,
  datetime TEXT NOT NULL,
  reference TEXT,
  note TEXT,
  FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE INDEX IF NOT EXISTS idx_product_barcode ON product(barcode);
CREATE INDEX IF NOT EXISTS idx_product_name ON product(name);
CREATE INDEX IF NOT EXISTS idx_sale_datetime ON sale(datetime);
