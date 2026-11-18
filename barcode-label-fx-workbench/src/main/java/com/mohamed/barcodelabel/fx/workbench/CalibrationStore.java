package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.print.PrintOffset;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

final class CalibrationStore {
    private final Preferences preferences;

    CalibrationStore(Preferences preferences) {
        this.preferences = preferences;
    }

    PrintOffset load(String printer, DimensionsMm paper) {
        String value = preferences.get(key(printer, paper), "");
        String[] axes = value.split(",", -1);
        if (axes.length != 2) return PrintOffset.ZERO;
        try {
            return new PrintOffset(Double.parseDouble(axes[0]), Double.parseDouble(axes[1]));
        } catch (IllegalArgumentException malformed) {
            return PrintOffset.ZERO;
        }
    }

    void save(String printer, DimensionsMm paper, PrintOffset offset) throws BackingStoreException {
        preferences.put(key(printer, paper), offset.horizontalMm() + "," + offset.verticalMm());
        preferences.flush();
    }

    private static String key(String printer, DimensionsMm paper) {
        // Network printer names can contain separators and exceed the Preferences key length limit.
        String identity = printer.toLowerCase(Locale.ROOT) + "\n" + paper.width() + "x" + paper.height();
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
