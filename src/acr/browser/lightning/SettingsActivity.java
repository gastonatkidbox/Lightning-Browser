/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
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
	private String mHomepage;
	private TextView mHomepageText;
	private SharedPreferences mPreferences;
	private Context mContext;
	private Activity mActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		mContext = this;
		mActivity = this;
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
		RelativeLayout layoutFullScreen = (RelativeLayout) findViewById(R.id.layoutFullScreen);
		RelativeLayout layoutFlash = (RelativeLayout) findViewById(R.id.layoutFlash);
		RelativeLayout layoutBlockAds = (RelativeLayout) findViewById(R.id.layoutAdBlock);
	

		mHomepageText = (TextView) findViewById(R.id.homepageText);
		if (API >= 19) {
			mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 0);
			mEditPrefs.commit();
		}
		boolean locationBool = mPreferences.getBoolean(PreferenceConstants.LOCATION, false);
		int flashNum = mPreferences.getInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 0);
		boolean fullScreenBool = mPreferences.getBoolean(PreferenceConstants.FULL_SCREEN, false);
		mHomepage = mPreferences.getString(PreferenceConstants.HOMEPAGE, Constants.HOMEPAGE);
		
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

		if (mHomepage.contains("about:home")) {
			mHomepageText.setText(getResources().getString(R.string.action_homepage));
		} else if (mHomepage.contains("about:blank")) {
			mHomepageText.setText(getResources().getString(R.string.action_blank));
		} else if (mHomepage.contains("about:bookmarks")) {
			mHomepageText.setText(getResources().getString(R.string.action_bookmarks));
		} else {
			mHomepageText.setText(mHomepage);
		}

		RelativeLayout r2, r3, r4, licenses;
		r2 = (RelativeLayout) findViewById(R.id.setR2);
		r3 = (RelativeLayout) findViewById(R.id.setR3);
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

		Switch location = new Switch(this);
		Switch fullScreen = new Switch(this);
		Switch flash = new Switch(this);
		Switch adblock = new Switch(this);

		r2.addView(fullScreen);
		r3.addView(flash);
		r4.addView(adblock);
		location.setChecked(locationBool);
		fullScreen.setChecked(fullScreenBool);
		if (flashNum > 0) {
			flash.setChecked(true);
		} else {
			flash.setChecked(false);
		}
		adblock.setChecked(mPreferences.getBoolean(PreferenceConstants.BLOCK_ADS, false));

		initSwitch(location, fullScreen, flash, adblock);
		clickListenerForSwitches(layoutFullScreen, layoutFlash, layoutBlockAds,
				location, fullScreen, flash, adblock);

		RelativeLayout homepage = (RelativeLayout) findViewById(R.id.layoutHomepage);
		RelativeLayout advanced = (RelativeLayout) findViewById(R.id.layoutAdvanced);

		homepage(homepage);
		advanced(advanced);
	}


	public void clickListenerForSwitches(RelativeLayout two,
			RelativeLayout three, RelativeLayout layoutBlockAds,
			final Switch loc, final Switch full, final Switch flash, final Switch adblock) {
		layoutBlockAds.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				adblock.setChecked(!adblock.isChecked());
			}

		});

		two.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				full.setChecked(!full.isChecked());
			}

		});
		three.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (API < 19) {
					flash.setChecked(!flash.isChecked());
				} else {
					Utils.createInformativeDialog(mContext,
							getResources().getString(R.string.title_warning), getResources()
									.getString(R.string.dialog_adobe_dead));
				}
			}

		});
	}

	public void initSwitch(Switch location, Switch fullscreen, Switch flash, Switch adblock) {
		adblock.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.BLOCK_ADS, isChecked);
				mEditPrefs.commit();
			}

		});
		location.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.LOCATION, isChecked);
				mEditPrefs.commit();

			}

		});
		flash.setEnabled(API < 19);
		flash.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					getFlashChoice();
				} else {
					mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 0);
					mEditPrefs.commit();
				}

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

				} else if ((API >= 17) && isChecked) {
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

	private void getFlashChoice() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setTitle(mContext.getResources().getString(R.string.title_flash));
		builder.setMessage(getResources().getString(R.string.flash))
				.setCancelable(true)
				.setPositiveButton(getResources().getString(R.string.action_manual),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 1);
								mEditPrefs.commit();
							}
						})
				.setNegativeButton(getResources().getString(R.string.action_auto),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 2);
								mEditPrefs.commit();
							}
						}).setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 0);
						mEditPrefs.commit();
					}

				});
		AlertDialog alert = builder.create();
		alert.show();
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

	public void homePicker() {
		final AlertDialog.Builder homePicker = new AlertDialog.Builder(mActivity);
		homePicker.setTitle(getResources().getString(R.string.title_custom_homepage));
		final EditText getHome = new EditText(this);
		mHomepage = mPreferences.getString(PreferenceConstants.HOMEPAGE, Constants.HOMEPAGE);
		if (!mHomepage.startsWith("about:")) {
			getHome.setText(mHomepage);
		} else {
			getHome.setText("http://www.google.com");
		}
		homePicker.setView(getHome);
		homePicker.setPositiveButton(getResources().getString(R.string.action_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = getHome.getText().toString();
						mEditPrefs.putString(PreferenceConstants.HOMEPAGE, text);
						mEditPrefs.commit();
						mHomepageText.setText(text);
					}
				});
		homePicker.show();
	}

	public void homepage(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
				picker.setTitle(getResources().getString(R.string.home));
				mHomepage = mPreferences
						.getString(PreferenceConstants.HOMEPAGE, Constants.HOMEPAGE);
				int n;
				if (mHomepage.contains("about:home")) {
					n = 1;
				} else if (mHomepage.contains("about:blank")) {
					n = 2;
				} else if (mHomepage.contains("about:bookmarks")) {
					n = 3;
				} else {
					n = 4;
				}

				picker.setSingleChoiceItems(R.array.homepage, n - 1,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {

								switch (which + 1) {
									case 1:
										mEditPrefs.putString(PreferenceConstants.HOMEPAGE,
												"about:home");
										mEditPrefs.commit();
										mHomepageText.setText(getResources().getString(
												R.string.action_homepage));
										break;
									case 2:
										mEditPrefs.putString(PreferenceConstants.HOMEPAGE,
												"about:blank");
										mEditPrefs.commit();
										mHomepageText.setText(getResources().getString(
												R.string.action_blank));
										break;
									case 3:
										mEditPrefs.putString(PreferenceConstants.HOMEPAGE,
												"about:bookmarks");
										mEditPrefs.commit();
										mHomepageText.setText(getResources().getString(
												R.string.action_bookmarks));

										break;
									case 4:
										homePicker();

										break;
								}
							}
						});
				picker.setNeutralButton(getResources().getString(R.string.action_ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {

							}
						});
				picker.show();
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
