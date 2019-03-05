package com.licktrip.automaticfeeder.Services

import android.content.Context
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader




class SettingsChanger {

    companion object {
        const val fileName:String = "feeder_settings.txt"
    }

    fun changeSetting(value: String, time: String, bConn: BluetoothConn){
        if (value == "1" || value == "2" || value == "3" || value == "4" || value == "5" || value == "6" || value == "7" || value == "8" || value == "9"){
            bConn.sendData("s" + time + "00" + value)
        }else{
            bConn.sendData("s" + time + "0" + value)
        }
        Thread.sleep(1000)
    }

    fun changeSetting(value: Int, time: String, bConn: BluetoothConn){
        if (value < 10){
            bConn.sendData("s" + time + "00" + value.toString())
        }else{
            bConn.sendData("s" + time + "0" + value.toString())
        }
        Thread.sleep(1000)
    }

    fun writeToStorage(context : Context, data: String){
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = context.openFileOutput(Companion.fileName, Context.MODE_PRIVATE)
            fileOutputStream.write(data.toByteArray())
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun readFromStorage(context: Context): String{
        var fileInputStream: FileInputStream? = null
        fileInputStream = context.openFileInput(Companion.fileName)
        var inputStreamReader: InputStreamReader = InputStreamReader(fileInputStream)
        val bufferedReader: BufferedReader = BufferedReader(inputStreamReader)
        val stringBuilder: StringBuilder = StringBuilder()
        var text: String? = null
        while ({ text = bufferedReader.readLine(); text }() != null) {
            stringBuilder.append(text)
        }
        return stringBuilder.toString()
    }

    fun fileExist(context: Context): Boolean {
        val file = context.getFileStreamPath(fileName)
        return file.exists()
    }

    fun splitData(data: String): List<String>{
        return data.split("-")
    }


}