/* Copyright 2011 PeerFinity Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.example.Chat;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.peerfinity.appcast.lib.client.PFClientListActivity;
import com.peerfinity.appcast.lib.client.PFClientReceiver;
import com.peerfinity.appcast.lib.client.PFClientRequester;
import com.peerfinity.appcast.lib.client.PFClientRoom;
import com.peerfinity.appcast.lib.client.commands.GetEvents;
import com.peerfinity.appcast.lib.client.commands.GetObject;
import com.peerfinity.appcast.lib.client.commands.GetProfile;
import com.peerfinity.appcast.lib.client.commands.PFCommand;
import com.peerfinity.appcast.lib.client.commands.PostObject;
import com.peerfinity.appcast.lib.common.ComConstants;
import com.peerfinity.appcast.lib.common.PFClientGUI;
import com.peerfinity.appcast.lib.common.PFEvent;
import com.peerfinity.appcast.lib.common.PFEventFilter;
import com.peerfinity.appcast.lib.common.PFLocation;
import com.peerfinity.appcast.lib.common.PFObject;
import com.peerfinity.appcast.lib.common.exceptions.AppCastVersionOld;
import com.peerfinity.appcast.lib.common.exceptions.AppcastNotInstalled;
import com.peerfinity.appcast.lib.common.exceptions.AppcastSetupException;
import com.peerfinity.appcast.lib.common.exceptions.ClientVersionOld;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;

/**
 * Simple chat plugin implementation
 * 
 * @author alex
 *
 */
public class Chat extends PFClientListActivity {

	private static final String TAG = Chat.class.getSimpleName();

	private TextView empty_text = null;
	private TextView empty_text_more = null;
	private EditText inputtext = null;
	private ImageView connectionstatus = null;
	private LinearLayout pkgrow = null;
	private LinearLayout postrow = null;
	private LinearLayout refreshprogress = null;
	private Button postbutton = null;

	// adapter
	private PeerInfoAdapter peerinfoadapter = null;

	// list of all events and their data
	private Set<EventString> currentEvents = null;

	// pf connection
	private PFClientRequester pfclient;
	private PFClientReceiver pfrecv;
	private ChatRoom chatroom;

	// set to true when there is not more historic data in the room
	private boolean nomoredata = false;
	// timestamp of the oldest post currently in memory
	private long oldest_post = 0;
	// flag to indicate complete reload of data onResume

	private int SELECT_OBJECT = 1;

	private List<PFEvent> inchatevents = null;

