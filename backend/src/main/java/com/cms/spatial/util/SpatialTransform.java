package com.cms.spatial.util;

import com.cms.spatial.model.FloorPlan;
import com.cms.spatial.model.enums.OriginAnchor;

/**
 * Converts between physical-space coordinates (in {@link FloorPlan#getUnitSystem()} units,
 * physical y increasing away from the ground-plane origin) and SVG-space coordinates (px within
 * the plan's viewBox, y increasing downward per SVG convention), using a FloorPlan's calibration.
 *
 * <p>{@code originAnchor} plus ({@code originX}, {@code originY}) locate the physical origin
 * within the SVG viewBox:
 * <ul>
 *   <li>{@code TOP_LEFT} — SVG point ({@code originX}, {@code originY}); physical y and SVG y
 *       both increase downward, no flip.</li>
 *   <li>{@code BOTTOM_LEFT} — SVG point ({@code originX}, {@code viewboxHeight - originY});
 *       physical y increases upward, so SVG y is flipped relative to physical y.</li>
 *   <li>{@code CENTER} — SVG point ({@code viewboxWidth/2 + originX}, {@code viewboxHeight/2 +
 *       originY}); physical y increases upward, flipped, like a centered cartesian plane.</li>
 * </ul>
 *
 * <p>{@code scaleFactor} is physical units per SVG unit (e.g. meters per px), derived at
 * calibration time from two SVG-space calibration points and the real-world distance between
 * them ({@link #computeScaleFactor}).
 */
public final class SpatialTransform {

    private SpatialTransform() {
    }

    public static SpatialPoint physicalToSvg(FloorPlan plan, double physicalX, double physicalY) {
        double scale = requireScale(plan);
        SpatialPoint anchor = anchorSvgPoint(plan);
        double svgX = anchor.x() + physicalX / scale;
        double svgY = isYFlipped(plan) ? anchor.y() - physicalY / scale : anchor.y() + physicalY / scale;
        return new SpatialPoint(svgX, svgY);
    }

    public static SpatialPoint svgToPhysical(FloorPlan plan, double svgX, double svgY) {
        double scale = requireScale(plan);
        SpatialPoint anchor = anchorSvgPoint(plan);
        double physicalX = (svgX - anchor.x()) * scale;
        double physicalY = isYFlipped(plan) ? (anchor.y() - svgY) * scale : (svgY - anchor.y()) * scale;
        return new SpatialPoint(physicalX, physicalY);
    }

    public static double physicalLengthToSvg(FloorPlan plan, double physicalLength) {
        return physicalLength / requireScale(plan);
    }

    public static double svgLengthToPhysical(FloorPlan plan, double svgLength) {
        return svgLength * requireScale(plan);
    }

    /**
     * Physical units per SVG unit, derived from two SVG-space calibration points and the known
     * real-world distance between them. Pure function — does not read or mutate a FloorPlan.
     */
    public static double computeScaleFactor(double point1X, double point1Y, double point2X, double point2Y,
            double physicalLength) {
        if (physicalLength <= 0) {
            throw new IllegalArgumentException("Calibration physical length must be positive");
        }
        double pixelDistance = Math.hypot(point2X - point1X, point2Y - point1Y);
        if (pixelDistance <= 0) {
            throw new IllegalArgumentException("Calibration points must not coincide");
        }
        return physicalLength / pixelDistance;
    }

    private static boolean isYFlipped(FloorPlan plan) {
        return plan.getOriginAnchor() != OriginAnchor.TOP_LEFT;
    }

    private static double requireScale(FloorPlan plan) {
        Double scale = plan.getScaleFactor();
        if (scale == null || scale <= 0) {
            throw new IllegalStateException("Floor plan " + plan.getId() + " is not calibrated");
        }
        return scale;
    }

    private static SpatialPoint anchorSvgPoint(FloorPlan plan) {
        double originX = plan.getOriginX() != null ? plan.getOriginX() : 0.0;
        double originY = plan.getOriginY() != null ? plan.getOriginY() : 0.0;
        return switch (plan.getOriginAnchor()) {
            case TOP_LEFT -> new SpatialPoint(originX, originY);
            case BOTTOM_LEFT -> new SpatialPoint(originX, requireViewboxHeight(plan) - originY);
            case CENTER -> new SpatialPoint(requireViewboxWidth(plan) / 2 + originX,
                    requireViewboxHeight(plan) / 2 + originY);
        };
    }

    private static double requireViewboxWidth(FloorPlan plan) {
        Double width = plan.getViewboxWidth();
        if (width == null) {
            throw new IllegalStateException("Floor plan " + plan.getId() + " has no viewboxWidth set");
        }
        return width;
    }

    private static double requireViewboxHeight(FloorPlan plan) {
        Double height = plan.getViewboxHeight();
        if (height == null) {
            throw new IllegalStateException("Floor plan " + plan.getId() + " has no viewboxHeight set");
        }
        return height;
    }
}
