package com.example.bdsqltester.scenes.user;

import com.example.bdsqltester.HelloApplication; // Pastikan ini ada jika digunakan
import com.example.bdsqltester.datasources.GradingDataSource;
import com.example.bdsqltester.datasources.MainDataSource;
import com.example.bdsqltester.dtos.Assignment;
import com.example.bdsqltester.dtos.Submission; // Pastikan Submission DTO ada
import com.example.bdsqltester.dtos.User; // Pastikan User DTO ada

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader; // Pastikan ini ada jika digunakan
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane; // Pastikan ini ada jika digunakan
import javafx.scene.layout.StackPane; // Diperlukan untuk TableView di pop-up
import javafx.scene.layout.VBox; // Pastikan ini ada jika digunakan
import javafx.stage.Modality; // Pastikan ini ada jika digunakan
import javafx.stage.Stage;

import java.io.IOException; // Pastikan ini ada jika digunakan
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors; // Diperlukan untuk calculateGrade

public class UserController {

    @FXML
    private Label assignmentNameLabel;
    @FXML
    private TextArea assignmentInstructionsArea;
    @FXML
    private TextArea userAnswerArea;
    @FXML
    private Label gradeLabel;
    @FXML
    private ListView<Assignment> assignmentListView;
    @FXML
    private Label notificationLabel; // Tambahkan jika ada di FXML

    @FXML
    private TableView<Submission> submissionHistoryTable; // Tambahkan jika ada di FXML
    @FXML
    private TableColumn<Submission, Timestamp> timestampColumn; // Tambahkan jika ada di FXML
    @FXML
    private TableColumn<Submission, Integer> gradeColumn; // Tambahkan jika ada di FXML
    @FXML
    private TableColumn<Submission, String> submittedQueryColumn; // Tambahkan jika ada di FXML


    private ObservableList<Assignment> assignments = FXCollections.observableArrayList();
    private ObservableList<Submission> submissionHistory = FXCollections.observableArrayList(); // Tambahkan jika ada riwayat
    private Assignment currentAssignment;
    private User currentUser; // Akan diatur setelah login

