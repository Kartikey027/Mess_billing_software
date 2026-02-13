package com.smvdu.mess.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import com.smvdu.mess.App;
import com.smvdu.mess.database.DatabaseConnection;
import com.smvdu.mess.models.Student;
import com.smvdu.mess.utils.MessUtils;
import com.smvdu.mess.utils.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

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

    private final ObservableList<Student> studentsList = FXCollections.observableArrayList();
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

        searchField.textProperty().addListener((obs, o, n) -> filterStudents(n));
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
        studentsList.clear();
        LocalDate now = LocalDate.now();
        
        // Use centralized utility to get operating days
        int operatingDays = MessUtils.getOperatingDays(messId, now.getMonthValue(), now.getYear());

        try {
            // Get all hostel IDs for this mess
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
                studentsList.add(st);
            }

            totalLabel.setText("Total: " + studentsList.size() + " students");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showEditDialog(Student student) {
        LocalDate now = LocalDate.now();
        
        // Use centralized utility to get operating days
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

    private void filterStudents(String q) {
        if (q == null || q.isEmpty()) {
            studentsTable.setItems(studentsList);
            return;
        }
        studentsTable.setItems(studentsList.filtered(
            s -> s.getName().toLowerCase().contains(q.toLowerCase())
                || s.getEntryNumber().toLowerCase().contains(q.toLowerCase())
        ));
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