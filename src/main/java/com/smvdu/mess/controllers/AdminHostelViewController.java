package com.smvdu.mess.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import com.smvdu.mess.App;
import com.smvdu.mess.database.DatabaseConnection;
import com.smvdu.mess.models.Student;
import com.smvdu.mess.utils.AdminSessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminHostelViewController {
    
    @FXML private Label hostelNameLabel;
    @FXML private Label messNameLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label activeStudentsLabel;
    @FXML private Label daysInMonthLabel;
    @FXML private Label totalMessDaysLabel;
    @FXML private Label estimatedBillLabel;
    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, String> entryNumberCol;
    @FXML private TableColumn<Student, String> nameCol;
    @FXML private TableColumn<Student, String> roomCol;
    @FXML private TableColumn<Student, Integer> messDaysCol;
    @FXML private TableColumn<Student, Integer> absentDaysCol;
    @FXML private Label monthlyFineLabel;
    
    private int messId;
    private ObservableList<Student> studentsList = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        // Will be called after setMessInfo
    }
    
    public void setMessInfo(int messId, String messName) {
        this.messId = messId;
        hostelNameLabel.setText(messName);
        messNameLabel.setText(messName);
        setupTable();
        loadData();
    }
    
    // Backward compatibility
    public void setHostelInfo(int hostelId, String hostelName, String messName) {
        this.messId = hostelId;
        hostelNameLabel.setText(messName);
        messNameLabel.setText(messName);
        setupTable();
        loadData();
    }
    
    private void setupTable() {
        entryNumberCol.setCellValueFactory(new PropertyValueFactory<>("entryNumber"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        messDaysCol.setCellValueFactory(new PropertyValueFactory<>("messDays"));
        absentDaysCol.setCellValueFactory(new PropertyValueFactory<>("absentDays"));
        
        studentsTable.setItems(studentsList);
    }
    
  private void loadData() {
    LocalDate now = LocalDate.now();
    int daysInMonth = now.lengthOfMonth();
    
    try {
        Connection conn = DatabaseConnection.getConnection();
        
        // Get all hostel IDs that belong to this mess
        PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM hostels WHERE mess_id = ?");
        pstmt.setInt(1, messId);
        ResultSet rs = pstmt.executeQuery();
        
        StringBuilder hostelIds = new StringBuilder();
        while (rs.next()) {
            if (hostelIds.length() > 0) hostelIds.append(",");
            hostelIds.append(rs.getInt("id"));
        }
        
        // Get statistics
        String query = "SELECT COUNT(*) FROM students WHERE hostel_id IN (" + hostelIds + ")";
        rs = conn.createStatement().executeQuery(query);
        int totalStudents = rs.next() ? rs.getInt(1) : 0;
        totalStudentsLabel.setText(String.valueOf(totalStudents));
        
        query = "SELECT COUNT(*) FROM students WHERE hostel_id IN (" + hostelIds + ") AND is_active = 1";
        rs = conn.createStatement().executeQuery(query);
        int activeStudents = rs.next() ? rs.getInt(1) : 0;
        activeStudentsLabel.setText(String.valueOf(activeStudents));
        
        daysInMonthLabel.setText(String.valueOf(daysInMonth));
        
        // Calculate total mess days
        String absentQuery = "SELECT COALESCE(SUM(sa.absent_days), 0) as total_absent_days " +
                           "FROM students s " +
                           "LEFT JOIN student_attendance sa ON s.id = sa.student_id " +
                           "AND sa.month = ? AND sa.year = ? " +
                           "WHERE s.hostel_id IN (" + hostelIds + ") AND s.is_active = 1";
        
        pstmt = conn.prepareStatement(absentQuery);
        pstmt.setInt(1, now.getMonthValue());
        pstmt.setInt(2, now.getYear());
        rs = pstmt.executeQuery();
        
        int totalAbsentDays = rs.next() ? rs.getInt("total_absent_days") : 0;
        int totalPossibleDays = activeStudents * daysInMonth;
        int netMessDays = totalPossibleDays - totalAbsentDays;
        totalMessDaysLabel.setText(String.valueOf(netMessDays));
        
        // Get rate and calculate bill
        pstmt = conn.prepareStatement("SELECT value FROM settings WHERE key = 'per_day_rate'");
        rs = pstmt.executeQuery();
        double perDayRate = rs.next() ? Double.parseDouble(rs.getString("value")) : 120.0;
        
        pstmt = conn.prepareStatement("SELECT value FROM settings WHERE key = 'gst_percent'");
        rs = pstmt.executeQuery();
        double gstPercent = rs.next() ? Double.parseDouble(rs.getString("value")) : 5.0;
        
        double subtotal = netMessDays * perDayRate;
        double gst = subtotal * (gstPercent / 100);
        double total = subtotal + gst;
        estimatedBillLabel.setText(String.format("₹%.2f", total));
        
        // Load students
        loadStudents(hostelIds.toString());

        loadMonthlyFine(hostelIds.toString());
        
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    private void loadMonthlyFine(String hostelIds) {
        LocalDate now = LocalDate.now();

        try {
            Connection conn = DatabaseConnection.getConnection();

            String query = "SELECT COALESCE(SUM(fine_amount),0) " +
                    "FROM bills WHERE hostel_id IN (" + hostelIds + ") " +
                    "AND month = ? AND year = ?";

            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, now.getMonthValue());
            ps.setInt(2, now.getYear());

            ResultSet rs = ps.executeQuery();
            double fine = rs.next() ? rs.getDouble(1) : 0;

            monthlyFineLabel.setText(String.format("₹%.2f", fine));

        } catch (Exception e) {
            e.printStackTrace();
            monthlyFineLabel.setText("₹0.00");
        }
    }

private void loadStudents(String hostelIds) {
    studentsList.clear();
    LocalDate now = LocalDate.now();
    int daysInMonth = now.lengthOfMonth();
    
    try {
        Connection conn = DatabaseConnection.getConnection();
        
        String studentQuery = "SELECT s.*, " +
                             "COALESCE(sa.mess_days, ?) as mess_days, " +
                             "COALESCE(sa.absent_days, 0) as absent_days " +
                             "FROM students s " +
                             "LEFT JOIN student_attendance sa ON s.id = sa.student_id " +
                             "AND sa.month = ? AND sa.year = ? " +
                             "WHERE s.hostel_id IN (" + hostelIds + ") AND s.is_active = 1 " +
                             "ORDER BY s.entry_number";
        
        PreparedStatement pstmt = conn.prepareStatement(studentQuery);
        pstmt.setInt(1, daysInMonth);
        pstmt.setInt(2, now.getMonthValue());
        pstmt.setInt(3, now.getYear());
        
        ResultSet rs = pstmt.executeQuery();
        
        while (rs.next()) {
            Student student = new Student(
                rs.getInt("id"),
                rs.getString("entry_number"),
                rs.getString("name"),
                rs.getInt("hostel_id"),
                rs.getString("room_number"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getInt("is_active") == 1
            );
            student.setMessDays(rs.getInt("mess_days"));
            student.setAbsentDays(rs.getInt("absent_days"));
            studentsList.add(student);
        }
        
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    
    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/admin_dashboard.fxml")
            );
            Parent root = loader.load();
            
            AdminDashboardController controller = loader.getController();
            
            // Restore admin info from session
            String adminName = AdminSessionManager.getAdminName();
            String designation = AdminSessionManager.getDesignation();
            
            if (adminName != null && designation != null) {
                controller.setAdminInfo(adminName, designation);
            }
            
            App.getPrimaryStage().getScene().setRoot(root);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
