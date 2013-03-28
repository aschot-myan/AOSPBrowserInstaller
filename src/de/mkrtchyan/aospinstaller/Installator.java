package de.mkrtchyan.aospinstaller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

public class Installator extends AsyncTask <Boolean, Integer, Void>{
	
	private static final String SDPath = Environment.getExternalStorageDirectory().getPath();
	private static final String SystemApps = "/system/app";
	private static final String PathToBin = "/system/bin";
	private static boolean useown = false;
	
	Context context;
	NotificationUtil nu;
	ProgressDialog pd;
	File busybox = new File(PathToBin + "/" , "busybox");
	File ownbusybox;
	File browser = new File(SystemApps + "/", "Browser.apk");
	File chromesync = new File(SystemApps + "/", "ChromeBookmarksSyncAdapter.apk");
	File browserapk = new File(SDPath + "/", "Browser.apk");
	File bppapk = new File(SystemApps + "/", "BrowserProviderProxy.apk");
	File bppapkold = new File(SystemApps + "/", "BrowserProviderProxy.apk.old");
	File bppodex = new File(SystemApps + "/", "BrowserProviderProxy.odex");
	File bppodexold = new File(SystemApps + "/", "BrowserProviderProxy.odex.old");
	
	static Runnable rtrue, rneutral, rfalse;
	Runnable getInfos;
	
	public Installator(Context context, Runnable getInfos){
		this.context = context;
		this.getInfos = getInfos;
		nu = new NotificationUtil(context);
	}
	
	protected void onPreExecute(){
		resetRunnables();
		ownbusybox = new File(context.getFilesDir(), "/busybox");
		if (!busybox.exists()) {
			useown = true;
			if (!ownbusybox.exists()) {
				pushFileFromRAW(ownbusybox, R.raw.busybox);
				chmod("641", ownbusybox);
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
		pushFileFromRAW(browserapk, R.raw.browser);
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
		chmod("644", browser);
		if (options[0] && !chromesync.exists()){
			File chromesyncapk = new File(SDPath + "/", "ChromeBookmarksSyncAdapter.apk");
			publishProgress(R.string.unpacksync, 6);
			pushFileFromRAW(chromesyncapk, R.raw.chromebookmarkssyncadapter);
			publishProgress(R.string.pushsync, 7);
			moveFile(chromesyncapk, chromesync);
			publishProgress(R.string.setpermissions, 8);
			chmod("644", chromesync);
		}
		publishProgress(R.string.unmount, 9);
		mountSystem(false);
		return null;
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
	public String executeShell(String[] Command){
		return new Shell().sendShellCommand(Command);
	}
	public void mountSystem(boolean RW){
		if (useown){
			if (RW){
				String Command[] = {"su", "-c", context.getFilesDir().getAbsolutePath() + "/busybox mount -o remount,rw /system"};
				executeShell(Command);
			} else {
				String Command[] = {"su", "-c", context.getFilesDir().getAbsolutePath() + "/busybox mount -o remount,ro /system"};
				executeShell(Command);
			}
		} else {
			if (RW){
				String Command[] = {"su", "-c", "busybox mount -o remount,rw /system"};
				executeShell(Command);
			} else {
				String Command[] = {"su", "-c", "busybox mount -o remount,ro /system"};
				executeShell(Command);
			}
		}
	}
	public void pushFileFromRAW(File file, int RAW) {
	    if (!file.exists()){
		    try {
		        InputStream is = context.getResources().openRawResource(RAW);
		        OutputStream os = new FileOutputStream(file);
		        byte[] data = new byte[is.available()];
		        is.read(data);
		        os.write(data);
		        is.close();
		        os.close();
		    } catch (IOException e) {}
	    }
	}
	
	protected void onPostExecute(Void result){
		pd.dismiss();
		resetRunnables();
		rtrue = new Runnable(){
			
			@Override
			public void run() {
				String Command[] = {"su", "-c", "reboot"};
				executeShell(Command);
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
