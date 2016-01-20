package com.ticwear.app.gpstracker;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ticwear.app.gpstracker.utils.DistanceUtil;

import java.util.ArrayList;
import java.util.List;

abstract public class TraceActivity extends Activity {

    private static class LogAdapter extends ArrayAdapter<String> {

        public LogAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
        }
    }

    public static final long SCAN_SPAN = 1000l;
    public static final long ALIVE_CHECK = 3000l;

    public static final double SMALLEST_LAT_LNG = 0.001;

    ViewGroup layoutMapContainer;
    TextView textCountDown;

    TextView textTraceSpeed;
    TextView textTraceGps;
    Chronometer chronometerTrace;
    TextView textTraceDistance;

    ListView listLog;
    LogAdapter logAdapter;

    Button btnTraceStart;
    Button btnTraceStop;
    Button btnSnapshot;

    private Handler uiHandler = new Handler();

    protected final List<TracePoint> tracePointList = new ArrayList<>();
    protected boolean isTracking = false;
    protected boolean isStarting = false;
    protected double distance = 0;
    protected long totalTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trace);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        layoutMapContainer = (ViewGroup) findViewById(R.id.layout_map_container);
        textCountDown = (TextView) findViewById(R.id.text_count_down);

        textTraceSpeed = (TextView) findViewById(R.id.trace_speed);
        textTraceGps = (TextView) findViewById(R.id.trace_gps);
        chronometerTrace = (Chronometer) findViewById(R.id.trace_timer);
        textTraceDistance = (TextView) findViewById(R.id.trace_distance);

        btnTraceStart = (Button) findViewById(R.id.btn_trace_start);
        btnTraceStop = (Button) findViewById(R.id.btn_trace_stop);
        btnSnapshot = (Button) findViewById(R.id.btn_snapshot);
        btnSnapshot.setVisibility(View.GONE);
        btnSnapshot.setEnabled(false);

        logAdapter = new LogAdapter(
                this,
                R.layout.list_log_item,
                R.id.text
        );

        addMapInto(layoutMapContainer);
        updateButtons();

        initChronometer();


        btnTraceStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onTracePrepare();
                updateButtons();
            }
        });
        btnTraceStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onTraceEnd();
                updateButtons();
            }
        });

        btnSnapshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSnapshot();
            }
        });
    }

    @Override
    protected void onDestroy() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
    }

    abstract protected void addMapInto(ViewGroup parent);

    /**
     * When click start, we have 3 seconds to prepare trace.
     * Prepare needs to do this things:
     *  1. Find my location
     *  2. Gather first N trace point. Base Activity will calculate the average location
     *     seems the first N location is not trustful.
     */
    abstract protected void prepareTrace();
    abstract protected void startTrace(TracePoint endpoint);
    abstract protected void stopTrace(TracePoint endpoint);
    abstract protected void addTrace(List<TracePoint> tracePoints);

    abstract protected void onSnapshot();

    /**
     * This method can be called after prepare & before stop.
     * When receive a new location from SDK, translate the point type and pass it in.
     * @param tracePoint the new raw point from SDK.
     */
    protected void addRawTracePoint(TracePoint tracePoint) {
        addLog(String.format("Point (%f, %f), %f", tracePoint.latitude, tracePoint.longitude, tracePoint.accuracy));

        if (tracePoint.isEmpty() || (tracePoint.latitude < SMALLEST_LAT_LNG && tracePoint.longitude < SMALLEST_LAT_LNG))
            return;

        tracePointList.add(tracePoint);
        if (isStarting) {
            // Show speed
            textTraceSpeed.setText(getString(R.string.text_trace_speed, tracePoint.speed));

            // Show Gps status
            textTraceGps.setText(getString(R.string.text_trace_gps, tracePoint.accuracy));

            // Show distance.
            distance = getCurrentDistance(tracePoint);
            textTraceDistance.setText(getString(R.string.text_trace_distance, distance));

            // TODO: make a path curve, instead of single point.
            if (tracePointList.size() >= 3) {
                addTrace(tracePointList.subList(tracePointList.size() - 3, tracePointList.size()));
            }
        }
    }

    protected void onMapPrepared() {
        addLog("Map prepared");
        btnSnapshot.setEnabled(true);
        // onSnapshot();
    }

    protected void showSnapshot(Bitmap bitmap) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_snapshot_image);
        ImageView imgView=(ImageView)dialog.findViewById(R.id.image_snapshot);
        imgView.setImageBitmap(bitmap);
        dialog.show();
    }

    protected void addLog(String log) {
        logAdapter.add(log);
        if (listLog != null) {
            listLog.smoothScrollToPosition(listLog.getCount() - 1);
        }
    }


    protected void removeLogListView() {
        if (listLog == null) {
            return;
        }

        listLog.setAdapter(null);
        ((ViewGroup) listLog.getParent()).removeView(listLog);
        listLog = null;
    }

    protected void addLogListViewInto(ViewGroup parent) {
        if (listLog != null) {
            return;
        }

        listLog = (ListView) getLayoutInflater().inflate(R.layout.list_log, parent, false);
        parent.addView(listLog);
        listLog.setAdapter(logAdapter);

        listLog.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                removeLogListView();
            }
        });

        addLog("Start logging...");
    }

    /**
     * 初始化计时器
     */
    private void initChronometer() {
        chronometerTrace
                .setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {

                    public void onChronometerTick(Chronometer cArg) {
                        totalTime++;

                        long time = SystemClock.elapsedRealtime()
                                - cArg.getBase();
                        int h = (int) (time / 3600000);
                        int m = (int) (time - h * 3600000) / 60000;
                        int s = (int) (time - h * 3600000 - m * 60000) / 1000;
                        String hh = h < 10 ? "0" + h : h + "";
                        String mm = m < 10 ? "0" + m : m + "";
                        String ss = s < 10 ? "0" + s : s + "";
                        cArg.setText(hh + ":" + mm + ":" + ss);
                    }
                });
        chronometerTrace.setBase(SystemClock.elapsedRealtime());
    }

    private void onTracePrepare() {
        isTracking = true;
        isStarting = false;

        tracePointList.clear();
        distance = 0;
        textCountDown.setVisibility(View.VISIBLE);
        prepareTrace();
        Runnable countDownRunnable = createCountDownRunnable();
        uiHandler.postDelayed(countDownRunnable, 1000);
    }

    private void onTraceStart() {
        isStarting = true;
        textCountDown.setVisibility(View.GONE);
        totalTime = 0;
        startTrace(tracePointList.get(0));
        if (tracePointList.size() >= 2) {
            addTrace(new ArrayList<>(tracePointList));
        }
    }

    private void onTraceEnd() {
        isTracking = false;
        isStarting = false;

        textCountDown.setVisibility(View.GONE);
        TracePoint endPoint = new TracePoint();
        if (!tracePointList.isEmpty()) {
            endPoint = tracePointList.get(tracePointList.size() - 1);
        }
        stopTrace(endPoint);
    }

    private Runnable createCountDownRunnable() {
        return new Runnable() {
            int countDown = 3;
            @Override
            public void run() {
                textCountDown.setText(String.valueOf(countDown));
                if (countDown > 0) {
                    countDown--;
                    uiHandler.postDelayed(this, 1000);
                } else {
                    uiHandler.postDelayed(createStartTraceRunnable(), 1000);
                }
            }
        };
    }

    private Runnable createStartTraceRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                List<TracePoint> points = mergeTraceStartPoints();
                if (!points.isEmpty()) {
                    onTraceStart();
                } else if (isTracking) {
                    // not received any trace point, re-call this to init.
                    // TODO: throw error when list is continues empty.
                    uiHandler.postDelayed(this, 1000);
                }
            }
        };
    }

    private void updateButtons() {
        if (isTracking) {
            btnTraceStart.setVisibility(View.GONE);
            btnTraceStop.setVisibility(View.VISIBLE);
        } else {
            btnTraceStart.setVisibility(View.VISIBLE);
            btnTraceStop.setVisibility(View.GONE);
        }
    }

    // Merge All trace points to one.
    // This is doing when start trace after 3 seconds prepare.
    private List<TracePoint> mergeTraceStartPoints() {

        if (!tracePointList.isEmpty()) {
            double totalLat = 0.0;
            double totalLng = 0.0;

            for (TracePoint point : tracePointList) {
                totalLat += point.latitude;
                totalLng += point.longitude;
            }
            TracePoint avePoint = new TracePoint();
            avePoint.latitude = totalLat / tracePointList.size();
            avePoint.longitude = totalLng / tracePointList.size();

            tracePointList.clear();
            tracePointList.add(avePoint);
        }

        return tracePointList;
    }

    private double getCurrentDistance(TracePoint tracePoint) {
        TracePoint lastPoint = tracePointList.get(tracePointList.size() - 1);
        if (tracePointList.isEmpty())
            lastPoint = tracePoint;

        double diff = DistanceUtil.GetShortDistance(
                lastPoint.longitude, lastPoint.latitude,
                tracePoint.longitude, tracePoint.latitude);

        return distance + diff;
    }


}
