package com.qsp.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.gesture.GestureStroke;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;





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

public class QspPlayerStart extends Activity implements UrlClickCatcher, OnGesturePerformedListener{

    public static final int SWIPE_MIN = 120;
    public static final int WIN_INV = 0;
    public static final int WIN_MAIN = 1;
    public static final int WIN_EXT = 2;
	Resources res;
	boolean invUnread, varUnread;
	int currentWin;
	
	final private Context uiContext = this;
	final private ReentrantLock musicLock = new ReentrantLock();
	
	private boolean gui_debug_mode = true; 


	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		if(gesture.getLength()>SWIPE_MIN) {
			ArrayList<GestureStroke> strokes = gesture.getStrokes();
			float[] points = strokes.get(0).points; 
			if(points[0]<points[points.length-1]){
                //swipe left
            	if(currentWin>0)
            		currentWin--;
            	else
            		currentWin = 2;
			}else{
            	if(currentWin<2) 
            		currentWin++;
            	else
            		currentWin = 0;
			}
			setCurrentWin(currentWin); 				
		}
	}	
	
	public QspPlayerStart() {
    	//Контекст UI
	}
	

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {    	
    	WriteLog("onCreate\\");
    	//Контекст UI
        super.onCreate(savedInstanceState);
        //будем использовать свой вид заголовка
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        res = getResources();

		gameIsRunning = false;
		qspInited = false;
		waitForImageBox = false;

        //подключаем жесты
        GestureOverlayView gestures = (GestureOverlayView) findViewById(R.id.gestures);
        gestures.addOnGesturePerformedListener(this);
        
   		//текущий вид - основное описание
    	setCurrentWin(currentWin=WIN_MAIN);
        
        //Создаем объект для обработки ссылок
        qspLinkMovementMethod = QspLinkMovementMethod.getQspInstance();
        qspLinkMovementMethod.setCatcher(this);
        
        //Создаем список для звуков и музыки
        mediaPlayersList = new Vector<MusicContent>();
        
        //Создаем список для всплывающего меню
        menuList = new Vector<QspMenuItem>();
        
        //Создаем диалог ввода текста
        LayoutInflater factory = LayoutInflater.from(uiContext);
        View textEntryView = factory.inflate(R.layout.inputbox, null);
        inputboxDialog = new AlertDialog.Builder(uiContext)
        .setView(textEntryView)
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	EditText edit = (EditText)inputboxDialog.findViewById(R.id.inputbox_edit);
            	inputboxResult = edit.getText().toString();
				dialogHasResult = true;
				WriteLog("InputBox(UI): OK clicked, unparking library thread");
            	setThreadUnpark();
            }
        })
        .setCancelable(false)
        .create();
        
        //Создаем объект для таймера
        timerHandler = new Handler();
        
        //Выбираем игру
        BrowseGame(GetDefaultPath(), true);
        
        //Запускаем поток библиотеки
        StartLibThread();
    	WriteLog("onCreate/");
    }
    
    @Override
    public void onResume()
    {
    	WriteLog("onResume\\");    	
    	//Контекст UI
    	super.onResume();
    	
    	waitForImageBox = false;
    	WriteLog("onResume/");    	
    }
    
    @Override
    public void onPause() {
    	WriteLog("onPause\\");    	
    	//Контекст UI
    	super.onPause();
    	
    	if (waitForImageBox)
    	{
        	WriteLog("onPause/ (waitForImageBox)");    	
    		return;
    	}

    	//Очищаем ВСЕ на выходе
    	if (qspInited)
    	{
        	WriteLog("onPause: stopping game");    	
    		StopGame();
    	}
    	//Останавливаем поток библиотеки
   		StopLibThread();
    	WriteLog("onPause/");  
    	finish();
    }
    
    private void WriteLog(String msg)
    {
    	Log.i("QSP", msg);
    }
    
    //******************************************************************************
    //******************************************************************************
    //****** / THREADS \ ***********************************************************
    //******************************************************************************
    //******************************************************************************
    /** паркует-останавливает указанный тред, и сохраняет на него указатель в parkThread */
    protected void setThreadPark()    {
    	WriteLog("setThreadPark: enter ");    	
    	//Контекст библиотеки
    	if (libThread == null)
    	{
    		WriteLog("setThreadPark: failed, libthread is null");
    		return;
    	}
        parkThread = libThread;
        LockSupport.park();
    	WriteLog("setThreadPark: success ");    	
    }
    
    /** возобновляет работу треда сохраненного в указателе parkThread */
    protected boolean setThreadUnpark()    {
    	WriteLog("setThreadUnPark: enter ");    	
    	//Контекст UI
        if (parkThread!=null && parkThread.isAlive()) {
            LockSupport.unpark(parkThread);
        	WriteLog("setThreadUnPark: success ");    	
            return true;
        }
    	WriteLog("setThreadUnPark: failed, ");
    	if (parkThread==null)
        	WriteLog("parkThread is null ");
    	else
        	WriteLog("parkThread is dead ");
        return false;
    }
    
    protected void StartLibThread()
    {
    	WriteLog("StartLibThread: enter ");    	
    	//Контекст UI
    	if (libThread!=null)
    	{
        	WriteLog("StartLibThread: failed, libThread is null");    	
    		return;
    	}
    	//Запускаем поток библиотеки
    	Thread t = new Thread() {
            public void run() {
    			Looper.prepare();
    			libThreadHandler = new Handler();
            	WriteLog("LibThread runnable: libThreadHandler is set");    	
        		Looper.loop();
            	WriteLog("LibThread runnable: library thread exited");    	
            }
        };
        libThread = t;
        t.start();
    	WriteLog("StartLibThread: success ");    	
    }
    
    protected void StopLibThread()
    {
    	WriteLog("StopLibThread: enter ");    	
    	//Контекст UI
    	//Останавливаем поток библиотеки
       	libThreadHandler.getLooper().quit();
		libThread = null;		
    	WriteLog("StopLibThread: success ");    	
    }
    //******************************************************************************
    //******************************************************************************
    //****** \ THREADS / ***********************************************************
    //******************************************************************************
    //******************************************************************************

    //устанавливаем текст заголовка окна
    private void setTitle(String second) {
   		TextView winTitle = (TextView) findViewById(R.id.title_text);
   		winTitle.setText(second);
		updateTitle();
    }	
    
    //анимация иконок при смене содержимого скрытых окон
    private void updateTitle() {
		ImageButton image = (ImageButton) findViewById(R.id.title_button_1);
		image.clearAnimation();
		if(invUnread){
    		Animation update = AnimationUtils.loadAnimation(this, R.anim.update);
    		image.startAnimation(update);
    		image.setBackgroundResource(R.drawable.btn_bg_selected);
    		invUnread = false;
    	}
		image = (ImageButton) findViewById(R.id.title_button_2);
		image.clearAnimation();
    	if(varUnread){
    		Animation update = AnimationUtils.loadAnimation(this, R.anim.update);
    		image.startAnimation(update);
    		image.setBackgroundResource(R.drawable.btn_bg_selected);
    		varUnread = false;
    	}	
    }	
    
    //обработчик "Описание" в заголовке
    public void onHomeClick(View v) {
    	setCurrentWin(WIN_MAIN);
    }
    
    //обработчик "Инвентарь" в заголовке
    public void onInvClick(View v) {
    	setCurrentWin(WIN_INV);
    }
    
    //обработчик "Доп" в заголовке
    public void onExtClick(View v) {
    	setCurrentWin(WIN_EXT);
    }
    
    //смена активного экрана
    private void setCurrentWin(int win) {
    	switch(win){
    	case WIN_INV: 
       		toggleInv(true);
       		toggleMain(false);
       		toggleExt(false);
       		invUnread = false;
       		setTitle("Инвентарь");
       		break;
    	case WIN_MAIN: 
       		toggleInv(false);
       		toggleMain(true);
       		toggleExt(false);
       		setTitle("Описание");
       		break;
    	case WIN_EXT: 
       		toggleInv(false);
       		toggleMain(false);
       		toggleExt(true);
       		varUnread = false;
       		setTitle("Доп. описание");
       		break;
    	}
    }
    
    private void toggleInv(boolean vis) {
   		findViewById(R.id.inv).setVisibility(vis ? View.VISIBLE : View.GONE);
   		findViewById(R.id.title_sep_1).setVisibility(vis ? View.GONE : View.VISIBLE);
   		findViewById(R.id.title_button_1).setVisibility(vis ? View.GONE : View.VISIBLE);
   		if(vis)
   			findViewById(R.id.title_button_1).setBackgroundDrawable(null);
    }
    
    private void toggleMain(boolean vis) {
   		findViewById(R.id.main_desc).setVisibility(vis ? View.VISIBLE : View.GONE);
   		findViewById(R.id.acts).setVisibility(vis ? View.VISIBLE : View.GONE);
   		findViewById(R.id.title_home_button).setVisibility(vis ? View.GONE : View.VISIBLE);
   		findViewById(R.id.title_home_button_sep).setVisibility(vis ? View.GONE : View.VISIBLE);    	
    }
    
    private void toggleExt(boolean vis) {
   		findViewById(R.id.vars_desc).setVisibility(vis ? View.VISIBLE : View.GONE);
   		findViewById(R.id.title_sep_2).setVisibility(vis ? View.GONE : View.VISIBLE);
   		findViewById(R.id.title_button_2).setVisibility(vis ? View.GONE : View.VISIBLE);    	
   		if(vis)
   			findViewById(R.id.title_button_2).setBackgroundDrawable(null);
   }
    
    private Runnable timerUpdateTask = new Runnable() {
    	//Контекст UI
		public void run() {
			libThreadHandler.post(new Runnable() {
				public void run() {
					if (libraryThreadIsRunning)
						return;
			    	libraryThreadIsRunning = true;
				   	QSPExecCounter(true);
					libraryThreadIsRunning = false;
				}
			});
			timerHandler.postDelayed(this, timerInterval);
		}
	};
    
    android.content.DialogInterface.OnClickListener browseFileClick = new DialogInterface.OnClickListener()
    {
    	//Контекст UI
		@Override
		public void onClick(DialogInterface dialog, int which) 
		{
			boolean canGoUp = backPath.compareTo("") != 0;
			int shift = 0;
			if (canGoUp)
				shift = 1;
			if (which == 0 && canGoUp)
			{
				dialog.dismiss();
				BrowseGame(backPath, false);
			}
			else
			{
				File f = qspGames.get(which - shift);
				if (f.isDirectory())
					BrowseGame(f.getPath(), false);
				else
					runGame(f.getPath());
			}
		}    	
    };
    
    //LINKS HACKS
    static class InternalURLSpan extends ClickableSpan {
    	//Контекст UI
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
    	//Контекст UI
    	if (libThreadHandler==null)
    	{
    		WriteLog("runGame: failed, libThreadHandler is null");
    		return;
    	}

		if (libraryThreadIsRunning)
		{
    		WriteLog("runGame: failed, library thread is already running");
			return;
		}
    	
    	qspInited = true;
    	final String gameFileName = fileName;
        curGameDir = gameFileName.substring(0, gameFileName.lastIndexOf(File.separator, gameFileName.length() - 1) + 1);
        imgGetter.SetDirectory(curGameDir);
        imgGetter.SetScreenWidth(getWindow().getWindowManager().getDefaultDisplay().getWidth());

        libThreadHandler.post(new Runnable() {
    		public void run() {
    	        QSPInit();
    	        File tqsp = new File (gameFileName);
    	        FileInputStream fIn = null;
    	        int size = 0;
    			try {
    				fIn = new FileInputStream(tqsp);
    			} catch (FileNotFoundException e) {
    	        	e.printStackTrace();
    			}
    			try {
    				size = fIn.available();
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
    	        
    			byte[] inputBuffer = new byte[size];
    			try {
    			// Fill the Buffer with data from the file
    			fIn.read(inputBuffer);
    			fIn.close();
    			} catch (IOException e) {
    				e.printStackTrace();
    			}

    			final boolean gameLoaded = QSPLoadGameWorldFromData(inputBuffer, size, gameFileName );
    			
    			runOnUiThread(new Runnable() {
    				public void run() {
    	    			TextView tv = (TextView) findViewById(R.id.main_desc); 
    	    	        
    	    	        if (gameLoaded)
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
    	    	            libThreadHandler.post(new Runnable() {
    	    	        		public void run() {
    	    	                	libraryThreadIsRunning = true;
    	    	        			QSPRestartGame(true);
    	    	                	libraryThreadIsRunning = false;
    	    	        		}
    	    	            } );
    	    	            
    	    	            gameIsRunning = true;
    	    	        }
    	    	        else
    	    	        {
    	    	        	String s = "Not able to parse file: "+Integer.toString(QSPGetLastErrorData());
    	    	        	tv.setText(s);
    	    	        }
    				}
    			});
    		}
    	});
    }
    
    private void StopGame()
    {
    	//Контекст UI
		if (gameIsRunning)
		{    			
            //останавливаем таймер
            timerHandler.removeCallbacks(timerUpdateTask);

            //останавливаем музыку
            CloseFileUI(null);
            
            //отключаем колбэки действий
            ListView lvAct = (ListView)findViewById(R.id.acts);
            lvAct.setOnItemClickListener(null);
            lvAct.setOnItemSelectedListener(null);        

            //отключаем колбэки инвентаря
            ListView lvInv = (ListView)findViewById(R.id.inv);
            lvInv.setOnItemClickListener(null);
            lvInv.setOnItemSelectedListener(null);
            
            gameIsRunning = false;
		}
		curGameDir = "";
		qspInited = false;

		//Очищаем библиотеку
		if (libraryThreadIsRunning)
			return;
        libThreadHandler.post(new Runnable() {
    		public void run() {
            	libraryThreadIsRunning = true;
        		QSPDeInit();
            	libraryThreadIsRunning = false;
    		}
        } );
    }
    
    private void BrowseGame(String startpath, boolean start)
    {
    	//Контекст UI
    	if (startpath == null)
    		return;
    	
    	//Устанавливаем путь "выше"    	
    	if (!start)
    		if (startRootPath.compareTo(startpath) == 0)
    			start = true;
    	if (!start)
    	{
    		int slash = startpath.lastIndexOf(File.separator, startpath.length() - 2);
    		if (slash >= 0)
    			backPath = startpath.substring(0, slash + 1);
    		else
    			start = true;
    	}
    	if (start)
    	{
    		startRootPath = startpath;
    		backPath = "";
    	}
    	
        //Ищем все файлы .qsp и .gam в корне флэшки
        File sdcardRoot = new File (startpath);
        File[] sdcardFiles = sdcardRoot.listFiles();        
        qspGames = new ArrayList<File>();
        //Сначала добавляем все папки
        for (File currentFile : sdcardFiles)
        {
        	if (currentFile.isDirectory() && !currentFile.isHidden() && !currentFile.getName().startsWith("."))
        		qspGames.add(currentFile);
        }
        //Потом добавляем все QSP-игры
        for (File currentFile : sdcardFiles)
        {
        	if (!currentFile.isHidden() && (currentFile.getName().endsWith(".qsp") || currentFile.getName().endsWith(".gam")))
        		qspGames.add(currentFile);
        }
        
        //Если мы не на самом верхнем уровне, то добавляем ссылку 
        int shift = 0;
        if (!start)
        	shift = 1;
        int total = qspGames.size() + shift;
        final CharSequence[] items = new String[total];
        if (!start)
            items[0] = "[..]";
        for (int i=shift; i<total; i++)
        {
        	File f = qspGames.get(i - shift);
        	String displayName = f.getName();
        	if (f.isDirectory())
        		displayName = "["+ displayName + "]";
        	items[i] = displayName;
        }
        
        //Показываем диалог выбора файла
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите файл с игрой");
        builder.setItems(items, browseFileClick);
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    private String GetDefaultPath()
    {
    	//Контекст UI

    	//Возвращаем путь к папке с играми.
    	String flashCard = "/mnt/sdcard/";    	
    	String tryFull = flashCard + "/qsp/games/";
    	File f = new File(tryFull);
    	if (f.exists())
    		return tryFull;    	
    	return flashCard;
    }

    private void PlayFileUI(String file, int volume)
    {
    	//Контекст UI
    	if (file == null || file.length() == 0)
    		return;
    	
    	//Проверяем, проигрывается ли уже этот файл.
    	//Если проигрывается, ничего не делаем.
    	if (IsPlayingFileUI(file))
    		return;

		//Проверяем, существует ли файл.
		//Если нет, ничего не делаем.
		File mediaFile = new File(curGameDir, file);
        if (!mediaFile.exists())
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
		final String fileName = file;
		mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
		        musicLock.lock();
		        try {
			    	for (int i=0; i<mediaPlayersList.size(); i++)
			    	{
			    		MusicContent it = mediaPlayersList.elementAt(i);    		
			    		if (it.path.compareTo(fileName)==0)
			    		{
			    			mediaPlayersList.remove(it);
			    			break;
			    		}
			    	}
		        } finally {
		        	musicLock.unlock();
		        }
			}
		});
	    mediaPlayer.start();
	    MusicContent musicContent = new MusicContent();
	    musicContent.path = file;
	    musicContent.player = mediaPlayer;
        musicLock.lock();
        try {
        	mediaPlayersList.add(musicContent);
        } finally {
        	musicLock.unlock();
        }
    }

    private boolean IsPlayingFileUI(String file)
    {
    	//Контекст UI
    	if (file == null || file.length() == 0)
    		return false;
    	boolean foundPlaying = false; 
        musicLock.lock();
        try {
	    	for (int i=0; i<mediaPlayersList.size(); i++)
	    	{
	    		MusicContent it = mediaPlayersList.elementAt(i);
	    		if (it.path.compareTo(file)==0)
	    		{
	    			foundPlaying = true;
	    			break;
	    		}
	    	}
        } finally {
        	musicLock.unlock();
        }
    	return foundPlaying;
    }
    
    private void CloseFileUI(String file)
    {
    	//Контекст UI
    	//Если вместо имени файла пришел null, значит закрываем все файлы(CLOSE ALL)
    	boolean bCloseAll = false;
    	if (file == null)
    		bCloseAll = true;
    	else if (file.length() == 0)
    		return;
        musicLock.lock();
        try {
	    	for (int i=0; i<mediaPlayersList.size(); i++)
	    	{
	    		MusicContent it = mediaPlayersList.elementAt(i);    		
	    		if (bCloseAll || it.path.compareTo(file)==0)
	    		{
	    			if (it.player.isPlaying())
	    				it.player.stop();
	    			it.player.release();
	    			mediaPlayersList.remove(it);
	    			break;
	    		}
	    	}
        } finally {
        	musicLock.unlock();
        }
    }
    
    //******************************************************************************
    //******************************************************************************
    //****** / QSP  LIBRARY  REQUIRED  CALLBACKS \ *********************************
    //******************************************************************************
    //******************************************************************************
    private void RefreshInt() 
    {
    	//Контекст библиотеки
    	JniResult htmlResult = (JniResult) QSPGetVarValues("USEHTML", 0);
    	final boolean html = htmlResult.success && (htmlResult.int1 == 1);
    	
    	
    	//основное описание
    	if (QSPIsMainDescChanged())
    	{
			final String txtMainDesc = QSPGetMainDesc();
			runOnUiThread(new Runnable() {
				public void run() {
					TextView tvDesc = (TextView) findViewById(R.id.main_desc);
					if (html)
					{
						tvDesc.setText(Utility.QspStrToHtml(txtMainDesc, imgGetter));
						tvDesc.setMovementMethod(QspLinkMovementMethod.getInstance());
					}
					else
						tvDesc.setText(txtMainDesc);
				}
			} );
    	}
    
    	//список действий
    	if (QSPIsActionsChanged())
    	{
	        int nActsCount = QSPGetActionsCount();
			if (html)
			{
		        final Spanned []acts = new Spanned[nActsCount];
		        for (int i=0;i<nActsCount;i++)
		        {
		        	JniResult actsResult = (JniResult) QSPGetActionData(i);
		        	acts[i] = Utility.QspStrToHtml(actsResult.str1, imgGetter);
		        }
				runOnUiThread(new Runnable() {
					public void run() {
				        ListView lvAct = (ListView)findViewById(R.id.acts);
				        lvAct.setAdapter(new ArrayAdapter<Spanned>(uiContext, R.layout.act_item, acts));
				        //Разворачиваем список действий
				        Utility.setListViewHeightBasedOnChildren(lvAct);
					}
				} );
			}
			else
			{
		        final String []acts = new String[nActsCount];
		        for (int i=0;i<nActsCount;i++)
		        {
		        	JniResult actsResult = (JniResult) QSPGetActionData(i);
		        	acts[i] = actsResult.str1;
		        }
				runOnUiThread(new Runnable() {
					public void run() {
				        ListView lvAct = (ListView)findViewById(R.id.acts);
				        lvAct.setAdapter(new ArrayAdapter<String>(uiContext, R.layout.act_item, acts));
				        //Разворачиваем список действий
				        Utility.setListViewHeightBasedOnChildren(lvAct);
					}
				} );
			}
    	}
        
        //инвентарь
    	if (QSPIsObjectsChanged())
    	{
			runOnUiThread(new Runnable() {
				public void run() {
					if(currentWin!=WIN_INV){
						invUnread = true;
						updateTitle();
					}
				}
			} );
	        int nObjsCount = QSPGetObjectsCount();
			if (html)
			{
		        final Spanned []objs = new Spanned[nObjsCount];
		        for (int i=0;i<nObjsCount;i++)
		        {
		        	JniResult objsResult = (JniResult) QSPGetObjectData(i);
		        	objs[i] = Utility.QspStrToHtml(objsResult.str1, imgGetter);
		        }
				runOnUiThread(new Runnable() {
					public void run() {
				        ListView lvInv = (ListView)findViewById(R.id.inv);
				        lvInv.setAdapter(new ArrayAdapter<Spanned>(uiContext, R.layout.obj_item, objs));
					}
				} );
			}
			else
			{
		        final String []objs = new String[nObjsCount];
		        for (int i=0;i<nObjsCount;i++)
		        {
		        	JniResult objsResult = (JniResult) QSPGetObjectData(i);
		        	objs[i] = objsResult.str1;
		        }
				runOnUiThread(new Runnable() {
					public void run() {
				        ListView lvInv = (ListView)findViewById(R.id.inv);
				        lvInv.setAdapter(new ArrayAdapter<String>(uiContext, R.layout.obj_item, objs));
					}
				} );
			}
    	}
        
        //доп. описание
    	if (QSPIsVarsDescChanged())
    	{
			final String txtVarsDesc = QSPGetVarsDesc();			
			runOnUiThread(new Runnable() {
				public void run() {
					if(currentWin!=WIN_EXT) {
						varUnread = true;
						updateTitle();
					}
					TextView tvVarsDesc = (TextView) findViewById(R.id.vars_desc);
					if (html)
					{
						tvVarsDesc.setText(Utility.QspStrToHtml(txtVarsDesc, imgGetter));
						tvVarsDesc.setMovementMethod(QspLinkMovementMethod.getInstance());
					}
					else
						tvVarsDesc.setText(txtVarsDesc);
				}
			} );
    	}
    }
    
    private void SetTimer(int msecs)
    {
    	//Контекст библиотеки
    	final int timeMsecs = msecs;
		runOnUiThread(new Runnable() {
			public void run() {
				timerInterval = timeMsecs;
			}
		} );
    }

    private void ShowMessage(String message)
    {
    	//Контекст библиотеки
		if (libThread==null)
		{
			WriteLog("ShowMessage: failed, libThread is null");
			return;
		}

		String msgValue = "";
		if ( message != null )
			msgValue = message;
		
		dialogHasResult = false;

    	final String msg = msgValue;
		runOnUiThread(new Runnable() {
			public void run() {
		    	new AlertDialog.Builder(uiContext)
		        .setMessage(msg)
		        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialogHasResult = true;
						WriteLog("ShowMessage(UI): OK clicked, unparking library thread");
		            	setThreadUnpark();
		            }
		        })
		        .setCancelable(false)
		        .show();
				WriteLog("ShowMessage(UI): dialog showed");
			}
		} );
    	
		WriteLog("ShowMessage: parking library thread");
        while (!dialogHasResult) {
        	setThreadPark();
        }
        parkThread = null;
		WriteLog("ShowMessage: library thread unparked, finishing");
    }
    
    private void PlayFile(String file, int volume)
    {
    	//Контекст библиотеки
    	final String musicFile = file;
    	final int musicVolume = volume;
    	runOnUiThread(new Runnable() {
    		public void run() {
    			PlayFileUI(musicFile, musicVolume);
    		}
    	});
    }
    
    private boolean IsPlayingFile(String file)
    {
    	//Контекст библиотеки
    	return IsPlayingFileUI(file);
    }

    private void CloseFile(String file)
    {
    	//Контекст библиотеки
    	final String musicFile = file;
    	runOnUiThread(new Runnable() {
    		public void run() {
    			CloseFileUI(musicFile);
    		}
    	});
    }
    
    private void ShowPicture(String file)
    {
    	//Контекст библиотеки
    	if (file == null || file.length() == 0)
    		return;
    	
    	final String fileName = file;
    	
		runOnUiThread(new Runnable() {
			public void run() {
		    	String prefix = "";
		    	if (curGameDir != null)
		    		prefix = curGameDir;
		    	
		    	//Проверяем, существует ли файл.
		    	//Если нет - выходим
		    	File gfxFile = new File(prefix.concat(fileName));
		        if (!gfxFile.exists())
		        	return;
		
		        waitForImageBox = true;
		        
		    	Intent imageboxIntent = new Intent();
		    	imageboxIntent.setClassName("com.qsp.player", "com.qsp.player.QspImageBox");
		    	Bundle b = new Bundle();
		    	b.putString("imageboxFile", prefix.concat(fileName));
		    	imageboxIntent.putExtras(b);
		    	startActivity(imageboxIntent);
			}
		});    	    	
    }
    
    private String InputBox(String prompt)
    {
    	//Контекст библиотеки
		if (libThread==null)
		{
			WriteLog("InputBox: failed, libThread is null");
			return "";
		}
    	
		String promptValue = "";
		if ( prompt != null )
			promptValue = prompt;
		
		dialogHasResult = false;

    	final String inputboxTitle = promptValue;
    	
		runOnUiThread(new Runnable() {
			public void run() {
				inputboxResult = "";
			    inputboxDialog.setTitle(inputboxTitle);
			    inputboxDialog.show();
				WriteLog("InputBox(UI): dialog showed");
			}
		} );
    	
		WriteLog("InputBox: parking library thread");
        while (!dialogHasResult) {
        	setThreadPark();
        }
        parkThread = null;
		WriteLog("InputBox: library thread unparked, finishing");
    	return inputboxResult;
    }
    
    private int GetMSCount()
    {
    	//Контекст библиотеки
    	return (int) (System.currentTimeMillis() - gameStartTime);
    }
    
    private void AddMenuItem(String name, String imgPath)
    {
    	//Контекст библиотеки
    	QspMenuItem item = new QspMenuItem();
    	item.imgPath = imgPath;
    	item.name = name;
    	menuList.add(item);
    }
    
    private void ShowMenu()
    {
    	//Контекст библиотеки
		if (libThread==null)
		{
			WriteLog("ShowMenu: failed, libThread is null");
			return;
		}
    	
		dialogHasResult = false;
		menuResult = -1;

		int total = menuList.size();
        final CharSequence[] items = new String[total];
        for (int i=0; i<total; i++)
        {
        	items[i] = menuList.elementAt(i).name;
        }
    	
		runOnUiThread(new Runnable() {
			public void run() {
		        new AlertDialog.Builder(uiContext)
		        .setItems(items, new DialogInterface.OnClickListener()
		        {
		    		@Override
		    		public void onClick(DialogInterface dialog, int which) 
		    		{
		               	menuResult = which;
		    			dialogHasResult = true;
		    			WriteLog("ShowMenu(UI): menu item selected, unparking library thread");
		               	setThreadUnpark();
		    		}
		        })
		        .setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						dialogHasResult = true;
						WriteLog("ShowMenu(UI): menu cancelled, unparking library thread");
			           	setThreadUnpark();
					}
				})
				.show();
			    WriteLog("ShowMenu(UI): dialog showed");
			}
		} );
    	
		WriteLog("ShowMenu: parking library thread");
        while (!dialogHasResult) {
        	setThreadPark();
        }
        parkThread = null;
		WriteLog("ShowMenu: library thread unparked, finishing");
    	
		if (menuResult != -1)
			QSPSelectMenuItem(menuResult);
    }
    
    private void DeleteMenu()
    {
    	//Контекст библиотеки
    	menuList.clear();
    }
    //******************************************************************************
    //******************************************************************************
    //****** \ QSP  LIBRARY  REQUIRED  CALLBACKS / *********************************
    //******************************************************************************
    //******************************************************************************
    
    
    public void OnUrlClicked (String href)
    {
    	//Контекст UI
    	String tag = href.substring(0, 5).toLowerCase();
    	if (tag.compareTo("exec:") == 0)
    	{
    		if (libraryThreadIsRunning)
    			return;
    		final String code = href.substring(5);
    		libThreadHandler.post(new Runnable() {
    			public void run() {
    	    		if (libraryThreadIsRunning)
    	    			return;
                	libraryThreadIsRunning = true;
                	
        	    	boolean bExec = QSPExecString(code, true);
        	    	if (!bExec)
        	    	{
        	    		int nError = QSPGetLastErrorData();
        	    		final String txtError = "Error: "+String.valueOf(nError);  
        	    		runOnUiThread(new Runnable() {
        	    			public void run() {
		        	    		new AlertDialog.Builder(uiContext)
		        	            .setMessage(txtError)
		        	            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		        	                public void onClick(DialogInterface dialog, int whichButton) { }
		        	            })
		        	            .show();
        	    			}
        	    		});
        	    	}
                	
            		libraryThreadIsRunning = false;
    			}
    		});
    	}
    }

	//Callback for click on selected act
    private OnItemClickListener actListClickListener = new OnItemClickListener() 
    {
    	//Контекст UI
    	@Override
    	public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3) 
    	{
    		if (libraryThreadIsRunning)
    			return;
    		final int actionIndex = position;
    		libThreadHandler.post(new Runnable() {
    			public void run() {
    	    		if (libraryThreadIsRunning)
    	    			return;
                	libraryThreadIsRunning = true;
            		QSPSetSelActionIndex(actionIndex, false);
            		QSPExecuteSelActionCode(true);
            		libraryThreadIsRunning = false;
    			}
    		});
    	}
    };
    
    //Callback for select act
    private OnItemSelectedListener actListSelectedListener = new OnItemSelectedListener() 
    {
    	//Контекст UI
		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
    		if (libraryThreadIsRunning)
    			return;
    		final int actionIndex = arg2;
    		libThreadHandler.post(new Runnable() {
    			public void run() {
    	    		if (libraryThreadIsRunning)
    	    			return;
                	libraryThreadIsRunning = true;
    				QSPSetSelActionIndex(actionIndex, true);
            		libraryThreadIsRunning = false;
    			}
    		});
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) 
		{
		}
    };
    
    //Callback for click on selected object
    private OnItemClickListener objListClickListener = new OnItemClickListener() 
    {
    	//Контекст UI
    	@Override
    	public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3) 
    	{
    		if (libraryThreadIsRunning)
    			return;
    		final int itemIndex = position;
    		libThreadHandler.post(new Runnable() {
    			public void run() {
    	    		if (libraryThreadIsRunning)
    	    			return;
                	libraryThreadIsRunning = true;
            		QSPSetSelObjectIndex(itemIndex, true);
            		libraryThreadIsRunning = false;
    			}
    		});
    	}
    };
    
    //Callback for select object
    private OnItemSelectedListener objListSelectedListener = new OnItemSelectedListener() 
    {
    	//Контекст UI
		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
    		if (libraryThreadIsRunning)
    			return;
    		final int itemIndex = arg2;
    		libThreadHandler.post(new Runnable() {
    			public void run() {
    	    		if (libraryThreadIsRunning)
    	    			return;
                	libraryThreadIsRunning = true;
            		QSPSetSelObjectIndex(itemIndex, true);
            		libraryThreadIsRunning = false;
    			}
    		});
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) 
		{
		}
    };
    
    //Для отображения картинок в HTML
    static QspImageGetter imgGetter = new QspImageGetter();
    
    //Хэндлер для UI-потока
    final Handler uiThreadHandler = new Handler();

    //Хэндлер для потока библиотеки
    private Handler libThreadHandler;
    
    //Поток библиотеки
    private Thread libThread;
    private Thread parkThread;
    
    //Запущен ли поток библиотеки
    boolean					libraryThreadIsRunning = false;
 
    //Есть ответ от MessageBox, InputBox либо Menu
    private boolean 		dialogHasResult;
    String					inputboxResult;
    int						menuResult;
    AlertDialog				inputboxDialog;
    
    ArrayList<File> 		qspGames;
    String					startRootPath;
    String					backPath;
    String 					curGameDir;
    Vector<MusicContent>	mediaPlayersList;
    Handler					timerHandler;
	long					timerStartTime;
	long					gameStartTime;
	int						timerInterval;
	boolean					gameIsRunning;
	boolean					qspInited;
	boolean					waitForImageBox;
	Vector<QspMenuItem>		menuList;
	
    
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
    public native Object 	QSPOpenSavedGameFromData(String str, boolean isRefresh);//!!!STUB
    public native boolean 	QSPRestartGame(boolean isRefresh);
    public native void		QSPSelectMenuItem(int index); 
    //public native void QSPSetCallBack(int type, QSP_CALLBACK func) 

    static {
	    System.loadLibrary("ndkqsp");
	}
}