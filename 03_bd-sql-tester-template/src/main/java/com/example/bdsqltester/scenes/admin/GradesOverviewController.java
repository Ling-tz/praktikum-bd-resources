package com.example.bdsqltester.scenes.admin;

import com.example.bdsqltester.dtos.Grade;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class GradesOverviewController {

    @FXML
    private TableView<Grade> gradesTable;
    @FXML
    private TableColumn<Grade, String> assignmentNameColumn;
    @FXML
    private TableColumn<Grade, String> userNameColumn;
    @FXML
    private TableColumn<Grade, Double> finalGradeColumn;

    private final ObservableList<Grade> gradesData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        assignmentNameColumn.setCellValueFactory(new PropertyValueFactory<>("assignmentName"));
        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        finalGradeColumn.setCellValueFactory(new PropertyValueFactory<>("gradeValue"));

        gradesTable.setItems(gradesData);
    }

    public void setGradesData(ObservableList<Grade> gradesList) {
        gradesData.setAll(gradesList);
    }
}