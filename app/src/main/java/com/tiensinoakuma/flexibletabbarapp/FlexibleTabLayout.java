package com.tiensinoakuma.flexibletabbarapp;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.util.Pools;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.content.res.AppCompatResources;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Interpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import static android.support.v4.view.ViewPager.SCROLL_STATE_DRAGGING;
import static android.support.v4.view.ViewPager.SCROLL_STATE_IDLE;
import static android.support.v4.view.ViewPager.SCROLL_STATE_SETTLING;

/**
 * Composite view with all methods taken from the TabLayout view. Only visual difference is that
 * the
 * width of the sliding tab bar now matches the width of the textview inside instead of the width
 * that the tab fills. Because designers.
 */
// CHECKSTYLE:OFF
@ViewPager.DecorView
public class FlexibleTabLayout extends HorizontalScrollView {

  /**
   * Scrollable tabs display a subset of tabs at any given moment, and can contain longer tab
   * labels and a larger number of tabs. They are best used for browsing contexts in touch
   * interfaces when users don’t need to directly compare the tab labels.
   *
   * @see #setTabMode(int)
   * @see #getTabMode()
   */
  public static final int MODE_SCROLLABLE = 0;
  /**
   * Fixed tabs display all tabs concurrently and are best used with content that benefits from
   * quick pivots between tabs. The maximum number of tabs is limited by the view’s width.
   * Fixed tabs have equal width, based on the widest tab label.
   *
   * @see #setTabMode(int)
   * @see #getTabMode()
   */
  public static final int MODE_FIXED = 1;
  /**
   * Gravity used to fill the {@link FlexibleTabLayout} as much as possible. This option only takes
   * effect
   * when used with {@link #MODE_FIXED}.
   *
   * @see #setTabGravity(int)
   * @see #getTabGravity()
   */
  public static final int GRAVITY_FILL = 0;
  /**
   * Gravity used to lay out the tabs in the center of the {@link FlexibleTabLayout}.
   *
   * @see #setTabGravity(int)
   * @see #getTabGravity()
   */
  public static final int GRAVITY_CENTER = 1;
  public static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutSlowInInterpolator();
  static final int DEFAULT_GAP_TEXT_ICON = 8; // dps
  static final int FIXED_WRAP_GUTTER_MIN = 16; //dps
  static final int MOTION_NON_ADJACENT_OFFSET = 24;
  private static final int DEFAULT_HEIGHT_WITH_TEXT_ICON = 72; // dps
  private static final int INVALID_WIDTH = -1;
  private static final int DEFAULT_HEIGHT = 48; // dps
  private static final int TAB_MIN_WIDTH_MARGIN = 56; //dps
  private static final int ANIMATION_DURATION = 300;
  private static final Pools.Pool<FlexibleTabLayout.Tab> sTabPool =
      new Pools.SynchronizedPool<>(16);
  final int mTabBackgroundResId;
  private final int[] APPCOMPAT_CHECK_ATTRS = {
      android.support.v7.appcompat.R.attr.colorPrimary
  };
  private final ArrayList<FlexibleTabLayout.Tab> mTabs = new ArrayList<>();
  private final int mRequestedTabMinWidth;
  private final int mRequestedTabMaxWidth;
  private final int mScrollableTabMinWidth;
  private final ArrayList<FlexibleTabLayout.OnTabSelectedListener> mSelectedListeners =
      new ArrayList<>();
  // Pool we use as a simple RecyclerBin
  private final Pools.Pool<FlexibleTabLayout.TabView> mTabViewPool = new Pools.SimplePool<>(12);
  //
  private FlexibleTabLayout.GroSlidingTabStrip mTabStrip;
  int mTabPaddingStart;
  int mTabPaddingTop;
  int mTabPaddingEnd;
  int mTabPaddingBottom;
  int mTabTextAppearance;
  ColorStateList mTabTextColors;
  float mTabTextSize;
  float mTabTextMultiLineSize;
  int mTabMaxWidth = Integer.MAX_VALUE / 2;
  int mTabGravity;
  int mMode;
  ViewPager mViewPager;
  //FlexibleTabLayout Additions
  //Used to set fonts of textviews involved
  private Typeface mTypeface;
  private FlexibleTabLayout.Tab mSelectedTab;
  private int mContentInsetStart;
  private FlexibleTabLayout.OnTabSelectedListener mSelectedListener;
  private FlexibleTabLayout.OnTabSelectedListener mCurrentVpSelectedListener;
  private ValueAnimator mScrollAnimator;
  private PagerAdapter mPagerAdapter;
  private DataSetObserver mPagerAdapterObserver;
  private FlexibleTabLayout.TabLayoutOnPageChangeListener
      mPageChangeListener;
  private FlexibleTabLayout.AdapterChangeListener mAdapterChangeListener;
  private boolean mSetupViewPagerImplicitly;

  public FlexibleTabLayout(final Context context) {
    this(context, null);
  }

