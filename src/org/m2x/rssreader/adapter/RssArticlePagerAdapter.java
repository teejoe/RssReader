/**
 * RssReader
 *
 * Copyright (c) 2013-2014 teejoe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.m2x.rssreader.adapter;

import java.lang.ref.WeakReference;

import org.m2x.rssreader.fragment.RssArticleFragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

public class RssArticlePagerAdapter extends FragmentStatePagerAdapter {	

	private long[] mRssEntryIds;
	private SparseArray<WeakReference<Fragment>> mRegisterdFragments = 
			new SparseArray<WeakReference<Fragment>>();
	
	public RssArticlePagerAdapter(FragmentManager fm, long[] rssEntryIds) {
		super(fm);
		mRssEntryIds = rssEntryIds;
	}
	
	@Override
	public int getCount() {
		return mRssEntryIds.length;
	}

	@Override
	public Fragment getItem(int position) {
		return RssArticleFragment.newInstance(mRssEntryIds[position]);
	}
    
    @Override
    public Object instantiateItem(ViewGroup container, int position){
    	Fragment fragment = (Fragment) super.instantiateItem(container, position);
        WeakReference<Fragment> rFragment = new WeakReference<Fragment>(fragment);
        mRegisterdFragments.put(position, rFragment);
        return fragment;
    }
        
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		mRegisterdFragments.remove(position);
	    super.destroyItem(container, position, object);
	}

    public Fragment getRegisteredFragment(int position) {
    	WeakReference<Fragment> fragment = mRegisterdFragments.get(position);
        if (fragment != null)
        	return fragment.get();
        
        return null;
    }   
}
