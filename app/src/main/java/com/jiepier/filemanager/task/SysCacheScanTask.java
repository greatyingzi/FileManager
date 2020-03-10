package com.jiepier.filemanager.task;

import android.annotation.SuppressLint;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.AsyncTask;
import android.os.Build;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;


import com.jiepier.filemanager.R;
import com.jiepier.filemanager.base.App;
import com.jiepier.filemanager.bean.JunkInfo;
import com.jiepier.filemanager.task.callback.ISysScanCallBack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;


/**
 * Created by panruijie on 2017/2/15.
 * Email : zquprj@gmail.com
 */

public class SysCacheScanTask extends AsyncTask<Void, Void, Void> {

	private ISysScanCallBack mCallBack;
	private int mScanCount;
	private int mTotalCount;
	private ArrayList<JunkInfo> mSysCaches;
	private HashMap<String, String> mAppNames;
	private long mTotalSize = 0L;
	private boolean mIsOverTime = true;

	StorageStatsManager storageStatsManager = null;


	public SysCacheScanTask(ISysScanCallBack callBack) {
		this.mCallBack = callBack;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			storageStatsManager = (StorageStatsManager) App.getAppContext().getSystemService(Context.STORAGE_STATS_SERVICE);
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		Observable.timer(30 * 1000, TimeUnit.SECONDS)
				.subscribe(aLong -> {
					if (mIsOverTime) {
						mCallBack.onOverTime();
					}
				});
	}

	@Override
	protected Void doInBackground(Void... params) {
		mCallBack.onBegin();

		if (isCancelled()) {
			mCallBack.onCancel();
			return null;
		}

		PackageManager pm = App.getAppContext().getPackageManager();
		@SuppressLint("WrongConstant") List<ApplicationInfo> installedPackages = pm.getInstalledApplications(PackageManager.GET_GIDS);

		IPackageStatsObserver.Stub observer = new PackageStatsObserver();
		mTotalCount = installedPackages.size();
		mSysCaches = new ArrayList<>();
		mAppNames = new HashMap<>();

		for (int i = 0; i < mTotalCount; i++) {
			ApplicationInfo info = installedPackages.get(i);
			mAppNames.put(info.packageName, pm.getApplicationLabel(info).toString());
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
				getPackageInfo26(info.packageName, observer);
			else {
				getPackageInfo(info);
			}
		}

		mIsOverTime = false;
		return null;
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void getPackageInfo(ApplicationInfo appInfo) {
		try {
			ApplicationInfo ai = App.getAppContext().getPackageManager().getApplicationInfo(appInfo.packageName, 0);
			StorageStats storageStats = storageStatsManager.queryStatsForUid(ai.storageUuid, appInfo.uid);
			mScanCount++;
			if (storageStats != null) {
				JunkInfo info = new JunkInfo();
				info.setPackageName(appInfo.packageName)
						.setName(appInfo.packageName)
						.setSize(storageStats.getCacheBytes() + storageStats.getDataBytes());

				if (info.getSize() > 0) {
					mSysCaches.add(info);
					mTotalSize += info.getSize();
				}
				mCallBack.onProgress(info);
			}

			if (mScanCount == mTotalCount) {
				JunkInfo junkInfo = new JunkInfo();
				junkInfo.setName(App.getAppContext().getString(R.string.system_cache))
						.setSize(mTotalSize)
						.setChildren(mSysCaches)
						.setVisible(true)
						.setChild(false)
						.isCheck(false);

				/*Collections.sort(mSysCaches);
				Collections.reverse(mSysCaches);*/

				ArrayList<JunkInfo> list = new ArrayList<>();
				list.add(junkInfo);
				mCallBack.onFinish(list);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//api 小于26的系统使用
	private void getPackageInfo26(String packageName, IPackageStatsObserver.Stub observer) {

		if (isCancelled()) {
			return;
		}
		try {
			PackageManager pm = App.getAppContext().getPackageManager();
			Method getPackageSizeInfo = pm.getClass()
					.getMethod("getPackageSizeInfo", String.class, IPackageStatsObserver.class);
			getPackageSizeInfo.setAccessible(true);
			getPackageSizeInfo.invoke(pm, packageName, observer);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private class PackageStatsObserver extends IPackageStatsObserver.Stub {

		@Override
		public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) throws RemoteException {
			mScanCount++;
			if (succeeded && pStats != null) {
				JunkInfo info = new JunkInfo();
				info.setPackageName(pStats.packageName)
						.setName(pStats.packageName)
						.setSize(pStats.cacheSize + pStats.externalCacheSize);

				if (info.getSize() > 0) {
					mSysCaches.add(info);
					mTotalSize += info.getSize();
				}
				mCallBack.onProgress(info);
			}

			if (mScanCount == mTotalCount) {
				JunkInfo junkInfo = new JunkInfo();
				junkInfo.setName(App.getAppContext().getString(R.string.system_cache))
						.setSize(mTotalSize)
						.setChildren(mSysCaches)
						.setVisible(true)
						.setChild(false)
						.isCheck(false);

				/*Collections.sort(mSysCaches);
				Collections.reverse(mSysCaches);*/

				ArrayList<JunkInfo> list = new ArrayList<>();
				list.add(junkInfo);
				mCallBack.onFinish(list);
			}
		}
	}
}
