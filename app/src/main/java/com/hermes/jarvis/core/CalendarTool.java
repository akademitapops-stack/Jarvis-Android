package com.hermes.jarvis.core;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.CalendarContract;
import androidx.core.app.ActivityCompat;
import java.util.TimeZone;

public class CalendarTool {
    public static String addEvent(Context c,String title,long startMs,long endMs,String note){
        if(ActivityCompat.checkSelfPermission(c, Manifest.permission.WRITE_CALENDAR)!=PackageManager.PERMISSION_GRANTED)return "PERMISSION_REQUIRED";
        try{
            ContentValues v=new ContentValues();v.put(CalendarContract.Events.TITLE,title);v.put(CalendarContract.Events.DTSTART,startMs);v.put(CalendarContract.Events.DTEND,endMs);v.put(CalendarContract.Events.EVENT_TIMEZONE,TimeZone.getDefault().getID());v.put(CalendarContract.Events.DESCRIPTION,note);
            long calId=findCalendar(c);if(calId<0)return "❌ Kalender tidak ditemukan";v.put(CalendarContract.Events.CALENDAR_ID,calId);c.getContentResolver().insert(CalendarContract.Events.CONTENT_URI,v);return "✅ Event kalender dibuat: "+title;
        }catch(Exception e){return "❌ Kalender: "+e.getMessage();}
    }
    private static long findCalendar(Context c){try(android.database.Cursor cur=c.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,new String[]{CalendarContract.Calendars._ID},CalendarContract.Calendars.VISIBLE+"=1",null,CalendarContract.Calendars._ID+" ASC")){if(cur!=null&&cur.moveToFirst())return cur.getLong(0);}catch(Exception ignored){}return -1;}
}
