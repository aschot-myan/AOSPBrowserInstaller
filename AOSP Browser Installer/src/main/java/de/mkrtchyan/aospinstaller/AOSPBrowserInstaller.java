package de.mkrtchyan.aospinstaller;

/**
 * Copyright (c) 2014 Ashot Mkrtchyan
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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;
import org.sufficientlysecure.rootcommands.util.FailedExecuteCommand;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.mkrtchyan.utils.Common;
import de.mkrtchyan.utils.Downloader;
import de.mkrtchyan.utils.Notifyer;

public class AOSPBrowserInstaller extends Activity {

	private static final String TAG = "AOSPBrowserInstaller";
    private static final String PREF_NAME = "prefs";
    private static final String PREF_FIRST_RUN = "first_run";
    private static final String PREF_SHOW_ADS = "show_ads";

	private final Activity mActivity = this;
    private final Context mContext = this;
    private Shell mShell;
    private Toolbox mToolbox;

	private static final String Device = Build.DEVICE;
    public static final File SystemApps = new File("/system/app");
    public static final File installed_browser = new File(SystemApps, "Browser.apk");
    public static final File chromesync = new File(SystemApps, "ChromeBookmarksSyncAdapter.apk");
    public static final File bppapk = new File(SystemApps, "BrowserProviderProxy.apk");
    public static final File bppapk_bak = new File(SystemApps, "BrowserProviderProxy.apk.bak");
    public static final File bppodex = new File(SystemApps, "BrowserProviderProxy.odex");
    public static final File bppodex_bak = new File(SystemApps, "BrowserProviderProxy.odex.bak");

	boolean installed[];
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.aosp_installer);

        try {
            mShell = Shell.startRootShell();
            mToolbox = new Toolbox(mShell);
        } catch (IOException e) {
            e.printStackTrace();
        }
		installed = checkInstallation();

        if (!Common.getBooleanPref(mContext, PREF_NAME, PREF_FIRST_RUN)) {
            Common.setBooleanPref(mContext, PREF_NAME, PREF_FIRST_RUN, true);
            Common.setBooleanPref(mContext, PREF_NAME, PREF_SHOW_ADS, true);
        }

        ImageView ivIcon = (ImageView) findViewById(R.id.ivIcon);
        TextView tvInfo = (TextView) findViewById(R.id.tvInfo);
        ProgressBar pbInstallation = (ProgressBar) findViewById(R.id.pbInstallation);
        Button bGo = (Button) findViewById(R.id.bGo);

		pbInstallation.setMax(1);
		ivIcon.setImageResource(installed[0] ? R.drawable.ic_launcher_browser : R.drawable.ic_launcher_browserun);

		pbInstallation.setProgress(installed[0] ? 1 : 0);
		if (installed[0]) {
			bGo.setText(R.string.uninstall);
			tvInfo.setText(R.string.installed);
			if (installed[1]) {
				tvInfo.setText(String.format(getString(R.string.with_sync), R.string.installed));
			}
		} else {
			bGo.setText(R.string.install);
			tvInfo.setText(R.string.notinstalled);
		}

        if (!Device.equals("grouper") && !Device.equals("mako") && !Device.equals("manta")
                && !Device.equals("tilapia") && !Device.equals("deb") && !Device.equals("flo")
                && !Device.equals("hammerhead")) {
	        Toast.makeText(mContext, R.string.notsupported, Toast.LENGTH_LONG).show();
		}

        if (!installed[0] && installed_browser.exists()) {
            AlertDialog.Builder Alert = new AlertDialog.Builder(mContext);
            Alert.setTitle(R.string.warning);
            Alert.setMessage(R.string.install_failed);
            Alert.show();
        }

        AdView ads = (AdView) findViewById(R.id.ads);
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.AOSPLayout);
        if (ads != null) {
            if (Common.getBooleanPref(mContext, PREF_NAME, PREF_SHOW_ADS)) {
                ads.loadAd(new AdRequest.Builder()
                        .addTestDevice("53B35F6E356EB90AD09B357DF092BC8F")
                        .build());
            } else {
                layout.removeView(ads);
            }
        }
	}

	public boolean[] checkInstallation() {
		PackageManager pm = getPackageManager();
		boolean[] installed = {true, true};
		try {
			pm.getPackageInfo("com.android.browser", 0);
		} catch (PackageManager.NameNotFoundException e) {
			installed[0] = false;
		}
		try {
			pm.getPackageInfo("com.google.android.syncadapters.bookmarks", 0);
		} catch (PackageManager.NameNotFoundException e) {
			installed[1] = false;
		}
		return installed;
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
                Common.setBooleanPref(mContext, PREF_NAME, PREF_SHOW_ADS,
		                !Common.getBooleanPref(mContext, PREF_NAME, PREF_SHOW_ADS));
                Toast
                        .makeText(mContext, R.string.please_restart, Toast.LENGTH_SHORT)
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	public void Go(View view){
		if (!installed[0]) {
			showBrowserDialog();
		} else {
			Uninstaller uninstaller = new Uninstaller(mActivity, mContext, mShell, mToolbox);
            uninstaller.uninstall();
		}
	}

	public void showBrowserDialog() {
		final Dialog dialog = new Dialog(mContext);
		LinearLayout lLayout = new LinearLayout(mContext);
		lLayout.setOrientation(LinearLayout.HORIZONTAL);
		ImageView AOSP = new ImageView(mContext);
		AOSP.setImageResource(R.drawable.ic_aosp);
		AOSP.setTag("aosp");
		AOSP.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showVersionOfSystem(view.getTag().toString());
				dialog.dismiss();
			}
		});
        ImageView OMNI = new ImageView(mContext);
		OMNI.setTag("omni");
		OMNI.setImageResource(R.drawable.ic_omni);
		OMNI.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showVersionOfSystem(view.getTag().toString());
				dialog.dismiss();
			}
		});
        ImageView CM = new ImageView(mContext);
		CM.setImageResource(R.drawable.ic_cm);
		CM.setTag("cm");
		CM.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showVersionOfSystem(view.getTag().toString());
				dialog.dismiss();
			}
		});
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        lLayout.addView(CM, params);
		lLayout.addView(AOSP, params);
		lLayout.addView(OMNI, params);
		dialog.setTitle("Choose Browser");
		dialog.setContentView(lLayout);
		dialog.show();
	}

	public void showVersionOfSystem(String system) {
		final ArrayList<String> fileNames = new ArrayList<String>();
		ArrayAdapter<String> descriptions = new ArrayAdapter<String>(mContext,
				android.R.layout.simple_list_item_1);
		if (system.equals("aosp")) {
			fileNames.add("browser_19.apk");
			fileNames.add("browser_18.apk");
			fileNames.add("browser_17.apk");
			fileNames.add("browser_16.apk");
			fileNames.add("browser_14.apk");
		} else if (system.equals("cm")) {
			fileNames.add("cm_browser_19.apk");
			fileNames.add("cm_browser_18.apk");
			fileNames.add("cm_browser_17.apk");
			fileNames.add("cm_browser_16.apk");
			fileNames.add("cm_browser_14.apk");
		} else if (system.equals("omni")) {
			fileNames.add("omni_browser_19.apk");
		} else {
			return;
		}
		for (String i : fileNames) {
			String description;
			if (i.contains("cm")) {
				description = "CyanogenMod Browser";
			} else if (i.contains("omni")) {
				description = "OmniRom Browser";
			} else {
				description = "AOSP Browser";
			}

			if (i.contains("14")) {
				description += " 4.0.x";
			} else if (i.contains("16")) {
				description += " 4.1.x";
			} else if (i.contains("17")) {
				description += " 4.2.x";
			} else if (i.contains("18")) {
				description += " 4.3.x";
			} else if (i.contains("19")) {
				description += " 4.4.x";
			}
			descriptions.add(description);
		}

		final Dialog dialog = new Dialog(mContext);
		dialog.setTitle(system.toUpperCase() + " Versions");
		ListView versions = new ListView(mContext);
		versions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				File localBrowser = new File(mContext.getFilesDir(), fileNames.get(i));
				if (!localBrowser.exists()) {
					Downloader downloader = new Downloader(mContext, "http://dslnexus.de/Android/AOSPBrowser",
							localBrowser.getName(), localBrowser);
					downloader.setCancelable(true);
					downloader.setOnDownloadListener(new Downloader.OnDownloadListener() {
						@Override
						public void success(File file) {
							install(file);
						}

						@Override
						public void failed(Exception e) {
							Toast.makeText(mContext, e.toString(), Toast.LENGTH_SHORT).show();
						}
					});
					downloader.setAskBeforeDownload(true);
					downloader.ask();
				} else {
					install(localBrowser);
				}
				dialog.dismiss();
			}
		});
		versions.setAdapter(descriptions);
		dialog.setContentView(versions);
		dialog.show();
	}

	public void install(final File file) {
        final Installer installer = new Installer(mActivity, mShell, mToolbox);
		AlertDialog.Builder abuilder = new AlertDialog.Builder(mContext);
		abuilder.setMessage("Use Google Bookmarks Sync?");
		abuilder.setPositiveButton(R.string.positive, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
                installer.setInstallSync(true);
                installer.install(file);
			}
		});
		abuilder.setNegativeButton(R.string.negative, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
                installer.install(file);
			}
		});
		abuilder.show();
	}

    public static void suRemove(Shell shell, File file) throws FailedExecuteCommand {
        shell.execCommand("rm " + file.getAbsolutePath());
    }

    public static void suMove(Shell shell, File src, File dest) throws FailedExecuteCommand {
        shell.execCommand("mv " + src.getAbsolutePath() + " " + dest.getAbsolutePath());
    }
}