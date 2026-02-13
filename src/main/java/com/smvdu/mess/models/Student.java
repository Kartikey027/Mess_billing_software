package com.smvdu.mess.models;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Student {
    private final IntegerProperty id;
    private final StringProperty entryNumber;
    private final StringProperty name;
    private final IntegerProperty hostelId;
    private final StringProperty roomNumber;
    private final StringProperty phone;
    private final StringProperty email;
    private final BooleanProperty isActive;
    private final IntegerProperty messDays;
    private final IntegerProperty absentDays;
    
    public Student(int id, String entryNumber, String name, int hostelId, 
                   String roomNumber, String phone, String email, boolean isActive) {
        this.id = new SimpleIntegerProperty(id);
        this.entryNumber = new SimpleStringProperty(entryNumber);
        this.name = new SimpleStringProperty(name);
        this.hostelId = new SimpleIntegerProperty(hostelId);
        this.roomNumber = new SimpleStringProperty(roomNumber);
        this.phone = new SimpleStringProperty(phone);
        this.email = new SimpleStringProperty(email);
        this.isActive = new SimpleBooleanProperty(isActive);
        this.messDays = new SimpleIntegerProperty(0);
        this.absentDays = new SimpleIntegerProperty(0);
    }
    
    // Property getters for TableView binding
    public IntegerProperty idProperty() { return id; }
    public StringProperty entryNumberProperty() { return entryNumber; }
    public StringProperty nameProperty() { return name; }
    public IntegerProperty hostelIdProperty() { return hostelId; }
    public StringProperty roomNumberProperty() { return roomNumber; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty emailProperty() { return email; }
    public BooleanProperty isActiveProperty() { return isActive; }
    public IntegerProperty messDaysProperty() { return messDays; }
    public IntegerProperty absentDaysProperty() { return absentDays; }
    
    // Standard getters
    public int getId() { return id.get(); }
    public String getEntryNumber() { return entryNumber.get(); }
    public String getName() { return name.get(); }
    public int getHostelId() { return hostelId.get(); }
    public String getRoomNumber() { return roomNumber.get(); }
    public String getPhone() { return phone.get(); }
    public String getEmail() { return email.get(); }
    public boolean isActive() { return isActive.get(); }
    public int getMessDays() { return messDays.get(); }
    public int getAbsentDays() { return absentDays.get(); }

    
    
    // Setters
    public void setMessDays(int days) { messDays.set(days); }
    public void setAbsentDays(int days) { absentDays.set(days); }
}
