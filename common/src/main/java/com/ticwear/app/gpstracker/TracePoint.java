package com.ticwear.app.gpstracker;

/**
 * Created by tankery on 10/26/15.
 *
 * Trace point with location, distance, speed.
 */
public class TracePoint {

    public double longitude;
    public double latitude;

    public double distance; // in meter
    public double speed;    // in m/s

    public float accuracy;

    public TracePoint() {
        longitude = 0;
        latitude = 0;
        accuracy = -1;
    }

    public boolean isEmpty() {
        return accuracy == -1;
    }

    public boolean isValid() { return accuracy > 0; }

}
