package com.smvdu.mess.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

import com.smvdu.mess.database.DatabaseConnection;

public class MessStatsService {

    public static MessStats getStats(int messId, int month, int year) {

        // 1️⃣ Check cache first
        MessStats cached = MessStatsCache.get(messId, month, year);
        if (cached != null) {
            return cached;
        }

        // 2️⃣ Not cached → calculate
        MessStats calculated = calculate(messId, month, year);

        // 3️⃣ Store in cache
        MessStatsCache.put(messId, month, year, calculated);

        return calculated;
    }

    private static MessStats calculate(int messId, int month, int year) {

        int operatingDays = LocalDate.of(year, month, 1).lengthOfMonth();
        int activeStudents = 0;
        int totalAbsentDays = 0;
        double perDayRate = 120;
        double gstPercent = 5;

        try {
            Connection conn = DatabaseConnection.getConnection();

            // Operating days (if overridden)
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT operating_days FROM mess_operation_days WHERE mess_id=? AND month=? AND year=?"
            );
            ps.setInt(1, messId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) operatingDays = rs.getInt(1);

            // Active students
            ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM students WHERE hostel_id IN " +
                            "(SELECT id FROM hostels WHERE mess_id=?) AND is_active=1"
            );
            ps.setInt(1, messId);
            rs = ps.executeQuery();
            if (rs.next()) activeStudents = rs.getInt(1);

            // Absent days
            ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(absent_days),0) FROM student_attendance " +
                            "WHERE student_id IN " +
                            "(SELECT id FROM students WHERE hostel_id IN " +
                            "(SELECT id FROM hostels WHERE mess_id=?)) " +
                            "AND month=? AND year=?"
            );
            ps.setInt(1, messId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            rs = ps.executeQuery();
            if (rs.next()) totalAbsentDays = rs.getInt(1);

            // Rates
            rs = conn.createStatement().executeQuery(
                    "SELECT key,value FROM settings WHERE key IN ('per_day_rate','gst_percent')"
            );
            while (rs.next()) {
                if (rs.getString("key").equals("per_day_rate"))
                    perDayRate = Double.parseDouble(rs.getString("value"));
                if (rs.getString("key").equals("gst_percent"))
                    gstPercent = Double.parseDouble(rs.getString("value"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        int totalPossibleDays = operatingDays * activeStudents;
        int netMessDays = Math.max(0, totalPossibleDays - totalAbsentDays);
        double subtotal = netMessDays * perDayRate;
        double gst = subtotal * (gstPercent / 100);
        double total = subtotal + gst;

        return new MessStats(
                operatingDays,
                activeStudents,
                totalAbsentDays,
                netMessDays,
                total
        );
    }
}
