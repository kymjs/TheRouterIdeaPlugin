package cn.therouter.idea.utils

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


fun doGet(text: String?): String? {
    var connection: HttpURLConnection? = null
    var br: BufferedReader? = null
    var result: String? = null // 返回结果字符串
    try {
        val url = URL(text)
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 60000
        connection.connect()
        if (connection.responseCode == 200) {
            br = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
            val sbf = StringBuffer()
            var temp: String?
            while (br.readLine().also { temp = it } != null) {
                sbf.append(temp)
                sbf.append("\r\n")
            }
            result = sbf.toString()
        }
    } catch (e: Exception) {
        throw e
    } finally {
        if (null != br) {
            try {
                br.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        connection?.disconnect()
    }
    return result
}