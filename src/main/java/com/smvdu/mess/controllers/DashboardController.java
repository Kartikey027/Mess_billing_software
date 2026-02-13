package com.smvdu.mess.controllers;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import com.smvdu.mess.App;
import com.smvdu.mess.models.User;
import com.smvdu.mess.utils.MessUtils;
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
    private int messId;
    
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
        
        // Get mess ID using utility
        messId = MessUtils.getMessIdForHostel(currentUser.getHostelId());
        
        loadDashboardStats();
    }
   private void loadDashboardStats() {
    try {
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();
        
        // Get operating days using centralized utility
        int operatingDays = MessUtils.getOperatingDays(messId, currentMonth, currentYear);
        
        // Get all hostel IDs for this mess
        List<Integer> hostelIds = MessUtils.getHostelIdsForMess(messId);
        
        if (hostelIds.isEmpty()) {
            showAlert("Error", "No hostels found for this mess", Alert.AlertType.ERROR);
            return;
        }
        
        // Get student counts
        int totalStudents = MessUtils.getTotalStudentCount(hostelIds);
        int activeStudents = MessUtils.getActiveStudentCount(hostelIds);
        
        // Get total absent days
        int totalAbsentDays = MessUtils.getTotalAbsentDays(hostelIds, currentMonth, currentYear);
        
        // Calculate billing
        int totalPossibleDays = activeStudents * operatingDays;
        int netMessDays = totalPossibleDays - totalAbsentDays;
        if (netMessDays < 0) netMessDays = 0;
        
        // Get rates from settings
        double perDayRate = MessUtils.getSetting("per_day_rate", 120.0);
        double gstPercent = MessUtils.getSetting("gst_percent", 5.0);
        
        // ✅ Get fine amount
        double fineAmount = MessUtils.getFineAmount(messId, currentMonth, currentYear);
        
        // Calculate bill
        double subtotal = netMessDays * perDayRate;
        double gst = subtotal * (gstPercent / 100);
        double total = subtotal + gst + fineAmount;  // ✅ Include fine
        
        // Update UI
        totalStudentsLabel.setText(String.valueOf(totalStudents));
        activeStudentsLabel.setText(String.valueOf(activeStudents));
        
        String monthName = Month.of(currentMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        currentMonthLabel.setText(monthName + " " + currentYear);
        
        totalMessDaysLabel.setText(String.valueOf(operatingDays));
        estimatedBillLabel.setText(String.format("₹%.2f", total));
        
    } catch (Exception e) {
        e.printStackTrace();
        showAlert("Error", "Failed to load dashboard statistics: " + e.getMessage(), Alert.AlertType.ERROR);
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