package com.hookmobile.age;

import static com.hookmobile.age.AgeConstants.AGE_LAST_PHONE_COUNT;
import static com.hookmobile.age.AgeConstants.AGE_PREFERENCES;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;

/**
 * Provides utility methods for AGE services and the sample app.
 */
public class AgeUtils {
	
	private static Cursor contactCursor;
	
	
    /**
     * Normalizes the phone number format.
     * 
     * @param number the input phone number.
     * @return a normalized phone number.
     */
    public static String normalizePhone(String number) {
    	if(number != null && number.length() > 0) {
    		if(number.charAt(0) == '+') {
    			number = "+" + number.substring(1).replaceAll("[^\\d]", "");
    			
    			return number;
    		}
    		else {
    			number = number.replaceAll("[^\\d]", "");
    			
    			if(number.length() == 10)
    				return "+1" + number;
    			else if(number.length() == 11 && number.charAt(0) == '1')
    				return "+" + number;
    			else if(number.length() > 10)
    				return "+" + number;
    			else
    				return "+1" + number;
    		}
    	}
    	else {
    		return "+1";
    	}
    }

    /**
     * Looks up contact name in the address book by the phone number.
     * 
     * @param context the Android context.
     * @param phone the phone number to look up.
     * @return a contact name.
     */
    public static String lookupNameByPhone(Context context, String phone) {
    	Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
    	String[] projection = new String[] { ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID };
    	Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
    	
    	try {
    		while(cursor.moveToNext()) {
    			String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
    			
    			if(name != null && name.length() > 0) {
    				return name;
    			}
    		}
    	}
    	finally {
    		cursor.close();
    	} 
    	
    	return phone;
    }

    /**
     * Checks if this device supports SMS.
     * 
     * @param context the Android context.
     * @return true if this device is SMS capable; false otherwise.
     */
    public static boolean isSmsSupported(Context context) {
    	PackageManager manager = context.getPackageManager();
    	
    	if(manager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
    		return true;
    	}
    	
    	return false;
    }
    
    /**
     * Checks if this device is connected to an internet.
     * 
     * @param context the Android context.
     * @return true if this device is connected to an internet; false otherwise.
     */
    public static boolean isOnline(Context context) {
    	ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo netInfo = cm.getActiveNetworkInfo();
    	
    	if(netInfo != null && netInfo.isConnectedOrConnecting()) {
    		return true;
    	}
    	
    	return false;
    }

    /**
     * Checks whether the string is empty or null.
     * 
     * @param str the string.
     * @return true if it is empty or null; false otherwise;
     */
    public static boolean isEmptyStr(String str) {
    	if(str == null || str.length() == 0) {
    		return true;
    	}
    	
    	return false;
	}
	
    /**
     * Gets this device information in JSON format.
     * 
     * @param context the Android context.
     * @return a JSON representation of this device information.
     */
    public static String getDeviceInfo(Context context) {
    	String osVersion = android.os.Build.VERSION.RELEASE; 
    	String brand = android.os.Build.BRAND; 
    	String product = android.os.Build.PRODUCT; 
    	String model = android.os.Build.MODEL; 
    	String manufacturer = android.os.Build.MANUFACTURER; 
    	String device = android.os.Build.DEVICE; 
    	String devicePhone = queryDevicePhone(context);
    	
        JSONObject deviceObj = new JSONObject();
        
        try {
        	deviceObj.put("os", "android");
        	deviceObj.put("osVersion", osVersion);
        	deviceObj.put("brand", brand);
        	deviceObj.put("product", product);
        	deviceObj.put("model", model);
        	deviceObj.put("manufacturer", manufacturer);
        	deviceObj.put("device", device);
        	deviceObj.put("phoneNum", devicePhone);
        }
        catch(JSONException e) {
        	
        }
        
        return deviceObj.toString();
    }
    
