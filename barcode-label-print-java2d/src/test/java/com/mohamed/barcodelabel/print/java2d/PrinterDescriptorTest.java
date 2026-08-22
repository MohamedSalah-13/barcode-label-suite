package com.mohamed.barcodelabel.print.java2d;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrinterDescriptorTest {
    @Test
    void defaultPrinterSortsFirstThenAlphabeticalIgnoringCase() {
        var sorted = List.of(
                new PrinterDescriptor("zebra", false),
                new PrinterDescriptor("Alpha", false),
                new PrinterDescriptor("Office", true)
        ).stream().sorted().toList();

        assertEquals(List.of("Office", "Alpha", "zebra"), sorted.stream().map(PrinterDescriptor::name).toList());
    }

    @Test
    void toStringMarksTheDefaultPrinter() {
        assertEquals("HP LaserJet (Default)", new PrinterDescriptor("HP LaserJet", true).toString());
        assertEquals("HP LaserJet", new PrinterDescriptor("HP LaserJet", false).toString());
    }
}
