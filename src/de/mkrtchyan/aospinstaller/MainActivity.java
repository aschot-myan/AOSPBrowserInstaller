package de.mkrtchyan.aospinstaller;

/*
 * Copyright (c) 2013 Ashot Mkrtchyan
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.File;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private static final String Device = android.os.Build.DEVICE;
	private static final String SystemApps = "/system/app";
	private static final int nid = 25;
	private static boolean firststart = false;
	
	Context context = this;
	NotificationUtil nu = new NotificationUtil(context);
	CommonUtil cu = new CommonUtil(context);
	
	File browser = new File(SystemApps + "/", "Browser.apk");
	File chromesync = new File(SystemApps + "/", "ChromeBookmarksSyncAdapter.apk");
	
	static Runnable rtrue, rneutral, rfalse, getInfos;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (!firststart){
			if (!cu.suRecognition()){
				createNotification(R.drawable.ic_launcher, R.string.warning, R.string.noroot, nid);
				finish();
				System.exit(0);
			}
			
			resetRunnables();
			
			cu.checkFolder(context.getFilesDir());
			
			firststart = true;
		
	        if (!Device.equals("grouper") && !Device.equals("mako") && !Device.equals("manta") && !Device.equals("tilapia")) {
				nu.createDialog(R.string.warning, R.string.notsupported, true, true);
			}
		}
		getInfos = new Runnable(){

			@Override
			public void run() {
				ProgressBar pbInstallation = (ProgressBar) findViewById(R.id.pbInstallation);
				Button Install = (Button) findViewById(R.id.bInstall);
				Button Uninstall = (Button) findViewById(R.id.bUninstall);
				TextView tvInfo = (TextView) findViewById(R.id.tvInfo);
				ImageView ivIconUninstalled = (ImageView) findViewById(R.id.ivIconUninstalled);
				ImageView ivIconInstalled = (ImageView) findViewById(R.id.ivIconInstalled);
				pbInstallation.setMax(1);
				if (browser.exists()) {
					tvInfo.setText(R.string.installed);
					Install.setVisibility(4);
					Install.setClickable(false);
					Uninstall.setVisibility(0);
					Uninstall.setClickable(true);
					ivIconInstalled.setVisibility(0);
					ivIconUninstalled.setVisibility(4);
					pbInstallation.setProgress(1);
					if (chromesync.exists()){
						tvInfo.setText(tvInfo.getText()+ " (with ChromeSync)");
					}
				} else {
					tvInfo.setText(R.string.notinstalled);
					pbInstallation.setProgress(0);
					Install.setVisibility(0);
					Install.setClickable(true);
					Uninstall.setVisibility(4);
					Uninstall.setClickable(false);
					ivIconInstalled.setVisibility(4);
					ivIconUninstalled.setVisibility(0);
				}
			}				
		};
		getInfos.run();
	}
	
	
	public void install(View v){
		
		rtrue = new Runnable(){

			@Override
			public void run() {
				new Installer(context, getInfos).execute(true);
			}
			
		};
		rneutral = new Runnable(){

			@Override
			public void run() {
				new Installer(context, getInfos).execute(false);
			}
			
		};
		nu.createAlertDialog(R.string.option, R.string.addsync, true, rtrue , true, rneutral, true, rfalse);
	}

	public void uninstall(View v){
		new Uninstaller(context, getInfos).execute();
	}
	
	public void resetRunnables(){
		rtrue = new Runnable(){
			@Override
			public void run() {
			}
		};
		rneutral = new Runnable(){
			@Override
			public void run() {
			}
		};
		rfalse = new Runnable(){
			@Override
			public void run() {
			}
		};
	}
	
	@SuppressWarnings("deprecation")
	public void createNotification(int Icon, int Title, int Message, int nid) {
		Intent intent = new Intent();
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification n = new Notification(Icon, getString(Message), System.currentTimeMillis());
		n.setLatestEventInfo(this, getString(Title), getString(Message), pi);
		n.flags = Notification.FLAG_AUTO_CANCEL;
		nm.notify(nid, n);	
	}
}
