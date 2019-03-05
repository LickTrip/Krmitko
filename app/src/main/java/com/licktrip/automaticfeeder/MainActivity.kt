package com.licktrip.automaticfeeder

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.*
import com.licktrip.automaticfeeder.Model.MyConstants
import com.licktrip.automaticfeeder.Services.BluetoothConn
import com.licktrip.automaticfeeder.Services.SettingsChanger
import com.licktrip.automaticfeeder.Services.VariableListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    private var mBlueToothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val REQUEST_ENABLE_BT: Int = 1

    private lateinit var bConn: BluetoothConn
    private lateinit var onlineListener: VariableListener
    private lateinit var settingsChanger: SettingsChanger
    private lateinit var progressDialog: ProgressDialog

    private lateinit var option1: Spinner
    private lateinit var option2: Spinner
    private lateinit var option3: Spinner
    private lateinit var option4: Spinner
    private lateinit var option5: Spinner
    private val spinnerTimeList = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24")

    private lateinit var mySeek: SeekBar
    private lateinit var textViewSeek: TextView

    private var seekInt: Int = 0

    private lateinit var result1: String
    private lateinit var result2: String
    private lateinit var result3: String
    private lateinit var result4: String
    private lateinit var result5: String

    private lateinit var storedData: List<String>

    companion object {
        const val empty: String = "empty"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        bConn = BluetoothConn()
        onlineListener = VariableListener()
        settingsChanger = SettingsChanger()

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Connecting..", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            getConnection(view)
        }

        activateSwitches()
        generateSpinners()
        activateSeekBar()
        readEEPROM()

        onlineListener.setListener(object : VariableListener.ChangeListener {
            override fun onChange() {
                progressDialog.dismiss()
                if (onlineListener.isBoo())
                    onlineBaby()
                else
                    offlineMode()
            }
        })
    }

    private fun getConnection(view: View) {
        callProgressDialog()
        val isConnStart = bConn.connect(this, MyConstants.MAC_ADDRESS, object : BluetoothConn.ConnectionChangedListener {
            override fun onConnectionChanged(connected: Boolean) {
                runOnUiThread {
                    if (!connected) {
                        onlineListener.setBoo(false)
                        Snackbar.make(view, "Disconnected", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show()
                    } else {
                        onlineListener.setBoo(true)
                        Snackbar.make(view, "Online baby", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show()
                    }
                }
            }
        })

        if (!isConnStart) {
            return
        }
    }

    private fun readEEPROM() {
        if (!settingsChanger.fileExist(this)) {
            setDefaultFcn()
        }
        storedData = settingsChanger.splitData(settingsChanger.readFromStorage(this))

        if (storedData.size != MyConstants.EEPROM_SIZE) {
            setDefaultFcn()
            storedData = settingsChanger.splitData(settingsChanger.readFromStorage(this))
        }

        refreshComponents()
    }

    fun sendMess(view: View) {
        bConn.sendData(MyConstants.ORDER_FEED)
        Snackbar.make(view, "Feeding..", Snackbar.LENGTH_SHORT).show()
    }

    fun setSettings(view: View) {
        var data: String
        if (switch_feed_q.isChecked) {
            settingsChanger.changeSetting(seekInt, "0", bConn)
            data = "$seekInt-"
        } else {
            data = storedData[0] + "-"
        }

        if (switch_time1.isChecked && result1 != empty) {
            settingsChanger.changeSetting(result1, "1", bConn)
            data = "$data$result1-"
        } else {
            data = data + storedData[1] + "-"
        }

        if (switch_time2.isChecked && result2 != empty) {
            settingsChanger.changeSetting(result2, "2", bConn)
            data = "$data$result2-"
        } else {
            data = data + storedData[2] + "-"
        }

        if (switch_time3.isChecked && result3 != empty) {
            settingsChanger.changeSetting(result3, "3", bConn)
            data = "$data$result3-"
        } else {
            data = data + storedData[3] + "-"
        }

        if (switch_time4.isChecked && result4 != empty) {
            settingsChanger.changeSetting(result4, "4", bConn)
            data = "$data$result4-"
        } else {
            data = data + storedData[4] + "-"
        }

        if (switch_time5.isChecked && result5 != empty) {
            settingsChanger.changeSetting(result5, "5", bConn)
            data = "$data$result5"
        } else {
            data += storedData[5]
        }

        settingsChanger.writeToStorage(this, data)
        Thread.sleep(1000)
        readEEPROM()

        Snackbar.make(view, "Setting set successfully", Snackbar.LENGTH_SHORT).show()
    }

//    fun testMess(view: View) {
//        //bConn.sendData(editTextTesting.text.toString())
//    }

    fun setDefault(view: View) {
        setDefaultFcn()
        Snackbar.make(view, "Default setting set successfully", Snackbar.LENGTH_SHORT).show()
    }

    private fun setDefaultFcn() {
        bConn.sendData("x")
        settingsChanger.writeToStorage(this, MyConstants.FACTORY_DATA)
        Thread.sleep(1000)
        storedData = settingsChanger.splitData(settingsChanger.readFromStorage(this))
        refreshComponents()
    }

    private fun refreshComponents() {
        spinner_time1.setSelection(storedData[1].toInt())
        spinner_time2.setSelection(storedData[2].toInt())
        spinner_time3.setSelection(storedData[3].toInt())
        spinner_time4.setSelection(storedData[4].toInt())
        spinner_time5.setSelection(storedData[5].toInt())
        seek_quantity.progress = 0
        seek_quantity.progress = storedData[0].toInt()
    }

    private fun activateSeekBar() {
        mySeek = seek_quantity
        textViewSeek = textView_seek_quantity
        mySeek.max = 100
        mySeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                seekInt = p1
                textViewSeek.text = p1.toString()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })
    }

    private fun activateSwitches() {
        switch_time1.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                spinner_time1.isEnabled = true
                spinner_time1.visibility = Spinner.VISIBLE
            } else {
                spinner_time1.isEnabled = false
                spinner_time1.visibility = Spinner.INVISIBLE
            }
        }

        switch_time2.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                spinner_time2.isEnabled = true
                spinner_time2.visibility = Spinner.VISIBLE
            } else {
                spinner_time2.isEnabled = false
                spinner_time2.visibility = Spinner.INVISIBLE
            }
        }

        switch_time3.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                spinner_time3.isEnabled = true
                spinner_time3.visibility = Spinner.VISIBLE
            } else {
                spinner_time3.isEnabled = false
                spinner_time3.visibility = Spinner.INVISIBLE
            }
        }

        switch_time4.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                spinner_time4.isEnabled = true
                spinner_time4.visibility = Spinner.VISIBLE
            } else {
                spinner_time4.isEnabled = false
                spinner_time4.visibility = Spinner.INVISIBLE
            }
        }

        switch_time5.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                spinner_time5.isEnabled = true
                spinner_time5.visibility = Spinner.VISIBLE
            } else {
                spinner_time5.isEnabled = false
                spinner_time5.visibility = Spinner.INVISIBLE
            }
        }

        switch_feed_q.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mySeek.visibility = SeekBar.VISIBLE
                textViewSeek.visibility = TextView.VISIBLE
            } else {
                mySeek.visibility = SeekBar.INVISIBLE
                textViewSeek.visibility = TextView.INVISIBLE
            }
        }
    }

    private fun generateSpinners() {
        option1 = spinner_time1
        option1.adapter = ArrayAdapter<String>(this, R.layout.spinner_items_styles, spinnerTimeList)

        option1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                result1 = empty
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                result1 = spinnerTimeList[p2]
            }
        }

        option2 = spinner_time2
        option2.adapter = ArrayAdapter<String>(this, R.layout.spinner_items_styles, spinnerTimeList)

        option2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                result2 = empty
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                result2 = spinnerTimeList[p2]
            }
        }

        option3 = spinner_time3
        option3.adapter = ArrayAdapter<String>(this, R.layout.spinner_items_styles, spinnerTimeList)

        option3.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                result3 = empty
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                result3 = spinnerTimeList[p2]
            }
        }

        option4 = spinner_time4
        option4.adapter = ArrayAdapter<String>(this, R.layout.spinner_items_styles, spinnerTimeList)

        option4.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                result4 = empty
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                result4 = spinnerTimeList[p2]
            }
        }

        option5 = spinner_time5
        option5.adapter = ArrayAdapter<String>(this, R.layout.spinner_items_styles, spinnerTimeList)

        option5.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                result5 = empty
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                result5 = spinnerTimeList[p2]
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    fun onlineBaby() {
        fab.backgroundTintList = ColorStateList.valueOf(Color
                .parseColor("#36b50c"))
        sendOrder.isEnabled = true
        switch_feed_q.isEnabled = true
        switch_time1.isEnabled = true
        switch_time2.isEnabled = true
        switch_time3.isEnabled = true
        switch_time4.isEnabled = true
        switch_time5.isEnabled = true
        setDefaultBtn.isEnabled = true
        setSettingBtn.isEnabled = true
        textViewW2.visibility = TextView.GONE
        textViewW3.visibility = TextView.VISIBLE
        textViewW4.visibility = TextView.VISIBLE
        textViewW5.visibility = TextView.VISIBLE
    }

    @SuppressLint("ResourceAsColor")
    fun offlineMode() {
        fab.backgroundTintList = ColorStateList.valueOf(Color
                .parseColor("#1c1d20"))
        sendOrder.isEnabled = false
        switch_feed_q.isEnabled = false
        switch_time1.isEnabled = false
        switch_time2.isEnabled = false
        switch_time3.isEnabled = false
        switch_time4.isEnabled = false
        switch_time5.isEnabled = false
        setDefaultBtn.isEnabled = false
        setSettingBtn.isEnabled = false
        textViewW2.visibility = TextView.VISIBLE
        textViewW3.visibility = TextView.GONE
        textViewW4.visibility = TextView.GONE
        textViewW5.visibility = TextView.GONE
    }

    private fun callProgressDialog() {
        progressDialog = ProgressDialog(this, R.style.ThemeMyProgressDialog)
        progressDialog.setMessage("Connecting..")
        progressDialog.setCancelable(false)
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Large)
        progressDialog.show()
    }
}
