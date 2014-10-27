/*
 * Copyright 2014 A.C.R. Development
 */
package net.kidbox.browser;

import android.os.Environment;

public final class Constants {

	private Constants() {
	}

	public static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36";
	public static final String MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.4; en-us; Nexus 4 Build/JOP24G) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";
	public static final int API = android.os.Build.VERSION.SDK_INT;
	public static final String GOOGLE_SEARCH = "https://www.google.com/search?client=lightning&ie=UTF-8&oe=UTF-8&q=";
	public static final String HOMEPAGE = "about:home";
	public static final String EXTERNAL_STORAGE = Environment.getExternalStorageDirectory().toString();

	public static final String SEPARATOR = "\\|\\$\\|SEPARATOR\\|\\$\\|";
	public static final String HTTP = "http://";
	public static final String HTTPS = "https://";
	public static final String FILE = "file://";
	public static final String FOLDER = "folder://";
	public static final String TAG = "Lightning";
}
