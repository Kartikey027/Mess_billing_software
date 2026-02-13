package com.smvdu.mess.controllers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import com.smvdu.mess.App;
import com.smvdu.mess.database.DatabaseConnection;
import com.smvdu.mess.utils.AdminSessionManager;
import com.smvdu.mess.utils.MessUtils;

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
            
            // Get rates from settings using utility
            double perDayRate = MessUtils.getSetting("per_day_rate", 120.0);
            double gstPercent = MessUtils.getSetting("gst_percent", 5.0);
            
            // Get all messes
            String messQuery = "SELECT id, name, code FROM messes ORDER BY name";
            ResultSet rs = conn.createStatement().executeQuery(messQuery);
            
            int messCount = 0;
            while (rs.next()) {
                int messId = rs.getInt("id");
                String messName = rs.getString("name");
                String messCode = rs.getString("code");
                
                // Get all hostel IDs for this mess using utility
                List<Integer> hostelIds = MessUtils.getHostelIdsForMess(messId);
                
                // Skip if no hostels assigned to this mess
                if (hostelIds.isEmpty()) continue;
                
                // Get operating days using centralized utility
                int operatingDays = MessUtils.getOperatingDays(messId, currentMonth, currentYear);
                
                // Count active students using utility
                int activeStudents = MessUtils.getActiveStudentCount(hostelIds);
                
                // Calculate total absent days using utility
                int totalAbsentDays = MessUtils.getTotalAbsentDays(hostelIds, currentMonth, currentYear);
                
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