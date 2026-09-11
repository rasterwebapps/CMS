package com.cms.inventory.catalog.service;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.lowagie.text.pdf.Barcode128;

/**
 * Renders a printable Code128 barcode label PNG for a {@link com.cms.inventory.catalog.model.Product}
 * — see the 2026-09-11 "Barcode/GTIN" decision-log entry. Deliberately a standalone, minimal
 * renderer rather than a shared extraction of {@code com.cms.service.LibraryBarcodeService}: that
 * service is hospital/college-branded (a hardcoded institution tag) and configured through
 * {@code LibrarySetting}, and Inventory must stay industry-agnostic with no cross-module
 * dependency into Library internals (see the "no vertical branding" standing decision). Fixed
 * canvas size, PNG only — no PDF label sheets or thermal-printer (ZPL) integration in this first
 * slice; a future slice can add those following {@code LibraryBarcodeService}'s proven shape if a
 * real need shows up, without this class needing to change.
 */
@Service
public class InventoryBarcodeService {

    private static final int CANVAS_WIDTH = 600;
    private static final int CANVAS_HEIGHT = 300;
    private static final int MARGIN = 24;

    /** @param code the value encoded in the barcode — the product's barcode/GTIN if captured,
     *  else its own productCode. @param title the product name, printed above the barcode.
     *  @param subtitle the code itself, printed below the barcode for human reading. */
    public byte[] generateBarcodePng(String code, String title, String subtitle) throws IOException {
        BufferedImage rawBarcode = renderRawBarcode(code);

        BufferedImage canvas = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = canvas.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        g2d.setColor(Color.BLACK);

        int titleHeight = 40;
        int subtitleHeight = 34;
        int drawableWidth = CANVAS_WIDTH - 2 * MARGIN;
        int drawableHeight = CANVAS_HEIGHT - titleHeight - subtitleHeight - 2 * MARGIN;

        int y = MARGIN;
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        drawCentered(g2d, fitToWidth(g2d, title, drawableWidth), y, titleHeight);
        y += titleHeight;

        double scale = Math.min(
            (double) drawableWidth / rawBarcode.getWidth(),
            (double) drawableHeight / rawBarcode.getHeight());
        int scaledWidth = (int) Math.round(rawBarcode.getWidth() * scale);
        int scaledHeight = (int) Math.round(rawBarcode.getHeight() * scale);
        int bx = (CANVAS_WIDTH - scaledWidth) / 2;
        g2d.drawImage(rawBarcode, bx, y, scaledWidth, scaledHeight, null);
        y += drawableHeight;

        g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
        drawCentered(g2d, subtitle, y, subtitleHeight);

        g2d.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(canvas, "png", out);
            return out.toByteArray();
        }
    }

    private static void drawCentered(Graphics2D g2d, String text, int rowTop, int rowHeight) {
        if (text == null || text.isBlank()) return;
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = Math.max(0, (CANVAS_WIDTH - textWidth) / 2);
        int baselineY = rowTop + (rowHeight + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(text, x, baselineY);
    }

    private static String fitToWidth(Graphics2D g2d, String text, int maxWidthPx) {
        if (text == null) return "";
        String trimmed = text.trim();
        FontMetrics fm = g2d.getFontMetrics();
        if (fm.stringWidth(trimmed) <= maxWidthPx) return trimmed;
        String ellipsis = "…";
        int lo = 0, hi = trimmed.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (fm.stringWidth(trimmed.substring(0, mid) + ellipsis) <= maxWidthPx) lo = mid; else hi = mid - 1;
        }
        return lo == 0 ? ellipsis : trimmed.substring(0, lo) + ellipsis;
    }

    private BufferedImage renderRawBarcode(String value) throws IOException {
        Barcode128 barcode = new Barcode128();
        barcode.setCode(value);
        barcode.setBarHeight(50f);
        barcode.setX(1.4f);

        // Barcode128#createAwtImage returns a plain java.awt.Image (Toolkit/MemoryImageSource),
        // not a BufferedImage — MediaTracker forces the pixel producer to finish before reading
        // dimensions, then it's painted onto a real BufferedImage so it can be scaled/encoded.
        // Same approach as com.cms.service.LibraryBarcodeService.renderRawBarcode.
        Image awtImage = barcode.createAwtImage(Color.BLACK, Color.WHITE);
        MediaTracker tracker = new MediaTracker(new Container());
        tracker.addImage(awtImage, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while rendering barcode image", e);
        }

        int width = awtImage.getWidth(null);
        int height = awtImage.getHeight(null);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.drawImage(awtImage, 0, 0, null);
        g2d.dispose();
        return image;
    }
}
