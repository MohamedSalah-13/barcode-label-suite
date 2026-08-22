package com.mohamed.barcodelabel.print;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface LabelPrinter {
    CompletionStage<PrintResult> print(LabelPrintRequest request);

    /** Requests cancellation of whichever print job this printer currently has in flight, if any. No-op by default. */
    default void cancelCurrent() {}
}

