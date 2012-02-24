/*
 * Copyright (C) 2007-2012 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.ui.android.library;

import java.lang.reflect.*;

import java.io.*;
import java.util.*;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.*;
import android.os.PowerManager;

import android.net.Uri;

import android.app.AlertDialog;
import android.content.DialogInterface;

import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;

import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.application.ZLAndroidApplicationWindow;

import org.geometerplus.fbreader.formats.*;
import org.geometerplus.fbreader.Paths;

import android.util.Log;

public abstract class ZLAndroidActivity extends Activity {
	public static class FileOpener {
		private final Activity myActivity;

		public FileOpener(Activity activity) {
			myActivity = activity;
		}

		private void showErrorDialog(final String errName) {
			myActivity.runOnUiThread(new Runnable() {
				public void run() {
					final String title = ZLResource.resource("errorMessage").getResource(errName).getValue();
					new AlertDialog.Builder(myActivity)
						.setTitle(title)
						.setIcon(0)
						.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						})
						.create().show();
					}
			});
		}

		public void openFile(String extension, ZLFile f, String appData) {
			Uri uri = null;
			if (f.getPath().contains(":")) {

				try {
					String filepath = f.getPath();
					int p1 = filepath.lastIndexOf(":");
					String filename = filepath.substring(p1 + 1);
//					p1 = filename.lastIndexOf(".");
//					filename = filename.substring(0, p1);
//					File tmpfile = File.createTempFile(filename, "." + extension);
					final File dirFile = new File(Paths.TempDirectoryOption().getValue());
					dirFile.mkdirs();
					String path = Paths.TempDirectoryOption().getValue() + "/" + filename;
					OutputStream out = new FileOutputStream(Paths.TempDirectoryOption().getValue() + "/" + filename);

					int read = 0;
					byte[] bytes = new byte[1024];
					InputStream inp = f.getInputStream();

					while ((read = inp.read(bytes)) > 0) {
						out.write(bytes, 0, read);
					}
					out.flush();
					out.close();
					uri = Uri.parse("file://" + path);
				} catch (IOException e) {
					showErrorDialog("unzipFailed");
					return;
				}
			} else {
				uri = Uri.parse("file://" + f.getPath());
			}
			Intent LaunchIntent = new Intent(Intent.ACTION_VIEW);
			LaunchIntent.setPackage(appData);
			LaunchIntent.setData(uri);
			if (BigMimeTypeMap.getTypes(extension) != null) {
				for (String type : BigMimeTypeMap.getTypes(extension)) {
					LaunchIntent.setDataAndType(uri, type);
					try {
						myActivity.startActivity(LaunchIntent);
						return;
					} catch (ActivityNotFoundException e) {
					}
				}
				showErrorDialog("externalNotFound");
				return;
			}
			try {
				myActivity.startActivity(LaunchIntent);
			} catch (ActivityNotFoundException e) {
				showErrorDialog("externalNotFound");
			}
			return;
		}
	}

	protected abstract ZLApplication createApplication();

	private static final String REQUESTED_ORIENTATION_KEY = "org.geometerplus.zlibrary.ui.android.library.androidActiviy.RequestedOrientation";
	private static final String ORIENTATION_CHANGE_COUNTER_KEY = "org.geometerplus.zlibrary.ui.android.library.androidActiviy.ChangeCounter";

	protected final FileOpener myFileOpener = new FileOpener(this);

	private void setScreenBrightnessAuto() {
		final WindowManager.LayoutParams attrs = getWindow().getAttributes();
		attrs.screenBrightness = -1.0f;
		getWindow().setAttributes(attrs);
	}

	final void setScreenBrightness(int percent) {
		if (percent < 1) {
			percent = 1;
		} else if (percent > 100) {
			percent = 100;
		}
		final WindowManager.LayoutParams attrs = getWindow().getAttributes();
		attrs.screenBrightness = percent / 100.0f;
		getWindow().setAttributes(attrs);
		getLibrary().ScreenBrightnessLevelOption.setValue(percent);
	}

	final int getScreenBrightness() {
		final int level = (int)(100 * getWindow().getAttributes().screenBrightness);
		return (level >= 0) ? level : 50;
	}

	private void setButtonLight(boolean enabled) {
		try {
			final WindowManager.LayoutParams attrs = getWindow().getAttributes();
			final Class<?> cls = attrs.getClass();
			final Field fld = cls.getField("buttonBrightness");
			if (fld != null && "float".equals(fld.getType().toString())) {
				fld.setFloat(attrs, enabled ? -1.0f : 0.0f);
				getWindow().setAttributes(attrs);
			}
		} catch (NoSuchFieldException e) {
		} catch (IllegalAccessException e) {
		}
	}

	protected abstract ZLFile fileFromIntent(Intent intent);

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);

		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		getLibrary().setActivity(this);

		final ZLAndroidApplication androidApplication = (ZLAndroidApplication)getApplication();
		if (androidApplication.myMainWindow == null) {
			final ZLApplication application = createApplication();
			androidApplication.myMainWindow = new ZLAndroidApplicationWindow(application);
			application.initWindow();
		} else {
			final ZLFile fileToOpen = fileFromIntent(getIntent());
			if (fileToOpen != null) {
				final FormatPlugin plugin = PluginCollection.Instance().getPlugin(fileToOpen);
				if (plugin.type() != FormatPlugin.Type.EXTERNAL) {
					new Thread() {
						public void run() {
							ZLApplication.Instance().openFile(fileToOpen);
							ZLApplication.Instance().getViewWidget().repaint();
						}
					}.start();
				}
			}
		}

		ZLApplication.Instance().getViewWidget().repaint();
	}

	private PowerManager.WakeLock myWakeLock;
	private boolean myWakeLockToCreate;
	private boolean myStartTimer;

	public final void createWakeLock() {
		if (myWakeLockToCreate) {
			synchronized (this) {
				if (myWakeLockToCreate) {
					myWakeLockToCreate = false;
					myWakeLock =
						((PowerManager)getSystemService(POWER_SERVICE)).
							newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "FBReader");
					myWakeLock.acquire();
				}
			}
		}
		if (myStartTimer) {
			ZLApplication.Instance().startTimer();
			myStartTimer = false;
		}
	}

	private final void switchWakeLock(boolean on) {
		if (on) {
			if (myWakeLock == null) {
				myWakeLockToCreate = true;
			}
		} else {
			if (myWakeLock != null) {
				synchronized (this) {
					if (myWakeLock != null) {
						myWakeLock.release();
						myWakeLock = null;
					}
				}
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		switchWakeLock(
			getLibrary().BatteryLevelToTurnScreenOffOption.getValue() <
			ZLApplication.Instance().getBatteryLevel()
		);
		myStartTimer = true;
		final int brightnessLevel =
			getLibrary().ScreenBrightnessLevelOption.getValue();
		if (brightnessLevel != 0) {
			setScreenBrightness(brightnessLevel);
		} else {
			setScreenBrightnessAuto();
		}
		if (getLibrary().DisableButtonLightsOption.getValue()) {
			setButtonLight(false);
		}

		registerReceiver(myBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	}

	@Override
	public void onPause() {
		unregisterReceiver(myBatteryInfoReceiver);
		ZLApplication.Instance().stopTimer();
		switchWakeLock(false);
		if (getLibrary().DisableButtonLightsOption.getValue()) {
			setButtonLight(true);
		}
		ZLApplication.Instance().onWindowClosing();
		super.onPause();
	}

	@Override
	public void onLowMemory() {
		ZLApplication.Instance().onWindowClosing();
		super.onLowMemory();
	}

	protected abstract void processFile(ZLFile f);

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		ZLFile fileToOpen = fileFromIntent(intent);
		if (fileToOpen == null) return;
		if (fileToOpen.isArchive() && fileToOpen.getPath().endsWith(".fb2.zip")) {
			final List<ZLFile> children = fileToOpen.children();
			if (children.size() == 1) {
				final ZLFile child = children.get(0);
				if (child.getPath().endsWith(".fb2")) {
					fileToOpen = child;
				}
			} 
		}
		final FormatPlugin plugin = PluginCollection.Instance().getPlugin(fileToOpen);
		if (plugin.type() != FormatPlugin.Type.EXTERNAL) {
			ZLApplication.Instance().openFile(fileToOpen);
		} else {
			final CustomPlugin p = (CustomPlugin)plugin;
			processFile(fileToOpen);
			myFileOpener.openFile(p.supportedFileType(), fileToOpen, p.getPackage());
		}
	}

	private static ZLAndroidLibrary getLibrary() {
		return (ZLAndroidLibrary)ZLAndroidLibrary.Instance();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		View view = findViewById(R.id.main_view);
		return ((view != null) && view.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		View view = findViewById(R.id.main_view);
		return ((view != null) && view.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event);
	}

	BroadcastReceiver myBatteryInfoReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			final int level = intent.getIntExtra("level", 100);
			final ZLAndroidApplication application = (ZLAndroidApplication)getApplication();
			application.myMainWindow.setBatteryLevel(level);
			switchWakeLock(
				getLibrary().BatteryLevelToTurnScreenOffOption.getValue() < level
			);
		}
	};

}
