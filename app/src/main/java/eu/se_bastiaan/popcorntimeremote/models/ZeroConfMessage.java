package eu.se_bastiaan.popcorntimeremote.models;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class ZeroConfMessage {
    private String message;
    private InetAddress ip;

    public ZeroConfMessage(String message) throws IllegalArgumentException {
        this(message, null);
    }

    public ZeroConfMessage(String message, InetAddress ip) throws IllegalArgumentException {
        this.ip = ip;
        this.message = message;
    }

    public String getMessage() { return message; }
    public InetAddress getSrcIp() { return ip; }
    public DatagramPacket getPacket(int port) {
        byte data[] = this.message.getBytes();
        return new DatagramPacket(data, data.length, getSrcIp(), port);
    }
}