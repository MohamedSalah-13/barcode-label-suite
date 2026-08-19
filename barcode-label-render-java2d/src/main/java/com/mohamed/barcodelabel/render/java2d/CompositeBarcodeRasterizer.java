package com.mohamed.barcodelabel.render.java2d;

import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.render.RenderException;

import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class CompositeBarcodeRasterizer implements BarcodeRasterizer {
    private final List<BarcodeRasterizer> providers;
    private final Set<Symbology> supported;

    public CompositeBarcodeRasterizer(List<BarcodeRasterizer> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
        if (this.providers.isEmpty()) {
            throw new IllegalArgumentException("at least one barcode provider is required");
        }
        var symbologies = new LinkedHashSet<Symbology>();
        this.providers.forEach(provider -> symbologies.addAll(provider.supportedSymbologies()));
        this.supported = Set.copyOf(symbologies);
    }

    public static CompositeBarcodeRasterizer standard() {
        return new CompositeBarcodeRasterizer(List.of(new Barcode4jRasterizer(), new ZxingRasterizer()));
    }

    @Override
    public Set<Symbology> supportedSymbologies() {
        return supported;
    }

    @Override
    public BufferedImage rasterize(BarcodeElement element, int widthPixels, int heightPixels, double dpi)
            throws RenderException {
        return providers.stream()
                .filter(provider -> provider.supports(element.symbology()))
                .findFirst()
                .orElseThrow(() -> new RenderException("Unsupported barcode symbology: " + element.symbology()))
                .rasterize(element, widthPixels, heightPixels, dpi);
    }
}

