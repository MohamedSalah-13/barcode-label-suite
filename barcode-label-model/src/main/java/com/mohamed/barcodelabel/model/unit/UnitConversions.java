package com.mohamed.barcodelabel.model.unit;

public final class UnitConversions {
    public static final double MILLIMETRES_PER_INCH = 25.4;
    public static final double POINTS_PER_INCH = 72.0;

    private UnitConversions() {
    }

    public static double mmToPoints(double millimetres) {
        return millimetres * POINTS_PER_INCH / MILLIMETRES_PER_INCH;
    }

    public static double pointsToMm(double points) {
        return points * MILLIMETRES_PER_INCH / POINTS_PER_INCH;
    }

    public static double mmToPixels(double millimetres, double dpi) {
        requirePositiveDpi(dpi);
        return millimetres * dpi / MILLIMETRES_PER_INCH;
    }

    public static double pixelsToMm(double pixels, double dpi) {
        requirePositiveDpi(dpi);
        return pixels * MILLIMETRES_PER_INCH / dpi;
    }

    private static void requirePositiveDpi(double dpi) {
        if (!Double.isFinite(dpi) || dpi <= 0) {
            throw new IllegalArgumentException("dpi must be a finite positive number");
        }
    }
}

