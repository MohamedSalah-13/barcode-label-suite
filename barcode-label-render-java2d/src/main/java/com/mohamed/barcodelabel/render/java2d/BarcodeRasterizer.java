package com.mohamed.barcodelabel.render.java2d;

import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.model.unit.UnitConversions;
import com.mohamed.barcodelabel.render.RenderException;

import java.awt.image.BufferedImage;
import java.util.Set;

public interface BarcodeRasterizer {
    Set<Symbology> supportedSymbologies();

    BufferedImage rasterize(BarcodeElement element, int widthPixels, int heightPixels, double dpi)
            throws RenderException;

    /** Rasterizes at the size of the printed element, checking that the entire symbol fits. */
    default BufferedImage rasterize(BarcodeElement element, double dpi) throws RenderException {
        var bounds = element.frame().bounds();
        int width = Math.max(1, (int) Math.floor(UnitConversions.mmToPixels(bounds.width(), dpi)));
        int height = Math.max(1, (int) Math.floor(UnitConversions.mmToPixels(bounds.height(), dpi)));
        BufferedImage image = rasterize(element, width, height, dpi);
        if (image.getWidth() > width || image.getHeight() > height) {
            throw new RenderException("Barcode does not fit its frame. Required " + image.getWidth() + "x"
                    + image.getHeight() + " px, available " + width + "x" + height + " px");
        }
        return image;
    }

    default boolean supports(Symbology symbology) {
        return supportedSymbologies().contains(symbology);
    }
}

