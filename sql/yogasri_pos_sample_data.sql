USE yogasri_pos;

-- Passwords are SHA-256 hashes (hex)
-- admin123 => 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
-- staff123 => 10176e7b7b24d317acfcf8d2064cfd2f24e154f7b5a96603077d5ef813d6a6b6

INSERT INTO users (username, password_hash, role)
VALUES
  ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN'),
  ('staff', '10176e7b7b24d317acfcf8d2064cfd2f24e154f7b5a96603077d5ef813d6a6b6', 'STAFF')
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO customers (name, phone)
VALUES
  ('Ravi Kumar', '9000000001'),
  ('Sita Devi', '9000000002')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO products (name, category, price, gst, stock, barcode)
VALUES
  ('Steel Plate - Medium', 'Steel', 120.00, 12.00, 50, 'STEELPLATE120'),
  ('Aluminium Kadai 2L', 'Aluminium', 450.00, 18.00, 20, 'ALUKADAI450'),
  ('Plastic Bowl Set', 'Plastic', 199.00, 5.00, 15, 'PLBOWL199'),
  ('Steel Spoon Set', 'Steel', 99.00, 12.00, 8, 'SPOON99')
ON DUPLICATE KEY UPDATE price = VALUES(price), gst = VALUES(gst), stock = VALUES(stock);

