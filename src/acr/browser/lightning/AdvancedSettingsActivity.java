/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.RelativeLayout;

public class AdvancedSettingsActivity extends Activity {

	// mPreferences variables
	private static final int API = android.os.Build.VERSION.SDK_INT;
	private SharedPreferences mPreferences;
	private Context mContext;
	private boolean mSystemBrowser;
	private Handler messageHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.advanced_settings);

		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setHomeButtonEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
		if (mPreferences.getBoolean(PreferenceConstants.HIDE_STATUS_BAR, false)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		mSystemBrowser = mPreferences.getBoolean(PreferenceConstants.SYSTEM_BROWSER_PRESENT, false);
		mContext = this;
		initialize();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}

	private void initialize() {

		RelativeLayout r8, r15, rClearCache;

		r8 = (RelativeLayout) findViewById(R.id.rClearHistory);
		r15 = (RelativeLayout) findViewById(R.id.r15);
		rClearCache = (RelativeLayout) findViewById(R.id.rClearCache);

		r8(r8);
		r15(r15);
		rClearCache(rClearCache);

		messageHandler = new MessageHandler(mContext);
	}

	private static class MessageHandler extends Handler {

		Context mHandlerContext;

		public MessageHandler(Context context) {
			this.mHandlerContext = context;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 1:
					Utils.showToast(mHandlerContext,
							mHandlerContext.getResources()
									.getString(R.string.message_clear_history));
					break;
				case 2:
					Utils.showToast(
							mHandlerContext,
							mHandlerContext.getResources().getString(
									R.string.message_cookies_cleared));
					break;
			}
			super.handleMessage(msg);
		}
	}

	private void r8(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedSettingsActivity.this); // dialog
				builder.setTitle(getResources().getString(R.string.title_clear_history));
				builder.setMessage(getResources().getString(R.string.dialog_history))
						.setPositiveButton(getResources().getString(R.string.action_yes),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface arg0, int arg1) {
										Thread clear = new Thread(new Runnable() {

											@Override
											public void run() {
												clearHistory();
											}

										});
										clear.start();
									}

								})
						.setNegativeButton(getResources().getString(R.string.action_no),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface arg0, int arg1) {
										// TODO Auto-generated method stub

									}

								}).show();
			}

		});
	}


	private void r15(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(AdvancedSettingsActivity.this); // dialog
				builder.setTitle(getResources().getString(R.string.title_clear_cookies));
				builder.setMessage(getResources().getString(R.string.dialog_cookies))
						.setPositiveButton(getResources().getString(R.string.action_yes),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface arg0, int arg1) {
										Thread clear = new Thread(new Runnable() {

											@Override
											public void run() {
												clearCookies();
											}

										});
										clear.start();
									}

								})
						.setNegativeButton(getResources().getString(R.string.action_no),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface arg0, int arg1) {

									}

								}).show();
			}

		});
	}

	private void rClearCache(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				clearCache();
			}

		});

	}

	public void clearCache() {
		WebView webView = new WebView(this);
		webView.clearCache(true);
		webView.destroy();
		Utils.showToast(mContext, getResources().getString(R.string.message_cache_cleared));
	}

	@SuppressWarnings("deprecation")
	public void clearHistory() {
		deleteDatabase(HistoryDatabaseHandler.DATABASE_NAME);
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
			} catch (Exception ignored) {
			}
		}
		SettingsController.setClearHistory(true);
		Utils.trimCache(this);
		messageHandler.sendEmptyMessage(1);
	}

	public void clearCookies() {
		CookieManager c = CookieManager.getInstance();
		CookieSyncManager.createInstance(this);
		c.removeAllCookie();
		messageHandler.sendEmptyMessage(2);
	}

	public void importFromStockBrowser() {
		BookmarkManager manager = new BookmarkManager(this);
		manager.importBookmarksFromBrowser();
	}
}
