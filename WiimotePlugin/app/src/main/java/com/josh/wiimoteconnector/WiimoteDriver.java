package com.josh.wiimoteconnector;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;

import junit.framework.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Josh on 2/20/2016.
 */
public class WiimoteDriver
{
    private static final int REQUEST_ENABLE_BT = 1;

    private Activity context;

    private BroadcastReceiver discoveryReceiver;
    private IntentFilter discoveryFilter;
    private BroadcastReceiver pairReceiver;
    private IntentFilter pairFilter;

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothDevice wiimote;
    private BluetoothSocket socket;

    private String status = "";

    public String getStatus()
    {
        String stat = status;
        status = "";
        return stat;
    }

    private void logStatus(String stat)
    {
        status += stat + "\n";
        Log.d("WiimoteDriver", stat);
    }

    private String error = "";

    public String getError()
    {
        String err = error;
        error = "";
        return err;
    }

    private void logError(String err)
    {
        logStatus("ERROR");
        error += err + "\n";
        Log.e("WiimoteDriver", err);
    }

    public WiimoteDriver(Activity c)
    {
        context = c;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null)
        {
            logError("Bluetooth adapter could not be found!");
        }

        discoveryReceiver = new BroadcastReceiver()
        {
            public void onReceive(Context context, Intent intent)
            {
                HandleDiscoveryIntent(context, intent);
            }
        };
        pairReceiver = new BroadcastReceiver()
        {
            public void onReceive(Context context, Intent intent)
            {
                HandlePairIntent(context, intent);
            }
        };

        logStatus("WiimoteDriver successfully instantiated.");
    }

    public void Connect()
    {
        if (!bluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            logStatus("Enabling BlueTooth");
        }

        discoveryFilter = new IntentFilter(Intent.CATEGORY_DEFAULT);
        discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        discoveryFilter.addAction(BluetoothDevice.ACTION_FOUND);
        discoveryFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(discoveryReceiver, discoveryFilter); // Don't forget to unregister during onDestroy

        if (bluetoothAdapter.startDiscovery())
        {
            logStatus("Discovery started");
        }
        else
        {
            logError("Could not start discovery");
        }
    }

    private void HandleDiscoveryIntent(Context context, Intent intent)
    {
        String action = intent.getAction();
        // When discovery finds a device
        if (BluetoothDevice.ACTION_FOUND.equals(action))
        {
            logStatus("Device found");
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            // Add the name and address to an array adapter to show in a ListView

            String devicename = device.getName();
            logStatus("Device found: " + devicename + " " + device.getAddress());

            boolean nintendo = false;
            try
            {
                nintendo = devicename.contains("Nintendo");
            }
            catch (Exception ex)
            { }

            if (nintendo)
            {
                bluetoothAdapter.cancelDiscovery();
                PairWiimote(device);
            }
        }
        else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
        {
            logStatus("Discovery finished");
            context.unregisterReceiver(discoveryReceiver);
        }
        else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
        {
            logStatus("ACTION_BOND_STATE_CHANGED received");
            BluetoothDevice[] devices = bluetoothAdapter.getBondedDevices().toArray(new BluetoothDevice[0]);

            for (int i = 0; i < devices.length; i++)
            {
                logStatus("Bound to device " + devices[i].getName());
            }
        }
        else
        {
            logError("Unknown intent received when discovering: " + action);
        }
    }

    private void PairWiimote(BluetoothDevice device)
    {
        wiimote = device;
        if (TestBondState(wiimote.getBondState()))
        {
            //already bonded, try connecting
            ConnectToDevice();
        }
        else
        {
            Bond();
        }
    }

    private void Bond()
    {
        pairFilter = new IntentFilter(Intent.CATEGORY_DEFAULT);
        pairFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        pairFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        context.registerReceiver(pairReceiver, pairFilter); // Don't forget to unregister during onDestroy

        if (wiimote.createBond())
        {
            logStatus("Bonding started");
        }
        else
        {
            logError("Unable to start bonding");
        }
    }

    private void HandlePairIntent(Context context, Intent intent)
    {
        String action = intent.getAction();
        logStatus("Got " + action);
        // When discovery finds a device
        if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action))
        {
            if (TestBondState(intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)))
            {
                context.unregisterReceiver(pairReceiver);
                ConnectToDevice();
            }
        }
        else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action))
        {
            String addressString = wiimote.getAddress();
            String[] addressBytes = addressString.split(":");
            byte[] pin = new byte[6];
            for (int i = addressBytes.length - 1; i >= 0; i--)
            {
                int val = Integer.valueOf(addressBytes[i], 16);
                pin[5 - i] += (byte) val;
            }

            if (wiimote.setPin(pin))
            {
                logStatus("PIN set");
            }
            else
            {
                logError("Error setting PIN");
                return;
            }
        }
        else
        {
            logError("Unknown intent received when pairing: " + action);
        }
    }

    private void ConnectToDevice()
    {
        logStatus("Getting UUIDs");
        ParcelUuid[] uuids = wiimote.getUuids();
        logStatus("UUIDs found: " + uuids.length);
        if (uuids.length > 0)
        {
            try
            {
                socket = wiimote.createInsecureRfcommSocketToServiceRecord(uuids[0].getUuid());
                logStatus("Got socket, attempting to connect");
                socket.connect();
            }
            catch (Exception ex)
            {
                logError(ex.toString());
            }
        }
    }

    private boolean TestBondState(int state)
    {
        boolean bonded = false;
        switch (state)
        {
            case BluetoothDevice.BOND_NONE:
                logStatus("Got BOND_NONE");
                break;
            case BluetoothDevice.BOND_BONDING:
                logStatus("Got BOND_BONDING");
                break;
            case BluetoothDevice.BOND_BONDED:
                logStatus("Got BOND_BONDED");
                bonded = true;
                break;
            default:
                logError("Got other bonding value: " + state);
                break;
        }
        return bonded;
    }
}