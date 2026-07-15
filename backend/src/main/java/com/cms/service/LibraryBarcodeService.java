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
 *
 * Every label (PNG preview, PDF sheet cell, and each ZPL row cell) uses the same
 * four-row layout — institution header, barcode, truncated title, accession+shelf
 * footer. For ZPL, {@code barcode_labels_per_row} describes the physical media (how
 * many labels are die-cut side by side across one row of the roll), not a way to print
 * a wider composite row: the printable footprint per row always stays fixed at the
 * configured {@code barcode_label_width_mm × barcode_label_height_mm}, and that fixed
 * area gets subdivided into {@code barcode_labels_per_row} equally-scaled sub-cells
 * (both width and height divided by the count) rather than being multiplied by it — see
 * {@link #buildZplRow} for why side-by-side sub-labels always share the same feed-axis
 * gap and so must scale down together.
 */
@Service
public class LibraryBarcodeService {

    /** Print-quality raster resolution for the single-item PNG label canvas. */
    private static final int PNG_DPI = 300;

    /**
     * Minimum quiet-zone margin around the barcode, guaranteed even when the configured
     * label is tiny — a purely proportional margin (e.g. canvasWidth/40) collapses toward
     * zero at small label sizes and can starve the blank space scanners need beside the
     * bars. Also reused as the general outer margin for the header/title/footer text rows.
     */
    private static final float MIN_QUIET_ZONE_MM = 2f;

    /** Native resolution assumed for ZPL dot math — standard for GT420-class desktop thermal printers. */
    private static final int ZPL_DPI = 203;

    /** Connect/read timeout for the raw ZPL socket, so an unreachable printer fails fast rather than hanging the request. */
    private static final int PRINTER_SOCKET_TIMEOUT_MS = 3000;

    /** Short institution tag printed on every label — SKSCON's official short form, not the full college name, since label space is extremely tight. */
    private static final String INSTITUTION_LABEL = "SKSCON";

    private static final Pattern IPV4_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    private final LibrarySettingRepository settingRepository;

    public LibraryBarcodeService(LibrarySettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /** @param shelfLocation formatted rack/shelf display string (e.g. "C3 / R2"), or null when the item has no shelf assignment (always null for periodicals — BR-35's rack/shelf hierarchy is book-only). */
    public record LabelItem(String code, String title, String accessionNumber, String shelfLocation) {}

    /**
     * Renders a single barcode onto a canvas sized exactly to the configured label
     * dimensions (mm, converted to pixels at {@link #PNG_DPI}), laid out as institution
     * header / barcode / truncated title / accession+shelf footer, so the PNG is a
     * physically accurate preview/print of the sticker, not just a bare barcode.
     */
    public byte[] generateBarcodePng(LabelItem item) throws IOException {
        int widthMm = getSettingInt("barcode_label_width_mm", 50);
        int heightMm = getSettingInt("barcode_label_height_mm", 25);
        int canvasWidth = mmToPixels(widthMm);
        int canvasHeight = mmToPixels(heightMm);

        BufferedImage rawBarcode = renderRawBarcode(item.code());

        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = canvas.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, canvasWidth, canvasHeight);
        g2d.setColor(Color.BLACK);

        int padding = Math.max(mmToPixels(MIN_QUIET_ZONE_MM), canvasWidth / 40);
        int headerHeight = Math.max(14, canvasHeight / 9);
        int titleHeight  = Math.max(14, canvasHeight / 9);
        int footerHeight = Math.max(18, canvasHeight / 6);
        int drawableWidth = Math.max(1, canvasWidth - 2 * padding);
        int drawableHeight = Math.max(1, canvasHeight - headerHeight - titleHeight - footerHeight - 2 * padding);

        int y = padding;

        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(9, headerHeight - 4)));
        drawCentered(g2d, INSTITUTION_LABEL, canvasWidth, y, headerHeight);
        y += headerHeight;

        double scale = Math.min(
            (double) drawableWidth / rawBarcode.getWidth(),
            (double) drawableHeight / rawBarcode.getHeight());
        int scaledWidth = (int) Math.round(rawBarcode.getWidth() * scale);
        int scaledHeight = (int) Math.round(rawBarcode.getHeight() * scale);
        int bx = (canvasWidth - scaledWidth) / 2;
        g2d.drawImage(rawBarcode, bx, y, scaledWidth, scaledHeight, null);
        y += drawableHeight;

        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(9, titleHeight - 4)));
        drawCentered(g2d, fitToWidth(g2d, item.title(), drawableWidth), canvasWidth, y, titleHeight);
        y += titleHeight;

        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(11, footerHeight - 6)));
        drawCentered(g2d, footerLine(item), canvasWidth, y, footerHeight);

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
        int widthMm = getSettingInt("barcode_label_width_mm", 50);
        float widthPt = mmToPoints(widthMm);
        float heightPt = mmToPoints(getSettingInt("barcode_label_height_mm", 25));

        float margin = 20f;
        float pageWidth = PageSize.A4.getWidth() - (2 * margin);
        int columns = Math.max(1, Math.min(items.size(), (int) (pageWidth / widthPt)));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, margin, margin, margin, margin);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            com.lowagie.text.Font headerFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 6);
            com.lowagie.text.Font titleFont  = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 6);
            com.lowagie.text.Font footerFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 7);

            PdfPTable table = new PdfPTable(columns);
            table.setTotalWidth(columns * widthPt);
            table.setLockedWidth(true);

            float cellPadding = mmToPoints(MIN_QUIET_ZONE_MM);
            float headerSpace = 9f;
            float titleSpace = 9f;
            float footerSpace = 11f;
            float barcodeMaxWidth = widthPt - (2 * cellPadding);
            float barcodeMaxHeight = heightPt - (2 * cellPadding) - headerSpace - titleSpace - footerSpace;

            for (LabelItem item : items) {
                PdfPCell cell = new PdfPCell();
                cell.setFixedHeight(heightPt);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(cellPadding);

                Paragraph header = new Paragraph(INSTITUTION_LABEL, headerFont);
                header.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(header);

                Barcode128 barcode128 = new Barcode128();
                barcode128.setCode(item.code());
                barcode128.setBarHeight(Math.max(8f, barcodeMaxHeight));
                com.lowagie.text.Image barcodeImage = barcode128.createImageWithBarcode(writer.getDirectContent(), null, null);
                barcodeImage.scaleToFit(barcodeMaxWidth, barcodeMaxHeight);
                barcodeImage.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(barcodeImage);

                Paragraph title = new Paragraph(truncateChars(item.title(), titleMaxChars(widthMm)), titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(title);

                Paragraph footer = new Paragraph(footerLine(item), footerFont);
                footer.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(footer);

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
        int labelsPerRow = Math.max(1, getSettingInt("barcode_labels_per_row", 1));
        return buildZplRow(List.of(item), widthMm, heightMm, labelsPerRow);
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
            zpl.append(buildZplRow(new ArrayList<>(row), widthMm, heightMm, labelsPerRow));
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
     * One {@code ^XA...^XZ} job for one physical "row" as the printer's gap sensor sees it —
     * the print area is always exactly the configured single-label footprint
     * ({@code singleLabelWidthMm × heightMm}); it never grows with {@code slotsPerRow}. What
     * changes is how that fixed footprint is subdivided: {@code slotsPerRow} labels are packed
     * side by side, cross-web, each getting {@code 1/slotsPerRow} of both the width AND the
     * height — side-by-side sub-labels share the same feed-direction gap, so they're always
     * scaled down together, not just narrowed. {@code slotsPerRow} is the physical media's
     * fixed layout (how many columns are actually die-cut across the roll), independent of how
     * many real items are being printed right now — {@code rowItems} may be shorter than
     * {@code slotsPerRow} (a batch's trailing partial row, or a single on-demand print onto
     * multi-up media), in which case the remaining physical slots are simply left blank rather
     * than stretched to fill the row, since a single die-cut position can't be resized per job.
     */
    private String buildZplRow(List<LabelItem> rowItems, int singleLabelWidthMm, int heightMm, int slotsPerRow) {
        int rowWidthDots = mmToDots(singleLabelWidthMm);
        int rowHeightDots = mmToDots(heightMm);
        int cellWidthDots = Math.max(1, rowWidthDots / slotsPerRow);
        int cellHeightDots = Math.max(1, rowHeightDots / slotsPerRow);
        int quietZoneDots = Math.min(mmToDots(MIN_QUIET_ZONE_MM), Math.max(2, cellWidthDots / 10));

        int headerHeightDots = Math.max(10, cellHeightDots / 9);
        int titleHeightDots  = Math.max(10, cellHeightDots / 9);
        int footerHeightDots = Math.max(12, cellHeightDots / 6);
        int barHeightDots = Math.max(mmToDots(3f), cellHeightDots - headerHeightDots - titleHeightDots - footerHeightDots - 2 * quietZoneDots);
        int barcodeAreaWidthDots = Math.max(1, cellWidthDots - 2 * quietZoneDots);
        int cellWidthMm = Math.max(1, singleLabelWidthMm / slotsPerRow);

        StringBuilder zpl = new StringBuilder();
        zpl.append("^XA\n");
        zpl.append("^PW").append(rowWidthDots).append('\n');
        zpl.append("^LL").append(rowHeightDots).append('\n');
        zpl.append("^CI28\n");

        for (int i = 0; i < rowItems.size(); i++) {
            LabelItem item = rowItems.get(i);
            int xOffset = i * cellWidthDots + quietZoneDots;
            int y = quietZoneDots;

            int headerFontSize = Math.max(10, headerHeightDots - 2);
            zpl.append("^FO").append(xOffset).append(',').append(y).append('\n');
            zpl.append("^A0N,").append(headerFontSize).append(',').append(headerFontSize).append('\n');
            zpl.append("^FB").append(barcodeAreaWidthDots).append(",1,0,C\n");
            zpl.append("^FD").append(escapeZpl(INSTITUTION_LABEL)).append("^FS\n");
            y += headerHeightDots;

            zpl.append("^FO").append(xOffset).append(',').append(y).append('\n');
            zpl.append("^BY2,3,").append(barHeightDots).append('\n');
            zpl.append("^BCN,").append(barHeightDots).append(",N,N,N\n");
            zpl.append("^FD").append(escapeZpl(item.code())).append("^FS\n");
            y += barHeightDots;

            int titleFontSize = Math.max(10, titleHeightDots - 2);
            zpl.append("^FO").append(xOffset).append(',').append(y).append('\n');
            zpl.append("^A0N,").append(titleFontSize).append(',').append(titleFontSize).append('\n');
            zpl.append("^FB").append(barcodeAreaWidthDots).append(",1,0,C\n");
            zpl.append("^FD").append(escapeZpl(truncateChars(item.title(), titleMaxChars(cellWidthMm)))).append("^FS\n");
            y += titleHeightDots;

            int footerFontSize = Math.max(11, footerHeightDots - 2);
            zpl.append("^FO").append(xOffset).append(',').append(y).append('\n');
            zpl.append("^A0N,").append(footerFontSize).append(',').append(footerFontSize).append('\n');
            zpl.append("^FB").append(barcodeAreaWidthDots).append(",1,0,C\n");
            zpl.append("^FD").append(escapeZpl(footerLine(item))).append("^FS\n");
        }

        zpl.append("^XZ\n");
        return zpl.toString();
    }

    /** The bottom-row caption shared by all three renderers: accession number, plus shelf location when the item has one (books only — periodicals have no BR-35 shelf assignment). */
    private static String footerLine(LabelItem item) {
        String accession = item.accessionNumber() != null ? item.accessionNumber() : item.code();
        String shelf = item.shelfLocation();
        return (shelf == null || shelf.isBlank()) ? accession : accession + "  ·  " + shelf;
    }

    /** Rough chars-per-label-width budget for the title row on the ZPL/PDF renderers, which truncate by character count rather than measured text width. */
    private static int titleMaxChars(int widthMm) {
        return Math.max(6, Math.round(widthMm * 0.9f));
    }

    private static String truncateChars(String value, int maxChars) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.length() <= maxChars) return trimmed;
        int cut = Math.max(1, maxChars - 1);
        return trimmed.substring(0, cut).trim() + "…";
    }

    /** Truncates to the exact pixel width available, using real font metrics — used only by the PNG renderer, which (unlike ZPL/PDF) has live glyph measurement on hand. */
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

    /** Horizontally centers `text` in the canvas and vertically centers it within the row spanning [rowTop, rowTop + rowHeight). No-op for blank text (e.g. an item with no shelf). */
    private static void drawCentered(Graphics2D g2d, String text, int canvasWidth, int rowTop, int rowHeight) {
        if (text == null || text.isBlank()) return;
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = Math.max(0, (canvasWidth - textWidth) / 2);
        int baselineY = rowTop + (rowHeight + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(text, x, baselineY);
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
