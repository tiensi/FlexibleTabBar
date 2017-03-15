package com.tiensinoakuma.flexibletabbarapp;

import android.content.res.Resources;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class DemoPagerAdapter extends FragmentPagerAdapter {

  private final String[] tabTitles;

  public DemoPagerAdapter(final FragmentManager fm, final Resources resources) {
    super(fm);
    tabTitles = new String[] {
        resources.getString(R.string.tab_one),
        resources.getString(R.string.tab_two)
    };
  }

  @Override public int getCount() {
    return tabTitles.length;
  }

  @Override public DemoTabFragment getItem(final int position) {
    return DemoTabFragment.getInstance();
  }

  @Override public CharSequence getPageTitle(final int position) {
    return tabTitles[position];
  }
}
