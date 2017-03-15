package com.tiensinoakuma.flexibletabbarapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Demo fragment with buttons to demonstrate use of flexible tab layout
 */
public class DemoTabFragment extends Fragment {

  public static DemoTabFragment getInstance() {
    return new DemoTabFragment();
  }

  @Nullable @Override public View onCreateView(
      final LayoutInflater inflater,
      @Nullable final ViewGroup container,
      @Nullable final Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.fragment_demo, container, false);
    return view;
  }
}
