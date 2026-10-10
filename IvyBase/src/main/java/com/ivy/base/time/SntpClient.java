package com.ivy.base.time;


import android.os.SystemClock;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * 轻量级 SNTP Client
 * 支持获取 UTC 时间戳（毫秒）
 * <p>
 * 用法：
 * SntpClient client = new SntpClient();
 * if (client.requestTime("time.google.com", 3000)) {
 * long now = client.getNtpTime();
 * }
 */
public class SntpClient {

    private static final int NTP_PORT = 123;
    private static final int NTP_PACKET_SIZE = 48;

    private static final int INDEX_TRANSMIT_TIME = 40;

    // 1900 与 1970 的秒差
    private static final long OFFSET_1900_TO_1970 = 2208988800L;

    private volatile long ntpTime;
    private volatile long ntpTimeReference;
    private volatile long roundTripTime;

    public boolean state = false;

    public synchronized void updateOffsetFromExternal(long serverUtcTime) {
        this.ntpTime = serverUtcTime;
        this.ntpTimeReference = SystemClock.elapsedRealtime();
        this.state = true; // 标记为已同步
    }

    public boolean requestTime(String host, int timeout) {
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeout);

            InetAddress address = InetAddress.getByName(host);

            byte[] buffer = new byte[NTP_PACKET_SIZE];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, NTP_PORT);

            // LI = 0
            // VN = 3
            // Mode = 3 (client)
            buffer[0] = 0x1B;

            long requestTime = System.currentTimeMillis();
            long requestTicks = SystemClock.elapsedRealtime();

            writeTimeStamp(buffer, INDEX_TRANSMIT_TIME, requestTime);

            socket.send(request);

            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);

            long responseTicks = SystemClock.elapsedRealtime();
            long responseTime = requestTime + (responseTicks - requestTicks);

            long originateTime = readTimeStamp(buffer, 24);
            long receiveTime = readTimeStamp(buffer, 32);
            long transmitTime = readTimeStamp(buffer, 40);

            long roundTripDelay = (responseTicks - requestTicks) - (transmitTime - receiveTime);

            long clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime)) / 2;

            synchronized (SntpClient.class){
                ntpTime = responseTime + clockOffset;
                ntpTimeReference = responseTicks;
                roundTripTime = roundTripDelay;
                state = true;
            }
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }

        return false;
    }

    public boolean isSuccess(){
        return state;
    }

    public long getNtpTime() {
        return ntpTime;
    }

    public long getNtpTimeReference() {
        return ntpTimeReference;
    }

    public long getRoundTripTime() {
        return roundTripTime;
    }

    /**
     * 当前 UTC 时间（动态计算）
     */
    public long currentTimeMillis() {
        return ntpTime + (SystemClock.elapsedRealtime() - ntpTimeReference);
    }

    private long read32(byte[] buffer, int offset) {
        byte b0 = buffer[offset];
        byte b1 = buffer[offset + 1];
        byte b2 = buffer[offset + 2];
        byte b3 = buffer[offset + 3];

        int i0 = (b0 & 0xFF);
        int i1 = (b1 & 0xFF);
        int i2 = (b2 & 0xFF);
        int i3 = (b3 & 0xFF);

        return ((long) i0 << 24) + ((long) i1 << 16) + ((long) i2 << 8) + i3;
    }

    private long readTimeStamp(byte[] buffer, int offset) {
        long seconds = read32(buffer, offset);
        long fraction = read32(buffer, offset + 4);

        return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((fraction * 1000L) / 0x100000000L);
    }

    private void writeTimeStamp(byte[] buffer, int offset, long time) {
        long seconds = time / 1000L;
        long milliseconds = time - (seconds * 1000L);

        seconds += OFFSET_1900_TO_1970;

        buffer[offset++] = (byte) (seconds >> 24);
        buffer[offset++] = (byte) (seconds >> 16);
        buffer[offset++] = (byte) (seconds >> 8);
        buffer[offset++] = (byte) (seconds);

        long fraction = milliseconds * 0x100000000L / 1000L;

        buffer[offset++] = (byte) (fraction >> 24);
        buffer[offset++] = (byte) (fraction >> 16);
        buffer[offset++] = (byte) (fraction >> 8);
        buffer[offset] = (byte) (fraction);
    }
}