package Burnie;

import java.nio.ByteBuffer;

public class Message {
    private final byte[] packet;

    public Message(byte[] msg, boolean forController) {
        if (forController) {
            packet = msg;
            return;
        }
        byte[] msgLength = ByteBuffer.allocate(4).putInt(msg.length).array();
        packet = new byte[msgLength.length+msg.length];
        for (int i = 0; i < packet.length; i++) {
            packet[i] = (i < 4) ? msgLength[i] : msg[i-4];
        }
    }

    public Message(byte[] msg) {this(msg, false);}

    public byte[] getMessage() {
        return packet;
    }
}
