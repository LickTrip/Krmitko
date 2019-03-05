package com.licktrip.automaticfeeder.Services

import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.nio.charset.Charset
import java.util.*


/**
 * Created by Michal on 07.02.2019.
 */
class BluetoothConn{
    val LOG_TAG: String? = "BLE_CONNECTION" //set this to null to disable logging

    val SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    val CHAR_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    private var connectionChangedListener: ConnectionChangedListener? = null
    private var connection: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    private var device: BluetoothDevice? = null
    private var bluetoothManager: BluetoothManager? = null
    private var lastState: Int = BluetoothProfile.STATE_DISCONNECTED

    /**
     * Callback for receiving connection changes and updates.
     */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: kotlin.Int) {
            characteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(CHAR_UUID)
            if (characteristic != null) {
                LOG_TAG ?.let{Log.d(LOG_TAG, "characteristic found: ${characteristic?.uuid}")}
            } else {
                LOG_TAG ?.let{Log.d(LOG_TAG, "characteristic NOT found - check the UUID values")}
            }
            connectionChangedListener?.onConnectionChanged(characteristic != null)
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            var stateString = "UNKNOWN"
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> stateString = "CONNECTED"
                BluetoothProfile.STATE_CONNECTING -> stateString = "CONNECTING"
                BluetoothProfile.STATE_DISCONNECTED -> stateString = "DISCONNECTED"
                BluetoothProfile.STATE_DISCONNECTING -> stateString = "DISCONNECTING"
            }
            LOG_TAG ?.let{Log.d(LOG_TAG, "connection changed state to: $stateString")}

            if (newState != BluetoothProfile.STATE_CONNECTED) {
                //notify the listener only when the connection was broken or could not be created
                connectionChangedListener?.onConnectionChanged(false)
            } else if (lastState != BluetoothProfile.STATE_CONNECTED) {
                //we are connected after previously being disconnected
                if (connection?.discoverServices() == true) {
                    LOG_TAG ?.let{Log.d(LOG_TAG, "services discovery started")}
                } else {
                    // discovery was not started => notify the listener
                    connectionChangedListener?.onConnectionChanged(false)
                }
            }

            lastState = newState
        }
    }

    /**
     * Connect to BLE device with the provided MAC address (format: 20:91:48:BC:CA:BA).
     * The bluetooth must be turned on at this point or false will be returned.
     * Returns true if the connecting process been has been started (doesnt mean we can send data yet),
     * returns false if there was a problem
     */
    fun connect(context: Context, mac: String, connectionChangedListener: ConnectionChangedListener): Boolean {
        this.connectionChangedListener = connectionChangedListener
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            return false
        }

        val systemService = context.getSystemService(Context.BLUETOOTH_SERVICE)
        if (systemService is BluetoothManager) {
            bluetoothManager = systemService //todo can we get of this manual casting from temp val?
        }

        device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac)
        connection = device?.connectGatt(context, false, gattCallback) ?: return false
        return connection != null
    }

    /**
     * Same as [sendData(ByteArray)] with the String provided converted using the ASCII charset
     */
    fun sendData(data: String): Boolean {
        return sendData(data.toByteArray(Charset.forName("ASCII")))
    }

    /**
     * Send the data to the BLE device (by setting the [BluetoothGattCharacteristic] value).
     * @return FALSE is the connection was not ready for data sending or the was an error sending the data.
     * TRUE does NOT mean the data was reliably received by the BLE device
     */
    fun sendData(data: ByteArray): Boolean {
        if (!canSendData()) {
            LOG_TAG ?.let{ Log.d(LOG_TAG, "cannot send data")}
            return false
        }

        characteristic?.value = data
        return connection?.writeCharacteristic(characteristic) ?: false
    }

    /**
     * @return state of the connection, see [BluetoothManager.getConnectionState] and the returned constants
     */
    fun getConnectionStatus(): Int {
        //we have to use bluetoothManager since BluetoothGatt.getConnectionState is not available
        return bluetoothManager?.getConnectionState(device, BluetoothProfile.GATT)
                ?: BluetoothProfile.STATE_DISCONNECTED
    }

    /**
     * @return TRUE if the connection is ready for data sending
     */
    fun canSendData(): Boolean {
        return connection != null && characteristic != null && getConnectionStatus() == BluetoothProfile.STATE_CONNECTED
    }

    /**
     * Breaks the BLE connection if possible
     */
    fun disconnect() {
        if (canSendData()) {
            connection?.disconnect()
            connection = null
            characteristic = null
        }
    }

    interface ConnectionChangedListener {
        /**
         * Called when the connection status has changed (might be called multiple times for the same state change).
         * @param connected <code>TRUE</code> if the connection has been established, services discovered and we can send data.
         * <code>FALSE</code> if the connection has been disconnected or there was an error creating the connection       */
        fun onConnectionChanged(connected: Boolean)
    }
}