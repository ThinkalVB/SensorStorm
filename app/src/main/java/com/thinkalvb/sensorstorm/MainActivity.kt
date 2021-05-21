package com.thinkalvb.sensorstorm

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
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
    private lateinit var mCamera: Camera
    private lateinit var mSensors: Sensors

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkAndRequestPermissions()

        mCamera = Camera(this)
        mLocation = Location(this)
        mSensors = Sensors(this)
        displaySensorList()

        val startButton: Button = findViewById(R.id.start_button)
        val stopButton: Button = findViewById(R.id.stop_button)
        startButton.setOnClickListener{
            if(isRunning) {
                showToast("Services are already running")
                return@setOnClickListener
            }
            val (validPort, portNumber) = getPortNumber()
            if(!validPort) {
                showToast("Invalid Port Number")
                return@setOnClickListener
            }
            val (validIP, ip) = getIP()
            if(!validIP) {
                showToast("Invalid IP Address")
                return@setOnClickListener
            }

            startNetworkService(portNumber, ip)
            Handler(Looper.getMainLooper()).postDelayed({
                if(!mBroadcaster.isRunning) showToast("No Network permission")
            }, 5000)
            startServices()
            isRunning = true
        }
        stopButton.setOnClickListener{
            if(!isRunning) {
                showToast("No service is running")
                return@setOnClickListener
            }
            stopNetworkService()
            stopServices()
            isRunning = false
        }
    }

    private fun displaySensorList() {
        if(!mCamera.isAvailable) findViewById<CheckBox>(R.id.cb_camera).isGone = true
        if(!mLocation.isAvailable) findViewById<CheckBox>(R.id.cb_location).isGone = true

        if(!mSensors.isAvailable(Sensor.TYPE_LINEAR_ACCELERATION)) findViewById<CheckBox>(R.id.cb_acceleration).isGone = true
        if(!mSensors.isAvailable(Sensor.TYPE_ROTATION_VECTOR)) findViewById<CheckBox>(R.id.cb_rotationVector).isGone = true
        if(!mSensors.isAvailable(Sensor.TYPE_LIGHT)) findViewById<CheckBox>(R.id.cb_light).isGone = true

        if(!mSensors.isAvailable(Sensor.TYPE_GRAVITY)) findViewById<CheckBox>(R.id.cb_gravity).isGone = true
        if(!mSensors.isAvailable(Sensor.TYPE_MAGNETIC_FIELD)) findViewById<CheckBox>(R.id.cb_magneticField).isGone = true
        if(!mSensors.isAvailable(Sensor.TYPE_AMBIENT_TEMPERATURE)) findViewById<CheckBox>(R.id.cb_temperature).isGone = true
        if(!mSensors.isAvailable(Sensor.TYPE_RELATIVE_HUMIDITY)) findViewById<CheckBox>(R.id.cb_humidity).isGone = true
        if(!mSensors.isAvailable(Sensor.TYPE_PRESSURE)) findViewById<CheckBox>(R.id.cb_pressure).isGone = true
        if(!mSensors.isAvailable(Sensor.TYPE_PROXIMITY)) findViewById<CheckBox>(R.id.cb_proximity).isGone = true
    }

    private fun stopServices() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mCamera.stopCamera()
        mLocation.stopService()
        mSensors.stopSensors()
    }

    private fun startServices() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if(findViewById<CheckBox>(R.id.cb_camera).isChecked) {
            mCamera.startCamera(this)
            if(!mCamera.isRunning) showToast("No Camera permission")
        }
        if (findViewById<CheckBox>(R.id.cb_location).isChecked) {
            mLocation.startService()
            if(!mLocation.isRunning) {
                showToast("No Location permission")
                return
            }
            if(!mLocation.isEnabled(this)) showToast("Enable GPS for Location")
        }

        if(findViewById<CheckBox>(R.id.cb_acceleration).isChecked) mSensors.startSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if(findViewById<CheckBox>(R.id.cb_rotationVector).isChecked) mSensors.startSensor(Sensor.TYPE_ROTATION_VECTOR)
        if(findViewById<CheckBox>(R.id.cb_light).isChecked) mSensors.startSensor(Sensor.TYPE_LIGHT)

        if(findViewById<CheckBox>(R.id.cb_gravity).isChecked) mSensors.startSensor(Sensor.TYPE_GRAVITY)
        if(findViewById<CheckBox>(R.id.cb_magneticField).isChecked) mSensors.startSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if(findViewById<CheckBox>(R.id.cb_temperature).isChecked) mSensors.startSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        if(findViewById<CheckBox>(R.id.cb_humidity).isChecked) mSensors.startSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
        if(findViewById<CheckBox>(R.id.cb_pressure).isChecked) mSensors.startSensor(Sensor.TYPE_PRESSURE)
        if(findViewById<CheckBox>(R.id.cb_proximity).isChecked) mSensors.startSensor(Sensor.TYPE_PROXIMITY)
    }

    private fun startNetworkService(portNumber: Int, ip: InetAddress) {
        mBroadcaster = Broadcaster(ip, portNumber)
        mNetworkThread = Thread(mBroadcaster)
        mNetworkThread.start()
    }

    private fun stopNetworkService(){
        mNetworkThread.interrupt()
    }

    private fun checkAndRequestPermissions() {
        val appPermissions = arrayOf( Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA)
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

    private fun getPortNumber() : Pair<Boolean, Int> {
        val portNumberStr: EditText = findViewById(R.id.port_number)
        return try {
            val targetPort = portNumberStr.text.toString().toInt()
            if (targetPort < 0 || targetPort > 65535) Pair(false, 1357)
            else Pair(true, targetPort)
        } catch (ex: Exception){
            Pair(false, 1357)
        }
    }

    private fun getIP() : Pair<Boolean, InetAddress> {
        val ipAddressStr: EditText = findViewById(R.id.ip_address)
        return try {
            val targetIP = InetAddress.getByName(ipAddressStr.text.toString())
            Pair(true, targetIP)
        } catch (ex: Exception){
            Pair(false, InetAddress.getByName("10.0.2.2"))
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
}


