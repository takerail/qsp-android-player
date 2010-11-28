package com.qsp.player;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;





/*public class jniResult
{
	boolean success;
	
	int int1;
	int int2;
	int int3;

	String str1;
	String str2;
	String str3;
};
*/

public class QspPlayerStart extends TabActivity implements UrlClickCatcher{


	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TabHost tabHost = getTabHost();
        LayoutInflater.from(getApplicationContext()).inflate(R.layout.main, tabHost.getTabContentView(), true);
        tabHost.addTab(tabHost.newTabSpec("main")
                .setIndicator("Описание")
                .setContent(R.id.main_tab));
        tabHost.addTab(tabHost.newTabSpec("inv")
                .setIndicator("Инвентарь")
                .setContent(R.id.inv));
        tabHost.addTab(tabHost.newTabSpec("vars_desc")
                .setIndicator("Доп. Описание")
                .setContent(R.id.vars_desc));
        
        //Ищем все файлы .qsp и .gam в корне флэшки
        File sdcardRoot = new File ("/mnt/sdcard/");
        File[] sdcardFiles = sdcardRoot.listFiles();
        qspGames = new ArrayList<File>();
        for (File currentFile : sdcardFiles)
        {
        	if (currentFile.getName().endsWith(".qsp") || currentFile.getName().endsWith(".gam"))
        		qspGames.add(currentFile);
        }
        
        int total = qspGames.size();
        final CharSequence[] items = new String[total];
        for (int i=0; i<total; i++)
        {
        	items[i] = qspGames.get(i).getName();
        }
        
        //Показываем диалог выбора файла
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите файл с игрой");
        builder.setItems(items, browseFileClick);
        AlertDialog alert = builder.create();
        alert.show();
        
        //Создаем объект для обработки ссылок
        qspLinkMovementMethod = QspLinkMovementMethod.getQspInstance();
        qspLinkMovementMethod.setCatcher(this);
        
        //Создаем список для звуков и музыки
        mediaPlayersList = new Vector<MusicContent>();
        
