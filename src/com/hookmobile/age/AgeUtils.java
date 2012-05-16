package com.hookmobile.age;

import java.io.IOException;
import java.util.List;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.telephony.TelephonyManager;

import com.hookmobile.age.Discoverer.AgeResponse;

public class AgeUtils {
	
	private static final String P_STATUS		= "status";
	private static final String P_DESCRIPTION	= "desc";
	
	private static final int timeoutConnection	= 100000;
	private static final int timeoutSocket		= 200000;
	
	private static final String contactSelection = new StringBuffer(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
														.append("=?")
														.toString();
	private static final String contactItemSelection = new StringBuffer(Data.CONTACT_ID)
														.append("=?")
														.append(" AND ")
														.append(Data.MIMETYPE)
														.append("='")
														.append(StructuredName.CONTENT_ITEM_TYPE)
														.append("'")
														.toString();
	
	
	public static AgeResponse doPost(String url, List<NameValuePair> form) throws ClientProtocolException, IOException, JSONException {
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
    
    public static String getAddressbook(Context context) {    	
    	JSONArray addressBook = new JSONArray();
        Cursor cursor = openContactCursor(context);
        
        if(cursor.moveToFirst()) {  
            do { 
            	String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));     
            	int phoneCount = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));  
                
            	if(phoneCount > 0) {
            		String familyName = null;
            		String givenName = null;
            		Cursor nameCursor = openNameCursor(context, contactId);
                    
            		if(nameCursor.moveToFirst()) { 
            			do {  
            				familyName = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.FAMILY_NAME)); 
            				givenName = nameCursor.getString(nameCursor.getColumnIndex(StructuredName.GIVEN_NAME));
            			}
            			while(nameCursor.moveToNext());  
            		}
            		nameCursor.close();
                    
            		Cursor phoneCursor = openPhoneCursor(context, contactId);
            		
            		if(phoneCursor.moveToFirst()) { 
            			do {
            				String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));   
            				JSONObject contactObj = new JSONObject();
            				
            				try {
            					contactObj.put("lastName", familyName != null ? familyName : "");
            					contactObj.put("phone", normalizePhone(phoneNumber));
            					contactObj.put("firstName", givenName != null ? givenName : "");
            					addressBook.put(contactObj);
            				}
            				catch(JSONException e) {
                            	
            				}
            			}
            			while(phoneCursor.moveToNext());  
            		}
            		phoneCursor.close();
            	}
            }
            while(cursor.moveToNext());  
        }
        cursor.close();
        
        return addressBook.toString();
    }
    
    public static String getDeviceOsInfo(Context context) {
    	String osVersion = android.os.Build.VERSION.RELEASE; 
    	String brand = android.os.Build.BRAND; 
    	String product = android.os.Build.PRODUCT; 
    	String model = android.os.Build.MODEL; 
    	String manufacturer = android.os.Build.MANUFACTURER; 
    	String device = android.os.Build.DEVICE; 
    	String devicePhone = queryDevicePhone(context);
    	
        JSONObject deviceObj = new JSONObject();
        
        try {
        	deviceObj.put("osVersion", osVersion);
        	deviceObj.put("brand", brand);
        	deviceObj.put("product", product);
        	deviceObj.put("model", model);
        	deviceObj.put("manufacturer", manufacturer);
        	deviceObj.put("device", device);
        	deviceObj.put("phoneNumNorm", devicePhone);
        }
        catch(JSONException e) {
        	
        }
        
        return deviceObj.toString();
    }
    
    public static String queryDevicePhone(Context context) {
    	TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE); 
    	
    	return normalizePhone(manager.getLine1Number());
    }
    
    public static boolean isSmsSupported(Context context) {
    	PackageManager manager = context.getPackageManager();
    	
    	if(manager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
    		return true;
    	}
    	
    	return false;
    }
    
    public static boolean isEmptyStr(String str) {
    	if(str == null || str.length() == 0) {
    		return true;
    	}
    	
    	return false;
	}
    
    private static Cursor openContactCursor(Context context) {
    	return context.getContentResolver().query(  
    					ContactsContract.Contacts.CONTENT_URI,  
    					null, 
    					null,
    					null,
    					null);
    }
    
    private static Cursor openNameCursor(Context context, String contactId) {
    	return context.getContentResolver().query(  
    						Data.CONTENT_URI,  
    						new String[] { Data._ID, StructuredName.FAMILY_NAME, StructuredName.GIVEN_NAME },  
    						contactItemSelection,  
    						new String[] { contactId },
    						null);
    }
    
    private static Cursor openPhoneCursor(Context context, String contactId) {
    	return context.getContentResolver().query(
						ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
						null,  
						contactSelection,
						new String[] { contactId },
						null);
    }
    
}
