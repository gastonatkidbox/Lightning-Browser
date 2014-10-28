/*
 * Copyright 2014 A.C.R. Development
 */
package net.kidbox.browser.activities;

import net.kidbox.browser.R;
import net.kidbox.browser.R.id;
import net.kidbox.browser.R.layout;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

/*
 *NOTE: This activity must not be removed in order to comply with the Mozilla Public License v. 2.0 
 *under which this code is licensed. Unless you plan on providing other attribution in the app to 
 *the original source in another visible way, it is advised against the removal of this Activity.
 */
public class LicenseActivity extends Activity implements View.OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.license_activity);

		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		findViewById(R.id.kidboxBrowserLicense).setOnClickListener(this);
		findViewById(R.id.lightningBrowserLicense).setOnClickListener(this);
		findViewById(R.id.licenseAOSP).setOnClickListener(this);
		findViewById(R.id.licenseHosts).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.kidboxBrowserLicense) {
			actionView("http://www.mozilla.org/MPL/2.0/");
			
		}else if (v.getId() == R.id.lightningBrowserLicense) {
			actionView("http://www.mozilla.org/MPL/2.0/");
			
		}else if (v.getId() == R.id.licenseAOSP) {
			actionView("http://www.apache.org/licenses/LICENSE-2.0");
			
		}else if (v.getId() == R.id.licenseHosts) {
			actionView("http://hosts-file.net/");
		}
	}

	private void actionView(String url) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url), this, MainActivity.class));
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return super.onOptionsItemSelected(item);
	}
}
