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
import com.mohamed.barcodelabel.print.PrintOffset;
import com.mohamed.barcodelabel.print.PrintResult;
import org.junit.jupiter.api.Test;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.StreamPrintServiceFactory;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prints for real, to a PostScript stream print service, so the whole path from request to printer job runs
 * without a printer. A scripted PrinterJob covers what a real printer does mid-job: cancel and fail.
 *
 * <p>The template is a 3 x 7 A4 sheet, 21 labels per page: 25 records fill one page and 4 labels of the next.</p>
 */
class Java2DLabelPrinterPrintingTest {
    private final ByteArrayOutputStream postScript = new ByteArrayOutputStream();
    private final PrintService fileService = StreamPrintServiceFactory
            .lookupStreamPrintServiceFactories(DocFlavor.SERVICE_FORMATTED.PAGEABLE, "application/postscript")[0]
            .getPrintService(postScript);
    private final PrinterCatalog catalog = new PrinterCatalog(() -> new PrintService[] {fileService}, () -> fileService);

    @Test
    void aPhysicalOffsetMovesInkByTheSameDistanceOnEveryPrintedPage() throws Exception {
        var request = new LabelPrintRequest(template(), records(2), "Test", 1, 20);
        var original = new SheetPrintable(request, 300);
        var shifted = new SheetPrintable(request.withOffset(new PrintOffset(3.175, -6.35)), 300);
        assertEquals(2, shifted.pageCount());
        for (int page = 0; page < 2; page++) {
            var expected = inkBounds(original, page);
            expected.translate(9, -18); // at 72 printer points per inch
            assertEquals(expected, inkBounds(shifted, page));
        }
    }