    // Pastikan userId digunakan jika ada di FXML untuk login, jika tidak hapus ini
    // private Long loggedInUserId; // Dihapus karena diganti dengan currentUser.getId()

    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Jika ada elemen UI yang perlu diperbarui berdasarkan user, lakukan di sini
        // Misalnya: refreshAssignmentList(); // Uncomment jika Anda ingin memuat tugas setelah user diset
        if (currentUser != null) {
            loadAssignments(); // Panggil loadAssignments setelah currentUser diatur
        }
    }

    @FXML
    public void initialize() {
        // Pastikan kolom-kolom terhubung ke FXML dan tidak null
        if (timestampColumn != null) {
            timestampColumn.setCellValueFactory(new PropertyValueFactory<>("submissionTimestamp"));
        }
        if (gradeColumn != null) {
            gradeColumn.setCellValueFactory(new PropertyValueFactory<>("gradeObtained"));
            gradeColumn.setCellFactory(column -> new TableCell<Submission, Integer>() {
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
        if (submittedQueryColumn != null) {
            submittedQueryColumn.setCellValueFactory(cellData -> {
                if (cellData == null || cellData.getValue() == null) {
                    return new SimpleStringProperty("");
                }
                String query = cellData.getValue().getSubmittedQuery();
                return new SimpleStringProperty(query != null && query.length() > 50 ? query.substring(0, 47) + "..." : query);
            });
        }

        if (submissionHistoryTable != null) {
            submissionHistoryTable.setItems(submissionHistory);
        } else {
            System.err.println("ERROR: submissionHistoryTable is null in initialize()");
        }

        // loadAssignments(); // Akan dipanggil setelah setCurrentUser
        assignmentListView.setItems(assignments);
        assignmentListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    currentAssignment = newValue;
                    showAssignmentDetails(newValue);
                    if (newValue != null) {
                        loadSubmissionHistory(newValue.getId());
                        gradeLabel.setText("Grade: -"); // Reset grade saat tugas baru dipilih
                        userAnswerArea.setText(""); // Clear previous answer
                        if (notificationLabel != null) { // Cek null sebelum menggunakan
                            notificationLabel.setText(""); // Clear notifications
                        }
                    } else {
                        submissionHistory.clear();
                    }
                });
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
            showAlert("Database Error", "Gagal memuat tugas.", e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAssignmentDetails(Assignment assignment) {
        if (assignment != null) {
            assignmentNameLabel.setText(assignment.getName());
            assignmentInstructionsArea.setText(assignment.getInstructions());
        } else {
            assignmentNameLabel.setText("Pilih Tugas");
            assignmentInstructionsArea.setText("");
        }
    }

    private void loadSubmissionHistory(Long assignmentId) {
        submissionHistory.clear();
        if (assignmentId != null && currentUser != null) {
            try (Connection conn = MainDataSource.getConnection()) {
                String query = "SELECT id, user_id, assignment_id, submitted_query, grade_obtained, submission_timestamp " +
                        "FROM public.submissions " +
                        "WHERE user_id = ? AND assignment_id = ? " +
                        "ORDER BY submission_timestamp DESC";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setLong(1, currentUser.getId());
                stmt.setLong(2, assignmentId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    submissionHistory.add(new Submission(rs));
                }
                // Update gradeLabel with the latest submission grade if available
                if (!submissionHistory.isEmpty()) {
                    Submission latestSubmission = submissionHistory.get(0);
                    gradeLabel.setText("Grade: " + latestSubmission.getGradeObtained());
                } else {
                    gradeLabel.setText("Grade: -");
                }
            } catch (SQLException e) {
                showAlert("Database Error", "Gagal memuat riwayat submission.", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    void onTestButtonClick(ActionEvent event) {
        if (currentAssignment == null) {
            showAlert("Warning", "No Assignment Selected", "Please select an assignment to test your query.");
            return;
        }

        String userAnswer = userAnswerArea.getText();
        if (userAnswer.isEmpty()) {
            showAlert("Warning", "Empty Query", "Please enter your SQL query in the answer area.");
            return;
        }

        Stage stage = new Stage();
        stage.setTitle("Query Results");
        TableView<ArrayList<String>> tableView = new TableView<>();
        ObservableList<ArrayList<String>> data = FXCollections.observableArrayList();
        ArrayList<String> headers = new ArrayList<>();

        try (Connection conn = GradingDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(userAnswer)) { // Gunakan userAnswer

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                final int columnIndex = i - 1;
                String headerText = metaData.getColumnLabel(i);
                headers.add(headerText);
                TableColumn<ArrayList<String>, String> column = new TableColumn<>(headerText);
                column.setCellValueFactory(cellData -> {
                    ArrayList<String> rowData = cellData.getValue();
                    return (rowData != null && columnIndex < rowData.size()) ? new SimpleStringProperty(rowData.get(columnIndex)) : new SimpleStringProperty("");
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
                showAlert("Query Results", null, "The query executed successfully but returned no data.");
                return;
            }

            tableView.setItems(data);
            StackPane root = new StackPane();
            root.getChildren().add(tableView);
            Scene scene = new Scene(root, 800, 600); // Ukuran pop-up untuk tabel
            stage.setScene(scene);
            stage.show();

        } catch (SQLException e) {
            showAlert("Database Error", "Failed to execute your query.", e.getMessage());
            e.printStackTrace(); // Cetak stack trace untuk debug
        }
    }


    @FXML
    void onSubmitButtonClick() {
        if (currentAssignment == null) {
            showAlert("Peringatan", "Tugas Belum Dipilih", "Silakan pilih tugas terlebih dahulu.");
            return;
        }
        if (currentUser == null) {
            showAlert("Error", "Pengguna Tidak Dikenal", "Tidak dapat mengirimkan jawaban. Informasi pengguna tidak tersedia.");
            return;
        }

        String userAnswer = userAnswerArea.getText().trim();
        if (userAnswer.isEmpty()) {
            showAlert("Peringatan", "Jawaban Kosong", "Silakan masukkan jawaban SQL Anda untuk dikirimkan.");
            return;
        }

        String answerKey = currentAssignment.getAnswerKey();

        // Grading Logic Sederhana
        int grade = 0;
        try (Connection connGrader = GradingDataSource.getConnection()) {
            // Periksa apakah query user dapat dieksekusi
            try (PreparedStatement stmtUser = connGrader.prepareStatement(userAnswer);
                 ResultSet rsUser = stmtUser.executeQuery()) {

                // Periksa apakah kunci jawaban dapat dieksekusi
                try (PreparedStatement stmtKey = connGrader.prepareStatement(answerKey);
                     ResultSet rsKey = stmtKey.executeQuery()) {

                    // Bandingkan hasil (ini masih sangat sederhana, idealnya perlu perbandingan yang lebih kompleks)
                    if (rsUser.getMetaData().getColumnCount() == rsKey.getMetaData().getColumnCount()) {
                        boolean sameContent = true;
                        List<List<Object>> userResults = new ArrayList<>();
                        List<List<Object>> keyResults = new ArrayList<>();

                        // Ambil hasil user
                        while(rsUser.next()) {
                            List<Object> row = new ArrayList<>();
                            for(int i = 1; i <= rsUser.getMetaData().getColumnCount(); i++) {
                                row.add(rsUser.getObject(i));
                            }
                            userResults.add(row);
                        }

                        // Ambil hasil kunci jawaban
                        while(rsKey.next()) {
                            List<Object> row = new ArrayList<>();
                            for(int i = 1; i <= rsKey.getMetaData().getColumnCount(); i++) {
                                row.add(rsKey.getObject(i));
                            }
                            keyResults.add(row);
                        }

                        // Bandingkan ukuran
                        if (userResults.size() != keyResults.size()) {
                            sameContent = false;
                        } else {
                            // Bandingkan isi (order-dependent)
                            for (int i = 0; i < userResults.size(); i++) {
                                if (!userResults.get(i).equals(keyResults.get(i))) {
                                    sameContent = false;
                                    break;
                                }
                            }
                        }

                        if (sameContent) {
                            grade = 100;
                        } else {
                            grade = 50; // Jika konten tidak sama
                        }
                    } else {
                        grade = 50; // Salah jika jumlah kolom tidak cocok
                    }

                } catch (SQLException e) {
                    showAlert("SQL Error (Kunci Jawaban)", "Gagal mengeksekusi kunci jawaban. Silakan hubungi admin.", e.getMessage());
                    grade = 0; // Jika kunci jawaban tidak bisa dieksekusi, anggap salah
                }
            } catch (SQLException e) {
                showAlert("SQL Error (Jawaban Anda)", "Gagal mengeksekusi jawaban Anda. Pastikan syntax SQL benar.", e.getMessage());
                grade = 0; // Jika jawaban user tidak bisa dieksekusi, anggap salah
            }

            // Simpan submission ke database sql-tester
            try (Connection connMain = MainDataSource.getConnection()) {
                String insertSubmissionQuery = "INSERT INTO public.submissions (user_id, assignment_id, submitted_query, grade_obtained, submission_timestamp) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement insertStmt = connMain.prepareStatement(insertSubmissionQuery);
                insertStmt.setLong(1, currentUser.getId());
                insertStmt.setLong(2, currentAssignment.getId());
                insertStmt.setString(3, userAnswer);
                insertStmt.setInt(4, grade);
                insertStmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                insertStmt.executeUpdate();

                gradeLabel.setText("Grade: " + grade);
                if (notificationLabel != null) { // Cek null sebelum menggunakan
                    notificationLabel.setText("Jawaban berhasil dikirimkan. Grade: " + grade);
                    notificationLabel.setStyle(grade == 100 ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
                }


                loadSubmissionHistory(currentAssignment.getId()); // Perbarui riwayat
            } catch (SQLException mainDbError) {
                showAlert("Database Error (Main)", "Gagal menyimpan submission.", mainDbError.getMessage());
                mainDbError.printStackTrace();
            }

        } catch (SQLException connGraderError) {
            showAlert("Database Error (Grader)", "Gagal terhubung ke database grader.", connGraderError.getMessage());
            connGraderError.printStackTrace();
            if (notificationLabel != null) { // Cek null sebelum menggunakan
                notificationLabel.setText("Error koneksi grader: " + connGraderError.getMessage());
                notificationLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }


    @FXML
    void onViewQueryDetailsClick() {
        Submission selectedSubmission = submissionHistoryTable.getSelectionModel().getSelectedItem();
        if (selectedSubmission != null) {
            String submittedQuery = selectedSubmission.getSubmittedQuery();

            // Logika untuk menampilkan hasil query di jendela pop-up yang lebih canggih (tabel)
            StringBuilder resultText = new StringBuilder();
            try (Connection conn = GradingDataSource.getConnection(); // Gunakan GradingDataSource
                 Statement stmt = conn.createStatement()) {

                boolean isResultSet = stmt.execute(submittedQuery);

                if (isResultSet) {
                    ResultSet rs = stmt.getResultSet();
                    resultText.append("Hasil Query:\n\n");

                    ResultSetMetaData rsmd = rs.getMetaData();
                    int columnCount = rsmd.getColumnCount();

                    // Header kolom
                    for (int i = 1; i <= columnCount; i++) {
                        resultText.append(String.format("%-25s", rsmd.getColumnName(i)));
                    }
                    resultText.append("\n----------------------------------------------------------------------------\n");

                    // Data baris
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
                resultAlert.getDialogPane().setPrefWidth(800); // Sesuaikan ukuran pop-up
                resultAlert.getDialogPane().setPrefHeight(600); // Sesuaikan ukuran pop-up
                resultAlert.showAndWait();

            } catch (SQLException e) {
                showAlert("SQL Error", "Gagal mengeksekusi query yang tersimpan. Pastikan database 'oracle_hr' aktif dan tabel tersedia.", e.getMessage());
                e.printStackTrace();
            }

        } else {
            showAlert("Peringatan", "Pilih Submission", "Silakan pilih baris submission dari tabel riwayat untuk melihat detail query.");
        }
    }

    // Metode `calculateGrade` yang sudah ada, pertahankan
    private int calculateGrade(String userAnswerQuery, String correctAnswerQuery) {
        List<String> userResults = executeAndFetch(userAnswerQuery);
        List<String> correctResults = executeAndFetch(correctAnswerQuery);

        if (userResults.equals(correctResults)) {
            return 100;
        } else if (userResults.stream().sorted().collect(Collectors.toList())
                .equals(correctResults.stream().sorted().collect(Collectors.toList())) && !userResults.isEmpty() && !correctResults.isEmpty() && userResults.size() == correctResults.size()) {
            return 50;
        } else {
            return 0;
        }
    }

    // Metode `executeAndFetch` yang sudah ada, pertahankan
    private List<String> executeAndFetch(String sqlQuery) {
        List<String> results = new ArrayList<>();
        try (Connection conn = GradingDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlQuery)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                List<String> rowValues = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    rowValues.add(value != null ? value : "");
                }
                results.add(String.join(",", rowValues)); // Join row values for easier comparison
            }
        } catch (SQLException e) {
            // Log the error, but we'll compare based on potentially empty results
            e.printStackTrace();
        }
        return results;
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}