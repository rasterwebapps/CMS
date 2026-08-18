package com.cms.spatial.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

import com.cms.spatial.model.FloorPlan;
import com.cms.spatial.model.enums.OriginAnchor;

class SpatialTransformTest {

    private FloorPlan plan(OriginAnchor anchor, double originX, double originY, double scaleFactor) {
        FloorPlan plan = new FloorPlan();
        plan.setId(1L);
        plan.setOriginAnchor(anchor);
        plan.setOriginX(originX);
        plan.setOriginY(originY);
        plan.setViewboxWidth(1000.0);
        plan.setViewboxHeight(800.0);
        plan.setScaleFactor(scaleFactor);
        return plan;
    }

    @Test
    void computeScaleFactor_derivesPhysicalUnitsPerSvgUnit() {
        // 100px apart on the SVG, known to be 5 meters apart in reality -> 0.05 m/px
        double scale = SpatialTransform.computeScaleFactor(0, 0, 100, 0, 5.0);

        assertThat(scale).isCloseTo(0.05, within(1e-9));
    }

    @Test
    void computeScaleFactor_rejectsCoincidentPoints() {
        assertThatThrownBy(() -> SpatialTransform.computeScaleFactor(10, 10, 10, 10, 5.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void computeScaleFactor_rejectsNonPositivePhysicalLength() {
        assertThatThrownBy(() -> SpatialTransform.computeScaleFactor(0, 0, 100, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void physicalToSvg_topLeft_noFlipNoOffset() {
        FloorPlan plan = plan(OriginAnchor.TOP_LEFT, 0, 0, 0.05);

        SpatialPoint svg = SpatialTransform.physicalToSvg(plan, 5.0, 2.0);

        assertThat(svg.x()).isCloseTo(100.0, within(1e-9));
        assertThat(svg.y()).isCloseTo(40.0, within(1e-9));
    }

    @Test
    void physicalToSvg_bottomLeft_flipsY() {
        FloorPlan plan = plan(OriginAnchor.BOTTOM_LEFT, 0, 0, 0.05);

        SpatialPoint svg = SpatialTransform.physicalToSvg(plan, 5.0, 2.0);

        // anchor SVG y = viewboxHeight(800) - originY(0) = 800; physical y increases upward
        assertThat(svg.x()).isCloseTo(100.0, within(1e-9));
        assertThat(svg.y()).isCloseTo(760.0, within(1e-9));
    }

    @Test
    void physicalToSvg_center_offsetAndFlip() {
        FloorPlan plan = plan(OriginAnchor.CENTER, 10, -10, 0.1);

        SpatialPoint svg = SpatialTransform.physicalToSvg(plan, 0.0, 0.0);

        // anchor SVG = (1000/2 + 10, 800/2 - 10) = (510, 390); origin itself maps exactly to anchor
        assertThat(svg.x()).isCloseTo(510.0, within(1e-9));
        assertThat(svg.y()).isCloseTo(390.0, within(1e-9));
    }

    @Test
    void svgToPhysical_isInverseOfPhysicalToSvg_acrossAllAnchors() {
        for (OriginAnchor anchor : OriginAnchor.values()) {
            FloorPlan plan = plan(anchor, 15, 25, 0.08);

            SpatialPoint svg = SpatialTransform.physicalToSvg(plan, 12.5, -3.75);
            SpatialPoint physical = SpatialTransform.svgToPhysical(plan, svg.x(), svg.y());

            assertThat(physical.x()).as("anchor=%s", anchor).isCloseTo(12.5, within(1e-9));
            assertThat(physical.y()).as("anchor=%s", anchor).isCloseTo(-3.75, within(1e-9));
        }
    }

    @Test
    void lengthConversions_areMutualInverses() {
        FloorPlan plan = plan(OriginAnchor.TOP_LEFT, 0, 0, 0.05);

        double svgLength = SpatialTransform.physicalLengthToSvg(plan, 5.0);
        double physicalLength = SpatialTransform.svgLengthToPhysical(plan, svgLength);

        assertThat(svgLength).isCloseTo(100.0, within(1e-9));
        assertThat(physicalLength).isCloseTo(5.0, within(1e-9));
    }

    @Test
    void physicalToSvg_uncalibratedPlan_throws() {
        FloorPlan plan = new FloorPlan();
        plan.setId(2L);
        plan.setOriginAnchor(OriginAnchor.TOP_LEFT);

        assertThatThrownBy(() -> SpatialTransform.physicalToSvg(plan, 1.0, 1.0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not calibrated");
    }

    @Test
    void physicalToSvg_centerAnchorWithoutViewbox_throws() {
        FloorPlan plan = new FloorPlan();
        plan.setId(3L);
        plan.setOriginAnchor(OriginAnchor.CENTER);
        plan.setScaleFactor(0.05);

        assertThatThrownBy(() -> SpatialTransform.physicalToSvg(plan, 1.0, 1.0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("viewboxWidth");
    }
}
