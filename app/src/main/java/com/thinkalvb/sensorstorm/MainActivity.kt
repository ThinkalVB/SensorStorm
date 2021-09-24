package com.thinkalvb.sensorstorm

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import java.net.InetAddress

class MainActivity : AppCompatActivity() {
    private lateinit var mNetworkThread: Thread
    private var isRunning = false

    private lateinit var mBroadcaster: Broadcaster
    private lateinit var mLocation: Location
    private lateinit var mSensors: Sensors

    private var mPortNumber = 1357
    private var mIPAddress = InetAddress.getByName("10.0.2.2")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkAndRequestPermissions()

        mLocation = Location(this)
        mSensors = Sensors(this)

        val startStopButton: Button = findViewById(R.id.start_stop)
        startStopButton.setOnClickListener{
            if(isRunning){
                stopNetworkService()
                stopServices()
                startStopButton.text = getString(R.string.start)
                isRunning = false
            } else {
                if(!validateInputs()) return@setOnClickListener
                startNetworkService(mPortNumber, mIPAddress)
                startServices()
                startStopButton.text = getString(R.string.stop)
                isRunning = true
            }
        }
    }

    private fun stopServices() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mLocation.stopService()
        mSensors.stopSensors()
    }

    private fun startServices() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (mLocation.isAvailable) {
            mLocation.startService()
            Handler(Looper.getMainLooper()).postDelayed({
                if(isRunning && !mLocation.isRunning) showToast("No Location permission")
            }, 1000)
            if(!mLocation.isEnabled(this)) showToast("Enable GPS for Location")
        }

        if(mSensors.isAvailable(Sensor.TYPE_LINEAR_ACCELERATION)) mSensors.startSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if(mSensors.isAvailable(Sensor.TYPE_ROTATION_VECTOR)) mSensors.startSensor(Sensor.TYPE_ROTATION_VECTOR)
        if(mSensors.isAvailable(Sensor.TYPE_LIGHT)) mSensors.startSensor(Sensor.TYPE_LIGHT)
    }

    private fun startNetworkService(portNumber: Int, ip: InetAddress) {
        mBroadcaster = Broadcaster(ip, portNumber)
        mNetworkThread = Thread(mBroadcaster)
        mNetworkThread.start()

        Handler(Looper.getMainLooper()).postDelayed({
            if(isRunning && !mBroadcaster.isRunning) showToast("No Network permission")
        }, 1000)
    }

    private fun stopNetworkService(){
        mNetworkThread.interrupt()
    }

    private fun checkAndRequestPermissions() {
        val appPermissions = arrayOf( Manifest.permission.ACCESS_FINE_LOCATION)
        val neededPermissions = ArrayList<String>()
        for (perm in appPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(perm)
            }
        }

        if(neededPermissions.isNotEmpty()) {
            val requestCode = 108
            ActivityCompat.requestPermissions(this, neededPermissions.toTypedArray(), requestCode)
        }
    }

    private fun showToast(message: String) {
            Toast.makeText(applicationContext,message, Toast.LENGTH_SHORT).show()
    }

    override fun onUserInteraction() {
        val view = currentFocus
        if (view != null && view !is EditText) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
    }

    private fun validateInputs() : Boolean {
        val ipAddressStr: EditText = findViewById(R.id.ip_address)
        try {
            mIPAddress = InetAddress.getByName(ipAddressStr.text.toString())
        } catch (ex: Exception) {
            showToast("Invalid IP Address")
            return false
        }

        val portNumberStr: EditText = findViewById(R.id.port_number)
        try{
            val targetPort = portNumberStr.text.toString().toInt()
            if (targetPort < 0 || targetPort > 65535) {
                showToast("Invalid Port Number, use range (0 - 65535)")
                return false
            } else mPortNumber = targetPort
        }catch(ex: Exception){
            showToast("Invalid Port Number")
            return false
        }
        return true
    }
}