package com.smvdu.mess.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.smvdu.mess.database.DatabaseConnection;

public class MessUtils {
    
    // ===== EXISTING METHODS =====
    
    /**
     * Get operating days for a specific mess, month, and year
     * Falls back to calendar month length if not set
     */
    public static int getOperatingDays(int messId, int month, int year) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT operating_days FROM mess_operation_days " +
                "WHERE mess_id = ? AND month = ? AND year = ?"
            );
            ps.setInt(1, messId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("operating_days");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Fallback: actual days in the month
        return LocalDate.of(year, month, 1).lengthOfMonth();
    }
    
    /**
     * Save operating days for a specific mess, month, and year
     */
    public static boolean saveOperatingDays(int messId, int month, int year, int operatingDays) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO mess_operation_days " +
                "(mess_id, month, year, operating_days) " +
                "VALUES (?, ?, ?, ?)"
            );
            ps.setInt(1, messId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            ps.setInt(4, operatingDays);
            
            int result = ps.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get mess ID for a given hostel ID
     */
    public static int getMessIdForHostel(int hostelId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT mess_id FROM hostels WHERE id = ?"
            );
            ps.setInt(1, hostelId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("mess_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Fallback to hostel ID itself
        return hostelId;
    }
    
    /**
     * Get all hostel IDs that belong to a specific mess
     */
    public static List<Integer> getHostelIdsForMess(int messId) {
        List<Integer> hostelIds = new ArrayList<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM hostels WHERE mess_id = ?"
            );
            ps.setInt(1, messId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                hostelIds.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return hostelIds;
    }
    
    /**
     * Convert list of hostel IDs to comma-separated string for SQL IN clause
     * WARNING: Only use with internally generated IDs, not user input
     */
    public static String hostelIdsToString(List<Integer> hostelIds) {
        if (hostelIds.isEmpty()) {
            return "0"; // Invalid ID that won't match anything
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hostelIds.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(hostelIds.get(i));
        }
        return sb.toString();
    }
    
    /**
     * Get setting value by key
     */
    public static double getSetting(String key, double defaultValue) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT value FROM settings WHERE key = ?"
            );
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return Double.parseDouble(rs.getString("value"));
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
        
        return defaultValue;
    }
    
    /**
     * Update setting value
     */
    public static boolean updateSetting(String key, String value) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)"
            );
            ps.setString(1, key);
            ps.setString(2, value);
            
            int result = ps.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Calculate total absent days for a mess in a given month/year
     */
    public static int getTotalAbsentDays(List<Integer> hostelIds, int month, int year) {
        if (hostelIds.isEmpty()) return 0;
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            String query = "SELECT COALESCE(SUM(sa.absent_days), 0) as total_absent_days " +
                          "FROM students s " +
                          "LEFT JOIN student_attendance sa ON s.id = sa.student_id " +
                          "AND sa.month = ? AND sa.year = ? " +
                          "WHERE s.hostel_id IN (" + hostelIdsToString(hostelIds) + ") " +
                          "AND s.is_active = 1";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, month);
            ps.setInt(2, year);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("total_absent_days");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Get active student count for hostels
     */
    public static int getActiveStudentCount(List<Integer> hostelIds) {
        if (hostelIds.isEmpty()) return 0;
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            String query = "SELECT COUNT(*) FROM students " +
                          "WHERE hostel_id IN (" + hostelIdsToString(hostelIds) + ") " +
                          "AND is_active = 1";
            
            ResultSet rs = conn.createStatement().executeQuery(query);
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Get total student count (including inactive) for hostels
     */
    public static int getTotalStudentCount(List<Integer> hostelIds) {
        if (hostelIds.isEmpty()) return 0;
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            String query = "SELECT COUNT(*) FROM students " +
                          "WHERE hostel_id IN (" + hostelIdsToString(hostelIds) + ")";
            
            ResultSet rs = conn.createStatement().executeQuery(query);
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    // ===== NEW METHODS FOR BILL CONFIGURATION =====
    
    /**
     * Bill configuration class to hold dates and fine
     */
    public static class BillConfig {
        public LocalDate startDate;
        public LocalDate endDate;
        public int operatingDays;
        public double fineAmount;
        
        public BillConfig(LocalDate startDate, LocalDate endDate, int operatingDays, double fineAmount) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.operatingDays = operatingDays;
            this.fineAmount = fineAmount;
        }
    }
    
    /**
     * Save bill configuration (dates, operating days, and fine amount)
     */
    public static boolean saveBillConfig(int messId, int month, int year, 
                                         LocalDate startDate, LocalDate endDate, 
                                         int operatingDays, double fineAmount) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO bill_configurations " +
                "(mess_id, month, year, start_date, end_date, operating_days, fine_amount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)"
            );
            ps.setInt(1, messId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            ps.setString(4, startDate.toString());
            ps.setString(5, endDate.toString());
            ps.setInt(6, operatingDays);
            ps.setDouble(7, fineAmount);
            
            int result = ps.executeUpdate();
            
            // Also update operating days in the separate table for backward compatibility
            saveOperatingDays(messId, month, year, operatingDays);
            
            return result > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get bill configuration (dates, operating days, and fine amount)
     * Returns null if not configured yet
     */
    public static BillConfig getBillConfig(int messId, int month, int year) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT start_date, end_date, operating_days, fine_amount " +
                "FROM bill_configurations " +
                "WHERE mess_id = ? AND month = ? AND year = ?"
            );
            ps.setInt(1, messId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                LocalDate startDate = LocalDate.parse(rs.getString("start_date"));
                LocalDate endDate = LocalDate.parse(rs.getString("end_date"));
                int operatingDays = rs.getInt("operating_days");
                double fineAmount = rs.getDouble("fine_amount");
                
                return new BillConfig(startDate, endDate, operatingDays, fineAmount);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null; // No configuration found
    }
    
    /**
     * Get fine amount for a specific mess/month/year
     */
    public static double getFineAmount(int messId, int month, int year) {
        BillConfig config = getBillConfig(messId, month, year);
        return config != null ? config.fineAmount : 0.0;
    }
}