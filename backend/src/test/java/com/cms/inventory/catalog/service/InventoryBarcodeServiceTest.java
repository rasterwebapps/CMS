package com.cms.inventory.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/**
 * Covers the "Barcode/GTIN — capture + lookup + label printing" Phase 3 item's label renderer.
 * Only a smoke test on the produced image, matching this codebase's own precedent
 * (LibraryBarcodeServiceTest never asserts on PNG pixels either — the deterministic logic worth
 * testing there is the string-based ZPL generation, which this class doesn't have).
 */
class InventoryBarcodeServiceTest {

    private final InventoryBarcodeService service = new InventoryBarcodeService();

    @Test
    void generatesADecodablePngOfTheExpectedSize() throws Exception {
        byte[] png = service.generateBarcodePng("CHM-0001", "Sodium Chloride", "CHM-0001");

        assertThat(png).isNotEmpty();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(600);
        assertThat(image.getHeight()).isEqualTo(300);
    }

    @Test
    void generatesAValidPngForARealWorldGtinValue() throws Exception {
        byte[] png = service.generateBarcodePng("8901030826001", "Sodium Chloride", "8901030826001");

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image).isNotNull();
    }
}
