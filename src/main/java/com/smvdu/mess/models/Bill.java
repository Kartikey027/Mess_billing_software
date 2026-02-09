package com.smvdu.mess.models;

import java.time.LocalDateTime;

public class Bill {
    private int id;
    private int hostelId;
    private String hostelName;
    private int month;
    private int year;
    private int totalStudents;
    private int totalMessDays;
    private double perDayRate;
    private double subtotal;
    private double gstPercent;
    private double gstAmount;
    private double totalAmount;
    private double fineAmount;
    private LocalDateTime generatedAt;
    
    // Constructor
    public Bill(int hostelId, int month, int year, int totalStudents, 
                int totalMessDays, double perDayRate, double gstPercent, double fineAmount) {
        this.hostelId = hostelId;
        this.month = month;
        this.year = year;
        this.totalStudents = totalStudents;
        this.totalMessDays = totalMessDays;
        this.perDayRate = perDayRate;
        this.gstPercent = gstPercent;
        this.fineAmount=fineAmount;
        calculateBill();
    }
    
    private void calculateBill() {
        this.subtotal = totalMessDays * perDayRate;
        this.gstAmount = subtotal * (gstPercent / 100);
        this.totalAmount = subtotal + gstAmount+ fineAmount;
    }
    
    // Getters
    public int getId() { return id; }
    public int getHostelId() { return hostelId; }
    public String getHostelName() { return hostelName; }
    public int getMonth() { return month; }
    public int getYear() { return year; }
    public int getTotalStudents() { return totalStudents; }
    public int getTotalMessDays() { return totalMessDays; }
    public double getPerDayRate() { return perDayRate; }
    public double getSubtotal() { return subtotal; }
    public double getGstPercent() { return gstPercent; }
    public double getGstAmount() { return gstAmount; }
    public double getTotalAmount() { return totalAmount; }
    public double getFineAmount() {return fineAmount;}
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setHostelName(String hostelName) { this.hostelName = hostelName; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public void setFineAmount(double fineAmount) {this.fineAmount = fineAmount;}
}
