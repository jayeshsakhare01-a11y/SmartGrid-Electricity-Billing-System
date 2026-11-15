# ⚡ SmartGrid Electricity Billing & Consumer Management System

An enterprise-grade **Electricity Billing & Tariff Management System** built with **Java** demonstrating core **Object-Oriented Programming (OOP)** principles. Features a dual-interface architecture: a native **Java Swing Desktop Application** and an embedded real-time **Web Management Portal**.

---

## 🌟 Key Features

- **Multi-Category Consumer Management:** Register, update, search, and manage Residential, Commercial, and Industrial accounts.
- **Slab-Based Tiered Tariff Engine:** Automated computation across tiered energy consumption slabs with fixed charges and state electricity duty taxes.
- **10-Year Historical Records:** In-depth 10-year tracking of electricity consumption (kWh) and revenues (2016–2026).
- **Embedded Web Server & REST API:** Built-in Java HTTP server serving both the web dashboard and REST API endpoints (`/api/customers`, `/api/bills`) with zero external dependencies.
- **Dual-Interface Real-time Sync:** Changes made from the Desktop GUI or Web Portal immediately synchronize with the underlying CSV data repository.
- **Print & Export Ready:** Generates standardized utility bills formatted for printing and text export.

---

## 📐 OOP Principles Demonstrated

1. **Encapsulation:** Domain entities (`Customer` and `Bill`) encapsulate state variables and data transformation logic (`toCsv()`, `fromCsv()`).
2. **Inheritance:** `oop extends JFrame` for desktop graphical display; `ApiError extends Exception` for structured error handling.
3. **Abstraction:** Tariff rules and file persistence layers are isolated from user-facing presentations.
4. **Polymorphism:** Overridden `toString()`, dynamic HTTP handler routing, and table event listener callbacks.
5. **File I/O & Persistence:** Robust reading and writing of comma-separated records via `java.nio.file.Files`.
6. **Multithreading & Concurrency:** Concurrently executes a thread pool executor (`Executors.newFixedThreadPool(4)`) for network requests alongside the Swing Event Dispatch Thread (`SwingUtilities.invokeLater`).

---

## ⚡ Tariff Structure

| Category | Slab 1 (0–100 kWh) | Slab 2 (101–300 kWh) | Slab 3 (>300 kWh) | Fixed Charge | Electricity Duty Tax |
|---|---|---|---|---|---|
| **Residential** | ₹4.50 / unit | ₹7.00 / unit | ₹10.00 / unit | ₹50.00 | 5% on (Energy + Fixed) |
| **Commercial** | ₹8.00 / unit | ₹10.00 / unit | ₹12.00 / unit | ₹150.00 | 5% on (Energy + Fixed) |
| **Industrial** | ₹9.00 / unit | ₹11.00 / unit | ₹14.00 / unit | ₹300.00 | 5% on (Energy + Fixed) |

---

## 📁 Repository Structure

```text
├── customers.csv             # Consumer database
├── bills.csv                 # 10-year monthly invoice history
├── oop.java                  # Main application: Models, Tariffs, Server, GUI
├── tools/
│   └── generate_history.py   # Historical simulation generator
└── web/
    ├── index.html            # Web dashboard portal interface
    └── images/
        └── logo.png          # SmartGrid logo
```

---

## 🚀 Getting Started

### Prerequisites
- **Java Development Kit (JDK 17+)**

### Run Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/jayeshsakhare01-a11y/SmartGrid-Electricity-Billing-System.git
   cd SmartGrid-Electricity-Billing-System
   ```

2. **Compile the application:**
   ```bash
   javac -encoding UTF-8 oop.java
   ```

3. **Launch the core:**
   ```bash
   java oop
   ```

4. **Access the Web Portal:**
   Open your browser and navigate to:
   **[http://localhost:8080](http://localhost:8080)**

---

## 📡 REST API Documentation

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/customers` | Retrieve all customer records |
| `POST` | `/api/customers` | Register a new consumer |
| `PUT` | `/api/customers/{id}` | Update consumer profile |
| `DELETE` | `/api/customers/{id}` | Delete consumer profile |
| `GET` | `/api/bills` | Retrieve all invoice records |
| `POST` | `/api/bills/preview` | Preview slab calculations without saving |
| `POST` | `/api/bills` | Generate and record a new monthly bill |
| `DELETE` | `/api/bills/{invoiceNo}` | Delete an invoice record |

---

## 👤 Author
- **Jayesh Sakhare** ([@jayeshsakhare01-a11y](https://github.com/jayeshsakhare01-a11y))
