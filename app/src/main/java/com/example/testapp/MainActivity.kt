package com.example.testapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.uwb.gesture.GestureService

class MainActivity : AppCompatActivity() {

    private var gestureService: GestureService? = null
    private var isBound = false

    private lateinit var tvStatus:      TextView
    private lateinit var tvGesture:     TextView
    private lateinit var tvAction:      TextView
    private lateinit var btnTest:       Button

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            gestureService = (binder as GestureService.LocalBinder).getService()
            isBound = true
            tvStatus.text = "Status: Service Connected ✓"

            gestureService?.onGestureDetected = { gesture ->
                runOnUiThread {
                    tvGesture.text = "Gesture : ${gesture.name}"
                    tvAction.text  = "Action  : ${gesture.label}"
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            tvStatus.text = "Status: Disconnected"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus      = findViewById<TextView>(R.id.tvStatus)
        tvGesture     = findViewById<TextView>(R.id.tvGesture)
        tvAction      = findViewById<TextView>(R.id.tvAction)
        btnTest       = findViewById<Button>(R.id.btnTest)

        // Start GestureService
        val intent = Intent(this, GestureService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // Test inference button
        btnTest.setOnClickListener {
            if (!isBound) {
                tvStatus.text = "Status: Service not connected yet"
                return@setOnClickListener
            }

            try {
                fun loadCsv(filename: String): FloatArray {
                    val lines = assets.open(filename).bufferedReader().readLines()
                    Log.d("MainActivity", "Loaded $filename: ${lines.size} lines")
                    return FloatArray(lines.size) { lines[it].trim().toFloat() }
                }

                tvStatus.text = "Status: Loading CSV files..."
                val left  = loadCsv("test_left.csv")
                val right = loadCsv("test_right.csv")
                val top   = loadCsv("test_top.csv")

                Log.d("MainActivity", "left=${left.size} right=${right.size} top=${top.size}")
                Log.d("MainActivity", "left[0]=${left[0]} left[100]=${left[100]}")

                tvStatus.text = "Status: Running inference on G4 sample..."
                gestureService?.processUWBData(left, right, top)
                tvStatus.text = "Status: Inference complete ✓"

            } catch (e: Exception) {
                Log.e("MainActivity", "CSV load failed: ${e.message}")
                tvStatus.text = "Status: CSV load failed — ${e.message}"
            }
        }
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        super.onDestroy()
    }
}