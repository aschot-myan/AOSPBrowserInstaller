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

import org.rootcommands.util.RootAccessDeniedException;

import java.io.File;

import de.mkrtchyan.utils.Common;
import de.mkrtchyan.utils.Notifyer;

public class Uninstaller extends AsyncTask <Void, Integer, Void>{

	Context mContext;
	Notifyer mNotifyer;
	Common mCommon = new Common();
	ProgressDialog pDialog;
	File busybox;
	
	Runnable rtrue, rneutral, rfalse, reloadUI;

	
	public Uninstaller(Context mContext, Runnable reloadUI){
		this.mContext = mContext;
		this.reloadUI = reloadUI;
		mNotifyer = new Notifyer(mContext);
		busybox = new File(mContext.getFilesDir(), "busybox");
	}
	
	protected void onPreExecute(){
		resetRunnables();
		pDialog = new ProgressDialog(mContext);
		pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pDialog.setTitle(R.string.uninstaller);
		pDialog.setMax(4);
		pDialog.setCancelable(false);
		pDialog.show();
		mCommon.pushFileFromRAW(mContext, busybox, R.raw.busybox);
	}

	@Override
	protected Void doInBackground(Void... options) {
		try {
            publishProgress(R.string.mount, 1);
            mCommon.mountDir(AOSPBrowserInstaller.SystemApps, "RW");
            publishProgress(R.string.restore, 2);
            if (AOSPBrowserInstaller.bppapkold.exists() && !AOSPBrowserInstaller.bppapk.exists()) {
                copy(AOSPBrowserInstaller.bppapkold, AOSPBrowserInstaller.bppapk);
            }
            if (AOSPBrowserInstaller.bppodexold.exists() && !AOSPBrowserInstaller.bppodex.exists()){
                copy(AOSPBrowserInstaller.bppodexold, AOSPBrowserInstaller.bppodex);
            }
            publishProgress(R.string.clean, 3);
            mCommon.executeSuShell("rm " + AOSPBrowserInstaller.browser.getAbsolutePath());
            if (AOSPBrowserInstaller.chromesync.exists()){
                mCommon.executeSuShell("rm " + AOSPBrowserInstaller.chromesync.getAbsolutePath());
            }
            publishProgress(R.string.unmount, 4);
            mCommon.mountDir(AOSPBrowserInstaller.SystemApps, "RO");
        } catch (RootAccessDeniedException e) {
            mNotifyer.showExceptionToast(e);
        }
		return null;
	}

    public void resetRunnables(){
        rtrue = Notifyer.rEmpty;
        rneutral = Notifyer.rEmpty;
        rfalse = Notifyer.rEmpty;
    }
	
	protected void onPostExecute(Void result){
		pDialog.dismiss();
		resetRunnables();
		rtrue = new Runnable(){
			
			@Override
			public void run() {
                try {
				    mCommon.executeSuShell("reboot");
                } catch (RootAccessDeniedException e) {
                    mNotifyer.showExceptionToast(e);
                }
			}
		};
		mNotifyer.createAlertDialog(R.string.information, R.string.completeuninstallation, rtrue, null, rfalse).show();
		reloadUI.run();
	}

	protected void onProgressUpdate(Integer... states) {
		pDialog.setTitle(states[0]);
		pDialog.setProgress(states[1]);
	}

	public void copy(File source, File destination) throws RootAccessDeniedException {
		mCommon.executeSuShell(busybox.getAbsolutePath() + " mv " + source.getAbsolutePath() + " " + destination.getAbsolutePath());
	}

}