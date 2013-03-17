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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private static final String Device = android.os.Build.DEVICE;
	private static final String SDPath = Environment.getExternalStorageDirectory().getPath();
	private static final String SystemApps = "/system/app";
	private static final String PathToBin = "/system/bin";
	
	private static boolean firststart = false;
	private static boolean useown = false;
	
	Context context = this;
	
	File browser = new File(SystemApps + "/", "Browser.apk");
	File chromesync = new File(SystemApps + "/", "ChromeBookmarksSyncAdapter.apk");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (!firststart){
			
			checkFolder(context.getFilesDir());
			
			firststart = true;
		
			final Dialog dialog = createDialog(R.string.loading, R.layout.activity_loading, false);
			TextView tvLoading = (TextView) dialog.findViewById(R.id.tvLoading);
			tvLoading.setText(R.string.checking);
			dialog.show();
			
	        if (!Device.equals("grouper") && !Device.equals("mako") && !Device.equals("manta") && !Device.equals("tilapia")) {
				createDialog(R.string.warning, R.string.notsupported, true);
			} else {
				File suxbin = new File("/system/xbin/", "su");
				File subin = new File ("/system/bin/", "su");
				if (!suxbin.exists() && !subin.exists()) {
					createDialog(R.string.warning, R.string.noroot, true);
				}else {
					File busybox = new File(PathToBin + "/" , "busybox");
					if (!busybox.exists()) {
						useown = true;
						File ownbusybox = new File(context.getFilesDir(), "/busybox");
						if (!ownbusybox.exists()) {
							pushFileFromRAW(ownbusybox, R.raw.busybox);
							chmod("641", ownbusybox);
						}
					}
				}
			}
	        final Timer t = new Timer();
	        t.schedule(new TimerTask() {
	            public void run() {
	            	dialog.dismiss();
	                t.cancel();
	            }
	        }, 1500);
		}
		getInfos();
	}
	
	public void checkFolder(File Folder) {
		if (!Folder.exists()) {
			Folder.mkdir();
		}
	}
	
	public void getInfos() {
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
	
	public void install(View v){
		
		if (!browser.exists()){
			AlertDialog.Builder abuilder = new AlertDialog.Builder(this);
			abuilder
				.setTitle(R.string.option)
				.setMessage(R.string.addsync)
				.setPositiveButton(R.string.positive, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					installBrowser(true);
				}
			})
				.setNeutralButton(R.string.neutral, new DialogInterface.OnClickListener() {
					
				@Override
				public void onClick(DialogInterface dialog, int which) {
					installBrowser(false);
				}
						
			})
				.setNegativeButton(R.string.negative, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			})
				.show();
		} else {
			createDialog(R.string.warning, R.string.already, true).show();
		}
	}
	
	public void installBrowser(boolean andsync){
		
		File browserapk = new File(SDPath + "/", "Browser.apk");
		pushFileFromRAW(browserapk, R.raw.browser);
		mountSystem(true);
		File bppapk = new File(SystemApps + "/", "BrowserProviderProxy.apk");
		File bppapkold = new File(SystemApps + "/", "BrowserProviderProxy.apk.old");
		if (!bppapkold.exists() && bppapk.exists()){
			moveFile(bppapk, bppapkold);
		}
		File bppodex = new File(SystemApps + "/", "BrowserProviderProxy.odex");
		File bppodexold = new File(SystemApps + "/", "BrowserProviderProxy.odex.old");
		if (!bppodexold.exists() && bppodex.exists()) {
			moveFile(bppodex, bppodexold);
		}
		moveFile(browserapk, browser);
		chmod("644", browser);
		if (andsync && !chromesync.exists()){
			File chromesyncapk = new File(SDPath + "/", "ChromeBookmarksSyncAdapter.apk");
			pushFileFromRAW(chromesyncapk, R.raw.chromebookmarkssyncadapter);
			moveFile(chromesyncapk, chromesync);
			chmod("644", chromesync);
		}
		mountSystem(false);
		createAlertDialog();
		getInfos();
	}
	
	public void uninstall(View v){
		if (browser.exists()) {
			mountSystem(true);
			File bppapk = new File(SystemApps + "/", "BrowserProviderProxy.apk");
			File bppapkold = new File(SystemApps + "/", "BrowserProviderProxy.apk.old");
			if (bppapkold.exists() && !bppapk.exists()) {
				moveFile(bppapkold, bppapk);
			}
			File bppodex = new File(SystemApps + "/", "BrowserProviderProxy.odex");
			File bppodexold = new File(SystemApps + "/", "BrowserProviderProxy.odex.old");
			if (bppodexold.exists() && !bppodex.exists()){
				moveFile(bppodexold, bppodex);
			}
			deleteFile(browser);
			if (chromesync.exists()){
				deleteFile(chromesync);
			}
			mountSystem(false);
			createAlertDialog();
		}else{
			createDialog(R.string.warning, R.string.notalready, true);
		}
		
		getInfos();
		
	}
	
	public Dialog createDialog(int Title, int Content, boolean isMessage) {
		Dialog dialog = new Dialog(this);
		if (!isMessage){
			dialog.setTitle(Title);
			dialog.setContentView(Content);
		} else {
			TextView tv = new TextView(this);
			tv.setText(Content);
			dialog.setContentView(tv);
			dialog.setTitle(Title);
		}
		return dialog;
	}
	public Dialog createDialog(String Title, File Message) {
		TextView tv = new TextView(this);
		tv.setText(Message.getAbsolutePath());
		Dialog dialog = new Dialog(this);
		dialog.setContentView(tv);
		dialog.setTitle(Title);
		dialog.show();
		return dialog;
	}
	
	public AlertDialog.Builder createAlertDialog(){
		AlertDialog.Builder abuilder = new AlertDialog.Builder(this);
		abuilder.setTitle(R.string.information);
		if (browser.exists()) {
			abuilder.setMessage(R.string.completeinstallation);
		} else {
			abuilder.setMessage(R.string.completeuninstallation);
		}
		abuilder.setPositiveButton(R.string.positive, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String Command[] = {"su", "-c", "reboot"};
				executeShell(Command);
			}
		})
			.setNeutralButton(R.string.neutral, new DialogInterface.OnClickListener() {
					
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}			
		})
			.show();
		return abuilder;
	}
	
	public void pushFileFromRAW(File file, int RAW) {
	    if (!file.exists()){
		    try {
		        InputStream is = getResources().openRawResource(RAW);
		        OutputStream os = new FileOutputStream(file);
		        byte[] data = new byte[is.available()];
		        is.read(data);
		        os.write(data);
		        is.close();
		        os.close();
		    } catch (IOException e) {}
	    }
	}

	public String executeShell(String[] Command){
		return new Shell().sendShellCommand(Command);
	}
	public void mountSystem(boolean RW){
		if (useown){
			if (RW){
				String Command[] = {"su", "-c", context.getFilesDir().getAbsolutePath() + "/busybox mount -o remount,rw /system"};
				executeShell(Command);
			} else {
				String Command[] = {"su", "-c", context.getFilesDir().getAbsolutePath() + "/busybox mount -o remount,rw /system"};
				executeShell(Command);
			}
		} else {
			if (RW){
				String Command[] = {"su", "-c", "busybox mount -o remount,rw /system"};
				executeShell(Command);
			} else {
				String Command[] = {"su", "-c", "busybox mount -o remount,rw /system"};
				executeShell(Command);
			}
		}
	}
	public void chmod(String mod, File file) {
		String Command[] = {"su", "-c", "chmod " + mod + " " + file.getAbsolutePath()};
		executeShell(Command);
	}
	public void moveFile(File input, File output){
		String from = input.getAbsolutePath();
		String to = output.getAbsolutePath();
		if (useown) {
			String Command[] = {"su", "-c", context.getFilesDir().getAbsolutePath() + "/busybox mv " + from + " " + to};
			executeShell(Command);
		} else{
			String Command[] = {"su", "-c", "busybox mv " + from + " " + to};
			executeShell(Command);
		}
	}
	public void deleteFile(File FileToDelete){
		String filepath = FileToDelete.getAbsolutePath();
		String Command[] = {"su", "-c", "rm " + filepath};
		executeShell(Command);
	}
}
