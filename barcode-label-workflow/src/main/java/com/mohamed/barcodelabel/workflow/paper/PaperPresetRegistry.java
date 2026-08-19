package com.mohamed.barcodelabel.workflow.paper;

import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.PageMode;
import com.mohamed.barcodelabel.model.page.PageOrientation;
import com.mohamed.barcodelabel.model.page.SheetLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PaperPresetRegistry {
    private final Map<String, PaperPreset> presets = new LinkedHashMap<>();

    public PaperPresetRegistry() { builtIns().forEach(this::register); }

    public void register(PaperPreset preset) {
        if (presets.putIfAbsent(preset.id(), preset) != null) {
            throw new IllegalArgumentException("Duplicate paper preset id: " + preset.id());
        }
    }

    public List<PaperPreset> all() { return List.copyOf(presets.values()); }
    public Optional<PaperPreset> find(String id) { return Optional.ofNullable(presets.get(id)); }

    private static List<PaperPreset> builtIns() {
        var result = new ArrayList<PaperPreset>();
        result.add(sheet("a4-3x7-63.5x38.1", "A4 — 3 × 7 (63.5 × 38.1 مم)", 3, 7, 63.5, 38.1, 2.5, 0, 15.15, 7.25));
        result.add(sheet("a4-2x7-99.1x38.1", "A4 — 2 × 7 (99.1 × 38.1 مم)", 2, 7, 99.1, 38.1, 2.8, 0, 15.15, 4.5));
        result.add(sheet("a4-4x10-48.5x25", "A4 — 4 × 10 (48.5 × 25 مم)", 4, 10, 48.5, 25, 2, 2, 14.5, 5));
        result.add(roll("roll-40x25", "رول حراري — 40 × 25 مم", 40, 25));
        result.add(roll("roll-50x30", "رول حراري — 50 × 30 مم", 50, 30));
        result.add(roll("roll-58x40", "رول حراري — 58 × 40 مم", 58, 40));
        result.add(roll("roll-100x50", "رول حراري — 100 × 50 مم", 100, 50));
        return result;
    }

    private static PaperPreset sheet(String id, String name, int columns, int rows,
                                     double labelWidth, double labelHeight, double horizontalGap, double verticalGap,
                                     double verticalMargin, double horizontalMargin) {
        var layout = new SheetLayout(columns, rows, new DimensionsMm(labelWidth, labelHeight), horizontalGap, verticalGap);
        var page = new PageDefinition(PageMode.SHEET, new DimensionsMm(210, 297),
                new InsetsMm(verticalMargin, horizontalMargin, verticalMargin, horizontalMargin),
                PageOrientation.PORTRAIT, layout);
        return new PaperPreset(id, name, "A4", page);
    }

    private static PaperPreset roll(String id, String name, double width, double height) {
        var page = new PageDefinition(PageMode.ROLL, new DimensionsMm(width, height), InsetsMm.ZERO,
                PageOrientation.PORTRAIT, null);
        return new PaperPreset(id, name, "رول حراري", page);
    }
}
