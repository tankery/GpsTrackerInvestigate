package com.ticwear.app.gpstracker.amap;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.ViewGroup;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.LocationManagerProxy;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.PolylineOptions;
import com.ticwear.app.gpstracker.TraceActivity;
import com.ticwear.app.gpstracker.TracePoint;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends TraceActivity implements AMapLocationListener, LocationSource {

    public static final String TAG = TraceActivity.TAG + "AMap";

    MapView mapView;
    AMap aMap;
    private OnLocationChangedListener locationChangedListener;

    // 定位相关
    private LocationManagerProxy aMapLocManager = null;
    // 是否停止定位服务
    boolean isStopLocClient = false;
    // 定时器相关，定时检查GPS是否开启（这里只须检查mLocationClient是否启动）
    Handler handler = new Handler();

    List<LatLng> pointsToDraw = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mapView.onCreate(savedInstanceState);

        if (aMapLocManager == null) {
            aMapLocManager = initLocation();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        mapView = null;
        super.onDestroy();
    }

    @Override
    protected void addMapInto(final ViewGroup parent) {
        mapView = new MapView(this);
        parent.addView(mapView);
        aMap = mapView.getMap();

        aMap.setOnMapLongClickListener(new AMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                addLogListViewInto(parent);
            }
        });
    }

    @Override
    protected void prepareTrace() {
        isStopLocClient = false;
        registerLocationListener();
        aMap.clear();
    }

    @Override
    protected void startTrace(TracePoint endpoint) {
        // 开启定位图层
        aMap.setMyLocationEnabled(true);

        if (!endpoint.isEmpty())
            drawStart(endpoint);
    }

    @Override
    protected void stopTrace(TracePoint endpoint) {
        stopLocationListener();
        isStopLocClient = true;
        // 关闭定位图层+
        aMap.setMyLocationEnabled(false);

        if (!endpoint.isEmpty())
            drawEnd(endpoint);
    }

    @Override
    protected void addTrace(List<TracePoint> tracePoints) {
        pointsToDraw.clear();
        for (TracePoint point : tracePoints) {
            if (!point.isEmpty())
                pointsToDraw.add(TracePointTranslator.getPoint(point));
        }
        PolylineOptions options = new PolylineOptions()
                .color(0xAAFF0000)
                .width(6)
                .addAll(pointsToDraw);
        aMap.addPolyline(options);
    }



    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

    @Override
    public void onLocationChanged(AMapLocation location) {
        logTheLocation(location);

        // map view 销毁后不在处理新接收的位置
        if (location == null || mapView == null)
            return;


        if (locationChangedListener != null) {
            locationChangedListener.onLocationChanged(location);// 显示系统小蓝点
        }

        TracePoint tracePoint = TracePointTranslator.from(location);
        addRawTracePoint(tracePoint);
    }

    @Override
    protected void onSnapshot() {
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        locationChangedListener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {
        locationChangedListener = null;
    }



    // 初始化定位
    private LocationManagerProxy initLocation() {
        LocationManagerProxy managerProxy = LocationManagerProxy.getInstance(this);

        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false

        return managerProxy;
    }

    private void registerLocationListener() {
        /*
         * mAMapLocManager.setGpsEnable(false);//
         * 1.0.2版本新增方法，设置true表示混合定位中包含gps定位，false表示纯网络定位，默认是true Location
         * API定位采用GPS和网络混合定位方式
         * ，第一个参数是定位provider，第二个参数时间最短是2000毫秒，第三个参数距离间隔单位是米，第四个参数是定位监听者
         */
        aMapLocManager.requestLocationData(
                LocationManagerProxy.GPS_PROVIDER, SCAN_SPAN, 10, this);
        aMapLocManager.setGpsEnable(true);
    }

    private void stopLocationListener() {
        if (aMapLocManager != null) {
            aMapLocManager.removeUpdates(this);
        }
    }

    private void destroyLocation() {
        stopLocationListener();
        if (aMapLocManager != null) {
            aMapLocManager.removeUpdates(this);
            aMapLocManager.destroy();
        }
        aMapLocManager = null;
    }


    /**
     * 绘制起点
     */
    private void drawStart(TracePoint endpoint) {
        LatLng avePoint = TracePointTranslator.getPoint(endpoint);
        CircleOptions options = new CircleOptions()
                .center(avePoint)
                .fillColor(0xAA00ff00)
                .strokeColor(0xAA00ff00)
                .strokeWidth(0)
                .radius(5);
        aMap.addCircle(options);
    }

    /**
     * 绘制终点。
     */
    private void drawEnd(TracePoint endpoint) {
        LatLng avePoint = TracePointTranslator.getPoint(endpoint);
        CircleOptions options = new CircleOptions()
                .center(avePoint)
                .fillColor(0xAAff00ff)
                .strokeColor(0xAAff00ff)
                .strokeWidth(0)
                .radius(5);
        aMap.addCircle(options);
    }



    private void logTheLocation(AMapLocation location) {
        if (location == null) {
            Log.w(TAG, "No location data!");
            return;
        }

        Double geoLat = location.getLatitude();
        Double geoLng = location.getLongitude();
        String cityCode = "";
        String desc = "";
        Bundle locBundle = location.getExtras();
        if (locBundle != null) {
            cityCode = locBundle.getString("citycode");
            desc = locBundle.getString("desc");
        }
        String str = ("定位成功:(" + geoLng + "," + geoLat + ")"
                + "\n精    度    :" + location.getAccuracy() + "米"
                + "\n定位方式:" + location.getProvider()
                + "\n城市编码:" + cityCode
                + "\n位置描述:" + desc
                + "\n省:" + location.getProvince()
                + "\n市:" + location.getCity()
                + "\n区(县):" + location.getDistrict()
                + "\n区域编码:" + location
                .getAdCode());
        Log.v(TAG, str);
    }
}
