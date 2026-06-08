package com.timaimee.vpdemo.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.UiSettings;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.maps2d.model.PolylineOptions;
import com.orhanobut.logger.Logger;
import com.timaimee.vpdemo.R;
import com.timaimee.vpdemo.demo.DemoTextTranslator;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.data.IReportGpsDataListener;
import com.veepoo.protocol.model.datas.ReportGpsLatLongData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by timaimee on 2017/4/24.
 */
public class GpsReportActivity extends Activity implements LocationSource, AMapLocationListener {
    private Context mContext;
    private final static String TAG = GpsReportActivity.class.getSimpleName();
    WriteResponse writeResponse = new WriteResponse();
    MapView mMapView;
    AMap aMap;

    OnLocationChangedListener mListener;
    AMapLocationClient mlocationClient;

    TextView mResultTv;

    /**
     * estado
     */
    static class WriteResponse implements IBleWriteResponse {

        @Override
        public void onResponse(int code) {
            Logger.t(TAG).i("write cmd status:" + code);

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_report);
        mResultTv = findViewById(R.id.result);
        mContext = getApplicationContext();

        //
        mMapView = (MapView) findViewById(R.id.map);
        //activityonCreatemMapView.onCreate(savedInstanceState)，
        mMapView.onCreate(savedInstanceState);

        //
        if (aMap == null) {
            aMap = mMapView.getMap();
            setUpMap();
        }

    }


    List<LatLng> mLatLngsPointList = new ArrayList<>();
    List<String> mPointStrList = new ArrayList<>();

    public void startReport() {
        mLatLngsPointList.clear();

        VPOperateManager.getMangerInstance(mContext).setReportGps(writeResponse, true, new IReportGpsDataListener() {
            @Override
            public void onReportGpsDataDataChange(ReportGpsLatLongData reportGpsLatLongData) {
                String message = "AtivarGps:" + reportGpsLatLongData.toString();
                mResultTv.setText(DemoTextTranslator.translate(message));
                addPolyline(new LatLng(reportGpsLatLongData.getLat(), reportGpsLatLongData.getLon()));
                logInfo(message);
            }
        });
    }

    public void stopReport() {
        VPOperateManager.getMangerInstance(mContext).setReportGps(writeResponse, false, new IReportGpsDataListener() {
            @Override
            public void onReportGpsDataDataChange(ReportGpsLatLongData reportGpsLatLongData) {
                String message = "DesativarGps:" + reportGpsLatLongData.toString();
                mResultTv.setText(DemoTextTranslator.translate(message));
                logInfo(message);
            }
        });
    }


    private void addPolyline(LatLng latLng) {
        mLatLngsPointList.add(latLng);

        addMark(latLng);
        PolylineOptions polylineOptions = new PolylineOptions().
                addAll(mLatLngsPointList).width(10).color(Color.argb(255, 1, 1, 1));
        aMap.addPolyline(polylineOptions);

//        LatLng latLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
        LatLng latLngLast = mLatLngsPointList.get(mLatLngsPointList.size() - 1);
        aMap.moveCamera(CameraUpdateFactory.newLatLng(latLngLast));
    }

    private void addMark(LatLng latLng) {
        MarkerOptions mark = new MarkerOptions()
                // .setFlat(true)
                .position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.fit_start_point));
        aMap.addMarker(mark);
    }

    /**
     * Regista mensagens vindas do SDK com as etiquetas conhecidas traduzidas.
     */
    private static void logInfo(String message) {
        Logger.t(TAG).i(DemoTextTranslator.translate(message));
    }

    void setUpMap() {

        MyLocationStyle myLocationStyle;
        myLocationStyle = new MyLocationStyle();//myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//，dispositivo，dispositivo（11）ConfigurarmyLocationType，
        aMap.setMyLocationStyle(myLocationStyle);//ConfigurarStyle

        aMap.setLocationSource(this);// Configurar
        UiSettings uiSettings = aMap.getUiSettings();
        uiSettings.setMyLocationButtonEnabled(false);// Configurar

        aMap.setMyLocationEnabled(false);// Configurartrue，false，false
    }


    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        Logger.t(TAG).i("onLocationChanged");
        mListener.onLocationChanged(amapLocation);// 

    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        Logger.t(TAG).i("activate");
//        startLocation();
    }

    private void startLocation() {

        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient = null;
        }

        if (mlocationClient == null) {
            try {
                mlocationClient = new AMapLocationClient(this);
                mlocationClient.setLocationListener(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
        mLocationOption.setOnceLocation(true);
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mlocationClient.setLocationOption(mLocationOption);

        mlocationClient.startLocation();
    }

    @Override
    public void deactivate() {
        Logger.t(TAG).i("deactivate");
        if (mlocationClient != null && mlocationClient.isStarted())
            mlocationClient.stopLocation();
        mlocationClient = null;
        mListener = null;

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        Logger.t(TAG).i("onPointerCaptureChanged");
    }


    @Override
    protected void onResume() {
        super.onResume();
        //activityonResumemMapView.onResume ()，
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //activityonPausemMapView.onPause ()，
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //activityonSaveInstanceStatemMapView.onSaveInstanceState (outState)，estado
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //activityonDestroymMapView.onDestroy()，
        mMapView.onDestroy();
        if (null != mlocationClient) {
            mlocationClient.onDestroy();
        }
    }
}
