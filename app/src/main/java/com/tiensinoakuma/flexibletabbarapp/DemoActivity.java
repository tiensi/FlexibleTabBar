package com.tiensinoakuma.flexibletabbarapp;

import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.tiensinoakuma.flexibletabbarapp.databinding.ActivityDemoBinding;

public class DemoActivity extends AppCompatActivity implements
    DemoTabFragment.DemoFragmentListener {

  private FlexibleTabLayout tabLayout;
  //Used to switch between fonts
  private boolean fontSwitch;
  private boolean matchTextWidthSwitch;

  private Typeface waltTypeface;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final ActivityDemoBinding binding =
        DataBindingUtil.setContentView(DemoActivity.this, R.layout.activity_demo);
    tabLayout = binding.tabLayout;
    tabLayout.setupWithViewPager(binding.viewPager);
    binding.viewPager.setAdapter(
        new DemoPagerAdapter(
            getSupportFragmentManager(),
            getResources(),
            DemoActivity.this
        )
    );

    this.waltTypeface = Typeface.createFromAsset(
        getAssets(), getString(R.string.waltograph));
  }

  @Override public void matchTextWidth() {
    tabLayout.setMatchTextWidth(matchTextWidthSwitch);
    matchTextWidthSwitch = !matchTextWidthSwitch;
  }

  @Override public void swapFont() {
    tabLayout.setTypeface(fontSwitch ? waltTypeface : Typeface.DEFAULT);
    fontSwitch = !fontSwitch;
  }


}
