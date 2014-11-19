package de.mkrtchyan.aospinstaller;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;

/**
 * Created by Ashot on 16.11.2014.
 */
public class Uninstaller {
    private final Activity mActivity;
    private final Context mContext;
    private final Shell mShell;
    private final Toolbox mToolbox;
    private boolean mInstallSync = false;

    public Uninstaller(Activity activity, Context context, Shell shell, Toolbox toolbox) {
        mActivity = activity;
        mContext = context;
        mShell = shell;
        mToolbox = toolbox;
    }

    public void uninstall() {
        final ProgressDialog dialog = new ProgressDialog(mContext);
        dialog.setTitle(R.string.uninstalling);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(6);
        dialog.setCancelable(false);
        dialog.show();
        Runnable uninstallRoutine = new Runnable() {
            @Override
            public void run() {
                try {
                    mToolbox.remount(AOSPBrowserInstaller.SystemApps, "RW");
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setProgress(1);
                        }
                    });
                    if (AOSPBrowserInstaller.bppapk_bak.exists())
                        AOSPBrowserInstaller.suMove(mShell, AOSPBrowserInstaller.bppapk_bak,
                                AOSPBrowserInstaller.bppapk);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setProgress(2);
                        }
                    });
                    if (AOSPBrowserInstaller.bppodex_bak.exists())
                        AOSPBrowserInstaller.suMove(mShell, AOSPBrowserInstaller.bppodex_bak,
                                AOSPBrowserInstaller.bppodex);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setProgress(3);
                        }
                    });
                    if (AOSPBrowserInstaller.installed_browser.exists())
                        AOSPBrowserInstaller.suRemove(mShell, AOSPBrowserInstaller.installed_browser);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setProgress(4);
                        }
                    });
                    if (AOSPBrowserInstaller.chromesync.exists())
                        AOSPBrowserInstaller.suRemove(mShell, AOSPBrowserInstaller.chromesync);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setProgress(5);
                        }
                    });
                    mToolbox.remount(AOSPBrowserInstaller.SystemApps, "RO");
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setProgress(6);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                });
            }
        };
        Thread uninstallThread = new Thread(uninstallRoutine, "Uninstall-Thread");
        uninstallThread.start();
    }
}
