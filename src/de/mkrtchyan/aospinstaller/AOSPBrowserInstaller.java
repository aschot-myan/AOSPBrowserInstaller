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

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.rootcommands.util.Log;

import java.io.File;

import de.mkrtchyan.utils.Common;
import de.mkrtchyan.utils.Notifyer;

public class AOSPBrowserInstaller extends Activity {

	private static final String TAG = "AOSPBrowserInstaller";
    private final Context mContext = this;
    private final Notifyer mNotifyer = new Notifyer(mContext);
    private final Common mCommon = new Common();

	private static final String Device = Build.DEVICE;
	private boolean firststart = true;
    public static final File SystemApps = new File("/system/app");
    public static final File browser = new File(SystemApps, "Browser.apk");
    public static final File chromesync = new File(SystemApps, "ChromeBookmarksSyncAdapter.apk");
    public static final File bppapk = new File(SystemApps, "BrowserProviderProxy.apk");
    public static final File bppapkold = new File(SystemApps, "BrowserProviderProxy.apk.old");
    public static final File bppodex = new File(SystemApps, "BrowserProviderProxy.odex");
    public static final File bppodexold = new File(SystemApps, "BrowserProviderProxy.odex.old");
	final Runnable reloadUI = new Runnable(){

		@Override
		public void run() {

			final ProgressBar pbInstallation = (ProgressBar) findViewById(R.id.pbInstallation);
			final Button bGo = (Button) findViewById(R.id.bGo);
			final TextView tvInfo = (TextView) findViewById(R.id.tvInfo);
			final ImageView ivIcon = (ImageView) findViewById(R.id.ivIcon);

			pbInstallation.setMax(1);
			if (browser.exists()) {
				tvInfo.setText(R.string.installed);
				bGo.setText(R.string.uninstall);
				ivIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher_browser));
				pbInstallation.setProgress(1);
				if (chromesync.exists()){
					tvInfo.setText(String.format(mContext.getString(R.string.with_sync), mContext.getText(R.string.installed)));
				}
			} else {
				tvInfo.setText(R.string.notinstalled);
				pbInstallation.setProgress(0);
				bGo.setText(R.string.install);
				ivIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher_browserun));
			}
		}
	};;
	Runnable rtrue, rneutral, rfalse;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		Log.i(TAG, "started");

		if (firststart){
			if (!mCommon.suRecognition()){
				mNotifyer.showRootDeniedDialog();
			}
			
			resetRunnables();
			
			firststart = false;
		
	        if (!Device.equals("grouper") && !Device.equals("mako") && !Device.equals("manta") && !Device.equals("tilapia")) {
				mNotifyer.createDialog(R.string.warning, R.string.notsupported, true, true).show();
			}
            firststart = false;
		}

		reloadUI.run();
	}
	
	
	public void Go(View view){

        if (!browser.exists()) {
		    rtrue = new Runnable(){

			@Override
			public void run() {
				new Installer(mContext, reloadUI).execute(true);
			}
		};
		    rneutral = new Runnable(){

			@Override
			public void run() {
				new Installer(mContext, reloadUI).execute(false);
			}
		};
		    mNotifyer.createAlertDialog(R.string.option, R.string.addsync, rtrue, rneutral, rfalse).show();
        } else {
            new Uninstaller(mContext, reloadUI).execute();
        }
	}

	public void resetRunnables(){

        rtrue = Notifyer.rEmpty;
		rneutral = Notifyer.rEmpty;
		rfalse = Notifyer.rEmpty;
	}
}
