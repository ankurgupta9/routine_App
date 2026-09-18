package com.routinesys.app
import android.app.*; import android.content.*; import androidx.core.app.NotificationCompat
class RoutineAlarmReceiver:BroadcastReceiver(){
 private fun prettyTime(v:String):String { val p=v.split(":"); val h=p[0].toInt(); val m=p[1].toInt(); return String.format(java.util.Locale.getDefault(), "%d:%02d %s", if(h%12==0)12 else h%12,m,if(h<12)"AM" else "PM") }
 override fun onReceive(ctx:Context,intent:Intent){
  val id=intent.getStringExtra("id") ?: return; val b=RoutineStore.load(ctx).firstOrNull{it.id==id} ?: return
  AlarmScheduler.channel(ctx)
  val open=PendingIntent.getActivity(ctx, id.hashCode()+10000, Intent(ctx,MainActivity::class.java).apply{putExtra("routine_id",id);flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP}, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val n=NotificationCompat.Builder(ctx,"routine_starts").setSmallIcon(com.routinesys.app.R.drawable.ic_stat_routine).setContentTitle("Start ${b.label}").setContentText("Starting now — ${prettyTime(b.start)} to ${prettyTime(b.end)}").setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(open).build()
  ctx.getSystemService(NotificationManager::class.java).notify(id.hashCode(),n); AlarmScheduler.scheduleNext(ctx,b)
 }
}
