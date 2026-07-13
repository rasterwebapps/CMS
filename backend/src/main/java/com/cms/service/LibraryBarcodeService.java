package com.cms.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.cms.model.LibrarySetting;
import com.cms.repository.LibrarySettingRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Renders Code128 barcodes for library books/periodicals, using openpdf's built-in
 * Barcode128 (already a backend dependency for PDF exports — no separate barcode
 * library needed). Single-item PNGs for on-screen preview/printing, and multi-item
 * label-sheet PDFs sized per the librarian-configured label dimensions (mm).
 */
@Service
public class LibraryBarcodeService {

    /** Print-quality raster resolution for the single-item PNG label canvas. */
    private static final int PNG_DPI = 300;

    /**
     * Minimum quiet-zone margin around the barcode, guaranteed even when the configured
     * label is tiny — a purely proportional margin (e.g. canvasWidth/40) collapses toward
     * zero at small label sizes and can starve the blank space scanners need beside the
     * bars.
     */
    private static final float MIN_QUIET_ZONE_MM = 2f;

    /** Native resolution assumed for ZPL dot math — standard for GT420-class desktop thermal printers. */
    private static final int ZPL_DPI = 203;

    /** Connect/read timeout for the raw ZPL socket, so an unreachable printer fails fast rather than hanging the request. */
    private static final int PRINTER_SOCKET_TIMEOUT_MS = 3000;

    private static final Pattern IPV4_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    private final LibrarySettingRepository settingRepository;

    public LibraryBarcodeService(LibrarySettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public record LabelItem(String code, String title, String accessionNumber) {}

    /**
     * Renders a single barcode onto a canvas sized exactly to the configured label
     * dimensions (mm, converted to pixels at {@link #PNG_DPI}), with the barcode
     * centered and the code printed as a caption beneath it — so the PNG is a
     * physically accurate preview/print of the sticker, not just a bare barcode.
     */
    public byte[] generateBarcodePng(String value) throws IOException {
        int widthMm = getSettingInt("barcode_label_width_mm", 50);
        int heightMm = getSettingInt("barcode_label_height_mm", 25);
        int canvasWidth = mmToPixels(widthMm);
        int canvasHeight = mmToPixels(heightMm);

        BufferedImage rawBarcode = renderRawBarcode(value);

        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = canvas.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, canvasWidth, canvasHeight);

        int padding = Math.max(mmToPixels(MIN_QUIET_ZONE_MM), canvasWidth / 40);
        int captionHeight = Math.max(16, canvasHeight / 6);
        int drawableWidth = Math.max(1, canvasWidth - 2 * padding);
        int drawableHeight = Math.max(1, canvasHeight - captionHeight - 2 * padding);

        double scale = Math.min(
            (double) drawableWidth / rawBarcode.getWidth(),
            (double) drawableHeight / rawBarcode.getHeight());
        int scaledWidth = (int) Math.round(rawBarcode.getWidth() * scale);
        int scaledHeight = (int) Math.round(rawBarcode.getHeight() * scale);
        int x = (canvasWidth - scaledWidth) / 2;
        int y = padding;
        g2d.drawImage(rawBarcode, x, y, scaledWidth, scaledHeight, null);

        g2d.setColor(Color.BLACK);
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(10, captionHeight - 6));
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(value);
        int textX = Math.max(padding, (canvasWidth - textWidth) / 2);
        int textY = canvasHeight - padding;
        g2d.drawString(value, textX, textY);

