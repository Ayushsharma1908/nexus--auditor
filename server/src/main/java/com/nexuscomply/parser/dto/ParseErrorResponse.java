package com.nexuscomply.parser.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexuscomply.parser.model.ParseError;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParseErrorResponse {
    private String jobId;
    private List<ParseError> errors = new ArrayList<>();
    private Integer totalErrors;

    public ParseErrorResponse() {}

    public ParseErrorResponse(String jobId, List<ParseError> errors) {
        this.jobId = jobId;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.totalErrors = this.errors.size();
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public List<ParseError> getErrors() {
        return errors;
    }

    public void setErrors(List<ParseError> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
        this.totalErrors = this.errors.size();
    }

    public Integer getTotalErrors() {
        return totalErrors;
    }

    public void setTotalErrors(Integer totalErrors) {
        this.totalErrors = totalErrors;
    }
}
