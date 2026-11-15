import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

/**
 * ============================================================================
 * SMARTGRID ELECTRICITY BILLING SYSTEM (OOP PROJECT)
 * ============================================================================
 * Key OOP Principles Demonstrated:
 * 1. ENCAPSULATION : Classes bundle data (fields) and methods (logic &
 * validation).
 * 2. INHERITANCE : oop extends JFrame for desktop GUI; custom ApiError extends
 * Exception.
 * 3. ABSTRACTION : Tariff calculation & file persistence are isolated from the
 * UI.
 * 4. POLYMORPHISM : Overridden toString(), interface implementations
 * (DocumentListener).
 * 5. FILE I/O : CSV data persistence using java.nio.file.Files.
 * 6. MULTITHREADING : Built-in HttpServer thread pool + Swing Event Dispatch
 * Thread.
 * ============================================================================
 */
public class oop extends JFrame {

    // ========================================================================
    // 1. MODEL CLASSES (OOP Concept: Encapsulation)
    // ========================================================================

    /**
     * Customer represents an electricity consumer account.
     */
    static class Customer {
        String id, name, address, phone, category;

        Customer(String id, String name, String address, String phone, String category) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.phone = phone;
            this.category = category;
        }

        String toCsv() {
            return String.join(",", id, name, address, phone, category);
        }

        static Customer fromCsv(String line) {
            String[] p = line.split(",", -1);
            return new Customer(p[0], p[1], p[2], p[3], p[4]);
        }

