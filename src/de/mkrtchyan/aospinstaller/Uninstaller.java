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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import de.mkrtchyan.utils.Common;
import de.mkrtchyan.utils.Notifyer;

public class Uninstaller extends AsyncTask <Void, Integer, Boolean>{

    public static final String TAG = "Uninstaller";

	final private Context mContext;
	final private Notifyer mNotifyer;
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
			Common.pushFileFromRAW(mContext, busybox, R.raw.busybox, true);
            Common.chmod(busybox, "741");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected Boolean doInBackground(Void... options) {
		try {
            publishProgress(R.string.mount, 1);
            Log.i(TAG, mContext.getString(R.string.mount));
            Common.mountDir(AOSPBrowserInstaller.SystemApps, "RW");
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
            Common.executeSuShell("rm " + AOSPBrowserInstaller.installed_browser.getAbsolutePath());
            if (AOSPBrowserInstaller.chromesync.exists()){
                Common.executeSuShell("rm " + AOSPBrowserInstaller.chromesync.getAbsolutePath());
            }
            publishProgress(R.string.unmount, 4);
            Log.i(TAG, mContext.getString(R.string.unmount));
            Common.mountDir(AOSPBrowserInstaller.SystemApps, "RO");
        } catch (Exception e) {
            Notifyer.showExceptionToast(mContext, TAG, e);
            Log.i(TAG, e.getMessage());
			return false;
        }
		return true;
	}

    public void resetRunnables(){
        rtrue = new Runnable() {
            @Override
            public void run() {

            }
        };
        rfalse = new Runnable() {
            @Override
            public void run() {

            }
        };
    }
	
	protected void onPostExecute(Boolean result){
		pDialog.dismiss();
		if (result) {
			resetRunnables();
			rtrue = new Runnable(){

				@Override
				public void run() {
        	        try {
					    Common.executeSuShell("reboot");
        	        } catch (Exception e) {
                        Notifyer.showExceptionToast(mContext, TAG, e);
        	        }
				}
			};
        	rfalse = new Runnable() {
        	    @Override
        	    public void run() {
                    Notifyer.showAppRateDialog(mContext);
                    reloadUI.run();
        	    }
        	};
			mNotifyer.createAlertDialog(R.string.information, R.string.completeuninstallation, rtrue, null, rfalse).show();
			reloadUI.run();
		} else {
			Toast
                    .makeText(mContext, R.string.uninstall_process_failed, Toast.LENGTH_SHORT)
                    .show();
		}
    }

	protected void onProgressUpdate(Integer... states) {
		pDialog.setTitle(states[0]);
		pDialog.setProgress(states[1]);
	}

	public void move(File source, File destination) throws Exception {
		Common.executeSuShell(busybox.getAbsolutePath() + " mv " + source.getAbsolutePath() + " " + destination.getAbsolutePath());
	}

}