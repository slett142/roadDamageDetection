package com.example.roaddamagedetection;


import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;

public class Tcp {
    public static final String TAG = TcpClient.class.getSimpleName();
    public static final String SERVER_IP = "128.101.170.62"; //server IP address
    public static final int SERVER_PORT = 8099;
    private byte[] mServerMessage;
    // sends message received notifications
    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;

//    private ByteArrayOutputStream baos;
    private DataOutputStream dos;
    private DataInputStream dis;
    private FileOutputStream fos;
    Socket sock;

    // predefined messages
    byte[] IMG = null;
    byte[] END = null;
    byte[] GPS = null;

    {
        try {
            IMG = "img".getBytes("UTF-8");
            END = "end".getBytes("UTF-8");
            GPS = "gps".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    public Tcp(OnMessageReceived listener){
        mMessageListener = listener;
        mServerMessage = new byte[1024];
    }

    public void sendMessages(final byte[][] messages){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (byte[] m : messages){
                    if(dos != null){
                        try {
                            Log.d(TAG, "Sending: " + new String(m, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        try {
                            dos.write(m);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void sendPacket(final byte[] data, final byte[] gps){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(dos != null){
                    try {
                        dos.write(IMG, 0, IMG.length);
                        dos.flush();
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "writing: "+ new String(data, "UTF-8"));
                        dos.write(data, 0, data.length);
                        dos.flush();
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        dos.write(END, 0, END.length);
                        dos.flush();
//                        try {
//                            Thread.sleep(500);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                        dos.write(GPS, 0, GPS.length);
                        dos.flush();
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "writing: "+ new String(gps, "UTF-8"));
                        dos.write(gps, 0, gps.length);
                        dos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void sendFile(final File file, final byte[] gps){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(dos != null){
                    try {
                        dos.write(IMG, 0, IMG.length);
//                        dos.flush();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "writing: "+ file.getName());
                        fos = new FileOutputStream(file.getAbsolutePath());
                        fos.flush();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        dos.write(END, 0, END.length);
//                        dos.flush();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        dos.write(GPS, 0, GPS.length);
//                        dos.flush();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "writing: "+ new String(gps, "UTF-8"));
                        dos.write(gps, 0, gps.length);
                        dos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();

    }

    public void stopClient(){
        mRun = false;

        try {
            dos.close();
            dis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run(){
        mRun = true;

        try {
            InetAddress addr = InetAddress.getByName(SERVER_IP);
            Log.d("TCP", "Connecting...");
            sock = new Socket(addr, SERVER_PORT);
            try {
//                baos = new ByteArrayOutputStream();
                dos = new DataOutputStream(sock.getOutputStream());
                dis = new DataInputStream(sock.getInputStream());

                while(mRun){
                    int len = dis.read(mServerMessage);

                    if(mServerMessage != null && mMessageListener != null){
                        mMessageListener.messageReceived(new String(mServerMessage, "UTF-8"));
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                sock.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendData(byte[] data){
        try {
            dos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] recvData(){
        byte[] data = null;
        try {
            int len = dis.readInt();
            data = new byte[len];
            if(len > 0){
                dis.readFully(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public interface OnMessageReceived {
        public void messageReceived(String message);
    }

}
