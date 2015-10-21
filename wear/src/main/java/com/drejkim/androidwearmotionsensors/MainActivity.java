package com.drejkim.androidwearmotionsensors;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    Button start;
    Button stop;

    float dataX;
    float dataY;
    float dataZ;

    float azimuth;
    float roll;
    float pitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //start = (Button) findViewById(R.id.buttonStart);
        //stop = (Button) findViewById(R.id.buttonStop);


        WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override public void onLayoutInflated(WatchViewStub stub) {
                final GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
                //pager.setAdapter(new SensorFragmentPagerAdapter(getFragmentManager()));

                DotsPageIndicator indicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
                indicator.setPager(pager);
            }
        });
    }
}