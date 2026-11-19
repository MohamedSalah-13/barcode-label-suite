package com.mohamed.barcodelabel.workflow.template;

import com.mohamed.barcodelabel.data.TemplateJsonService;
import com.mohamed.barcodelabel.model.LabelTemplate;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class TemplateDocuments {
    private final TemplateJsonService json = new TemplateJsonService();

    public LabelTemplate open(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        return json.read(path);
    }

    public void save(Path path, LabelTemplate template) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(template, "template");
        Path absolute = path.toAbsolutePath();
        Path directory = absolute.getParent();
        if (directory != null) Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, absolute.getFileName().toString(), ".tmp");
        try {
            json.write(temporary, template);
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public LabelTemplate withPage(LabelTemplate source, com.mohamed.barcodelabel.model.page.PageDefinition page) {
        Objects.requireNonNull(source, "source");
        return source.withLayout(Objects.requireNonNull(page, "page"), source.dualLayout());
    }
}
