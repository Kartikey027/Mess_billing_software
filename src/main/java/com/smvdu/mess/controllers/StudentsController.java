package com.smvdu.mess.controllers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.smvdu.mess.App;
import com.smvdu.mess.database.DatabaseConnection;
import com.smvdu.mess.models.Student;
import com.smvdu.mess.utils.MessUtils;
import com.smvdu.mess.utils.SessionManager;
import com.smvdu.mess.utils.StudentReportPDFGenerator;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

public class StudentsController {

    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, String> entryNumberCol;
    @FXML private TableColumn<Student, String> nameCol;
    @FXML private TableColumn<Student, String> roomCol;
    @FXML private TableColumn<Student, Integer> messDaysCol;
    @FXML private TableColumn<Student, Integer> absentDaysCol;
    @FXML private TableColumn<Student, Void> actionCol;
    @FXML private TextField searchField;
    @FXML private Label hostelLabel;
    @FXML private Label totalLabel;
    @FXML private ComboBox<String> batchFilterCombo;
    @FXML private Button printButton;
    @FXML private HBox filterBox;

    private final ObservableList<Student> studentsList = FXCollections.observableArrayList();
    private final ObservableList<Student> allStudentsList = FXCollections.observableArrayList();
    private int hostelId;
    private int messId;

    @FXML
    public void initialize() {
        hostelId = SessionManager.getCurrentHostelId();
        messId = MessUtils.getMessIdForHostel(hostelId);

        hostelLabel.setText(
            SessionManager.getCurrentUser().getMessName() != null
                ? SessionManager.getCurrentUser().getMessName()
                : SessionManager.getCurrentUser().getHostelName()
        );

        setupTable();
        loadStudents();
        setupBatchFilter();

        searchField.textProperty().addListener((obs, o, n) -> filterStudents());
    }

