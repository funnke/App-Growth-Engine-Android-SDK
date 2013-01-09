package com.hookmobile.age;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;

public class AgeJavaScriptHelper {

	private final static int HANDLE_SHOW_LOADING = 1;
	private final static int HANDLE_HIDE_LOADING = 2; 
	private final static int HANDLE_SHOW_MESSAGE_DIALOG = 4;

	private Activity activity;
	private String appKey;
	boolean useVirtualNumber;
	private ProgressDialog progressDialog;
	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if(msg.what == HANDLE_SHOW_LOADING) {
				progressDialog.show();
			}
			else if(msg.what == HANDLE_HIDE_LOADING) {
				progressDialog.cancel();
			}
			else if(msg.what == HANDLE_SHOW_MESSAGE_DIALOG) {
				String[] content = (String[])msg.obj;
				showErrorDialog(content[0], content[1], content[2]);
			}
		}

		private void showErrorDialog(String title, String message, String buttonText) {
			new AlertDialog.Builder(activity)
				.setTitle(title)
				.setMessage(message)
				.setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
	    				
	    			}
				})
				.show();
		}
	};
	
	/**
	 * Constructor to create helper class that send invitation via virtual number.
	 * @param activity
	 * @param appKey
	 */
	public AgeJavaScriptHelper(Activity activity, String appKey) {
		this.activity = activity;
		this.appKey = appKey;
		useVirtualNumber = true;
		
		Discoverer.activate(activity.getBaseContext(), appKey);

		progressDialog = new ProgressDialog(this.activity);
		progressDialog.setMessage("Please Wait...");
		progressDialog.setCancelable(false);
	}
	
	/**
	 * Constructor to create helper class
	 * @param activity
	 * @param appKey
	 * @param useVirtualNumber
	 */
	public AgeJavaScriptHelper(Activity activity, String appKey, boolean useVirtualNumber) {
		this.activity = activity;
		this.appKey = appKey;
		this.useVirtualNumber = useVirtualNumber;
		
		Discoverer.activate(activity.getBaseContext(), appKey);
	}


	public String getAppKey() {
		return appKey;
	}

	public void setUseVirtualNumber(boolean useVirtualNumber) {
		this.useVirtualNumber = useVirtualNumber;
	}

	//Call Age SDK.
	public void discover() {
		
		Thread a = new Thread() {
			@Override
			public void run() {
				super.run();

				try {
					Discoverer.getInstance().discover();
					
				} catch (AgeException e) {
					displayError(e);
				}
			}
		};
		a.start();
		a = null;
	}
	
	public String getInstallCode(){
		return Discoverer.getInstance().getInstallCode();
	}
	
	public String lookupNameByPhone(String phone){
		return com.hookmobile.age.utils.AgeUtils.lookupNameByPhone(activity, phone);		
	}
	
	public long newReferral(String[] phones){
		List<String> listPhones = new ArrayList<String>();
		for(int i = 0; i < phones.length; ++i)
			listPhones.add(phones[i]);
		try{
			return Discoverer.getInstance().newReferral(listPhones, useVirtualNumber, null);
		} catch (AgeException e) {
			 displayError(e);
			 return 0;
		}
	}
	
	public boolean hasPreviouslyDiscovered() {
		return Discoverer.getInstance().hasPreviouslyDiscovered();
	}

	//Backward
	public void back() {
		activity.setResult(Activity.RESULT_OK, activity.getIntent());
		activity.finish();
	}

	//Display message.
	private void showMessage(String[] content) {
		Message msg = handler.obtainMessage();
		msg.what = HANDLE_SHOW_MESSAGE_DIALOG;
		msg.obj = content;
		handler.sendMessage(msg);
	}

	private void displayError(AgeException e) {
		String body = "Hook Mobile server encountered a problem: ";

		if (e.getMessage() != null) {
			body += e.getMessage();
		} else {
			body += "Unknown Error";
		}

		showMessage(new String[] { "Finished", body, "Dismiss" });
	}
}
