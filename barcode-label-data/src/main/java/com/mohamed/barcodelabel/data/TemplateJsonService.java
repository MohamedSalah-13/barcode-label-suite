package com.mohamed.barcodelabel.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mohamed.barcodelabel.model.LabelTemplate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class TemplateJsonService {
    private final ObjectMapper mapper;

    public TemplateJsonService() {
        this(defaultMapper());
    }

    public TemplateJsonService(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public LabelTemplate read(Path path) throws IOException {
        return mapper.readValue(path.toFile(), LabelTemplate.class);
    }

    public LabelTemplate read(String json) throws IOException {
        return mapper.readValue(json, LabelTemplate.class);
    }

    public void write(Path path, LabelTemplate template) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(template, "template");
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), template);
    }

    public String write(LabelTemplate template) throws IOException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(template);
    }

    private static ObjectMapper defaultMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}

