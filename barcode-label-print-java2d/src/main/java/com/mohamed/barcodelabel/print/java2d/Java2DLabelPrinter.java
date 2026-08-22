package com.mohamed.barcodelabel.print.java2d;

import com.mohamed.barcodelabel.model.page.PageOrientation;
import com.mohamed.barcodelabel.model.unit.UnitConversions;
import com.mohamed.barcodelabel.print.LabelPrintRequest;
import com.mohamed.barcodelabel.print.LabelPrinter;
import com.mohamed.barcodelabel.print.PrintResult;

import java.awt.Graphics;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

public final class Java2DLabelPrinter implements LabelPrinter {
    private final PrinterCatalog printerCatalog;
    private final double renderDpi;
    private volatile PrinterJob currentJob;

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

    @Override
    public void cancelCurrent() {
        PrinterJob job = currentJob;
        if (job != null) job.cancel();
    }

    private PrintResult printBlocking(LabelPrintRequest request) {
        PrinterJob job = PrinterJob.getPrinterJob();
        currentJob = job;
        try {
            try {
                job.setPrintService(printerCatalog.findRequired(request.printerName()));
            } catch (IllegalArgumentException exception) {
                throw new PrinterNotFoundException(exception.getMessage(), exception);
            }
            SheetPrintable printable = new SheetPrintable(request, renderDpi);
            if (printable.pageCount() == 0) return new PrintResult(0, 0);

            CountingPrintable counting = new CountingPrintable(printable);
            PageFormat format = job.validatePage(createPageFormat(request));
            Book book = new Book();
            book.append(counting, format, printable.pageCount());
            job.setPageable(book);
            job.setCopies(request.copies());
            job.setJobName(request.template().name());
            try {
                job.print();
            } catch (PrinterAbortException exception) {
                throw new PrintCancelledException("Print job was cancelled after "
                        + counting.pagesCompleted() + " of " + printable.pageCount() + " page(s)", exception);
            } catch (PrinterException exception) {
                throw new PrintJobFailedException("Print job failed after " + counting.pagesCompleted()
                        + " of " + printable.pageCount() + " page(s): " + exception.getMessage(), exception,
                        counting.pagesCompleted(), printable.pageCount());
            }
            return new PrintResult(printable.pageCount() * request.copies(), request.records().size() * request.copies());
        } catch (Java2DPrintException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new Java2DPrintException("Printing failed: " + exception.getMessage(), exception);
        } finally {
            currentJob = null;
        }
    }

    /** Tracks how many pages the spooler actually rendered before a mid-job failure. */
    private static final class CountingPrintable implements Printable {
        private final Printable delegate;
        private final AtomicInteger pagesCompleted = new AtomicInteger();

        CountingPrintable(Printable delegate) {
            this.delegate = delegate;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            int result = delegate.print(graphics, pageFormat, pageIndex);
            if (result == Printable.PAGE_EXISTS) pagesCompleted.set(pageIndex + 1);
            return result;
        }

        int pagesCompleted() { return pagesCompleted.get(); }
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

