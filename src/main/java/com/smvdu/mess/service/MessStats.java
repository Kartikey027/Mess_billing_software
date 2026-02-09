package com.smvdu.mess.service;

public class MessStats {

    private final int operatingDays;
    private final int activeStudents;
    private final int totalAbsentDays;
    private final int netMessDays;
    private final double estimatedBill;

    public MessStats(
            int operatingDays,
            int activeStudents,
            int totalAbsentDays,
            int netMessDays,
            double estimatedBill
    ) {
        this.operatingDays = operatingDays;
        this.activeStudents = activeStudents;
        this.totalAbsentDays = totalAbsentDays;
        this.netMessDays = netMessDays;
        this.estimatedBill = estimatedBill;
    }

    public int getOperatingDays() {
        return operatingDays;
    }

    public int getActiveStudents() {
        return activeStudents;
    }

    public int getTotalAbsentDays() {
        return totalAbsentDays;
    }

    public int getNetMessDays() {
        return netMessDays;
    }

    public double getEstimatedBill() {
        return estimatedBill;
    }
}
