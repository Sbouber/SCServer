package org.opencv.samples.facedetect;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Represents a message, send from client to server or vice versa.
 * Note that this class is not written to be extended.
 */
public class Message implements Serializable {

    static final long serialVersionUID = 42L;

    public static final int MSG_JOIN = 0;
    public static final int MSG_ACKJOIN = 1;
    public static final int MSG_NACKJOIN = 2;
    public static final int MSG_REQGAME = 3;
    public static final int MSG_ACKREQ = 4;
    public static final int MSG_NACKREQ = 5;
    public static final int MSG_SENDBLINK = 6;
    public static final int MSG_ENDGAME = 7;
    public static final int MSG_SENDF = 8;
    public static final int MSG_WON = 9;
    public static final int MSG_LOST = 10;
    public static final int MSG_DISC = 11;
    public static final int MSG_STARTGAME = 12;
    public static final int MSG_LEFT = 13;

    public final int type;
    public final int[] frame;
    public final String from;
    public final String to;
    public final long time;

    public Message(String from, String to, int[] frame, int type, long time) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.frame = frame;
        this.time = time;
    }

}
