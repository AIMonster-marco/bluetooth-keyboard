package com.example.bluetoothkeyboard

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private lateinit var textView: TextView
    private lateinit var editText: EditText
    private lateinit var connectButton: Button
    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (it.name != null) {
                            Toast.makeText(context, "Found device: ${it.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_ON -> {
                            Toast.makeText(context, "블루투스가 켜졌습니다", Toast.LENGTH_SHORT).show()
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            Toast.makeText(context, "블루투스가 꺼졌습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        textView = findViewById(R.id.textView)
        editText = findViewById(R.id.editText)
        connectButton = findViewById(R.id.connectButton)

        // 블루투스 상태 확인
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "이 기기는 블루투스를 지원하지 않습니다", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "블루투스가 꺼져있습니다. 블루투스를 켜주세요", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "블루투스가 켜져있습니다", Toast.LENGTH_SHORT).show()
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(bluetoothReceiver, filter)

        connectButton.setOnClickListener {
            if (bluetoothAdapter.isEnabled) {
                startBluetoothDiscovery()
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, 1)
            }
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    textView.text = it
                    sendData(it.toString())
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "블루투스가 켜졌습니다", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "블루투스 활성화가 취소되었습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startBluetoothDiscovery() {
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            Toast.makeText(this, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendData(data: String) {
        try {
            outputStream?.write(data.toByteArray())
        } catch (e: IOException) {
            Toast.makeText(this, "Failed to send data", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
} 