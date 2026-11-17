package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.workflow.paper.PaperPreset;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Where the workbench keeps the paper sizes the user named in the custom size dialog, so they are in the paper
 * list again on the next run.
 */
public interface CustomPaperStore {
    /** The saved sizes, in the order they were saved; none that cannot be read. */
    List<PaperPreset> load();

    /** Replaces the saved sizes with these. */
    void save(List<PaperPreset> presets);

    /** Sizes kept only while the application runs: the default for an embedded workbench. */
    static CustomPaperStore inMemory() {
        return new CustomPaperStore() {
            private List<PaperPreset> saved = List.of();
            @Override public List<PaperPreset> load() { return saved; }
            @Override public void save(List<PaperPreset> presets) { saved = List.copyOf(new ArrayList<>(presets)); }
        };
    }

    /** Sizes kept in this preferences node, one child node each, so they survive a restart. */
    static CustomPaperStore preferences(Preferences node) {
        return new PreferencesPaperStore(node);
    }
}
