package com.thinkalvb.sensorstorm

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

private const val MAX_PACKET_SIZE = 65507
private const val CAM_SENSOR_CODE = 121

class Broadcaster(private val mDestinationIP: InetAddress, private val mDestinationPort: Int) : Runnable {
    private lateinit var mUdpSocket: DatagramSocket
    var isRunning = false

    override fun run() {
        try {
            mUdpSocket = DatagramSocket()
            mUdpSocket.trafficClass = 0x10
            isRunning = true
        } catch (e: Exception) {
            isRunning = false
            return
        }

        while(!Thread.currentThread().isInterrupted)
        {
            if(mDataBuffer.position() != 0) {
                mDataBufferLock.lock()
                val packet = DatagramPacket(mDataBuffer.array(), mDataBuffer.position(), mDestinationIP, mDestinationPort)
                mDataBuffer.clear()
                mDataBufferLock.unlock()
                mUdpSocket.send(packet)
            }

            if(mImageBuffer.position() != 0) {
                mImageBufferLock.lock()
                val packet = DatagramPacket(mImageBuffer.array(), mImageBuffer.position(), mDestinationIP, mDestinationPort)
                mImageBuffer.clear()
                mImageBufferLock.unlock()
                mUdpSocket.send(packet)
            }
        }
        mUdpSocket.close()
        isRunning = false
    }

    companion object{
        private var mDataBuffer: ByteBuffer = ByteBuffer.allocate(MAX_PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        private var mImageBuffer: ByteBuffer = ByteBuffer.allocate(MAX_PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)

        private var mFrameNumber = 0
        private var mDataBufferLock: Lock = ReentrantLock()
        private var mImageBufferLock: Lock = ReentrantLock()

        fun sendData(data: ByteArray){
            mDataBufferLock.lock()
            if(mDataBuffer.remaining() > data.size) mDataBuffer.put(data)
            else mDataBuffer.clear()
            mDataBufferLock.unlock()
        }

        /* SensorCode -- FrameNumber -- FrameSize -- StartingIndex -- NoOfBytes */
        fun sendFrame(image: ByteArray) {
            mImageBufferLock.lock()
            val imageDataSize = image.size + (Int.SIZE_BYTES * 5)
            if(mImageBuffer.remaining() > imageDataSize) {
                mImageBuffer.putInt(CAM_SENSOR_CODE)
                mImageBuffer.putInt(mFrameNumber)
                mImageBuffer.putInt(image.size)
                mImageBuffer.putInt(0)
                mImageBuffer.putInt(image.size)
                mImageBuffer.put(image)
                mFrameNumber++
            } else mImageBuffer.clear()
            mImageBufferLock.unlock()
        }
    }
}