package com.qsp.player;

import java.io.File;
import java.io.IOException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class Utility {
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter(); 
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        int desiredWidth = MeasureSpec.makeMeasureSpec(listView.getWidth(), MeasureSpec.AT_MOST);
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    public static Spanned QspStrToHtml(String str, ImageGetter imgGetter)
    {
    	Spanned result = Html.fromHtml("");
    	if (str!=null && str.length() > 0)
    	{
    		str = str.replaceAll("\r", "<br>");
    		str = str.replaceAll("(?i)</td>", " ");
    		str = str.replaceAll("(?i)</tr>", "<br>");
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

    private static void CheckNoMedia(String path)
    {
    	//Создаем в папке QSP пустой файл .nomedia
    	File f = new File(path);
    	if (f.exists())
    		return;
    	try {
			f.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
        	String noMedia = flashCard + "/qsp/.nomedia";
        	File f = new File(tryFull1);
	    	if (f.exists())
	    	{
	    		CheckNoMedia(noMedia);
	    		return tryFull2;
	    	}
	    	else
	    	{
				if (f.mkdirs())
				{
					CheckNoMedia(noMedia);
					return tryFull2;
				}
	    	}
    	}
    	return null;
    }
    
    public static void WriteLog(String msg)
    {
    	Log.i("QSP", msg);
    }
    
    public static void ShowError(Context context, String message)
    {
		new AlertDialog.Builder(context)
    	.setTitle("Ошибка")
		.setMessage(message)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) { }
		})
		.show();
    }
    
    public static void ShowInfo(Context context, String message)
    {
    	Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    public static void DeleteRecursive(File f)
    {
    	if ((f == null) || !f.exists())
    		return;
    	if (f.isDirectory())
    	{
            File[] files = f.listFiles();
            for (File currentFile : files)
            	DeleteRecursive(currentFile);
    	}
    	f.delete();
    }
}