  public FlexibleTabLayout(final Context context, final AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public FlexibleTabLayout(
      final Context context,
      final AttributeSet attrs,
      final int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    checkAppCompatTheme(context);

    // Disable the Scroll Bar
    setHorizontalScrollBarEnabled(false);

    // Add the TabStrip
    //todo see if custom is possible? by moving this to a method that can be set default in tab lyaout
    mTabStrip = new FlexibleTabLayout.GroSlidingTabStrip(context);
    super.addView(mTabStrip, 0, new HorizontalScrollView.LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

    final TypedArray a =
        context.obtainStyledAttributes(attrs, android.support.design.R.styleable.TabLayout,
            defStyleAttr, android.support.design.R.style.Widget_Design_TabLayout
        );

    //Custom TypedArray attrs for FlexibleTabLayout
    final TypedArray flexibleTabTypedArray =
        context.obtainStyledAttributes(attrs, R.styleable.FlexibleTabLayout);

    //Todo once Android O comes out fontFamily will be a valid param
    mTypeface = Typeface.createFromAsset(
        getContext().getAssets(),
        flexibleTabTypedArray.getString(R.styleable.FlexibleTabLayout_fontFamilyPath)
    );

    mTabStrip.setSelectedIndicatorWidth(
        flexibleTabTypedArray.getDimensionPixelSize(R.styleable.FlexibleTabLayout_stripWidth, 0)
    );

    mTabStrip.setMatchTextWidth(flexibleTabTypedArray.getBoolean(
          R.styleable.FlexibleTabLayout_stripMatchTextWidth,
          false
        )
    );

    flexibleTabTypedArray.recycle();

    //End of custom attrs for FlexibleTabLayout

    mTabStrip.setSelectedIndicatorHeight(
        a.getDimensionPixelSize(
            android.support.design.R.styleable.TabLayout_tabIndicatorHeight,
            0
        ));
    mTabStrip.setSelectedIndicatorColor(a.getColor(
        android.support.design.R.styleable.TabLayout_tabIndicatorColor,
        0
    ));

    mTabPaddingStart = mTabPaddingTop = mTabPaddingEnd = mTabPaddingBottom = a
        .getDimensionPixelSize(android.support.design.R.styleable.TabLayout_tabPadding, 0);
    mTabPaddingStart = a.getDimensionPixelSize(
        android.support.design.R.styleable.TabLayout_tabPaddingStart,
        mTabPaddingStart
    );
    mTabPaddingTop = a.getDimensionPixelSize(
        android.support.design.R.styleable.TabLayout_tabPaddingTop,
        mTabPaddingTop
    );
    mTabPaddingEnd = a.getDimensionPixelSize(
        android.support.design.R.styleable.TabLayout_tabPaddingEnd,
        mTabPaddingEnd
    );
    mTabPaddingBottom = a.getDimensionPixelSize(
        android.support.design.R.styleable.TabLayout_tabPaddingBottom,
        mTabPaddingBottom
    );

    mTabTextAppearance = a.getResourceId(
        android.support.design.R.styleable.TabLayout_tabTextAppearance,
        android.support.design.R.style.TextAppearance_Design_Tab
    );

    // Text colors/sizes come from the text appearance first
    final TypedArray ta = context.obtainStyledAttributes(
        mTabTextAppearance,
        android.support.v7.appcompat.R.styleable.TextAppearance
    );
    try {
      mTabTextSize = ta.getDimensionPixelSize(
          android.support.v7.appcompat.R.styleable.TextAppearance_android_textSize, 0);
      mTabTextColors = ta.getColorStateList(
          android.support.v7.appcompat.R.styleable.TextAppearance_android_textColor);
    } finally {
      ta.recycle();
    }

    if (a.hasValue(android.support.design.R.styleable.TabLayout_tabTextColor)) {
      // If we have an explicit text color set, use it instead
      mTabTextColors =
          a.getColorStateList(android.support.design.R.styleable.TabLayout_tabTextColor);
    }

    if (a.hasValue(android.support.design.R.styleable.TabLayout_tabSelectedTextColor)) {
      // We have an explicit selected text color set, so we need to make merge it with the
      // current colors. This is exposed so that developers can use theme attributes to set
      // this (theme attrs in ColorStateLists are Lollipop+)
      final int selected =
          a.getColor(android.support.design.R.styleable.TabLayout_tabSelectedTextColor, 0);
      mTabTextColors = createColorStateList(mTabTextColors.getDefaultColor(), selected);
    }

    mRequestedTabMinWidth = a.getDimensionPixelSize(
        android.support.design.R.styleable.TabLayout_tabMinWidth,
        INVALID_WIDTH
    );
    mRequestedTabMaxWidth = a.getDimensionPixelSize(
        android.support.design.R.styleable.TabLayout_tabMaxWidth,
        INVALID_WIDTH
    );
    mTabBackgroundResId =
        a.getResourceId(android.support.design.R.styleable.TabLayout_tabBackground, 0);
    mContentInsetStart =
        a.getDimensionPixelSize(android.support.design.R.styleable.TabLayout_tabContentStart, 0);
    mMode = a.getInt(android.support.design.R.styleable.TabLayout_tabMode, MODE_FIXED);
    mTabGravity = a.getInt(android.support.design.R.styleable.TabLayout_tabGravity, GRAVITY_FILL);
    a.recycle();

    final Resources res = getResources();
    mTabTextMultiLineSize =
        res.getDimensionPixelSize(android.support.design.R.dimen.design_tab_text_size_2line);
    mScrollableTabMinWidth =
        res.getDimensionPixelSize(android.support.design.R.dimen.design_tab_scrollable_min_width);

    // Now apply the tab mode and gravity
    applyModeAndGravity();
  }

  private static ColorStateList createColorStateList(
      final int defaultColor,
      final int selectedColor) {
    final int[][] states = new int[2][];
    final int[] colors = new int[2];
    int i = 0;

    states[i] = SELECTED_STATE_SET;
    colors[i] = selectedColor;
    i++;

    // Default enabled state
    states[i] = EMPTY_STATE_SET;
    colors[i] = defaultColor;
    i++;

    return new ColorStateList(states, colors);
  }

  /**
   * Linear interpolation between {@code startValue} and {@code endValue} by {@code fraction}.
   */

  public static int lerp(final int startValue, final int endValue, final float fraction) {
    return startValue + Math.round(fraction * (endValue - startValue));
  }

  @Override
  public void addView(final View child) {
    addViewInternal(child);
  }

  @Override
  public void addView(final View child, final int index) {
    addViewInternal(child);
  }

  @Override
  public void addView(final View child, final ViewGroup.LayoutParams params) {
    addViewInternal(child);
  }

  @Override
  public void addView(final View child, final int index, final ViewGroup.LayoutParams params) {
    addViewInternal(child);
  }

  @Override
  public LayoutParams generateLayoutParams(final AttributeSet attrs) {
    // We don't care about the layout params of any views added to us, since we don't actually
    // add them. The only view we add is the SlidingTabStrip, which is done manually.
    // We return the default layout params so that we don't blow up if we're given a TabItem
    // without android:layout_* values.
    return generateDefaultLayoutParams();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    if (mViewPager == null) {
      // If we don't have a ViewPager already, check if our parent is a ViewPager to
      // setup with it automatically
      final ViewParent vp = getParent();
      if (vp instanceof ViewPager) {
        // If we have a ViewPager parent and we've been added as part of its decor, let's
        // assume that we should automatically setup to display any titles
        setupWithViewPager((ViewPager) vp, true, true);
      }
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    if (mSetupViewPagerImplicitly) {
      // If we've been setup with a ViewPager implicitly, let's clear out any listeners, etc
      setupWithViewPager(null);
      mSetupViewPagerImplicitly = false;
    }
  }

  @Override
  protected void onMeasure(final int widthMeasureSpec, int heightMeasureSpec) {
    // If we have a MeasureSpec which allows us to decide our height, try and use the default
    // height
    final int idealHeight = dpToPx(getDefaultHeight()) + getPaddingTop() + getPaddingBottom();
    switch (MeasureSpec.getMode(heightMeasureSpec)) {
      case MeasureSpec.AT_MOST:
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(
            Math.min(idealHeight, MeasureSpec.getSize(heightMeasureSpec)),
            MeasureSpec.EXACTLY
        );
        break;
      case MeasureSpec.UNSPECIFIED:
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(idealHeight, MeasureSpec.EXACTLY);
        break;
    }

    final int specWidth = MeasureSpec.getSize(widthMeasureSpec);
    if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
      // If we don't have an unspecified width spec, use the given size to calculate
      // the max tab width
      mTabMaxWidth = mRequestedTabMaxWidth > 0
                     ? mRequestedTabMaxWidth
                     : specWidth - dpToPx(TAB_MIN_WIDTH_MARGIN);
    }

    // Now super measure itself using the (possibly) modified height spec
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    if (getChildCount() == 1) {
      // If we're in fixed mode then we need to make the tab strip is the same width as us
      // so we don't scroll
      final View child = getChildAt(0);
      boolean remeasure = false;

      switch (mMode) {
        case MODE_SCROLLABLE:
          // We only need to resize the child if it's smaller than us. This is similar
          // to fillViewport
          remeasure = child.getMeasuredWidth() < getMeasuredWidth();
          break;
        case MODE_FIXED:
          // Resize the child so that it doesn't scroll
          remeasure = child.getMeasuredWidth() != getMeasuredWidth();
          break;
      }

      if (remeasure) {
        // Re-measure the child with a widthSpec set to be exactly our measure width
        final int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop()
            + getPaddingBottom(), child.getLayoutParams().height);
        final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
            getMeasuredWidth(), MeasureSpec.EXACTLY);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
      }
    }
  }

  @Override
  public boolean shouldDelayChildPressedState() {
    // Only delay the pressed state if the tabs can scroll
    return getTabScrollRange() > 0;
  }

  /**
   * Add a {@link FlexibleTabLayout.OnTabSelectedListener} that will be invoked when tab selection
   * changes.
   *
   * <p>Components that add a listener should take care to remove it when finished via
   * {@link #removeOnTabSelectedListener(FlexibleTabLayout.OnTabSelectedListener)}.</p>
   *
   * @param listener listener to add
   */
  public void addOnTabSelectedListener(
      @NonNull final FlexibleTabLayout.OnTabSelectedListener
          listener) {
    if (!mSelectedListeners.contains(listener)) {
      mSelectedListeners.add(listener);
    }
  }

  /**
   * Add a tab to this layout. The tab will be added at the end of the list.
   * If this is the first tab to be added it will become the selected tab.
   *
   * @param tab Tab to add
   */
  public void addTab(@NonNull final FlexibleTabLayout.Tab tab) {
    addTab(tab, mTabs.isEmpty());
  }

  /**
   * Add a tab to this layout. The tab will be inserted at <code>position</code>.
   * If this is the first tab to be added it will become the selected tab.
   *
   * @param tab The tab to add
   * @param position The new position of the tab
   */
  public void addTab(@NonNull final FlexibleTabLayout.Tab tab, final int position) {
    addTab(tab, position, mTabs.isEmpty());
  }

  /**
   * Add a tab to this layout. The tab will be added at the end of the list.
   *
   * @param tab Tab to add
   * @param setSelected True if the added tab should become the selected tab.
   */
  public void addTab(@NonNull final FlexibleTabLayout.Tab tab, final boolean setSelected) {
    addTab(tab, mTabs.size(), setSelected);
  }

  /**
   * Add a tab to this layout. The tab will be inserted at <code>position</code>.
   *
   * @param tab The tab to add
   * @param position The new position of the tab
   * @param setSelected True if the added tab should become the selected tab.
   */
  public void addTab(
      @NonNull final FlexibleTabLayout.Tab tab,
      final int position,
      final boolean setSelected) {
    if (tab.mParent != this) {
      throw new IllegalArgumentException("Tab belongs to a different TabLayout.");
    }
    configureTab(tab, position);
    addTabView(tab);

    if (setSelected) {
      tab.select();
    }
  }

  public void checkAppCompatTheme(final Context context) {
    final TypedArray a = context.obtainStyledAttributes(APPCOMPAT_CHECK_ATTRS);
    final boolean failed = !a.hasValue(0);
    if (a != null) {
      a.recycle();
    }
    if (failed) {
      throw new IllegalArgumentException("You need to use a Theme.AppCompat theme "
          + "(or descendant) with the design library.");
    }
  }

  /**
   * Remove all previously added {@link FlexibleTabLayout.OnTabSelectedListener}s.
   */
  public void clearOnTabSelectedListeners() {
    mSelectedListeners.clear();
  }

  /**
   * Returns the position of the current selected tab.
   *
   * @return selected tab position, or {@code -1} if there isn't a selected tab.
   */
  public int getSelectedTabPosition() {
    return mSelectedTab != null ? mSelectedTab.getPosition() : -1;
  }

  /**
   * Returns the tab at the specified index.
   */
  @Nullable
  public FlexibleTabLayout.Tab getTabAt(final int index) {
    return (index < 0 || index >= getTabCount()) ? null : mTabs.get(index);
  }

  /**
   * Returns the number of tabs currently registered with the action bar.
   *
   * @return Tab count
   */
  public int getTabCount() {
    return mTabs.size();
  }

  /**
   * The current gravity used for laying out tabs.
   *
   * @return one of {@link #GRAVITY_CENTER} or {@link #GRAVITY_FILL}.
   */
  @FlexibleTabLayout.TabGravity
  public int getTabGravity() {
    return mTabGravity;
  }

  /**
   * Changes tab layout strip to match width of textviews
   * @param matchTextWidth
   */
  public void setMatchTextWidth(final boolean matchTextWidth) {
    mTabStrip.setMatchTextWidth(matchTextWidth);
    updateAllTabs();
  }

