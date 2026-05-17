## Yogasri POS System (Utensil Shop Billing Software) — Java Swing + MySQL (MVC)

Simple, fast billing software for utensil shops.

### Tech
- **Java**: 17+
- **UI**: Java Swing
- **DB**: MySQL 8+ (JDBC)
- **Architecture**: MVC (Models + DAO/Services + Swing Views/Controllers)

### What’s included
- **Login** (Admin/Staff)
- **Product Management**: add/update/delete, instant search, table view
- **Billing**: product search, cart, GST + discount, unique invoice number, cash/UPI, printable bill
- **Inventory**: auto stock reduction after billing, low-stock list
- **Customers**: add customer, view customer sales history, optional credit flag per sale
- **Reports**: daily totals, monthly summary (basic)

### Project structure
- `src/` Java source (packages under `yogasri.pos`)
- `sql/` MySQL schema + sample data
- `lib/` JDBC driver JAR
- `bin/` compiled classes output

### Setup (MySQL)
1. Start MySQL and create the database + tables:

```sql
-- Run this file in MySQL Workbench / CLI:
SOURCE sql/yogasri_pos_schema.sql;
SOURCE sql/yogasri_pos_sample_data.sql;
```

2. Update DB config in `app.properties`:
- `db.url=jdbc:mysql://localhost:3306/yogasri_pos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
- `db.user=root`
- `db.password=your_password`

### Build & run (no Maven/Gradle)
From the project folder:

```powershell
.\run.ps1
```

Or manually:

```powershell
javac -cp ".;lib\mysql-connector-j-9.7.0.jar" -d bin (Get-ChildItem -Recurse -Filter *.java src | % FullName)
java -cp ".;bin;lib\mysql-connector-j-9.7.0.jar" yogasri.pos.App
```

### Default users
- **Admin**: `*****` / `*****`
- **Staff**: `*****` / `*****`