	static final Comparator<EventString> TIMEORDER = new Comparator<EventString>() {
		public int compare(EventString e1, EventString e2) {
			Long a = e1.getPublished();
			Long b = e2.getPublished();
			int c = b.compareTo(a);
			if(c != 0)
				return c;
			// incase the timestamps are the same
			return e1.getUuid().compareTo(e2.getUuid());
		}
	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SELECT_OBJECT) {
			if (resultCode == RESULT_OK) {
				Bundle bun = data.getExtras();

				ArrayList<PFEvent> events = bun.getParcelableArrayList(ComConstants.BUN_EVENTS);
				for(PFEvent e : events)
					setObjPackage(e);
			}
		}
	}

	@Override
	public Context getContext() {
		return this;
	}

	/**
	 * stops the application from resetting it self on screen orentation changes
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.chat_main);

		//room = (TextView) findViewById(R.id.room_id);
		inputtext = (EditText) findViewById(R.id.EditText01);
		pkgrow = (LinearLayout)findViewById(R.id.LinearLayoutPkgs);
		postrow = (LinearLayout)findViewById(R.id.LinearLayoutPosts);
		refreshprogress = (LinearLayout)findViewById(R.id.refreshing);
		refreshprogress.setVisibility(LinearLayout.INVISIBLE);
		connectionstatus = (ImageView)findViewById(R.id.imageConnectionStatus);

		empty_text = (TextView) findViewById(R.id.reasonHeader);
		empty_text_more = (TextView) findViewById(R.id.reasonMore);

		// Init the pf connection stuff
		pfclient = generatePFClientRequester(100);
		chatroom = new ChatRoom(pfclient);
		pfrecv = generatePFClientReceiver(chatroom,
				PFClientReceiver.FLAG_ADDPEER | PFClientReceiver.FLAG_DELPEER | PFClientReceiver.FLAG_NEWOBJECT);

		ListView peerview = this.getListView();
		peerinfoadapter = new PeerInfoAdapter(Chat.this, pfclient);
		peerview.setAdapter(peerinfoadapter);

		peerview.setOnScrollListener(new OnScrollListener() {
			private int lastSavedFirst = -1;

			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

				// detect if last item is visible
				if (visibleItemCount < totalItemCount && (firstVisibleItem + visibleItemCount == totalItemCount)) {
					// only process first event
					if (firstVisibleItem != lastSavedFirst) {
						lastSavedFirst = firstVisibleItem;

						Log.d("OnScrollListener - end of list", "vic: " + visibleItemCount + ", tic: "
								+ totalItemCount);
						// get next
						if(!nomoredata) {
							getBatchData();
						}
					}
				}
			}

			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
			}
		});

		currentEvents = Collections.synchronizedSet(new TreeSet< EventString >(TIMEORDER));
		peerinfoadapter.setEventList(currentEvents);

		postbutton = (Button) findViewById(R.id.Button01);
		postbutton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				try {
					if(!pfclient.isNetworkUp()) {
						Toast.makeText(Chat.this, "Network down, will post later", Toast.LENGTH_SHORT).show();					
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				DoPostObject();
			}
		});

		postbutton.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				showDialog(DIALOG_CLEAR_EDITOR);
				return true;
			}
		});
	}

	private void setObjPackage(PFEvent event) {
		if(event != null) {
			ImageView iv = new ImageView(this);
			iv.setImageResource(R.drawable.unknown);
			pkgrow.addView(iv);
			if(inchatevents == null) {
				inchatevents = new ArrayList<PFEvent>();
			}
			inchatevents.add(event);
		} else {
			pkgrow.removeAllViews();
			inchatevents = null;
		}
	}

	/**
	 * generate the datapackage to send to the server
	 * @return
	 */

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		pfclient.connect();
		pfrecv.connect();
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
		pfclient.clean();
		pfrecv.clean();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		return super.onMenuItemSelected(featureId, item);
	}


	private static final int DIALOG_CLEAR_EDITOR = 5;

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		AlertDialog.Builder builder;
		switch(id) {

		case DIALOG_CLEAR_EDITOR:

			builder = new AlertDialog.Builder(Chat.this);
			builder.setTitle(R.string.dialog_clear_title)
			.setCancelable(true)
			.setPositiveButton(R.string.dialog_clear_clear, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
					setObjPackage(null);
					inputtext.setText("");
				}
			})
			.setNegativeButton(R.string.dialog_clear_dismiss, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}
			});

			dialog = builder.create();
			break;

		default:
			dialog = null;
		}
		return dialog;
	}



	/**
	 * perform standard peerfinity action on clicking of item in list
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		EventString es = (EventString)l.getItemAtPosition(position);
		List<PFEvent> el = es.getRelated();
		if(el == null)
			return;
		if(el.isEmpty())
			return;

		// Launch this app
		PFClientGUI.runPluginSelectorView(this, new ArrayList<PFEvent>(el));
		return;
	}

	/**
	 * new event has occured, update the oldes_post info
	 * if need be.
	 * 
	 * start thread to fetch new data
	 * @param es
	 */
	public void dealWithNewEvent(final EventString es) {

		if(oldest_post > es.getPublished() || oldest_post == 0) {
			oldest_post = es.getPublished();
			//System.out.println("oldest_post: "+oldest_post);
		}

		pfclient.execute(
				new GetObject(
						es.getUuid(),
						PFClientRequester.DATA_ALL,
						PFClientRequester.RELATED_FORWARD,
						pfclient.genDefFilter()),
						new GetObject.GetObjectReply() {

					@Override
					public void onObject(final GetObject arg0, boolean arg1) {
						if(arg0.object == null) {
							Log.e(TAG, "no object!!");
						}
						es.setObject(arg0.object);
						peerinfoadapter.notifyDataSetChanged();
					}

					@Override
					public void onException(PFCommand arg0, Exception e) {
						Log.e(TAG, "no data: "+e.getMessage());
					}

				}, false);
	}	


	class ChatRoom extends PFClientRoom {

		public ChatRoom(PFClientRequester req) {
			super(req);
		}

		@Override
		public void pfOnNetworkDown() {
			super.pfOnNetworkDown();
			Log.d(TAG, "pfOnNetworkDown");
			connectionstatus.setVisibility(ImageView.VISIBLE);
		}

		@Override
		public void pfOnNetworkUp() {
			super.pfOnNetworkUp();
			Log.d(TAG, "pfOnNetworkUp");
			connectionstatus.setVisibility(ImageView.INVISIBLE);
		}


		@Override
		public void pfOnEvent(PFEvent event) {
			super.pfOnEvent(event);

			switch(event.getType()) {

			case PFEvent.EVENT_POST_NEW:

				EventString es = new EventString(
						PFEvent.EVENT_POST_NEW,
						getDataFeedName(),
						event.getUuid(),
						event.getPeer(),
						event.getPublished(),
						event.getRange());

				synchronized(currentEvents) {
					if(currentEvents.contains(es)) {
						currentEvents.remove(es);
					}
					currentEvents.add(es);
				}
				dealWithNewEvent(es);
				break;

			case PFEvent.EVENT_PEER_GONE:
				if(isForeground()) {

					pfclient.execute(
							new GetProfile(event.getPeer(), 48, 48, Chat.this),
							new GetProfile.GetProfileReply() {

								@Override
								public void onProfile(final GetProfile arg0, boolean arg1) {
									Toast.makeText(Chat.this, "Chat: "+arg0.profile.getNickName()+" left", Toast.LENGTH_SHORT).show();
								}

								@Override
								public void onException(PFCommand arg0, Exception e) {
									e.printStackTrace();
								}						
							}, false);

				}
				break;

			case PFEvent.EVENT_PEER_NEW:
				if(isForeground()) {

					pfclient.execute(
							new GetProfile(event.getPeer(), 48, 48, Chat.this),
							new GetProfile.GetProfileReply() {
								@Override
								public void onProfile(final GetProfile arg0, boolean arg1) {
									Toast.makeText(Chat.this, "Chat: "+arg0.profile.getNickName()+" joined", Toast.LENGTH_SHORT).show();
								}

								@Override
								public void onException(PFCommand arg0, Exception e) {
									e.printStackTrace();
								}

							}, false);
				}
				break;

			default:
				Log.e(TAG, "Unknown event type");
			}
		}

		/**
		 * for some reason we have to completely reload all
		 * our data. This is probably due to a room change.
		 * 
		 */
		@Override
		public void pfOnRoomChange(PFLocation location) {
			super.pfOnRoomChange(location);

			synchronized(currentEvents) {
				currentEvents.clear();
			}

			empty_text.setText(R.string.empty_connecting);
			empty_text_more.setText(R.string.empty_connecting_more);

			oldest_post = 0;
			nomoredata = false;

			getBatchData();
			peerinfoadapter.notifyDataSetChanged();
		}

		@Override
		public void pfOnAppcastDown() {
			// ignore
		}
	}

	@Override
	public void pfSessionOnConnected(String arg0) {
		try {
			chatroom.pfOnRoomChange(pfclient.getLocation());
			if(pfclient.isNetworkUp()) {
				chatroom.pfOnNetworkUp();
			} else {	
				chatroom.pfOnNetworkDown();
			}
		} catch (RemoteException e) {
			e.printStackTrace();
			finish();
		}
		refreshPosts();

	}

	@Override
	public void pfSessionOnDisconnected(String arg0) {
	}

	@Override
	public void pfSessionOnError(String arg0, Throwable e) {
		e.printStackTrace();

		if(e.getClass() == AppCastVersionOld.class) {
			PFClientGUI.errorAppCastVersionOld(this).show();
		}
		if(e.getClass() == ClientVersionOld.class) {
			PFClientGUI.errorClientVersionOld(this).show();
		}
		if(e.getClass() == AppcastSetupException.class) {
			PFClientGUI.warningAppCastNotSetup(this).show();
		}
		if(e.getClass() == AppcastNotInstalled.class) {
			PFClientGUI.warningAppCastNotPresent(this).show();
		}
	}

	@Override
	public void pfSessionOnAppcastShutdown() {
		Log.d(TAG, "pfSessionOnAppcastShutdown");
		finish();
	}

	public void getBatchData() {
		PFEventFilter pfef = new PFEventFilter(getDataFeedName(), oldest_post, 0, 10);
		boolean force_reload = true;
		// only need to force reload, when reload the latest data
		// not reloading old historical data
		if(oldest_post != 0) {
			force_reload = false;
		}

		// set loading display
		refreshprogress.setVisibility(LinearLayout.VISIBLE);
		// if we already have data, this wont show
		empty_text.setText(R.string.empty_connecting);
		empty_text_more.setText(R.string.empty_connecting_more);

		pfclient.execute(
				new GetEvents(pfef),
				new GetEvents.GetEventsReply() {

					@Override
					public void onEvents(GetEvents arg0, boolean another) {
						if(another == false) {
							refreshprogress.setVisibility(LinearLayout.INVISIBLE);							
						}

						// no events returned from call
						if(arg0.events.isEmpty()) {
							empty_text.setText(R.string.empty_connected);
							empty_text_more.setText(R.string.empty_connected_more);
						}

						for(PFEvent event : arg0.events)
							chatroom.pfOnEvent(event);
					}

					@Override
					public void onException(PFCommand arg0, Exception e) {
						Toast.makeText(Chat.this, e.getMessage(), Toast.LENGTH_SHORT).show();
						refreshprogress.setVisibility(LinearLayout.INVISIBLE);
					}	
				}, force_reload);
	}

	private void refreshPosts() {
		try {
			postrow.removeAllViews();
			int c = pfclient.getCachedPostsNumber();
			Log.d(TAG, "There are "+c+" post(s) in the queue for this client");
			for(int i = 0; i<c; i++) {
				ImageView iv = new ImageView(this);
				iv.setImageResource(R.drawable.post);
				postrow.addView(iv);				
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void DoPostObject() {

		String text = inputtext.getText().toString();
		if(text.length() == 0) {
			return;
		}

		final PFObject data = new PFObject();
		data.setData(text.getBytes());
		if(inchatevents != null)
			for(PFEvent e : inchatevents)
				data.addRelation(e);

		PostObject po = new PostObject(data, false);

		// Clear objects in editor
		inputtext.setText(null);
		setObjPackage(null);

		pfclient.execute(po,
				new PostObject.PostObjectReply() {

			@Override
			public void onException(PFCommand arg0, Exception arg1) {
				Toast.makeText(Chat.this, arg1.getMessage(), Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onSuccess(PostObject arg0) {
				PFEvent event = arg0.event;

				EventString es = new EventString(
						PFEvent.EVENT_POST_NEW,
						getDataFeedName(),
						event.getUuid(),
						event.getPeer(),
						event.getPublished(),
						event.getRange());

				synchronized(currentEvents) {
					if(currentEvents.contains(es)) {
						currentEvents.remove(es);
					}
					currentEvents.add(es);
				}
				dealWithNewEvent(es);
				refreshPosts();
			}

		}, false);
		refreshPosts();
	}
}
