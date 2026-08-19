package com.mohamed.barcodelabel.render.java2d;

import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.render.RenderException;

import java.awt.image.BufferedImage;
import java.util.Set;

public interface BarcodeRasterizer {
    Set<Symbology> supportedSymbologies();

    BufferedImage rasterize(BarcodeElement element, int widthPixels, int heightPixels, double dpi)
            throws RenderException;

    default boolean supports(Symbology symbology) {
        return supportedSymbologies().contains(symbology);
    }
}

