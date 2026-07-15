package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.model.LibrarySetting;
import com.cms.repository.LibrarySettingRepository;

@ExtendWith(MockitoExtension.class)
class LibraryBarcodeServiceTest {

    @Mock private LibrarySettingRepository settingRepository;

    private LibraryBarcodeService barcodeService;

    @BeforeEach
    void setUp() {
        barcodeService = new LibraryBarcodeService(settingRepository);
    }

    private void stubSetting(String key, String value) {
        LibrarySetting setting = new LibrarySetting();
        setting.setSettingValue(value);
        lenient().when(settingRepository.findBySettingKey(eq(key))).thenReturn(Optional.of(setting));
    }

    @Test
    void generateZplLabelSheet_groupsItemsIntoRowsOfConfiguredSize() {
        stubSetting("barcode_label_width_mm", "50");
        stubSetting("barcode_label_height_mm", "25");
        stubSetting("barcode_labels_per_row", "2");

        List<LibraryBarcodeService.LabelItem> items = List.of(
            new LibraryBarcodeService.LabelItem("CODE-1", "Book One", "ACC-1", "C1 / R1"),
            new LibraryBarcodeService.LabelItem("CODE-2", "Book Two", "ACC-2", null),
            new LibraryBarcodeService.LabelItem("CODE-3", "Book Three", "ACC-3", "C3 / R3"));

        String zpl = barcodeService.generateZplLabelSheet(items);

        // 3 items at 2-per-row => 2 rows (2 + 1), so 2 label jobs.
        assertThat(countOccurrences(zpl, "^XA")).isEqualTo(2);
        assertThat(countOccurrences(zpl, "^XZ")).isEqualTo(2);
        // One ^BC (barcode) block per item, regardless of row grouping.
        assertThat(countOccurrences(zpl, "^BCN,")).isEqualTo(3);
        assertThat(zpl).contains("^FDCODE-1^FS").contains("^FDCODE-2^FS").contains("^FDCODE-3^FS");
        // Institution header on every cell, and shelf appended to the footer only when present.
        assertThat(countOccurrences(zpl, "^FDSKSCON^FS")).isEqualTo(3);
        assertThat(zpl).contains("^FDACC-1  ·  C1 / R1^FS");
        assertThat(zpl).contains("^FDACC-2^FS");
        assertThat(zpl).doesNotContain("ACC-2  ·");
    }

    @Test
    void generateZplLabelSheet_rowFootprintStaysFixedRegardlessOfSlotCount() {
        stubSetting("barcode_label_width_mm", "40");
        stubSetting("barcode_label_height_mm", "25");
        stubSetting("barcode_labels_per_row", "2");

        List<LibraryBarcodeService.LabelItem> items = List.of(
            new LibraryBarcodeService.LabelItem("CODE-1", "Book One", "ACC-1", null),
            new LibraryBarcodeService.LabelItem("CODE-2", "Book Two", "ACC-2", null));

        String zpl = barcodeService.generateZplLabelSheet(items);

        // 40mm at 203dpi ~ 320 dots. "2 across" packs 2 sub-labels into that SAME fixed
        // row footprint — ^PW must never be multiplied by the slot count (i.e. never 640).
        assertThat(zpl).contains("^PW320\n");
        assertThat(zpl).doesNotContain("^PW640");
    }

    @Test
    void generateZplLabelSheet_partialRowLeavesRemainingSlotsBlankInsteadOfStretching() {
        stubSetting("barcode_label_width_mm", "40");
        stubSetting("barcode_label_height_mm", "25");
        stubSetting("barcode_labels_per_row", "2");

        // Only 1 real item on 2-across media: the second physical slot must stay blank,
        // not have the single item stretched to fill the whole fixed footprint.
        String zpl = barcodeService.generateZplLabelSheet(
            List.of(new LibraryBarcodeService.LabelItem("CODE-1", "Book One", "ACC-1", null)));

        assertThat(countOccurrences(zpl, "^BCN,")).isEqualTo(1);
        assertThat(zpl).contains("^PW320\n");
    }

    @Test
    void generateZpl_singleItemIsARowOfOne() {
        stubSetting("barcode_label_width_mm", "50");
        stubSetting("barcode_label_height_mm", "25");

        String zpl = barcodeService.generateZpl(new LibraryBarcodeService.LabelItem("CODE-1", "Book One", "ACC-1", "C1 / R1"));

        assertThat(countOccurrences(zpl, "^XA")).isEqualTo(1);
        assertThat(countOccurrences(zpl, "^XZ")).isEqualTo(1);
        assertThat(countOccurrences(zpl, "^BCN,")).isEqualTo(1);
        assertThat(zpl).contains("^FDSKSCON^FS");
        assertThat(zpl).contains("^FDACC-1  ·  C1 / R1^FS");
    }

    @Test
    void generateZpl_truncatesLongTitle() {
        stubSetting("barcode_label_width_mm", "40");
        stubSetting("barcode_label_height_mm", "25");

        String longTitle = "A Very Long Book Title That Will Not Fit On A Small Label At All";
        String zpl = barcodeService.generateZpl(new LibraryBarcodeService.LabelItem("CODE-1", longTitle, "ACC-1", null));

        assertThat(zpl).doesNotContain(longTitle);
        assertThat(zpl).contains("…^FS");
    }

    @Test
    void sendZpl_streamsExactBytesToConfiguredSocket() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            stubSetting("barcode_printer_ip", "127.0.0.1");
            stubSetting("barcode_printer_port", String.valueOf(serverSocket.getLocalPort()));

            String zpl = "^XA^FDHELLO^FS^XZ\n";
            CompletableFuture<byte[]> received = CompletableFuture.supplyAsync(() -> {
                try (var socket = serverSocket.accept()) {
                    return socket.getInputStream().readAllBytes();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            barcodeService.sendZpl(zpl);

            byte[] bytes = received.get();
            assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo(zpl);
        }
    }

    @Test
    void sendZpl_rejectsBlankIp() {
        stubSetting("barcode_printer_ip", "");

        assertThatThrownBy(() -> barcodeService.sendZpl("^XA^XZ"))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("No label printer IP is configured");
    }

    @Test
    void sendZpl_rejectsPublicIp() {
        stubSetting("barcode_printer_ip", "8.8.8.8");
        stubSetting("barcode_printer_port", "9100");

        assertThatThrownBy(() -> barcodeService.sendZpl("^XA^XZ"))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("not a private/local network address");
    }

    @Test
    void isPrivateNetworkAddress_acceptsRfc1918AndLoopbackOnly() {
        assertThat(LibraryBarcodeService.isPrivateNetworkAddress("10.0.0.5")).isTrue();
        assertThat(LibraryBarcodeService.isPrivateNetworkAddress("172.16.5.5")).isTrue();
        assertThat(LibraryBarcodeService.isPrivateNetworkAddress("192.168.1.100")).isTrue();
        assertThat(LibraryBarcodeService.isPrivateNetworkAddress("127.0.0.1")).isTrue();
        assertThat(LibraryBarcodeService.isPrivateNetworkAddress("8.8.8.8")).isFalse();
        assertThat(LibraryBarcodeService.isPrivateNetworkAddress("not-an-ip")).isFalse();
        assertThat(LibraryBarcodeService.isPrivateNetworkAddress("999.1.1.1")).isFalse();
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0, idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
