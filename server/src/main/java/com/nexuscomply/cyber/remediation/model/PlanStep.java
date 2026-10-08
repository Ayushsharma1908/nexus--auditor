package com.nexuscomply.cyber.remediation.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanStep {

    private int order;
    private String action = "EXECUTE_COMMAND";
    private String command;
    private String description;

    public PlanStep() {}

    public PlanStep(int order, String action, String command, String description) {
        this.order = order;
        this.action = action;
        this.command = command;
        this.description = description;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
