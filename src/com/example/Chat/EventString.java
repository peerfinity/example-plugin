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

import com.peerfinity.appcast.lib.common.PFEvent;
import com.peerfinity.appcast.lib.common.PFObject;


public class EventString extends PFEvent {

	private PFObject object = null;

	public EventString(PFEvent e) {
		super(e);
	}
	public EventString(int ty, String app, String uuid, String user, long published, int r) {
		super(ty, app, uuid, user, published, r);
	}

	public String getText() {
		if(object == null)
			return null;
		if(object.getData() == null)
			return null;
		return new String(object.getData());
	}
	public List<PFEvent> getRelated() {
		if(object == null)
			return null;
		return object.getRelated();
	}
	
	public void setObject(PFObject object) {
		this.object = object;
	}

	public PFObject getObject() {
		return object;
	}
	
	public static ArrayList<PFEvent> genPFEventArrayList(List<EventString> l){
		ArrayList<PFEvent> al = new ArrayList<PFEvent>();
		for(EventString e : l)
			al.add(new PFEvent(e));
		return al;
	}
}
