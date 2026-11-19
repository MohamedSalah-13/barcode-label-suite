package com.mohamed.barcodelabel.batch;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Supplier;

/** Session-local, bounded cache of encoding diagnostics, never raster images or row data.
 * A changed template starts an independent scope; in-flight checks keep their old scope.
 * Within a scope only the same barcode element and resolved value can reuse a result.
 */
public final class BarcodeValidationCache {
    private final int capacity;
    private LabelTemplate template;
    private Scope current;

    public BarcodeValidationCache() { this(20_000); }

    BarcodeValidationCache(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    synchronized Scope scope(LabelTemplate value) {
        Objects.requireNonNull(value, "template");
        if (!value.equals(template)) {
            template = value;
            current = new Scope(capacity);
        }
        return current;
    }

    static final class Scope {
        private record Key(BarcodeElement barcode, String value) {}
        private final int capacity;
        private final LinkedHashMap<Key, String> diagnostics = new LinkedHashMap<>(16, .75f, true);

        Scope(int capacity) { this.capacity = capacity; }

        synchronized String diagnostic(BarcodeElement barcode, String value, Supplier<String> encode) {
            var key = new Key(barcode, value);
            String cached = diagnostics.get(key);
            if (cached != null) return cached;
            String result = Objects.requireNonNull(encode.get());
            if (diagnostics.size() == capacity) diagnostics.remove(diagnostics.keySet().iterator().next());
            diagnostics.put(key, result);
            return result;
        }
    }
}
