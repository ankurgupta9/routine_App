package com.routinesys.app

import android.Manifest
import android.app.*
import android.content.*
import android.graphics.Color
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity:Activity(){
 private lateinit var root:LinearLayout; private var blocks=mutableListOf<RoutineBlock>(); private val teal=Color.rgb(53,208,186); private val bg=Color.rgb(15,19,25); private val surface=Color.rgb(23,28,37); private val dim=Color.rgb(139,147,163); private val text=Color.rgb(230,233,239)
 override fun onCreate(b:Bundle?){super.onCreate(b);window.statusBarColor=bg;window.navigationBarColor=bg;blocks=RoutineStore.load(this); build(); requestNotifications(); AlarmScheduler.scheduleAll(this)}
 private fun tv(s:String,size:Float,color:Int=text):TextView=TextView(this).apply{text=s;textSize=size;setTextColor(color);setPadding(0,6,0,6)}
 private fun btn(s:String,primary:Boolean=false)=Button(this).apply{text=s;textSize=12f;setTextColor(if(primary)Color.rgb(11,21,18) else dim);setBackgroundColor(if(primary)teal else surface);setPadding(18,8,18,8);isAllCaps=false}
 private fun build(){
  root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(20,24,20,50)}
  val scroll=ScrollView(this).apply{addView(root)};setContentView(scroll); render()
 }
 private fun panel():LinearLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(18,14,18,14);setBackgroundColor(surface);val p=LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,14);layoutParams=p}
 private fun render(){root.removeAllViews(); val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}; val brand=tv("ROUTINE.sys",20f);brand.setTypeface(null,1);top.addView(brand,LinearLayout.LayoutParams(0,-2,1f));top.addView(tv(SimpleDateFormat("EEE, dd MMM",Locale.getDefault()).format(Date()).uppercase(),11f,dim));root.addView(top)
  val status=panel(); val now=Calendar.getInstance(); val today=blocks.filter{it.days.contains(now.get(Calendar.DAY_OF_WEEK)-1)}.sortedBy{mins(it.start)}; val nowM=now.get(Calendar.HOUR_OF_DAY)*60+now.get(Calendar.MINUTE); val current=today.firstOrNull{active(it,nowM)}; val next=today.firstOrNull{mins(it.start)>nowM}; status.addView(tv(if(current!=null)"STATUS: in progress — ${current.label}" else "STATUS: on track",13f,teal));status.addView(tv(if(next!=null)"NEXT: ${next.label} in ${duration(mins(next.start)-nowM)}" else "NEXT: —",12f,dim)); root.addView(status)
  val log=panel(); val head=LinearLayout(this);head.addView(tv("BLOCK LOG — TODAY",12f,dim),LinearLayout.LayoutParams(0,-2,1f)); val edit=btn("Edit routine");edit.setOnClickListener{editDialog()};head.addView(edit);log.addView(head)
  if(today.isEmpty())log.addView(tv("No blocks scheduled for today.",13f,dim)) else today.forEach{b-> val row=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,8,0,8)};row.addView(tv("${pretty(b.start)}–${pretty(b.end)}   ${b.label}",13f,text));row.addView(tv(status(b,nowM),10f,if(status(b,nowM)=="IN PROGRESS")teal else dim)); if(status(b,nowM) in listOf("IN PROGRESS","LOG NOW","FOLLOWED","MISSED")){val actions=LinearLayout(this);val y=btn("✓ Followed");val n=btn("✕ Missed");y.setOnClickListener{logBlock(b,true)};n.setOnClickListener{logBlock(b,false)};actions.addView(y);actions.addView(n);row.addView(actions)};log.addView(row)};root.addView(log)
  val rem=panel();rem.addView(tv("REMINDERS",12f,dim));rem.addView(tv("Android will notify you when each scheduled block starts, even when ROUTINE.sys is closed.",12f,dim));val rb=btn(if(hasExactAlarm())"Reminders active" else "Enable reminders",true);rb.setOnClickListener{if(Build.VERSION.SDK_INT>=31&&!hasExactAlarm()){startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))}else{requestNotifications();AlarmScheduler.scheduleAll(this);Toast.makeText(this,"Reminders scheduled",Toast.LENGTH_SHORT).show()}};rem.addView(rb);root.addView(rem)
  val data=panel();data.addView(tv("DATA",12f,dim));val imp=btn("Import JSON");val exp=btn("Export JSON");exp.setOnClickListener{share(RoutineStore.exportJson(this))};imp.setOnClickListener{Toast.makeText(this,"Import is kept simple in this first build; use Android file picker in the next revision.",Toast.LENGTH_LONG).show()};data.addView(exp);data.addView(imp);root.addView(data)
 }
 private fun hasExactAlarm()=Build.VERSION.SDK_INT<31 || getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
 private fun requestNotifications(){if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=0)ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.POST_NOTIFICATIONS),44)}
 private fun mins(s:String)=s.substringBefore(":").toInt()*60+s.substringAfter(":").toInt(); private fun pretty(s:String):String{val m=mins(s);return String.format(Locale.getDefault(),"%d:%02d %s",if(m/60%12==0)12 else m/60%12,m%60,if(m/60<12)"AM" else "PM")}
 private fun duration(x:Int):String{val m=((x%1440)+1440)%1440;return if(m<60)"${m}m" else "${m/60}h ${m%60}m"}
 private fun active(b:RoutineBlock,n:Int):Boolean{val s=mins(b.start);val e=mins(b.end);return if(e<=s)n>=s||n<e else n>=s&&n<e}
 private fun status(b:RoutineBlock,n:Int):String{val s=mins(b.start);val e=mins(b.end);return when{active(b,n)->"IN PROGRESS";n<s->"UPCOMING";else->"LOG NOW"}}
 private fun logBlock(b:RoutineBlock,follow:Boolean){val p=getSharedPreferences("logs",0);p.edit().putBoolean("${dateKey()}_${b.id}",follow).apply();Toast.makeText(this,if(follow)"Marked followed" else "Marked missed",Toast.LENGTH_SHORT).show();render()}
 private fun dateKey()=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())
 private fun share(raw:String){startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="application/json";putExtra(Intent.EXTRA_TEXT,raw)},"Export routine data"))}
 private fun editDialog(){val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,0,12,0)};val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};blocks.sortedBy{mins(it.start)}.forEach{b->val r=LinearLayout(this);r.addView(tv("${b.label}\n${pretty(b.start)}–${pretty(b.end)}",13f),LinearLayout.LayoutParams(0,-2,1f));val del=btn("Delete");del.setOnClickListener{AlarmScheduler.cancel(this,b);blocks.remove(b);RoutineStore.save(this,blocks);AlarmScheduler.scheduleAll(this);editDialog()};r.addView(del);list.addView(r)};box.addView(list);val add=btn("+ Add routine",true);add.setOnClickListener{addDialog()};box.addView(add);AlertDialog.Builder(this).setTitle("Edit routine").setView(box).setPositiveButton("Done"){_,_->render()}.show()}
 private fun addDialog(){val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(30,0,30,0)};val label=EditText(this).apply{hint="e.g. Dinner"};val start=EditText(this).apply{hint="Start HH:MM (24h)"};val end=EditText(this).apply{hint="End HH:MM (24h)"};l.addView(label);l.addView(start);l.addView(end);val days=TextView(this).apply{text="Days: 0=Sun, 1=Mon ... 6=Sat\nExample: 0,1,2,3,4,5,6";setTextColor(dim);setPadding(0,12,0,12)};l.addView(days);AlertDialog.Builder(this).setTitle("Add block").setView(l).setNegativeButton("Cancel",null).setPositiveButton("Add"){_,_->try{val s=start.text.toString();val e=end.text.toString();val d=(0..6).toMutableSet();blocks.add(RoutineBlock("b"+System.currentTimeMillis(),label.text.toString().trim(),"study",s,e,d));RoutineStore.save(this,blocks);AlarmScheduler.scheduleAll(this);render()}catch(_:Exception){Toast.makeText(this,"Use valid times like 20:00",Toast.LENGTH_SHORT).show()}}.show()}
}
