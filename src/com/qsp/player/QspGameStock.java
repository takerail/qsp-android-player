package com.qsp.player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import org.apache.http.util.ByteArrayBuffer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.AdapterView.OnItemClickListener;

public class QspGameStock extends TabActivity {

	public class GameItem {
		//Parsed
		String author;
		String ported_by;
		String version;
		String title;
		String lang;
		String player;
		String file_url;
		String desc_url;
		String pub_date;
		String mod_date;
		//Flags
		boolean downloaded;
		boolean checked;
		//Local
		String game_file;
		GameItem()
		{
			author = "";
			ported_by = "";
			version = "";
			title = "";
			lang = "";
			player = "";
			file_url = "";
			desc_url = "";
			pub_date = "";
			mod_date = "";
			downloaded = false;
			checked = false;
			game_file = "";
		}
	}
	
	final private Context uiContext = this;
	private String xmlGameListCached;
	private boolean openDefaultTab;	

    String					startRootPath;
    String					backPath;
    ArrayList<File> 		qspGamesBrowseList;
	
	HashMap<String, GameItem> gamesMap;
	
	ListView lvAll;
	ListView lvDownloaded;
	ListView lvStarred;
	
	

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);
        
        TabHost tabHost = getTabHost();
        LayoutInflater.from(getApplicationContext()).inflate(R.layout.gamestock, tabHost.getTabContentView(), true);
        tabHost.addTab(tabHost.newTabSpec("downloaded")
                .setIndicator("Загруженные")
                .setContent(R.id.downloaded_tab));
        tabHost.addTab(tabHost.newTabSpec("starred")
                .setIndicator("Отмеченные")
                .setContent(R.id.starred_tab));
        tabHost.addTab(tabHost.newTabSpec("all")
                .setIndicator("Все")
                .setContent(R.id.all_tab));
        
    	gamesMap = new HashMap<String, GameItem>();
    	
    	openDefaultTab = true;
    	
    	InitListViews();
    	
    	setResult(RESULT_CANCELED);
        
        loadGameList.start();
        
        //TODO: 
        // 1. Отображение статуса "Загружено", например цветом фона.
        // 2. Авто-обновление игр
        // 3. Кэширование списка игр
        // 4. Доступ к играм, даже когда сервер недоступен
        // 5. Вывод игр в папке "Загруженные" в порядке последнего доступа к ним
        // 6. Возможность открыть файл из любой папки(через специальное меню этой активити)
        // 7. Доступ к настройкам приложения через меню этой активити
    }
    
    private void InitListViews()
    {
		lvAll = (ListView)findViewById(R.id.all_tab);
		lvDownloaded = (ListView)findViewById(R.id.downloaded_tab);
		lvStarred = (ListView)findViewById(R.id.starred_tab);

        lvDownloaded.setTextFilterEnabled(true);
        lvDownloaded.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lvDownloaded.setOnItemClickListener(gameListClickListener);
		
        lvStarred.setTextFilterEnabled(true);
        lvStarred.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lvStarred.setOnItemClickListener(gameListClickListener);

        lvAll.setTextFilterEnabled(true);
        lvAll.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lvAll.setOnItemClickListener(gameListClickListener);
    }

    //Выбрана игра в списке
    private OnItemClickListener gameListClickListener = new OnItemClickListener() 
    {
    	@Override
    	public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3) 
    	{
    		String value = null;
    		switch (getTabHost().getCurrentTab()) {
    		case 0:
    			//Загруженные
    			value = lvDownloaded.getAdapter().getItem(position).toString();
    			break;
    		case 1:
    			//Отмеченные
    			value = lvStarred.getAdapter().getItem(position).toString();
    			break;
    		case 2:
    			//Все
    			value = lvAll.getAdapter().getItem(position).toString();
    			break;
    		}
    		
    		GameItem selectedGame = gamesMap.get(value);
    		if (selectedGame == null)
    			return;
    		
    		if (selectedGame.downloaded)
    		{
    			//Если игра загружена, стартуем
    			Intent data = new Intent();
    			data.putExtra("file_name", selectedGame.game_file);
    			setResult(RESULT_OK, data);
    			finish();
    		}
    		else
    		{
    			//Игра не загружена, пытаемся загрузить с сервера
    			//!!! STUB
    		}
    	}
    };

    private void RefreshLists()
    {
		if (!ParseGameList(xmlGameListCached))
			return;
		if (!ScanDownloadedGames())
			return;
		GetCheckedGames();
		
		
		RefreshAllTabs();
    }
    
    private boolean ScanDownloadedGames()
    {
    	//!!! STUB
    	//Заполняем список скачанных игр
    	
    	String path = Utility.GetDefaultPath();
    	if (path == null)
    		return false;
    	
        File gameStartDir = new File (path);
        File[] sdcardFiles = gameStartDir.listFiles();        
        ArrayList<File> qspGameDirs = new ArrayList<File>();
        ArrayList<File> qspGameFiles = new ArrayList<File>();
        //Сначала добавляем все папки
        for (File currentFile : sdcardFiles)
        {
        	if (currentFile.isDirectory() && !currentFile.isHidden() && !currentFile.getName().startsWith("."))
        	{
        		//Из папок добавляем только те, в которых есть игра
                File[] curDirFiles = currentFile.listFiles();        
                for (File innerFile : curDirFiles)
                {
                	if (!innerFile.isHidden() && (innerFile.getName().endsWith(".qsp") || innerFile.getName().endsWith(".gam")))
                	{
                		qspGameDirs.add(currentFile);
                		qspGameFiles.add(innerFile);
                		break;
                	}
                }
        	}
        }

        //Ищем загруженные игры в карте
        for (int i=0; i<qspGameDirs.size(); i++)
        {
        	File d = qspGameDirs.get(i);
        	String displayName = d.getName();
        	GameItem game = gamesMap.get(displayName);
        	if (game == null)
        	{
        		game = new GameItem();
        		game.title = displayName;
        	}
        	File f = qspGameFiles.get(i);
    		game.game_file = f.getPath();
    		game.downloaded = true;
    		gamesMap.put(displayName, game);
        }
    
        return true;
    }
    
    private void GetCheckedGames()
    {
    	//!!! STUB
    	//Заполняем список отмеченных игр
    	
    }
   
    private void RefreshAllTabs()
    {
    	//!!! STUB
    	//Выводим списки игр на экран

    	//Все
        int gamesCount = gamesMap.size();
        final String []gamesAll = new String[gamesCount];
        int i = 0;
        for (HashMap.Entry<String, GameItem> e : gamesMap.entrySet())
        {
        	gamesAll[i] = e.getKey();
        	i++;
        }
        lvAll.setAdapter(new ArrayAdapter<String>(uiContext, R.layout.act_item, gamesAll));

        //Загруженные
		Vector<String> gamesDownloaded = new Vector<String>();
        for (HashMap.Entry<String, GameItem> e : gamesMap.entrySet())
        {
        	if (e.getValue().downloaded)
        		gamesDownloaded.add(e.getKey());
        }
        String []gamesD = gamesDownloaded.toArray(new String[gamesDownloaded.size()]);
        lvDownloaded.setAdapter(new ArrayAdapter<String>(uiContext, R.layout.act_item, gamesD));
        
        //Отмеченные
        //!!! STUB
        String []gamesStarred = new String[0];
        lvStarred.setAdapter(new ArrayAdapter<String>(uiContext, R.layout.act_item, gamesStarred));
        
        //Определяем, какую вкладку открыть
        if (openDefaultTab)
        {
        	openDefaultTab = false;
        	
        	int tabIndex = 0;//Загруженные
        	if (lvDownloaded.getAdapter().isEmpty())
        	{
        		if (lvStarred.getAdapter().isEmpty())
        			tabIndex = 2;//Все
        		else
        			tabIndex = 1;//Отмеченные
        	}
        	
        	getTabHost().setCurrentTab(tabIndex);
        }
    }
    
    private boolean ParseGameList(String xml)
    {
    	boolean parsed = false;
    	gamesMap.clear();
    	GameItem curItem = null;
    	try {
    		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    		factory.setNamespaceAware(true);
    		XmlPullParser xpp = factory.newPullParser();

    		xpp.setInput( new StringReader ( xml ) );
    		int eventType = xpp.getEventType();
    		boolean doc_started = false;
    		boolean list_started = false;
    		String lastTagName = "";
    		while (eventType != XmlPullParser.END_DOCUMENT) {
    			if(eventType == XmlPullParser.START_DOCUMENT) {
    				doc_started = true;
    			} else if(eventType == XmlPullParser.END_DOCUMENT) {
    				//Never happens
    			} else if(eventType == XmlPullParser.START_TAG) {
    				if (doc_started)
    				{
    					lastTagName = xpp.getName();
    					if (lastTagName.equals("game_list"))
    					{
    						list_started = true;
    					}
    					if (list_started)
    					{
    						if (lastTagName.equals("game"))
    							curItem = new GameItem();
    					}            		 
    				}
    			} else if(eventType == XmlPullParser.END_TAG) {
    				if (doc_started && list_started)
    				{
    					if (xpp.getName().equals("game"))
    					{
    						gamesMap.put(curItem.title, curItem);
    					}
    					if (xpp.getName().equals("game_list"))
    						parsed = true;
    					lastTagName = "";
    				}
    			} else if(eventType == XmlPullParser.CDSECT) {
    				if (doc_started && list_started)
    				{
    					String val = xpp.getText();
    					if (lastTagName.equals("author"))
    						curItem.author = val;
    					else if (lastTagName.equals("ported_by"))
    						curItem.ported_by = val;
    					else if (lastTagName.equals("version"))
    						curItem.version = val;
    					else if (lastTagName.equals("title"))
    						curItem.title = val;
    					else if (lastTagName.equals("lang"))
    						curItem.lang = val;
    					else if (lastTagName.equals("player"))
    						curItem.player = val;
    					else if (lastTagName.equals("file_url"))
    						curItem.file_url = val;
    					else if (lastTagName.equals("desc_url"))
    						curItem.desc_url = val;
    					else if (lastTagName.equals("pub_date"))
    						curItem.pub_date = val;
    					else if (lastTagName.equals("mod_date"))
    						curItem.mod_date = val;
    				}
    			}
   				eventType = xpp.nextToken();
    		}
    	} catch (XmlPullParserException e) {
    		String errTxt = "Exception occured while trying to parse game list, XML corrupted at line ".
    					concat(String.valueOf(e.getLineNumber())).concat(", column ").
    					concat(String.valueOf(e.getColumnNumber())).concat(".");
    		Utility.WriteLog(errTxt);
    	} catch (Exception e) {
    		Utility.WriteLog("Exception occured while trying to parse game list, unknown error");
    	}
    	if (!parsed)
    	{
    		//Показываем сообщение об ошибке
    		new AlertDialog.Builder(uiContext)
    		.setMessage("Ошибка: Не удалось загрузить список игр. Проверьте интернет-подключение.")
    		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int whichButton) { }
    		})
    		.show();
    	}
    	return parsed;
    }

    private Thread loadGameList = new Thread() {
    	//Загружаем список игр
        public void run() {
            try {
                URL updateURL = new URL("http://qsp.su/gamestock/games-ru.xml");
                URLConnection conn = updateURL.openConnection();
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                ByteArrayBuffer baf = new ByteArrayBuffer(50);

                int current = 0;
                while((current = bis.read()) != -1){
                    baf.append((byte)current);
                }

                /* Convert the Bytes read to a String. */
                final String xml = new String(baf.toByteArray());
    			runOnUiThread(new Runnable() {
    				public void run() {
    					xmlGameListCached = xml;
   						RefreshLists();
    				}
    			});
            } catch (Exception e) {
            	Utility.WriteLog("Exception occured while trying to load game list");
            }
        }
    };
    
    
    //***********************************************************************
    //			выбор файла "напрямую", через пролистывание папок
    //***********************************************************************
    android.content.DialogInterface.OnClickListener browseFileClick = new DialogInterface.OnClickListener()
    {
    	//Контекст UI
		@Override
		public void onClick(DialogInterface dialog, int which) 
		{
			boolean canGoUp = !backPath.equals("");
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
				File f = qspGamesBrowseList.get(which - shift);
				if (f.isDirectory())
					BrowseGame(f.getPath(), false);
				else
				{
					//Сделать правильный возврат из активити
					//!!! STUB
					//runGame(f.getPath());
				}
			}
		}    	
    };
    
    private void BrowseGame(String startpath, boolean start)
    {
    	//Контекст UI
    	if (startpath == null)
    		return;
    	
    	//Устанавливаем путь "выше"    	
    	if (!start)
    		if (startRootPath.equals(startpath))
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
        qspGamesBrowseList = new ArrayList<File>();
        //Сначала добавляем все папки
        for (File currentFile : sdcardFiles)
        {
        	if (currentFile.isDirectory() && !currentFile.isHidden() && !currentFile.getName().startsWith("."))
        		qspGamesBrowseList.add(currentFile);
        }
        //Потом добавляем все QSP-игры
        for (File currentFile : sdcardFiles)
        {
        	if (!currentFile.isHidden() && (currentFile.getName().endsWith(".qsp") || currentFile.getName().endsWith(".gam")))
        		qspGamesBrowseList.add(currentFile);
        }
        
        //Если мы не на самом верхнем уровне, то добавляем ссылку 
        int shift = 0;
        if (!start)
        	shift = 1;
        int total = qspGamesBrowseList.size() + shift;
        final CharSequence[] items = new String[total];
        if (!start)
            items[0] = "[..]";
        for (int i=shift; i<total; i++)
        {
        	File f = qspGamesBrowseList.get(i - shift);
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
    
}