    private void setupTable() {
        entryNumberCol.setCellValueFactory(new PropertyValueFactory<>("entryNumber"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        messDaysCol.setCellValueFactory(new PropertyValueFactory<>("messDays"));
        absentDaysCol.setCellValueFactory(new PropertyValueFactory<>("absentDays"));

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");

            {
                editBtn.getStyleClass().add("edit-button");
                editBtn.setOnAction(e -> showEditDialog(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : editBtn);
            }
        });

        studentsTable.setItems(studentsList);
    }

    private void loadStudents() {
        allStudentsList.clear();
        studentsList.clear();
        LocalDate now = LocalDate.now();
        
        int operatingDays = MessUtils.getOperatingDays(messId, now.getMonthValue(), now.getYear());

        try {
            List<Integer> hostelIds = MessUtils.getHostelIdsForMess(messId);
            String hostelIdsStr = MessUtils.hostelIdsToString(hostelIds);

            Connection conn = DatabaseConnection.getConnection();
            
            String sql =
                "SELECT s.*, " +
                "COALESCE(sa.mess_days, ?) mess_days, " +
                "COALESCE(sa.absent_days, 0) absent_days " +
                "FROM students s " +
                "LEFT JOIN student_attendance sa ON s.id = sa.student_id " +
                "AND sa.month = ? AND sa.year = ? " +
                "WHERE s.hostel_id IN (" + hostelIdsStr + ") AND s.is_active = 1 " +
                "ORDER BY s.entry_number";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, operatingDays);
            ps.setInt(2, now.getMonthValue());
            ps.setInt(3, now.getYear());

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Student st = new Student(
                    rs.getInt("id"),
                    rs.getString("entry_number"),
                    rs.getString("name"),
                    rs.getInt("hostel_id"),
                    rs.getString("room_number"),
                    rs.getString("phone"),
                    rs.getString("email"),
                    rs.getInt("is_active") == 1
                );
                st.setMessDays(rs.getInt("mess_days"));
                st.setAbsentDays(rs.getInt("absent_days"));
                allStudentsList.add(st);
            }

            studentsList.setAll(allStudentsList);
            totalLabel.setText("Total: " + studentsList.size() + " students");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ✅ NEW: Setup batch filter dropdown
    private void setupBatchFilter() {
        Set<String> batches = new HashSet<>();
        batches.add("All Batches");

        for (Student student : allStudentsList) {
            String batch = extractBatch(student.getEntryNumber());
            if (batch != null) {
                batches.add(batch);
            }
        }

        List<String> sortedBatches = new ArrayList<>(batches);
        sortedBatches.sort((a, b) -> {
            if (a.equals("All Batches")) return -1;
            if (b.equals("All Batches")) return 1;
            return b.compareTo(a); // Descending order (2025, 2024, 2023...)
        });

        batchFilterCombo.setItems(FXCollections.observableArrayList(sortedBatches));
        batchFilterCombo.setValue("All Batches");
        
        batchFilterCombo.setOnAction(e -> filterStudents());
    }

    // ✅ NEW: Extract batch year from entry number
    private String extractBatch(String entryNumber) {
        if (entryNumber == null || entryNumber.length() < 2) {
            return null;
        }

        try {
            // Extract first 2 digits (e.g., "23" from "23BCS079")
            String yearPrefix = entryNumber.substring(0, 2);
            int year = Integer.parseInt(yearPrefix);
            
            // Convert to full year (23 -> 2023, 25 -> 2025)
            int fullYear = 2000 + year;
            
            return String.valueOf(fullYear);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ✅ UPDATED: Filter by both search and batch
    private void filterStudents() {
        String searchQuery = searchField.getText();
        String selectedBatch = batchFilterCombo.getValue();

        ObservableList<Student> filtered = allStudentsList.filtered(student -> {
            // Batch filter
            boolean batchMatch = true;
            if (selectedBatch != null && !selectedBatch.equals("All Batches")) {
                String studentBatch = extractBatch(student.getEntryNumber());
                batchMatch = selectedBatch.equals(studentBatch);
            }

            // Search filter
            boolean searchMatch = true;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                String lowerQuery = searchQuery.toLowerCase();
                searchMatch = student.getName().toLowerCase().contains(lowerQuery)
                           || student.getEntryNumber().toLowerCase().contains(lowerQuery);
            }

            return batchMatch && searchMatch;
        });

        studentsList.setAll(filtered);
        totalLabel.setText("Total: " + studentsList.size() + " students");
    }

    // ✅ NEW: Print button handler
    @FXML
    private void handlePrint() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Print Student Report");
        dialog.setHeaderText("Select report type");

        // Create buttons
        ButtonType allStudentsBtn = new ButtonType("All Students");
        ButtonType absentStudentsBtn = new ButtonType("Absent Students");
        ButtonType presentStudentsBtn = new ButtonType("Present Students");
        ButtonType cancelBtn = ButtonType.CANCEL;

        dialog.getDialogPane().getButtonTypes().addAll(
            allStudentsBtn, absentStudentsBtn, presentStudentsBtn, cancelBtn
        );

        dialog.setResultConverter(button -> {
            if (button == allStudentsBtn) return "ALL";
            if (button == absentStudentsBtn) return "ABSENT";
            if (button == presentStudentsBtn) return "PRESENT";
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(this::generatePDF);
    }

    // ✅ NEW: Generate PDF based on selection
    private void generatePDF(String reportType) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Student Report");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );

            String fileName = "Students_" + reportType + "_" + 
                            LocalDate.now().toString().replace("-", "") + ".pdf";
            fileChooser.setInitialFileName(fileName);

            File file = fileChooser.showSaveDialog(studentsTable.getScene().getWindow());

            if (file != null) {
                List<Student> studentsToReport = new ArrayList<>();

                switch (reportType) {
                    case "ALL":
                        studentsToReport.addAll(studentsList);
                        break;
                    case "ABSENT":
                        for (Student s : studentsList) {
                            if (s.getAbsentDays() > 0) {
                                studentsToReport.add(s);
                            }
                        }
                        break;
                    case "PRESENT":
                        for (Student s : studentsList) {
                            if (s.getAbsentDays() == 0) {
                                studentsToReport.add(s);
                            }
                        }
                        break;
                }

                String messName = SessionManager.getCurrentUser().getMessName() != null
                    ? SessionManager.getCurrentUser().getMessName()
                    : SessionManager.getCurrentUser().getHostelName();

                LocalDate now = LocalDate.now();
                int operatingDays = MessUtils.getOperatingDays(messId, now.getMonthValue(), now.getYear());

                StudentReportPDFGenerator.generateStudentReport(
                    file.getAbsolutePath(),
                    "SHRI MATA VAISHNO DEVI UNIVERSITY",
                    messName,
                    reportType,
                    studentsToReport,
                    operatingDays,
                    now
                );

                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION
                );
                alert.setTitle("Success");
                alert.setContentText("Report generated successfully!\nFile: " + file.getName());
                alert.showAndWait();
            }

        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle("Error");
            alert.setContentText("Failed to generate report: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void showEditDialog(Student student) {
        LocalDate now = LocalDate.now();
        int operatingDays = MessUtils.getOperatingDays(messId, now.getMonthValue(), now.getYear());

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Attendance");
        dialog.setHeaderText(student.getEntryNumber() + " - " + student.getName());

        Spinner<Integer> absentSpinner =
            new Spinner<>(0, operatingDays, student.getAbsentDays());

        Label messDaysLabel =
            new Label(String.valueOf(operatingDays - student.getAbsentDays()));

        absentSpinner.valueProperty().addListener((obs, o, n) ->
            messDaysLabel.setText(String.valueOf(operatingDays - n)));

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        grid.addRow(0, new Label("Operating Days:"), new Label(String.valueOf(operatingDays)));
        grid.addRow(1, new Label("Absent Days:"), absentSpinner);
        grid.addRow(2, new Label("Mess Days:"), messDaysLabel);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                updateAttendance(
                    student.getId(),
                    operatingDays - absentSpinner.getValue(),
                    absentSpinner.getValue(),
                    operatingDays
                );
                loadStudents();
                filterStudents();
            }
        });
    }

    private void updateAttendance(int studentId, int messDays, int absentDays, int totalDays) {
        LocalDate now = LocalDate.now();

        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO student_attendance " +
                "(student_id, month, year, total_days, mess_days, absent_days) " +
                "VALUES (?,?,?,?,?,?) " +
                "ON CONFLICT(student_id, month, year) DO UPDATE SET " +
                "total_days = excluded.total_days, " +
                "mess_days = excluded.mess_days, " +
                "absent_days = excluded.absent_days, " +
                "updated_at = CURRENT_TIMESTAMP"
            );

            ps.setInt(1, studentId);
            ps.setInt(2, now.getMonthValue());
            ps.setInt(3, now.getYear());
            ps.setInt(4, totalDays);
            ps.setInt(5, messDays);
            ps.setInt(6, absentDays);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
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