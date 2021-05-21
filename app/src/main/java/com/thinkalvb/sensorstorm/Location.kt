package com.thinkalvb.sensorstorm

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val GPS_SENSOR_CODE = 120

class Location(activity: Activity) {
    var isAvailable = false
    var isRunning = false

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private var mBroadcastBuffer = ByteBuffer.allocate((Double.SIZE_BYTES * 3) + Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)

    init {
        if(activity.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            isAvailable = true

            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
            mLocationRequest = LocationRequest.create().apply {
                interval = 2000
                fastestInterval = 1000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            mLocationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)

                    mBroadcastBuffer.clear()
                    mBroadcastBuffer.putInt(GPS_SENSOR_CODE)
                    mBroadcastBuffer.putDouble(locationResult.lastLocation.latitude)
                    mBroadcastBuffer.putDouble(locationResult.lastLocation.longitude)
                    mBroadcastBuffer.putDouble(locationResult.lastLocation.altitude)
                    Broadcaster.sendData(mBroadcastBuffer.array())
                }
            }
        }
    }

    fun startService() {
        if(!isAvailable) return
        isRunning = try {
            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
            true
        }catch (e: SecurityException) {
            false
        }
    }

    fun stopService() {
        if(isRunning){
            mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
            isRunning = false
        }
    }

    fun isEnabled(activity: Activity): Boolean {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}