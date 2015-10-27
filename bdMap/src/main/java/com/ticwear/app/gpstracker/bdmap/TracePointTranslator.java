package com.ticwear.app.gpstracker.bdmap;

import com.baidu.location.BDLocation;
import com.baidu.mapapi.model.LatLng;
import com.ticwear.app.gpstracker.TracePoint;

/**
 * Created by tankery on 10/26/15.
 *
 * translator to translate platform point to common trace point
 */
public class TracePointTranslator {

    public static TracePoint from(BDLocation location) {
        TracePoint point = new TracePoint();

        point.latitude = location.getLatitude();
        point.longitude = location.getLongitude();
        point.speed = location.getSpeed();
        point.gpsStrength = String.format("%s, %.1f", locTypeString(location.getLocType()), location.getRadius());

        return point;
    }

    public static LatLng getPoint(TracePoint tracePoint) {
        return new LatLng(tracePoint.latitude, tracePoint.longitude);
    }

    public static String locTypeString(int locType) {
        if (locType == BDLocation.TypeGpsLocation){// GPS定位结果
            return "gps";
        } else if (locType == BDLocation.TypeNetWorkLocation){// 网络定位结果
            return "lbs";
        } else if (locType == BDLocation.TypeOffLineLocation) {// 离线定位结果
            return "offline";
        } else if (locType == BDLocation.TypeServerError) {
            return "Error: Server";
        } else if (locType == BDLocation.TypeNetWorkException) {
            return "Error: Network";
        } else if (locType == BDLocation.TypeCriteriaException) {
            return "Error: Criteria";
        }

        return "Error: Unknown";
    }

}
