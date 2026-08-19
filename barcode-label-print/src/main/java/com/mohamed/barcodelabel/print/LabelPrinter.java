package com.mohamed.barcodelabel.print;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface LabelPrinter {
    CompletionStage<PrintResult> print(LabelPrintRequest request);
}

