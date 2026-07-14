package com.cms.util.export;

import java.io.IOException;
import java.time.LocalDate;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Collapses the format-branch / filename / content-type-headers / exception-handling
 * block that was duplicated verbatim across every export controller endpoint.
 */
public final class ExportResponseFactory {

    private ExportResponseFactory() {}

    @FunctionalInterface
    public interface ByteSupplier {
        byte[] get() throws IOException;
    }

    public static ResponseEntity<byte[]> respond(String format, String baseFilename,
                                                  ByteSupplier excelSupplier, ByteSupplier pdfSupplier) {
        try {
            if ("pdf".equalsIgnoreCase(format)) {
                byte[] bytes = pdfSupplier.get();
                String filename = baseFilename + "-" + LocalDate.now() + ".pdf";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            } else {
                byte[] bytes = excelSupplier.get();
                String filename = baseFilename + "-" + LocalDate.now() + ".xlsx";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
