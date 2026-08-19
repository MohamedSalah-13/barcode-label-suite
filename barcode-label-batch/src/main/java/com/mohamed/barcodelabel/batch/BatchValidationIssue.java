package com.mohamed.barcodelabel.batch;

import java.util.Objects;

public record BatchValidationIssue(ValidationSeverity severity, int sourceRow, String field, String message) {
    public BatchValidationIssue {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(message, "message");
    }
}
