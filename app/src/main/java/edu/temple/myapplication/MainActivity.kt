package edu.temple.myapplication

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {


    // commit test 2

    private var binder: TimerService.TimerBinder? = null

    private lateinit var textView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private var timerService: TimerService.TimerBinder? = null

    private var currentNum = 100

    private val prefs by lazy { getSharedPreferences("countdown_pref", Context.MODE_PRIVATE) }

    private val handler = Handler(Looper.getMainLooper()) {
        msg -> currentNum = msg.what // bruh ?
        textView.text = currentNum.toString()

        if (currentNum <= 0) {
            clearSaved()
        }

        true
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as TimerService.TimerBinder
            binder?.setHandler(handler)
            updateUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        currentNum = getStartValue()
        textView.text = currentNum.toString()

        bindService(Intent(this, TimerService::class.java), connection, BIND_AUTO_CREATE)

        startButton.setOnClickListener {
            val b = binder ?: return@setOnClickListener

            if (b.isRunning) {
                b.pause()
                saveCurrent()
            }
            else {
                currentNum = getStartValue()
                textView.text = currentNum.toString()
                b.start(currentNum)
            }

            updateUI()
        }

        stopButton.setOnClickListener {
            binder?.stop()

            clearSaved()

            currentNum = 100
            textView.text = currentNum.toString()
            updateUI()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                println("settings clicked")
                true
            }
            R.id.action_about -> {
                println("wifi clicked")
                openSettings(Settings.ACTION_WIFI_SETTINGS)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openSettings(action: String) {
        val intent = Intent(action)
        launchIntent(intent)
    }

    private fun launchIntent(intent: Intent) {
        try {
            startActivity(intent)
        }
        catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "u mess up wifi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI() {
        val b = binder ?: return

        if (b.isRunning) {
            startButton.text = "pause"
        }
        else if (hasSaved()) {
            startButton.text = "resume"
        }
        else {
            startButton.text = "start"
        }

        stopButton.text = "stop"
    }

    // timer saver helper func
    private fun getStartValue(): Int {
        return prefs.getInt("saved_count", 100)
    }

    private fun saveCurrent() {
        prefs.edit().putInt("saved_count", currentNum).apply()
    }

    private fun clearSaved() {
        prefs.edit().remove("saved_count").apply()
    }

    private fun hasSaved(): Boolean {
        return prefs.contains("saved_count")
    }

    override fun onStop() {
        super.onStop()

        if (binder?.isRunning == true) {
            clearSaved()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}
