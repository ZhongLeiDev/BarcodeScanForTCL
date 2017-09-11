package com.zkc.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.zkc.Service.CaptureService;


public class ShutdownReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		CaptureService.scanGpio.closePower();
	}
}