  /**
   * Set the gravity to use when laying out the tabs.
   *
   * @param gravity one of {@link #GRAVITY_CENTER} or {@link #GRAVITY_FILL}.
   * @attr ref android.support.design.R.styleable#TabLayout_tabGravity
   */
  public void setTabGravity(@FlexibleTabLayout.TabGravity final int gravity) {
    if (mTabGravity != gravity) {
      mTabGravity = gravity;
      applyModeAndGravity();
    }
  }

  /**
   * Returns the current mode used by this {@link FlexibleTabLayout}.
   *
   * @see #setTabMode(int)
   */
  @FlexibleTabLayout.Mode
  public int getTabMode() {
    return mMode;
  }

  /**
   * Set the behavior mode for the Tabs in this layout. The valid input options are:
   * <ul>
   * <li>{@link #MODE_FIXED}: Fixed tabs display all tabs concurrently and are best used
   * with content that benefits from quick pivots between tabs.</li>
   * <li>{@link #MODE_SCROLLABLE}: Scrollable tabs display a subset of tabs at any given moment,
   * and can contain longer tab labels and a larger number of tabs. They are best used for
   * browsing contexts in touch interfaces when users don’t need to directly compare the tab
   * labels. This mode is commonly used with a {@link android.support.v4.view.ViewPager}.</li>
   * </ul>
   *
   * @param mode one of {@link #MODE_FIXED} or {@link #MODE_SCROLLABLE}.
   * @attr ref android.support.design.R.styleable#TabLayout_tabMode
   */
  public void setTabMode(@FlexibleTabLayout.Mode final int mode) {
    if (mode != mMode) {
      mMode = mode;
      applyModeAndGravity();
    }
  }

  /**
   * Gets the text colors for the different states (normal, selected) used for the tabs.
   */
  @Nullable
  public ColorStateList getTabTextColors() {
    return mTabTextColors;
  }

  /**
   * Sets the text colors for the different states (normal, selected) used for the tabs.
   *
   * @see #getTabTextColors()
   */
  public void setTabTextColors(@Nullable final ColorStateList textColor) {
    if (mTabTextColors != textColor) {
      mTabTextColors = textColor;
      updateAllTabs();
    }
  }

  /**
   * Sets TabStrip Width for all tabs being used
   *
   * @param width
   */
  public void setStripWidth(final int width) {
    mTabStrip.setSelectedIndicatorWidth(width);
  }

  /**
   * Sets fonts with given typeface for textviews being used
   * @param typeface
   */
  public void setTypeface(final Typeface typeface) {
    if (typeface != null && !typeface.equals(mTypeface)) {
      mTypeface = typeface;
      updateAllTabs();
    }
  }

  /**
   * Create and return a new {@link FlexibleTabLayout.Tab}. You need to manually add this using
   * {@link #addTab(FlexibleTabLayout.Tab)} or a related method.
   *
   * @return A new Tab
   * @see #addTab(FlexibleTabLayout.Tab)
   */
  @NonNull
  public FlexibleTabLayout.Tab newTab() {
    FlexibleTabLayout.Tab tab = sTabPool.acquire();
    if (tab == null) {
      tab = new FlexibleTabLayout.Tab();
    }
    tab.mParent = this;
    tab.mView = createTabView(tab);
    return tab;
  }

  /**
   * Remove all tabs from the action bar and deselect the current tab.
   */
  public void removeAllTabs() {
    // Remove all the views
    for (int i = mTabStrip.getChildCount() - 1; i >= 0; i--) {
      removeTabViewAt(i);
    }

    for (final Iterator<FlexibleTabLayout.Tab> i = mTabs.iterator(); i.hasNext(); ) {
      final FlexibleTabLayout.Tab tab = i.next();
      i.remove();
      tab.reset();
      sTabPool.release(tab);
    }

    mSelectedTab = null;
  }

  /**
   * Remove the given {@link FlexibleTabLayout.OnTabSelectedListener} that was previously added via
   * {@link #addOnTabSelectedListener(FlexibleTabLayout.OnTabSelectedListener)}.
   *
   * @param listener listener to remove
   */
  public void removeOnTabSelectedListener(
      @NonNull final FlexibleTabLayout.OnTabSelectedListener
          listener) {
    mSelectedListeners.remove(listener);
  }

  /**
   * Remove a tab from the layout. If the removed tab was selected it will be deselected
   * and another tab will be selected if present.
   *
   * @param tab The tab to remove
   */
  public void removeTab(FlexibleTabLayout.Tab tab) {
    if (tab.mParent != this) {
      throw new IllegalArgumentException("Tab does not belong to this TabLayout.");
    }

    removeTabAt(tab.getPosition());
  }

  /**
   * Remove a tab from the layout. If the removed tab was selected it will be deselected
   * and another tab will be selected if present.
   *
   * @param position Position of the tab to remove
   */
  public void removeTabAt(final int position) {
    final int selectedTabPosition = mSelectedTab != null ? mSelectedTab.getPosition() : 0;
    removeTabViewAt(position);

    final FlexibleTabLayout.Tab removedTab = mTabs.remove(position);
    if (removedTab != null) {
      removedTab.reset();
      sTabPool.release(removedTab);
    }

    final int newTabCount = mTabs.size();
    for (int i = position; i < newTabCount; i++) {
      mTabs.get(i).setPosition(i);
    }

    if (selectedTabPosition == position) {
      selectTab(mTabs.isEmpty() ? null : mTabs.get(Math.max(0, position - 1)));
    }
  }

  /**
   * @deprecated Use {@link #addOnTabSelectedListener(FlexibleTabLayout.OnTabSelectedListener)} and
   * {@link #removeOnTabSelectedListener(FlexibleTabLayout.OnTabSelectedListener)}.
   */
  @Deprecated
  public void setOnTabSelectedListener(
      @Nullable final FlexibleTabLayout.OnTabSelectedListener
          listener) {

    // The logic in this method emulates what we had before support for multiple
    // registered listeners.
    if (mSelectedListener != null) {
      removeOnTabSelectedListener(mSelectedListener);
    }
    // Update the deprecated field so that we can remove the passed listener the next
    // time we're called
    mSelectedListener = listener;
    if (listener != null) {
      addOnTabSelectedListener(listener);
    }
  }

  /**
   * Set the scroll position of the tabs. This is useful for when the tabs are being displayed as
   * part of a scrolling container such as {@link android.support.v4.view.ViewPager}.
   * <p>
   * Calling this method does not update the selected tab, it is only used for drawing purposes.
   *
   * @param position current scroll position
   * @param positionOffset Value from [0, 1) indicating the offset from {@code position}.
   * @param updateSelectedText Whether to update the text's selected state.
   */
  public void setScrollPosition(
      final int position,
      final float positionOffset,
      final boolean updateSelectedText) {
    setScrollPosition(position, positionOffset, updateSelectedText, true);
  }

  /**
   * Sets the tab indicator's color for the currently selected tab.
   *
   * @param color color to use for the indicator
   * @attr ref android.support.design.R.styleable#TabLayout_tabIndicatorColor
   */
  public void setSelectedTabIndicatorColor(@ColorInt final int color) {
    mTabStrip.setSelectedIndicatorColor(color);
  }

  /**
   * Sets the tab indicator's height for the currently selected tab.
   *
   * @param height height to use for the indicator in pixels
   * @attr ref android.support.design.R.styleable#TabLayout_tabIndicatorHeight
   */
  public void setSelectedTabIndicatorHeight(final int height) {
    mTabStrip.setSelectedIndicatorHeight(height);
  }

  /**
   * Sets the text colors for the different states (normal, selected) used for the tabs.
   *
   * @attr ref android.support.design.R.styleable#TabLayout_tabTextColor
   * @attr ref android.support.design.R.styleable#TabLayout_tabSelectedTextColor
   */
  public void setTabTextColors(final int normalColor, final int selectedColor) {
    setTabTextColors(createColorStateList(normalColor, selectedColor));
  }

  /**
   * @deprecated Use {@link #setupWithViewPager(ViewPager)} to link a TabLayout with a ViewPager
   * together. When that method is used, the TabLayout will be automatically updated
   * when the {@link PagerAdapter} is changed.
   */
  @Deprecated
  public void setTabsFromPagerAdapter(@Nullable final PagerAdapter adapter) {
    setPagerAdapter(adapter, false);
  }

  /**
   * The one-stop shop for setting up this {@link FlexibleTabLayout} with a {@link ViewPager}.
   *
   * <p>This is the same as calling {@link #setupWithViewPager(ViewPager, boolean)} with
   * auto-refresh enabled.</p>
   *
   * @param viewPager the ViewPager to link to, or {@code null} to clear any previous link
   */
  public void setupWithViewPager(@Nullable final ViewPager viewPager) {
    setupWithViewPager(viewPager, true);
  }

  /**
   * The one-stop shop for setting up this {@link FlexibleTabLayout} with a {@link ViewPager}.
   *
   * <p>This method will link the given ViewPager and this TabLayout together so that
   * changes in one are automatically reflected in the other. This includes scroll state changes
   * and clicks. The tabs displayed in this layout will be populated
   * from the ViewPager adapter's page titles.</p>
   *
   * <p>If {@code autoRefresh} is {@code true}, any changes in the {@link PagerAdapter} will
   * trigger this layout to re-populate itself from the adapter's titles.</p>
   *
   * <p>If the given ViewPager is non-null, it needs to already have a
   * {@link PagerAdapter} set.</p>
   *
   * @param viewPager the ViewPager to link to, or {@code null} to clear any previous link
   * @param autoRefresh whether this layout should refresh its contents if the given ViewPager's
   * content changes
   */
  public void setupWithViewPager(@Nullable final ViewPager viewPager, final boolean autoRefresh) {
    setupWithViewPager(viewPager, autoRefresh, false);
  }

