/*
 * Copyright 2014 A.C.R. Development
 */

package net.kidbox.browser.activities;

import net.kidbox.browser.BookmarkManager;
import net.kidbox.browser.BookmarkPage;
import net.kidbox.browser.BrowserController;
import net.kidbox.browser.ClickHandler;
import net.kidbox.browser.Constants;
import net.kidbox.browser.HistoryDatabaseHandler;
import net.kidbox.browser.HistoryItem;
import net.kidbox.browser.HistoryPage;
import net.kidbox.browser.LightningView;
import net.kidbox.browser.PreferenceConstants;
import net.kidbox.browser.R;
import net.kidbox.browser.SearchAdapter;
import net.kidbox.browser.SettingsController;
import net.kidbox.browser.Utils;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.*;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebView.HitTestResult;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import org.apache.commons.validator.routines.UrlValidator;

public class BrowserActivity extends Activity implements BrowserController {

	private DrawerLayout mDrawerLayout;
	private LinearLayout mDrawerRight;
	private ListView mDrawerListRight;
	private List<Integer> mIdList = new ArrayList<Integer>();
	private LightningView mCurrentView;
	//private LightningViewAdapter mTitleAdapter;
	private List<HistoryItem> mBookmarkList;
	private BookmarkViewAdapter mBookmarkAdapter;
	private AutoCompleteTextView mSearch;
	private ClickHandler mClickHandler;
	private ProgressBar mProgressBar;
	private boolean mSystemBrowser = false;
	private ValueCallback<Uri> mUploadMessage;
	private View mCustomView;
	private int mOriginalOrientation;
	private int mActionBarSize;
	private ActionBar mActionBar;
	private boolean mFullScreen;
	private FrameLayout mBrowserFrame;
	private FullscreenHolder mFullscreenContainer;
	private CustomViewCallback mCustomViewCallback;
	private final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
	private Bitmap mDefaultVideoPoster;
	private View mVideoProgressView;
	private HistoryDatabaseHandler mHistoryHandler;
	private SharedPreferences mPreferences;
	private SharedPreferences.Editor mEditPrefs;
	private Context mContext;
	private Bitmap mWebpageBitmap;
	private String mSearchText;
	private Activity mActivity;
	private final int API = android.os.Build.VERSION.SDK_INT;
	private Drawable mDeleteIcon;
	private Drawable mRefreshIcon;
	private int mActionBarSizeDp;
	private int mNumberIconColor;
	private String mHomepage;
	private boolean mIsNewIntent;
	private VideoView mVideoView;
	private static SearchAdapter mSearchAdapter;
	private static LayoutParams mMatchParent = new LayoutParams(LayoutParams.MATCH_PARENT,
			LayoutParams.MATCH_PARENT);
	private BookmarkManager mBookmarkManager;
	private ImageButton previousButton;
	private ImageButton nextButton;
	private ImageButton addFavoriteButton;
	private ImageButton removeFavoriteButton;
	private ImageButton favoritesListButton;
	private Drawable mHttpsIcon;

