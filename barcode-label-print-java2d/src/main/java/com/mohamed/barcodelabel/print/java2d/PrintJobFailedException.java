package com.mohamed.barcodelabel.print.java2d;

/** Thrown when a print job reaches the printer but fails partway through. Reports how many pages actually completed. */
public final class PrintJobFailedException extends Java2DPrintException {
    private final int pagesCompleted;
    private final int totalPages;

    public PrintJobFailedException(String message, Throwable cause, int pagesCompleted, int totalPages) {
        super(message, cause);
        this.pagesCompleted = pagesCompleted;
        this.totalPages = totalPages;
    }

    public int pagesCompleted() { return pagesCompleted; }
    public int totalPages() { return totalPages; }
}
