package com.smvdu.mess.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

import com.smvdu.mess.App;
import com.smvdu.mess.database.DatabaseConnection;
import com.smvdu.mess.models.User;
import com.smvdu.mess.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DashboardController {
    
    @FXML private Label welcomeLabel;
    @FXML private Label hostelLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label activeStudentsLabel;
    @FXML private Label currentMonthLabel;
    @FXML private Label totalMessDaysLabel;
    @FXML private Label estimatedBillLabel;
    @FXML private VBox statsContainer;
    
    private User currentUser;
    
    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        
        if (currentUser == null) {
            try {
                App.setRoot("login");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        
        welcomeLabel.setText("Welcome, " + currentUser.getName());
        
        // Display mess name instead of hostel name
        if (currentUser.getMessName() != null) {
            hostelLabel.setText(currentUser.getMessName());
        } else {
            hostelLabel.setText(currentUser.getHostelName());
        }
        
        loadDashboardStats();
    }
    
   private void loadDashboardStats() {
    try {
        Connection conn = DatabaseConnection.getConnection();
        int hostelId = currentUser.getHostelId();
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();
        int operatingDays = now.lengthOfMonth(); // fallback



        
        // Get mess_id for this hostel
        PreparedStatement pstmt = conn.prepareStatement("SELECT mess_id FROM hostels WHERE id = ?");
        pstmt.setInt(1, hostelId);
        ResultSet rs = pstmt.executeQuery();
        int messId = rs.next() ? rs.getInt("mess_id") : hostelId;
        
        // Get all hostel IDs that share this mess
        pstmt = conn.prepareStatement("SELECT id FROM hostels WHERE mess_id = ?");
        pstmt.setInt(1, messId);
        rs = pstmt.executeQuery();
        
        StringBuilder hostelIds = new StringBuilder();
        while (rs.next()) {
            if (hostelIds.length() > 0) hostelIds.append(",");
            hostelIds.append(rs.getInt("id"));
        }

        PreparedStatement opStmt = conn.prepareStatement("""
    SELECT operating_days FROM mess_operation_days
    WHERE mess_id = ? AND month = ? AND year = ?
""");

opStmt.setInt(1, messId);
opStmt.setInt(2, currentMonth);
opStmt.setInt(3, currentYear);

ResultSet opRs = opStmt.executeQuery();
if (opRs.next()) {
    operatingDays = opRs.getInt("operating_days");
}

  // Days in month
totalMessDaysLabel.setText(String.valueOf(operatingDays));
        
        // Total students (from ALL hostels in this mess)
        String query = "SELECT COUNT(*) FROM students WHERE hostel_id IN (" + hostelIds + ")";
        rs = conn.createStatement().executeQuery(query);
        int totalStudents = rs.next() ? rs.getInt(1) : 0;
        totalStudentsLabel.setText(String.valueOf(totalStudents));
        
        // Active students
        query = "SELECT COUNT(*) FROM students WHERE hostel_id IN (" + hostelIds + ") AND is_active = 1";
        rs = conn.createStatement().executeQuery(query);
        int activeStudents = rs.next() ? rs.getInt(1) : 0;
        activeStudentsLabel.setText(String.valueOf(activeStudents));
        
        // Current month
        String monthName = Month.of(currentMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        currentMonthLabel.setText(monthName + " " + currentYear);
        
      
        
        
        // Calculate total absent days for current month (across all hostels in mess)
        String absentQuery = "SELECT COALESCE(SUM(sa.absent_days), 0) as total_absent_days " +
                           "FROM students s " +
                           "LEFT JOIN student_attendance sa ON s.id = sa.student_id " +
                           "AND sa.month = ? AND sa.year = ? " +
                           "WHERE s.hostel_id IN (" + hostelIds + ") AND s.is_active = 1";
        
        pstmt = conn.prepareStatement(absentQuery);
        pstmt.setInt(1, currentMonth);
        pstmt.setInt(2, currentYear);
        rs = pstmt.executeQuery();
        
        int totalAbsentDays = 0;
        if (rs.next()) {
            totalAbsentDays = rs.getInt("total_absent_days");
        }
        
        // Calculate net mess days for billing
        int totalPossibleDays = activeStudents * operatingDays;

        int netMessDays = totalPossibleDays - totalAbsentDays;
        if (netMessDays < 0) netMessDays = 0;
        
        // Get dynamic per day rate from settings
        pstmt = conn.prepareStatement("SELECT value FROM settings WHERE key = 'per_day_rate'");
        rs = pstmt.executeQuery();
        double perDayRate = 120.0;
        if (rs.next()) {
            perDayRate = Double.parseDouble(rs.getString("value"));
        }
        
        // Get GST percentage from settings
        pstmt = conn.prepareStatement("SELECT value FROM settings WHERE key = 'gst_percent'");
        rs = pstmt.executeQuery();
        double gstPercent = 5.0;
        if (rs.next()) {
            gstPercent = Double.parseDouble(rs.getString("value"));
        }
        
        // Calculate estimated bill using NET mess days
        double subtotal = netMessDays * perDayRate;
        double gst = subtotal * (gstPercent / 100);
        double total = subtotal + gst;
        estimatedBillLabel.setText(String.format("₹%.2f", total));
        
    } catch (SQLException e) {
        e.printStackTrace();
        showAlert("Error", "Failed to load dashboard statistics", Alert.AlertType.ERROR);
    }
}

    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    private void openBilling() {
        try {
            App.setRoot("billing");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void openStudents() {
        try {
            App.setRoot("students");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void openImport() {
        try {
            App.setRoot("import");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleLogout() {
        SessionManager.logout();
        try {
            App.setRoot("login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
