package com.example.bdsqltester.dtos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Submission {
    private Long id;
    private Long userId;
    private Long assignmentId;
    private String submittedQuery;
    private Integer gradeObtained;
    private String submissionTimestamp;
    private String userName;

    public Submission(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.userId = rs.getLong("user_id");
        this.assignmentId = rs.getLong("assignment_id");
        this.submittedQuery = rs.getString("submitted_query");
        this.gradeObtained = rs.getInt("grade_obtained");
        LocalDateTime timestamp = rs.getTimestamp("submission_timestamp").toLocalDateTime();
        this.submissionTimestamp = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getAssignmentId() { return assignmentId; }
    public String getSubmittedQuery() { return submittedQuery; }
    public Integer getGradeObtained() { return gradeObtained; }
    public String getSubmissionTimestamp() { return submissionTimestamp; }
    public String getUserName() { return userName; }

    public void setUserName(String userName) {
        this.userName = userName;
    }

}