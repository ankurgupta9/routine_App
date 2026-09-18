package com.routinesys.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class RoutineBlock(val id:String, var label:String, var category:String, var start:String, var end:String, var days:MutableSet<Int>)

object RoutineStore {
    private const val PREFS="routine_sys"; private const val KEY="data"
    fun load(ctx:Context): MutableList<RoutineBlock> {
        val raw=ctx.getSharedPreferences(PREFS,0).getString(KEY,null) ?: return seed().also{save(ctx,it)}
        return try { val a=JSONArray(raw); MutableList(a.length()){i-> val o=a.getJSONObject(i); RoutineBlock(o.getString("id"),o.getString("label"),o.getString("category"),o.getString("start"),o.getString("end"), mutableSetOf<Int>().apply{val d=o.getJSONArray("days");for(j in 0 until d.length())add(d.getInt(j))})} } catch(_:Exception){seed()}
    }
    fun save(ctx:Context, blocks:List<RoutineBlock>) { val a=JSONArray(); blocks.forEach{b->a.put(JSONObject().apply{put("id",b.id);put("label",b.label);put("category",b.category);put("start",b.start);put("end",b.end);put("days",JSONArray(b.days.toList().sorted()))})}; ctx.getSharedPreferences(PREFS,0).edit().putString(KEY,a.toString()).apply() }
    private fun seed()= mutableListOf(
        RoutineBlock("b1","Sleep","sleep","23:30","07:00",(0..6).toMutableSet()),
        RoutineBlock("b2","Wake & morning routine","wake","07:00","07:30",(0..6).toMutableSet()),
        RoutineBlock("b3","Morning study block","study","07:30","09:00",(1..5).toMutableSet()),
        RoutineBlock("b4","Accenture training / work","work","09:30","18:00",(1..5).toMutableSet()),
        RoutineBlock("b5","Evening study block","study","19:30","21:30",(1..5).toMutableSet()),
        RoutineBlock("b6","Wind-down (no screens)","winddown","22:30","23:30",(0..6).toMutableSet())
    )
    fun exportJson(ctx:Context)=ctx.getSharedPreferences(PREFS,0).getString(KEY,"[]") ?: "[]"
    fun importJson(ctx:Context, raw:String){JSONArray(raw);ctx.getSharedPreferences(PREFS,0).edit().putString(KEY,raw).apply()}
}
