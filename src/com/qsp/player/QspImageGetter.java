package com.qsp.player;

import android.graphics.drawable.Drawable;
import android.text.Html.ImageGetter;
import android.view.Display;

public class QspImageGetter implements ImageGetter {
	private String mDirectory;
	private int mScreenWidth;
	
	public void SetDirectory(String directory)
	{
		mDirectory = directory;
	}
	
	public void SetScreenWidth(int width)
	{
		mScreenWidth = width;
	}

	@Override
    public Drawable getDrawable(String source) {
          Drawable drawable = null;
          if (!source.contains("://"))
          {
        	  source = mDirectory.concat(source);
              drawable = Drawable.createFromPath(source);
              int nWidth = (int) ((int)drawable.getIntrinsicWidth()*0.75);
              int nHeight = (int) ((int)drawable.getIntrinsicHeight()*0.75);
              
              if (nWidth>mScreenWidth)
              {
            	  float k = mScreenWidth;
            	  k = k/nWidth;
            	  nWidth = mScreenWidth;
            	  nHeight = (int) ((int)nHeight*k);
              }
              
              drawable.setBounds(0, 0, nWidth, nHeight);
          }
          return drawable;
    }
}
