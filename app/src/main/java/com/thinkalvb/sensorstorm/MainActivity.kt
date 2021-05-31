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
    private lateinit var mCamera: Camera
    private lateinit var mSensors: Sensors

    private var mPortNumber = 1357
    private var mIPAddress = InetAddress.getByName("10.0.2.2")
    private var mImageQuality = 90
    private var mImageRotation = 90f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkAndRequestPermissions()

        mCamera = Camera(this)
        mLocation = Location(this)
        mSensors = Sensors(this)
        displaySensorList()

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
            mCamera.startCamera(this, mImageQuality, mImageRotation)
            Handler(Looper.getMainLooper()).postDelayed({
                if(isRunning && !mCamera.isRunning) showToast("No Camera permission")
            }, 1000)
        }
        if (findViewById<CheckBox>(R.id.cb_location).isChecked) {
            mLocation.startService()
            Handler(Looper.getMainLooper()).postDelayed({
                if(isRunning && !mLocation.isRunning) showToast("No Location permission")
            }, 1000)
            if(!mLocation.isEnabled(this)) showToast("Enable GPS for Location")
        }

        if(findViewById<CheckBox>(R.id.cb_acceleration).isChecked) mSensors.startSensor(Sensor.TYPE_LINEAR_ACCELERATION, getSensorDelay(Sensor.TYPE_LINEAR_ACCELERATION))
        if(findViewById<CheckBox>(R.id.cb_rotationVector).isChecked) mSensors.startSensor(Sensor.TYPE_ROTATION_VECTOR, getSensorDelay(Sensor.TYPE_ROTATION_VECTOR))
        if(findViewById<CheckBox>(R.id.cb_light).isChecked) mSensors.startSensor(Sensor.TYPE_LIGHT, getSensorDelay(Sensor.TYPE_LIGHT))

        if(findViewById<CheckBox>(R.id.cb_gravity).isChecked) mSensors.startSensor(Sensor.TYPE_GRAVITY, getSensorDelay(Sensor.TYPE_GRAVITY))
        if(findViewById<CheckBox>(R.id.cb_magneticField).isChecked) mSensors.startSensor(Sensor.TYPE_MAGNETIC_FIELD, getSensorDelay(Sensor.TYPE_MAGNETIC_FIELD))
        if(findViewById<CheckBox>(R.id.cb_temperature).isChecked) mSensors.startSensor(Sensor.TYPE_AMBIENT_TEMPERATURE, getSensorDelay(Sensor.TYPE_AMBIENT_TEMPERATURE))
        if(findViewById<CheckBox>(R.id.cb_humidity).isChecked) mSensors.startSensor(Sensor.TYPE_RELATIVE_HUMIDITY, getSensorDelay(Sensor.TYPE_RELATIVE_HUMIDITY))
        if(findViewById<CheckBox>(R.id.cb_pressure).isChecked) mSensors.startSensor(Sensor.TYPE_PRESSURE, getSensorDelay(Sensor.TYPE_PRESSURE))
        if(findViewById<CheckBox>(R.id.cb_proximity).isChecked) mSensors.startSensor(Sensor.TYPE_PROXIMITY, getSensorDelay(Sensor.TYPE_PROXIMITY))
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

    private fun getSensorDelay(sensorType: Int): Int {
        if(sensorType == Sensor.TYPE_LINEAR_ACCELERATION) return positionToDelay(findViewById<SeekBar>(R.id.sb_acceleration).progress)
        if(sensorType == Sensor.TYPE_ROTATION_VECTOR) return positionToDelay(findViewById<SeekBar>(R.id.sb_rotationVector).progress)
        if(sensorType == Sensor.TYPE_LIGHT) return positionToDelay(findViewById<SeekBar>(R.id.sb_light).progress)

        if(sensorType == Sensor.TYPE_GRAVITY) return positionToDelay(findViewById<SeekBar>(R.id.sb_gravity).progress)
        if(sensorType == Sensor.TYPE_MAGNETIC_FIELD) return positionToDelay(findViewById<SeekBar>(R.id.sb_magneticField).progress)
        if(sensorType == Sensor.TYPE_AMBIENT_TEMPERATURE) return positionToDelay(findViewById<SeekBar>(R.id.sb_temperature).progress)
        if(sensorType == Sensor.TYPE_RELATIVE_HUMIDITY) return positionToDelay(findViewById<SeekBar>(R.id.sb_humidity).progress)
        if(sensorType == Sensor.TYPE_PRESSURE) return positionToDelay(findViewById<SeekBar>(R.id.sb_pressure).progress)
        if(sensorType == Sensor.TYPE_PROXIMITY) return positionToDelay(findViewById<SeekBar>(R.id.sb_proximity).progress)
        return 100
    }

    private fun positionToDelay(sbPosition: Int): Int{
        return when (sbPosition) {
            0 -> SensorManager.SENSOR_DELAY_NORMAL
            1 -> SensorManager.SENSOR_DELAY_UI
            2 -> SensorManager.SENSOR_DELAY_GAME
            3 -> SensorManager.SENSOR_DELAY_FASTEST
            else -> 100
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

        val imageQualityStr: EditText = findViewById(R.id.camera_quality)
        try{
            val imageQuality = imageQualityStr.text.toString().toInt()
            if (imageQuality < 0 || imageQuality > 100) {
                showToast("Invalid Image Quality, use range (0 - 100)")
                return false
            } else mImageQuality = imageQuality
        }catch(ex: Exception){
            showToast("Invalid Image Quality")
            return false
        }

        val rotationString = findViewById<Spinner>(R.id.rotations_spinner)
        val selectedRotation = rotationString.selectedItem.toString()
        mImageRotation = selectedRotation.toFloat()
        return true
    }
}