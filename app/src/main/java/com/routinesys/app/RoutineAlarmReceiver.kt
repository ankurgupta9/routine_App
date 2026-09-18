package com.routinesys.app
import android.app.*; import android.content.*; import androidx.core.app.NotificationCompat
class RoutineAlarmReceiver:BroadcastReceiver(){
 override fun onReceive(ctx:Context,intent:Intent){
  val id=intent.getStringExtra("id") ?: return; val b=RoutineStore.load(ctx).firstOrNull{it.id==id} ?: return
  AlarmScheduler.channel(ctx)
  val open=PendingIntent.getActivity(ctx, id.hashCode()+10000, Intent(ctx,MainActivity::class.java).apply{putExtra("routine_id",id);flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP}, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val n=NotificationCompat.Builder(ctx,"routine_starts").setSmallIcon(com.routinesys.app.R.drawable.ic_stat_routine).setContentTitle("Start ${b.label}").setContentText("Starting now — ${b.start} to ${b.end}").setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(open).build()
  ctx.getSystemService(NotificationManager::class.java).notify(id.hashCode(),n); AlarmScheduler.scheduleNext(ctx,b)
 }
}
