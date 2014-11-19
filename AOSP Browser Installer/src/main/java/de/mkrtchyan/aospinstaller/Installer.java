package de.mkrtchyan.aospinstaller;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;

import java.io.File;

import de.mkrtchyan.utils.Common;

/**
 * Created by Ashot on 16.11.2014.
 */
public class Installer {
    private final Activity mActivity;
    private final Context mContext;
    private final Shell mShell;
    private final Toolbox mToolbox;
    private boolean mInstallSync = false;
    public Installer(Activity activity, Shell shell, Toolbox toolbox) {
        mActivity = activity;
        mContext = activity;
        mShell = shell;
        mToolbox = toolbox;
    }

    public void setInstallSync(boolean installSync) {
        mInstallSync = installSync;
    }

    public void install(final File APK) {
        final ProgressDialog dialog = new ProgressDialog(mContext);
        dialog.setTitle(R.string.installing);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(7);
        dialog.setCancelable(false);
        dialog.show();
        Runnable installRoutine = new Runnable() {
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
                    if (AOSPBrowserInstaller.bppapk.exists())
                        AOSPBrowserInstaller.suMove(mShell, AOSPBrowserInstaller.bppapk,
                                AOSPBrowserInstaller.bppapk_bak);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setProgress(2);
                        }
                    });
                    if (AOSPBrowserInstaller.bppodex.exists())
                        AOSPBrowserInstaller.suMove(mShell, AOSPBrowserInstaller.bppodex,
                                AOSPBrowserInstaller.bppodex_bak);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setProgress(3);
                        }
                    });
                    if (APK.exists())
                        mToolbox.copyFile(APK, AOSPBrowserInstaller.installed_browser, false, false);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setProgress(4);
                        }
                    });
                    if (mInstallSync) {
                        File localChromeSync = new File(mContext.getFilesDir(),
                                AOSPBrowserInstaller.chromesync.getName());
                        Common.pushFileFromRAW(mContext, localChromeSync,
                                R.raw.chromebookmarkssyncadapter, false);
                        mToolbox.copyFile(localChromeSync, AOSPBrowserInstaller.chromesync, false,
                                false);
                        mToolbox.setFilePermissions(AOSPBrowserInstaller.chromesync, "644");

                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.setProgress(5);
                            }
                        });
                    }
                    mToolbox.setFilePermissions(AOSPBrowserInstaller.installed_browser, "644");
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setProgress(6);
                        }
                    });
                    mToolbox.remount(AOSPBrowserInstaller.SystemApps, "RO");
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setProgress(7);
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
        Thread installThread = new Thread(installRoutine, "Install-Thread");
        installThread.start();
    }
}
