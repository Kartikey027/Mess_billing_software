package com.smvdu.mess.controllers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

import com.smvdu.mess.App;
import com.smvdu.mess.database.DatabaseConnection;
import com.smvdu.mess.utils.BillPDFGenerator;
import com.smvdu.mess.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class BillingController {
    
    @FXML private ComboBox<String> monthCombo;
    @FXML private ComboBox<Integer> yearCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label hostelNameLabel;
    @FXML private Label hostelCodeLabel;
    @FXML private Label billPeriodLabel;
    @FXML private Label preparedByLabel;
    @FXML private Label generatedDateLabel;
    @FXML private Label daysInMonthLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalStudentDaysLabel;
    @FXML private Label totalAbsentDaysLabel;
    @FXML private Label totalMessDaysLabel;
    @FXML private Label perDayRateLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label gstPercentLabel;
    @FXML private Label gstAmountLabel;
    @FXML private Label totalAmountLabel;
    @FXML private VBox billPreview;
    @FXML private TextField rateField;
    @FXML private TextField gstField;
    @FXML private Button updateRateButton;
    @FXML private TextField fineField;
    @FXML private Label fineAmountLabel;
    
    private int hostelId;
    private String hostelCode = "";
    private double perDayRate = 120.0;
    private double gstPercent = 5.0;
    private double fineAmount = 0;
    
    @FXML
    public void initialize() {
        hostelId = SessionManager.getCurrentHostelId();
        
        String hostelName = SessionManager.getCurrentUser().getHostelName();
        
        // Get hostel code and MESS NAME
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT code, mess_name FROM hostels WHERE id = ?"
            );
            pstmt.setInt(1, hostelId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                hostelCode = rs.getString("code");
                hostelCodeLabel.setText(hostelCode);
                
                // Display MESS NAME instead of hostel name in bills
                String messName = rs.getString("mess_name");
                hostelNameLabel.setText(messName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        preparedByLabel.setText(SessionManager.getCurrentUser().getName());
        
        // Populate months
        for (Month month : Month.values()) {
            monthCombo.getItems().add(month.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        }
        
        // Populate years
        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear - 2; y <= currentYear + 1; y++) {
            yearCombo.getItems().add(y);
        }
        
        // Set current month/year
        LocalDate now = LocalDate.now();
        monthCombo.setValue(now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        yearCombo.setValue(currentYear);
        
        startDatePicker.setValue(now.withDayOfMonth(1));
        endDatePicker.setValue(now.withDayOfMonth(now.lengthOfMonth()));
        
        loadSettings();
        rateField.setText(String.valueOf(perDayRate));
        gstField.setText(String.valueOf(gstPercent));
        
        monthCombo.setOnAction(e -> onMonthYearChange());
        yearCombo.setOnAction(e -> onMonthYearChange());
        startDatePicker.setOnAction(e -> generateBill());
        endDatePicker.setOnAction(e -> generateBill());
        
        generateBill();
    }
    
    private void onMonthYearChange() {
        int selectedMonth = monthCombo.getSelectionModel().getSelectedIndex() + 1;
        int selectedYear = yearCombo.getValue();
        
        LocalDate firstDay = LocalDate.of(selectedYear, selectedMonth, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        
        startDatePicker.setValue(firstDay);
        endDatePicker.setValue(lastDay);
        
        generateBill();
    }
    
    private void loadSettings() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement stmt = conn.createStatement();
            
            ResultSet rs = stmt.executeQuery("SELECT value FROM settings WHERE key = 'per_day_rate'");
            if (rs.next()) perDayRate = Double.parseDouble(rs.getString("value"));
            
            rs = stmt.executeQuery("SELECT value FROM settings WHERE key = 'gst_percent'");
            if (rs.next()) gstPercent = Double.parseDouble(rs.getString("value"));
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void updateRateAndGST() {
        try {
            double newRate = Double.parseDouble(rateField.getText());
            double newGST = Double.parseDouble(gstField.getText());
            
            if (newRate <= 0 || newGST < 0) {
                showAlert("Error", "Please enter valid positive numbers", Alert.AlertType.ERROR);
                return;
            }
            
            Connection conn = DatabaseConnection.getConnection();
            
            PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE settings SET value = ? WHERE key = 'per_day_rate'"
            );
            pstmt.setString(1, String.valueOf(newRate));
            pstmt.executeUpdate();
            
            pstmt = conn.prepareStatement(
                "UPDATE settings SET value = ? WHERE key = 'gst_percent'"
            );
            pstmt.setString(1, String.valueOf(newGST));
            pstmt.executeUpdate();
            
            perDayRate = newRate;
            gstPercent = newGST;
            
            showAlert("Success", "Rate and GST updated successfully!", Alert.AlertType.INFORMATION);
            
            generateBill();
            
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter valid numbers for rate and GST", Alert.AlertType.ERROR);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to update settings", Alert.AlertType.ERROR);
        }
    }
    
   @FXML
private void generateBill() {
    LocalDate startDate = startDatePicker.getValue();
    LocalDate endDate = endDatePicker.getValue();
    
    if (startDate == null || endDate == null) {
        return;
    }
    
    if (startDate.isAfter(endDate)) {
        showAlert("Error", "Start date must be before end date", Alert.AlertType.ERROR);
        return;
    }
    
    try {
        perDayRate = Double.parseDouble(rateField.getText());
        gstPercent = Double.parseDouble(gstField.getText());
    } catch (NumberFormatException e) {
        return;
    }
    
    int daysInRange = (int) (endDate.toEpochDay() - startDate.toEpochDay() + 1);

    
    
    int selectedMonth = monthCombo.getSelectionModel().getSelectedIndex() + 1;
    int selectedYear = yearCombo.getValue();
    
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
    billPeriodLabel.setText(startDate.format(formatter) + " to " + endDate.format(formatter));
    
    generatedDateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
    
    try {
        Connection conn = DatabaseConnection.getConnection();
        
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
        
        // Get active students count from ALL hostels in this mess
        String query = "SELECT COUNT(*) FROM students WHERE hostel_id IN (" + hostelIds + ") AND is_active = 1";
        rs = conn.createStatement().executeQuery(query);
        int activeStudents = rs.next() ? rs.getInt(1) : 0;
        
        // Calculate total absent days from ALL hostels in this mess
        String absentQuery = "SELECT COALESCE(SUM(sa.absent_days), 0) as total_absent_days " +
                           "FROM students s " +
                           "LEFT JOIN student_attendance sa ON s.id = sa.student_id " +
                           "AND sa.month = ? AND sa.year = ? " +
                           "WHERE s.hostel_id IN (" + hostelIds + ") AND s.is_active = 1";
        
        pstmt = conn.prepareStatement(absentQuery);
        pstmt.setInt(1, selectedMonth);
        pstmt.setInt(2, selectedYear);
        rs = pstmt.executeQuery();

        // SAVE OPERATING DAYS GLOBALLY FOR THIS MESS
PreparedStatement saveDays = conn.prepareStatement("""
    INSERT OR REPLACE INTO mess_operation_days
    (mess_id, month, year, operating_days)
    VALUES (?, ?, ?, ?)
""");

saveDays.setInt(1, messId);
saveDays.setInt(2, selectedMonth);
saveDays.setInt(3, selectedYear);
saveDays.setInt(4, daysInRange);

saveDays.executeUpdate();

        
        int totalAbsentDays = 0;
        if (rs.next()) {
            totalAbsentDays = rs.getInt("total_absent_days");
        }
        
        int totalStudentDays = activeStudents * daysInRange;
        int totalMessDays = totalStudentDays - totalAbsentDays;
        if (totalMessDays < 0) totalMessDays = 0;
        
        double subtotal = totalMessDays * perDayRate;
        double gstAmount = subtotal * (gstPercent / 100);

        try {
            fineAmount = fineField.getText().isEmpty() ? 0 : Double.parseDouble(fineField.getText());
        } catch (Exception e) {
            fineAmount = 0;
        }

        double total = subtotal + gstAmount + fineAmount;

        daysInMonthLabel.setText(String.valueOf(daysInRange));
        totalStudentsLabel.setText(String.valueOf(activeStudents));
        totalStudentDaysLabel.setText(String.valueOf(totalStudentDays));
        totalAbsentDaysLabel.setText(String.valueOf(totalAbsentDays));
        totalMessDaysLabel.setText(String.valueOf(totalMessDays));
        perDayRateLabel.setText(String.format("₹%.2f", perDayRate));
        subtotalLabel.setText(String.format("₹%.2f", subtotal));
        gstPercentLabel.setText(String.format("%.1f%%", gstPercent));
        gstAmountLabel.setText(String.format("₹%.2f", gstAmount));
        totalAmountLabel.setText(String.format("₹%.2f", total));
        
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    
    @FXML
    private void exportToPDF() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Bill as PDF");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );
            
            String monthYear = monthCombo.getValue() + "_" + yearCombo.getValue();
            String hostelName = hostelNameLabel.getText().replace(" ", "_");
            fileChooser.setInitialFileName("Bill_" + hostelName + "_" + monthYear + ".pdf");
            
            Stage stage = (Stage) billPreview.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                int daysInRange = Integer.parseInt(daysInMonthLabel.getText());
                int totalStudents = Integer.parseInt(totalStudentsLabel.getText());
                int totalStudentDays = Integer.parseInt(totalStudentDaysLabel.getText());
                int totalAbsentDays = Integer.parseInt(totalAbsentDaysLabel.getText());
                int totalMessDays = Integer.parseInt(totalMessDaysLabel.getText());
                double subtotal = Double.parseDouble(subtotalLabel.getText().replace("₹", "").replace(",", ""));
                double gstAmount = Double.parseDouble(gstAmountLabel.getText().replace("₹", "").replace(",", ""));
                double totalAmount = Double.parseDouble(totalAmountLabel.getText().replace("₹", "").replace(",", ""));
                double fineAmount = Double.parseDouble(fineAmountLabel.getText().replace("₹", "").replace(",", ""));
                
                BillPDFGenerator.generateBillPDF(
                    file.getAbsolutePath(),
                    "SHRI MATA VAISHNO DEVI UNIVERSITY",
                    hostelNameLabel.getText(),
                    hostelCodeLabel.getText(),
                    billPeriodLabel.getText(),
                    daysInRange,
                    totalStudents,
                    totalStudentDays,
                    totalAbsentDays,
                    totalMessDays,
                    perDayRate,
                    subtotal,
                    gstPercent,
                    gstAmount,
                    totalAmount,
                    preparedByLabel.getText(),
                    LocalDate.now()
                );
                
                showAlert("Success", "Bill exported to PDF successfully!\nFile: " + file.getName(), Alert.AlertType.INFORMATION);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to export PDF: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void printBill() {
        PrinterJob printerJob = PrinterJob.createPrinterJob();
        
        if (printerJob != null && printerJob.showPrintDialog(billPreview.getScene().getWindow())) {
            PageLayout pageLayout = printerJob.getJobSettings().getPageLayout();
            
            double scaleX = pageLayout.getPrintableWidth() / billPreview.getBoundsInParent().getWidth();
            double scaleY = pageLayout.getPrintableHeight() / billPreview.getBoundsInParent().getHeight();
            double scale = Math.min(scaleX, scaleY);
            
            Scale scaleTransform = new Scale(scale, scale);
            billPreview.getTransforms().add(scaleTransform);
            
            boolean success = printerJob.printPage(billPreview);
            
            billPreview.getTransforms().remove(scaleTransform);
            
            if (success) {
                printerJob.endJob();
                showAlert("Success", "Bill sent to printer!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Printing failed", Alert.AlertType.ERROR);
            }
        }
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    private void goBack() {
        try {
            App.setRoot("dashboard");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
