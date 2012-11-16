package com.hookmobile.age;

import static com.hookmobile.age.AgeUtils.normalizePhone;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

class AgeHelper {
	
	static final String AGE_PREFERENCES         = "age_preferences";
	static final String AGE_LOG                 = "Hook";
	static final String AGE_CURRENT_APP_KEY     = "current_app_key";
	static final String AGE_LAST_PHONE_COUNT    = "last_phone_count";
	
	private static final String P_STATUS				= "status";
	private static final String P_DESCRIPTION			= "desc";
	
	private static final int timeoutConnection	= 100000;
	private static final int timeoutSocket		= 200000;

	private static String deviceID;			// Device ID (IMEI/MEID).
	private static String sha2DeviceID;		// SHA-256 of DeviceID.
	private static String macAddress;		// Mac address.
	private static String serialID;			// Serial ID.
	
	
    static AgeResponse doPost(String url, List<NameValuePair> form) throws ClientProtocolException, IOException, JSONException {
		AgeResponse result = new AgeResponse();
		
		BasicHttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, timeoutConnection);
		HttpConnectionParams.setSoTimeout(params, timeoutSocket);
		
		HttpClient client = new DefaultHttpClient(params);
		HttpPost post = new HttpPost(url);
		post.setEntity(new UrlEncodedFormEntity(form, HTTP.UTF_8));
		post.addHeader("Content-type", "application/x-www-form-urlencoded");
		
		HttpResponse response = client.execute(post);
		
