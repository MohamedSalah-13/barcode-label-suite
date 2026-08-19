package com.mohamed.barcodelabel.print;

public record PrintResult(int pagesPrinted, int labelsPrinted) {
    public PrintResult {
        if (pagesPrinted < 0 || labelsPrinted < 0) {
            throw new IllegalArgumentException("printed counts must be non-negative");
        }
    }
}

