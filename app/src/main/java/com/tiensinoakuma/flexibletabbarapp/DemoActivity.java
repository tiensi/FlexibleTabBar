package com.tiensinoakuma.flexibletabbarapp;

import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.tiensinoakuma.flexibletabbarapp.databinding.ActivityDemoBinding;
import com.tiensinoakuma.flexibletablayout.FlexibleTabLayout;

public class DemoActivity extends AppCompatActivity implements
    DemoTabFragment.DemoFragmentListener {

  private FlexibleTabLayout tabLayout;
  private boolean fontSwitch;
  private boolean matchTextWidthSwitch;
  private boolean changeStripWidthSwitch;

  private Typeface waltTypeface;
  private int smallWidth;
  private int largeWidth;

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

    smallWidth = 0;
    largeWidth = getResources().getDimensionPixelOffset(R.dimen.spacing_48dp);
  }

  @Override public void matchTextWidth() {
    tabLayout.setMatchTextWidth(matchTextWidthSwitch);
    matchTextWidthSwitch = !matchTextWidthSwitch;
  }

  @Override public void swapFont() {
    tabLayout.setTypeface(fontSwitch ? waltTypeface : Typeface.DEFAULT);
    fontSwitch = !fontSwitch;
  }

  @Override public void swapStripWidth() {
    tabLayout.setStripWidth(changeStripWidthSwitch ? smallWidth : largeWidth);
    changeStripWidthSwitch = !changeStripWidthSwitch;
  }
}
