package com.hookmobile.age.sample;

import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hookmobile.age.Discoverer;
import com.hookmobile.age.R;
import com.hookmobile.age.Referral;

public class ShowReferralsView extends ListActivity {   
	
	private List<Referral> referrals;
    
	private BaseAdapter referralsAdapter = new BaseAdapter()  {        
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null) {
				convertView = LayoutInflater.from(ShowReferralsView.this).inflate(R.layout.referrals_item, null);
			}
			
			TextView time = (TextView)convertView.findViewById(R.id.invitation_date);
			TextView clickThrough = (TextView)convertView.findViewById(R.id.total_click_through);
			TextView invitee = (TextView)convertView.findViewById(R.id.total_invitee);
			
			time.setText("invitationDate: " + referrals.get(position).getInvitationDate());
			clickThrough.setText("totalClickThrough: " + referrals.get(position).getTotalClickThrough());
			invitee.setText("totalInvitee: " + referrals.get(position).getTotalInvitee());
			
			return convertView;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public int getCount() {
			return referrals != null ? referrals.size() : 0;
		}
	};
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("Referrals");
		
		final ListView listView = getListView();
		referrals = Discoverer.getInstance().getCachedReferrals();
		
		listView.setItemsCanFocus(false);
		listView.setAdapter(referralsAdapter);
	}
    
}
