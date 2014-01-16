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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.sufficientlysecure.rootcommands.Shell;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.mkrtchyan.utils.Common;
import de.mkrtchyan.utils.Downloader;
import de.mkrtchyan.utils.Notifyer;

public class AOSPBrowserInstaller extends Activity {

	private static final String TAG = "AOSPBrowserInstaller";
    private static final String PREF_NAME = "prefs";
    private static final String PREF_FIRST_RUN = "first_run";
    private static final String PREF_SHOW_ADS = "show_ads";

    private final Context mContext = this;
    private final Notifyer mNotifyer = new Notifyer(mContext);
    private Shell mShell;

	private static final String Device = Build.DEVICE;
    public static final File SystemApps = new File("/system/app");
    public static final File installed_browser = new File(SystemApps, "Browser.apk");
    private File downloaded_browser, apk_sums;
    public static final File chromesync = new File(SystemApps, "ChromeBookmarksSyncAdapter.apk");
    public static final File bppapk = new File(SystemApps, "BrowserProviderProxy.apk");
    public static final File bppapkold = new File(SystemApps, "BrowserProviderProxy.apk.old");
    public static final File bppodex = new File(SystemApps, "BrowserProviderProxy.odex");
    public static final File bppodexold = new File(SystemApps, "BrowserProviderProxy.odex.old");

    private boolean isBrowserInstalled = false;

	final Runnable reloadUI = new Runnable(){

		@Override
		public void run() {

			final ProgressBar pbInstallation = (ProgressBar) findViewById(R.id.pbInstallation);
			final Button bGo = (Button) findViewById(R.id.bGo);
			final TextView tvInfo = (TextView) findViewById(R.id.tvInfo);
			final ImageView ivIcon = (ImageView) findViewById(R.id.ivIcon);

            final PackageManager pm = getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            boolean isChromeSyncInstalled = false;
            for (ApplicationInfo packageInfo : packages) {
                if (packageInfo.packageName.equals("com.android.browser")) {
                    isBrowserInstalled = true;
                }
                if (packageInfo.packageName.equals("com.google.android.syncadapters.bookmarks")) {
                    isChromeSyncInstalled = !chromesync.exists();
                }
            }

            boolean installFailed = isBrowserInstalled && !installed_browser.exists();

			pbInstallation.setMax(1);
			if (isBrowserInstalled) {
				tvInfo.setText(R.string.installed);
				bGo.setText(R.string.uninstall);
				ivIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher_browser));
				pbInstallation.setProgress(1);
				if (isChromeSyncInstalled){
					tvInfo.setText(String.format(getString(R.string.with_sync_ins), getString(R.string.installed)));
				}
                findViewById(R.id.cbCyanogenmod).setVisibility(View.INVISIBLE);
			} else {
				tvInfo.setText(R.string.notinstalled);
				pbInstallation.setProgress(0);
				bGo.setText(R.string.install);
				ivIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher_browserun));
                findViewById(R.id.cbCyanogenmod).setVisibility(View.VISIBLE);
			}
            if (installFailed) {
                bGo.setText(R.string.uninstall);
            }
		}
	};
	final Runnable doWork = new Runnable() {
        @Override
        public void run() {
            if (downloaded_browser.exists()) {
                PopupMenu popup = new PopupMenu(mContext, findViewById(R.id.bGo));
                MenuInflater inflater = popup.getMenuInflater();
	            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
		            @Override
		            public boolean onMenuItemClick(MenuItem item) {
                        new Installer(mContext, mShell, reloadUI, downloaded_browser).execute(item.getItemId() == R.id.iWithSync);
                        return true;
                    }
	            });
                inflater.inflate(R.menu.install_popup, popup.getMenu());
                popup.show();
            }
        }
    };
	final Runnable download = new Runnable() {
		@Override
		public void run() {
			Downloader downloader = new Downloader(mContext, "http://dslnexus.org/Android/AOSPBrowser", downloaded_browser.getName(), downloaded_browser, doWork);
            downloader.setCancelable(true);
            downloader.setChecksumFile(apk_sums);
			downloader.execute();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.aosp_installer);

        try {
            mShell = Shell.startRootShell();
        } catch (IOException e) {
            try {
                mShell = Shell.startShell();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        apk_sums = new File(mContext.getFilesDir(), "apk_sums");
        try {
            Common.pushFileFromRAW(mContext, apk_sums, R.raw.apk_sums, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!Common.getBooleanPref(mContext, PREF_NAME, PREF_FIRST_RUN)) {
            Common.setBooleanPref(mContext, PREF_NAME, PREF_FIRST_RUN, true);
            Common.setBooleanPref(mContext, PREF_NAME, PREF_SHOW_ADS, true);
        }
		
        if (!Device.equals("grouper") && !Device.equals("mako") && !Device.equals("manta")
                && !Device.equals("tilapia") && !Device.equals("deb") && !Device.equals("flo")
                && !Device.equals("hammerhead")) {
			mNotifyer.createDialog(R.string.warning, R.string.notsupported, true, true).show();
		}

        if (!Common.getBooleanPref(mContext, PREF_NAME, PREF_SHOW_ADS)) {
            ((ViewGroup) findViewById(R.id.adView).getParent()).removeView(findViewById(R.id.adView));
        }

		reloadUI.run();

        if (!isBrowserInstalled && installed_browser.exists()) {
            AlertDialog.Builder Alert = new AlertDialog.Builder(mContext);
            Alert.setTitle(R.string.warning);

            Alert.setMessage(R.string.install_failed);
            Alert.show();
        }
	}

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.aosp_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        super.onPrepareOptionsMenu(menu);
        try {
            menu.findItem(R.id.iShowAds).setChecked(Common.getBooleanPref(mContext, PREF_NAME, PREF_SHOW_ADS));
        } catch (NullPointerException e) {
            Notifyer.showExceptionToast(mContext, TAG, e);
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.iDonate:
                startActivity(new Intent(this, DonationsActivity.class));
                return true;
            case R.id.iShowAds:
                Common.setBooleanPref(mContext, PREF_NAME, PREF_SHOW_ADS, !Common.getBooleanPref(mContext, PREF_NAME, PREF_SHOW_ADS));
                Toast
                        .makeText(mContext, R.string.please_restart, Toast.LENGTH_SHORT)
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	public void Go(View view){

        if (!Common.suRecognition() && !BuildConfig.DEBUG){
            Notifyer.showRootDeniedDialog(mContext);
        } else {
            String FileName = "";
            if (Build.VERSION.SDK_INT == 19) {
                FileName = "browser_19.apk";
            } else if (Build.VERSION.SDK_INT == 18) {
                FileName = "browser_43.apk";
            } else {
                FileName = "browser_42.apk";
            }


            if (((CheckBox) findViewById(R.id.cbCyanogenmod)).isChecked())
                FileName = "cm_browser_" + Build.VERSION.SDK_INT + ".apk";

            downloaded_browser = new File(mContext.getFilesDir(), FileName);
            if (!isBrowserInstalled) {
                if (!downloaded_browser.exists()) {
	                mNotifyer.createAlertDialog(R.string.warning, R.string.download_now, download, null, new Runnable() {
		                @Override
		                public void run() {

		                }
	                }).show();
                } else {
                    doWork.run();
                }
            } else {
                new Uninstaller(mContext, mShell, reloadUI).execute();
            }
        }
	}
}