        //Создаем объект для таймера
        timerHandler = new Handler();
    }
    
    private Runnable timerUpdateTask = new Runnable() {
    	   public void run() {
    		   QSPExecCounter(true);
    	       timerHandler.postDelayed(this, timerInterval);
    	   }
    	};
    
    android.content.DialogInterface.OnClickListener browseFileClick = new DialogInterface.OnClickListener()
    {
		@Override
		public void onClick(DialogInterface dialog, int which) 
		{
			runGame(qspGames.get(which).getPath());
		}    	
    };
    
    //LINKS HACKS
    static class InternalURLSpan extends ClickableSpan {
    	OnClickListener mListener;

    	public InternalURLSpan(OnClickListener listener) {
    		mListener = listener;
    	}

    	@Override
    	public void onClick(View widget) {
    		mListener.onClick(widget);
    	}
    }
    
    private void runGame(String fileName)
    {
        QSPInit();
        //String fileName = item. "/mnt/sdcard/The Punisher.gam";
        //String fileName = qspGames.get(which).getPath();
        File tqsp = new File (fileName); 
        
        curGameDir = fileName.substring(0, fileName.lastIndexOf(File.separator, fileName.length() - 1) + 1);
        imgGetter.SetDirectory(curGameDir);
        imgGetter.SetScreenWidth(getWindow().getWindowManager().getDefaultDisplay().getWidth());
        
        FileInputStream fIn = null;
        int size = 0;
		try {
			fIn = new FileInputStream(tqsp);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
        	e.printStackTrace();
		}
		try {
			size = fIn.available();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		byte[] inputBuffer = new byte[size];
		try {
		// Fill the Buffer with data from the file
		fIn.read(inputBuffer);
		fIn.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		TextView tv = (TextView) findViewById(R.id.main_desc); 
        
        if (QSPLoadGameWorldFromData(inputBuffer, size, fileName ))
        {
            //init acts callbacks
            ListView lvAct = (ListView)findViewById(R.id.acts);
            lvAct.setTextFilterEnabled(true);
            lvAct.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            lvAct.setFocusableInTouchMode(true);
            lvAct.setFocusable(true);
            lvAct.setItemsCanFocus(true);
            lvAct.setOnItemClickListener(actListClickListener);
            lvAct.setOnItemSelectedListener(actListSelectedListener);        

            //init objs callbacks
            ListView lvInv = (ListView)findViewById(R.id.inv);
            lvInv.setTextFilterEnabled(true);
            lvInv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            lvInv.setFocusableInTouchMode(true);
            lvInv.setFocusable(true);
            lvInv.setItemsCanFocus(true);
            lvInv.setOnItemClickListener(objListClickListener);
            lvInv.setOnItemSelectedListener(objListSelectedListener);        

            //Запускаем таймер
            timerInterval = 500;
            timerStartTime = System.currentTimeMillis();
            timerHandler.removeCallbacks(timerUpdateTask);
            timerHandler.postDelayed(timerUpdateTask, timerInterval);
            
            //Запускаем счетчик миллисекунд
            gameStartTime = System.currentTimeMillis();

            //Все готово, запускаем игру
            QSPRestartGame(true);
        }
        else
        {
        	String s = "Not able to parse file: "+Integer.toString(QSPGetLastErrorData());
        	tv.setText(s);
        }
    }
    
    
    //******************************************************************************
    //******************************************************************************
    //****** / QSP  LIBRARY  REQUIRED  CALLBACKS \ *********************************
    //******************************************************************************
    //******************************************************************************
    private void RefreshInt() 
    {
    	JniResult htmlResult = (JniResult) QSPGetVarValues("USEHTML", 0);
    	boolean html = htmlResult.success && (htmlResult.int1 == 1);
    	
    	
    	//основное описание
    	if (QSPIsMainDescChanged())
    	{
			TextView tvDesc = (TextView) findViewById(R.id.main_desc);
			String txtMainDesc = QSPGetMainDesc(); 
			if (html)
			{
				tvDesc.setText(Utility.QspStrToHtml(txtMainDesc, imgGetter));
				tvDesc.setMovementMethod(QspLinkMovementMethod.getInstance());
			}
			else
				tvDesc.setText(txtMainDesc);
    	}
    
    	//список действий
    	if (QSPIsActionsChanged())
    	{
	        ListView lvAct = (ListView)findViewById(R.id.acts);
	        int nActsCount = QSPGetActionsCount();
			if (html)
			{
		        Spanned []acts = new Spanned[nActsCount];
		        for (int i=0;i<nActsCount;i++)
		        {
		        	JniResult actsResult = (JniResult) QSPGetActionData(i);
		        	acts[i] = Utility.QspStrToHtml(actsResult.str1, imgGetter);
		        }
		        lvAct.setAdapter(new ArrayAdapter<Spanned>(this, R.layout.act_item, acts));
			}
			else
			{
		        String []acts = new String[nActsCount];
		        for (int i=0;i<nActsCount;i++)
		        {
		        	JniResult actsResult = (JniResult) QSPGetActionData(i);
		        	acts[i] = actsResult.str1;
		        }
		        lvAct.setAdapter(new ArrayAdapter<String>(this, R.layout.act_item, acts));
			}
	        
	        //Разворачиваем список действий
	        Utility.setListViewHeightBasedOnChildren(lvAct);
    	}
        
        //инвентарь
    	if (QSPIsObjectsChanged())
    	{
    		//Toast.makeText(this, "инвентарь", Toast.LENGTH_SHORT).show();
	        ListView lvInv = (ListView)findViewById(R.id.inv);
	        int nObjsCount = QSPGetObjectsCount();
			if (html)
			{
		        Spanned []objs = new Spanned[nObjsCount];
		        for (int i=0;i<nObjsCount;i++)
		        {
		        	JniResult objsResult = (JniResult) QSPGetObjectData(i);
		        	objs[i] = Utility.QspStrToHtml(objsResult.str1, imgGetter);
		        }
		        lvInv.setAdapter(new ArrayAdapter<Spanned>(this, R.layout.obj_item, objs));
			}
			else
			{
		        String []objs = new String[nObjsCount];
		        for (int i=0;i<nObjsCount;i++)
		        {
		        	JniResult objsResult = (JniResult) QSPGetObjectData(i);
		        	objs[i] = objsResult.str1;
		        }
		        lvInv.setAdapter(new ArrayAdapter<String>(this, R.layout.obj_item, objs));
			}
    	}
        
        //доп. описание
    	if (QSPIsVarsDescChanged())
    	{
    		//Toast.makeText(this, "доп. описание", Toast.LENGTH_SHORT).show();
			TextView tvVarsDesc = (TextView) findViewById(R.id.vars_desc);
			String txtVarsDesc = QSPGetVarsDesc();
			if (html)
			{
				tvVarsDesc.setText(Utility.QspStrToHtml(txtVarsDesc, imgGetter));
				tvVarsDesc.setMovementMethod(QspLinkMovementMethod.getInstance());
			}
			else
				tvVarsDesc.setText(txtVarsDesc);
    	}
    }
    
    private void SetTimer(int msecs)
    {
    	timerInterval = msecs;
    }

    private void ShowMessage(String message)
    {
    	//Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    	new AlertDialog.Builder(this)
        .setMessage(message)
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) { }
        })
        .show();
    }
    
    private void PlayFile(String file, int volume)
    {
    	if (file == null || file.length() == 0)
    		return;
    	
    	//Проверяем, проигрывается ли уже этот файл.
    	//Если проигрывается, ничего не делаем.
    	if (IsPlayingFile(file))
    		return;
    	
    	MediaPlayer mediaPlayer = new MediaPlayer();
	    try {
			mediaPlayer.setDataSource(curGameDir + file);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	    try {
			mediaPlayer.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	    mediaPlayer.start();
	    MusicContent musicContent = new MusicContent();
	    musicContent.path = file;
	    musicContent.player = mediaPlayer;
	    mediaPlayersList.add(musicContent);
    }
    
    private boolean IsPlayingFile(String file)
    {
    	if (file == null || file.length() == 0)
    		return false;
    	for (int i=0; i<mediaPlayersList.size(); i++)
    	{
    		MusicContent it = mediaPlayersList.elementAt(i);
    		if (it.path.compareTo(file)==0 && it.player.isPlaying())
    			return true;
    	}
    	return false;
    }

    private void CloseFile(String file)
    {
    	//Если вместо имени файла пришел null, значит закрываем все файлы(CLOSE ALL)
    	boolean bCloseAll = false;
    	if (file == null)
    		bCloseAll = true;
    	else if (file.length() == 0)
    		return;
    	for (int i=0; i<mediaPlayersList.size(); i++)
    	{
    		MusicContent it = mediaPlayersList.elementAt(i);    		
    		if (bCloseAll || it.path.compareTo(file)==0)
    		{
    			it.player.stop();
    			it.player.release();
    			mediaPlayersList.remove(it);
    			break;
    		}
    	}
    }
    
    private void ShowPicture(String file)
    {
    	if (file == null || file.length() == 0)
    		return;
    	String prefix = "";
    	if (curGameDir != null)
    		prefix = curGameDir;
    	Intent imageboxIntent = new Intent();
    	imageboxIntent.setClassName("com.qsp.player", "com.qsp.player.QspImageBox");
    	Bundle b = new Bundle();
    	b.putString("imageboxFile", prefix.concat(file));
    	imageboxIntent.putExtras(b);
    	startActivity(imageboxIntent);    	
    }
    
    private String InputBox(String prompt)
    {
    	//!!! STUB
    	return "stub";
    }
    
    private int GetMSCount()
    {
    	return (int) (System.currentTimeMillis() - gameStartTime);
    }
    //******************************************************************************
    //******************************************************************************
    //****** \ QSP  LIBRARY  REQUIRED  CALLBACKS / *********************************
    //******************************************************************************
    //******************************************************************************
    
    
    public void OnUrlClicked (String href)
    {
    	String tag = href.substring(0, 5).toLowerCase();
    	if (tag.compareTo("exec:") == 0)
    	{
    		String code = href.substring(5);
	    	boolean bExec = QSPExecString(code, true);
	    	if (!bExec)
	    	{
	    		int nError = QSPGetLastErrorData();
	    		String txtError = "Error: "+String.valueOf(nError);  
	    		new AlertDialog.Builder(this)
	            .setMessage(txtError)
	            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) { }
	            })
	            .show();
	    	}
    	}
    }
    
    //Callback for click on selected act
    private OnItemClickListener actListClickListener = new OnItemClickListener() 
    {
    	@Override
    	public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3) 
    	{
    		QSPSetSelActionIndex(position, false);
    		QSPExecuteSelActionCode(true);
    	}
    };
    
    //Callback for select act
    private OnItemSelectedListener actListSelectedListener = new OnItemSelectedListener() 
    {
		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			QSPSetSelActionIndex(arg2, true);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) 
		{
		}
    };
    
    //Callback for click on selected object
    private OnItemClickListener objListClickListener = new OnItemClickListener() 
    {
    	@Override
    	public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3) 
    	{
    		QSPSetSelObjectIndex(position, true);
    	}
    };
    
    //Callback for select object
    private OnItemSelectedListener objListSelectedListener = new OnItemSelectedListener() 
    {
		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			QSPSetSelObjectIndex(arg2, true);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) 
		{
		}
    };
    
    //Для отображения картинок в HTML
    static QspImageGetter imgGetter = new QspImageGetter();
    
    ArrayList<File> 		qspGames;
    String 					curGameDir;
    Vector<MusicContent>	mediaPlayersList;
    Handler					timerHandler;
	long					timerStartTime;
	long					gameStartTime;
	int						timerInterval;

    
    QspLinkMovementMethod 	qspLinkMovementMethod; 
    
    
    //control
    public native void 		QSPInit();
    public native void 		QSPDeInit();
    public native boolean 	QSPIsInCallBack();
    public native void 		QSPEnableDebugMode(boolean isDebug);
    public native Object 	QSPGetCurStateData();//!!!STUB
    public native String 	QSPGetVersion();
    public native int 		QSPGetFullRefreshCount();
    public native String 	QSPGetQstFullPath();
    public native String 	QSPGetCurLoc();
    public native String 	QSPGetMainDesc();
    public native boolean 	QSPIsMainDescChanged();
    public native String 	QSPGetVarsDesc();
    public native boolean 	QSPIsVarsDescChanged();
    public native Object 	QSPGetExprValue();//!!!STUB
    public native void 		QSPSetInputStrText(String val);
    public native int 		QSPGetActionsCount();
    public native Object 	QSPGetActionData(int ind);//!!!STUB
    public native boolean 	QSPExecuteSelActionCode(boolean isRefresh);
    public native boolean 	QSPSetSelActionIndex(int ind, boolean isRefresh);
    public native int 		QSPGetSelActionIndex();
    public native boolean 	QSPIsActionsChanged();
    public native int 		QSPGetObjectsCount();
    public native Object 	QSPGetObjectData(int ind);//!!!STUB
    public native boolean 	QSPSetSelObjectIndex(int ind, boolean isRefresh);
    public native int 		QSPGetSelObjectIndex();
    public native boolean 	QSPIsObjectsChanged();
    public native void 		QSPShowWindow(int type, boolean isShow);
    public native Object 	QSPGetVarValuesCount(String name);
    public native Object 	QSPGetVarValues(String name, int ind);//!!!STUB
    public native int 		QSPGetMaxVarsCount();
    public native Object 	QSPGetVarNameByIndex(int index);//!!!STUB
    public native boolean 	QSPExecString(String s, boolean isRefresh);
    public native boolean 	QSPExecLocationCode(String name, boolean isRefresh);
    public native boolean 	QSPExecCounter(boolean isRefresh);
    public native boolean 	QSPExecUserInput(boolean isRefresh);
    public native int 		QSPGetLastErrorData();//!!!STUB
    public native String 	QSPGetErrorDesc(int errorNum);
    public native boolean 	QSPLoadGameWorld(String fileName);
    public native boolean 	QSPLoadGameWorldFromData(byte data[], int dataSize, String fileName );
    public native boolean 	QSPSaveGame(String fileName, boolean isRefresh);
    public native Object 	QSPSaveGameAsString(boolean isRefresh);//!!!STUB
    public native boolean 	QSPOpenSavedGame(String fileName, boolean isRefresh);
    public native Object 	QSPOpenSavedGameFromString(String str, boolean isRefresh);//!!!STUB
    public native boolean 	QSPRestartGame(boolean isRefresh);
    //public native void QSPSetCallBack(int type, QSP_CALLBACK func) 

    static {
	    System.loadLibrary("ndkqsp");
	}
}