package com.revpay.transaction_service.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.revpay.transaction_service.dto.TransactionDto;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class TransactionExportService {

    public byte[] exportToCsv(List<TransactionDto> transactions) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8),
                        CSVFormat.DEFAULT.builder().setHeader("Transaction ID", "Reference", "Date", "Description",
                                "Type", "Amount", "Status", "Sender ID", "Receiver ID").build())) {

            for (TransactionDto txn : transactions) {
                csvPrinter.printRecord(
                        txn.getTransactionId(),
                        txn.getTransactionRef(),
                        txn.getTimestamp(),
                        txn.getDescription(),
                        txn.getType(),
                        txn.getAmount(),
                        txn.getStatus(),
                        txn.getSenderId(),
                        txn.getReceiverId());
            }
            csvPrinter.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV file", e);
        }
    }

    public byte[] exportToPdf(List<TransactionDto> transactions) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Transaction History", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1.5f, 2f, 2f, 1f, 1.5f, 1.5f, 1f });

            addTableHeader(table);

            for (TransactionDto txn : transactions) {
                table.addCell(String.valueOf(txn.getTransactionRef()));
                table.addCell(String.valueOf(txn.getTimestamp()));
                table.addCell(txn.getDescription() != null ? txn.getDescription() : "");
                table.addCell(String.valueOf(txn.getType()));
                table.addCell(String.valueOf(txn.getAmount()));
                table.addCell(String.valueOf(txn.getStatus()));

                String parties = "S:" + txn.getSenderId() + " R:" + txn.getReceiverId();
                table.addCell(parties);
            }

            document.add(table);
            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF file", e);
        }
    }

    private void addTableHeader(PdfPTable table) {
        String[] headers = { "Ref", "Date", "Desc", "Type", "Amount", "Status", "Parties" };
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

        for (String header : headers) {
            PdfPCell cell = new PdfPCell();
            cell.setPhrase(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }
}
