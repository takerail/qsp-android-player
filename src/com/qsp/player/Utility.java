package com.qsp.player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    
    public static String ConvertGameTitleToCorrectFolderName(String title)
    {
		// Обрезаем многоточие
		String folder = title.endsWith("...") ? title.substring(0, title.length()-3) : title;
		// Меняем двоеточие на запятую
		folder = folder.replace(':', ',');
		return folder;
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
    
    static final String DEBUGKEY = 
        "308201e53082014ea00302010202044cab9b17300d06092a864886f70d01010505003037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f6964204465627567301e170d3130313030353231333933355a170d3131313030353231333933355a3037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f696420446562756730819f300d06092a864886f70d010101050003818d00308189028181009c705d592a4c65c4c96ffc4996de8e2e9371c40cebc63982d24e9f2d59979276a5cbcf6e937d538c895cd129b6b04c91861c514a25435d8ac57ff6bbe1bdfd5149e58f6dc1e97b6b77c8248fa02a5791f4f3fd9d4c2dd94fc1affff962d484c29d6394cb3578cfed523638a83c06b0d028ce4ba67b1a5e8017dfa218845ce04f0203010001300d06092a864886f70d010105050003818100011fd28bd2c4d853632eb0a47259bb36cea5522249c14dc0ff3a0fd94071a76df8fb3d8674d65362df8a7340c1f3b57d6680cd43b035154219643f5f7344e104de6c7588ad905aefaf92ec1811d52ee42f3e74c0068f447dece91df8d8f70cefbb53c22323538de9a9b101906005c4ac701c1c8af565fd78073bb9dcb769525b";

    public static boolean signedWithDebugKey(Context context, Class<?> cls) 
    {
    	boolean result = false;
    	try {
    		ComponentName comp = new ComponentName(context, cls);
    		PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(),PackageManager.GET_SIGNATURES);
    		Signature sigs[] = pinfo.signatures;
    		for ( int i = 0; i < sigs.length;i++)
    			WriteLog(sigs[i].toCharsString());
    		if (DEBUGKEY.equals(sigs[0].toCharsString())) {
    			result = true;
    			WriteLog("package has been signed with the debug key");
    		} else {
    			WriteLog("package signed with a key other than the debug key");
    		}
    	} catch (android.content.pm.PackageManager.NameNotFoundException e) {
    		return false;
    	}
    	return result;
    }
    
    //decodes image and scales it to reduce memory consumption 
    //(пока что не используется)
    public static Bitmap decodeImageFile(File f){ 
    	try { 
    		//Decode image size 
    		BitmapFactory.Options o = new BitmapFactory.Options(); 
    		o.inJustDecodeBounds = true; 
    		BitmapFactory.decodeStream(new FileInputStream(f),null,o);

    		//The new size we want to scale to
    		final int REQUIRED_SIZE=70;

    		//Find the correct scale value. It should be the power of 2.
    		int width_tmp=o.outWidth, height_tmp=o.outHeight;
    		int scale=1;
    		while(true){
    			if(width_tmp/2<REQUIRED_SIZE || height_tmp/2<REQUIRED_SIZE)
    				break;
    			width_tmp/=2;
    			height_tmp/=2;
    			scale*=2;
    		}

    		//Decode with inSampleSize
    		BitmapFactory.Options o2 = new BitmapFactory.Options();
    		o2.inSampleSize=scale;
    		return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
    	} catch (FileNotFoundException e) {}
    	return null;
    }
}