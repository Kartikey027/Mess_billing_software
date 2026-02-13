package com.smvdu.mess.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.colors.DeviceRgb;
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
import com.smvdu.mess.models.Student;

public class StudentReportPDFGenerator {
    
    public static void generateStudentReport(
            String filePath,
            String universityName,
            String messName,
            String reportType,
            List<Student> students,
            int operatingDays,
            LocalDate reportDate) throws Exception {
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);
        
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        
        // ===== HEADER =====
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
                .setMarginBottom(15);
        document.add(addressLine);
        
        document.add(new LineSeparator(new SolidLine()));
        
        // ===== TITLE =====
        String title = "";
        switch (reportType) {
            case "ALL":
                title = "STUDENT ATTENDANCE REPORT - ALL STUDENTS";
                break;
            case "ABSENT":
                title = "STUDENT ATTENDANCE REPORT - ABSENT STUDENTS";
                break;
            case "PRESENT":
                title = "STUDENT ATTENDANCE REPORT - FULL ATTENDANCE";
                break;
        }
        
        Paragraph reportTitle = new Paragraph(title)
                .setFont(boldFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10)
                .setMarginBottom(10);
        document.add(reportTitle);
        
        // ===== INFO TABLE =====
        Table infoTable = new Table(new float[]{0.3f, 0.7f});
        infoTable.setWidth(UnitValue.createPercentValue(100));
        infoTable.setMarginBottom(15);
        
        addInfoRow(infoTable, "Mess:", messName, boldFont, regularFont);
        addInfoRow(infoTable, "Report Date:", reportDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), boldFont, regularFont);
        addInfoRow(infoTable, "Operating Days:", String.valueOf(operatingDays), boldFont, regularFont);
        addInfoRow(infoTable, "Total Students:", String.valueOf(students.size()), boldFont, regularFont);
        
        document.add(infoTable);
        document.add(new LineSeparator(new SolidLine()));
        
        // ===== STUDENT TABLE =====
        Table studentTable;
        
        if (reportType.equals("ALL")) {
            // All students: Entry Number, Name, Room, Mess Days, Absent Days
            studentTable = new Table(new float[]{0.2f, 0.3f, 0.15f, 0.175f, 0.175f});
            
            // Header row
            addHeaderCell(studentTable, "Entry Number", boldFont);
            addHeaderCell(studentTable, "Name", boldFont);
            addHeaderCell(studentTable, "Room", boldFont);
            addHeaderCell(studentTable, "Mess Days", boldFont);
            addHeaderCell(studentTable, "Absent Days", boldFont);
            
            // Data rows
            for (Student student : students) {
                addDataCell(studentTable, student.getEntryNumber(), regularFont);
                addDataCell(studentTable, student.getName(), regularFont);
                addDataCell(studentTable, student.getRoomNumber(), regularFont);
                addDataCell(studentTable, String.valueOf(student.getMessDays()), regularFont);
                addDataCell(studentTable, String.valueOf(student.getAbsentDays()), regularFont);
            }
            
        } else if (reportType.equals("ABSENT")) {
            // Absent students: Entry Number, Name, Room, Absent Days
            studentTable = new Table(new float[]{0.25f, 0.35f, 0.2f, 0.2f});
            
            addHeaderCell(studentTable, "Entry Number", boldFont);
            addHeaderCell(studentTable, "Name", boldFont);
            addHeaderCell(studentTable, "Room", boldFont);
            addHeaderCell(studentTable, "Absent Days", boldFont);
            
            for (Student student : students) {
                addDataCell(studentTable, student.getEntryNumber(), regularFont);
                addDataCell(studentTable, student.getName(), regularFont);
                addDataCell(studentTable, student.getRoomNumber(), regularFont);
                
                // Highlight absent days in red
                Cell absentCell = new Cell()
                        .add(new Paragraph(String.valueOf(student.getAbsentDays()))
                                .setFont(boldFont)
                                .setFontSize(10)
                                .setFontColor(new DeviceRgb(200, 0, 0)))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(6);
                studentTable.addCell(absentCell);
            }
            
        } else { // PRESENT
            // Present students: Entry Number, Name, Room, Mess Days
            studentTable = new Table(new float[]{0.25f, 0.35f, 0.2f, 0.2f});
            
            addHeaderCell(studentTable, "Entry Number", boldFont);
            addHeaderCell(studentTable, "Name", boldFont);
            addHeaderCell(studentTable, "Room", boldFont);
            addHeaderCell(studentTable, "Mess Days", boldFont);
            
            for (Student student : students) {
                addDataCell(studentTable, student.getEntryNumber(), regularFont);
                addDataCell(studentTable, student.getName(), regularFont);
                addDataCell(studentTable, student.getRoomNumber(), regularFont);
                
                // Highlight perfect attendance in green
                Cell messCell = new Cell()
                        .add(new Paragraph(String.valueOf(student.getMessDays()))
                                .setFont(boldFont)
                                .setFontSize(10)
                                .setFontColor(new DeviceRgb(0, 150, 0)))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(6);
                studentTable.addCell(messCell);
            }
        }
        
        studentTable.setWidth(UnitValue.createPercentValue(100));
        studentTable.setMarginTop(15);
        document.add(studentTable);
        
        // ===== FOOTER =====
        document.add(new Paragraph("\n"));
        document.add(new LineSeparator(new SolidLine()));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");
        Paragraph footer = new Paragraph("Generated on: " + java.time.LocalDateTime.now().format(formatter))
                .setFont(regularFont)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);
        document.add(footer);
        
        Paragraph disclaimer = new Paragraph("This is a computer-generated report.")
                .setFont(regularFont)
                .setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(5)
                .setFontColor(new DeviceGray(0.5f));
        document.add(disclaimer);
        
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
    
    private static void addHeaderCell(Table table, String text, PdfFont font) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(10))
                .setBackgroundColor(new DeviceGray(0.8f))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(6);
        table.addHeaderCell(cell);
    }
    
    private static void addDataCell(Table table, String text, PdfFont font) {
        Cell cell = new Cell()
                .add(new Paragraph(text != null ? text : "").setFont(font).setFontSize(9))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(6);
        table.addCell(cell);
    }
}