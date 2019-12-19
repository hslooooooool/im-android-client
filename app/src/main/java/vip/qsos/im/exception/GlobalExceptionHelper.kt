package vip.qsos.im.exception

import android.content.Context
import android.os.Environment
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author : 华清松
 * 全局异常处理帮助类
 */
class GlobalExceptionHelper(context: Context) : Thread.UncaughtExceptionHandler, Timber.Tree() {
    /**年月日*/
    private val mDayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    /**年月日时分秒*/
    private val mTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
    /**缓存文件保存路径 data\crash\*/
    private val mExceptionPath: String = "${context.externalCacheDir?.absoluteFile
        ?: Environment.getDataDirectory().path}" + "${File.separator}crash${File.separator}"

    override fun uncaughtException(t: Thread, e: Throwable) {
        Timber.e(e)
    }

    /**Timber日志输出*/
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        saveCrashFile(tag ?: "APP日志", "\n$message \n\nERROR>>>${t.toString()}\n")
    }

    /**
     * 保存错误信息到文件中
     */
    private fun saveCrashFile(type: String, value: String) {
        val sb = StringBuffer()
        val date = mTimeFormat.format(Date())
        sb.append("\r\n[$date]\n$type\n")
        sb.append("$value\n\n----------------------------华丽分割线----------------------------\n\n")
        writeFile(sb.toString())
    }

    private fun writeFile(sb: String) {
        try {
            val time = mDayFormat.format(Date())
            val fileName = "crash-$time.txt"
            val dir = File(mExceptionPath)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val fos = FileOutputStream(mExceptionPath + fileName, true)
            val osw = OutputStreamWriter(fos, "utf-8")
            osw.write(sb)
            osw.flush()
            osw.close()
            Timber.tag("存储异常日志").i("以上异常已记录到: ${mExceptionPath + fileName}")
        } catch (e: Exception) {
            Timber.tag("存储异常日志").i("以上异常记录失败")
        }
    }
}