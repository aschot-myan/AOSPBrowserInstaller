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

public class Uninstallator extends AsyncTask <Void, Integer, Void>{
	
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

	
	public Uninstallator(Context context, Runnable getInfos){
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
		pd.setTitle("Uninstallator");
		pd.setMax(4);
		pd.setCancelable(false);
		pd.show();
	}

	@Override
	protected Void doInBackground(Void... options) {
		publishProgress(R.string.mount, 1);
		mountSystem(true);
		publishProgress(R.string.restore, 2);
		if (bppapkold.exists() && !bppapk.exists()) {
			moveFile(bppapkold, bppapk);
		}
		if (bppodexold.exists() && !bppodex.exists()){
			moveFile(bppodexold, bppodex);
		}
		publishProgress(R.string.clean, 3);
		deleteFile(browser);
		if (chromesync.exists()){
			deleteFile(chromesync);
		}
		publishProgress(R.string.unmount, 4);
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
	public void deleteFile(File FileToDelete){
		String Command[] = {"su", "-c", "rm " + FileToDelete.getAbsolutePath()};
		executeShell(Command);
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
	
}