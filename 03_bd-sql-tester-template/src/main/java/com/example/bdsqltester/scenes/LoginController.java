package com.example.bdsqltester.scenes;

import com.example.bdsqltester.HelloApplication;
import com.example.bdsqltester.datasources.MainDataSource;
import com.example.bdsqltester.scenes.user.UserController;
import com.example.bdsqltester.dtos.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.*;

public class LoginController {

    @FXML
    private PasswordField passwordField;

    @FXML
    private ChoiceBox<String> selectRole;

    @FXML
    private TextField usernameField;

    private User getUserByUsername(String username) throws SQLException {
        try (Connection c = MainDataSource.getConnection()) {
            PreparedStatement stmt = c.prepareStatement("SELECT id, username, password, role FROM users WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User(rs.getString("username"), rs.getString("password"), rs.getString("role"));
                user.setId(rs.getLong("id"));
                return user;
            }
            return null;
        }
    }

    boolean verifyCredentials(String username, String password, String role) throws SQLException {
        try (Connection c = MainDataSource.getConnection()) {
            PreparedStatement stmt = c.prepareStatement("SELECT password FROM users WHERE username = ? AND role = ?");
            stmt.setString(1, username);
            stmt.setString(2, role.toLowerCase());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String dbPassword = rs.getString("password");
                return dbPassword.equals(password);
            }
            return false;
        }
    }

    @FXML
    void initialize() {
        selectRole.getItems().addAll("Admin", "User");
        selectRole.setValue("User");
    }

    @FXML
    void onLoginClick(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String role = selectRole.getValue();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Login Gagal", "Kolom Kosong", "Username dan Password tidak boleh kosong.");
            return;
        }


        try {
            if (verifyCredentials(username, password, role)) {
                showAlert(Alert.AlertType.INFORMATION, "Login Berhasil", "Selamat Datang!", "Anda berhasil login sebagai " + username + ".");

                HelloApplication app = HelloApplication.getApplicationInstance();
                FXMLLoader loader;
                Scene scene;

                if (role.equals("Admin")) {
                    app.getPrimaryStage().setTitle("Admin View");
                    loader = new FXMLLoader(HelloApplication.class.getResource("admin-view.fxml"));
                    scene = new Scene(loader.load());
                    app.getPrimaryStage().setScene(scene);
                } else {
                    app.getPrimaryStage().setTitle("User View");
                    loader = new FXMLLoader(HelloApplication.class.getResource("user-view.fxml"));
                    scene = new Scene(loader.load());
                    app.getPrimaryStage().setScene(scene);
                    UserController userController = loader.getController();
                    try {
                        User loggedInUser = getUserByUsername(username);
                        if (loggedInUser != null) {
                            System.out.println("Logged in user ID in LoginController: " + loggedInUser.getId());
                            userController.setCurrentUser(loggedInUser);
                            app.getPrimaryStage().setTitle("User View");
                            app.getPrimaryStage().setScene(scene);
                        } else {
                            showAlert("Error", "Gagal", "Gagal mendapatkan informasi pengguna setelah login.");
                        }
                    } catch (SQLException e) {
                        showAlert("Database Error", "Gagal mendapatkan informasi pengguna.", e.getMessage());
                    }
                }
            } else {
                showAlert("Login Gagal", "Kredensial Tidak Valid", "Silakan periksa Username dan Password Anda.");
                passwordField.clear();
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Koneksi Database Gagal", "Tidak dapat terhubung ke database. Silakan coba lagi nanti.\nDetail: " + e.getMessage());
        } catch (IOException e) {
            showAlert("Error Aplikasi", "Gagal Memuat Tampilan", "Tidak dapat memuat tampilan untuk peran ini.\nDetail: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void showAlert(String title, String header, String content) {
        showAlert(Alert.AlertType.ERROR, title, header, content);
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}