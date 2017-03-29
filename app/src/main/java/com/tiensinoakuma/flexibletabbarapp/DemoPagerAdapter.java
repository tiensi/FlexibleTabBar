package com.tiensinoakuma.flexibletabbarapp;

import android.content.res.Resources;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class DemoPagerAdapter extends FragmentPagerAdapter {

  private final String[] tabTitles;
  private DemoTabFragment.DemoFragmentListener listener;

  public DemoPagerAdapter(final FragmentManager fm, final Resources resources, final
      DemoTabFragment.DemoFragmentListener listener) {
    super(fm);
    tabTitles = new String[] {
        resources.getString(R.string.tab_one),
        resources.getString(R.string.tab_two)
    };
    this.listener = listener;
  }

  @Override public int getCount() {
    return tabTitles.length;
  }

  @Override public DemoTabFragment getItem(final int position) {
    DemoTabFragment fragment = DemoTabFragment.getInstance();
    fragment.setListener(listener);
    return fragment;
  }

  @Override public CharSequence getPageTitle(final int position) {
    return tabTitles[position];
  }
}
