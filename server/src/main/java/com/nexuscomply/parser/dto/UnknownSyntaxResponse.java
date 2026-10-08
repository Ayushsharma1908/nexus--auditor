package com.nexuscomply.parser.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexuscomply.parser.model.UnknownSyntaxItem;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnknownSyntaxResponse {
    private String jobId;
    private List<UnknownSyntaxItem> unknowns = new ArrayList<>();
    private Integer totalUnknowns;

    public UnknownSyntaxResponse() {}

    public UnknownSyntaxResponse(String jobId, List<UnknownSyntaxItem> unknowns) {
        this.jobId = jobId;
        this.unknowns = unknowns != null ? unknowns : new ArrayList<>();
        this.totalUnknowns = this.unknowns.size();
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public List<UnknownSyntaxItem> getUnknowns() {
        return unknowns;
    }

    public void setUnknowns(List<UnknownSyntaxItem> unknowns) {
        this.unknowns = unknowns != null ? unknowns : new ArrayList<>();
        this.totalUnknowns = this.unknowns.size();
    }

    public Integer getTotalUnknowns() {
        return totalUnknowns;
    }

    public void setTotalUnknowns(Integer totalUnknowns) {
        this.totalUnknowns = totalUnknowns;
    }
}
