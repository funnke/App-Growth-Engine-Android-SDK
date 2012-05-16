package com.hookmobile.age.sample;

import static com.hookmobile.age.AgeUtils.lookupNameByPhone;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.hookmobile.age.AgeException;
import com.hookmobile.age.Discoverer;
import com.hookmobile.age.Lead;
import com.hookmobile.age.R;

public class SendInvitationsView extends ListActivity {   
	
	private static int HANDLE_SHOWLOADING = 1;
	private static int HANDLE_HIDELOADING = 2; 
	private static int HANDLE_SHOWTIPS = 3; 
	
	private List<Lead> leads = null;
	private ProgressDialog progressDialog = null;
	
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if(msg.what == HANDLE_SHOWLOADING) {
				progressDialog.show();
			}
			else if(msg.what == HANDLE_HIDELOADING) {
				progressDialog.cancel();
			}
			else if(msg.what == HANDLE_SHOWTIPS) {
				Toast.makeText(SendInvitationsView.this, (String)msg.obj, Toast.LENGTH_SHORT).show();
			}
		}
	};
    
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("Leads");
		
		ListView listView = getListView();
		listView.setItemsCanFocus(false);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Please Wait...");
        
		leads = Discoverer.getInstance().getCachedLeads();
		populateLeads();
	}
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.getMenuInflater().inflate(R.menu.referral_menu, menu);
		
		return super.onCreateOptionsMenu(menu);
	}
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menu_referral_virtual_number: {
				sendReferralFromVirtualNumber();
				break;
			}
			case R.id.menu_referral_user_number: {
				sendReferralFromUserPhone();
				break;
			}
			case R.id.menu_referral_cancel: {
				finish();
				break;
			}
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void populateLeads() {
		if(leads != null) {
			int count = leads.size();
			
			if(count > 0) {
				String[] records = new String[count]; 
				
				for(int i = 0; i < count; i++) {
					String name = lookupNameByPhone(this, leads.get(i).getPhone());
					StringBuffer data = new StringBuffer(name)
											.append(" (")
											.append(leads.get(i).getOsType())
											.append(")");
					records[i] =  data.toString();
				}
				setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, records));
			}
		}
	}
	
	private void sendReferralFromVirtualNumber() {
		Thread a = new Thread() {
			@Override
			public void run() {
				super.run();
				handler.sendEmptyMessage(HANDLE_SHOWLOADING);
				
				List<String> phones = getSelectedPhones();
				
				if(phones.size() > 0) {
					try {
						Discoverer.getInstance().newReferral(phones, true, null);
						
						showMessage("Referral Success");
					}
					catch(AgeException e) {
						showMessage(e.getMessage() != null ? e.getMessage() : "Referral Error");
					}
				}
				handler.sendEmptyMessage(HANDLE_HIDELOADING);
			}
		};
		a.start();
		a = null;
	}
	
	private void sendReferralFromUserPhone() {
		handler.sendEmptyMessage(HANDLE_SHOWLOADING);
		
		List<String> phones = getSelectedPhones();
		
		if(phones.size() > 0) {
			try {
				Discoverer.getInstance().newReferral(phones, false, null);
				
				showMessage("Referral Success");
			}
			catch(AgeException e) {
				showMessage(e.getMessage() != null ? e.getMessage() : "Referral Error");
			}
		}
		
		handler.sendEmptyMessage(HANDLE_HIDELOADING);
	}

	private List<String> getSelectedPhones() {
		List<String> phones = new ArrayList<String>();
		SparseBooleanArray positions = getListView().getCheckedItemPositions();
		
		for(int i=0; i < positions.size(); i++) {
			if(positions.valueAt(i)) {
				phones.add(leads.get(positions.keyAt(i)).getPhone());
			}
		}
		
		return phones;
	}
	
	private void showMessage(String message) {
		Message msg = handler.obtainMessage();
		msg.what = HANDLE_SHOWTIPS;
		msg.obj = message;
		handler.sendMessage(msg);
	}
	
}
