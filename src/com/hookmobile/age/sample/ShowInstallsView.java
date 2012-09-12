package com.hookmobile.age.sample;

import static com.hookmobile.age.AgeUtils.lookupNameByPhone;

import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.hookmobile.age.Discoverer;

public class ShowInstallsView extends ListActivity {   
    
	private List<String> installs;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("Installs");
        
		ListView listView = getListView();
		listView.setItemsCanFocus(false);
		
		installs = Discoverer.getInstance().getCachedInstalls();
		populateInstalls();
	}
	
	private void populateInstalls() {
		if(installs != null) {
			int count = installs.size();
			
			if(count > 0) {
				String[] records = new String[count]; 
				
				for(int i = 0; i < count; i++) {
					records[i] =  lookupNameByPhone(this, installs.get(i));
				}
				setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, records));
			}
		}
	}
	
}
