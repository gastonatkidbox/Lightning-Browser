/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class SettingsActivity extends Activity {

	private static int API = android.os.Build.VERSION.SDK_INT;
	private SharedPreferences.Editor mEditPrefs;
	private SharedPreferences mPreferences;
	private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		mContext = this;
		init();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}

	@SuppressLint("NewApi")
	public void init() {
		// mPreferences storage
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

		mEditPrefs = mPreferences.edit();

		// initialize UI
		RelativeLayout layoutBlockAds = (RelativeLayout) findViewById(R.id.layoutAdBlock);
	

		if (API >= 19) {
			mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 0);
			mEditPrefs.commit();
		}
		boolean fullScreenBool = mPreferences.getBoolean(PreferenceConstants.FULL_SCREEN, false);
		
		String code = "HOLO";

		try {
			PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
			code = p.versionName;
		} catch (NameNotFoundException e) {
			// TODO add logging
			e.printStackTrace();
		}

		TextView version = (TextView) findViewById(R.id.versionCode);
		version.setText(code + "");


		RelativeLayout r4, licenses;
		r4 = (RelativeLayout) findViewById(R.id.setR4);
		licenses = (RelativeLayout) findViewById(R.id.layoutLicense);

		licenses.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// NOTE: In order to comply with the open source license,
				// it is advised that you leave this code so that the License
				// Activity may be viewed by the user.
				startActivity(new Intent(mContext, LicenseActivity.class));
			}

		});

		Switch fullScreen = new Switch(this);
		Switch adblock = new Switch(this);

		r4.addView(adblock);
		fullScreen.setChecked(fullScreenBool);

		adblock.setChecked(mPreferences.getBoolean(PreferenceConstants.BLOCK_ADS, false));

		initSwitch(fullScreen, adblock);
		clickListenerForSwitches(layoutBlockAds, adblock);

		RelativeLayout advanced = (RelativeLayout) findViewById(R.id.layoutAdvanced);

		advanced(advanced);
	}


	public void clickListenerForSwitches(RelativeLayout layoutBlockAds, final Switch adblock) {
		layoutBlockAds.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				adblock.setChecked(!adblock.isChecked());
			}

		});

	}

	public void initSwitch(Switch fullscreen, Switch adblock) {
		adblock.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.BLOCK_ADS, isChecked);
				mEditPrefs.commit();
			}

		});

		fullscreen.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.FULL_SCREEN, isChecked);
				mEditPrefs.commit();

			}

		});
	}

	public void initCheckBox(CheckBox location, CheckBox fullscreen, CheckBox flash) {
		location.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.LOCATION, isChecked);
				mEditPrefs.commit();

			}

		});
		flash.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				int n = 0;
				if (isChecked) {
					n = 1;
				}
				mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, n);
				mEditPrefs.commit();
				boolean flashInstalled = false;
				try {
					PackageManager pm = getPackageManager();
					ApplicationInfo ai = pm.getApplicationInfo("com.adobe.flashplayer", 0);
					if (ai != null) {
						flashInstalled = true;
					}
				} catch (NameNotFoundException e) {
					flashInstalled = false;
				}
				if (!flashInstalled && isChecked) {
					Utils.createInformativeDialog(SettingsActivity.this,
							getResources().getString(R.string.title_warning), getResources()
									.getString(R.string.dialog_adobe_not_installed));
					buttonView.setChecked(false);
					mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 0);
					mEditPrefs.commit();

				} else if ((API > 17) && isChecked) {
					Utils.createInformativeDialog(SettingsActivity.this,
							getResources().getString(R.string.title_warning), getResources()
									.getString(R.string.dialog_adobe_unsupported));
				}
			}

		});
		fullscreen.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.FULL_SCREEN, isChecked);
				mEditPrefs.commit();

			}

		});
	}

	public void advanced(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(mContext, AdvancedSettingsActivity.class));
			}

		});
	}
}
