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
import java.util.List;
import java.util.Set;

import com.peerfinity.appcast.lib.client.PFClientRequester;
import com.peerfinity.appcast.lib.client.commands.GetProfile;
import com.peerfinity.appcast.lib.client.commands.PFCommand;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class UserAdapter extends BaseAdapter {
	Set<String> mUserlist = null;
	private final Chat mContext;
	private PFClientRequester pfcache;

	public UserAdapter(Chat c, PFClientRequester pfc, Set<String> userlist) {
		mUserlist = userlist;
		mContext = c;
		pfcache = pfc;
	}

	public int getCount() {
		if(mUserlist == null)
			return 0;
		return mUserlist.size();
	}

	public Object getItem(int position) {
		List<String> tmp = new ArrayList<String>(mUserlist);
		return tmp.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {

		View view = null;
		final String name = (String)getItem(position);
		LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = vi.inflate(R.layout.chat_adapter_user, null);

		final TextView tname = (TextView) view.findViewById(R.id.PeerName);
		final ImageView peerimg = (ImageView) view.findViewById(R.id.PeerImage);
		tname.setTextAppearance(mContext, android.R.style.TextAppearance_Medium_Inverse);

		pfcache.execute(
				new GetProfile(name, 48, 48, mContext),
				new GetProfile.GetProfileReply() {
					private ImageView pimg = peerimg;
					private TextView ptv = tname;

					@Override
					public void onException(PFCommand arg0, Exception e) {
						e.printStackTrace();
					}
					
					@Override
					public void onProfile(GetProfile arg0, boolean arg1) {
						if(arg0.profile == null)
							return;
						if(arg0.profile.getProfilePic() != null)
							pimg.setImageBitmap(arg0.profile.getProfilePic());
						else
							pimg.setImageResource(R.drawable.profilepic);
						ptv.setText(arg0.profile.getNickName());
					}
				}, false);
		return view;
	}
}