  int dpToPx(final int dps) {
    return Math.round(getResources().getDisplayMetrics().density * dps);
  }

  int getTabMaxWidth() {
    return mTabMaxWidth;
  }

  void populateFromPagerAdapter() {
    removeAllTabs();

    if (mPagerAdapter != null) {
      final int adapterCount = mPagerAdapter.getCount();
      for (int i = 0; i < adapterCount; i++) {
        addTab(newTab().setText(mPagerAdapter.getPageTitle(i)), false);
      }

      // Make sure we reflect the currently set ViewPager item
      if (mViewPager != null && adapterCount > 0) {
        final int curItem = mViewPager.getCurrentItem();
        if (curItem != getSelectedTabPosition() && curItem < getTabCount()) {
          selectTab(getTabAt(curItem));
        }
      }
    }
  }

  void selectTab(final FlexibleTabLayout.Tab tab) {
    selectTab(tab, true);
  }

  void selectTab(final FlexibleTabLayout.Tab tab, final boolean updateIndicator) {
    final FlexibleTabLayout.Tab currentTab = mSelectedTab;

    if (currentTab == tab) {
      if (currentTab != null) {
        dispatchTabReselected(tab);
        animateToTab(tab.getPosition());
      }
    } else {
      final int newPosition =
          tab != null ? tab.getPosition() : FlexibleTabLayout.Tab.INVALID_POSITION;
      if (updateIndicator) {
        if ((currentTab == null
            || currentTab.getPosition() == FlexibleTabLayout.Tab.INVALID_POSITION)
            && newPosition != FlexibleTabLayout.Tab.INVALID_POSITION) {
          // If we don't currently have a tab, just draw the indicator
          setScrollPosition(newPosition, 0f, true);
        } else {
          animateToTab(newPosition);
        }
        if (newPosition != FlexibleTabLayout.Tab.INVALID_POSITION) {
          setSelectedTabView(newPosition);
        }
      }
      if (currentTab != null) {
        dispatchTabUnselected(currentTab);
      }
      mSelectedTab = tab;
      if (tab != null) {
        dispatchTabSelected(tab);
      }
    }
  }

  void setPagerAdapter(@Nullable final PagerAdapter adapter, final boolean addObserver) {
    if (mPagerAdapter != null && mPagerAdapterObserver != null) {
      // If we already have a PagerAdapter, unregister our observer
      mPagerAdapter.unregisterDataSetObserver(mPagerAdapterObserver);
    }

    mPagerAdapter = adapter;

    if (addObserver && adapter != null) {
      // Register our observer on the new adapter
      if (mPagerAdapterObserver == null) {
        mPagerAdapterObserver = new FlexibleTabLayout.PagerAdapterObserver();
      }
      adapter.registerDataSetObserver(mPagerAdapterObserver);
    }

    // Finally make sure we reflect the new adapter
    populateFromPagerAdapter();
  }

  void setScrollPosition(
      final int position, final float positionOffset, final boolean updateSelectedText,
      final boolean updateIndicatorPosition) {
    final int roundedPosition = Math.round(position + positionOffset);
    if (roundedPosition < 0 || roundedPosition >= mTabStrip.getChildCount()) {
      return;
    }

    // Set the indicator position, if enabled
    if (updateIndicatorPosition) {
      mTabStrip.setIndicatorPositionFromTabPosition(position, positionOffset);
    }

    // Now update the scroll position, canceling any running animation
    if (mScrollAnimator != null && mScrollAnimator.isRunning()) {
      mScrollAnimator.cancel();
    }
    scrollTo(calculateScrollXForTab(position, positionOffset), 0);

    // Update the 'selected state' view as we scroll, if enabled
    if (updateSelectedText) {
      setSelectedTabView(roundedPosition);
    }
  }

  void updateTabViews(final boolean requestLayout) {
    for (int i = 0; i < mTabStrip.getChildCount(); i++) {
      final View child = mTabStrip.getChildAt(i);
      child.setMinimumWidth(getTabMinWidth());
      updateTabViewLayoutParams((LinearLayout.LayoutParams) child.getLayoutParams());
      if (requestLayout) {
        child.requestLayout();
      }
    }
  }

  private void addTabFromItemView(@NonNull final TabItem item) {
    final FlexibleTabLayout.Tab tab = newTab();
    if (item.mText != null) {
      tab.setText(item.mText);
    }
    if (item.mIcon != null) {
      tab.setIcon(item.mIcon);
    }
    if (item.mCustomLayout != 0) {
      tab.setCustomView(item.mCustomLayout);
    }
    if (!TextUtils.isEmpty(item.getContentDescription())) {
      tab.setContentDescription(item.getContentDescription());
    }
    addTab(tab);
  }

  private void addTabView(final FlexibleTabLayout.Tab tab) {
    final FlexibleTabLayout.TabView tabView = tab.mView;
    mTabStrip.addView(tabView, tab.getPosition(), createLayoutParamsForTabs());
  }

  private void addViewInternal(final View child) {
    if (child instanceof TabItem) {
      addTabFromItemView((TabItem) child);
    } else {
      throw new IllegalArgumentException("Only TabItem instances can be added to TabLayout");
    }
  }

