package vip.qsos.im

import android.app.Application
import timber.log.Timber
import vip.qsos.im.exception.GlobalExceptionHelper

/**
 * @author : 华清松
 * AppApplication
 */
open class AppApplication : Application() {
    companion object {
        var testAccount: String = "Sender"
    }

    override fun onCreate() {
        super.onCreate()
        val exceptionHelper = GlobalExceptionHelper(this)
        /**Timber 日志*/
        Timber.plant()
        /**全局异常捕获处理*/
        Thread.setDefaultUncaughtExceptionHandler(exceptionHelper)
    }
}