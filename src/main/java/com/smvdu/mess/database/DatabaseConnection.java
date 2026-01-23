package com.smvdu.mess.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    // ===== USER HOME BASED DB PATH (EXE SAFE) =====
    private static final String DB_FOLDER =
            System.getProperty("user.home")
                    + File.separator + "SMVDU-Mess"
                    + File.separator + "db";

    private static final String DB_PATH =
            DB_FOLDER + File.separator + "mess_billing.db";

    private static final String DB_URL =
            "jdbc:sqlite:" + DB_PATH;

    private static Connection connection;

    // ===== INITIALIZATION =====
    public static void initialize() {
        try {
            // Create folder if not exists
            File folder = new File(DB_FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");

            connection = DriverManager.getConnection(DB_URL);
            createTables();
            insertDefaultData();
            migrateDatabase();

            System.out.println("Database initialized successfully!");
            System.out.println("DB Path: " + DB_PATH);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== CONNECTION PROVIDER =====
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }




private static void migrateDatabase() {
    try (Statement stmt = connection.createStatement()) {

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS messes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                code TEXT UNIQUE NOT NULL
            )
        """);

        stmt.execute("ALTER TABLE hostels ADD COLUMN mess_id INTEGER");

        stmt.execute("""
            INSERT OR IGNORE INTO messes (id, name, code) VALUES
            (1, 'Central Mess', 'CM'),
            (2, 'Vindhyachal Hostel Mess', 'VHM'),
            (3, 'Basohli Hostel Mess', 'BHM'),
            (4, 'Nilgiri Hostel Mess', 'NHM'),
            (5, 'Shivalik Hostel Mess', 'SHM'),
            (6, 'Vaishnavi Hostel Mess', 'VNHM')
        """);

        stmt.execute("UPDATE hostels SET mess_id = 1 WHERE id IN (1,2)");
        stmt.execute("UPDATE hostels SET mess_id = 2 WHERE id = 3");
        stmt.execute("UPDATE hostels SET mess_id = 3 WHERE id = 4");
        stmt.execute("UPDATE hostels SET mess_id = 4 WHERE id = 5");
        stmt.execute("UPDATE hostels SET mess_id = 5 WHERE id = 6");
        stmt.execute("UPDATE hostels SET mess_id = 6 WHERE id = 7");

        System.out.println("✓ Database migrated");

    } catch (Exception ignored) {}
}



    // ===== TABLE CREATION =====
    
    private static void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                name TEXT NOT NULL,
                hostel_id INTEGER NOT NULL,
                role TEXT DEFAULT 'caretaker',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);





        stmt.execute("""
            CREATE TABLE IF NOT EXISTS hostels (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                code TEXT UNIQUE NOT NULL,
                mess_name TEXT NOT NULL
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS students (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                entry_number TEXT UNIQUE NOT NULL,
                name TEXT NOT NULL,
                hostel_id INTEGER NOT NULL,
                room_number TEXT,
                phone TEXT,
                email TEXT,
                is_active INTEGER DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (hostel_id) REFERENCES hostels(id)
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS student_attendance (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER NOT NULL,
                month INTEGER NOT NULL,
                year INTEGER NOT NULL,
                total_days INTEGER NOT NULL,
                mess_days INTEGER NOT NULL,
                absent_days INTEGER DEFAULT 0,
                remarks TEXT,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (student_id) REFERENCES students(id),
                UNIQUE(student_id, month, year)
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS bills (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                hostel_id INTEGER NOT NULL,
                month INTEGER NOT NULL,
                year INTEGER NOT NULL,
                total_students INTEGER NOT NULL,
                total_mess_days INTEGER NOT NULL,
                per_day_rate REAL NOT NULL,
                subtotal REAL NOT NULL,
                gst_percent REAL DEFAULT 5.0,
                gst_amount REAL NOT NULL,
                total_amount REAL NOT NULL,
                generated_by INTEGER,
                generated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (hostel_id) REFERENCES hostels(id),
                FOREIGN KEY (generated_by) REFERENCES users(id)
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS settings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                key TEXT UNIQUE NOT NULL,
                value TEXT NOT NULL
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS admins (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                name TEXT NOT NULL,
                designation TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);
    }

    // ===== DEFAULT DATA =====
    private static void insertDefaultData() throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM hostels");

        if (rs.next() && rs.getInt(1) > 0) {
            return;
        }

        String[][] hostels = {
            {"Kailash Hostel", "KH", "Central Mess"},
            {"Trikuta Hostel", "TH", "Central Mess"},
            {"Vindhyachal Hostel", "VH", "Vindhyachal Hostel Mess"},
            {"Basohli Hostel", "BH", "Basohli Hostel Mess"},
            {"Nilgiri Hostel", "NH", "Nilgiri Hostel Mess"},
            {"Shivalik Hostel", "SH", "Shivalik Hostel Mess"},
            {"Vaishnavi Hostel", "VNH", "Vaishnavi Hostel Mess"}
        };

        PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO hostels (name, code, mess_name) VALUES (?, ?, ?)"
        );

        for (String[] h : hostels) {
            pstmt.setString(1, h[0]);
            pstmt.setString(2, h[1]);
            pstmt.setString(3, h[2]);
            pstmt.executeUpdate();
        }

        String[][] caretakers = {
            {"caretaker.kailashhostel@smvdu.ac.in", "admin123", "Kailash Caretaker", "1"},
            {"caretaker.trikutahostel@smvdu.ac.in", "admin123", "Trikuta Caretaker", "2"},
            {"caretaker.vindhyachalhostel@smvdu.ac.in", "admin123", "Vindhyachal Caretaker", "3"},
            {"caretaker.basohlihostel@smvdu.ac.in", "admin123", "Basohli Caretaker", "4"},
            {"caretaker.nilgirihostel@smvdu.ac.in", "admin123", "Nilgiri Caretaker", "5"},
            {"caretaker.shivalikhostela@smvdu.ac.in", "admin123", "Shivalik Block A Caretaker", "6"},
            {"caretaker.vaishnavihostel@smvdu.ac.in", "admin123", "Vaishnavi Caretaker", "7"}
        };

        pstmt = connection.prepareStatement(
                "INSERT INTO users (email, password, name, hostel_id, role) VALUES (?, ?, ?, ?, 'caretaker')"
        );

        for (String[] c : caretakers) {
            pstmt.setString(1, c[0]);
            pstmt.setString(2, c[1]);
            pstmt.setString(3, c[2]);
            pstmt.setInt(4, Integer.parseInt(c[3]));
            pstmt.executeUpdate();
        }

        String[][] admins = {
            {"vc.pk@smvdu.ac.in", "admin123", "Vice Chancellor", "VC"},
            {"dean.studens@smvdu.ac.in", "admin123", "Dean Student Welfare", "Dean"},
            {"registrar@smvdu.ac.in", "admin123", "Registrar", "Registrar"}
        };

        pstmt = connection.prepareStatement(
                "INSERT INTO admins (email, password, name, designation) VALUES (?, ?, ?, ?)"
        );

        for (String[] a : admins) {
            pstmt.setString(1, a[0]);
            pstmt.setString(2, a[1]);
            pstmt.setString(3, a[2]);
            pstmt.setString(4, a[3]);
            pstmt.executeUpdate();
        }

        stmt.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('per_day_rate', '120')");
        stmt.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('gst_percent', '5')");

        System.out.println("Default data inserted successfully!");
    }
}
