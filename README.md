# Sensor Storm

Sensor Storm is an Android app developed for streaming raw (Little Endian) data from inboard sensors. All available sensor data is streamed through UDP packets on a real-time basis. No sensor data is broken into multiple packets except for the image data. All raw sensor data is prefixed with a sensor code int32_t (4 bytes) followed by the data which is at the same exact order as mentioned in the Android [sensor event values](https://developer.android.com/reference/android/hardware/SensorEvent#values).  

Sensor code of each sensor depends on the constant values assigned to each sensor by the Android [sensor type code](https://developer.android.com/guide/topics/sensors/sensors_overview). For location sensor and camera sensor, the sensor code are 120 and 121. The maximum size of an expected UDP packet is 65507. All raw sensor values are 32-bit IEEE 754 floating point numbers (float) except for camera and location.  

For better precision, location data is send as three 64-bit IEEE double precision floating-point number (double) representing latitude, longitude and altitude. The camera sensor data starts with the sensor code 121 followed by the image/frame number, frame size(in bytes), starting index of the image data (0 - uint32_t.MAX), number of bytes of image data - all as int32_t values.

Android sensors supported on Version 1.0.0
1)  Camera (352 * 288 JPEG 90% quality)
2)  Location
3)  Acceleration
4)  Rotation Vector
5)  Light Intensity
6)  Gravity
7)  Magnetic Field
8)  Ambient Temperature
9)  Relative Humidity
10) Pressure
11) Proximity