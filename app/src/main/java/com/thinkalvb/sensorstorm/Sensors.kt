package com.thinkalvb.sensorstorm

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Sensors(activity: MainActivity): SensorEventListener{
    private var mSensorManager: SensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var mLinearAcceleration: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private var mRotationVector: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var mLight: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    fun isAvailable(sensorType: Int) : Boolean {
        if (sensorType == Sensor.TYPE_LINEAR_ACCELERATION && mLinearAcceleration != null) return true
        if (sensorType == Sensor.TYPE_ROTATION_VECTOR && mRotationVector != null) return true
        if (sensorType == Sensor.TYPE_LIGHT && mLight != null) return true
        return false
    }

    fun startSensor(sensorType: Int, sensorDelay: Int = SensorManager.SENSOR_DELAY_NORMAL) {
        if(sensorType == Sensor.TYPE_LINEAR_ACCELERATION) mSensorManager.registerListener(this, mLinearAcceleration, sensorDelay)
        if(sensorType == Sensor.TYPE_ROTATION_VECTOR) mSensorManager.registerListener(this, mRotationVector, sensorDelay)
        if(sensorType == Sensor.TYPE_LIGHT) mSensorManager.registerListener(this, mLight, sensorDelay)
    }

    fun stopSensors() {
        mSensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event != null){
            val sensorValues = event.values.clone()
            when (event.sensor.type) {
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    val mBroadcastBuffer = ByteBuffer.allocate((Float.SIZE_BYTES * 3) + Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
                    mBroadcastBuffer.putInt(Sensor.TYPE_LINEAR_ACCELERATION)
                    mBroadcastBuffer.putFloat(sensorValues[0])
                    mBroadcastBuffer.putFloat(sensorValues[1])
                    mBroadcastBuffer.putFloat(sensorValues[2])
                    Broadcaster.sendData(mBroadcastBuffer.array())
                }
                Sensor.TYPE_ROTATION_VECTOR -> {
                    val mBroadcastBuffer = ByteBuffer.allocate((Float.SIZE_BYTES * 5) + Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
                    mBroadcastBuffer.putInt(Sensor.TYPE_ROTATION_VECTOR)
                    mBroadcastBuffer.putFloat(sensorValues[0])
                    mBroadcastBuffer.putFloat(sensorValues[1])
                    mBroadcastBuffer.putFloat(sensorValues[2])
                    mBroadcastBuffer.putFloat(sensorValues[3])
                    mBroadcastBuffer.putFloat(sensorValues[4])
                    Broadcaster.sendData(mBroadcastBuffer.array())
                }
                Sensor.TYPE_LIGHT -> {
                    val mBroadcastBuffer = ByteBuffer.allocate(Float.SIZE_BYTES + Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
                    mBroadcastBuffer.putInt(Sensor.TYPE_LIGHT)
                    mBroadcastBuffer.putFloat(sensorValues[0])
                    Broadcaster.sendData(mBroadcastBuffer.array())
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}