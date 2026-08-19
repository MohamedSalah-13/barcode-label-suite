package com.mohamed.barcodelabel.print.java2d;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.util.Arrays;
import java.util.List;

public final class PrinterCatalog {
    public List<PrinterDescriptor> discover() {
        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
        String defaultName = defaultService == null ? null : defaultService.getName();
        return Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null))
                .map(service -> new PrinterDescriptor(service.getName(), service.getName().equals(defaultName)))
                .sorted()
                .toList();
    }

    PrintService findRequired(String name) {
        return Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null))
                .filter(service -> service.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Printer not found: " + name));
    }
}

