package com.ombapit.alarmbutton.utils;

import android.location.Location;

/**
 * Created by 247 on 10/6/2016.
 */

public interface LocationManagerInterface {
    String TAG = LocationManagerInterface.class.getSimpleName();

    void locationFetched(Location mLocation, Location oldLocation, String time, String locationProvider);

}