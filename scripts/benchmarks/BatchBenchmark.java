package com.mohamed.barcodelabel.fx.batch;

import com.mohamed.barcodelabel.batch.*;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.*;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import com.mohamed.barcodelabel.workflow.print.UniversalPagePlanner;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.function.Supplier;

/** Standalone diagnostic harness: invokes production methods, never starts JavaFX or prints. */
public final class BatchBenchmark {
    static final List<Object> LIVE = new ArrayList<>();
    static final com.sun.management.ThreadMXBean THREADS =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    static final Runtime RUNTIME = Runtime.getRuntime();
    static int rows, quantity, unique, fork;
    static long heap() { return RUNTIME.totalMemory() - RUNTIME.freeMemory(); }
    static void collect() throws Exception { System.gc(); Thread.sleep(150); System.gc(); Thread.sleep(150); }
    static long gcCount() { return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b -> Math.max(0,b.getCollectionCount())).sum(); }
    static long gcTime() { return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b -> Math.max(0,b.getCollectionTime())).sum(); }
    static <T> T measure(String phase, Supplier<T> action) throws Exception {
        jdk.jfr.Recording recording = null;
        if (phase.equals(System.getProperty("benchmark.jfr.phase"))) {
            recording = new jdk.jfr.Recording(jdk.jfr.Configuration.getConfiguration("profile"));
            recording.start();
        }
        collect();
        long before = heap(), gc = gcCount(), gcMs = gcTime();
        long allocated = THREADS.getThreadAllocatedBytes(Thread.currentThread().threadId());
        long start = System.nanoTime();
        T value = action.get();
        long elapsed = System.nanoTime() - start;
        long bytes = THREADS.getThreadAllocatedBytes(Thread.currentThread().threadId()) - allocated;
        long collections = gcCount()-gc, pause = gcTime()-gcMs;
        if (recording != null) {
            recording.stop();
            recording.dump(java.nio.file.Path.of(System.getProperty("benchmark.jfr.file")));
            recording.close();
        }
        LIVE.add(value);
        collect();
        // Capture before printf/autoboxing can reserve another TLAB and inflate Runtime's heap reading.
        long after = heap();
        System.out.printf(Locale.ROOT,
            "{\"rows\":%d,\"quantity\":%d,\"uniqueBarcodes\":%d,\"fork\":%d,\"phase\":\"%s\",\"ms\":%.3f,\"allocatedBytes\":%d,\"retainedDeltaBytes\":%d,\"liveHeapBytes\":%d,\"gcCount\":%d,\"gcMs\":%d}%n",
            rows,quantity,unique,fork,phase,elapsed/1e6,bytes,after-before,after,collections,pause);
        return value;
    }
    static CsvDataset data(int n, int q, int distinct) {
        var records = new ArrayList<Map<String,String>>(n);
        for (int i=0;i<n;i++) records.add(Map.of("Name", "منتج تجريبي "+i,
            "Code", "SKU-"+(100000+i%distinct), "Retail Price", "12.50", "Qty", Integer.toString(q)));
        return new CsvDataset(List.of("Name","Code","Retail Price","Qty"),records,',');
    }
    static LabelTemplate template() {
        var page = new PaperPresetRegistry().find("a4-3x7-63.5x38.1").orElseThrow().page();
        return LabelTemplate.create("Benchmark", page, List.of(
            new TextElement(ElementFrame.create("Product", new RectMm(4,3,55,7),1),"{{product_name}} {{price}}","Arial",11,"#111827",false,false,TextAlignment.CENTER),
            new BarcodeElement(ElementFrame.create("Barcode",new RectMm(4,11,55.5,19),2),"{{sku}}",Symbology.CODE_128,0.25,2.5,true)));
    }
    public static void main(String[] args) throws Exception {
        rows=Integer.parseInt(args[0]); quantity=Integer.parseInt(args[1]); unique=Integer.parseInt(args[2]); fork=Integer.parseInt(args[3]);
        THREADS.setThreadAllocatedMemoryEnabled(true);
        var template=template();
        var mapping=new ColumnMapping(Map.of("product_name","Name","sku","Code","price","Retail Price"),"Qty",1,100000,5000000);
        // Warm class loading, barcode/font caches and all measured paths. Each measurement is a fresh JVM.
        for(int i=0;i<3;i++) {
            var warm=new BatchPreparationService().prepare(template,data(200,10,200),mapping);
            if(!warm.ready()) throw new AssertionError(warm.issues());
            BatchProductionWorkbench.asDataset(warm.printRecords());
            new UniversalPagePlanner().plan(template.page(),warm.labelCount(),0);
            new BatchPreparationResult(200,warm.printRecords(),List.of());
        }
        // Keep the session cache alive, as the desktop workbench does, including after the final phase.
        var cache=new BarcodeValidationCache();
        LIVE.add(cache);
        var service=new BatchPreparationService(cache);
        var source=measure("source",()->data(rows,quantity,unique));
        var prepared=measure("prepare",()->service.prepare(template,source,mapping));
        if(!prepared.ready() || prepared.labelCount()!=rows*quantity) throw new AssertionError(prepared.issues());
        var converted=measure("asDataset",()->BatchProductionWorkbench.asDataset(prepared));
        if(converted.rows().size()!=prepared.labelCount()) throw new AssertionError("Conversion count");
        measure("plan",()->new UniversalPagePlanner().plan(template.page(),prepared.labelCount(),0));
        // Diagnostic probe, NOT an extra production stage; isolates the defensive copies inside prepare().
        measure("resultCopyProbe",()->new BatchPreparationResult(rows,prepared.printRecords(),List.of()));
        LIVE.removeLast();
        // Mirrors the second preflight in UniversalPrintPane, without JavaFX or preview image rendering.
        measure("expandedPreflight",()-> {
            var check=new BarcodePreflight(template,Map.of(),cache);
            var issues=check.checkRecords(converted.rows(),converted.sourceRows(),()->false);
            if(!issues.isEmpty()) throw new AssertionError(issues);
            return issues;
        });
        System.out.flush();
    }
}
