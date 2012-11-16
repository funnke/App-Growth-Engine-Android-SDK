package com.hookmobile.age;

import static com.hookmobile.age.AgeConstants.AGE_LOG;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

/**
 * Broadcast Receiver for listening to com.android.vending.INSTALL_REFERRER intent.
 * 
 * @author kirktsai
 *
 */
public class AgeBroadcast extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		invokeOtherBroadcastReceiver(context, intent);

		workaroundForAndroidSecurityIssue(intent);

		// new extract referr query string and save it
		if (intent.getAction().equals("com.android.vending.INSTALL_REFERRER")) {
			String referrer = intent.getStringExtra("referrer");
			Log.d("onReceive - referrer url string:", referrer);
			if (referrer != null && referrer.length() > 0) {
				try {
					referrer = URLDecoder.decode(referrer, "US-ASCII");
					storeInstallReferrer(context, referrer);
				} catch (UnsupportedEncodingException e) {
					return;
				}
			}
		}
	}

	/**
	 * Workaround for Android security issue: http://code.google.com/p/android/issues/detail?id=16006
	 */
	private static void workaroundForAndroidSecurityIssue(Intent intent) {
        try
        {
            final Bundle extras = intent.getExtras();
            if (extras != null) {
                extras.containsKey(null);
            }
        }
        catch (final Exception e) {
            return;
        }
	}

	/**
	 * Invoke other broadcast receiver registered with this app so they receive
	 * all the app install and referrer data passed from Google Play Store. Please
	 * read AGE developer guide for more details.
	 * 
	 * @param context
	 * @param intent
	 */
	private static void invokeOtherBroadcastReceiver(Context context, Intent intent) {
		try {
			ActivityInfo receiverInfo;
			receiverInfo = context.getPackageManager().getReceiverInfo(
					new ComponentName(context, AgeBroadcast.class),
					PackageManager.GET_META_DATA);

			Bundle bundle = receiverInfo.metaData;
			if (bundle != null) {
				String packageName = bundle.getString("packageName");
				if (packageName != null && packageName.length() != 0) {
					Log.d("invokeOtherBroadcastReceiver - Loading other broadcast receiver package:", packageName);

					// try to get instance of "other broadcast receiver" and send notification
					((BroadcastReceiver) Class.forName(packageName).newInstance())
					.onReceive(context, intent);
				}
			} else {
				Log.i(AGE_LOG, "invokeOtherBroadcastReceiver - No other broadcast receiver package defined");
			}
		} catch (NameNotFoundException e1) {
			Log.w(AGE_LOG, "invokeOtherBroadcastReceiver - Unable to locate meta data for AgeBroadcast class");
		} catch (InstantiationException e) {
			Log.w(AGE_LOG, "invokeOtherBroadcastReceiver - Unable to load other broadcast receiver", e);
		} catch (IllegalAccessException e) {
			Log.w(AGE_LOG, "invokeOtherBroadcastReceiver - Unable to load other broadcast receiver", e);
		} catch (ClassNotFoundException e) {
			Log.w(AGE_LOG, "invokeOtherBroadcastReceiver - Unable to load other broadcast receiver", e);
		}

	}
	
	/**
	 * Save install referrer query string into app for later retrieval
	 * @param context
	 * @param referrer
	 */
	private static void storeInstallReferrer(Context context, String referrer) {
		SharedPreferences storage = context.getSharedPreferences(
				"AgeConstants.AGE_PREFERENCES", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = storage.edit();
		editor.putString(AgeConstants.P_INSTALL_REFERRER, referrer);
		Log.d("storeInstallReferrer - save key:" + AgeConstants.P_INSTALL_REFERRER + " value:", referrer);
		editor.commit();
	}

}