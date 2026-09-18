package com.routinesys.app
import android.content.*
class BootReceiver:BroadcastReceiver(){override fun onReceive(ctx:Context,intent:Intent){AlarmScheduler.scheduleAll(ctx)}}
