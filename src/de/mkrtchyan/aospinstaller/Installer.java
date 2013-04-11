package de.mkrtchyan.aospinstaller;

import java.io.File;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public class Installer extends AsyncTask <Boolean, Integer, Void>{
	
	private static final String Device = android.os.Build.DEVICE;
	private static final File SystemApps = new File("/system/app");
	private static final File PathToBin = new File("/system/bin");
	private static boolean useown = false;
	
	Context context;
	NotificationUtil nu;
	CommonUtil cu;
	ProgressDialog pd;
	File busybox = new File(PathToBin, "busybox");
	File ownbusybox;
	File browser = new File(SystemApps, "Browser.apk");
	File chromesync = new File(SystemApps, "ChromeBookmarksSyncAdapter.apk");
	File browserapk;
	File chromesyncapk;
	File bppapk = new File(SystemApps, "BrowserProviderProxy.apk");
	File bppapkold = new File(SystemApps, "BrowserProviderProxy.apk.old");
	File bppodex = new File(SystemApps, "BrowserProviderProxy.odex");
	File bppodexold = new File(SystemApps, "BrowserProviderProxy.odex.old");
	
	static Runnable rtrue, rneutral, rfalse;
	Runnable getInfos;
	
	public Installer(Context context, Runnable getInfos){
		this.context = context;
		this.getInfos = getInfos;
		nu = new NotificationUtil(context);
		cu = new CommonUtil(context);
		browserapk = new File(context.getFilesDir(), "Browser.apk");
		chromesyncapk = new File(context.getFilesDir(), "ChromeBookmarksSyncAdapter.apk");
	}
	
	protected void onPreExecute(){
		resetRunnables();
		ownbusybox = new File(context.getFilesDir(), "/busybox");
		if (!busybox.exists()) {
			useown = true;
			if (!ownbusybox.exists()) {
				cu.pushFileFromRAW(ownbusybox, R.raw.busybox);
				cu.chmod("641", ownbusybox);
			}
		}
		pd = new ProgressDialog(context);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setTitle(R.string.installator);
		pd.setMax(9);
		pd.setCancelable(false);
		pd.show();
	}

	@Override
	protected Void doInBackground(Boolean... options) {
		publishProgress(R.string.unpackbrowser, 1);
		if (Device.equals("mako")) {
			cu.pushFileFromRAW(browserapk, R.raw.browser_n4);
		} else {
			cu.pushFileFromRAW(browserapk, R.raw.browser);
		}
		publishProgress(R.string.mount, 2);
		mountSystem(true);
		publishProgress(R.string.backup, 3);
		if (!bppapkold.exists() && bppapk.exists()){
			moveFile(bppapk, bppapkold);
		}
		if (!bppodexold.exists() && bppodex.exists()) {
			moveFile(bppodex, bppodexold);
		}
		publishProgress(R.string.pushbrowser, 4);
		moveFile(browserapk, browser);
		publishProgress(R.string.setpermissions, 5);
		cu.chmod("644", browser);
		if (options[0] && !chromesync.exists()){
			publishProgress(R.string.unpacksync, 6);
			cu.pushFileFromRAW(chromesyncapk, R.raw.chromebookmarkssyncadapter);
			publishProgress(R.string.pushsync, 7);
			moveFile(chromesyncapk, chromesync);
			publishProgress(R.string.setpermissions, 8);
			cu.chmod("644", chromesync);
		}
		publishProgress(R.string.unmount, 9);
		mountSystem(false);
		return null;
	}
	
	public void moveFile(File input, File output){

		if (useown) {
			cu.executeShell(context.getFilesDir().getAbsolutePath() + "/busybox mv " + input.getAbsolutePath() + " " + output.getAbsolutePath());
		} else{
			cu.executeShell("busybox mv " + input.getAbsolutePath() + " " + output.getAbsolutePath());
		}
	}
	
	public void mountSystem(boolean RW){
		if (useown){
			if (RW){
				cu.executeShell(ownbusybox.getAbsolutePath() + " mount -o remount,rw /system");
			} else {
				cu.executeShell(ownbusybox.getAbsolutePath() + " mount -o remount,ro /system");
			}
		} else {
			if (RW){
				cu.executeShell("busybox mount -o remount,rw /system");
			} else {
				cu.executeShell("busybox mount -o remount,ro /system");
			}
		}
	}
	
	protected void onPostExecute(Void result){
		pd.dismiss();
		resetRunnables();
		rtrue = new Runnable(){
			
			@Override
			public void run() {
				cu.executeShell("reboot");
			}
		};
		nu.createAlertDialog(R.string.information, R.string.completeuninstallation, true, rtrue, false, rneutral, true, rfalse);
		getInfos.run();
	}
	
	protected void onProgressUpdate(Integer... states) {
		pd.setTitle(states[0]);
		pd.setProgress(states[1]);
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
	
}
