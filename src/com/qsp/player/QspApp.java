package com.qsp.player;

import android.app.Application;
import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(formKey = "dF9nZ1dZT2FHR1NWRGdQeWZrX1M0a1E6MQ") 
public class QspApp extends Application {
	@Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        super.onCreate();
    }
}
