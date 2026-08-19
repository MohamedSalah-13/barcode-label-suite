package com.mohamed.barcodelabel.print.java2d;

import java.util.Objects;

public record PrinterDescriptor(String name, boolean defaultPrinter) implements Comparable<PrinterDescriptor> {
    public PrinterDescriptor {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) throw new IllegalArgumentException("printer name must not be blank");
    }

    @Override
    public int compareTo(PrinterDescriptor other) {
        if (defaultPrinter != other.defaultPrinter) return defaultPrinter ? -1 : 1;
        return name.compareToIgnoreCase(other.name);
    }

    @Override
    public String toString() {
        return defaultPrinter ? name + " (Default)" : name;
    }
}