		if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			String responseText = new String(EntityUtils.toByteArray(response.getEntity()));
			JSONObject json = new JSONObject(responseText);
			result.setJson(json);
			result.setCode(json.isNull(P_STATUS) ? -1 : json.getInt(P_STATUS));
			result.setMessage(json.isNull(P_DESCRIPTION) ? null : json.getString(P_DESCRIPTION));
		}
		else {
			result.setMessage(response.getStatusLine().getReasonPhrase());
		}
		
		return result;
	}
	
    static boolean isEmptyStr(String str) {
    	if(str == null || str.length() == 0) {
    		return true;
    	}
    	
    	return false;
	}
    
    static String convertToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();

		for(int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;

			do {
				if((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char)('0' + halfbyte));
				else
					buf.append((char)('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			}
			while(two_halfs++ < 1);
		}

		return buf.toString();
	}

	static String sha256(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
    	return hashAlgorithm("SHA-256", text);
    }

    private static String hashAlgorithm(String hash, String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
    	MessageDigest md = MessageDigest.getInstance(hash);
    	md.update(text.getBytes("iso-8859-1"), 0, text.length());
    	
    	return convertToHex(md.digest());
    }

    static String queryDevicePhone(Context context) {
    	TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE); 
    	
    	return normalizePhone(manager.getLine1Number());
    }

	static String loadCurrentAppKey(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
    	
		return prefs.getString(AGE_CURRENT_APP_KEY, null);
	}
    
	static void saveCurrentAppKey(Context context, String appKey) {
		if(appKey != null) {
			SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
			Editor editor = prefs.edit();
			editor.putString(AGE_CURRENT_APP_KEY, appKey);
			editor.commit();
		}
	}
	
	static int loadLastPhoneCount(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
		
		return prefs.getInt(AGE_LAST_PHONE_COUNT, -1);
	}
	
	static void saveLastPhoneCount(Context context, int count) {
		SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.putInt(AGE_LAST_PHONE_COUNT, count);
		editor.commit();
	}
	
	static Map<String, String> retrieveReferralParams(Context context) {
		HashMap<String, String> params = new HashMap<String, String>();
		SharedPreferences storage = context.getSharedPreferences(
				"AgeConstants.AGE_PREFERENCES", Context.MODE_PRIVATE);

		for (String key : AgeConstants.EXPECTED_PARAMETERS) {
			String value = storage.getString(key, null);
			if (value != null) {
				System.out.println(value);
				params.put(key, value);
			}
		}
		return params;
	}
	
	static String retrieveInstallReferrer(Context context) {
		SharedPreferences storage = context.getSharedPreferences(
				"AgeConstants.AGE_PREFERENCES", Context.MODE_PRIVATE);
		return storage.getString(AgeConstants.P_INSTALL_REFERRER, null);
	}

	static void initTapjoy(Context context) {
    	Log.i(AGE_LOG, "Tapjoy SDK Version: 1.0.1");
    	
		try {
			SharedPreferences settings = context.getSharedPreferences(AGE_PREFERENCES, 0);

			try {
				TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

				if(telephonyManager != null) {
					deviceID = telephonyManager.getDeviceId();
				}

				boolean invalidDeviceID = false;

				// ----------------------------------------
				// Is the device ID null or empty?
				// ----------------------------------------
				if(deviceID == null) {
					Log.e(AGE_LOG, "Device id is null.");
					invalidDeviceID = true;
				}
				// ----------------------------------------
				// Is this an emulator device ID?
				// ----------------------------------------
				else if(deviceID.length() == 0
						|| deviceID.equals("000000000000000")
						|| deviceID.equals("0")) {
					Log.e(AGE_LOG, "Device id is empty or an emulator.");
					invalidDeviceID = true;
				}
				// ----------------------------------------
				// Valid device ID.
				// ----------------------------------------
				else {
					// Lower case the device ID.
					deviceID = deviceID.toLowerCase();
				}
				
				// Is this at least Android 2.3+?
				// Then let's get the serial.
				if(Integer.parseInt(android.os.Build.VERSION.SDK) >= 9) {
					Log.i(AGE_LOG, "TRYING TO GET SERIAL OF 2.3+ DEVICE...");

					// THIS CLASS IS ONLY LOADED FOR ANDROID 2.3+
					TapjoyHardwareUtils hardware = new TapjoyHardwareUtils();
					serialID = hardware.getSerial();

					// Is there no IMEI or MEID?
					if(invalidDeviceID) {
						deviceID = serialID;
					}

					Log.i(AGE_LOG, "====================");
					Log.i(AGE_LOG, "SERIAL: deviceID: [" + deviceID + "]");
					Log.i(AGE_LOG, "====================");

					// ----------------------------------------
					// Is the device ID null or empty?
					// ----------------------------------------
					if(deviceID == null) {
						Log.e(AGE_LOG, "SERIAL: Device id is null.");
						invalidDeviceID = true;
					}
					// ----------------------------------------
					// Is this an emulator device ID?
					// ----------------------------------------
					else if(deviceID.length() == 0
							|| deviceID.equals("000000000000000")
							|| deviceID.equals("0")
							|| deviceID.equals("unknown")) {
						Log.e(AGE_LOG, "SERIAL: Device id is empty or an emulator.");
						invalidDeviceID = true;
					}
					// ----------------------------------------
					// Valid device ID.
					// ----------------------------------------
					else {
						// Lower case the device ID.
						deviceID = deviceID.toLowerCase();
						invalidDeviceID = false;
					}
				}

				// Is the device ID invalid? This is probably an emulator or
				// pre-production device.
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
			
			// Save the SHA-2 hash of the device id.
			sha2DeviceID = sha256(deviceID);
			
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
			Log.i(AGE_LOG, "sha2DeviceID: " + sha2DeviceID);
			Log.i(AGE_LOG, "macAddress: " + macAddress);
		}
		catch(Exception e) {
			Log.e(AGE_LOG, "Error initializing Tapjoy parameters.  e=" + e.toString());
		}
	}
    
	static String getTapjoyUDID() {
    	return sha2DeviceID;
    }
	
	static String getMacAddress() {
		return macAddress;
	}

	static class AgeResponse {
		
		private int code;
		private String message;
		private JSONObject json;
		
		
		public int getCode() {
			return code;
		}

		public void setCode(int code) {
			this.code = code;
		}

		public boolean isSuccess() {
			return code == 1000;
		}
		
		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public JSONObject getJson() {
			return json;
		}

		public void setJson(JSONObject json) {
			this.json = json;
		}
		
	}
    
}