    /**
     * Gets number of the phone in the address book.
     * 
     * @param context the Android context.
     * @return the count.
     */
     public static int getPhoneCount(Context context) {
    	if(contactCursor == null || contactCursor.isClosed()) {
    		contactCursor = getContactCursor(context);
    	}
    	
    	int phoneCount = contactCursor.getCount();
    	int lastCount = loadLastPhoneCount(context);
    	
    	if(phoneCount == lastCount) {
    		contactCursor.close();
    	}
    	
    	return phoneCount;
    }
    
    /**
     * Gets the entire address book in JSON format.
     * 
     * @param context the Android context.
     * @return a JSON representation of the address book.
     */
    public static String getAddressbook(Context context) {
    	return getAddressbook(context, Integer.MAX_VALUE);
    }
    
	/**
	 * Gets contacts of the address book limited up to max size.
	 * 
	 * @param context the Android context.
	 * @param limit the max size of contacts to fetch.
	 * @return a JSON representation of the address book.
	 */
    public static String getAddressbook(Context context, int limit) {
    	return getAddressbook(context, limit, false);
    }
    
	static String getAddressbook(Context context, int limit, boolean sleep) {
		JSONArray addressBook = new JSONArray();
		String lastName = null;
		String firstName = null;
		int counter = 0;
		
		if(contactCursor == null || contactCursor.isClosed()) {
    		contactCursor = getContactCursor(context);
    	}
		
		try {
			while(contactCursor.moveToNext()) {
				if(addressBook.length() < limit) {
					String phoneNumber = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
					String displayName = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
					JSONObject contactObj = new JSONObject();

					try {
						String phone = normalizePhone(phoneNumber);

						if(phone.length() >= 10) {
							if(displayName != null) {
								int pos = displayName.lastIndexOf(" ");
								
								if(pos >= 0) {
									firstName = displayName.substring(0, pos);
									lastName = displayName.substring(pos);
								}
								else {
									firstName = displayName;
									lastName = "";
								}
							}
							else {
								firstName = "";
								lastName = "";
							}
							
							contactObj.put("phone", phone);
							contactObj.put("firstName", firstName != null ? firstName : "");
							contactObj.put("lastName", lastName != null ? lastName : "");
							addressBook.put(contactObj);
						}
						
						if(sleep) {
							if(++counter % 200 == 0) {
								Thread.sleep(100);
							}
						}
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
				else {
					break;
				}
			}
		}
		finally {
			contactCursor.close();
		}
		
		return addressBook.toString();
    }

	static String getAddressHash(Context context) {
		contactCursor = getContactCursor(context);
		
		try {
			StringBuffer hash = new StringBuffer();
			int counter = 0;
			
			while(contactCursor.moveToNext()) {
				if(++counter <= 10) {
					String phoneNumber = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
					String phone = normalizePhone(phoneNumber);
					
					hash.append(MurmurHash.hash(phone.getBytes(), 0)).append("|");
				}
				else {
					break;
				}
			}
			
			if(hash.length() > 0) {
				hash.deleteCharAt(hash.length()-1);
			}
			
			return hash.toString();
		}
		finally {
			contactCursor.close();
		}
	}
	
    static String queryDevicePhone(Context context) {
    	TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE); 
    	
    	return normalizePhone(manager.getLine1Number());
    }
    
    static int loadLastPhoneCount(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
		
		return prefs.getInt(AGE_LAST_PHONE_COUNT, 0);
	}
	
	static void saveLastPhoneCount(Context context, int count) {
		SharedPreferences prefs = context.getSharedPreferences(AGE_PREFERENCES, Context.MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.putInt(AGE_LAST_PHONE_COUNT, count);
		editor.commit();
	}
	
    private static Cursor getContactCursor(Context context) {
    	return context.getContentResolver().query(
    			ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
    			new String[] {
    				ContactsContract.CommonDataKinds.Phone._ID,
    				ContactsContract.CommonDataKinds.Phone.NUMBER,
    				ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
    			},
    			ContactsContract.CommonDataKinds.Phone.NUMBER + " IS NOT NULL", null, null);
    }
    
}