        g2d.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(canvas, "png", out);
            return out.toByteArray();
        }
    }

    /**
     * Builds a grid of labels sized exactly to the configured mm dimensions — the table's
     * total width is explicitly locked to {@code columns * widthPt} (rather than stretched
     * to 100% of the page) so each cell is genuinely pinned to the configured width, and
     * each barcode image is scaled to fit its cell instead of rendering at a fixed size.
     */
    public byte[] generateLabelSheetPdf(List<LabelItem> items) throws Exception {
        float widthPt = mmToPoints(getSettingInt("barcode_label_width_mm", 50));
        float heightPt = mmToPoints(getSettingInt("barcode_label_height_mm", 25));

        float margin = 20f;
        float pageWidth = PageSize.A4.getWidth() - (2 * margin);
        int columns = Math.max(1, Math.min(items.size(), (int) (pageWidth / widthPt)));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, margin, margin, margin, margin);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            com.lowagie.text.Font captionFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 7);

            PdfPTable table = new PdfPTable(columns);
            table.setTotalWidth(columns * widthPt);
            table.setLockedWidth(true);

            float cellPadding = mmToPoints(MIN_QUIET_ZONE_MM);
            float captionSpace = 12f;
            float barcodeMaxWidth = widthPt - (2 * cellPadding);
            float barcodeMaxHeight = heightPt - (2 * cellPadding) - captionSpace;

            for (LabelItem item : items) {
                PdfPCell cell = new PdfPCell();
                cell.setFixedHeight(heightPt);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(cellPadding);

                Barcode128 barcode128 = new Barcode128();
                barcode128.setCode(item.code());
                barcode128.setBarHeight(Math.max(8f, barcodeMaxHeight));
                com.lowagie.text.Image barcodeImage = barcode128.createImageWithBarcode(writer.getDirectContent(), null, null);
                barcodeImage.scaleToFit(barcodeMaxWidth, barcodeMaxHeight);
                barcodeImage.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(barcodeImage);

                String caption = item.accessionNumber() != null ? item.accessionNumber() : item.code();
                Paragraph captionParagraph = new Paragraph(caption, captionFont);
                captionParagraph.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(captionParagraph);

                table.addCell(cell);
            }

            // Pad the final row with empty cells so the locked table width renders correctly
            // even when the item count isn't an exact multiple of the column count.
            int remainder = items.size() % columns;
            if (remainder != 0) {
                for (int i = 0; i < columns - remainder; i++) {
                    table.addCell(new PdfPCell(new Paragraph("")));
                }
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        }
    }

    /**
     * Plain ZPL (no Zebra-specific extensions) for a single label, sized to the configured
     * mm dimensions — understood natively or via emulation by most thermal label printers
     * (Zebra, TSC, Godex, many TVS models), not just Zebra hardware.
     */
    public String generateZpl(LabelItem item) {
        int widthMm = getSettingInt("barcode_label_width_mm", 50);
        int heightMm = getSettingInt("barcode_label_height_mm", 25);
        return buildZplRow(List.of(item), widthMm, heightMm);
    }

    /**
     * Builds one ZPL job containing a row per {@code barcode_labels_per_row} items — that
     * setting describes the physical media loaded (how many labels are die-cut across the
     * roll), not a per-job choice, so batch printing groups items into rows of that size
     * rather than deriving column count from a sheet page width like {@link #generateLabelSheetPdf}.
     */
    public String generateZplLabelSheet(List<LabelItem> items) {
        int widthMm = getSettingInt("barcode_label_width_mm", 50);
        int heightMm = getSettingInt("barcode_label_height_mm", 25);
        int labelsPerRow = Math.max(1, getSettingInt("barcode_labels_per_row", 1));

        StringBuilder zpl = new StringBuilder();
        for (int i = 0; i < items.size(); i += labelsPerRow) {
            List<LabelItem> row = items.subList(i, Math.min(i + labelsPerRow, items.size()));
            zpl.append(buildZplRow(new ArrayList<>(row), widthMm, heightMm));
        }
        return zpl.toString();
    }

    /**
     * Streams ZPL to the configured printer over a raw TCP socket (the standard way label
     * printers with an Ethernet/print-server interface accept jobs — port 9100 by default).
     * The IP is admin-configured free text, so it's re-validated here (not just at
     * settings-save time) before ever opening a connection.
     */
    public void sendZpl(String zpl) throws IOException {
        String ip = getSettingString("barcode_printer_ip", "");
        int port = getSettingInt("barcode_printer_port", 9100);
        if (ip.isBlank()) {
            throw new IOException("No label printer IP is configured (Library Settings → Label Printer)");
        }
        if (!isPrivateNetworkAddress(ip)) {
            throw new IOException("Configured printer IP is not a private/local network address: " + ip);
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), PRINTER_SOCKET_TIMEOUT_MS);
            socket.setSoTimeout(PRINTER_SOCKET_TIMEOUT_MS);
            try (OutputStream out = socket.getOutputStream()) {
                out.write(zpl.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }
    }

    /**
     * Restricts the configured printer IP to RFC1918 private ranges or loopback, so this
     * admin-entered free text can't turn the backend into a relay to an arbitrary public host.
     * The regex check runs first specifically so {@link InetAddress#getByName} only ever sees
     * a literal dotted-quad and never triggers a DNS lookup on a hostname.
     */
    static boolean isPrivateNetworkAddress(String ip) {
        Matcher matcher = IPV4_PATTERN.matcher(ip);
        if (!matcher.matches()) {
            return false;
        }
        for (int i = 1; i <= 4; i++) {
            int octet = Integer.parseInt(matcher.group(i));
            if (octet < 0 || octet > 255) {
                return false;
            }
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.isLoopbackAddress() || addr.isSiteLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * One ^XA...^XZ job containing {@code rowItems.size()} barcodes placed side by side —
     * one physical "row" as the printer's gap sensor sees it. A single-item call (the
     * on-demand single barcode print) is just the degenerate case of a row of one.
     */
    private String buildZplRow(List<LabelItem> rowItems, int singleLabelWidthMm, int heightMm) {
        int quietZoneDots = mmToDots(MIN_QUIET_ZONE_MM);
        int singleLabelWidthDots = mmToDots(singleLabelWidthMm);
        int heightDots = mmToDots(heightMm);
        int rowWidthDots = singleLabelWidthDots * rowItems.size();

        int captionHeightDots = Math.max(mmToDots(3f), heightDots / 6);
        int barHeightDots = Math.max(mmToDots(4f), heightDots - captionHeightDots - 2 * quietZoneDots);
        int barcodeAreaWidthDots = Math.max(1, singleLabelWidthDots - 2 * quietZoneDots);

        StringBuilder zpl = new StringBuilder();
        zpl.append("^XA\n");
        zpl.append("^PW").append(rowWidthDots).append('\n');
        zpl.append("^LL").append(heightDots).append('\n');
        zpl.append("^CI28\n");

        for (int i = 0; i < rowItems.size(); i++) {
            LabelItem item = rowItems.get(i);
            int xOffset = i * singleLabelWidthDots + quietZoneDots;
            String caption = item.accessionNumber() != null ? item.accessionNumber() : item.code();
            int captionFontSize = Math.max(14, captionHeightDots - 4);

            zpl.append("^FO").append(xOffset).append(',').append(quietZoneDots).append('\n');
            zpl.append("^BY2,3,").append(barHeightDots).append('\n');
            zpl.append("^BCN,").append(barHeightDots).append(",N,N,N\n");
            zpl.append("^FD").append(escapeZpl(item.code())).append("^FS\n");

            int captionY = quietZoneDots + barHeightDots + 4;
            zpl.append("^FO").append(xOffset).append(',').append(captionY).append('\n');
            zpl.append("^A0N,").append(captionFontSize).append(',').append(captionFontSize).append('\n');
            zpl.append("^FB").append(barcodeAreaWidthDots).append(",1,0,C\n");
            zpl.append("^FD").append(escapeZpl(caption)).append("^FS\n");
        }

        zpl.append("^XZ\n");
        return zpl.toString();
    }

    /** Strips ZPL's own command-prefix characters out of field data so encoded text can't break the command stream. */
    private static String escapeZpl(String value) {
        if (value == null) return "";
        return value.replace("^", "").replace("~", "");
    }

    private BufferedImage renderRawBarcode(String value) throws IOException {
        Barcode128 barcode = new Barcode128();
        barcode.setCode(value);
        barcode.setBarHeight(40f);
        barcode.setX(1.2f);

        // Barcode128#createAwtImage returns a plain java.awt.Image built via Toolkit/MemoryImageSource,
        // not a BufferedImage — casting directly throws ClassCastException. MediaTracker forces the
        // (normally synchronous, but not guaranteed) pixel producer to finish before we read
        // dimensions, then it's painted onto a real BufferedImage so it can be scaled/encoded.
        java.awt.Image awtImage = barcode.createAwtImage(Color.BLACK, Color.WHITE);
        MediaTracker tracker = new MediaTracker(new java.awt.Container());
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

    private static float mmToPoints(float mm) {
        return mm * 72f / 25.4f;
    }

    private static int mmToPixels(float mm) {
        return Math.round(mm / 25.4f * PNG_DPI);
    }

    private static int mmToDots(float mm) {
        return Math.round(mm / 25.4f * ZPL_DPI);
    }

    private int getSettingInt(String key, int defaultValue) {
        return settingRepository.findBySettingKey(key)
            .map(s -> { try { return Integer.parseInt(s.getSettingValue()); } catch (NumberFormatException e) { return defaultValue; } })
            .orElse(defaultValue);
    }

    private String getSettingString(String key, String defaultValue) {
        return settingRepository.findBySettingKey(key)
            .map(LibrarySetting::getSettingValue)
            .filter(v -> !v.isBlank())
            .orElse(defaultValue);
    }
}
