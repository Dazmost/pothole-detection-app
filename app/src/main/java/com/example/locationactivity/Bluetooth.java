package com.example.locationactivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class Bluetooth {

    private final String DEVICE_ADDRESS="60:64:05:91:56:67";
    private final UUID PORT_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"); //Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    boolean deviceConnected=false;
    Thread thread;
    byte buffer[];
    int bufferPosition;
    boolean stopThread;
    final Handler handler = new Handler();

    public Handler getHandler()
    {
        return handler;
    }

    public Bluetooth()
    {
        BTInit();
        BTConnect();
    }

    private boolean BTInit()
    {
        Log.d("BT", "Starting BT code");
        boolean found=false;
        BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Log.d("BT", "BT adapter is null");
        }

        if(!bluetoothAdapter.isEnabled())
        {
            //Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableAdapter, 0);

            //try {
            //    Thread.sleep(1000);
            //} catch (InterruptedException e) {
            //    e.printStackTrace();
            //}
            Log.d("BT", "BT Adapter is not enabled");
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        if(bondedDevices.isEmpty())
        {
            Log.d("BT", "No bonded devices");
        }
        else
        {
            for (BluetoothDevice iterator : bondedDevices)
            {
                if(iterator.getAddress().equals(DEVICE_ADDRESS))
                {
                    device=iterator;
                    found=true;
                    Log.d("BT", "Found BT device");
                    break;
                }
            }
        }
        return found;
    }

    private boolean BTConnect()
    {
        boolean connected=true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
            Log.d("BT", "Connected to BT");
        } catch (IOException e) {
            e.printStackTrace();
            connected=false;
        }
        if(connected)
        {
            try {
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream=socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return connected;
    }

    public void BTStop() throws IOException
    {
        stopThread = true;
        outputStream.close();
        inputStream.close();
        socket.close();
        deviceConnected=false;
    }

    void beginListenForData()
    {

        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        final int byteCount = inputStream.available();
                        if(byteCount > 9)
                        {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            Log.d("BT", String.valueOf(rawBytes));

                            Message msg = new Message();
                            Bundle bndle = new Bundle();
                            bndle.putByteArray("data", rawBytes);
                            msg.setData(bndle);
                            handler.sendMessage(msg);

                        }
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }
}
