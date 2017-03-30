package com.tiensinoakuma.flexibletabbarapp;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.tiensinoakuma.flexibletabbarapp.databinding.FragmentDemoBinding;

/**
 * Demo fragment with buttons to demonstrate use of flexible tab layout
 */
public class DemoTabFragment extends Fragment {

  private FragmentDemoBinding binding;
  private DemoFragmentListener listener;

  public static DemoTabFragment getInstance() {
    return new DemoTabFragment();
  }

  public void setListener(DemoFragmentListener listener) {
    this.listener = listener;
  }

  @Nullable @Override public View onCreateView(
      final LayoutInflater inflater,
      @Nullable final ViewGroup container,
      @Nullable final Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.fragment_demo, container, false);
    binding = DataBindingUtil.bind(view);

    binding.btnChangeFont.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(final View v) {
        if (listener != null) {
          listener.swapFont();
        }
      }
    });

    binding.btnChangeWidthText.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(final View v) {
        if (listener != null) {
          listener.matchTextWidth();
        }
      }
    });

    binding.btnChangeWidth.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(final View v) {
        if (listener != null) {
          listener.swapStripWidth();
        }
      }
    });
    return view;
  }


  interface DemoFragmentListener {
    void matchTextWidth();

    void swapFont();

    void swapStripWidth();
  }
}
