/*	Notify Me!, an app to enhance Android(TM)'s abilities to show notifications.
	Copyright (C) 2013 Tom Kranz
	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
	
	Android is a trademark of Google Inc.
*/
package org.tpmkranz.notifyme;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.TextView;

public class NotificationService extends AccessibilityService {

	Prefs prefs;
	int filter;
	private BroadcastReceiver receiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if( ((TelephonyManager)arg0.getSystemService(Context.TELEPHONY_SERVICE)).getCallState() != 0 ){
				arg0.unregisterReceiver(this);
				return;
			}
			startActivity(new Intent(arg0, ( ((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode() ? NotificationActivity.class : NotificationActivityTransparent.class ) ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("screenWasOff", ((TemporaryStorage)getApplicationContext()).wasScreenOff() || ((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode()) );
			arg0.unregisterReceiver(this);
		}
	};
	
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		((TemporaryStorage)getApplicationContext()).accessGranted(true);
		if( !event.getClassName().equals("android.app.Notification") )
			return;
		if( filterMatch(event, true) ){
			if( prefs.isAggressive(filter) );
			else if( ((PowerManager)getSystemService(POWER_SERVICE)).isScreenOn() && !((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode() )
				return;
		}else
			return;
		if( filterMatch(event, false) && ( prefs.isDuringCallAllowed(filter) || ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)).getCallState() == 0 ) ) triggerNotification(event);
	}

	@SuppressLint("NewApi")
	private boolean filterMatch(AccessibilityEvent event, boolean nameOnly) {

		boolean filterMatch = false;
		for( int i = 0; i < prefs.getNumberOfFilters() && !filterMatch; i++ ){
			if( event.getPackageName().equals(prefs.getFilterApp(i)) ){
				filter = i;
				if( prefs.hasFilterKeywords(i) && !nameOnly ){
					filterMatch = !prefs.isFilterWhitelist(filter);
					String notificationContents = ( event.getText().size() == 0 ? "" : event.getText().get(0).toString() );
					try{
						Notification notification = (Notification) event.getParcelableData();
						RemoteViews remoteViews = notification.contentView;
						ViewGroup localViews = (ViewGroup) remoteViews.apply(this, null);
						String piece = "";
						for( int j = 16905000 ; j < 16910000 ; j++ ){
							try{
								piece = "\n"+( (TextView) localViews.findViewById(j) ).getText();
								notificationContents = notificationContents.concat(piece);
							}catch(Exception e){
								
							}
						}
						if(android.os.Build.VERSION.SDK_INT >= 16){
							try{
								remoteViews = notification.bigContentView;
								localViews = (ViewGroup) remoteViews.apply(this, null);
								piece = "";
								for( int j = 16905000 ; j < 16910000 ; j++ ){
									try{
										piece = "\n"+( (TextView) localViews.findViewById(j) ).getText();
										notificationContents = notificationContents.concat(piece);
									}catch(Exception e){
										
									}
								}
							}catch(Exception e){
								
							}
						}
					}catch(Exception e){
						
					}
					String[] keywords = prefs.getFilterKeywords(i);
					for( int j = 0 ; j < keywords.length ; j++ ){
						if( notificationContents.contains(keywords[j]) && !keywords[j].equals("") ){
							filterMatch = prefs.isFilterWhitelist(filter);
						}
					}
				}else{
					filterMatch = true;
				}
			}
		}
		return filterMatch;
	}

	private void triggerNotification(AccessibilityEvent event) {
		try{
			unregisterReceiver(receiver);
		}catch(Exception e){
			
		}
		((TemporaryStorage)getApplicationContext()).storeStuff(event.getParcelableData());
		((TemporaryStorage)getApplicationContext()).storeStuff(filter);
		if( ((PowerManager)getSystemService(POWER_SERVICE)).isScreenOn() ){
			if( prefs.isPopupAllowed(filter) ){
				startActivity(new Intent(this, ( ((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode() ? NotificationActivity.class : NotificationActivityTransparent.class ) ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("screenWasOff", ((TemporaryStorage)getApplicationContext()).wasScreenOff() || ((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode()) );
			}
		}
		else{
			if( !prefs.isLightUpAllowed(filter) ){
				IntentFilter iFilter = new IntentFilter();
				iFilter.addAction(Intent.ACTION_SCREEN_ON);
				registerReceiver(receiver, iFilter);
			}else{
				startActivity(new Intent(this, LightUp.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) );
			}
		}
	}

	@Override
	protected void onServiceConnected(){
		AccessibilityServiceInfo info = new AccessibilityServiceInfo();
		info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
		info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
		info.flags = AccessibilityServiceInfo.DEFAULT;
		this.setServiceInfo(info);
		prefs = new Prefs(this);
	}
	
	@Override
	public void onInterrupt() {

	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		((TemporaryStorage)getApplicationContext()).accessGranted(false);
	}

}
