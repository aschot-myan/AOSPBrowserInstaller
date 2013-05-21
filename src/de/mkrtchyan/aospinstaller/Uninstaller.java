package de.mkrtchyan.aospinstaller;

import java.io.File;

import de.mkrtchyan.utils.Common;
import de.mkrtchyan.utils.Notifyer;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public class Uninstaller extends AsyncTask <Void, Integer, Void>{
	
	private static final File SystemApps = new File("/system/app");
	private static final File PathToBin = new File("/system/bin");
	private static boolean useown = false;
	
	Context mContext;
	Notifyer n;
	Common c = new Common();
	ProgressDialog pd;
	File busybox = new File(PathToBin, "busybox");
	File ownbusybox;
	File browser = new File(SystemApps, "Browser.apk");
	File chromesync = new File(SystemApps, "ChromeBookmarksSyncAdapter.apk");
	File bppapk = new File(SystemApps, "BrowserProviderProxy.apk");
	File bppapkold = new File(SystemApps, "BrowserProviderProxy.apk.old");
	File bppodex = new File(SystemApps, "BrowserProviderProxy.odex");
	File bppodexold = new File(SystemApps, "BrowserProviderProxy.odex.old");
	
	static Runnable rtrue, rneutral, rfalse;
	Runnable getInfos;

	
	public Uninstaller(Context context, Runnable getInfos){
		mContext = context;
		this.getInfos = getInfos;
		n = new Notifyer(context);
		ownbusybox = new File(context.getFilesDir(), "/busybox");
	}
	
	protected void onPreExecute(){
		resetRunnables();
		if (!busybox.exists()) {
			useown = true;
			if (!ownbusybox.exists()) {
				c.pushFileFromRAW(mContext, ownbusybox, R.raw.busybox);
				c.chmod(ownbusybox, "641", true);
			}
		}
		pd = new ProgressDialog(mContext);
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
	
	public void moveFile(File input, File output){

		if (useown) {
			c.executeShell(mContext.getFilesDir().getAbsolutePath() + "/busybox mv " + input.getAbsolutePath() + " " + output.getAbsolutePath());
		} else{
			c.executeShell("busybox mv " + input.getAbsolutePath() + " " + output.getAbsolutePath());
		}
	}
	
	public void deleteFile(File FileToDelete){
		c.executeShell("rm " + FileToDelete.getAbsolutePath());
	}
	
	public void mountSystem(boolean RW){
		if (useown){
			if (RW){
				c.executeShell(ownbusybox.getAbsolutePath() + " mount -o remount,rw /system");
			} else {
				c.executeShell(ownbusybox.getAbsolutePath() + " mount -o remount,ro /system");
			}
		} else {
			if (RW){
				c.executeShell("busybox mount -o remount,rw /system");
			} else {
				c.executeShell("busybox mount -o remount,ro /system");
			}
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
				c.executeShell("reboot");
			}
		};
		n.createAlertDialog(R.string.information, R.string.completeuninstallation, rtrue, null, rfalse);
		getInfos.run();
	}
	
	protected void onProgressUpdate(Integer... states) {
		pd.setTitle(states[0]);
		pd.setProgress(states[1]);
	}
	
}