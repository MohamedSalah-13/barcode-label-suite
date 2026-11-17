package com.mohamed.barcodelabel.app;

import com.mohamed.barcodelabel.data.TemplateJsonService;
import com.mohamed.barcodelabel.model.LabelTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Where the application keeps the label being designed when it closes, to open it again on the next run. The
 * label's page is part of it, so the paper size comes back too.
 */
interface LastDesign {
    /** The label kept by the last run; empty when there is none or it cannot be read. */
    Optional<LabelTemplate> load();

    /** Keeps this label for the next run. */
    void save(LabelTemplate template);

    /** Keeps the label in memory only: for tests. */
    static LastDesign inMemory() {
        return new LastDesign() {
            private LabelTemplate saved;
            @Override public Optional<LabelTemplate> load() { return Optional.ofNullable(saved); }
            @Override public void save(LabelTemplate template) { saved = template; }
        };
    }

    /** Keeps the label as a template JSON file, the same format as "Save". */
    static LastDesign file(Path file) {
        return new LastDesign() {
            @Override public Optional<LabelTemplate> load() {
                if (!Files.isRegularFile(file)) return Optional.empty();
                try {
                    return Optional.of(new TemplateJsonService().read(file));
                } catch (IOException | RuntimeException unreadable) {
                    // A damaged or older file is not worth failing the start for: the app opens its starting label.
                    return Optional.empty();
                }
            }

            @Override public void save(LabelTemplate template) {
                try {
                    Files.createDirectories(file.toAbsolutePath().getParent());
                    // Written beside the file, then moved over it, so a failed write never leaves half a file.
                    Path partial = file.resolveSibling(file.getFileName() + ".tmp");
                    new TemplateJsonService().write(partial, template);
                    Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException | RuntimeException ignored) {
                    // The label is only not remembered; closing the app must not fail because of it.
                }
            }
        };
    }

    /**
     * The user's own file: {@code %APPDATA%\Barcode Label Suite\last-design.label.json} on Windows,
     * {@code ~/.barcode-label-suite/last-design.label.json} elsewhere.
     */
    static LastDesign userFile() {
        return file(userFile(System.getenv("APPDATA"), System.getProperty("user.home")));
    }

    /** The file under {@code appData} when it is set, as on Windows, or else under {@code home}. */
    static Path userFile(String appData, String home) {
        Path folder = appData != null && !appData.isBlank()
                ? Path.of(appData, "Barcode Label Suite")
                : Path.of(home, ".barcode-label-suite");
        return folder.resolve("last-design.label.json");
    }
}
