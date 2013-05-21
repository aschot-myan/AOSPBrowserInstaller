package de.mkrtchyan.aospinstaller;

import java.io.File;

import org.rootcommands.util.RootAccessDeniedException;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import de.mkrtchyan.utils.Common;
import de.mkrtchyan.utils.Notifyer;

public class Installer extends AsyncTask <Boolean, Integer, Void>{
	
	private static final File SystemApps = new File("/system/app");
	private static final File PathToBin = new File("/system/bin");
	private static boolean useown = false;
	
	Context mContext;
	Notifyer n;
	Common c;
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
	Runnable getInfos = AOSPBrowserInstaller.getInfos;
	
	public Installer(Context context){
		mContext = context;
		n = new Notifyer(mContext);
		c = new Common();
		browserapk = new File(context.getFilesDir(), "Browser.apk");
		chromesyncapk = new File(context.getFilesDir(), "ChromeBookmarksSyncAdapter.apk");
	}
	
	protected void onPreExecute(){
		resetRunnables();
		ownbusybox = new File(mContext.getFilesDir(), "/busybox");
		if (!busybox.exists()) {
			useown = true;
			if (!ownbusybox.exists()) {
				c.pushFileFromRAW(mContext, ownbusybox, R.raw.busybox);
				c.chmod(ownbusybox, "641", true);
			}
		}
		pd = new ProgressDialog(mContext);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setTitle(R.string.installator);
		pd.setMax(9);
		pd.setCancelable(false);
		pd.show();
	}

	@Override
	protected Void doInBackground(Boolean... options) {
		publishProgress(R.string.unpackbrowser, 1);
		c.pushFileFromRAW(mContext, browserapk, R.raw.browser);
		publishProgress(R.string.mount, 2);
		try {
			c.mountDir(new File("/system"), "RW");
		} catch (RootAccessDeniedException e) {
			e.printStackTrace();
		}
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
		c.chmod(browser, "644", true);
		if (options[0] && !chromesync.exists()){
			publishProgress(R.string.unpacksync, 6);
			c.pushFileFromRAW(mContext, chromesyncapk, R.raw.chromebookmarkssyncadapter);
			publishProgress(R.string.pushsync, 7);
			moveFile(chromesyncapk, chromesync);
			publishProgress(R.string.setpermissions, 8);
			c.chmod(chromesync, "644", true);
		}
		publishProgress(R.string.unmount, 9);
		try {
			c.mountDir(new File("/system"), "RO");
		} catch (RootAccessDeniedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void moveFile(File input, File output){

		if (useown) {
			c.executeShell(mContext.getFilesDir().getAbsolutePath() + "/busybox mv " + input.getAbsolutePath() + " " + output.getAbsolutePath());
		} else{
			c.executeShell("busybox mv " + input.getAbsolutePath() + " " + output.getAbsolutePath());
		}
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
