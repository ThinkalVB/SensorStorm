# Sensor Storm

Sensor Storm is an Android app developed for streaming raw (Little Endian) data from inboard sensors. All available sensor data is streamed through UDP packets on a real-time basis. No sensor data is broken into multiple packets except for the image data. All raw sensor data is prefixed with a sensor code of type int32_t (int) followed by the data which is at the same exact order as mentioned in the Android [sensor event values](https://developer.android.com/reference/android/hardware/SensorEvent#values).  

Sensor code of each sensor depends on the constant values assigned to each sensor by the Android [sensor type code](https://developer.android.com/guide/topics/sensors/sensors_overview). For location sensor and camera sensor, the sensor code are 120 and 121. The maximum size of an expected UDP packet is 65500. All raw sensor values are 32-bit IEEE 754 floating point numbers (float) except for camera and location.  

For better precision, location data is send as three 64-bit IEEE double precision floating-point number (double) representing latitude, longitude and altitude. If camera produce images that can't be accommodated in a single UDP packet, then it is fragmented and send in different packets. 

The camera sensor data starts with the sensor code 121 followed by the image sequence number (1 - Int.MAX_VALUE), image size in bytes (1 - Int.MAX_VALUE), index value of fragment in image data (0 - Int.MAX_VALUE), fragment size (1 - Int.MAX_VALUE) - all as int32_t (int) values. 

### Download  
[Amazon App Store](https://www.amazon.com/dp/B095SMCM5T/)

## Supported Android Sensors  
1)  Camera (352 * 288 JPEG)
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

## Examples
C++ Example - Parsing Linear Acceleration and Location from UDP packet
```
#define LINEAR_ACCELERATION 10
#define LOCATION 120

float acceleration[3];
double position[3];

void processData(uint8_t dataBuffer[], int size)
{
    int index = 0;
    while (index < size)
    {
        int32_t sensorCode;
        memcpy(&sensorCode, &dataBuffer[index], sizeof(sensorCode));
        index += sizeof(sensorCode);

        if(sensorCode == LINEAR_ACCELERATION)
        {   
            memcpy(&acceleration[0], &dataBuffer[index], sizeof(acceleration));
            index += sizeof(acceleration);         
            std::cout<<"Acceleration "<<acceleration[0]<<" "<<acceleration[1]<<" "<<acceleration[2]<<std::endl;
          
        }
        else if(sensorCode == LOCATION)
        {   
            memcpy(&position[0], &dataBuffer[index], sizeof(position));
            index += sizeof(position);
            std::cout<<"Position "<<position[0]<<" "<<position[1]<<" "<<position[2]<<std::endl;        
        }
    }
}
```

C++ Example - Parsing Image from UDP packet  
Image Data Format [[..........Fragment1..........][..........Fragment2..........][..........Fragment..........]]  
Note: This is a basic example for demonstration purpose assuming the image fits into a single UDP packet which is true for JPEG images of (352 * 288 - 90% quality).
```
#define MAX_IMAGE_SIZE 65500
#define IMAGE          121

uint8_t image[MAX_IMAGE_SIZE];
int32_t imageSize;
int32_t frameNumber;

void SensorManager::processData(uint8_t dataBuffer[], int size)
{
    int index = 0;
    while (index < size)
    {
        int32_t sensorCode;
        memcpy(&sensorCode, &dataBuffer[index], sizeof(sensorCode));
        index += sizeof(sensorCode);

        if(sensorCode == IMAGE)
        {
            memcpy(&frameNumber, &dataBuffer[index], sizeof(frameNumber));
            index += sizeof(frameNumber);
            memcpy(&imageSize, &dataBuffer[index], sizeof(imageSize));
            index += sizeof(imageSize);

            int32_t startingIndex;
            int32_t noOfBytes;
            memcpy(&startingIndex, &dataBuffer[index], sizeof(startingIndex));
            index += sizeof(startingIndex);
            memcpy(&noOfBytes, &dataBuffer[index], sizeof(noOfBytes));
            index += sizeof(noOfBytes);

            memcpy(&image[startingIndex], &dataBuffer[index], noOfBytes);
            index += noOfBytes;

            std::ofstream file("image.jpeg", std::ios::out | std::ios::binary);
            file.write((char*)&image[0], imageSize);
            file.close();
        }
    }
}
```
