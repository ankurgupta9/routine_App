package com.routinesys.app

import android.app.*
import android.content.*
import android.os.Build
import java.util.*

object AlarmScheduler {
    private const val CHANNEL="routine_starts"
    fun channel(ctx:Context){ if(Build.VERSION.SDK_INT>=26){val nm=ctx.getSystemService(NotificationManager::class.java); nm.createNotificationChannel(NotificationChannel(CHANNEL,"Routine starts",NotificationManager.IMPORTANCE_HIGH).apply{description="Notifications when a routine block starts"})} }
    fun scheduleAll(ctx:Context){ channel(ctx); RoutineStore.load(ctx).forEach{scheduleNext(ctx,it)} }
    fun scheduleNext(ctx:Context,b:RoutineBlock){
        val now=Calendar.getInstance(); val best=Calendar.getInstance(); var found=false
        for(offset in 0..7){ val c=Calendar.getInstance(); c.timeInMillis=now.timeInMillis; c.add(Calendar.DAY_OF_YEAR,offset); if(!b.days.contains(c.get(Calendar.DAY_OF_WEEK)-1)) continue; val p=b.start.split(":"); c.set(Calendar.HOUR_OF_DAY,p[0].toInt()); c.set(Calendar.MINUTE,p[1].toInt()); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); if(c.timeInMillis<=now.timeInMillis) continue; best.timeInMillis=c.timeInMillis; found=true; break }
        if(!found)return
        val am=ctx.getSystemService(AlarmManager::class.java); val intent=Intent(ctx,RoutineAlarmReceiver::class.java).apply{putExtra("id",b.id)}; val pi=PendingIntent.getBroadcast(ctx,b.id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if(Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,best.timeInMillis,pi) else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,best.timeInMillis,pi)
    }
    fun cancel(ctx:Context,b:RoutineBlock){val i=Intent(ctx,RoutineAlarmReceiver::class.java);val pi=PendingIntent.getBroadcast(ctx,b.id.hashCode(),i,PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE); if(pi!=null)ctx.getSystemService(AlarmManager::class.java).cancel(pi)}
}
