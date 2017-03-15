package com.tiensinoakuma.flexibletabbarapp;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

  FlexibleTabLayout mTabLayout;
  ViewPager mViewPager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mTabLayout = (FlexibleTabLayout) findViewById(R.id.tab_layout);
    mViewPager = (ViewPager) findViewById(R.id.view_pager);

    mTabLayout.setupWithViewPager(mViewPager);
    mViewPager.setAdapter(
        new DemoPagerAdapter(
            getSupportFragmentManager(),
            getResources()
        )
    );
  }
}
