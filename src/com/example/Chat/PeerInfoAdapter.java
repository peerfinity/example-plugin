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
import java.util.Set;

import com.peerfinity.appcast.lib.client.PFClientRequester;
import com.peerfinity.appcast.lib.client.commands.GetAppIcon;
import com.peerfinity.appcast.lib.client.commands.GetProfile;
import com.peerfinity.appcast.lib.client.commands.PFCommand;
import com.peerfinity.appcast.lib.common.PFEvent;
import com.peerfinity.appcast.lib.common.utils.Misc;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PeerInfoAdapter extends BaseAdapter {
	private static final String TAG = PeerInfoAdapter.class.getSimpleName();

	private Set<EventString> eventlist = null;
	private Context mContext;
	private LayoutInflater mInflater;
	private PFClientRequester pfcache;


	public PeerInfoAdapter(Context c, PFClientRequester pfc) {
		mContext = c;
		mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		pfcache = pfc;
	}

	public void setEventList(Set<EventString> pl) {
		eventlist = pl;
	}

	public int getCount() {
		if(eventlist == null)
			return 0;
		synchronized(eventlist) {
			return eventlist.size();
		}
	}

	public EventString getItem(int position) {
		synchronized(eventlist) {
			ArrayList<EventString> al = new ArrayList<EventString>(eventlist);
			return al.get(position);
		}
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {

		convertView = mInflater.inflate(R.layout.chat_adapter_peer, null);
		final ViewHolder holder = new ViewHolder();

		holder.tname = (TextView) convertView.findViewById(R.id.peername);
		holder.post = (TextView) convertView.findViewById(R.id.posttext);
		holder.posttime = (TextView) convertView.findViewById(R.id.posttime);
		holder.peerimg = (ImageView) convertView.findViewById(R.id.peerimage);
		holder.intobj = (LinearLayout) convertView.findViewById(R.id.intobject);

		convertView.setTag(holder);

		synchronized(eventlist) {
			final EventString es = getItem(position);

			holder.tname.setText("[???]");
			holder.post.setText(es.getText());

			String nicetime = Misc.getUserFriendlyTime(es.getPublished());
			holder.posttime.setText(nicetime);

			pfcache.execute(
					new GetProfile(es.getPeer(), 48, 48, mContext),
					new GetProfile.GetProfileReply() {

						@Override
						public void onProfile(GetProfile arg0, boolean arg1) {
							if(arg0.profile == null)
								return;
							if(arg0.profile.getProfilePic() != null)
								holder.peerimg.setImageBitmap(arg0.profile.getProfilePic());
							else
								holder.peerimg.setImageResource(R.drawable.profilepic);
							holder.tname.setText(arg0.profile.getNickName());
						}

						@Override
						public void onException(PFCommand arg0, Exception e) {
							e.printStackTrace();
						}

					}, false);

			// deal with the attached objects
			// first remove all previous icons
			holder.intobj.removeAllViews();
			// for every related object fetch its icon
			// and add an Imageview to display it
			if(es.getRelated() != null) {
				for(PFEvent ev : es.getRelated()) {

					final ImageView mImg = new ImageView(mContext);
					mImg.setImageResource(R.drawable.unknown);
					holder.intobj.addView(mImg);

					pfcache.execute(
							new GetAppIcon(ev.getDataFeed(), 48, 48, mContext),
							new GetAppIcon.GetAppIconReply() {
								@Override
								public void onIcon(GetAppIcon arg0, boolean arg1) {
									if(arg0.img != null)
										mImg.setImageBitmap(arg0.img);
								}

								@Override
								public void onException(PFCommand arg0, Exception e) {
									e.printStackTrace();
								}
							}, false);
				}
			}

		}
		return convertView;
	}

	public class ViewHolder {
		public TextView tname;
		public TextView post;
		public TextView posttime;
		public LinearLayout intobj;
		public ImageView peerimg;
	}
}
