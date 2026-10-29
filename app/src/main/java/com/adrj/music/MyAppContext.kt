package com.adrj.music

import android.content.Context

object MyAppContext {
    private var appContext: Context? = null
    fun set(ctx: Context) { appContext = ctx.applicationContext }
    fun get(): Context? = appContext
}
