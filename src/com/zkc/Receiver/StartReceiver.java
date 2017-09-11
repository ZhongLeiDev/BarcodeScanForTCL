package com.zkc.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.zkc.Service.CaptureService;

public class StartReceiver extends BroadcastReceiver {

	public static int times = 0;
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
		
			Intent newIntent = new Intent(context, CaptureService.class);			    	 
	    	newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startService(newIntent);
			
		}
	}

}