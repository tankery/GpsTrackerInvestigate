package com.ticwear.app.gpstracker.bdmap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.DotOptions;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.ticwear.app.gpstracker.TracePoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by tankery on 11/7/15.
 *
 * Helper to add overlay to map & zoom & fit container.
 */
public class MapOverlayHelper {

    public static final float BEST_RUNNING_ZOOM_LEVEL = 18.5f;    // Support level is [3 - 20]

    public static final float[] BEST_TRACE_MARGINS = {
            0.1f, 0.1f, 0.1f, 0.1f
    };

    public static final int Z_INDEX_TRACE_BASE = 10;
    public static final int Z_INDEX_TRACE_PATH = Z_INDEX_TRACE_BASE;
    public static final int Z_INDEX_TRACE_START = Z_INDEX_TRACE_BASE + 1;
    public static final int Z_INDEX_TRACE_END = Z_INDEX_TRACE_BASE + 2;

    public static class TraceParam {

        @DrawableRes
        private int startPointDrawable;
        @DrawableRes
        private int endPointDrawable;
        @ColorRes
        private int traceLighterColor;
        @ColorRes
        private int traceDarkerColor;
        private float[] traceMargins;      // percentage in map coordinate.

        public TraceParam() {
            startPointDrawable = -1;
            endPointDrawable = -1;
            traceLighterColor = R.color.health_base_blue_light;
            traceDarkerColor = R.color.health_base_blue_dark;
            traceMargins = BEST_TRACE_MARGINS;
        }

        public TraceParam endPointDrawable(@DrawableRes int start, @DrawableRes int end) {
            startPointDrawable = start;
            endPointDrawable = end;

            return this;
        }

        public TraceParam traceColor(@ColorRes int lighter, @ColorRes int darker) {
            traceLighterColor = lighter;
            traceDarkerColor = darker;

            return this;
        }

        public TraceParam margins(float top, float left, float bottom, float right) {
            traceMargins = new float[] {
                top, left, bottom, right
            };

            return this;
        }
    }


    private Resources resources;
    private TraceParam traceParam;
    private TracePoint lastSportPoint;
    private LatLngBounds bestLatLngBounds;

    public MapOverlayHelper(Context context) {
        this.resources = context.getResources();
        traceParam = new TraceParam();
    }

    public void setTraceParam(TraceParam traceParam) {
        this.traceParam = traceParam;
    }

    public void updateOverlay(BaiduMap baiduMap, List<TracePoint> points, boolean isEnd) {
        if (points.isEmpty() || baiduMap == null)
            return;

        TracePoint first = getFirstValidPoint(points);
        TracePoint last = getLastValidPoint(points);

        if (first == null)
            return;

        baiduMap.clear();

        // Draw trace below
        if (first != last) {
            drawTrace(baiduMap, points);
        }
        // Draw points upper
        drawStart(baiduMap, first);
        drawEnd(baiduMap, last, isEnd);

        lastSportPoint = last;
        bestLatLngBounds = generateBestBounds(points);

        updateToBestLocation(baiduMap, isEnd);
    }

    public void updateToBestLocation(BaiduMap baiduMap, boolean isEnd) {
        if (baiduMap == null)
            return;

        MapStatusUpdate statusUpdate = getBestMapStatus(isEnd);
        if (statusUpdate != null) {
            baiduMap.animateMapStatus(statusUpdate);
        }
    }

    /**
     * 绘制起点
     */
    protected void drawStart(BaiduMap baiduMap, TracePoint startPoint) {
        if (startPoint == null || !startPoint.isValid())
            return;
        LatLng avePoint = translate(startPoint);
        OverlayOptions options;
        if (traceParam.startPointDrawable > 0) {
            BitmapDescriptor bitmap = BitmapDescriptorFactory
                    .fromResource(traceParam.startPointDrawable);
            options = new MarkerOptions()
                    .position(avePoint)
                    .anchor(0.5f, 0.5f)
                    .draggable(false)
                    .zIndex(Z_INDEX_TRACE_START)
                    .icon(bitmap);
        } else {
            int colorPoint = resources.getColor(traceParam.traceDarkerColor);
            options = new DotOptions()
                    .center(avePoint)
                    .color(colorPoint)
                    .radius(15)
                    .zIndex(Z_INDEX_TRACE_START);
        }
        baiduMap.addOverlay(options);
    }