	private Drawable mSearchRightIcon;
	private Drawable mSearchLeftIcon;
	private Drawable mGoIcon;
	private ImageButton homeButton;
	private LightningView downloadTab = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initialize();
	}

	@SuppressWarnings("deprecation")
	private synchronized void initialize() {
		setContentView(R.layout.activity_main);
		TypedValue typedValue = new TypedValue();
		Theme theme = getTheme();
		theme.resolveAttribute(R.attr.numberColor, typedValue, true);
		mNumberIconColor = typedValue.data;
		mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
		
		boolean mBlockAds = onGetBlockAds();

		mEditPrefs = mPreferences.edit();
		mContext = this;
		if (mIdList != null) {
			mIdList.clear();
		} else {
			mIdList = new ArrayList<Integer>();
		}

		mBookmarkManager = new BookmarkManager(this);
		mActivity = this;
		mClickHandler = new ClickHandler(this);
		mBrowserFrame = (FrameLayout) findViewById(R.id.content_frame);
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerRight = (LinearLayout) findViewById(R.id.right_drawer);
		mDrawerListRight = (ListView) findViewById(R.id.right_drawer_list);
		mDrawerListRight.setDivider(null);
		mDrawerListRight.setDividerHeight(0);
		setNavigationDrawerWidth();
		mWebpageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_webpage);
		mActionBar = getActionBar();
		final TypedArray styledAttributes = mContext.getTheme().obtainStyledAttributes(
				new int[] { android.R.attr.actionBarSize });
		mActionBarSize = (int) styledAttributes.getDimension(0, 0);
		if (pixelsToDp(mActionBarSize) < 48) {
			mActionBarSize = Utils.convertToDensityPixels(mContext, 48);
		}
		mActionBarSizeDp = pixelsToDp(mActionBarSize);
		styledAttributes.recycle();

		mHomepage = mPreferences.getString(PreferenceConstants.HOMEPAGE, Constants.HOMEPAGE);

		mBookmarkList = mBookmarkManager.getBookmarks(true);
		mBookmarkAdapter = new BookmarkViewAdapter(this, R.layout.bookmark_list_item, mBookmarkList);
		mDrawerListRight.setAdapter(mBookmarkAdapter);
		mDrawerListRight.setOnItemClickListener(new BookmarkItemClickListener());
		mDrawerListRight.setOnItemLongClickListener(new BookmarkItemLongClickListener());

		if (mHistoryHandler == null) {
			mHistoryHandler = new HistoryDatabaseHandler(this);
		} else if (!mHistoryHandler.isOpen()) {
			mHistoryHandler = new HistoryDatabaseHandler(this);
		}

		// set display options of the ActionBar
		mActionBar.setDisplayShowTitleEnabled(false);
		mActionBar.setDisplayShowCustomEnabled(true);
		mActionBar.setDisplayShowHomeEnabled(false);
		mActionBar.setDisplayHomeAsUpEnabled(false);
		mActionBar.setCustomView(R.layout.search); 

		RelativeLayout back = (RelativeLayout) findViewById(R.id.action_back);
		RelativeLayout forward = (RelativeLayout) findViewById(R.id.action_forward);
		if (back != null) {
			back.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mCurrentView != null) {
						mCurrentView.goBack();
					}
				}

			});
		}
		if (forward != null) {
			forward.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					goForward();
				}

			});
		} 

		// create the search EditText in the ActionBar
		mSearch = (AutoCompleteTextView) mActionBar.getCustomView().findViewById(R.id.search);

		//mProgressBar = (ProgressBar) findViewById(R.id.activity_bar);
		mProgressBar = (ProgressBar) findViewById(R.id.browser_load_progress);
		//mProgressBar.setVisibility(View.GONE);
		mProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.browser_progress_not_loading_style));

		mDeleteIcon = getResources().getDrawable(R.drawable.ic_action_delete);
		mDeleteIcon.setBounds(0, 0, Utils.convertToDensityPixels(mContext, 64), Utils.convertToDensityPixels(mContext, 64));

		mRefreshIcon = getResources().getDrawable(R.drawable.ic_action_refresh);
		mRefreshIcon.setBounds(0, 0, Utils.convertToDensityPixels(mContext, 64), Utils.convertToDensityPixels(mContext, 64));

		mGoIcon = getResources().getDrawable(R.drawable.browser_go_up);
		mGoIcon.setBounds(0, 0, Utils.convertToDensityPixels(mContext, 64), Utils.convertToDensityPixels(mContext, 64));


		mSearchRightIcon = mRefreshIcon;

		mHttpsIcon = getResources().getDrawable(R.drawable.browser_padlock);
		mHttpsIcon.setBounds(0, 0, Utils.convertToDensityPixels(mContext, 28), Utils.convertToDensityPixels(mContext, 64));

		updateSearchIcons();

		mSearch.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {

				switch (arg1) {
				case KeyEvent.KEYCODE_ENTER:
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
					searchTheWeb(mSearch.getText().toString());
					if (mCurrentView != null) {
						mCurrentView.requestFocus();
					}
					return true;
				default:
					break;
				}
				return false;
			}

		});
		mSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus && mCurrentView != null) {
					if (mCurrentView != null) {
						if (mCurrentView.getProgress() < 100) {
							setIsLoading();
						} else {
							setIsFinishedLoading();
						}
					}
					updateUrl(mCurrentView.getUrl());
				} else if (hasFocus) {
					mSearchRightIcon = mGoIcon;
					updateSearchIcons();
				}
			}
		});
		mSearch.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView arg0, int actionId, KeyEvent arg2) {
				// hide the keyboard and search the web when the enter key
				// button is pressed
				if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE
						|| actionId == EditorInfo.IME_ACTION_NEXT
						|| actionId == EditorInfo.IME_ACTION_SEND
						|| actionId == EditorInfo.IME_ACTION_SEARCH
						|| (arg2.getAction() == KeyEvent.KEYCODE_ENTER)) {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
					searchTheWeb(mSearch.getText().toString());
					if (mCurrentView != null) {
						mCurrentView.requestFocus();
					}
					return true;
				}
				return false;
			}

		});

		mSearch.setOnTouchListener(new OnTouchListener() {

			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (mSearch.getCompoundDrawables()[2] != null) {
					boolean tappedX = event.getX() > (mSearch.getWidth() - mSearch.getPaddingRight() - mSearchRightIcon.getIntrinsicWidth());
					if (tappedX) {
						if (event.getAction() == MotionEvent.ACTION_UP) {
							if (mSearch.hasFocus()) {
								navigate();
							} else {
								refreshOrStop();
							}
						}
						return true;
					}
				}
				return false;
			}

		});

		mSystemBrowser = getSystemBrowser();
		Thread initialize = new Thread(new Runnable() {

			@Override
			public void run() {
				initializeSearchSuggestions(mSearch);
			}

		});
		initialize.run();
		// ActionBarDrawerToggle ties together the the proper interactions
		// between the sliding drawer and the action bar app icon

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_right_shadow, GravityCompat.END);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_left_shadow, GravityCompat.START);
		initializePreferences();
		initializeTabs();

		if (API < 19) {
			WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());
		}


		previousButton = (ImageButton) findViewById(R.id.browser_previous_button);
		previousButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				goBack();
			}
		});
		setupBackgroundChangeOnTouch(previousButton, R.drawable.browser_back_up, R.drawable.browser_back_down);

		nextButton = (ImageButton) findViewById(R.id.browser_next_button);
		nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				goForward();
			}
		});
		setupBackgroundChangeOnTouch(nextButton, R.drawable.browser_forward_up, R.drawable.browser_forward_down);

		addFavoriteButton = (ImageButton) findViewById(R.id.browser_add_to_favorites_button);
		addFavoriteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				addBookmark();
			}
		});
		//setupBackgroundChangeOnTouch(addFavoriteButton, R.drawable.browser_add_favorite_up, R.drawable.browser_add_favorite_down);

		removeFavoriteButton = (ImageButton) findViewById(R.id.browser_remove_from_favorites_button);
		removeFavoriteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				removeBookmark();
			}
		});
		//setupBackgroundChangeOnTouch(removeFavoriteButton, R.drawable.browser_in_favorites_up, R.drawable.browser_in_favorites_down);

		favoritesListButton = (ImageButton) findViewById(R.id.browser_favorites_list_button);
		favoritesListButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleFavoritesList();
			}
		});
		setupBackgroundChangeOnTouch(favoritesListButton, R.drawable.browser_favorites_up, R.drawable.browser_favorites_down);

		/*
		closeButton = (ImageButton) findViewById(R.id.browser_close_button);
		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				closeActivity();
			}
		});
		setupBackgroundChangeOnTouch(closeButton, R.drawable.browser_close_up, R.drawable.browser_close_down);
		 */
		homeButton = (ImageButton) findViewById(R.id.browser_home_button);
		homeButton.setImageDrawable(onGetHomeButtonDrawable());
		
		homeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mCurrentView.gotoHomepage();
			}
		});
		//setupBackgroundChangeOnTouch(homeButton, R.drawable.browser_home_up, R.drawable.browser_home_down);

		previousButton = (ImageButton) findViewById(R.id.browser_previous_button);
		previousButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				goBack();
			}
		});
		setupBackgroundChangeOnTouch(previousButton, R.drawable.browser_back_up, R.drawable.browser_back_down);

		nextButton = (ImageButton) findViewById(R.id.browser_next_button);
		nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				goForward();
			}
		});
		setupBackgroundChangeOnTouch(nextButton, R.drawable.browser_forward_up, R.drawable.browser_forward_down);

		setPreviousButtonEnabled(mCurrentView.canGoBack());
		setNextButtonEnabled(mCurrentView.canGoForward());

		//loadProgress = (ProgressBar) findViewById(R.id.browser_load_progress);
		//loadProgress.setProgress(0);

	}

	protected Drawable onGetHomeButtonDrawable() {
		return homeButton.getDrawable();
	}

	protected boolean onGetBlockAds() {
		return mPreferences.getBoolean(PreferenceConstants.BLOCK_ADS, false);
	}
	
	private void setPreviousButtonEnabled(boolean enabled){
		if (previousButton != null) {
			previousButton.setEnabled(enabled);

			if (enabled) {
				previousButton.setImageResource(R.drawable.browser_back_up);
			}else{
				previousButton.setImageResource(R.drawable.browser_back_disabled);
			}
		}
	}
	
	private void setNextButtonEnabled(boolean enabled){
		if (nextButton != null) {
			nextButton.setEnabled(enabled);

			if (enabled) {
				nextButton.setImageResource(R.drawable.browser_forward_up);
			}else{
				nextButton.setImageResource(R.drawable.browser_forward_disabled);
			}
		}
	}
	
	private void updateSearchIcons() {
		mSearch.setCompoundDrawables(mSearchLeftIcon, null, mSearchRightIcon, null);
	}

	public void setNavigationDrawerWidth() {
		int width = getResources().getDisplayMetrics().widthPixels * 3 / 4;
		int maxWidth = Utils.convertToDensityPixels(mContext, 300);
		if (width > maxWidth) {
			DrawerLayout.LayoutParams paramsRight = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawerRight.getLayoutParams();
			paramsRight.width = maxWidth;
			mDrawerRight.setLayoutParams(paramsRight);
		} else {
			DrawerLayout.LayoutParams paramsRight = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawerRight.getLayoutParams();
			paramsRight.width = width;
			mDrawerRight.setLayoutParams(paramsRight);
		}
	}

	/*
	 * Override this class
	 */
	public synchronized void initializeTabs() {

	}

	public void restoreOrNewTab() {
		String url = null;
		
		if (getIntent() != null) {
			url = getIntent().getDataString();
			if (url != null) {
				if (url.startsWith(Constants.FILE)) {
					Utils.showToast(this, getResources().getString(R.string.message_blocked_local));
					url = null;
				}
			}
		}

		newTab(url, true);
	}

	public void initializePreferences() {
		if (mPreferences == null) {
			mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
		}
		
		//Habilita que se ocule la barra superior del navegador
		mFullScreen = onGetFullScreen();

		//Oculta la barra superior de Android
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		mSearchText = Constants.GOOGLE_SEARCH;

		updateCookiePreference();
	}

	protected boolean onGetFullScreen() {
		return mPreferences.getBoolean(PreferenceConstants.FULL_SCREEN, false);
	}

	/*
	 * Override this if class overrides BrowserActivity
	 */
	public void updateCookiePreference() {

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_ENTER) {
			if (mSearch.hasFocus()) {
				searchTheWeb(mSearch.getText().toString());
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}

	private void toggleFavoritesList() {
		if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
			closeBookmarks();
		}else{
			openBookmarks();
		}
	}
	
	private void refreshFavoriteButton() {
		if (mBookmarkManager != null && mCurrentView != null && removeFavoriteButton != null && addFavoriteButton != null) {
			if (mBookmarkManager.hasBookmark(mCurrentView.getUrl())) {
				removeFavoriteButton.setVisibility(View.VISIBLE);
				addFavoriteButton.setVisibility(View.GONE);
			}else{
				removeFavoriteButton.setVisibility(View.GONE);
				addFavoriteButton.setVisibility(View.VISIBLE);
			}
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action buttons
		
		if (item.getItemId() == android.R.id.home) {
			if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
				mDrawerLayout.closeDrawer(mDrawerRight);
			}
			return true;
		}else if (item.getItemId() == R.id.action_back) {
			goBack();
			return true;
		}else if (item.getItemId() == R.id.action_forward) {
			goForward();
			return true;
		}else if (item.getItemId() == R.id.action_settings) {
			openSettings();
			return true;
		}else if (item.getItemId() == R.id.action_history) {
			openHistory();
			return true;
		}else{
			return super.onOptionsItemSelected(item);
		}
	}

	private void openSettings() {
		startActivity(new Intent(this, SettingsActivity.class));
	}

	private void goForward() {
		if (mCurrentView != null) {
			if (mCurrentView.canGoForward()) {
				mCurrentView.goForward();
			}
		}
	}

	private void goBack() {
		if (mCurrentView != null) {
			if (mCurrentView.canGoBack()) {
				mCurrentView.goBack();
			}
		}
	}

	private void removeBookmark() {
		for (int i = 0; i < mBookmarkList.size(); i++) {
			if (mBookmarkList.get(i).getUrl().equals(mCurrentView.getUrl())) {
				removeBookmark(i);
				break;
			}
		}
	}

	private void removeBookmark(int position) {
		if (mBookmarkManager.deleteBookmark(mBookmarkList.get(position).getUrl())) {
			mBookmarkList.remove(position);
			notifyBookmarkDataSetChanged();
			mSearchAdapter.refreshBookmarks();
		}
		refreshFavoriteButton();
	}
	
	private void addBookmark() {
		if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
			HistoryItem bookmark = new HistoryItem(mCurrentView.getUrl(),
					mCurrentView.getTitle());
			if (mBookmarkManager.addBookmark(bookmark)) {
				mBookmarkList.add(bookmark);
				Collections.sort(mBookmarkList, new SortIgnoreCase());
				notifyBookmarkDataSetChanged();
				mSearchAdapter.refreshBookmarks();
			}
			refreshFavoriteButton();
		}
	}

	/**
	 * refreshes the underlying list of the Bookmark adapter since the bookmark
	 * adapter doesn't always change when notifyDataChanged gets called.
	 */
	private void notifyBookmarkDataSetChanged() {
		mBookmarkAdapter.clear();
		mBookmarkAdapter.addAll(mBookmarkList);
		mBookmarkAdapter.notifyDataSetChanged();
		refreshFavoriteButton();
	}

	private class BookmarkItemClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if (mCurrentView != null) {
				mCurrentView.loadUrl(mBookmarkList.get(position).getUrl());
			}
			// keep any jank from happening when the drawer is closed after the
			// URL starts to load
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mDrawerLayout.closeDrawer(mDrawerRight);
				}
			}, 150);
		}
	}

	private class BookmarkItemLongClickListener implements ListView.OnItemLongClickListener {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {

			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
			builder.setTitle(mContext.getResources().getString(R.string.action_bookmarks));
			builder.setMessage(getResources().getString(R.string.dialog_bookmark))
					.setCancelable(true)
					.setNegativeButton(getResources().getString(R.string.action_delete),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									removeBookmark(position);
								}
							})
					.setNeutralButton(getResources().getString(R.string.action_edit),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									editBookmark(position);
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		}
	}

	/**
	 * Takes in the id of which bookmark was selected and shows a dialog that
	 * allows the user to rename and change the url of the bookmark
	 * 
	 * @param id
	 *            which id in the list was chosen
	 */
	public synchronized void editBookmark(final int id) {
		final AlertDialog.Builder homePicker = new AlertDialog.Builder(mActivity);
		homePicker.setTitle(getResources().getString(R.string.title_edit_bookmark));
		final EditText getTitle = new EditText(mContext);
		getTitle.setHint(getResources().getString(R.string.hint_title));
		getTitle.setText(mBookmarkList.get(id).getTitle());
		getTitle.setSingleLine();
		final EditText getUrl = new EditText(mContext);
		getUrl.setHint(getResources().getString(R.string.hint_url));
		getUrl.setText(mBookmarkList.get(id).getUrl());
		getUrl.setSingleLine();
		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.addView(getTitle);
		layout.addView(getUrl);
		homePicker.setView(layout);
		homePicker.setPositiveButton(getResources().getString(R.string.action_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mBookmarkList.get(id).setTitle(getTitle.getText().toString());
						mBookmarkList.get(id).setUrl(getUrl.getText().toString());
						mBookmarkManager.overwriteBookmarks(mBookmarkList);
						Collections.sort(mBookmarkList, new SortIgnoreCase());
						notifyBookmarkDataSetChanged();
						if (mCurrentView != null) {
							if (mCurrentView.getUrl().startsWith(Constants.FILE)
									&& mCurrentView.getUrl().endsWith("bookmarks.html")) {
								openBookmarkPage(mCurrentView.getWebView());
							}
						}
					}
				});
		homePicker.show();
	}

	/**
	 * displays the WebView contained in the LightningView Also handles the
	 * removal of previous views
	 * 
	 * @param view
	 *            the LightningView to show
	 */
	private synchronized void showTab(LightningView view) {
		if (view == null) {
			return;
		}
		mBrowserFrame.removeAllViews();
		if (mCurrentView != null) {
			mCurrentView.setForegroundTab(false);
			mCurrentView.onPause();
		}
		mCurrentView = view;
		mCurrentView.setForegroundTab(true);
		if (mCurrentView.getWebView() != null) {
			updateUrl(mCurrentView.getUrl());
			updateProgress(mCurrentView.getProgress());
		} else {
			updateUrl("");
			updateProgress(0);
		}

		mBrowserFrame.addView(mCurrentView.getWebView(), mMatchParent);
		mCurrentView.onResume();

		// Use a delayed handler to make the transition smooth
		// otherwise it will get caught up with the showTab code
		// and cause a janky motion
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				mDrawerLayout.closeDrawers();
			}
		}, 150);
	}

	/**
	 * creates a new tab with the passed in URL if it isn't null
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	public void handleNewIntent(Intent intent) {
		if (mCurrentView == null) {
			initialize();
		}

		String url = null;
		if (intent != null) {
			url = intent.getDataString();
		}
		int num = 0;
		if (intent != null && intent.getExtras() != null) {
			num = intent.getExtras().getInt(getPackageName() + ".Origin");
		}
		if (num == 1) {
			mCurrentView.loadUrl(url);
		} else if (url != null) {
			if (url.startsWith(Constants.FILE)) {
				Utils.showToast(this, getResources().getString(R.string.message_blocked_local));
				url = null;
			}
			newTab(url, true);
			mIsNewIntent = true;
		}
	}

	@Override
	public void closeEmptyTab() {
		if (mCurrentView != null && mCurrentView.getWebView().copyBackForwardList().getSize() == 0) {
			closeCurrentTab();
		}
	}

	private void closeCurrentTab() {
		// don't delete the tab because the browser will close and mess stuff up
	}

	protected synchronized void newTab(String url, boolean show) {
		mIsNewIntent = false;
		LightningView startingTab = new LightningView(mActivity, url, onGetBlockAds(), onGetDownloadDir()){
			@Override
			protected String getSearchEngineLogo() {
				return onGetSearchEngineLogo(super.getSearchEngineLogo());
			}
			
			@Override
			protected String getSearchEngineUrl() {
				return onGetSearchEngineUrl(super.getSearchEngineUrl());
			}

		};

		if (show) {
			showTab(startingTab);
		}
	}


	protected String onGetSearchEngineUrl(String searchEngineUrl) {
		return searchEngineUrl;
	}
	
	protected String onGetSearchEngineLogo(String searchEngineLogo) {
		return searchEngineLogo;
	}

	protected File onGetDownloadDir() {
        String location = mActivity.getSharedPreferences(PreferenceConstants.PREFERENCES, 0).getString(PreferenceConstants.DOWNLOAD_DIRECTORY, Environment.DIRECTORY_DOWNLOADS);
		return new File(location);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mPreferences.getBoolean(PreferenceConstants.CLEAR_CACHE_EXIT, false)
					&& mCurrentView != null && !isIncognito()) {
				mCurrentView.clearCache(true);
				Log.i(Constants.TAG, "Cache Cleared");

			}
			if (mPreferences.getBoolean(PreferenceConstants.CLEAR_HISTORY_EXIT, false)
					&& !isIncognito()) {
				clearHistory();
				Log.i(Constants.TAG, "History Cleared");

			}
			if (mPreferences.getBoolean(PreferenceConstants.CLEAR_COOKIES_EXIT, false)
					&& !isIncognito()) {
				clearCookies();
				Log.i(Constants.TAG, "Cookies Cleared");

			}
			mCurrentView.onDestroy();
			mCurrentView = null;
			finish();
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	public void clearHistory() {
		this.deleteDatabase(HistoryDatabaseHandler.DATABASE_NAME);
		WebViewDatabase m = WebViewDatabase.getInstance(this);
		m.clearFormData();
		m.clearHttpAuthUsernamePassword();
		if (API < 18) {
			m.clearUsernamePassword();
			WebIconDatabase.getInstance().removeAllIcons();
		}
		if (mSystemBrowser) {
			try {
				Browser.clearHistory(getContentResolver());
			} catch (NullPointerException ignored) {
			}
		}
		SettingsController.setClearHistory(true);
		Utils.trimCache(this);
	}

	public void clearCookies() {
		CookieManager c = CookieManager.getInstance();
		CookieSyncManager.createInstance(this);
		c.removeAllCookie();
	}

	@Override
	public void onBackPressed() {
		if (!mActionBar.isShowing()) {
			mActionBar.show();
		}
		if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
			mDrawerLayout.closeDrawer(mDrawerRight);
		} else {
			if (mCurrentView != null) {
				Log.i(Constants.TAG, "onBackPressed");
				if (mCurrentView.canGoBack()) {
					if (!mCurrentView.isShown()) {
						onHideCustomView();
					} else {
						mCurrentView.goBack();
					}
				}
			} else {
				Log.e(Constants.TAG, "So madness. Much confusion. Why happen.");
				super.onBackPressed();
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(Constants.TAG, "onPause");
		if (mCurrentView != null) {
			mCurrentView.pauseTimers();
			mCurrentView.onPause();
		}
		if (mHistoryHandler != null) {
			if (mHistoryHandler.isOpen()) {
				mHistoryHandler.close();
			}
		}

	}

	public void saveOpenTabs() {
		String s = "";
		if (mCurrentView != null) {
			s = mCurrentView.getUrl();
		}
		mEditPrefs.putString(PreferenceConstants.URL_MEMORY, s);
		mEditPrefs.commit();
	}

	@Override
	protected void onDestroy() {
		Log.i(Constants.TAG, "onDestroy");
		if (mHistoryHandler != null) {
			if (mHistoryHandler.isOpen()) {
				mHistoryHandler.close();
			}
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(Constants.TAG, "onResume");
		if (SettingsController.getClearHistory()) {
		}
		if (mSearchAdapter != null) {
			mSearchAdapter.refreshPreferences();
			mSearchAdapter.refreshBookmarks();
		}
		if (mActionBar != null) {
			if (!mActionBar.isShowing()) {
				mActionBar.show();
			}
		}
		if (mCurrentView != null) {
			mCurrentView.resumeTimers();
			mCurrentView.onResume();

			if (mHistoryHandler == null) {
				mHistoryHandler = new HistoryDatabaseHandler(this);
			} else if (!mHistoryHandler.isOpen()) {
				mHistoryHandler = new HistoryDatabaseHandler(this);
			}
			mBookmarkList = mBookmarkManager.getBookmarks(true);
			notifyBookmarkDataSetChanged();
		} else {
			initialize();
		}
		initializePreferences();
		
		if (mCurrentView != null) {
			mCurrentView.initializePreferences(this, onGetBlockAds());
		} else {
			initialize();
		}
	}

	/**
	 * searches the web for the query fixing any and all problems with the input
	 * checks if it is a search, url, etc.
	 */
	void searchTheWeb(String query) {
		if (query.equals("")) {
			return;
		}
		String SEARCH = mSearchText;
		query = query.trim();
		mCurrentView.stopLoading();

		if (query.startsWith("www.")) {
			query = Constants.HTTP + query;
		} else if (query.startsWith("ftp.")) {
			query = "ftp://" + query;
		}

		boolean containsPeriod = query.contains(".");
		boolean isIPAddress = (TextUtils.isDigitsOnly(query.replace(".", ""))
				&& (query.replace(".", "").length() >= 4) && query.contains("."));
		boolean aboutScheme = query.contains("about:");
		boolean validURL = (query.startsWith("ftp://") || query.startsWith(Constants.HTTP)
				|| query.startsWith(Constants.FILE) || query.startsWith(Constants.HTTPS))
				|| isIPAddress;
		boolean isSearch = ((query.contains(" ") || !containsPeriod) && !aboutScheme);

		if (isIPAddress
				&& (!query.startsWith(Constants.HTTP) || !query.startsWith(Constants.HTTPS))) {
			query = Constants.HTTP + query;
		}

		if (isSearch) {
			try {
				query = URLEncoder.encode(query, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			mCurrentView.loadUrl(SEARCH + query);
		} else if (!validURL) {
			mCurrentView.loadUrl(Constants.HTTP + query);
		} else {
			mCurrentView.loadUrl(query);
		}
	}

	private int pixelsToDp(int num) {
		float scale = getResources().getDisplayMetrics().density;
		return (int) ((num - 0.5f) / scale);
	}

	/**
	 * writes the number of open tabs on the icon.
	 */
	public BitmapDrawable writeOnDrawable(int number) {

		Bitmap bm = Bitmap.createBitmap(mActionBarSize, mActionBarSize, Config.ARGB_8888);
		String text = number + "";
		Paint paint = new Paint();
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setAntiAlias(true);
		paint.setStyle(Style.FILL);
		paint.setColor(mNumberIconColor);
		if (number > 99) {
			number = 99;
		}
		// pixels, 36 dp
		if (mActionBarSizeDp < 50) {
			if (number > 9) {
				paint.setTextSize(mActionBarSize * 3 / 4); // originally
				// 40
				// pixels,
				// 24 dp
			} else {
				paint.setTextSize(mActionBarSize * 9 / 10); // originally 50
				// pixels, 30 dp
			}
		} else {
			paint.setTextSize(mActionBarSize * 3 / 4);
		}
		Canvas canvas = new Canvas(bm);
		// originally only vertical padding of 5 pixels

		int xPos = (canvas.getWidth() / 2);
		int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2));

		canvas.drawText(text, xPos, yPos, paint);

		return new BitmapDrawable(getResources(), bm);
	}

	public class BookmarkViewAdapter extends ArrayAdapter<HistoryItem> {

		Context context;

		int layoutResourceId;

		List<HistoryItem> data = null;

		public BookmarkViewAdapter(Context context, int layoutResourceId, List<HistoryItem> data) {
			super(context, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.context = context;
			this.data = data;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			BookmarkViewHolder holder = null;

			if (row == null) {
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);

				holder = new BookmarkViewHolder();
				holder.txtTitle = (TextView) row.findViewById(R.id.text1);
				holder.favicon = (ImageView) row.findViewById(R.id.favicon1);
				row.setTag(holder);
			} else {
				holder = (BookmarkViewHolder) row.getTag();
			}

			HistoryItem web = data.get(position);
			holder.txtTitle.setText(web.getTitle());
			holder.favicon.setImageBitmap(mWebpageBitmap);
			if (web.getBitmap() == null) {
				getImage(holder.favicon, web);
			} else {
				holder.favicon.setImageBitmap(web.getBitmap());
			}
			return row;
		}

		class BookmarkViewHolder {

			TextView txtTitle;

			ImageView favicon;
		}
	}

	public void getImage(ImageView image, HistoryItem web) {
		try {
			new DownloadImageTask(image, web).execute(Constants.HTTP + getDomainName(web.getUrl())
					+ "/favicon.ico");
		} catch (URISyntaxException e) {
			new DownloadImageTask(image, web)
					.execute("https://www.google.com/s2/favicons?domain_url=" + web.getUrl());
			e.printStackTrace();
		}
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

		ImageView bmImage;

		HistoryItem mWeb;

		public DownloadImageTask(ImageView bmImage, HistoryItem web) {
			this.bmImage = bmImage;
			this.mWeb = web;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon = null;
			// unique path for each url that is bookmarked.
			String hash = String.valueOf(urldisplay.hashCode());
			File image = new File(mContext.getCacheDir(), hash + ".png");
			// checks to see if the image exists
			if (!image.exists()) {
				try {
					// if not, download it...
					URL url = new URL(urldisplay);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.setDoInput(true);
					connection.connect();
					InputStream in = connection.getInputStream();

					if (in != null) {
						mIcon = BitmapFactory.decodeStream(in);
					}
					// ...and cache it
					if (mIcon != null) {
						FileOutputStream fos = new FileOutputStream(image);
						mIcon.compress(Bitmap.CompressFormat.PNG, 100, fos);
						fos.flush();
						fos.close();
						Log.i(Constants.TAG, "Downloaded: " + urldisplay);
					}

				} catch (Exception e) {
				} finally {

				}
			} else {
				// if it exists, retrieve it from the cache
				mIcon = BitmapFactory.decodeFile(image.getPath());
			}
			if (mIcon == null) {
				try {
					// if not, download it...
					InputStream in = new java.net.URL(
							"https://www.google.com/s2/favicons?domain_url=" + urldisplay)
							.openStream();

					if (in != null) {
						mIcon = BitmapFactory.decodeStream(in);
					}
					// ...and cache it
					if (mIcon != null) {
						FileOutputStream fos = new FileOutputStream(image);
						mIcon.compress(Bitmap.CompressFormat.PNG, 100, fos);
						fos.flush();
						fos.close();
					}

				} catch (Exception e) {
				}
			}
			if (mIcon == null) {
				return mWebpageBitmap;
			} else {
				return mIcon;
			}
		}

		protected void onPostExecute(Bitmap result) {
			bmImage.setImageBitmap(result);
			mWeb.setBitmap(result);
			notifyBookmarkDataSetChanged();
		}
	}

	static String getDomainName(String url) throws URISyntaxException {
		URI uri = new URI(url);
		String domain = uri.getHost();
		if (domain == null) {
			return url;
		}
		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}

	@Override
	public void updateUrl(String url) {
		if (url == null) {
			return;
		}
		url = url.replaceFirst(Constants.HTTP, "");
		if (url.startsWith(Constants.FILE)) {
			url = "";
		}

		mSearch.setText(url);
	}

	@Override
	public void updateProgress(int n) {

		if (n > mProgressBar.getProgress()) {
			ObjectAnimator animator = ObjectAnimator.ofInt(mProgressBar, "progress", n);
			animator.setDuration(200);
			animator.setInterpolator(new DecelerateInterpolator());
			animator.start();
		} else if (n < mProgressBar.getProgress()) {
			ObjectAnimator animator = ObjectAnimator.ofInt(mProgressBar, "progress", 0, n);
			animator.setDuration(200);
			animator.setInterpolator(new DecelerateInterpolator());
			animator.start();
		}
		if (n >= 100) {
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					//mProgressBar.setVisibility(View.INVISIBLE);
					setIsFinishedLoading();
				}
			}, 200);

			setPreviousButtonEnabled(mCurrentView.canGoBack());
			setNextButtonEnabled(mCurrentView.canGoForward());
		} else {
			//mProgressBar.setVisibility(View.VISIBLE);
			setIsLoading();
		}

		if (mCurrentView != null && mSearch != null) {
			String url = mCurrentView.getUrl();
			if (url !=  null) {
				if (url.startsWith("https")) {
					mSearchLeftIcon = mHttpsIcon;
					updateSearchIcons();
				}else{
					mSearchLeftIcon = null;
					updateSearchIcons();
				}
			}
		}
		
		refreshFavoriteButton();
	}

	@Override
	public void updateHistory(final String title, final String url) {

	}

	public void addItemToHistory(final String title, final String url) {
		Runnable update = new Runnable() {
			@Override
			public void run() {
				if (isSystemBrowserAvailable()
						&& mPreferences.getBoolean(PreferenceConstants.SYNC_HISTORY, true)) {
					try {
						Browser.updateVisitedHistory(getContentResolver(), url, true);
					} catch (NullPointerException ignored) {
					}
				}
				try {
					if (mHistoryHandler == null && !mHistoryHandler.isOpen()) {
						mHistoryHandler = new HistoryDatabaseHandler(mContext);
					}
					mHistoryHandler.visitHistoryItem(url, title);
				} catch (IllegalStateException e) {
					Log.e(Constants.TAG, "IllegalStateException in updateHistory");
				} catch (NullPointerException e) {
					Log.e(Constants.TAG, "NullPointerException in updateHistory");
				} catch (SQLiteException e) {
					Log.e(Constants.TAG, "SQLiteException in updateHistory");
				}
			}
		};
		if (url != null && !url.startsWith(Constants.FILE)) {
			new Thread(update).start();
		}
	}

	public boolean isSystemBrowserAvailable() {
		return mSystemBrowser;
	}

	public boolean getSystemBrowser() {
		Cursor c = null;
		String[] columns = new String[] { "url", "title" };
		boolean browserFlag = false;
		try {

			Uri bookmarks = Browser.BOOKMARKS_URI;
			c = getContentResolver().query(bookmarks, columns, null, null, null);
		} catch (SQLiteException ignored) {
		} catch (IllegalStateException ignored) {
		} catch (NullPointerException ignored) {
		}

		if (c != null) {
			Log.i("Browser", "System Browser Available");
			browserFlag = true;
		} else {
			Log.e("Browser", "System Browser Unavailable");
			browserFlag = false;
		}
		if (c != null) {
			c.close();
			c = null;
		}
		mEditPrefs.putBoolean("SystemBrowser", browserFlag);
		mEditPrefs.commit();
		return browserFlag;
	}

	/**
	 * method to generate search suggestions for the AutoCompleteTextView from
	 * previously searched URLs
	 */
	private void initializeSearchSuggestions(final AutoCompleteTextView getUrl) {

		getUrl.setThreshold(1);
		getUrl.setDropDownWidth(-1);
		getUrl.setDropDownAnchor(R.id.progressWrapper);
		getUrl.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				try {
					String url;
					url = ((TextView) arg1.findViewById(R.id.url)).getText().toString();
					if (url.startsWith(mContext.getString(R.string.suggestion))) {
						url = ((TextView) arg1.findViewById(R.id.title)).getText().toString();
					} else {
						getUrl.setText(url);
					}
					searchTheWeb(url);
					url = null;
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(getUrl.getWindowToken(), 0);
					if (mCurrentView != null) {
						mCurrentView.requestFocus();
					}
				} catch (NullPointerException e) {
					Log.e("Browser Error: ", "NullPointerException on item click");
				}
			}

		});

		getUrl.setSelectAllOnFocus(true);
		mSearchAdapter = new SearchAdapter(mContext, isIncognito());
		getUrl.setAdapter(mSearchAdapter);
	}

	@Override
	public boolean isIncognito() {
		return false;
	}

	/**
	 * function that opens the HTML history page in the browser
	 */
	private void openHistory() {
		// use a thread so that history retrieval doesn't block the UI
		Thread history = new Thread(new Runnable() {

			@Override
			public void run() {
				mCurrentView.loadUrl(HistoryPage.getHistoryPage(mContext));
				mSearch.setText("");
			}

		});
		history.run();
	}

	/**
	 * helper function that opens the bookmark drawer
	 */
	private void openBookmarks() {
		mDrawerLayout.openDrawer(mDrawerRight);
	}

	private void closeBookmarks() {
		mDrawerLayout.closeDrawers();
	}

	public void closeDrawers() {
		mDrawerLayout.closeDrawers();
	}

	@Override
	/**
	 * open the HTML bookmarks page, parameter view is the WebView that should show the page
	 */
	public void openBookmarkPage(WebView view) {
		String bookmarkHtml = BookmarkPage.HEADING;
		Iterator<HistoryItem> iter = mBookmarkList.iterator();
		HistoryItem helper;
		while (iter.hasNext()) {
			helper = iter.next();
			bookmarkHtml += (BookmarkPage.PART1 + helper.getUrl() + BookmarkPage.PART2
					+ helper.getUrl() + BookmarkPage.PART3 + helper.getTitle() + BookmarkPage.PART4);
		}
		bookmarkHtml += BookmarkPage.END;
		File bookmarkWebPage = new File(mContext.getFilesDir(), BookmarkPage.FILENAME);
		try {
			FileWriter bookWriter = new FileWriter(bookmarkWebPage, false);
			bookWriter.write(bookmarkHtml);
			bookWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		view.loadUrl(Constants.FILE + bookmarkWebPage);
	}

	@Override
	public void update() {
	}

	@Override
	/**
	 * opens a file chooser
	 * param ValueCallback is the message from the WebView indicating a file chooser
	 * should be opened
	 */
	public void openFileChooser(ValueCallback<Uri> uploadMsg) {
		mUploadMessage = uploadMsg;
		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.addCategory(Intent.CATEGORY_OPENABLE);
		i.setType("*/*");
		startActivityForResult(Intent.createChooser(i, getString(R.string.title_file_chooser)), 1);
	}

	@Override
	/**
	 * used to allow uploading into the browser, doesn't get called in KitKat :(
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 1) {
			if (null == mUploadMessage) {
				return;
			}
			Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
			mUploadMessage.onReceiveValue(result);
			mUploadMessage = null;

		}
	}

	@Override
	/**
	 * handles long presses for the browser, tries to get the
	 * url of the item that was clicked and sends it (it can be null)
	 * to the click handler that does cool stuff with it
	 */
	public void onLongPress() {
		if (mClickHandler == null) {
			mClickHandler = new ClickHandler(mContext);
		}
		Message click = mClickHandler.obtainMessage();
		if (click != null) {
			click.setTarget(mClickHandler);
		}
		mCurrentView.getWebView().requestFocusNodeHref(click);
	}

	@Override
	public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
		if (view == null) {
			return;
		}
		if (mCustomView != null && callback != null) {
			callback.onCustomViewHidden();
			return;
		}
		view.setKeepScreenOn(true);
		mOriginalOrientation = getRequestedOrientation();
		FrameLayout decor = (FrameLayout) getWindow().getDecorView();
		mFullscreenContainer = new FullscreenHolder(this);
		mCustomView = view;
		mFullscreenContainer.addView(mCustomView, COVER_SCREEN_PARAMS);
		decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
		setFullscreen(true);
		mCurrentView.setVisibility(View.GONE);
		if (view instanceof FrameLayout) {
			if (((FrameLayout) view).getFocusedChild() instanceof VideoView) {
				mVideoView = (VideoView) ((FrameLayout) view).getFocusedChild();
				mVideoView.setOnErrorListener(new VideoCompletionListener());
				mVideoView.setOnCompletionListener(new VideoCompletionListener());
			}
		}
		mCustomViewCallback = callback;
	}

	@Override
	public void onHideCustomView() {
		if (mCustomView == null || mCustomViewCallback == null || mCurrentView == null) {
			return;
		}
		Log.i(Constants.TAG, "onHideCustomView");
		mCurrentView.setVisibility(View.VISIBLE);
		mCustomView.setKeepScreenOn(false);
		setFullscreen(mPreferences.getBoolean(PreferenceConstants.HIDE_STATUS_BAR, false));
		FrameLayout decor = (FrameLayout) getWindow().getDecorView();
		if (decor != null) {
			decor.removeView(mFullscreenContainer);
		}

		if (API < 19) {
			try {
				mCustomViewCallback.onCustomViewHidden();
			} catch (Throwable ignored) {

			}
		}
		mFullscreenContainer = null;
		mCustomView = null;
		if (mVideoView != null) {
			mVideoView.setOnErrorListener(null);
			mVideoView.setOnCompletionListener(null);
			mVideoView = null;
		}
		setRequestedOrientation(mOriginalOrientation);
	}

	private class VideoCompletionListener implements MediaPlayer.OnCompletionListener,
			MediaPlayer.OnErrorListener {

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			return false;
		}

		@Override
		public void onCompletion(MediaPlayer mp) {
			onHideCustomView();
		}

	}

	/**
	 * turns on fullscreen mode in the app
	 * 
	 * @param enabled
	 *            whether to enable fullscreen or not
	 */
	public void setFullscreen(boolean enabled) {
		Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
		if (enabled) {
			winParams.flags |= bits;
		} else {
			winParams.flags &= ~bits;
			if (mCustomView != null) {
				mCustomView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			} else {
				mBrowserFrame.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			}
		}
		win.setAttributes(winParams);
	}

	/**
	 * a class extending FramLayout used to display fullscreen videos
	 */
	static class FullscreenHolder extends FrameLayout {

		public FullscreenHolder(Context ctx) {
			super(ctx);
			setBackgroundColor(ctx.getResources().getColor(android.R.color.black));
		}

		@SuppressLint("ClickableViewAccessibility")
		@Override
		public boolean onTouchEvent(MotionEvent evt) {
			return true;
		}

	}

	@Override
	/**
	 * a stupid method that returns the bitmap image to display in place of
	 * a loading video
	 */
	public Bitmap getDefaultVideoPoster() {
		if (mDefaultVideoPoster == null) {
			mDefaultVideoPoster = BitmapFactory.decodeResource(getResources(),
					android.R.drawable.ic_media_play);
		}
		return mDefaultVideoPoster;
	}

	@SuppressLint("InflateParams")
	@Override
	/**
	 * dumb method that returns the loading progress for a video
	 */
	public View getVideoLoadingProgressView() {
		if (mVideoProgressView == null) {
			LayoutInflater inflater = LayoutInflater.from(this);
			mVideoProgressView = inflater.inflate(R.layout.video_loading_progress, null);
		}
		return mVideoProgressView;
	}

	@Override
	/**
	 * handles javascript requests to create a new window in the browser
	 */
	public void onCreateWindow(boolean isUserGesture, Message resultMsg) {
		if (resultMsg == null) {
			return;
		}
		
		if (downloadTab != null) {
			downloadTab.getWebView().destroy();
		}
		downloadTab = new LightningView(mActivity, "", false, onGetDownloadDir());

		WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
		transport.setWebView(downloadTab.getWebView());
		resultMsg.sendToTarget();
	}

	@Override
	/**
	 * returns the Activity instance for this activity,
	 * very helpful when creating things in other classes... I think
	 */
	public Activity getActivity() {
		return mActivity;
	}

	/**
	 * it hides the action bar, seriously what else were you expecting
	 */
	@Override
	public void hideActionBar() {
		if (mActionBar.isShowing() && mFullScreen) {
			mActionBar.hide();
		}
	}

	@Override
	/**
	 * obviously it shows the action bar if it's hidden
	 */
	public void showActionBar() {
		if (!mActionBar.isShowing() && mFullScreen) {
			mActionBar.show();
		}
	}

	@Override
	/**
	 * handles a long click on the page, parameter String url 
	 * is the url that should have been obtained from the WebView touch node
	 * thingy, if it is null, this method tries to deal with it and find a workaround
	 */
	public void longClickPage(final String url) {
		HitTestResult result = null;
		if (mCurrentView.getWebView() != null) {
			result = mCurrentView.getWebView().getHitTestResult();
		}
		if (url != null) {
			if (result != null) {
				if (result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE
						|| result.getType() == HitTestResult.IMAGE_TYPE) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
								case DialogInterface.BUTTON_POSITIVE:
									newTab(url, false);
									break;

								case DialogInterface.BUTTON_NEGATIVE:
									mCurrentView.loadUrl(url);
									break;

								case DialogInterface.BUTTON_NEUTRAL:
									if (API > 8) {
										Utils.downloadFile(mActivity, url,
												mCurrentView.getUserAgent(), "attachment", false, onGetDownloadDir());
									}
									break;
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
					builder.setTitle(url.replace(Constants.HTTP, ""))
							.setMessage(getResources().getString(R.string.dialog_image))
							.setNegativeButton(getResources().getString(R.string.action_open),
									dialogClickListener)
							.setNeutralButton(getResources().getString(R.string.action_download),
									dialogClickListener).show();

				} else {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
								case DialogInterface.BUTTON_POSITIVE:
									newTab(url, false);
									break;

								case DialogInterface.BUTTON_NEGATIVE:
									mCurrentView.loadUrl(url);
									break;

								case DialogInterface.BUTTON_NEUTRAL:
									ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
									ClipData clip = ClipData.newPlainText("label", url);
									clipboard.setPrimaryClip(clip);
									break;
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
					builder.setTitle(url)
							.setMessage(getResources().getString(R.string.dialog_link))
							.setNegativeButton(getResources().getString(R.string.action_open),
									dialogClickListener)
							.setNeutralButton(getResources().getString(R.string.action_copy),
									dialogClickListener).show();
				}
			} else {
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case DialogInterface.BUTTON_POSITIVE:
								newTab(url, false);
								break;

							case DialogInterface.BUTTON_NEGATIVE:
								mCurrentView.loadUrl(url);
								break;

							case DialogInterface.BUTTON_NEUTRAL:
								ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
								ClipData clip = ClipData.newPlainText("label", url);
								clipboard.setPrimaryClip(clip);

								break;
						}
					}
				};

				AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
				builder.setTitle(url)
						.setMessage(getResources().getString(R.string.dialog_link))
						.setNegativeButton(getResources().getString(R.string.action_open),
								dialogClickListener)
						.setNeutralButton(getResources().getString(R.string.action_copy),
								dialogClickListener).show();
			}
		} else if (result != null) {
			if (result.getExtra() != null) {
				final String newUrl = result.getExtra();
				if (result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE
						|| result.getType() == HitTestResult.IMAGE_TYPE) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
								case DialogInterface.BUTTON_POSITIVE:
									newTab(newUrl, false);
									break;

								case DialogInterface.BUTTON_NEGATIVE:
									mCurrentView.loadUrl(newUrl);
									break;

								case DialogInterface.BUTTON_NEUTRAL:
									if (API > 8) {
										Utils.downloadFile(mActivity, newUrl,
												mCurrentView.getUserAgent(), "attachment", false, onGetDownloadDir());
									}
									break;
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
					builder.setTitle(newUrl.replace(Constants.HTTP, ""))
							.setMessage(getResources().getString(R.string.dialog_image))
							.setNegativeButton(getResources().getString(R.string.action_open),
									dialogClickListener)
							.setNeutralButton(getResources().getString(R.string.action_download),
									dialogClickListener).show();

				} else {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
								case DialogInterface.BUTTON_POSITIVE:
									newTab(newUrl, false);
									break;

								case DialogInterface.BUTTON_NEGATIVE:
									mCurrentView.loadUrl(newUrl);
									break;

								case DialogInterface.BUTTON_NEUTRAL:
									ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
									ClipData clip = ClipData.newPlainText("label", newUrl);
									clipboard.setPrimaryClip(clip);

									break;
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
					builder.setTitle(newUrl)
							.setMessage(getResources().getString(R.string.dialog_link))
							.setNegativeButton(getResources().getString(R.string.action_open),
									dialogClickListener)
							.setNeutralButton(getResources().getString(R.string.action_copy),
									dialogClickListener).show();
				}

			}

		}

	}

	/**
	 * This method lets the search bar know that the page is currently loading
	 * and that it should display the stop icon to indicate to the user that
	 * pressing it stops the page from loading
	 */  
	public void setIsLoading() {
		if (!mSearch.hasFocus()) {
			mSearchRightIcon = mDeleteIcon;
			updateSearchIcons();
		}
		mProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.browser_progress_loading_style));
	}

	/**
	 * This tells the search bar that the page is finished loading and it should
	 * display the refresh icon
	 */
	public void setIsFinishedLoading() {
		if (!mSearch.hasFocus()) {
			mSearchRightIcon = mRefreshIcon;
			updateSearchIcons();
		}
		mProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.browser_progress_not_loading_style));
	}

	/**
	 * handle presses on the refresh icon in the search bar, if the page is
	 * loading, stop the page, if it is done loading refresh the page.
	 * 
	 * See setIsFinishedLoading and setIsLoading for displaying the correct icon
	 */
	public void refreshOrStop() {
		if (mCurrentView != null) {
			if (mCurrentView.getProgress() < 100) {
				mCurrentView.stopLoading();
			} else {
				mCurrentView.reload();
			}
		}    
	}

	@Override
	public boolean isActionBarShowing() {
		if (mActionBar != null) { 
			return mActionBar.isShowing();
		} else {
			return false;
		}
	}

	// Override this, use finish() for Incognito, moveTaskToBack for Main
	public void closeActivity() {
		Intent home = new Intent("android.intent.action.MAIN");
		home.addCategory("android.intent.category.HOME");
		startActivity(home);
		this.overridePendingTransition(R.anim.test, R.anim.test);
		//finish();
	}

	public class SortIgnoreCase implements Comparator<HistoryItem> {

		public int compare(HistoryItem o1, HistoryItem o2) {
			return o1.getTitle().toLowerCase(Locale.getDefault())
					.compareTo(o2.getTitle().toLowerCase(Locale.getDefault()));
		}

	}
	
	private void setupBackgroundChangeOnTouch(final ImageButton imagebutton, final int upDrawableResource, final int downDrawableResource) {
		imagebutton.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN )
					imagebutton.setImageResource(downDrawableResource);

				else if (event.getAction() == MotionEvent.ACTION_UP)
					imagebutton.setImageResource(upDrawableResource);

				return false;
			}
		});
	}
	
	private void navigate() {
		navigate(mSearch.getText().toString(), false);
	}
	
	public void navigate(String url) {
		navigate(url, false);
	}
	
	private void navigate(String url, boolean fromSearch) {
		String finalUrl = url;

		if (! finalUrl.startsWith("http://") && ! finalUrl.startsWith("https://")) {
			finalUrl = "http://" + url;
		}

		String[] schemes = {"http","https"};
        UrlValidator urlValidator = new UrlValidator(schemes);

        if (urlValidator.isValid(finalUrl)) {
		    mCurrentView.loadUrl(finalUrl);
			mCurrentView.requestFocus();
		}else{
			searchTheWeb(url);
        }
	}
}
