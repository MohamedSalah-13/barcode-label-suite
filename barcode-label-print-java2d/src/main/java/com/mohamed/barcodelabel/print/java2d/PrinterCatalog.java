package com.mohamed.barcodelabel.print.java2d;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class PrinterCatalog {
    private final Supplier<PrintService[]> services;
    private final Supplier<PrintService> defaultService;

    public PrinterCatalog() {
        this(() -> PrintServiceLookup.lookupPrintServices(null, null), PrintServiceLookup::lookupDefaultPrintService);
    }

    /** For tests: the printers to offer instead of the system's. */
    PrinterCatalog(Supplier<PrintService[]> services, Supplier<PrintService> defaultService) {
        this.services = Objects.requireNonNull(services, "services");
        this.defaultService = Objects.requireNonNull(defaultService, "defaultService");
    }

    public List<PrinterDescriptor> discover() {
        PrintService defaultPrinter = defaultService.get();
        String defaultName = defaultPrinter == null ? null : defaultPrinter.getName();
        return Arrays.stream(services.get())
                .map(service -> new PrinterDescriptor(service.getName(), service.getName().equals(defaultName)))
                .distinct()
                .sorted()
                .toList();
    }

    PrintService findRequired(String name) {
        return Arrays.stream(services.get())
                .filter(service -> service.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Printer not found: " + name));
    }
}

