package com.mohamed.barcodelabel.print.java2d;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.ShapeType;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import com.mohamed.barcodelabel.print.LabelPrintRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Java2DLabelPrinterTest {
    @Test
    void printingToAnUnknownPrinterFailsWithPrinterNotFoundException() {
        var printer = new Java2DLabelPrinter();
        var request = new LabelPrintRequest(template(), records(3), "Definitely Not A Real Printer 12345", 1, 0);

        var future = printer.print(request).toCompletableFuture();
        ExecutionException wrapped = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(PrinterNotFoundException.class, wrapped.getCause());
    }

    @Test
    void cancelCurrentWithoutAnActiveJobIsANoOp() {
        new Java2DLabelPrinter().cancelCurrent();
    }

    private static LabelTemplate template() {
        var layout = new SheetLayout(3, 7, new DimensionsMm(63.5, 38.1), 2.5, 0);
        var page = PageDefinition.a4Sheet(layout, new InsetsMm(15.15, 7.25, 15.15, 7.25));
        var marker = new ShapeElement(
                ElementFrame.create("Marker", new RectMm(1, 1, 10, 10), 0),
                ShapeType.RECTANGLE, "#000000", "#000000", 0.1);
        return LabelTemplate.create("Test sheet", page, List.of(marker));
    }

    private static List<Map<String, ?>> records(int count) {
        return java.util.stream.IntStream.range(0, count)
                .<Map<String, ?>>mapToObj(index -> Map.of("index", index))
                .toList();
    }
}
