package com.jingyen.notes

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.jingyen.notes.databinding.ActivityMainBinding
import com.squareup.sqldelight.android.AndroidSqliteDriver
import kotlinx.coroutines.*
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var typeface: Typeface
    private lateinit var typefaceBold: Typeface
    private lateinit var imm: InputMethodManager
    private val Int.toPx: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    private lateinit var androidSqlDriver: AndroidSqliteDriver
    private lateinit var notesQueries: NotesQueries
    private lateinit var notes: List<Note>
    private var sortByTime = false

    private var sensorManager: SensorManager? = null
    private val SHAKE_THRESHOLD_GRAVITY = 2.3f
    private val SHAKE_SLOP_TIME_MS = 500
    private val SHAKE_COUNT_RESET_TIME_MS = 3000
    private var mShakeTimestamp: Long = 0
    private var mShakeCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Backend.processing = true
        Backend.done = true

        typeface = Typeface.createFromAsset(assets,"regular.ttf")
        typefaceBold = Typeface.createFromAsset(assets,"semibold.ttf")

        binding.parent.layoutTransition.setDuration(100)
        window.navigationBarColor = resources.getColor(R.color.background, this.theme)

        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        binding.search.setOnFocusChangeListener { _, b ->
            if (b) {
                binding.head.visibility = View.GONE
                binding.title.visibility = View.GONE
                binding.sort.visibility = View.GONE
                binding.entries.translationY = 0f
                (binding.backlayout as ViewGroup).descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                binding.back.visibility = View.VISIBLE
                (binding.backlayout as ViewGroup).descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            } else {
                binding.head.visibility = View.VISIBLE
                binding.title.visibility = View.VISIBLE
                binding.sort.visibility = View.VISIBLE
                binding.entries.translationY = -binding.sortButtons.height.toFloat()
                binding.back.visibility = View.GONE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (Backend.processing)
            if (binding.back.visibility==View.VISIBLE) stopSearch(binding.back)
            GlobalScope.launch(Dispatchers.Main) {
                while (!Backend.done) delay(100L)
                withContext(Dispatchers.IO) { notes = sync() }
                sortByTime = false
                sort(binding.sortTime)

                // Attach here to prevent runtime errors
                binding.search.addTextChangedListener(object: TextWatcher {
                    override fun afterTextChanged(s: Editable?) {}
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        val sortedNotes = if (!sortByTime) notes.sortedBy { it.color } else notes
                        binding.entries.removeAllViews()
                        if (start+count>0) {
                            for (note in sortedNotes) { if (note.title.contains(s!!, ignoreCase = true) || note.text.contains(s!!, ignoreCase = true)) addEntry(note) }
                        } else {
                            for (note in sortedNotes) addEntry(note)
                        }
                    }
                })
            }
    }

    override fun onResume() {
        sensorManager?.registerListener(sensorListener, sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
        super.onResume()
    }

    private fun addEntry(note: Note) {
        val colorCode = when (note.color) {
            1 -> R.color.redPreview
            2 -> R.color.yellowPreview
            3 -> R.color.greenPreview
            4 ->  R.color.bluePreview
            5 ->  R.color.purplePreview
            else ->  R.color.greyPreview }

        val title = TextView(this)
        title.text = note.title
        title.typeface = typefaceBold
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        title.setTextColor(resources.getColor(R.color.text, this.theme))

        val text = TextView(this)
        text.text = note.text
        text.typeface = typeface
        text.maxLines = 10
        text.ellipsize = TextUtils.TruncateAt.END
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        text.setTextColor(resources.getColor(R.color.text, this.theme))

        val entry = LinearLayout(this)
        entry.orientation = LinearLayout.VERTICAL
        entry.setBackgroundColor(resources.getColor(colorCode, this.theme))
        if (note.title.isNotEmpty()) entry.addView(title)
        if (note.text.isNotEmpty()) entry.addView(text)
        entry.setPadding(16.toPx, 12.toPx, 16.toPx, 12.toPx)

        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(0, 0, 0, 10.toPx)
        entry.layoutParams = layoutParams
        entry.setOnClickListener { goToNotes(note.id) }
        binding.entries.addView(entry)
    }

    fun newEntry(v: View) {
        goToNotes(-1)
    }

    fun stopSearch(v: View) {
        binding.search.setText("")
        binding.search.clearFocus()
        binding.entries.requestFocus()
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    fun sort(v: View) {
        if ((v==binding.sortTime&&!sortByTime)||(v==binding.sortColor&&sortByTime)) {
            binding.entries.removeAllViews()
            sortByTime = if (v == binding.sortTime) { for (note in notes) addEntry(note); true }
            else { val sortedNotes = notes.sortedBy { it.color }; for (note in sortedNotes) addEntry(note); false }
            binding.sortTime.background = if (sortByTime) resources.getDrawable(R.drawable.sortbar, this.theme) else null
            binding.sortColor.background = if (!sortByTime) resources.getDrawable(R.drawable.sortbar, this.theme) else null
        }
        binding.sortButtons.visibility = View.INVISIBLE
        binding.entries.animate()
            .translationY(-binding.sortButtons.height.toFloat())
            .duration = 100
        binding.sortText.text = "Sort by: ${if (sortByTime) "Time" else "Colour"}"
    }

    fun showSort(v: View) {
        if (binding.sortButtons.visibility==View.INVISIBLE) {
            binding.sortButtons.visibility = View.VISIBLE
            binding.entries.animate()
                .translationY(0f)
                .duration = 100
        }
        else {
            binding.sortButtons.visibility = View.INVISIBLE
            binding.entries.animate()
                .translationY(-binding.sortButtons.height.toFloat())
                .duration = 100
        }
    }

    private fun goToNotes(id: Int) {
        val intent = Intent(this, NotesActivity::class.java)
        if (id > 0L) intent.putExtra("id", id)
        startActivity(intent)
    }

    private fun sync(): List<Note> {
        androidSqlDriver = AndroidSqliteDriver(schema = Database.Schema, context = applicationContext, name = "items.db")
        notesQueries = Database(androidSqlDriver).notesQueries
        return Backend.getAll(notesQueries)
    }

    override fun onBackPressed() {
        if (binding.back.visibility==View.VISIBLE) {
            stopSearch(binding.back)
        } else super.onBackPressed()
    }

    override fun onPause() {
        sensorManager?.unregisterListener(sensorListener);
        super.onPause()
    }

    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH
            // gForce will be close to 1 when there is no movement.
            val gForce: Float = sqrt(gX * gX + gY * gY + gZ * gZ)
            Log.d("damn", gForce.toString())
            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                val now = System.currentTimeMillis()
                // ignore shake events too close to each other (500ms)
                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return
                }
                // reset the shake count after 3 seconds of no shakes
                if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                    mShakeCount = 0
                }
                mShakeTimestamp = now
                mShakeCount++
                onShake()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    fun onShake() {
        sensorManager?.unregisterListener(sensorListener)
        Toast.makeText(this, "Shake to create!", Toast.LENGTH_SHORT).show()
        newEntry(binding.avd)
    }
}