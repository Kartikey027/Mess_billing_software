package com.smvdu.mess.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

public class BillPDFGenerator {
    
    public static void generateBillPDF(
            String filePath,
            String universityName,
            String hostelName,
            String hostelCode,
            String billPeriod,
            int daysInPeriod,
            int totalStudents,
            int totalStudentDays,
            int totalAbsentDays,
            int totalMessDays,
            double perDayRate,
            double subtotal,
            double gstPercent,
            double gstAmount,
            double totalAmount,
            double fineAmount,
            String preparedBy,
            LocalDate generatedDate) throws Exception {
        
        // Create PDF writer
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);
        
        // Set fonts
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        
        // ===== HEADER SECTION =====
        Paragraph universityHeader = new Paragraph(universityName)
                .setFont(boldFont)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(universityHeader);
        
        Paragraph addressLine = new Paragraph("Katra, Jammu & Kashmir - 182320")
                .setFont(regularFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2);
        document.add(addressLine);
        
        Paragraph phoneEmail = new Paragraph("Phone: +91-1991-251201 | Email: registrar@smvdu.ac.in")
                .setFont(regularFont)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(phoneEmail);
        
        // Separator line - FIXED: Added SolidLine parameter
        LineSeparator separator1 = new LineSeparator(new SolidLine());
        document.add(separator1);
        
        // ===== BILL TITLE =====
        Paragraph billTitle = new Paragraph("MESS BILL")
                .setFont(boldFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10)
                .setMarginBottom(10);
        document.add(billTitle);
        
        // ===== HOSTEL INFO =====
        Table hostelInfoTable = new Table(new float[]{0.5f, 0.5f});
        hostelInfoTable.setWidth(UnitValue.createPercentValue(100));
        hostelInfoTable.setMarginBottom(15);
        
        addInfoRow(hostelInfoTable, "Hostel Name:", hostelName, boldFont, regularFont);
        addInfoRow(hostelInfoTable, "Hostel Code:", hostelCode, boldFont, regularFont);
        addInfoRow(hostelInfoTable, "Billing Period:", billPeriod, boldFont, regularFont);
        
        document.add(hostelInfoTable);
        
        // Separator - FIXED
        document.add(new LineSeparator(new SolidLine()));
        
        // ===== BILL DETAILS TABLE =====
        Table detailsTable = new Table(new float[]{0.6f, 0.4f});
        detailsTable.setWidth(UnitValue.createPercentValue(100));
        detailsTable.setMarginTop(15);
        detailsTable.setMarginBottom(15);
        
        // Header row
        Cell headerLabelCell = new Cell()
                .add(new Paragraph("Description").setFont(boldFont).setFontSize(11))
                .setBackgroundColor(new DeviceGray(0.8f))
                .setPadding(8);
        detailsTable.addHeaderCell(headerLabelCell);
        
        Cell headerValueCell = new Cell()
                .add(new Paragraph("Value").setFont(boldFont).setFontSize(11))
                .setBackgroundColor(new DeviceGray(0.8f))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(8);
        detailsTable.addHeaderCell(headerValueCell);
        
        // Details rows
        addDetailRow(detailsTable, "Days in Billing Period", String.valueOf(daysInPeriod), regularFont);
        addDetailRow(detailsTable, "Total Students", String.valueOf(totalStudents), regularFont);
        addDetailRow(detailsTable, "Total Student-Days", String.valueOf(totalStudentDays), regularFont);
        addDetailRow(detailsTable, "Total Student Leave Days", String.valueOf(totalAbsentDays), regularFont);
        addDetailRow(detailsTable, "Net Mess Days (Chargeable)", String.valueOf(totalMessDays), regularFont, true);
        addDetailRow(detailsTable, "Per Day Rate", "₹" + String.format("%.2f", perDayRate), regularFont);
        
        document.add(detailsTable);
        
        // Separator - FIXED
        document.add(new LineSeparator(new SolidLine()));
        
        // ===== FINANCIAL SUMMARY =====
        Table financialTable = new Table(new float[]{0.6f, 0.4f});
        financialTable.setWidth(UnitValue.createPercentValue(100));
        financialTable.setMarginTop(15);
        financialTable.setMarginBottom(20);
          if (fineAmount > 0) {
        addFinancialRow(financialTable, "Fine Amount", "₹" + String.format("%.2f", fineAmount), regularFont);
    }
        
