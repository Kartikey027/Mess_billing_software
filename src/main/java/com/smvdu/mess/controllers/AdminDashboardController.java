package com.smvdu.mess.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import com.smvdu.mess.App;
import com.smvdu.mess.database.DatabaseConnection;
import com.smvdu.mess.utils.AdminSessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class AdminDashboardController {
    
    @FXML private Label adminNameLabel;
    @FXML private Label designationLabel;
    @FXML private FlowPane hostelsContainer;
    
    private String adminName;
    private String designation;
    
    @FXML
    public void initialize() {
        System.out.println("AdminDashboardController initialized");
    }
    
    public void setAdminInfo(String name, String designation) {
        this.adminName = name;
        this.designation = designation;
        
        System.out.println("Setting admin info: " + name + " - " + designation);
        
        adminNameLabel.setText(name);
        designationLabel.setText(designation);
        
        loadHostels();
    }
    
private void loadHostels() {
    System.out.println("Loading messes...");
    hostelsContainer.getChildren().clear();
    
    try {
        Connection conn = DatabaseConnection.getConnection();
        
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();
        int operatingDays = now.lengthOfMonth();

        
        
        // Get rates from settings
        PreparedStatement pstmt = conn.prepareStatement("SELECT value FROM settings WHERE key = 'per_day_rate'");
        ResultSet rs = pstmt.executeQuery();
        double perDayRate = rs.next() ? Double.parseDouble(rs.getString("value")) : 120.0;
        
        pstmt = conn.prepareStatement("SELECT value FROM settings WHERE key = 'gst_percent'");
        rs = pstmt.executeQuery();
        double gstPercent = rs.next() ? Double.parseDouble(rs.getString("value")) : 5.0;
        
        // Get all messes
        String messQuery = "SELECT id, name, code FROM messes ORDER BY name";
        rs = conn.createStatement().executeQuery(messQuery);
        
        int messCount = 0;
        while (rs.next()) {
            int messId = rs.getInt("id");
            String messName = rs.getString("name");
            String messCode = rs.getString("code");
            
            // Get all hostel IDs for this mess
            pstmt = conn.prepareStatement("SELECT id FROM hostels WHERE mess_id = ?");
            pstmt.setInt(1, messId);
            ResultSet hostelRs = pstmt.executeQuery();
            
            StringBuilder hostelIds = new StringBuilder();
            while (hostelRs.next()) {
                if (hostelIds.length() > 0) hostelIds.append(",");
                hostelIds.append(hostelRs.getInt("id"));
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

            
            // Skip if no hostels assigned to this mess
            if (hostelIds.length() == 0) continue;
            
            // Count active students
            String studentQuery = "SELECT COUNT(*) FROM students WHERE hostel_id IN (" + hostelIds + ") AND is_active = 1";
            ResultSet studentRs = conn.createStatement().executeQuery(studentQuery);
            int activeStudents = studentRs.next() ? studentRs.getInt(1) : 0;
            
            // Calculate total absent days
            String absentQuery = "SELECT COALESCE(SUM(sa.absent_days), 0) as total_absent_days " +
                               "FROM students s " +
                               "LEFT JOIN student_attendance sa ON s.id = sa.student_id " +
                               "AND sa.month = ? AND sa.year = ? " +
                               "WHERE s.hostel_id IN (" + hostelIds + ") AND s.is_active = 1";
            
            pstmt = conn.prepareStatement(absentQuery);
            pstmt.setInt(1, currentMonth);
            pstmt.setInt(2, currentYear);
            ResultSet absentRs = pstmt.executeQuery();
            
            int totalAbsentDays = absentRs.next() ? absentRs.getInt("total_absent_days") : 0;
            
            // Calculate bill
            int totalPossibleDays = activeStudents * operatingDays;
            int netMessDays = totalPossibleDays - totalAbsentDays;
            if (netMessDays < 0) netMessDays = 0;
            
            double subtotal = netMessDays * perDayRate;
            double gst = subtotal * (gstPercent / 100);
            double totalBill = subtotal + gst;
            
            // Create card with calculated bill
            messCount++;
            VBox messCard = createMessCard(messId, messName, messCode, activeStudents, totalBill);
            hostelsContainer.getChildren().add(messCard);
        }
        
        System.out.println("Loaded " + messCount + " messes");
        
        if (messCount == 0) {
            Label noDataLabel = new Label("No messes found in database");
            noDataLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #999;");
            hostelsContainer.getChildren().add(noDataLabel);
        }
        
    } catch (SQLException e) {
        e.printStackTrace();
        System.err.println("SQL Error: " + e.getMessage());
        showAlert("Error", "Failed to load messes: " + e.getMessage(), Alert.AlertType.ERROR);
    }
}


    
    private VBox createMessCard(int messId, String messName, String code, 
                                int students, double billAmount) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(280);
        card.setMinHeight(220);
        card.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 12;
            -fx-padding: 25;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);
            -fx-cursor: hand;
        """);
        
        Label icon = new Label("🍽️");
        icon.setStyle("-fx-font-size: 48px;");
        
        Label nameLabel = new Label(messName);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        nameLabel.setStyle("-fx-text-fill: #1e3a5f;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(230);
        
        Label codeLabel = new Label("Code: " + code);
        codeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        
        Label studentsLabel = new Label("Students: " + students);
        studentsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");
        
        Separator sep = new Separator();
        sep.setPrefWidth(200);
        
        Label billLabel = new Label(String.format("₹%.2f", billAmount));
        billLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        billLabel.setStyle("-fx-text-fill: #2e7d32;");
        
        Label billTextLabel = new Label("Current Month Bill");
        billTextLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");
        
        card.getChildren().addAll(icon, nameLabel, codeLabel, studentsLabel, 
                                  sep, billLabel, billTextLabel);
        
        card.setOnMouseEntered(e -> {
            card.setStyle("""
                -fx-background-color: #f0f7ff;
                -fx-background-radius: 12;
                -fx-padding: 25;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 15, 0, 0, 5);
                -fx-cursor: hand;
            """);
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 12;
                -fx-padding: 25;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);
                -fx-cursor: hand;
            """);
        });
        
        card.setOnMouseClicked(e -> openMessDetails(messId, messName));
        
        return card;
    }
    
    private void openMessDetails(int messId, String messName) {
        try {
            System.out.println("Opening mess details for: " + messName);
            
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/admin_hostel_view.fxml")
            );
            Parent root = loader.load();
            
            AdminHostelViewController controller = loader.getController();
            controller.setMessInfo(messId, messName);
            
            App.getPrimaryStage().getScene().setRoot(root);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open mess details: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleLogout() {
        try {
            AdminSessionManager.clear();
            App.setRoot("login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
