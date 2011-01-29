package com.qsp.player;

import java.io.File;

import android.os.Environment;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

public class Utility {
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter(); 
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    public static Spanned QspStrToHtml(String str, ImageGetter imgGetter)
    {
    	Spanned result = Html.fromHtml("");
    	if (str!=null && str.length() > 0)
    	{
    		str = str.replaceAll("\r", "<br>");
    		result = Html.fromHtml(str, imgGetter, null);
    	}
    	return result;
    }
    
    public static String QspStrToStr(String str)
    {
    	String result = "";
    	if (str!=null && str.length() > 0)
    	{
    		result = str.replaceAll("\r", "");
    	}
    	return result;
    }

    public static String GetDefaultPath()
    {
    	if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
    		throw new UnsupportedOperationException("SD card isn`t mounted");
    	String qspPath = "/qsp/games/";   	
    	File sdDir = new File(Environment.getExternalStorageDirectory().getPath());
    	if (sdDir.exists() && sdDir.canWrite()) {
			File fullDir = new File(sdDir.getAbsolutePath().concat(qspPath));
			if (!fullDir.exists()){
				if (!fullDir.mkdirs())
					throw new UnsupportedOperationException("Can`t create dirs on sd card");
			}
			return fullDir.getPath();
		}else throw new UnsupportedOperationException("SD card isn`t writeable");		
    }
    
    public static String GetDefaultPathCache(){
    	File cacheDir = new File(Utility.GetDefaultPath().concat("/cache/"));
    	if (!cacheDir.exists()) {
    		if (!cacheDir.mkdirs())
				throw new UnsupportedOperationException("Can`t create dirs on sd card");
    	}
    	return cacheDir.getPath();
    }

    public static void WriteLog(String msg)
    {
    	Log.i("QSP", msg);
    }
}