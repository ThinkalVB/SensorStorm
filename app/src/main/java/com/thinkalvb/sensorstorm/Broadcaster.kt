package com.thinkalvb.sensorstorm

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

private const val MAX_PACKET_SIZE = 65500
private const val PACKET_BUFFER_COUNT = 3
private const val IMAGE_HEADER_SIZE = (Int.SIZE_BYTES * 5)
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

        while(!Thread.currentThread().isInterrupted) {
            for (index in 0 until PACKET_BUFFER_COUNT - 1) {
                if(mPacketBufferLock[index].tryLock()) {
                    if(mPacketBuffer[index].position() != 0) {
                        val packet = DatagramPacket(mPacketBuffer[index].array(), mPacketBuffer[index].position(), mDestinationIP, mDestinationPort)
                        mPacketBuffer[index].clear()
                        mUdpSocket.send(packet)
                    }
                    mPacketBufferLock[index].unlock()
                }
            }
        }
        mUdpSocket.close()
        isRunning = false
    }

    companion object{
        private var mPacketBuffer = Array<ByteBuffer>(PACKET_BUFFER_COUNT) { ByteBuffer.allocate(MAX_PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN) }
        private var mPacketBufferLock = Array<Lock>(PACKET_BUFFER_COUNT) { ReentrantLock() }
        private var mFrameNumber = 0

        fun sendData(data: ByteArray){
            for (index in 0 until PACKET_BUFFER_COUNT - 1) {
                if(mPacketBufferLock[index].tryLock()) {
                    if(mPacketBuffer[index].remaining() > data.size) {
                        mPacketBuffer[index].put(data)
                        mPacketBufferLock[index].unlock()
                        break
                    } else mPacketBufferLock[index].unlock()
                }
            }
        }

        /* SensorCode -- FrameNumber -- FrameSize -- StartingIndex -- NoOfBytes */
        fun sendFrame(image: ByteArray) {
            val imageBuffer = ByteBuffer.wrap(image)
            mFrameNumber++

            /* While their is some image data left to be send   */
            while (true) {
                /* Iterate through all the available buffers    */
                for (index in 0 until PACKET_BUFFER_COUNT - 1) {
                    /* Try to access the buffer                 */
                    if (imageBuffer.remaining() == 0) return
                    if(mPacketBufferLock[index].tryLock()) {
                        if(mPacketBuffer[index].remaining() > IMAGE_HEADER_SIZE){
                            mPacketBuffer[index].putInt(CAM_SENSOR_CODE)
                            mPacketBuffer[index].putInt(mFrameNumber)
                            mPacketBuffer[index].putInt(image.size)

                            val packetFreeSpace = mPacketBuffer[index].remaining() - ( Int.SIZE_BYTES * 2)
                            val imageFragmentSize =
                                if(packetFreeSpace >= imageBuffer.remaining()) imageBuffer.remaining()
                                else packetFreeSpace

                            mPacketBuffer[index].putInt(imageBuffer.position())
                            mPacketBuffer[index].putInt(imageFragmentSize)
                            imageBuffer.get(mPacketBuffer[index].array(), mPacketBuffer[index].position(), imageFragmentSize)
                            mPacketBuffer[index].position(mPacketBuffer[index].position() + imageFragmentSize)
                        }
                        mPacketBufferLock[index].unlock()
                    }
                }
            }
        }
    }
}