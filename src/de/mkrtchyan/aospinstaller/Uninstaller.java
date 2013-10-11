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

import com.devspark.appmsg.AppMsg;
import com.sbstrm.appirater.Appirater;

import java.io.File;

import de.mkrtchyan.utils.Common;
import de.mkrtchyan.utils.Notifyer;

public class Uninstaller extends AsyncTask <Void, Integer, Boolean>{

    public static final String TAG = "Uninstaller";

	final private Context mContext;
	final private Notifyer mNotifyer;
	final private Common mCommon = new Common();
	private ProgressDialog pDialog;
	final private File busybox;
	
	private Runnable rtrue, rfalse, reloadUI;

	
	public Uninstaller(Context mContext, Runnable reloadUI){
		this.mContext = mContext;
		this.reloadUI = reloadUI;
		mNotifyer = new Notifyer(mContext);
		busybox = new File(mContext.getFilesDir(), "busybox");
	}
	
	protected void onPreExecute(){
		pDialog = new ProgressDialog(mContext);
		pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pDialog.setTitle(R.string.uninstalling);
		pDialog.setMax(4);
		pDialog.setCancelable(false);
		pDialog.show();
        Log.i(TAG, "Preparing uninstall");
		try {
			mCommon.pushFileFromRAW(mContext, busybox, R.raw.busybox);
		} catch (Exception e) {
			e.printStackTrace();
		}
        mCommon.chmod(busybox, "741");
	}

	@Override
	protected Boolean doInBackground(Void... options) {
		try {
            publishProgress(R.string.mount, 1);
            Log.i(TAG, mContext.getString(R.string.mount));
            mCommon.mountDir(AOSPBrowserInstaller.SystemApps, "RW");
            publishProgress(R.string.restore, 2);
            Log.i(TAG, mContext.getString(R.string.restore));
            if (AOSPBrowserInstaller.bppapkold.exists() && !AOSPBrowserInstaller.bppapk.exists()) {
                move(AOSPBrowserInstaller.bppapkold, AOSPBrowserInstaller.bppapk);
            }
            if (AOSPBrowserInstaller.bppodexold.exists() && !AOSPBrowserInstaller.bppodex.exists()){
                move(AOSPBrowserInstaller.bppodexold, AOSPBrowserInstaller.bppodex);
            }
            publishProgress(R.string.clean, 3);
            Log.i(TAG, mContext.getString(R.string.clean));
            mCommon.executeSuShell("rm " + AOSPBrowserInstaller.browser.getAbsolutePath());
            if (AOSPBrowserInstaller.chromesync.exists()){
                mCommon.executeSuShell("rm " + AOSPBrowserInstaller.chromesync.getAbsolutePath());
            }
            publishProgress(R.string.unmount, 4);
            Log.i(TAG, mContext.getString(R.string.unmount));
            mCommon.mountDir(AOSPBrowserInstaller.SystemApps, "RO");
        } catch (Exception e) {
            mNotifyer.showExceptionToast(e);
            Log.i(TAG, e.getMessage());
			return false;
        }
		return true;
	}

    public void resetRunnables(){
        rtrue = Notifyer.rEmpty;
        rfalse = Notifyer.rEmpty;
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
			mNotifyer.createAlertDialog(R.string.information, R.string.completeuninstallation, rtrue, null, rfalse).show();
			reloadUI.run();
		} else {
			mNotifyer.showToast(R.string.uninstall_failed, AppMsg.STYLE_ALERT);
		}
    }

	protected void onProgressUpdate(Integer... states) {
		pDialog.setTitle(states[0]);
		pDialog.setProgress(states[1]);
	}

	public void move(File source, File destination) throws Exception {
		mCommon.executeSuShell(busybox.getAbsolutePath() + " mv " + source.getAbsolutePath() + " " + destination.getAbsolutePath());
	}

}