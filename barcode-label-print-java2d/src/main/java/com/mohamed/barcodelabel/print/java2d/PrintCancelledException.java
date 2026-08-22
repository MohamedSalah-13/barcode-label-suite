package com.mohamed.barcodelabel.print.java2d;

/** Thrown when a print job is aborted via {@link Java2DLabelPrinter#cancelCurrent()}. */
public final class PrintCancelledException extends Java2DPrintException {
    public PrintCancelledException(String message, Throwable cause) {
        super(message, cause);
    }
}
