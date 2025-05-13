package com.example.bdsqltester.scenes.admin;

import com.example.bdsqltester.HelloApplication;
import com.example.bdsqltester.datasources.GradingDataSource;
import com.example.bdsqltester.datasources.MainDataSource;
import com.example.bdsqltester.dtos.Assignment;
import com.example.bdsqltester.dtos.Grade;
import com.example.bdsqltester.dtos.Submission;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class AdminController {

    @FXML
    private ListView<Assignment> assignmentList;
    @FXML
    private TextField idField;
    @FXML
    private TextField nameField;
    @FXML
    private TextArea instructionsField;
    @FXML
    private TextArea answerKeyField;
    @FXML
    private Button deleteButton;

    @FXML
    private TableView<Submission> adminSubmissionHistoryTable;
    @FXML
    private TableColumn<Submission, String> adminSubmissionUserColumn;
    @FXML
    private TableColumn<Submission, String> adminTimestampColumn;
    @FXML
    private TableColumn<Submission, Integer> adminGradeColumn;
    @FXML
    private TableColumn<Submission, String> adminSubmittedQueryColumn;

    private ObservableList<Assignment> assignments = FXCollections.observableArrayList();
    private final ObservableList<Submission> adminSubmissionHistory = FXCollections.observableArrayList();


    @FXML
    public void initialize() {
        loadAssignments();
        assignmentList.setItems(assignments);
        assignmentList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    showAssignmentDetails(newValue);
                    if (newValue != null) {
                        loadAdminSubmissionHistory(newValue.getId());
                        deleteButton.setDisable(false);
                    } else {
                        adminSubmissionHistory.clear();
                        deleteButton.setDisable(true);
                    }
                });

        if (adminSubmissionUserColumn != null) {
            adminSubmissionUserColumn.setCellValueFactory(cellData -> {
                if (cellData == null || cellData.getValue() == null) {
                    return new SimpleStringProperty("N/A");
                }
                try (Connection conn = MainDataSource.getConnection()) {
                    PreparedStatement stmt = conn.prepareStatement("SELECT username FROM public.users WHERE id = ?");
                    stmt.setLong(1, cellData.getValue().getUserId());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        return new SimpleStringProperty(rs.getString("username"));
                    }
                } catch (SQLException e) {
                    System.err.println("SQL Exception in adminSubmissionUserColumn: " + e.getMessage());
                    e.printStackTrace();
                }
                return new SimpleStringProperty("N/A");
            });
        }

        if (adminTimestampColumn != null) {
            adminTimestampColumn.setCellValueFactory(new PropertyValueFactory<>("submissionTimestamp"));
        }
        if (adminGradeColumn != null) {
            adminGradeColumn.setCellValueFactory(new PropertyValueFactory<>("gradeObtained"));
            adminGradeColumn.setCellFactory(column -> new TableCell<Submission, Integer>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item.toString());
                        if (item == 100) {
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        } else if (item == 50) {
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                        } else { // 0
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        }
                    }
                }
            });
        }
        if (adminSubmittedQueryColumn != null) {
            adminSubmittedQueryColumn.setCellValueFactory(cellData -> {
                if (cellData == null || cellData.getValue() == null) {
                    return new SimpleStringProperty("");
                }
                String query = cellData.getValue().getSubmittedQuery();
                return new SimpleStringProperty(query != null && query.length() > 50 ? query.substring(0, 47) + "..." : query);
            });
        }

        if (adminSubmissionHistoryTable != null) {
            adminSubmissionHistoryTable.setItems(adminSubmissionHistory);
        }

        deleteButton.setDisable(true);
    }

    private void loadAssignments() {
        assignments.clear();
        try (Connection conn = MainDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, name, instructions, answer_key FROM public.assignments")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                assignments.add(new Assignment(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("instructions"),
                        rs.getString("answer_key")
                ));
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Gagal memuat tugas. Pastikan tabel 'assignments' ada di database. " + e.getMessage(), e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAssignmentDetails(Assignment assignment) {
        if (assignment != null) {
            idField.setText(String.valueOf(assignment.getId()));
            nameField.setText(assignment.getName());
            instructionsField.setText(assignment.getInstructions());
            answerKeyField.setText(assignment.getAnswerKey());
        } else {
            idField.setText("");
            nameField.setText("");
            instructionsField.setText("");
            answerKeyField.setText("");
        }
    }

    @FXML
    void onNewAssignmentClick() {
        idField.setText("");
        nameField.setText("");
        instructionsField.setText("");
        answerKeyField.setText("");
        deleteButton.setDisable(true);
        assignmentList.getSelectionModel().clearSelection();
        showAlert("Info", "Tugas Baru", "Silakan masukkan detail tugas baru.");
    }

    @FXML
    void onSaveClick() {
        String name = nameField.getText();
        String instructions = instructionsField.getText();
        String answerKey = answerKeyField.getText();

        if (name.isEmpty() || instructions.isEmpty() || answerKey.isEmpty()) {
            showAlert("Peringatan", "Data Tidak Lengkap", "Nama, Instruksi, dan Kunci Jawaban tidak boleh kosong.");
            return;
        }

        try (Connection conn = MainDataSource.getConnection()) {
            if (idField.getText().isEmpty()) {
                String insertQuery = "INSERT INTO public.assignments (name, instructions, answer_key) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                stmt.setString(1, name);
                stmt.setString(2, instructions);
                stmt.setString(3, answerKey);
                stmt.executeUpdate();

                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    Long newId = generatedKeys.getLong(1);
                    idField.setText(String.valueOf(newId));
                    showAlert("Sukses", "Tugas Baru Tersimpan", "Tugas '" + name + "' berhasil ditambahkan.");
                }

            } else {
                Long id = Long.parseLong(idField.getText());
                String updateQuery = "UPDATE public.assignments SET name = ?, instructions = ?, answer_key = ? WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(updateQuery);
                stmt.setString(1, name);
                stmt.setString(2, instructions);
                stmt.setString(3, answerKey);
                stmt.setLong(4, id);
                stmt.executeUpdate();
                showAlert("Sukses", "Tugas Tersimpan", "Tugas '" + name + "' berhasil diperbarui.");
            }
            loadAssignments();
            if (!idField.getText().isEmpty()) {
                Long currentId = Long.parseLong(idField.getText());
                assignments.stream()
                        .filter(a -> a.getId() != null && a.getId().equals(currentId))
                        .findFirst()
                        .ifPresent(assignmentList.getSelectionModel()::select);
                deleteButton.setDisable(false);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Gagal menyimpan tugas. " + e.getMessage(), e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid ID", "ID tugas tidak valid. " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void onDeleteClick(ActionEvent event) {
        String idText = idField.getText();
        if (idText.isEmpty()) {
            showAlert("Peringatan", "Pilih Tugas", "Silakan pilih tugas yang ingin dihapus.");
            return;
        }

        Long idToDelete;
        try {
            idToDelete = Long.parseLong(idText);
        } catch (NumberFormatException e) {
            showAlert("Error", "ID Tidak Valid", "ID tugas tidak valid. " + e.getMessage());
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Konfirmasi Penghapusan");
        confirmationAlert.setHeaderText("Hapus Tugas Ini?");
        confirmationAlert.setContentText("Anda yakin ingin menghapus tugas dengan ID " + idToDelete + " dan semua data terkait (submission dan grade)?");

        confirmationAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = MainDataSource.getConnection()) {
                    String deleteGradesQuery = "DELETE FROM public.grades WHERE assignment_id = ?";
                    PreparedStatement stmtGrades = conn.prepareStatement(deleteGradesQuery);
                    stmtGrades.setLong(1, idToDelete);
                    stmtGrades.executeUpdate();

                    String deleteSubmissionsQuery = "DELETE FROM public.submissions WHERE assignment_id = ?";
                    PreparedStatement stmtSub = conn.prepareStatement(deleteSubmissionsQuery);
                    stmtSub.setLong(1, idToDelete);
                    stmtSub.executeUpdate();

                    String deleteAssignmentQuery = "DELETE FROM public.assignments WHERE id = ?";
                    PreparedStatement stmtAssignment = conn.prepareStatement(deleteAssignmentQuery);
                    stmtAssignment.setLong(1, idToDelete);
                    int rowsAffected = stmtAssignment.executeUpdate();

                    if (rowsAffected > 0) {
                        showAlert("Sukses", "Tugas Dihapus", "Tugas berhasil dihapus.");
                        loadAssignments();
                        showAssignmentDetails(null);
                        adminSubmissionHistory.clear();
                        deleteButton.setDisable(true);
                    } else {
                        showAlert("Gagal", "Penghapusan Gagal", "Tugas tidak ditemukan atau gagal dihapus.");
                    }
                } catch (SQLException e) {
                    showAlert("Database Error", "Gagal menghapus tugas. " + e.getMessage(), e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    void onTestButtonClick(ActionEvent event) {
        String answerKeySql = answerKeyField.getText();
        if (answerKeySql.isEmpty()) {
            showAlert("Peringatan", "Kunci Jawaban Kosong", "Silakan masukkan kunci jawaban SQL untuk diuji.");
            return;
        }

        Stage stage = new Stage();
        stage.setTitle("Answer Key Results");

        TableView<ArrayList<String>> tableView = new TableView<>();

        ObservableList<ArrayList<String>> data = FXCollections.observableArrayList();
        ArrayList<String> headers = new ArrayList<>();

        try (Connection conn = GradingDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(answerKeySql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                final int columnIndex = i - 1;
                String headerText = metaData.getColumnLabel(i);
                headers.add(headerText);

                TableColumn<ArrayList<String>, String> column = new TableColumn<>(headerText);

                column.setCellValueFactory(cellData -> {
                    ArrayList<String> rowData = cellData.getValue();
                    if (rowData != null && columnIndex < rowData.size()) {
                        return new SimpleStringProperty(rowData.get(columnIndex));
                    } else {
                        return new SimpleStringProperty("");
                    }
                });
                column.setPrefWidth(120);
                tableView.getColumns().add(column);
            }

            while (rs.next()) {
                ArrayList<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    row.add(value != null ? value : "");
                }
                data.add(row);
            }

            if (headers.isEmpty() && data.isEmpty()) {
                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                infoAlert.setTitle("Query Results");
                infoAlert.setHeaderText(null);
                infoAlert.setContentText("The answer key query executed successfully but returned no data.");
                infoAlert.showAndWait();
                return;
            }

            tableView.setItems(data);

            StackPane root = new StackPane();
            root.getChildren().add(tableView);
            Scene scene = new Scene(root, 800, 600);

            stage.setScene(scene);
            stage.show();

        } catch (SQLException e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Database Error");
            errorAlert.setHeaderText("Failed to execute the answer key query or retrieve results.");
            errorAlert.setContentText("SQL Error: " + e.getMessage());
            errorAlert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText("An unexpected error occurred.");
            errorAlert.setContentText(e.getMessage());
            errorAlert.showAndWait();
        }
    }

    @FXML
    void onShowGradesClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("grades-overview-view.fxml"));
            AnchorPane gradesRoot = loader.load();
            GradesOverviewController gradesController = loader.getController();

            ObservableList<Grade> allFinalGrades = loadAllFinalGrades();

            gradesController.setGradesData(allFinalGrades);

            Stage gradesStage = new Stage();
            gradesStage.setTitle("Overall Grades - Latest Submission");
            gradesStage.setScene(new Scene(gradesRoot));
            gradesStage.initModality(Modality.APPLICATION_MODAL);
            gradesStage.show();

        } catch (IOException e) {
            showAlert("Error", "Gagal memuat tampilan Overall Grades. " + e.getMessage(), e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadAdminSubmissionHistory(Long assignmentId) {
        adminSubmissionHistory.clear();
        if (assignmentId != null) {
            try (Connection c = MainDataSource.getConnection()) {
                String query = "SELECT s.id, s.user_id, s.assignment_id, s.submitted_query, s.grade_obtained, s.submission_timestamp, u.username " +
                        "FROM public.submissions s " +
                        "JOIN public.users u ON s.user_id = u.id " +
                        "WHERE s.assignment_id = ? " +
                        "ORDER BY s.submission_timestamp DESC";
                PreparedStatement stmt = c.prepareStatement(query);
                stmt.setLong(1, assignmentId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Submission submission = new Submission(rs);
                    submission.setUserName(rs.getString("username"));
                    adminSubmissionHistory.add(submission);
                }
            } catch (SQLException e) {
                showAlert("Database Error", "Gagal memuat riwayat submission untuk tugas ini. " + e.getMessage(), e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private ObservableList<Grade> loadAllFinalGrades() {
        ObservableList<Grade> allFinalGrades = FXCollections.observableArrayList();
        try (Connection c = MainDataSource.getConnection()) {
            String query = "WITH LatestSubmissions AS (" +
                    "    SELECT " +
                    "        s.id, " +
                    "        s.user_id, " +
                    "        s.assignment_id, " +
                    "        s.grade_obtained, " +
                    "        s.submission_timestamp, " +
                    "        ROW_NUMBER() OVER(PARTITION BY s.user_id, s.assignment_id ORDER BY s.submission_timestamp DESC) as rn " +
                    "    FROM " +
                    "        public.submissions s" +
                    ") " +
                    "SELECT " +
                    "    u.id AS user_id, " +
                    "    u.username, " +
                    "    a.id AS assignment_id, " +
                    "    a.name AS assignment_name, " +
                    "    ls.grade_obtained AS final_grade " +
                    "FROM " +
                    "    public.users u " +
                    "JOIN " +
                    "    LatestSubmissions ls ON u.id = ls.user_id " +
                    "JOIN " +
                    "    public.assignments a ON ls.assignment_id = a.id " +
                    "WHERE " +
                    "    ls.rn = 1 " +
                    "ORDER BY " +
                    "    final_grade DESC, u.username, a.name";

            PreparedStatement stmt = c.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Grade grade = new Grade(
                        rs.getLong("user_id"),
                        rs.getLong("assignment_id"),
                        (double) rs.getInt("final_grade"),
                        rs.getString("username"),
                        rs.getString("assignment_name")
                );
                allFinalGrades.add(grade);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Gagal memuat semua nilai akhir. " + e.getMessage(), e.getMessage());
            e.printStackTrace();
        }
        return allFinalGrades;
    }

    @FXML
    void onAdminViewQueryDetailsClick(ActionEvent event) {
        Submission selectedSubmission = adminSubmissionHistoryTable.getSelectionModel().getSelectedItem();
        if (selectedSubmission != null) {
            String submittedQuery = selectedSubmission.getSubmittedQuery();

            StringBuilder resultText = new StringBuilder();
            try (Connection conn = GradingDataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                boolean isResultSet = stmt.execute(submittedQuery);

                if (isResultSet) {
                    ResultSet rs = stmt.getResultSet();
                    resultText.append("Hasil Query:\n\n");

                    ResultSetMetaData rsmd = rs.getMetaData();
                    int columnCount = rsmd.getColumnCount();

                    for (int i = 1; i <= columnCount; i++) {
                        resultText.append(String.format("%-25s", rsmd.getColumnName(i)));
                    }
                    resultText.append("\n----------------------------------------------------------------------------\n");

                    int rowCount = 0;
                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            Object columnValue = rs.getObject(i);
                            resultText.append(String.format("%-25s", (columnValue != null ? columnValue.toString() : "NULL")));
                        }
                        resultText.append("\n");
                        rowCount++;
                    }
                    resultText.append("\nJumlah baris: ").append(rowCount);
                } else {
                    int updateCount = stmt.getUpdateCount();
                    resultText.append("Query berhasil dieksekusi (Update/Insert/Delete).\n");
                    resultText.append("Jumlah baris terpengaruh: ").append(updateCount);
                }

                Alert resultAlert = new Alert(Alert.AlertType.INFORMATION);
                resultAlert.setTitle("Submitted Query Result");
                resultAlert.setHeaderText("Hasil Eksekusi Query yang Dikirimkan:");

                TextArea textArea = new TextArea(resultText.toString());
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);

                resultAlert.getDialogPane().setExpandableContent(textArea);
                resultAlert.getDialogPane().setExpanded(true);
                resultAlert.getDialogPane().setPrefWidth(800);
                resultAlert.getDialogPane().setPrefHeight(600);
                resultAlert.showAndWait();

            } catch (SQLException e) {
                showAlert("SQL Error", "Gagal mengeksekusi query yang tersimpan. Pastikan database 'oracle_hr' aktif dan tabel tersedia.", e.getMessage());
                e.printStackTrace();
            }

        } else {
            showAlert("Peringatan", "Pilih Submission", "Silakan pilih baris submission dari tabel riwayat.");
        }
    }


    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}