package com.hookmobile.age;

import static com.hookmobile.age.AgeConstants.AGE_LOG;
import static com.hookmobile.age.AgeConstants.AGE_PREFERENCES;
import static com.hookmobile.age.AgeUtils.isEmptyStr;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

class TapjoyManager {

	private static String deviceID;			// Device ID (IMEI/MEID).
	private static String macAddress;		// Mac address.
	private static String serialID;			// Serial ID.
	
	
	static void init(Context context) {
    	Log.i(AGE_LOG, "Tapjoy SDK Version: 1.0.1");
    	
		try {
			SharedPreferences settings = context.getSharedPreferences(AGE_PREFERENCES, 0);

			try {
				TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

				if(telephonyManager != null) {
					deviceID = telephonyManager.getDeviceId();
				}

				boolean invalidDeviceID = false;
				
				if(deviceID == null) {
					Log.e(AGE_LOG, "Device id is null.");
					invalidDeviceID = true;
				}
				else if(deviceID.length() == 0
						|| deviceID.equals("000000000000000")
						|| deviceID.equals("0")) {
					Log.e(AGE_LOG, "Device id is empty or an emulator.");
					invalidDeviceID = true;
				}
				else {
					deviceID = deviceID.toLowerCase();
				}
				
				// Is this at least Android 2.3+? Then let's get the serial.
				if(Integer.parseInt(android.os.Build.VERSION.SDK) >= 9) {
					Log.i(AGE_LOG, "TRYING TO GET SERIAL OF 2.3+ DEVICE...");

					// THIS CLASS IS ONLY LOADED FOR ANDROID 2.3+
					TapjoyHardwareUtils hardware = new TapjoyHardwareUtils();
					serialID = hardware.getSerial();

					// Is there no IMEI or MEID?
					if(invalidDeviceID) {
						deviceID = serialID;
					}

					Log.i(AGE_LOG, "SERIAL: deviceID: [" + deviceID + "]");
					
					if(deviceID == null) {
						Log.e(AGE_LOG, "SERIAL: Device id is null.");
						invalidDeviceID = true;
					}
					else if(deviceID.length() == 0
							|| deviceID.equals("000000000000000")
							|| deviceID.equals("0")
							|| deviceID.equals("unknown")) {
						Log.e(AGE_LOG, "SERIAL: Device id is empty or an emulator.");
						invalidDeviceID = true;
					}
					else {
						deviceID = deviceID.toLowerCase();
						invalidDeviceID = false;
					}
				}

				// Is the device ID invalid? This is probably an emulator or pre-production device.
				if(invalidDeviceID) {
					StringBuffer buff = new StringBuffer();
					buff.append("EMULATOR");
					String deviceId = settings.getString("emulatorDeviceId", null);

					// Do we already have an emulator device id stored for this device?
					if(deviceId != null && !deviceId.equals("")) {
						deviceID = deviceId;
					}
					// Otherwise generate a deviceID for emulator testing.
					else {
						String constantChars = "1234567890abcdefghijklmnopqrstuvw";

						for(int i = 0; i < 32; i++) {
							int randomChar = (int) (Math.random() * 100);
							int ch = randomChar % 30;
							buff.append(constantChars.charAt(ch));
						}

						deviceID = buff.toString().toLowerCase();

						// Save the emulator device ID in the prefs so we can reuse it.
						SharedPreferences.Editor editor = settings.edit();
						editor.putString("emulatorDeviceId", deviceID);
						editor.commit();
					}
				}
			}
			catch(Exception e) {
				Log.e(AGE_LOG, "Error getting deviceID. e: " + e.toString());
				deviceID = null;
			}
			
			// Get mac address.
			try {
				WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

				if(wifiManager != null) {
					WifiInfo wifiInfo = wifiManager.getConnectionInfo();

					if(wifiInfo != null) {
						macAddress = wifiInfo.getMacAddress();

						if(! isEmptyStr(macAddress)) {
							macAddress = macAddress.toUpperCase();
						}
					}
				}
			}
			catch(Exception e) {
				Log.e(AGE_LOG, "Error getting device mac address: " + e.toString());
			}
			
			Log.i(AGE_LOG, "deviceID: " + deviceID);
			Log.i(AGE_LOG, "macAddress: " + macAddress);
		}
		catch(Exception e) {
			Log.e(AGE_LOG, "Error initializing Tapjoy parameters.  e=" + e.toString());
		}
	}
	
	static String getTapjoyUDID() {
    	return deviceID;
    }
	
	static String getMacAddress() {
		return macAddress;
	}
	
}
