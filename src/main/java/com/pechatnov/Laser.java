package com.pechatnov;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by ura on 03.10.15.
 */
public class Laser {
    private FileOutputStream laser_pipe;

    private final byte LASER_CMD = 1;
    private final byte LASER_OFF_CMD = 0;
    private final byte LASER_ON_CMD = 1;
    private final byte LASER_SHOUT_CMD = 2;
    private final byte MOVE_CMD = 2;
    private final byte LCD_CMD = 3;
    private final byte FREQ_CMD = 4;

    private int cx, cy;
    private int intensity = 60;
    private int minIntensity = 0;

    private boolean useGreyscale = false;

    private int sign(int a) {
        if (a < 0)
            return -1;
        if (a > 0)
            return 1;
        return 0;
    }

    public void setIntensity(int new_intense) {
        intensity = new_intense;
    }
    public Integer getIntensity() {
        return intensity;
    }

    public void setMinIntensity(int newMinIntense) {
        minIntensity = newMinIntense;
    }
    public Integer getMinIntensity() {
        return minIntensity;
    }

    public Integer getX() {
        return cx;
    }
    public Integer getY() {
        return cy;
    }

    public boolean isUseGreyscale() {
        return useGreyscale;
    }

    public void setUseGreyscale(boolean useGreyscale) {
        this.useGreyscale = useGreyscale;
    }

    private static byte[] intToBytes(int a) {
        return ByteBuffer.allocate(4).putInt(a).array();
    }


    public void write(byte[] data) throws LaserConnectException {
        for (int i = 0; i < 5; i++) {
            try {
                laser_pipe.write(data);
                return;
            } catch (IOException ignored) {}
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}
        }
        throw new LaserConnectException();
    }

    public void on() throws LaserConnectException {
        write(new byte[] {LASER_CMD, LASER_ON_CMD});
    }

    public void off() throws LaserConnectException {
        write(new byte[] {LASER_CMD, LASER_OFF_CMD});
    }

    public byte[] reverse(byte[] word) {
        byte[] ret = new byte[word.length];
        for (int i = 0; i < word.length; i++)
            ret[i] = word[word.length - 1 - i];
        return ret;
    }

    protected void fire(int length) throws LaserConnectException {
        byte[] cmd = {LASER_CMD, LASER_SHOUT_CMD, 0, 0, 0, 0};
        System.arraycopy(reverse(ByteBuffer.allocate(4).putInt(length).array()), 0, cmd, 2, 4);
        write(cmd);

    }

    /* "P" from "percents" */
    public void fireP(int percentage) throws LaserConnectException {
        if (percentage == 0)
            return;
        if (!useGreyscale)
            percentage = 100;
        fire(minIntensity + (intensity - minIntensity) * percentage / 100);
    }

    public void makeDot() throws LaserConnectException {
        fire(intensity);
    }

    public void move(int x, int y) throws LaserConnectException {
        byte[] cmd = {MOVE_CMD, 0, 0, 0, 0, 0, 0, 0, 0};
        System.arraycopy(reverse(ByteBuffer.allocate(4).putInt(x).array()), 0, cmd, 1, 4);
        System.arraycopy(reverse(ByteBuffer.allocate(4).putInt(y).array()), 0, cmd, 5, 4);
        write(cmd);
        cx += x;
        cy += y;
    }

    public void print(String[] strs) throws LaserConnectException {
        byte[] cmd = new byte[33];
        cmd[0] = LCD_CMD;
        for (int i = 1; i < 33; i++)
            cmd[i] = ' ';
        System.arraycopy(strs[0].getBytes(), 0, cmd, 1, Math.min(16, strs[0].length()));
        System.arraycopy(strs[1].getBytes(), 0, cmd, 17, Math.min(16, strs[1].length()));
        write(cmd);
    }

    public void setAs00() {
        cx = cy = 0;
    }

    public void goTo(int x, int y) throws LaserConnectException {
        move(x - cx, y - cy);
    }
    public void goToStrict(int x, int y) throws LaserConnectException {
        int crX = 0, crY = 0;
        if (x - cx < 0) crX = 16;
        if (y - cy < 0) crY = 16;
        move(x - cx - crX, y - cy - crY);
        move(crX, crY);
    }

    Laser() throws LaserConnectException {
        try {
            laser_pipe = new FileOutputStream("/home/root/laser_fifo");
        } catch (IOException e) {
            throw new LaserConnectException();
        }
    }


}