    /**
     * 绘制终点。
     */
    protected void drawEnd(BaiduMap baiduMap, TracePoint endpoint, boolean isEnd) {
        if (endpoint == null || !endpoint.isValid())
            return;
        LatLng avePoint = translate(endpoint);

        if (traceParam.endPointDrawable > 0) {
            BitmapDescriptor bitmap = BitmapDescriptorFactory
                    .fromResource(traceParam.endPointDrawable);
            OverlayOptions options = new MarkerOptions()
                    .position(avePoint)
                    .anchor(0.5f, 0.5f)
                    .draggable(false)
                    .zIndex(Z_INDEX_TRACE_END)
                    .icon(bitmap);
            baiduMap.addOverlay(options);
        } else {
            int colorPoint = resources.getColor(traceParam.traceDarkerColor);
            OverlayOptions options;
            if (!isEnd) {
                // When we are running (not end), draw a transparent circle in end point to indicate
                // current position.
                int transparentColor = Color.argb(102,
                        Color.red(colorPoint), Color.green(colorPoint), Color.blue(colorPoint));
                options = new DotOptions()
                        .center(avePoint)
                        .color(transparentColor)
                        .radius(25)
                        .zIndex(Z_INDEX_TRACE_END);
                baiduMap.addOverlay(options);
            }
            options = new DotOptions()
                    .center(avePoint)
                    .color(colorPoint)
                    .radius(15)
                    .zIndex(Z_INDEX_TRACE_END);
            baiduMap.addOverlay(options);
        }
    }

    protected void drawTrace(BaiduMap baiduMap, List<TracePoint> tracePoints) {
        List<LatLng> pointsToDraw = new ArrayList<>();
        pointsToDraw.clear();
        for (TracePoint point : tracePoints) {
            if (point.isValid())
                pointsToDraw.add(translate(point));
        }

        if (pointsToDraw.size() < 2)
            return;

        int colorTrace = resources.getColor(traceParam.traceLighterColor);
        OverlayOptions options = new PolylineOptions()
                .color(colorTrace)
                .width(10)
                .points(pointsToDraw)
                .zIndex(Z_INDEX_TRACE_PATH);
        baiduMap.addOverlay(options);
    }


    private TracePoint getFirstValidPoint(List<TracePoint> points) {
        for (TracePoint point : points) {
            if (point.isValid())
                return point;
        }

        return null;
    }

    private TracePoint getLastValidPoint(List<TracePoint> points) {
        for (int i = points.size() - 1; i >= 0; i--) {
            TracePoint point = points.get(i);
            if (point.isValid())
                return point;
        }

        return null;
    }

    private LatLng translate(TracePoint point) {
        return new LatLng(point.latitude, point.longitude);
    }

    private LatLngBounds generateBestBounds(List<TracePoint> points) {
        Iterator<TracePoint> iterator = points.iterator();
        TracePoint first = null;
        while (iterator.hasNext() && (first == null || !first.isValid())) {
            first = iterator.next();
        }
        if (first == null)
            return null;

        double minLat = translate(first).latitude;
        double maxLat = translate(first).latitude;
        double minLng = translate(first).longitude;
        double maxLng = translate(first).longitude;

        for (TracePoint point : points) {
            if (!point.isValid()) {
                continue;
            }
            if (point.latitude < minLat) {
                minLat = point.latitude;
            }
            if (point.latitude > maxLat) {
                maxLat = point.latitude;
            }
            if (point.longitude < minLng) {
                minLng = point.longitude;
            }
            if (point.longitude > maxLng) {
                maxLng = point.longitude;
            }
        }

        double latDistance = (maxLat - minLat) > 0 ? (maxLat - minLat) : 0;
        double lngDistance = (maxLng - minLng) > 0 ? (maxLng - minLng) : 0;
        double marginTop = traceParam.traceMargins[0] * latDistance;
        double marginLeft = traceParam.traceMargins[1] * lngDistance;
        double marginBottom = traceParam.traceMargins[2] * latDistance;
        double marginRight = traceParam.traceMargins[3] * lngDistance;

        LatLng southwest = new LatLng(minLat - marginBottom, minLng - marginLeft);
        LatLng northeast = new LatLng(maxLat + marginTop, maxLng + marginRight);

        return new LatLngBounds.Builder()
                .include(southwest)
                .include(northeast)
                .build();
    }

    private MapStatusUpdate getBestMapStatus(boolean isEnd) {
        MapStatusUpdate statusUpdate = null;
        if (isEnd) {
            if (bestLatLngBounds != null) {
                statusUpdate = MapStatusUpdateFactory.newLatLngBounds(bestLatLngBounds);
            }
        } else {
            if (lastSportPoint != null) {
                LatLng lastPoint = translate(lastSportPoint);
                statusUpdate = MapStatusUpdateFactory.newLatLngZoom(lastPoint, BEST_RUNNING_ZOOM_LEVEL);
            }
        }

        return statusUpdate;
    }

}
