package com.example.authride;

import androidx.appcompat.app.AppCompatActivity;

public class Routes extends AppCompatActivity {

    private String driverName;
    private String departureTime;
    private String startLocation;
    private String endLocation;

    public Routes(String driverName, String departureTime, String startLocation, String endLocation) {
        this.driverName = driverName;
        this.departureTime = departureTime;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
    }

    public String getDriverName() { return driverName; }
    public String getDepartureTime() { return departureTime; }
    public String getStartLocation() { return startLocation; }
    public String getEndLocation() { return endLocation; }
}
