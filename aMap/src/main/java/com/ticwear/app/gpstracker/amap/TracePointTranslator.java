package com.ticwear.app.gpstracker.amap;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.LocationManagerProxy;
import com.amap.api.maps2d.model.LatLng;
import com.ticwear.app.gpstracker.TracePoint;

/**
 * Created by tankery on 10/26/15.
 *
 * translator to translate platform point to common trace point
 */
public class TracePointTranslator {

    public static TracePoint from(AMapLocation location) {
        TracePoint point = new TracePoint();

        if (LocationManagerProxy.GPS_PROVIDER.equals(location.getProvider())) {
            point.latitude = location.getLatitude();
            point.longitude = location.getLongitude();
            point.speed = location.getSpeed();
            point.accuracy = location.getAccuracy();
        }

        return point;
    }

    public static LatLng getPoint(TracePoint tracePoint) {
        return new LatLng(tracePoint.latitude, tracePoint.longitude);
    }

}
