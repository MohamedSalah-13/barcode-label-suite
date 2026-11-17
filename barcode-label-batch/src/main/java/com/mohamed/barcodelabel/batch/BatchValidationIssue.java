package com.mohamed.barcodelabel.batch;

import java.util.Objects;

/**
 * One problem found while preparing a batch. {@code message} is an English description for logs and library
 * users; {@code kind} and {@code value} let a UI describe the problem in its own language.
 */
public record BatchValidationIssue(ValidationSeverity severity, BatchIssueKind kind, int sourceRow, String field,
                                   String value, String message) {
    public BatchValidationIssue {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(message, "message");
    }

    /** An issue of kind {@link BatchIssueKind#OTHER}, described only by its message. */
    public BatchValidationIssue(ValidationSeverity severity, int sourceRow, String field, String message) {
        this(severity, BatchIssueKind.OTHER, sourceRow, field, "", message);
    }
}
