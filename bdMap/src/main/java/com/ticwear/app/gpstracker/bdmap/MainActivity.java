package com.ticwear.app.gpstracker.bdmap;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ViewGroup;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.DotOptions;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.ticwear.app.gpstracker.TraceActivity;
import com.ticwear.app.gpstracker.TracePoint;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends TraceActivity implements BDLocationListener {

    final static String LOG_TAG = "BaiduMapTracer";

    MapView mapView;
    BaiduMap baiduMap;

    // 定位相关
    private LocationClient locationClient;
    // 是否停止定位服务
    boolean isStopLocClient = false;
    // 定时器相关，定时检查GPS是否开启（这里只须检查mLocationClient是否启动）
    Handler handler = new Handler();

    List<LatLng> pointsToDraw = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (locationClient == null) {
            locationClient = initLocation();
        }
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
    protected void addMapInto(ViewGroup parent) {
        mapView = new MapView(this);
        parent.addView(mapView);
        baiduMap = mapView.getMap();
    }

    @Override
    protected void prepareTrace() {
        // 启动计时器(每3秒检测一次)
        handler.postDelayed(activeCheckRunnable, ALIVE_CHECK);

        isStopLocClient = false;
        locationClient.registerLocationListener(this);
        if (!locationClient.isStarted()) {
            locationClient.start();
        }
        baiduMap.clear();
    }

    @Override
    protected void startTrace(TracePoint endpoint) {
        // 开启定位图层
        baiduMap.setMyLocationEnabled(true);
        LatLng ll = new LatLng(endpoint.latitude, endpoint.longitude);
        MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
        baiduMap.animateMapStatus(u);

        drawStart(endpoint);
    }

    @Override
    protected void stopTrace(TracePoint endpoint) {
        if (locationClient.isStarted())
            locationClient.stop();
        locationClient.unRegisterLocationListener(this);
        isStopLocClient = true;
        // 关闭定位图层+
        baiduMap.setMyLocationEnabled(false);

        handler.removeCallbacks(activeCheckRunnable);

        drawEnd(endpoint);
    }

    @Override
    protected void addTrace(List<TracePoint> tracePoints) {
        pointsToDraw.clear();
        for (TracePoint point : tracePoints) {
            pointsToDraw.add(TracePointTranslator.getPoint(point));
        }
        OverlayOptions options = new PolylineOptions()
                .color(0xAAFF0000)
                .width(6)
                .points(pointsToDraw);
        baiduMap.addOverlay(options);
    }



    @Override
    public void onReceiveLocation(BDLocation location) {
        logTheLocation(location);

        // map view 销毁后不在处理新接收的位置
        if (location == null || mapView == null)
            return;

        // 如果不显示定位精度圈，将accuracy赋值为0即可
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(location.getRadius())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();

        baiduMap.setMyLocationData(locData);

        TracePoint tracePoint = TracePointTranslator.from(location);
        addRawTracePoint(tracePoint);
    }


    // 初始化定位
    public LocationClient initLocation() {
        // 定位初始化
        LocationClient client = new LocationClient(this);

        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);// 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan((int) SCAN_SPAN);

        // 设置定位方式的优先级。
        // 当gps可用，而且获取了定位结果时，不再发起网络请求，直接返回给用户坐标。这个选项适合希望得到准确坐标位置的用户。如果gps不可用，再发起网络请求，进行定位。
        option.setPriority(LocationClientOption.GpsFirst);
        // option.setPriority(LocationClientOption.NetWorkFirst);
        client.setLocOption(option);

        return client;
    }



    /**
     * 绘制起点
     */
    public void drawStart(TracePoint endpoint) {
        LatLng avePoint = TracePointTranslator.getPoint(endpoint);
        OverlayOptions options = new DotOptions()
                .center(avePoint)
                .color(0xAA00ff00)
                .radius(15);
        baiduMap.addOverlay(options);
    }

    /**
     * 绘制终点。
     */
    protected void drawEnd(TracePoint endpoint) {
        LatLng avePoint = TracePointTranslator.getPoint(endpoint);
        OverlayOptions options = new DotOptions()
                .center(avePoint)
                .color(0xAAff00ff)
                .radius(15);
        baiduMap.addOverlay(options);
    }



    Runnable activeCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (!locationClient.isStarted()) {
                Log.i(LOG_TAG, "Client not started, start it.");
                locationClient.start();
            }
            if (!isStopLocClient) {
                handler.postDelayed(this, ALIVE_CHECK);
            }
        }
    };

    private void logTheLocation(BDLocation location) {
        //Receive Location
        StringBuffer sb = new StringBuffer(256);
        sb.append("time : ");
        sb.append(location.getTime());
        sb.append("\nerror code : ");
        sb.append(location.getLocType());
        sb.append("\nlatitude : ");
        sb.append(location.getLatitude());
        sb.append("\nlontitude : ");
        sb.append(location.getLongitude());
        sb.append("\nradius : ");
        sb.append(location.getRadius());
        sb.append("\nspeed : ");
        sb.append(location.getSpeed());
        if (location.getLocType() == BDLocation.TypeGpsLocation){// GPS定位结果
            sb.append("\nspeed : ");
            sb.append(location.getSpeed());// 单位：公里每小时
            sb.append("\nsatellite : ");
            sb.append(location.getSatelliteNumber());
            sb.append("\nheight : ");
            sb.append(location.getAltitude());// 单位：米
            sb.append("\ndirection : ");
            sb.append(location.getDirection());// 单位度
            sb.append("\naddr : ");
            sb.append(location.getAddrStr());
            sb.append("\ndescribe : ");
            sb.append("gps定位成功");

        } else if (location.getLocType() == BDLocation.TypeNetWorkLocation){// 网络定位结果
            sb.append("\naddr : ");
            sb.append(location.getAddrStr());
            //运营商信息
            sb.append("\noperationers : ");
            sb.append(location.getOperators());
            sb.append("\ndescribe : ");
            sb.append("网络定位成功");
        } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
            sb.append("\ndescribe : ");
            sb.append("离线定位成功，离线定位结果也是有效的");
        } else if (location.getLocType() == BDLocation.TypeServerError) {
            sb.append("\ndescribe : ");
            sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
        } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
            sb.append("\ndescribe : ");
            sb.append("网络不同导致定位失败，请检查网络是否通畅");
        } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
            sb.append("\ndescribe : ");
            sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
        }
        sb.append("\nlocationdescribe : ");
        sb.append(location.getLocationDescribe());// 位置语义化信息
        List<Poi> list = location.getPoiList();// POI数据
        if (list != null) {
            sb.append("\npoilist size = : ");
            sb.append(list.size());
            for (Poi p : list) {
                sb.append("\npoi= : ");
                sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
            }
        }
        Log.v(LOG_TAG, sb.toString());
    }

}
