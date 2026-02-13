package com.smvdu.mess.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.smvdu.mess.App;
import com.smvdu.mess.database.DatabaseConnection;
import com.smvdu.mess.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

public class ImportController {
    
    @FXML private Label fileLabel;
    @FXML private TextArea logArea;
    @FXML private ProgressBar progressBar;
    @FXML private Label hostelLabel;
    
    private File selectedFile;
    private int hostelId;
    
    @FXML
    public void initialize() {
        hostelId = SessionManager.getCurrentHostelId();
        hostelLabel.setText(SessionManager.getCurrentUser().getHostelName());
        progressBar.setProgress(0);
    }
    
    @FXML
    private void selectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Excel File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        
        selectedFile = fileChooser.showOpenDialog(fileLabel.getScene().getWindow());
        
        if (selectedFile != null) {
            fileLabel.setText(selectedFile.getName());
            log("File selected: " + selectedFile.getName());
        }
    }
    
    @FXML
    private void importData() {
        if (selectedFile == null) {
            showAlert("Error", "Please select a file first", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            if (selectedFile.getName().endsWith(".csv")) {
                importCSV();
            } else {
                importExcel();
            }
        } catch (Exception e) {
            e.printStackTrace();
            log("ERROR: " + e.getMessage());
            showAlert("Error", "Import failed: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void importExcel() throws Exception {
        // FIX: Use try-with-resources to prevent resource leak
        try (FileInputStream fis = new FileInputStream(selectedFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            int totalRows = sheet.getPhysicalNumberOfRows() - 1; // Exclude header
            int imported = 0;
            int errors = 0;
            
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement("""
                INSERT OR REPLACE INTO students (entry_number, name, hostel_id, room_number, phone, email)
                VALUES (?, ?, ?, ?, ?, ?)
            """);
            
            log("Starting import of " + totalRows + " records...");
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                    
                try {
                    String entryNumber = getCellValue(row.getCell(0));
                    String name = getCellValue(row.getCell(1));
                    String roomNumber = getCellValue(row.getCell(2));
                    String phone = getCellValue(row.getCell(3));
                    String email = getCellValue(row.getCell(4));
                    
                    if (entryNumber.isEmpty() || name.isEmpty()) {
                        log("Skipping row " + i + ": Missing entry number or name");
                        errors++;
                        continue;
                    }
                    
                    pstmt.setString(1, entryNumber);
                    pstmt.setString(2, name);
                    pstmt.setInt(3, hostelId);
                    pstmt.setString(4, roomNumber);
                    pstmt.setString(5, phone);
                    pstmt.setString(6, email);
                    pstmt.executeUpdate();
                    
                    imported++;
                    progressBar.setProgress((double) i / totalRows);
                    
                } catch (Exception e) {
                    log("Error on row " + i + ": " + e.getMessage());
                    errors++;
                }
            }
            
            log("Import complete! Imported: " + imported + ", Errors: " + errors);
            showAlert("Success", "Imported " + imported + " students successfully!", Alert.AlertType.INFORMATION);
        }
    }
    
    private void importCSV() throws Exception {
        // FIX: Use try-with-resources
        try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
            String line;
            int lineNum = 0;
            int imported = 0;
            int errors = 0;
            
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement("""
                INSERT OR REPLACE INTO students (entry_number, name, hostel_id, room_number, phone, email)
                VALUES (?, ?, ?, ?, ?, ?)
            """);
            
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (lineNum == 1) continue; // Skip header
                
                try {
                    String[] parts = line.split(",");
                    if (parts.length < 2) continue;
                    
                    pstmt.setString(1, parts[0].trim());
                    pstmt.setString(2, parts[1].trim());
                    pstmt.setInt(3, hostelId);
                    pstmt.setString(4, parts.length > 2 ? parts[2].trim() : "");
                    pstmt.setString(5, parts.length > 3 ? parts[3].trim() : "");
                    pstmt.setString(6, parts.length > 4 ? parts[4].trim() : "");
                    pstmt.executeUpdate();
                    
                    imported++;
                } catch (Exception e) {
                    log("Error on line " + lineNum + ": " + e.getMessage());
                    errors++;
                }
            }
            
            log("Import complete! Imported: " + imported + ", Errors: " + errors);
            showAlert("Success", "Imported " + imported + " students!", Alert.AlertType.INFORMATION);
        }
    }
    
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default -> "";
        };
    }
    
    private void log(String message) {
        logArea.appendText(message + "\n");
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    private void downloadTemplate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Template");
        fileChooser.setInitialFileName("students_template.csv");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        
        File file = fileChooser.showSaveDialog(fileLabel.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("entry_number,name,room_number,phone,email");
                writer.println("2021BCE001,John Doe,A-101,9876543210,john@example.com");
                writer.println("2021BCE002,Jane Smith,A-102,9876543211,jane@example.com");
                showAlert("Success", "Template saved!", Alert.AlertType.INFORMATION);
            }

             catch (Exception e) {
                showAlert("Error", "Failed to save template", Alert.AlertType.ERROR);
            }
        }
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