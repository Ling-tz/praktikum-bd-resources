package com.example.bdsqltester.dtos;

public class Grade {
    private Long userId;
    private Long assignmentId;
    private Double gradeValue;
    private String userName;
    private String assignmentName;

    public Grade(Long userId, Long assignmentId, Double gradeValue, String userName, String assignmentName) {
        this.userId = userId;
        this.assignmentId = assignmentId;
        this.gradeValue = gradeValue;
        this.userName = userName;
        this.assignmentName = assignmentName;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public Double getGradeValue() {
        return gradeValue;
    }

    public String getUserName() {
        return userName;
    }

    public String getAssignmentName() {
        return assignmentName;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public void setGradeValue(Double gradeValue) {
        this.gradeValue = gradeValue;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setAssignmentName(String assignmentName) {
        this.assignmentName = assignmentName;
    }
}