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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.sbstrm.appirater.Appirater;

import java.io.File;

import de.mkrtchyan.utils.Common;
import de.mkrtchyan.utils.Notifyer;

public class Installer extends AsyncTask <Boolean, Integer, Boolean>{

    private static final String TAG = "Installer";
	
	final private Context mContext;
	final private Notifyer mNotifyer;
	final private Common mCommon;
	private ProgressDialog pDialog;

    final private File browserapk, chromesyncapk, busybox;
	
	private Runnable rtrue, rfalse, reloadUI;
	
	public Installer(Context mContext, Runnable reloadUI){
		this.mContext = mContext;
		this.reloadUI = reloadUI;
		mNotifyer = new Notifyer(mContext);
		mCommon = new Common();
		browserapk = new File(mContext.getFilesDir(), "Browser.apk");
		chromesyncapk = new File(mContext.getFilesDir(), "ChromeBookmarksSyncAdapter.apk");
		busybox = new File(mContext.getFilesDir(), "busybox");
	}
	
	protected void onPreExecute() {
		pDialog = new ProgressDialog(mContext);
		pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pDialog.setTitle(R.string.installing);
		pDialog.setMax(8);
		pDialog.setCancelable(false);
		pDialog.show();
        Log.i(TAG, "Preparing installation");
		try {
			mCommon.pushFileFromRAW(mContext, busybox, R.raw.busybox);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mCommon.chmod(busybox, "741");
	}

	@Override
	protected Boolean doInBackground(Boolean... options)  {
		try {
			publishProgress(R.string.mount, 1);
            Log.i(TAG, mContext.getString(R.string.mount));
			mCommon.mountDir(AOSPBrowserInstaller.SystemApps, "RW");

			publishProgress(R.string.backup, 2);
			if (!AOSPBrowserInstaller.bppapkold.exists() && AOSPBrowserInstaller.bppapk.exists()){
                Log.i(TAG, mContext.getString(R.string.backup));
				move(AOSPBrowserInstaller.bppapk, AOSPBrowserInstaller.bppapkold);
			}
			if (!AOSPBrowserInstaller.bppodexold.exists() && AOSPBrowserInstaller.bppodex.exists()) {
                Log.i(TAG, mContext.getString(R.string.backup));
				move(AOSPBrowserInstaller.bppodex, AOSPBrowserInstaller.bppodexold);
			}
			publishProgress(R.string.pushbrowser, 3);
            Log.i(TAG, mContext.getString(R.string.pushbrowser));
			copy(browserapk, AOSPBrowserInstaller.browser);
			publishProgress(R.string.setpermissions, 4);
            Log.i(TAG, mContext.getString(R.string.setpermissions));
			mCommon.chmod(AOSPBrowserInstaller.browser, "644");
			if (options[0] && !AOSPBrowserInstaller.chromesync.exists()){
				publishProgress(R.string.unpacksync, 5);
                Log.i(TAG, mContext.getString(R.string.unpacksync));
				mCommon.pushFileFromRAW(mContext, chromesyncapk, R.raw.chromebookmarkssyncadapter);
				publishProgress(R.string.pushsync, 6);
                Log.i(TAG, mContext.getString(R.string.pushsync));
				move(chromesyncapk, AOSPBrowserInstaller.chromesync);
				publishProgress(R.string.setpermissions, 7);
                Log.i(TAG, mContext.getString(R.string.setpermissions));
				mCommon.chmod(AOSPBrowserInstaller.chromesync, "644");
			}
			publishProgress(R.string.unmount, 8);
            Log.i(TAG, mContext.getString(R.string.unmount));
			mCommon.mountDir(AOSPBrowserInstaller.SystemApps, "RO");

		} catch (Exception e) {
			mNotifyer.showExceptionToast(e);
            Log.i(TAG, e.getMessage());
			return false;
		}
		return true;
	}
	
	protected void onPostExecute(Boolean result){
		pDialog.dismiss();

		if (result) {
			resetRunnables();
			rtrue = new Runnable(){

				@Override
				public void run() {
					try {
						mCommon.executeSuShell("reboot");
					} catch (Exception e) {
						mNotifyer.showExceptionToast(e);
					}
				}
			};
        	rfalse = new Runnable() {
        	    @Override
        	    public void run() {
        	        Appirater.appLaunched(mContext);
        	    }
        	};
			mNotifyer.createAlertDialog(R.string.information, R.string.completeinstallation, rtrue, null, rfalse).show();
			reloadUI.run();
		} else {
			mNotifyer.createDialog(R.string.warning, R.string.install_failed, true, false).show();
		}
    }
	
	protected void onProgressUpdate(Integer... states) {
		pDialog.setTitle(states[0]);
		pDialog.setProgress(states[1]);
	}

    public void resetRunnables(){
        rtrue = Notifyer.rEmpty;
        rfalse = Notifyer.rEmpty;
    }

	public void move(File source, File destination) throws Exception {
		mCommon.executeSuShell(busybox.getAbsolutePath() + " mv " + source.getAbsolutePath() + " " + destination.getAbsolutePath());
	}

    public void copy(File source, File destination) throws Exception {
        mCommon.executeSuShell(busybox.getAbsolutePath() + " cp " + source.getAbsolutePath() + " " + destination.getAbsolutePath());
    }
}
