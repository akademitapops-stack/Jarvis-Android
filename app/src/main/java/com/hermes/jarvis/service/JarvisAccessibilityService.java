package com.hermes.jarvis.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityEvent;
import android.os.Bundle;
import com.hermes.jarvis.core.AppAccessStore;

public class JarvisAccessibilityService extends AccessibilityService {
    private static JarvisAccessibilityService instance;
    public static boolean isEnabled(){return instance!=null;}
    @Override protected void onServiceConnected(){super.onServiceConnected();instance=this;AccessibilityServiceInfo i=getServiceInfo();i.flags|=AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;setServiceInfo(i);}
    @Override public void onAccessibilityEvent(AccessibilityEvent e){}
    @Override public void onInterrupt(){}
    @Override public void onDestroy(){if(instance==this)instance=null;super.onDestroy();}
    public static String tapText(String text){if(instance==null)return "❌ Accessibility service belum aktif";if(!allowed())return "🔒 Aplikasi ini belum diizinkan JARVIS untuk dikendalikan";AccessibilityNodeInfo n=find(instance.getRootInActiveWindow(),text);if(n!=null){n.performAction(AccessibilityNodeInfo.ACTION_CLICK);return "✅ Tap: "+text;}return "❌ Teks tidak ditemukan: "+text;}
    public static String typeText(String text){if(instance==null)return "❌ Accessibility service belum aktif";if(!allowed())return "🔒 Aplikasi ini belum diizinkan JARVIS untuk dikendalikan";AccessibilityNodeInfo root=instance.getRootInActiveWindow();if(root==null)return "❌ UI root tidak tersedia";AccessibilityNodeInfo target=findEditable(root);if(target==null)return "❌ Kolom input tidak ditemukan";Bundle b=new Bundle();b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b);return "✅ Teks diisi";}
    public static String back(){return instance!=null&&instance.performGlobalAction(GLOBAL_ACTION_BACK)?"✅ Back":"❌ Accessibility service belum aktif";}
    public static String home(){return instance!=null&&instance.performGlobalAction(GLOBAL_ACTION_HOME)?"✅ Home":"❌ Accessibility service belum aktif";}
    public static String recents(){return instance!=null&&instance.performGlobalAction(GLOBAL_ACTION_RECENTS)?"✅ Recent apps":"❌ Accessibility service belum aktif";}
    public static String scroll(boolean forward){if(instance==null)return "❌ Accessibility service belum aktif";if(!allowed())return "🔒 Aplikasi ini belum diizinkan JARVIS untuk dikendalikan";AccessibilityNodeInfo root=instance.getRootInActiveWindow();if(root==null)return "❌ UI root tidak tersedia";AccessibilityNodeInfo n=findScrollable(root);if(n==null)return "❌ Scroll container tidak ditemukan";n.performAction(forward?AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);return "✅ Scroll";}

    private static boolean allowed(){
        AccessibilityNodeInfo root=instance==null?null:instance.getRootInActiveWindow();
        if(root==null) return false;
        CharSequence pkg=root.getPackageName();
        return new AppAccessStore(instance).isAllowed(pkg==null?"":pkg.toString());
    }
    private static AccessibilityNodeInfo find(AccessibilityNodeInfo n,String text){if(n==null)return null;if(text.equalsIgnoreCase(String.valueOf(n.getText()))||String.valueOf(n.getText()).toLowerCase().contains(text.toLowerCase()))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo r=find(n.getChild(i),text);if(r!=null)return r;}return null;}
    private static AccessibilityNodeInfo findEditable(AccessibilityNodeInfo n){if(n==null)return null;if(n.isEditable())return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo r=findEditable(n.getChild(i));if(r!=null)return r;}return null;}
    private static AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo n){if(n==null)return null;if(n.isScrollable())return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo r=findScrollable(n.getChild(i));if(r!=null)return r;}return null;}
}
