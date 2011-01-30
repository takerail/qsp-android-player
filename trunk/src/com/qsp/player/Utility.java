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

    public static String QspPathTranslate(String str)
    {
    	if (str==null)
    		return null;
    	//В QSP папки разделяются знаком \ , как в DOS и Windows, для Android переводим это в / . 
    	//Т.к. первый аргумент - регэксп, то эскейпим дважды.
    	String result = str.replaceAll("\\\\", "/");
    	return result;
    }

    public static String GetDefaultPath()
    {
    	//Возвращаем путь к папке с играми.
    	if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
    		return null;
    	File sdDir = Environment.getExternalStorageDirectory();
    	if (sdDir.exists() && sdDir.canWrite())
    	{
        	String flashCard = sdDir.getPath();
        	String tryFull1 = flashCard + "/qsp/games";
        	String tryFull2 = tryFull1 + "/";
        	File f = new File(tryFull1);
	    	if (f.exists())
	    	{
	    		return tryFull2;
	    	}
	    	else
	    	{
				if (f.mkdirs())
					return tryFull2;
	    	}
    	}
    	return null;
    }
    
    public static void WriteLog(String msg)
    {
    	Log.i("QSP", msg);
    }
}