        addFinancialRow(financialTable, "Subtotal", "₹" + String.format("%.2f", subtotal), regularFont);
        addFinancialRow(financialTable, "GST (" + String.format("%.1f%%", gstPercent) + ")", "₹" + String.format("%.2f", gstAmount), regularFont);
        
        // Total row
        Cell totalLabelCell = new Cell()
                .add(new Paragraph("TOTAL AMOUNT DUE").setFont(boldFont).setFontSize(12))
                .setBackgroundColor(new DeviceGray(0.9f))
                .setPadding(10);
        financialTable.addCell(totalLabelCell);
        
        Cell totalValueCell = new Cell()
                .add(new Paragraph("₹" + String.format("%.2f", totalAmount))
                        .setFont(boldFont)
                        .setFontSize(12))
                .setBackgroundColor(new DeviceGray(0.9f))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(10);
        financialTable.addCell(totalValueCell);
        
        document.add(financialTable);
        
        // ===== FOOTER SECTION =====
        document.add(new Paragraph("\n")); // Space
        document.add(new LineSeparator(new SolidLine())); // FIXED
        document.add(new Paragraph("\n")); // Space
        
        // Signature section
        Table signatureTable = new Table(new float[]{0.33f, 0.33f, 0.33f});
        signatureTable.setWidth(UnitValue.createPercentValue(100));
        signatureTable.setMarginTop(20);
        
        Cell preparedCell = new Cell()
                .add(new Paragraph("\n\n" + preparedBy)
                        .setFont(boldFont)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("_______________")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(10))
                .add(new Paragraph("Prepared by: Caretaker")
                        .setFont(regularFont)
                        .setFontSize(9)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(null);
        signatureTable.addCell(preparedCell);
        
        Cell verifiedCell = new Cell()
                .add(new Paragraph("\n\n\n")
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("_______________")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(10))
                .add(new Paragraph("Verified by: Admin")
                        .setFont(regularFont)
                        .setFontSize(9)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(null);
        signatureTable.addCell(verifiedCell);
        
        Cell approvedCell = new Cell()
                .add(new Paragraph("\n\n\n")
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("_______________")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(10))
                .add(new Paragraph("Approved by: Authority")
                        .setFont(regularFont)
                        .setFontSize(9)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(null);
        signatureTable.addCell(approvedCell);
        
        document.add(signatureTable);
        
        // Generation date
        document.add(new Paragraph("\n"));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");
        Paragraph dateFooter = new Paragraph("Generated on: " + LocalDateTime.now().format(formatter))
                .setFont(regularFont)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20);
        document.add(dateFooter);
        
        Paragraph footer = new Paragraph("This is a computer-generated bill and does not require a signature.")
                .setFont(regularFont)
                .setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(5)
                .setFontColor(new DeviceGray(0.5f));
        document.add(footer);
        
        // Close document
        document.close();
    }
    
    private static void addInfoRow(Table table, String label, String value, PdfFont boldFont, PdfFont regularFont) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label).setFont(boldFont).setFontSize(10))
                .setBorder(null)
                .setPadding(3);
        table.addCell(labelCell);
        
        Cell valueCell = new Cell()
                .add(new Paragraph(value).setFont(regularFont).setFontSize(10))
                .setBorder(null)
                .setPadding(3);
        table.addCell(valueCell);
    }
    
    private static void addDetailRow(Table table, String label, String value, PdfFont font) {
        addDetailRow(table, label, value, font, false);
    }
    
    private static void addDetailRow(Table table, String label, String value, PdfFont font, boolean highlight) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label).setFont(font).setFontSize(10))
                .setPadding(6);
        if (highlight) {
            labelCell.setBackgroundColor(new DeviceGray(0.95f));
        }
        table.addCell(labelCell);
        
        Cell valueCell = new Cell()
                .add(new Paragraph(value).setFont(font).setFontSize(10))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(6);
        if (highlight) {
            valueCell.setBackgroundColor(new DeviceGray(0.95f));
        }
        table.addCell(valueCell);
    }
    
    private static void addFinancialRow(Table table, String label, String value, PdfFont font) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label).setFont(font).setFontSize(11))
                .setPadding(8);
        table.addCell(labelCell);
        
        Cell valueCell = new Cell()
                .add(new Paragraph(value).setFont(font).setFontSize(11))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(8);
        table.addCell(valueCell);
    }
}
