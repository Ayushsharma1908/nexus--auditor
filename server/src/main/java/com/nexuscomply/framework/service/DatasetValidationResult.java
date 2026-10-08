package com.nexuscomply.framework.service;

import java.util.ArrayList;
import java.util.List;

public class DatasetValidationResult {
    private boolean valid = true;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private int insertedCount = 0;
    private int updatedCount = 0;
    private int rejectedCount = 0;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
        this.rejectedCount++;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public void incrementInserted(int count) {
        this.insertedCount += count;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void incrementUpdated(int count) {
        this.updatedCount += count;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }
}