    private static java.awt.Rectangle inkBounds(SheetPrintable printable, int page) throws PrinterException {
        var image = new BufferedImage(620, 850, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            assertEquals(Printable.PAGE_EXISTS, printable.print(graphics, new PageFormat(), page));
        } finally {
            graphics.dispose();
        }
        var ink = new java.awt.Rectangle(0, 0, -1, -1);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 255) < 128) ink.add(x, y);
            }
        }
        assertTrue(ink.width > 0 && ink.height > 0);
        return ink;
    }

    @Test
    void printsEveryPlannedPage() throws Exception {
        PrintResult result = print(new LabelPrintRequest(template(), records(25), fileService.getName(), 1, 0));
        assertEquals(new PrintResult(2, 25), result);
        assertEquals(2, pagesInPostScript());
    }

    @Test
    void aPageRangePrintsOnlyThosePagesAndCountsTheirLabels() throws Exception {
        PrintResult result = print(new LabelPrintRequest(template(), records(25), fileService.getName(), 1, 0, 2, 2));
        assertEquals(new PrintResult(1, 4), result);
        assertEquals(1, pagesInPostScript());
    }

    @Test
    void aRangePastTheLastPageStopsAtTheLastPage() throws Exception {
        assertEquals(new PrintResult(2, 25),
                print(new LabelPrintRequest(template(), records(25), fileService.getName(), 1, 0, 1, 99)));
    }

    @Test
    void copiesMultiplyPagesAndLabels() throws Exception {
        assertEquals(new PrintResult(4, 50),
                print(new LabelPrintRequest(template(), records(25), fileService.getName(), 2, 0)));
    }

    @Test
    void aStartSlotMovesLabelsOnToTheNextPage() throws Exception {
        // From the last slot, the first label fills page 1 and the second starts page 2.
        assertEquals(new PrintResult(2, 2),
                print(new LabelPrintRequest(template(), records(2), fileService.getName(), 1, 20)));
    }

    @Test
    void nothingToPrintPrintsNothing() throws Exception {
        assertEquals(new PrintResult(0, 0), print(new LabelPrintRequest(template(), List.of(), fileService.getName(), 1, 0)));
        assertEquals(0, pagesInPostScript());
    }

    @Test
    void aRangeStartingPastTheLastPageIsRejected() {
        Throwable error = failure(new LabelPrintRequest(template(), records(25), fileService.getName(), 1, 0, 3, 3));
        assertInstanceOf(Java2DPrintException.class, error);
        assertTrue(error.getMessage().contains("exceeds the planned page count 2"), error.getMessage());
    }

    @Test
    void printerNamesMatchWhateverTheirCase() throws Exception {
        assertEquals(new PrintResult(1, 1), print(new LabelPrintRequest(template(), records(1),
                fileService.getName().toUpperCase(), 1, 0)));
    }

    @Test
    void theRenderResolutionMustSuitPrinting() {
        assertThrows(IllegalArgumentException.class, () -> new Java2DLabelPrinter(catalog, 100));
        assertThrows(IllegalArgumentException.class, () -> new Java2DLabelPrinter(catalog, 2400));
        assertThrows(IllegalArgumentException.class, () -> new Java2DLabelPrinter(catalog, Double.NaN));
    }

    @Test
    void theCatalogListsTheDefaultPrinterFirstThenByNameOnce() {
        PrintService zebra = service("Zebra ZD420");
        PrintService office = service("office laser");
        PrintService brother = service("Brother QL");
        var printers = new PrinterCatalog(() -> new PrintService[] {zebra, office, brother, zebra}, () -> office)
                .discover();
        assertEquals(List.of(new PrinterDescriptor("office laser", true), new PrinterDescriptor("Brother QL", false),
                new PrinterDescriptor("Zebra ZD420", false)), printers);
        assertEquals(List.of(), new PrinterCatalog(() -> new PrintService[0], () -> null).discover());
    }

    @Test
    void anOverlappingRequestIsRejectedAndANewJobCanStartAfterCancellation() throws Exception {
        var job = new ScriptedJob(1, null);
        var printer = new Java2DLabelPrinter(catalog, 150, () -> job);
        var first = printer.print(new LabelPrintRequest(template(), records(25), fileService.getName(), 1, 0))
                .toCompletableFuture();
        var next = new LabelPrintRequest(template(), records(1), fileService.getName(), 1, 0);
        try {
            assertTrue(job.waiting.await(30, TimeUnit.SECONDS));
            var second = printer.print(next).toCompletableFuture();
            var rejected = assertThrows(ExecutionException.class, () -> second.get(5, TimeUnit.SECONDS));
            assertInstanceOf(IllegalStateException.class, rejected.getCause());
        } finally {
            printer.cancelCurrent();
        }
        var cancelled = assertThrows(ExecutionException.class, () -> first.get(30, TimeUnit.SECONDS));
        assertInstanceOf(PrintCancelledException.class, cancelled.getCause());
        assertEquals(new PrintResult(1, 1), printer.print(next).toCompletableFuture().get(30, TimeUnit.SECONDS));
    }

    @Test
    void cancellingAJobInProgressReportsHowFarItGot() throws Exception {
        ScriptedJob job = new ScriptedJob(1, null);
        Java2DLabelPrinter printer = new Java2DLabelPrinter(catalog, 150, () -> job);
        var future = printer.print(new LabelPrintRequest(template(), records(25), fileService.getName(), 1, 0))
                .toCompletableFuture();
        assertTrue(job.waiting.await(30, TimeUnit.SECONDS), "the job reached its second page");
        printer.cancelCurrent();

        ExecutionException wrapped = assertThrows(ExecutionException.class, () -> future.get(30, TimeUnit.SECONDS));
        PrintCancelledException cancelled = assertInstanceOf(PrintCancelledException.class, wrapped.getCause());
        assertEquals("Print job was cancelled after 1 of 2 page(s)", cancelled.getMessage());
        printer.cancelCurrent(); // the finished job is gone; nothing to cancel
    }

    @Test
    void aPrinterFailureMidJobReportsTheCompletedPages() {
        ScriptedJob job = new ScriptedJob(1, new PrinterException("paper jam"));
        Java2DLabelPrinter printer = new Java2DLabelPrinter(catalog, 150, () -> job);
        var future = printer.print(new LabelPrintRequest(template(), records(25), fileService.getName(), 3, 0))
                .toCompletableFuture();

        ExecutionException wrapped = assertThrows(ExecutionException.class, () -> future.get(30, TimeUnit.SECONDS));
        PrintJobFailedException failed = assertInstanceOf(PrintJobFailedException.class, wrapped.getCause());
        assertEquals(1, failed.pagesCompleted());
        assertEquals(2, failed.totalPages());
        assertTrue(failed.getMessage().endsWith("paper jam"), failed.getMessage());
        assertEquals(3, job.getCopies(), "the job was set up before it failed");
        assertEquals("Test sheet", job.getJobName());
    }

    private PrintResult print(LabelPrintRequest request) throws Exception {
        return new Java2DLabelPrinter(catalog, 150).print(request).toCompletableFuture().get(60, TimeUnit.SECONDS);
    }

    private Throwable failure(LabelPrintRequest request) {
        var future = new Java2DLabelPrinter(catalog, 150).print(request).toCompletableFuture();
        return assertThrows(ExecutionException.class, () -> future.get(60, TimeUnit.SECONDS)).getCause();
    }

    private int pagesInPostScript() {
        return postScript.toString().split("%%Page:", -1).length - 1;
    }

    private static PrintService service(String name) {
        return (PrintService) Proxy.newProxyInstance(PrintService.class.getClassLoader(),
                new Class<?>[] {PrintService.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getName", "toString" -> name;
                    case "hashCode" -> name.hashCode();
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
    }

    private static LabelTemplate template() {
        var layout = new SheetLayout(3, 7, new DimensionsMm(63.5, 38.1), 2.5, 0);
        var page = PageDefinition.a4Sheet(layout, new InsetsMm(15.15, 7.25, 15.15, 7.25));
        var marker = new ShapeElement(ElementFrame.create("Marker", new RectMm(1, 1, 10, 10), 0),
                ShapeType.RECTANGLE, "#000000", "#000000", 0.1);
        return LabelTemplate.create("Test sheet", page, List.of(marker));
    }

    private static List<Map<String, ?>> records(int count) {
        return java.util.stream.IntStream.range(0, count)
                .<Map<String, ?>>mapToObj(index -> Map.of("index", index))
                .toList();
    }

    /**
     * Renders pages like a printer until {@code stopBefore}, then either fails with {@code failure} or, when that
     * is null, waits to be cancelled and aborts as a real job does.
     */
    private static final class ScriptedJob extends PrinterJob {
        private final int stopBefore;
        private final PrinterException failure;
        private final CountDownLatch waiting = new CountDownLatch(1);
        private volatile boolean cancelled;
        private Pageable pageable;
        private int copies = 1;
        private String jobName = "";

        ScriptedJob(int stopBefore, PrinterException failure) {
            this.stopBefore = stopBefore;
            this.failure = failure;
        }

        @Override
        public void print() throws PrinterException {
            for (int page = 0; page < pageable.getNumberOfPages(); page++) {
                if (page == stopBefore) {
                    if (failure != null) throw failure;
                    waiting.countDown();
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                    while (!cancelled && System.nanoTime() < deadline) Thread.onSpinWait();
                    throw new PrinterAbortException();
                }
                BufferedImage sheet = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
                Printable printable = pageable.getPrintable(page);
                printable.print(sheet.createGraphics(), pageable.getPageFormat(page), page);
            }
        }

        @Override public void setPrintService(PrintService service) {}
        @Override public void setPrintable(Printable painter) {}
        @Override public void setPrintable(Printable painter, PageFormat format) {}
        @Override public void setPageable(Pageable document) { pageable = document; }
        @Override public boolean printDialog() { return false; }
        @Override public PageFormat pageDialog(PageFormat page) { return page; }
        @Override public PageFormat defaultPage(PageFormat page) { return page; }
        @Override public PageFormat validatePage(PageFormat page) { return page; }
        @Override public void setCopies(int copies) { this.copies = copies; }
        @Override public int getCopies() { return copies; }
        @Override public String getUserName() { return "test"; }
        @Override public void setJobName(String jobName) { this.jobName = jobName; }
        @Override public String getJobName() { return jobName; }
        @Override public void cancel() { cancelled = true; }
        @Override public boolean isCancelled() { return cancelled; }
    }
}
