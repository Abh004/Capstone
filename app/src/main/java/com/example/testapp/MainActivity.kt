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
    private var currentGesture = 1
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

            val gesture = "G$currentGesture"
            tvStatus.text = "Status: Testing $gesture..."

            try {
                fun loadCsv(filename: String): FloatArray {
                    val lines = assets.open("output/$filename").bufferedReader().readLines()
                    Log.d("MainActivity", "Loaded output/$filename: ${lines.size} lines")
                    return FloatArray(lines.size) { lines[it].trim().toFloat() }
                }

                val left  = loadCsv("test_left_$gesture.csv")
                val right = loadCsv("test_right_$gesture.csv")
                val top   = loadCsv("test_top_$gesture.csv")

                gestureService?.processUWBData(left, right, top)
                tvStatus.text = "Status: $gesture inference complete ✓"

            } catch (e: Exception) {
                tvStatus.text = "Status: Failed — ${e.message}"
                Log.e("MainActivity", "CSV load failed: ${e.message}")
            }

            currentGesture = if (currentGesture >= 12) 1 else currentGesture + 1
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