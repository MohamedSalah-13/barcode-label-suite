package com.mohamed.barcodelabel.print.java2d;

import com.mohamed.barcodelabel.model.page.PageOrientation;
import com.mohamed.barcodelabel.model.unit.UnitConversions;
import com.mohamed.barcodelabel.print.LabelPrintRequest;
import com.mohamed.barcodelabel.print.LabelPrinter;
import com.mohamed.barcodelabel.print.PrintResult;

import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class Java2DLabelPrinter implements LabelPrinter {
    private final PrinterCatalog printerCatalog;
    private final double renderDpi;

    public Java2DLabelPrinter() {
        this(new PrinterCatalog(), 300);
    }

    public Java2DLabelPrinter(PrinterCatalog printerCatalog, double renderDpi) {
        this.printerCatalog = Objects.requireNonNull(printerCatalog, "printerCatalog");
        if (!Double.isFinite(renderDpi) || renderDpi < 150 || renderDpi > 1200) {
            throw new IllegalArgumentException("renderDpi must be between 150 and 1200");
        }
        this.renderDpi = renderDpi;
    }

    @Override
    public CompletionStage<PrintResult> print(LabelPrintRequest request) {
        Objects.requireNonNull(request, "request");
        CompletableFuture<PrintResult> future = new CompletableFuture<>();
        Thread.ofVirtual().name("barcode-label-print").start(() -> {
            try {
                future.complete(printBlocking(request));
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    private PrintResult printBlocking(LabelPrintRequest request) {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(printerCatalog.findRequired(request.printerName()));
            SheetPrintable printable = new SheetPrintable(request, renderDpi);
            if (printable.pageCount() == 0) return new PrintResult(0, 0);

            PageFormat format = job.validatePage(createPageFormat(request));
            Book book = new Book();
            book.append(printable, format, printable.pageCount());
            job.setPageable(book);
            job.setCopies(request.copies());
            job.setJobName(request.template().name());
            job.print();
            return new PrintResult(printable.pageCount() * request.copies(), request.records().size() * request.copies());
        } catch (Exception exception) {
            throw new Java2DPrintException("Printing failed: " + exception.getMessage(), exception);
        }
    }

    private static PageFormat createPageFormat(LabelPrintRequest request) {
        var page = request.template().page();
        double width = UnitConversions.mmToPoints(page.pageSize().width());
        double height = UnitConversions.mmToPoints(page.pageSize().height());
        Paper paper = new Paper();
        paper.setSize(width, height);
        paper.setImageableArea(0, 0, width, height);
        PageFormat format = new PageFormat();
        format.setPaper(paper);
        format.setOrientation(page.orientation() == PageOrientation.LANDSCAPE ? PageFormat.LANDSCAPE : PageFormat.PORTRAIT);
        return format;
    }
}