        @Override
        public String toString() {
            return id + " - " + name + " (" + category + ")";
        }
    }

    /**
     * Bill represents a monthly electricity consumption invoice.
     */
    static class Bill {
        String invoiceNo, customerId, month, date;
        int year, prev, curr, units;
        double energy, fixed, tax, total;

        String toCsv() {
            return String.join(",", invoiceNo, customerId, month, String.valueOf(year),
                    String.valueOf(prev), String.valueOf(curr), String.valueOf(units),
                    String.valueOf(energy), String.valueOf(fixed), String.valueOf(tax),
                    String.valueOf(total), date);
        }

        static Bill fromCsv(String line) {
            String[] p = line.split(",", -1);
            Bill b = new Bill();
            b.invoiceNo = p[0];
            b.customerId = p[1];
            b.month = p[2];
            b.year = Integer.parseInt(p[3]);
            b.prev = Integer.parseInt(p[4]);
            b.curr = Integer.parseInt(p[5]);
            b.units = Integer.parseInt(p[6]);
            b.energy = Double.parseDouble(p[7]);
            b.fixed = Double.parseDouble(p[8]);
            b.tax = Double.parseDouble(p[9]);
            b.total = Double.parseDouble(p[10]);
            b.date = p[11];
            return b;
        }
    }

    // ========================================================================
    // 2. TARIFF LOGIC (OOP Concept: Abstraction & Business Rules)
    // ========================================================================

    static final String[] CATEGORIES = { "Residential", "Commercial", "Industrial" };
    static final String[] MONTHS = { "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December" };
    static final double TAX_RATE = 0.05; // 5% State Electricity Duty

    /**
     * Computes charges based on tiered slabs:
     * - Slab 1: 0 - 100 units
     * - Slab 2: 101 - 300 units
     * - Slab 3: Above 300 units
     * Returns: [energyCharge, fixedCharge, tax, totalAmount]
     */
    static double[] calculateTariff(String category, int units) {
        double r1, r2, r3, fixed;
        switch (category) {
            case "Commercial":
                r1 = 8.00;
                r2 = 10.00;
                r3 = 12.00;
                fixed = 150.00;
                break;
            case "Industrial":
                r1 = 9.00;
                r2 = 11.00;
                r3 = 14.00;
                fixed = 300.00;
                break;
            default: // Residential
                r1 = 4.50;
                r2 = 7.00;
                r3 = 10.00;
                fixed = 50.00;
                break;
        }

        double energy = 0;
        int remaining = units;
        int s1 = Math.min(remaining, 100);
        energy += s1 * r1;
        remaining -= s1;
        int s2 = Math.min(remaining, 200);
        energy += s2 * r2;
        remaining -= s2;
        energy += remaining * r3;

        double tax = (energy + fixed) * TAX_RATE;
        double total = energy + fixed + tax;
        return new double[] { energy, fixed, tax, total };
    }

    // ========================================================================
    // 3. DATA REPOSITORY & PERSISTENCE (File I/O & Exception Handling)
    // ========================================================================

    private final List<Customer> customers = new ArrayList<>();
    private final List<Bill> bills = new ArrayList<>();
    private static final Path CUST_FILE = Paths.get("customers.csv");
    private static final Path BILL_FILE = Paths.get("bills.csv");

    private void loadData() {
        try {
            if (Files.exists(CUST_FILE)) {
                for (String line : Files.readAllLines(CUST_FILE, StandardCharsets.UTF_8)) {
                    if (!line.trim().isEmpty())
                        customers.add(Customer.fromCsv(line));
                }
            }
            if (Files.exists(BILL_FILE)) {
                for (String line : Files.readAllLines(BILL_FILE, StandardCharsets.UTF_8)) {
                    if (!line.trim().isEmpty())
                        bills.add(Bill.fromCsv(line));
                }
            }
        } catch (Exception e) {
            System.err.println("Notice: Initializing data files. " + e.getMessage());
        }
    }

    private synchronized void saveData() {
        try {
            List<String> cLines = new ArrayList<>();
            for (Customer c : customers)
                cLines.add(c.toCsv());
            Files.write(CUST_FILE, cLines, StandardCharsets.UTF_8);

            List<String> bLines = new ArrayList<>();
            for (Bill b : bills)
                bLines.add(b.toCsv());
            Files.write(BILL_FILE, bLines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("File write error: " + e.getMessage());
        }
    }

    private Customer findCustomer(String id) {
        for (Customer c : customers)
            if (c.id.equalsIgnoreCase(id))
                return c;
        return null;
    }

    private String nextCustomerId() {
        int max = 0;
        for (Customer c : customers) {
            try {
                max = Math.max(max, Integer.parseInt(c.id.replaceAll("\\D+", "")));
            } catch (Exception ignored) {
            }
        }
        return String.format("C%03d", max + 1);
    }

    private int nextInvoiceNumber() {
        int max = 0;
        for (Bill b : bills) {
            try {
                max = Math.max(max, Integer.parseInt(b.invoiceNo.replaceAll("\\D+", "")));
            } catch (Exception ignored) {
            }
        }
        return max + 1;
    }

    // ========================================================================
    // 4. EMBEDDED HTTP SERVER & REST API (Network Programming)
    // ========================================================================

    static class ApiError extends Exception {
        ApiError(String message) {
            super(message);
        }
    }

    private JTextArea taLog;
    private String siteUrl = "";

    private void log(String msg) {
        String entry = new SimpleDateFormat("HH:mm:ss").format(new Date()) + "  " + msg + "\n";
        System.out.println(entry.trim());
        SwingUtilities.invokeLater(() -> {
            if (taLog != null) {
                taLog.append(entry);
                taLog.setCaretPosition(taLog.getDocument().getLength());
            }
        });
    }

    private void startWebServer() {
        for (int port = 8080; port < 8090; port++) {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext("/api/", this::handleApi);
                server.createContext("/", this::handleStatic);
                server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
                server.start();
                siteUrl = "http://localhost:" + port;
                log("Server running live at " + siteUrl);
                return;
            } catch (IOException ignored) {
            }
        }
        log("Error: Ports 8080-8089 are busy.");
    }

    private void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        try {
            ex.getResponseHeaders().set("Content-Type", type);
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().set("Cache-Control", "no-store");
            ex.sendResponseHeaders(code, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Serves frontend files from the 'web/' directory.
     */
    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/"))
            path = "/index.html";

        Path root = Paths.get("web").toAbsolutePath().normalize();
        Path file = root.resolve(path.substring(1)).normalize();

        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            send(ex, 404, "text/plain; charset=utf-8", "File not found.".getBytes(StandardCharsets.UTF_8));
            return;
        }

        String fileName = file.getFileName().toString().toLowerCase();
        String contentType = "text/html; charset=utf-8";
        if (fileName.endsWith(".css"))
            contentType = "text/css; charset=utf-8";
        else if (fileName.endsWith(".js"))
            contentType = "text/javascript; charset=utf-8";
        else if (fileName.endsWith(".png"))
            contentType = "image/png";
        else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
            contentType = "image/jpeg";
        else if (fileName.endsWith(".svg"))
            contentType = "image/svg+xml";

        send(ex, 200, contentType, Files.readAllBytes(file));
    }

    /**
     * REST API Request Router.
     */
    private void handleApi(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String[] parts = ex.getRequestURI().getPath().substring(5).split("/");
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        int code = 200;
        String response;
        try {
            Map<String, String> params = body.trim().isEmpty() ? new HashMap<>() : parseJson(body);
            response = routeApi(method, parts, params);
        } catch (ApiError e) {
            code = 400;
            response = "{\"error\":\"" + e.getMessage() + "\"}";
            log("API Warning: " + e.getMessage());
        } catch (Exception e) {
            code = 500;
            response = "{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}";
        }
        send(ex, code, "application/json; charset=utf-8", response.getBytes(StandardCharsets.UTF_8));
    }

    private synchronized String routeApi(String method, String[] parts, Map<String, String> in) throws Exception {
        String endpoint = parts[0];
        String id = parts.length > 1 ? parts[1] : null;

        if (endpoint.equals("customers")) {
            if (method.equals("GET"))
                return customersToJson();
            if (method.equals("POST"))
                return apiAddCustomer(in);
            if (method.equals("PUT") && id != null)
                return apiUpdateCustomer(id, in);
            if (method.equals("DELETE") && id != null)
                return apiDeleteCustomer(id);
        } else if (endpoint.equals("bills")) {
            if (method.equals("GET"))
                return billsToJson();
            if (method.equals("POST") && "preview".equals(id))
                return billToJson(makeBill(in, false));
            if (method.equals("POST"))
                return apiSaveBill(in);
            if (method.equals("DELETE") && id != null)
                return apiDeleteBill(id);
        }
        throw new ApiError("Endpoint not found: " + method + " /api/" + String.join("/", parts));
    }

    private String apiAddCustomer(Map<String, String> in) throws ApiError {
        String name = in.getOrDefault("name", "").trim();
        String address = in.getOrDefault("address", "").trim();
        String phone = in.getOrDefault("phone", "").trim();
        String category = in.getOrDefault("category", "Residential").trim();

        if (name.isEmpty() || address.isEmpty())
            throw new ApiError("Name and Address are required.");
        if (!phone.matches("\\d{10}"))
            throw new ApiError("Phone number must be exactly 10 digits.");

        Customer c = new Customer(nextCustomerId(), name, address, phone, category);
        customers.add(c);
        saveData();
        refreshSwingUi();
        log("Customer added: " + c.name + " (" + c.id + ")");
        return customerToJson(c);
    }

    private String apiUpdateCustomer(String id, Map<String, String> in) throws ApiError {
        Customer c = findCustomer(id);
        if (c == null)
            throw new ApiError("Customer not found.");
        c.name = in.getOrDefault("name", c.name).trim();
        c.address = in.getOrDefault("address", c.address).trim();
        c.phone = in.getOrDefault("phone", c.phone).trim();
        c.category = in.getOrDefault("category", c.category).trim();
        saveData();
        refreshSwingUi();
        log("Customer updated: " + c.name + " (" + c.id + ")");
        return customerToJson(c);
    }

    private String apiDeleteCustomer(String id) throws ApiError {
        Customer c = findCustomer(id);
        if (c == null)
            throw new ApiError("Customer not found.");
        for (Bill b : bills) {
            if (b.customerId.equalsIgnoreCase(id)) {
                throw new ApiError("Customer has existing bills and cannot be deleted.");
            }
        }
        customers.remove(c);
        saveData();
        refreshSwingUi();
        log("Customer deleted: " + c.name + " (" + c.id + ")");
        return "{\"ok\":true}";
    }

    private Bill makeBill(Map<String, String> in, boolean assignInvoice) throws ApiError {
        Customer c = findCustomer(in.get("customerId"));
        if (c == null)
            throw new ApiError("Please select a valid customer.");

        int prev, curr, year;
        try {
            prev = Integer.parseInt(in.get("prev"));
            curr = Integer.parseInt(in.get("curr"));
            year = Integer.parseInt(in.get("year"));
        } catch (Exception e) {
            throw new ApiError("Meter readings and year must be valid integers.");
        }

        if (prev < 0 || curr < 0)
            throw new ApiError("Meter readings cannot be negative.");
        if (curr < prev)
            throw new ApiError("Current reading cannot be lower than previous reading.");

        Bill b = new Bill();
        b.invoiceNo = assignInvoice ? String.format("INV%04d", nextInvoiceNumber()) : "(preview)";
        b.customerId = c.id;
        b.month = in.getOrDefault("month", "October");
        b.year = year;
        b.prev = prev;
        b.curr = curr;
        b.units = curr - prev;

        double[] r = calculateTariff(c.category, b.units);
        b.energy = r[0];
        b.fixed = r[1];
        b.tax = r[2];
        b.total = r[3];
        b.date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        return b;
    }

    private String apiSaveBill(Map<String, String> in) throws ApiError {
        Bill b = makeBill(in, true);
        for (Bill x : bills) {
            if (x.customerId.equalsIgnoreCase(b.customerId) && x.month.equalsIgnoreCase(b.month) && x.year == b.year) {
                throw new ApiError("A bill for " + b.month + " " + b.year + " already exists for this consumer.");
            }
        }
        bills.add(b);
        saveData();
        refreshSwingUi();
        log("Generated Bill " + b.invoiceNo + " for " + b.customerId + " (Total: Rs. " + String.format("%.2f", b.total)
                + ")");
        return billToJson(b);
    }

    private String apiDeleteBill(String invoiceNo) throws ApiError {
        Bill target = null;
        for (Bill b : bills) {
            if (b.invoiceNo.equalsIgnoreCase(invoiceNo)) {
                target = b;
                break;
            }
        }
        if (target == null)
            throw new ApiError("Invoice " + invoiceNo + " not found.");
        bills.remove(target);
        saveData();
        refreshSwingUi();
        log("Deleted invoice: " + invoiceNo);
        return "{\"ok\":true}";
    }

    // ========================================================================
    // 5. JSON SERIALIZATION UTILITIES
    // ========================================================================

    private static String q(String s) {
        if (s == null)
            return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "") + "\"";
    }

    private String customerToJson(Customer c) {
        return "{\"id\":" + q(c.id) + ",\"name\":" + q(c.name) + ",\"address\":" + q(c.address)
                + ",\"phone\":" + q(c.phone) + ",\"category\":" + q(c.category) + "}";
    }

    private String billToJson(Bill b) {
        return String.format(Locale.ROOT,
                "{\"invoiceNo\":%s,\"customerId\":%s,\"month\":%s,\"year\":%d,\"prev\":%d,\"curr\":%d,\"units\":%d,"
                        + "\"energy\":%.2f,\"fixed\":%.2f,\"tax\":%.2f,\"total\":%.2f,\"date\":%s}",
                q(b.invoiceNo), q(b.customerId), q(b.month), b.year, b.prev, b.curr, b.units,
                b.energy, b.fixed, b.tax, b.total, q(b.date));
    }

    private String customersToJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < customers.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(customerToJson(customers.get(i)));
        }
        return sb.append("]").toString();
    }

    private String billsToJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bills.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(billToJson(bills.get(i)));
        }
        return sb.append("]").toString();
    }

    private static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.trim();
        if (json.startsWith("{"))
            json = json.substring(1);
        if (json.endsWith("}"))
            json = json.substring(0, json.length() - 1);

        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            if (kv.length == 2) {
                String k = kv[0].trim().replaceAll("^\"|\"$", "");
                String v = kv[1].trim().replaceAll("^\"|\"$", "");
                map.put(k, v);
            }
        }
        return map;
    }

    // ========================================================================
    // 6. DESKTOP SWING GUI (OOP Concept: Inheritance from JFrame)
    // ========================================================================

    private DefaultTableModel custModel;
    private DefaultTableModel billModel;

    public oop() {
        super("SmartGrid - Electricity Billing System");
        loadData();

        // Top Navigation Bar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(15, 45, 82));
        topPanel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel("SmartGrid Electricity Billing Core");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JButton btnOpenWeb = new JButton("Open Web Portal");
        btnOpenWeb.setBackground(new Color(29, 78, 216));
        btnOpenWeb.setForeground(Color.WHITE);
        btnOpenWeb.setFocusPainted(false);
        btnOpenWeb.addActionListener(e -> {
            try { Desktop.getDesktop().browse(new java.net.URI(siteUrl)); } catch (Exception ignored) {}
        });

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(btnOpenWeb, BorderLayout.EAST);

        // Center Tabs for Quick Desktop Inspection
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));

        custModel = new DefaultTableModel(new String[]{"ID", "Name", "Category", "Phone", "Address"}, 0);
        JTable custTable = new JTable(custModel);
        tabs.addTab("Customers (" + customers.size() + ")", new JScrollPane(custTable));

        billModel = new DefaultTableModel(new String[]{"Invoice", "Customer ID", "Period", "Units (kWh)", "Total (Rs.)", "Date"}, 0);
        JTable billTable = new JTable(billModel);
        tabs.addTab("Invoices (" + bills.size() + ")", new JScrollPane(billTable));

        // Bottom Console Activity Logger
        taLog = new JTextArea(5, 50);
        taLog.setEditable(false);
        taLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logPane = new JScrollPane(taLog);
        logPane.setBorder(BorderFactory.createTitledBorder("Live System Activity Log"));

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(logPane, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 600);
        setLocationRelativeTo(null);

        refreshSwingUi();
        startWebServer();
    }

    private void refreshSwingUi() {
        SwingUtilities.invokeLater(() -> {
            if (custModel != null) {
                custModel.setRowCount(0);
                for (Customer c : customers) {
                    custModel.addRow(new Object[] { c.id, c.name, c.category, c.phone, c.address });
                }
            }
            if (billModel != null) {
                billModel.setRowCount(0);
                for (Bill b : bills) {
                    billModel.addRow(new Object[]{b.invoiceNo, b.customerId, b.month + " " + b.year, b.units, String.format("%.2f", b.total), b.date});
                }
            }
        });
    }

    // ========================================================================
    // 7. MAIN METHOD (Entry Point)
    // ========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new oop().setVisible(true);
        });
    }
}