package com.mohamed.barcodelabel.print.java2d;

/** Thrown when the requested printer name does not match any discovered {@code PrintService}. */
public final class PrinterNotFoundException extends Java2DPrintException {
    public PrinterNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