  private void animateToTab(final int newPosition) {
    if (newPosition == FlexibleTabLayout.Tab.INVALID_POSITION) {
      return;
    }

    if (getWindowToken() == null || !ViewCompat.isLaidOut(this)
        || mTabStrip.childrenNeedLayout()) {
      // If we don't have a window token, or we haven't been laid out yet just draw the new
      // position now
      setScrollPosition(newPosition, 0f, true);
      return;
    }

    final int startScrollX = getScrollX();
    final int targetScrollX = calculateScrollXForTab(newPosition, 0);

    if (startScrollX != targetScrollX) {
      if (mScrollAnimator == null) {
        mScrollAnimator = new ValueAnimator();
        mScrollAnimator.setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR);
        mScrollAnimator.setDuration(ANIMATION_DURATION);
        mScrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
          @Override public void onAnimationUpdate(final ValueAnimator animation) {

            scrollTo((int) animation.getAnimatedValue(), 0);
          }
        });
      }

      mScrollAnimator.setIntValues(startScrollX, targetScrollX);
      mScrollAnimator.start();
    }

    // Now animate the indicator
    mTabStrip.animateIndicatorToPosition(newPosition, ANIMATION_DURATION);
  }

  private void applyModeAndGravity() {
    int paddingStart = 0;
    if (mMode == MODE_SCROLLABLE) {
      // If we're scrollable, or fixed at start, inset using padding
      paddingStart = Math.max(0, mContentInsetStart - mTabPaddingStart);
    }
    ViewCompat.setPaddingRelative(mTabStrip, paddingStart, 0, 0, 0);

    switch (mMode) {
      case MODE_FIXED:
        mTabStrip.setGravity(Gravity.CENTER_HORIZONTAL);
        break;
      case MODE_SCROLLABLE:
        mTabStrip.setGravity(GravityCompat.START);
        break;
    }

    updateTabViews(true);
  }

  private int calculateScrollXForTab(final int position, final float positionOffset) {
    if (mMode == MODE_SCROLLABLE) {
      final FlexibleTabLayout.TabView selectedChild =
          (FlexibleTabLayout.TabView) mTabStrip.getChildAt(position);
      final View nextChild = position + 1 < mTabStrip.getChildCount()
                             ? mTabStrip.getChildAt(position + 1)
                             : null;
      final int selectedWidth = selectedChild != null ? selectedChild.getWidth() : 0;
      final int nextWidth = nextChild != null ? nextChild.getWidth() : 0;

      assert selectedChild != null;
      return (selectedChild.getLeft())
          + ((int) ((selectedWidth + nextWidth) * positionOffset * 0.5f))
          + (selectedChild.getWidth() / 2)
          - (getWidth() / 2);
    }
    return 0;
  }

  private void configureTab(final FlexibleTabLayout.Tab tab, final int position) {
    tab.setPosition(position);
    mTabs.add(position, tab);

    final int count = mTabs.size();
    for (int i = position + 1; i < count; i++) {
      mTabs.get(i).setPosition(i);
    }
  }

  private LinearLayout.LayoutParams createLayoutParamsForTabs() {
    final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
    updateTabViewLayoutParams(lp);
    return lp;
  }

  private FlexibleTabLayout.TabView createTabView(@NonNull final FlexibleTabLayout.Tab tab) {

    FlexibleTabLayout.TabView tabView = mTabViewPool != null ? mTabViewPool.acquire() : null;
    if (tabView == null) {
      tabView = new FlexibleTabLayout.TabView(getContext());
    }
    tabView.setTab(tab);
    tabView.setFocusable(true);
    tabView.setMinimumWidth(getTabMinWidth());
    return tabView;
  }

  private void dispatchTabReselected(@NonNull final FlexibleTabLayout.Tab tab) {
    for (int i = mSelectedListeners.size() - 1; i >= 0; i--) {
      mSelectedListeners.get(i).onTabReselected(tab);
    }
  }

  private void dispatchTabSelected(@NonNull final FlexibleTabLayout.Tab tab) {
    for (int i = mSelectedListeners.size() - 1; i >= 0; i--) {
      mSelectedListeners.get(i).onTabSelected(tab);
    }
  }

  private void dispatchTabUnselected(@NonNull final FlexibleTabLayout.Tab tab) {
    for (int i = mSelectedListeners.size() - 1; i >= 0; i--) {
      mSelectedListeners.get(i).onTabUnselected(tab);
    }
  }

  private int getDefaultHeight() {
    boolean hasIconAndText = false;
    for (int i = 0, count = mTabs.size(); i < count; i++) {
      final FlexibleTabLayout.Tab tab = mTabs.get(i);
      if (tab != null && tab.getIcon() != null && !TextUtils.isEmpty(tab.getText())) {
        hasIconAndText = true;
        break;
      }
    }
    return hasIconAndText ? DEFAULT_HEIGHT_WITH_TEXT_ICON : DEFAULT_HEIGHT;
  }

  private float getScrollPosition() {
    return mTabStrip.getIndicatorPosition();
  }

  private int getTabMinWidth() {
    if (mRequestedTabMinWidth != INVALID_WIDTH) {
      // If we have been given a min width, use it
      return mRequestedTabMinWidth;
    }
    // Else, we'll use the default value
    return mMode == MODE_SCROLLABLE ? mScrollableTabMinWidth : 0;
  }

  private int getTabScrollRange() {
    return Math.max(0, mTabStrip.getWidth() - getWidth() - getPaddingLeft()
        - getPaddingRight());
  }

  private void removeTabViewAt(final int position) {
    final FlexibleTabLayout.TabView
        view = (FlexibleTabLayout.TabView) mTabStrip.getChildAt(position);
    mTabStrip.removeViewAt(position);
    if (view != null) {
      view.reset();
      mTabViewPool.release(view);
    }
    requestLayout();
  }

  private void setSelectedTabView(final int position) {
    final int tabCount = mTabStrip.getChildCount();
    if (position < tabCount) {
      for (int i = 0; i < tabCount; i++) {
        final View child = mTabStrip.getChildAt(i);
        child.setSelected(i == position);
      }
    }
  }

  private void setupWithViewPager(
      @Nullable final ViewPager viewPager, final boolean autoRefresh,
      final boolean implicitSetup) {
    if (mViewPager != null) {
      // If we've already been setup with a ViewPager, remove us from it
      if (mPageChangeListener != null) {
        mViewPager.removeOnPageChangeListener(mPageChangeListener);
      }
      if (mAdapterChangeListener != null) {
        mViewPager.removeOnAdapterChangeListener(mAdapterChangeListener);
      }
    }

    if (mCurrentVpSelectedListener != null) {
      // If we already have a tab selected listener for the ViewPager, remove it
      removeOnTabSelectedListener(mCurrentVpSelectedListener);
      mCurrentVpSelectedListener = null;
    }

    if (viewPager != null) {
      mViewPager = viewPager;

      // Add our custom OnPageChangeListener to the ViewPager
      if (mPageChangeListener == null) {
        mPageChangeListener = new FlexibleTabLayout.TabLayoutOnPageChangeListener(this);
      }
      mPageChangeListener.reset();
      viewPager.addOnPageChangeListener(mPageChangeListener);

      // Now we'll add a tab selected listener to set ViewPager's current item
      mCurrentVpSelectedListener = new FlexibleTabLayout.ViewPagerOnTabSelectedListener(viewPager);
      addOnTabSelectedListener(mCurrentVpSelectedListener);

      final PagerAdapter adapter = viewPager.getAdapter();
      if (adapter != null) {
        // Now we'll populate ourselves from the pager adapter, adding an observer if
        // autoRefresh is enabled
        setPagerAdapter(adapter, autoRefresh);
      }

      // Add a listener so that we're notified of any adapter changes
      if (mAdapterChangeListener == null) {
        mAdapterChangeListener = new FlexibleTabLayout.AdapterChangeListener();
      }
      mAdapterChangeListener.setAutoRefresh(autoRefresh);
      viewPager.addOnAdapterChangeListener(mAdapterChangeListener);

      // Now update the scroll position to match the ViewPager's current item
      setScrollPosition(viewPager.getCurrentItem(), 0f, true);
    } else {
      // We've been given a null ViewPager so we need to clear out the internal state,
      // listeners and observers
      mViewPager = null;
      setPagerAdapter(null, false);
    }

    mSetupViewPagerImplicitly = implicitSetup;
  }

  private void updateAllTabs() {
    for (int i = 0, z = mTabs.size(); i < z; i++) {
      mTabs.get(i).updateView();
    }
  }

  private void updateTabViewLayoutParams(final LinearLayout.LayoutParams lp) {
    if (mMode == MODE_FIXED && mTabGravity == GRAVITY_FILL) {
      lp.width = 0;
      lp.weight = 1;
    } else {
      lp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
      lp.weight = 0;
    }
  }

  /**
   * @hide
   */
  @IntDef(value = { MODE_SCROLLABLE, MODE_FIXED })
  @Retention(RetentionPolicy.SOURCE)
  public @interface Mode {
  }

  /**
   * Callback interface invoked when a tab's selection state changes.
   */
  public interface OnTabSelectedListener {

    /**
     * Called when a tab that is already selected is chosen again by the user. Some applications
     * may use this action to return to the top level of a category.
     *
     * @param tab The tab that was reselected.
     */
    public void onTabReselected(FlexibleTabLayout.Tab tab);

    /**
     * Called when a tab enters the selected state.
     *
     * @param tab The tab that was selected
     */
    public void onTabSelected(FlexibleTabLayout.Tab tab);

    /**
     * Called when a tab exits the selected state.
     *
     * @param tab The tab that was unselected
     */
    public void onTabUnselected(FlexibleTabLayout.Tab tab);
  }

  /**
   * @hide
   */
  @IntDef(flag = true, value = { GRAVITY_FILL, GRAVITY_CENTER })
  @Retention(RetentionPolicy.SOURCE)
  public @interface TabGravity {
  }

  /**
   * A tab in this layout. Instances can be created via {@link #newTab()}.
   */
  public static final class Tab {

    /**
     * An invalid position for a tab.
     *
     * @see #getPosition()
     */
    public static final int INVALID_POSITION = -1;
    FlexibleTabLayout mParent;
    FlexibleTabLayout.TabView mView;
    private Object mTag;
    private Drawable mIcon;
    private CharSequence mText;
    private CharSequence mContentDesc;
    private int mPosition = INVALID_POSITION;
    private View mCustomView;

    Tab() {
      // Private constructor
    }

    /**
     * Gets a brief description of this tab's content for use in accessibility support.
     *
     * @return Description of this tab's content
     * @see #setContentDescription(CharSequence)
     * @see #setContentDescription(int)
     */
    @Nullable
    public CharSequence getContentDescription() {
      return mContentDesc;
    }

    /**
     * Set a description of this tab's content for use in accessibility support. If no content
     * description is provided the title will be used.
     *
     * @param contentDesc Description of this tab's content
     * @return The current instance for call chaining
     * @see #setContentDescription(int)
     * @see #getContentDescription()
     */
    @NonNull
    public FlexibleTabLayout.Tab setContentDescription(@Nullable final CharSequence contentDesc) {
      mContentDesc = contentDesc;
      updateView();
      return this;
    }

    /**
     * Returns the custom view used for this tab.
     *
     * @see #setCustomView(View)
     * @see #setCustomView(int)
     */
    @Nullable
    public View getCustomView() {
      return mCustomView;
    }

    /**
     * Set a custom view to be used for this tab.
     * <p>
     * If the inflated layout contains a {@link TextView} with an ID of
     * {@link android.R.id#text1} then that will be updated with the value given
     * to {@link #setText(CharSequence)}. Similarly, if this layout contains an
     * {@link ImageView} with ID {@link android.R.id#icon} then it will be updated with
     * the value given to {@link #setIcon(Drawable)}.
     * </p>
     *
     * @param resId A layout resource to inflate and use as a custom tab view
     * @return The current instance for call chaining
     */
    @NonNull
    public FlexibleTabLayout.Tab setCustomView(@LayoutRes final int resId) {
      final LayoutInflater inflater = LayoutInflater.from(mView.getContext());
      return setCustomView(inflater.inflate(resId, mView, false));
    }

    /**
     * Return the icon associated with this tab.
     *
     * @return The tab's icon
     */
    @Nullable
    public Drawable getIcon() {
      return mIcon;
    }

    /**
     * Set the icon displayed on this tab.
     *
     * @param resId A resource ID referring to the icon that should be displayed
     * @return The current instance for call chaining
     */
    @NonNull
    public FlexibleTabLayout.Tab setIcon(@DrawableRes final int resId) {
      if (mParent == null) {
        throw new IllegalArgumentException("Tab not attached to a TabLayout");
      }
      return setIcon(AppCompatResources.getDrawable(mParent.getContext(), resId));
    }

    /**
     * Return the current position of this tab in the action bar.
     *
     * @return Current position, or {@link #INVALID_POSITION} if this tab is not currently in
     * the action bar.
     */
    public int getPosition() {
      return mPosition;
    }

    void setPosition(final int position) {
      mPosition = position;
    }

    /**
     * @return This Tab's tag object.
     */
    @Nullable
    public Object getTag() {
      return mTag;
    }

    /**
     * Give this Tab an arbitrary object to hold for later use.
     *
     * @param tag Object to store
     * @return The current instance for call chaining
     */
    @NonNull
    public FlexibleTabLayout.Tab setTag(@Nullable final Object tag) {
      mTag = tag;
      return this;
    }

    /**
     * Return the text of this tab.
     *
     * @return The tab's text
     */
    @Nullable
    public CharSequence getText() {
      return mText;
    }

    /**
     * Set the text displayed on this tab. Text may be truncated if there is not room to display
     * the entire string.
     *
     * @param resId A resource ID referring to the text that should be displayed
     * @return The current instance for call chaining
     */
    @NonNull
    public FlexibleTabLayout.Tab setText(@StringRes final int resId) {
      if (mParent == null) {
        throw new IllegalArgumentException("Tab not attached to a TabLayout");
      }
      return setText(mParent.getResources().getText(resId));
    }

    /**
     * Returns true if this tab is currently selected.
     */
    public boolean isSelected() {
      if (mParent == null) {
        throw new IllegalArgumentException("Tab not attached to a TabLayout");
      }
      return mParent.getSelectedTabPosition() == mPosition;
    }

    /**
     * Select this tab. Only valid if the tab has been added to the action bar.
     */
    public void select() {
      if (mParent == null) {
        throw new IllegalArgumentException("Tab not attached to a TabLayout");
      }
      mParent.selectTab(this);
    }

    /**
     * Set a description of this tab's content for use in accessibility support. If no content
     * description is provided the title will be used.
     *
     * @param resId A resource ID referring to the description text
     * @return The current instance for call chaining
     * @see #setContentDescription(CharSequence)
     * @see #getContentDescription()
     */
    @NonNull
    public FlexibleTabLayout.Tab setContentDescription(@StringRes final int resId) {
      if (mParent == null) {
        throw new IllegalArgumentException("Tab not attached to a TabLayout");
      }
      return setContentDescription(mParent.getResources().getText(resId));
    }

    /**
     * Set a custom view to be used for this tab.
     * <p>
     * If the provided view contains a {@link TextView} with an ID of
     * {@link android.R.id#text1} then that will be updated with the value given
     * to {@link #setText(CharSequence)}. Similarly, if this layout contains an
     * {@link ImageView} with ID {@link android.R.id#icon} then it will be updated with
     * the value given to {@link #setIcon(Drawable)}.
     * </p>
     *
     * @param view Custom view to be used as a tab.
     * @return The current instance for call chaining
     */
    @NonNull
    public FlexibleTabLayout.Tab setCustomView(@Nullable final View view) {
      mCustomView = view;
      updateView();
      return this;
    }

    /**
     * Set the icon displayed on this tab.
     *
     * @param icon The drawable to use as an icon
     * @return The current instance for call chaining
     */
    @NonNull
    public FlexibleTabLayout.Tab setIcon(@Nullable final Drawable icon) {
      mIcon = icon;
      updateView();
      return this;
    }

    /**
     * Set the text displayed on this tab. Text may be truncated if there is not room to display
     * the entire string.
     *
     * @param text The text to display
     * @return The current instance for call chaining
     */
    @NonNull
    public FlexibleTabLayout.Tab setText(@Nullable final CharSequence text) {
      mText = text;
      updateView();
      return this;
    }

    void reset() {
      mParent = null;
      mView = null;
      mTag = null;
      mIcon = null;
      mText = null;
      mContentDesc = null;
      mPosition = INVALID_POSITION;
      mCustomView = null;
    }

    void updateView() {
      if (mView != null) {
        mView.update();
      }
    }
  }

  /**
   * A {@link ViewPager.OnPageChangeListener} class which contains the
   * necessary calls back to the provided {@link FlexibleTabLayout} so that the tab position is
   * kept in sync.
   *
   * <p>This class stores the provided TabLayout weakly, meaning that you can use
   * {@link ViewPager#addOnPageChangeListener(ViewPager.OnPageChangeListener)
   * addOnPageChangeListener(OnPageChangeListener)} without removing the listener and
   * not cause a leak.
   */
  public static class TabLayoutOnPageChangeListener implements ViewPager.OnPageChangeListener {
    private final WeakReference<FlexibleTabLayout> mTabLayoutRef;
    private int mPreviousScrollState;
    private int mScrollState;

    public TabLayoutOnPageChangeListener(final FlexibleTabLayout tabLayout) {
      mTabLayoutRef = new WeakReference<>(tabLayout);
    }

    @Override
    public void onPageScrollStateChanged(final int state) {
      mPreviousScrollState = mScrollState;
      mScrollState = state;
    }

    @Override
    public void onPageScrolled(
        final int position, final float positionOffset,
        final int positionOffsetPixels) {
      final FlexibleTabLayout tabLayout = mTabLayoutRef.get();
      if (tabLayout != null) {
        // Only update the text selection if we're not settling, or we are settling after
        // being dragged
        final boolean updateText = mScrollState != SCROLL_STATE_SETTLING ||
            mPreviousScrollState == SCROLL_STATE_DRAGGING;
        // Update the indicator if we're not settling after being idle. This is caused
        // from a setCurrentItem() call and will be handled by an animation from
        // onPageSelected() instead.
        final boolean updateIndicator = !(mScrollState == SCROLL_STATE_SETTLING
            && mPreviousScrollState == SCROLL_STATE_IDLE);
        tabLayout.setScrollPosition(position, positionOffset, updateText, updateIndicator);
      }
    }

    @Override
    public void onPageSelected(final int position) {
      final FlexibleTabLayout tabLayout = mTabLayoutRef.get();
      if (tabLayout != null && tabLayout.getSelectedTabPosition() != position
          && position < tabLayout.getTabCount()) {
        // Select the tab, only updating the indicator if we're not being dragged/settled
        // (since onPageScrolled will handle that).
        final boolean updateIndicator = mScrollState == SCROLL_STATE_IDLE
            || (mScrollState == SCROLL_STATE_SETTLING
            && mPreviousScrollState == SCROLL_STATE_IDLE);
        tabLayout.selectTab(tabLayout.getTabAt(position), updateIndicator);
      }
    }

    void reset() {
      mPreviousScrollState = mScrollState = SCROLL_STATE_IDLE;
    }
  }

  /**
   * A {@link FlexibleTabLayout.OnTabSelectedListener} class which contains the necessary calls back
   * to the provided {@link ViewPager} so that the tab position is kept in sync.
   */
  public static class ViewPagerOnTabSelectedListener implements
      FlexibleTabLayout.OnTabSelectedListener {
    private final ViewPager mViewPager;

    public ViewPagerOnTabSelectedListener(final ViewPager viewPager) {
      mViewPager = viewPager;
    }

    @Override
    public void onTabReselected(final FlexibleTabLayout.Tab tab) {
      // No-op
    }

    @Override
    public void onTabSelected(final FlexibleTabLayout.Tab tab) {
      mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(final FlexibleTabLayout.Tab tab) {
      // No-op
    }
  }

  private class AdapterChangeListener implements ViewPager.OnAdapterChangeListener {
    private boolean mAutoRefresh;

    AdapterChangeListener() {
    }

    @Override
    public void onAdapterChanged(
        @NonNull final ViewPager viewPager,
        @Nullable final PagerAdapter oldAdapter, @Nullable final PagerAdapter newAdapter) {
      if (mViewPager == viewPager) {
        setPagerAdapter(newAdapter, mAutoRefresh);
      }
    }

    void setAutoRefresh(final boolean autoRefresh) {
      mAutoRefresh = autoRefresh;
    }
  }

  private class GroSlidingTabStrip extends LinearLayout {
    private final Paint mSelectedIndicatorPaint;
    int mSelectedPosition = -1;
    float mSelectionOffset;
    private boolean mMatchTextWidth;
    private int mSelectedIndicatorHeight;
    private int mSelectedIndicatorWidth;
    private int mIndicatorLeft = -1;
    private int mIndicatorRight = -1;

    private ValueAnimator mIndicatorAnimator;

    GroSlidingTabStrip(final Context context) {
      super(context);
      setWillNotDraw(false);
      mSelectedIndicatorPaint = new Paint();
    }

    @Override
    public void draw(final Canvas canvas) {
      super.draw(canvas);

      // Thick colored underline below the current selection
      if (mIndicatorLeft >= 0 && mIndicatorRight > mIndicatorLeft) {
        canvas.drawRect(mIndicatorLeft, getHeight() - mSelectedIndicatorHeight,
            mIndicatorRight, getHeight(), mSelectedIndicatorPaint
        );
      }
    }

    @Override
    protected void onLayout(
        final boolean changed,
        final int l,
        final int t,
        final int r,
        final int b) {
      super.onLayout(changed, l, t, r, b);

      if (mIndicatorAnimator != null && mIndicatorAnimator.isRunning()) {
        // If we're currently running an animation, lets cancel it and start a
        // new animation with the remaining duration
        mIndicatorAnimator.cancel();
        final long duration = mIndicatorAnimator.getDuration();
        animateIndicatorToPosition(
            mSelectedPosition,
            Math.round((1f - mIndicatorAnimator.getAnimatedFraction()) * duration)
        );
      } else {
        // If we've been layed out, update the indicator position
        updateIndicatorPosition();
      }
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);

      if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
        // HorizontalScrollView will first measure use with UNSPECIFIED, and then with
        // EXACTLY. Ignore the first call since anything we do will be overwritten anyway
        return;
      }

      if (mMode == MODE_FIXED && mTabGravity == GRAVITY_CENTER) {
        final int count = getChildCount();

        // First we'll find the widest tab
        int largestTabWidth = 0;
        for (int i = 0, z = count; i < z; i++) {
          final View child = getChildAt(i);
          if (child.getVisibility() == VISIBLE) {
            largestTabWidth = Math.max(largestTabWidth, child.getMeasuredWidth());
          }
        }

        if (largestTabWidth <= 0) {
          // If we don't have a largest child yet, skip until the next measure pass
          return;
        }

        final int gutter = dpToPx(FIXED_WRAP_GUTTER_MIN);
        boolean remeasure = false;

        if (largestTabWidth * count <= getMeasuredWidth() - gutter * 2) {
          // If the tabs fit within our width minus gutters, we will set all tabs to have
          // the same width
          for (int i = 0; i < count; i++) {
            final LinearLayout.LayoutParams lp =
                (LayoutParams) getChildAt(i).getLayoutParams();
            if (lp.width != largestTabWidth || lp.weight != 0) {
              lp.width = largestTabWidth;
              lp.weight = 0;
              remeasure = true;
            }
          }
        } else {
          // If the tabs will wrap to be larger than the width minus gutters, we need
          // to switch to GRAVITY_FILL
          mTabGravity = GRAVITY_FILL;
          updateTabViews(false);
          remeasure = true;
        }

        if (remeasure) {
          // Now re-measure after our changes
          super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
      }
    }

    void animateIndicatorToPosition(final int position, final int duration) {
      if (mIndicatorAnimator != null && mIndicatorAnimator.isRunning()) {
        mIndicatorAnimator.cancel();
      }

      final boolean isRtl = ViewCompat.getLayoutDirection(this)
          == ViewCompat.LAYOUT_DIRECTION_RTL;

      final FlexibleTabLayout.TabView targetView = (FlexibleTabLayout.TabView) getChildAt(position);
      if (targetView == null) {
        // If we don't have a view, just update the position now and return
        updateIndicatorPosition();
        return;
      }

      final int targetLeft = targetView.getLeft() + (mMatchTextWidth ? targetView.getTextViewLeft() : 0);
      final int targetRight = targetView.getRight() + (mMatchTextWidth ? targetView.getTextViewRight() : 0);
      final int startLeft;
      final int startRight;

      if (Math.abs(position - mSelectedPosition) <= 1) {
        // If the views are adjacent, we'll animate from edge-to-edge
        startLeft = mIndicatorLeft;
        startRight = mIndicatorRight;
      } else {
        // Else, we'll just grow from the nearest edge
        final int offset = dpToPx(MOTION_NON_ADJACENT_OFFSET);
        if (position < mSelectedPosition) {
          // We're going end-to-start
          if (isRtl) {
            startLeft = startRight = targetLeft - offset;
          } else {
            startLeft = startRight = targetRight + offset;
          }
        } else {
          // We're going start-to-end
          if (isRtl) {
            startLeft = startRight = targetRight + offset;
          } else {
            startLeft = startRight = targetLeft - offset;
          }
        }
      }

      if (startLeft != targetLeft || startRight != targetRight) {
        final ValueAnimator animator = mIndicatorAnimator = new ValueAnimator();
        animator.setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR);
        animator.setDuration(duration);
        animator.setFloatValues(0, 1);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
          @Override public void onAnimationUpdate(final ValueAnimator animation) {
            final float fraction = animator.getAnimatedFraction();
            setIndicatorPosition(
                lerp(startLeft, targetLeft, fraction),
                lerp(startRight, targetRight, fraction)
            );
          }
        });
        animator.addListener(new Animator.AnimatorListener() {
          @Override public void onAnimationCancel(final Animator animation) {

          }

          @Override public void onAnimationEnd(final Animator animation) {
            mSelectedPosition = position;
            mSelectionOffset = 0f;
          }

          @Override public void onAnimationRepeat(final Animator animation) {

          }

          @Override public void onAnimationStart(final Animator animation) {

          }
        });
        animator.start();
      }
    }

    boolean childrenNeedLayout() {
      for (int i = 0, z = getChildCount(); i < z; i++) {
        final View child = getChildAt(i);
        if (child.getWidth() <= 0) {
          return true;
        }
      }
      return false;
    }

    float getIndicatorPosition() {
      return mSelectedPosition + mSelectionOffset;
    }

    void setIndicatorPosition(final int left, final int right) {
      if (left != mIndicatorLeft || right != mIndicatorRight) {
        // If the indicator's left/right has changed, invalidate
        mIndicatorLeft = left;
        mIndicatorRight = right;
        ViewCompat.postInvalidateOnAnimation(this);
      }
    }

    void setMatchTextWidth(final boolean match) {
      if (mMatchTextWidth != match) {
        mMatchTextWidth = match;
        ViewCompat.postInvalidateOnAnimation(this);
      }
    }

    void setIndicatorPositionFromTabPosition(final int position, final float positionOffset) {
      if (mIndicatorAnimator != null && mIndicatorAnimator.isRunning()) {
        mIndicatorAnimator.cancel();
      }

      mSelectedPosition = position;
      mSelectionOffset = positionOffset;
      updateIndicatorPosition();
    }

    void setSelectedIndicatorColor(final int color) {
      if (mSelectedIndicatorPaint.getColor() != color) {
        mSelectedIndicatorPaint.setColor(color);
        ViewCompat.postInvalidateOnAnimation(this);
      }
    }

    void setSelectedIndicatorHeight(final int height) {
      if (mSelectedIndicatorHeight != height) {
        mSelectedIndicatorHeight = height;
        ViewCompat.postInvalidateOnAnimation(this);
      }
    }

    //Added method for modifying width
    void setSelectedIndicatorWidth(final int width) {
      if (mSelectedIndicatorWidth != width) {
        mSelectedIndicatorWidth = width;
        ViewCompat.postInvalidateOnAnimation(this);
      }
    }

    private void updateIndicatorPosition() {
      final FlexibleTabLayout.TabView selectedTitle =
          (FlexibleTabLayout.TabView) getChildAt(mSelectedPosition);
      int left, right;

      if (selectedTitle != null && selectedTitle.getWidth() > 0) {
        //If we are matching text width then modify the width
        if (mMatchTextWidth) {
          left = selectedTitle.getLeft() + selectedTitle.getTextViewLeft();
          right = selectedTitle.getLeft() + selectedTitle.getTextViewRight();
        } else {
          left = selectedTitle.getLeft();
          right = selectedTitle.getRight();
        }

        if (mSelectionOffset > 0f && mSelectedPosition < getChildCount() - 1) {
          // Draw the selection partway between the tabs
          final FlexibleTabLayout.TabView
              nextTitle = (FlexibleTabLayout.TabView) getChildAt(mSelectedPosition + 1);
          if (mMatchTextWidth) {
            left = (int) (mSelectionOffset * (nextTitle.getLeft() + nextTitle.getTextViewLeft()) +
                (1.0f - mSelectionOffset) * left);
            right = (int) (mSelectionOffset * (nextTitle.getLeft() + nextTitle.getTextViewRight()) +
                (1.0f - mSelectionOffset) * right);
          } else {
            left = (int) (mSelectionOffset * nextTitle.getLeft() +
                (1.0f - mSelectionOffset) * left);
            right = (int) (mSelectionOffset * nextTitle.getRight() +
                (1.0f - mSelectionOffset) * right);
          }
        }
      } else {
        left = right = -1;
      }

      setIndicatorPosition(left, right);
    }
  }

  private class PagerAdapterObserver extends DataSetObserver {
    PagerAdapterObserver() {
    }

    @Override
    public void onChanged() {
      populateFromPagerAdapter();
    }

    @Override
    public void onInvalidated() {
      populateFromPagerAdapter();
    }
  }

  public final class TabItem extends View {
    final CharSequence mText;
    final Drawable mIcon;
    final int mCustomLayout;

    public TabItem(final Context context) {
      this(context, null);
    }

    public TabItem(final Context context, final AttributeSet attrs) {
      super(context, attrs);

      final TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs,
          android.support.design.R.styleable.TabItem
      );
      mText = a.getText(android.support.design.R.styleable.TabItem_android_text);
      mIcon = a.getDrawable(android.support.design.R.styleable.TabItem_android_icon);
      mCustomLayout = a.getResourceId(android.support.design.R.styleable.TabItem_android_layout, 0);
      a.recycle();
    }
  }

  class TabView extends LinearLayout implements OnLongClickListener {
    private FlexibleTabLayout.Tab mTab;
    private TextView mTextView;
    private ImageView mIconView;

    private View mCustomView;
    private TextView mCustomTextView;
    private ImageView mCustomIconView;

    private int mDefaultMaxLines = 2;

    public TabView(final Context context) {
      super(context);
      if (mTabBackgroundResId != 0) {
        setBackgroundDrawable(AppCompatResources.getDrawable(context, mTabBackgroundResId));
      }
      ViewCompat.setPaddingRelative(this, mTabPaddingStart, mTabPaddingTop,
          mTabPaddingEnd, mTabPaddingBottom
      );
      setGravity(Gravity.CENTER);
      setOrientation(VERTICAL);
      setClickable(true);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onInitializeAccessibilityEvent(final AccessibilityEvent event) {
      super.onInitializeAccessibilityEvent(event);
      // This view masquerades as an action bar tab.
      event.setClassName(ActionBar.Tab.class.getName());
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onInitializeAccessibilityNodeInfo(final AccessibilityNodeInfo info) {
      super.onInitializeAccessibilityNodeInfo(info);
      // This view masquerades as an action bar tab.
      info.setClassName(ActionBar.Tab.class.getName());
    }

    @Override
    public boolean onLongClick(final View v) {
      final int[] screenPos = new int[2];
      final Rect displayFrame = new Rect();
      getLocationOnScreen(screenPos);
      getWindowVisibleDisplayFrame(displayFrame);

      final Context context = getContext();
      final int width = getWidth();
      final int height = getHeight();
      final int midy = screenPos[1] + height / 2;
      int referenceX = screenPos[0] + width / 2;
      if (ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_LTR) {
        final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        referenceX = screenWidth - referenceX; // mirror
      }

      final Toast cheatSheet = Toast.makeText(context, mTab.getContentDescription(),
          Toast.LENGTH_SHORT
      );
      if (midy < displayFrame.height()) {
        // Show below the tab view
        cheatSheet.setGravity(Gravity.TOP | GravityCompat.END, referenceX,
            screenPos[1] + height - displayFrame.top
        );
      } else {
        // Show along the bottom center
        cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, height);
      }
      cheatSheet.show();
      return true;
    }

    @Override
    public void onMeasure(final int origWidthMeasureSpec, final int origHeightMeasureSpec) {
      final int specWidthSize = MeasureSpec.getSize(origWidthMeasureSpec);
      final int specWidthMode = MeasureSpec.getMode(origWidthMeasureSpec);
      final int maxWidth = getTabMaxWidth();

      final int widthMeasureSpec;

      if (maxWidth > 0 && (specWidthMode == MeasureSpec.UNSPECIFIED
          || specWidthSize > maxWidth)) {
        // If we have a max width and a given spec which is either unspecified or
        // larger than the max width, update the width spec using the same mode
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(mTabMaxWidth, MeasureSpec.AT_MOST);
      } else {
        // Else, use the original width spec
        widthMeasureSpec = origWidthMeasureSpec;
      }

      // Now lets measure
      super.onMeasure(widthMeasureSpec, origHeightMeasureSpec);

      // We need to switch the text size based on whether the text is spanning 2 lines or not
      if (mTextView != null) {
        float textSize = mTabTextSize;
        int maxLines = mDefaultMaxLines;

        //Set textview font if exists
        if (mTypeface != null) {
          mTextView.setTypeface(mTypeface);
        }

        if (mIconView != null && mIconView.getVisibility() == VISIBLE) {
          // If the icon view is being displayed, we limit the text to 1 line
          maxLines = 1;
        } else if (mTextView != null && mTextView.getLineCount() > 1) {
          // Otherwise when we have text which wraps we reduce the text size
          textSize = mTabTextMultiLineSize;
        }

        final float curTextSize = mTextView.getTextSize();
        final int curLineCount = mTextView.getLineCount();
        final int curMaxLines = TextViewCompat.getMaxLines(mTextView);

        if (textSize != curTextSize || (curMaxLines >= 0 && maxLines != curMaxLines)) {
          // We've got a new text size and/or max lines...
          boolean updateTextView = true;

          if (mMode == MODE_FIXED && textSize > curTextSize && curLineCount == 1) {
            // If we're in fixed mode, going up in text size and currently have 1 line
            // then it's very easy to get into an infinite recursion.
            // To combat that we check to see if the change in text size
            // will cause a line count change. If so, abort the size change and stick
            // to the smaller size.
            final Layout layout = mTextView.getLayout();
            if (layout == null || approximateLineWidth(layout, 0, textSize)
                > getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) {
              updateTextView = false;
            }
          }

          if (updateTextView) {
            mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            mTextView.setMaxLines(maxLines);
            super.onMeasure(widthMeasureSpec, origHeightMeasureSpec);
          }
        }
      }
    }

    @Override
    public boolean performClick() {
      final boolean value = super.performClick();

      if (mTab != null) {
        mTab.select();
        return true;
      } else {
        return value;
      }
    }

    @Override
    public void setSelected(final boolean selected) {
      final boolean changed = isSelected() != selected;

      super.setSelected(selected);

      if (changed && selected && Build.VERSION.SDK_INT < 16) {
        // Pre-JB we need to manually send the TYPE_VIEW_SELECTED event
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
      }

      // Always dispatch this to the child views, regardless of whether the value has
      // changed
      if (mTextView != null) {
        mTextView.setSelected(selected);
      }
      if (mIconView != null) {
        mIconView.setSelected(selected);
      }
      if (mCustomView != null) {
        mCustomView.setSelected(selected);
      }
    }

    public FlexibleTabLayout.Tab getTab() {
      return mTab;
    }

    void setTab(@Nullable final FlexibleTabLayout.Tab tab) {
      if (tab != mTab) {
        mTab = tab;
        update();
      }
    }

    public int getTextViewLeft() {
      return mTextView.getLeft();
    }

    public int getTextViewRight() {
      return mTextView.getRight();
    }

    void reset() {
      setTab(null);
      setSelected(false);
    }

    final void update() {
      final FlexibleTabLayout.Tab tab = mTab;
      final View custom = tab != null ? tab.getCustomView() : null;
      if (custom != null) {
        final ViewParent customParent = custom.getParent();
        if (customParent != this) {
          if (customParent != null) {
            ((ViewGroup) customParent).removeView(custom);
          }
          addView(custom);
        }
        mCustomView = custom;
        if (mTextView != null) {
          mTextView.setVisibility(GONE);
        }
        if (mIconView != null) {
          mIconView.setVisibility(GONE);
          mIconView.setImageDrawable(null);
        }

        mCustomTextView = (TextView) custom.findViewById(android.R.id.text1);
        if (mCustomTextView != null) {
          mDefaultMaxLines = TextViewCompat.getMaxLines(mCustomTextView);
        }
        mCustomIconView = (ImageView) custom.findViewById(android.R.id.icon);
      } else {
        // We do not have a custom view. Remove one if it already exists
        if (mCustomView != null) {
          removeView(mCustomView);
          mCustomView = null;
        }
        mCustomTextView = null;
        mCustomIconView = null;
      }

      if (mCustomView == null) {
        // If there isn't a custom view, we'll us our own in-built layouts
        if (mIconView == null) {
          ImageView iconView;
          iconView = (ImageView) LayoutInflater.from(getContext())
              .inflate(android.support.design.R.layout.design_layout_tab_icon, this, false);
          addView(iconView, 0);
          mIconView = iconView;
        }
        if (mTextView == null) {
          final TextView textView = (TextView) LayoutInflater.from(getContext())
              .inflate(android.support.design.R.layout.design_layout_tab_text, this, false);
          addView(textView);
          mTextView = textView;
          mDefaultMaxLines = TextViewCompat.getMaxLines(mTextView);
        }
        mTextView.setTextAppearance(getContext(), mTabTextAppearance);
        if (mTabTextColors != null) {
          mTextView.setTextColor(mTabTextColors);
        }
        if (mTypeface != null) {
          mTextView.setTypeface(mTypeface);
        }
        updateTextAndIcon(mTextView, mIconView);
      } else {
        // Else, we'll see if there is a TextView or ImageView present and update them
        if (mCustomTextView != null || mCustomIconView != null) {
          updateTextAndIcon(mCustomTextView, mCustomIconView);
        }
      }

      // Finally update our selected state
      setSelected(tab != null && tab.isSelected());
    }

    /**
     * Approximates a given lines width with the new provided text size.
     */
    private float approximateLineWidth(final Layout layout, final int line, final float textSize) {
      return layout.getLineWidth(line) * (textSize / layout.getPaint().getTextSize());
    }

    private void updateTextAndIcon(
        @Nullable final TextView textView,
        @Nullable final ImageView iconView) {
      final Drawable icon = mTab != null ? mTab.getIcon() : null;
      final CharSequence text = mTab != null ? mTab.getText() : null;
      final CharSequence contentDesc = mTab != null ? mTab.getContentDescription() : null;

      if (iconView != null) {
        if (icon != null) {
          iconView.setImageDrawable(icon);
          iconView.setVisibility(VISIBLE);
          setVisibility(VISIBLE);
        } else {
          iconView.setVisibility(GONE);
          iconView.setImageDrawable(null);
        }
        iconView.setContentDescription(contentDesc);
      }

      final boolean hasText = !TextUtils.isEmpty(text);
      if (textView != null) {
        if (hasText) {
          textView.setText(text);
          textView.setVisibility(VISIBLE);
          setVisibility(VISIBLE);
        } else {
          textView.setVisibility(GONE);
          textView.setText(null);
        }
        textView.setContentDescription(contentDesc);
      }

      if (iconView != null) {
        final MarginLayoutParams lp = ((MarginLayoutParams) iconView.getLayoutParams());
        int bottomMargin = 0;
        if (hasText && iconView.getVisibility() == VISIBLE) {
          // If we're showing both text and icon, add some margin bottom to the icon
          bottomMargin = dpToPx(DEFAULT_GAP_TEXT_ICON);
        }
        if (bottomMargin != lp.bottomMargin) {
          lp.bottomMargin = bottomMargin;
          iconView.requestLayout();
        }
      }

      if (!hasText && !TextUtils.isEmpty(contentDesc)) {
        setOnLongClickListener(this);
      } else {
        setOnLongClickListener(null);
        setLongClickable(false);
      }
    }
  }
}
