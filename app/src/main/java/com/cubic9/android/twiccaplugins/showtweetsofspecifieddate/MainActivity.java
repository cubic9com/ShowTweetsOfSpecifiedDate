package com.cubic9.android.twiccaplugins.showtweetsofspecifieddate;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.CalendarView;

import java.util.Calendar;

/**
 * Main class of ShowTweetsOfSpecifiedDate.
 *
 * Have you wanted to see tweets of specified date?
 * "Show tweets of specified date" will show them.
 * This app is twicca<http://twicca.r246.jp/> plugin.
 * This app will work with twicca and twicca plugin-compatible twitter client.
 * (This app tested with TwitPane+, twicle plus, and Justaway.)
 *
 * The latest version of the sources are available at following URL.
 * <https://github.com/cubic9com/ShowTweetsOfSpecifiedDate/>
 *
 * Copyright (C) 2015, cubic9com All rights reserved.
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE for more information.
 *
 * @author cubic9com
 */
public class MainActivity extends Activity implements CalendarView.OnDateChangeListener, Button.OnClickListener {
    /** the package of official Twitter application */
    private static final String OFFICIAL_APP_PACKAGE = "com.twitter.android";

    /** page URL of searching Twitter */
    private static final String TWITTER_SEARCH_URL = "https://twitter.com/search?q=";
    /** page URL of Google play store */
    private static final String STORE_URL = "market://details?id=";

    /** separator string between key and value of filter */
    private static final String KEY_VALUE_SEPARATOR = "%3A";
    /** separator string between filters */
    private static final String FILTER_SEPARATOR = "%20";
    /** separator string between parts of date */
    private static final String DATE_SEPARATOR = "-";

    /** threshold to be regarded as too old */
    private static final long OLD_TWEET_THRESHOLD_DAYS = 8;
    /** hours of a day */
    private static final long HOURS_OF_DAY = 24;
    /** milliseconds of one hour */
    private static final long MILLIS_OF_ONE_HOUR = 60 * 60 * 1000;

    /** calendar for picking up date */
    CalendarView calendar;
    /** user name */
    String userScreenName;
    /** specified date by user */
    Calendar specifiedDate;

    Button submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intentIn = getIntent();
        if (intentIn == null) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }

        final String action = intentIn.getAction();
        if (action.equals("jp.r246.twicca.ACTION_SHOW_TWEET")) {
            userScreenName = intentIn.getStringExtra("user_screen_name");
            setContentView(R.layout.activity_main);
            calendar = (CalendarView) findViewById(R.id.calendar);
            calendar.setShowWeekNumber(false);
            calendar.setOnDateChangeListener(this);

            submit = (Button) findViewById(R.id.submit);
            submit.setOnClickListener(this);
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onSelectedDayChange(CalendarView view, int year, int month, int day) {
        specifiedDate = Calendar.getInstance();
        specifiedDate.set(year, month, day);
    }

    @Override
    public void onClick(View view) {
        if (System.currentTimeMillis() - specifiedDate.getTimeInMillis() < OLD_TWEET_THRESHOLD_DAYS * HOURS_OF_DAY * MILLIS_OF_ONE_HOUR) {
            // difference is less than threshold
            // search tweets by default twitter application.
            searchTweets(userScreenName, specifiedDate.getTimeInMillis(), false);
        } else {
            // difference is greater than threshold
            if (isOfficialAppEnabled()) {
                // search tweets by official Twitter application.
                Toast toast = Toast.makeText(this, getString(R.string.info_could_find_official_app, OLD_TWEET_THRESHOLD_DAYS), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                searchTweets(userScreenName, specifiedDate.getTimeInMillis(), true);
            } else {
                // show official Twitter application in play store.
                Toast toast = Toast.makeText(this, getString(R.string.info_could_not_find_official_app, OLD_TWEET_THRESHOLD_DAYS), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                showOfficialAppInPlayStore();
            }
        }

        setResult(RESULT_OK);
        finish();
    }

    /**
     * detect whether official Twitter app installed or not.
     *
     * @return installed or not
     */
    private boolean isOfficialAppEnabled() {
        try {
            PackageManager pm = getPackageManager();
            pm.getApplicationInfo(OFFICIAL_APP_PACKAGE, PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * search tweets of specified date.
     *
     * @param userScreenName user which posted a specified tweet
     * @param specified UNIX time of a spacified date
     * @param useOfficialApp flag for using the official Twitter app
     */
    private void searchTweets(String userScreenName, long specified, boolean useOfficialApp) {
        // calculation of term
        Calendar calenderSince = Calendar.getInstance();
        calenderSince.setTimeInMillis(specified - HOURS_OF_DAY / 2 * MILLIS_OF_ONE_HOUR);
        Calendar calenderUntil = Calendar.getInstance();
        calenderUntil.setTimeInMillis(specified + HOURS_OF_DAY * MILLIS_OF_ONE_HOUR);

        // query building
        StringBuilder query = new StringBuilder();
        query.append(TWITTER_SEARCH_URL);
        query.append("from");
        query.append(KEY_VALUE_SEPARATOR);
        query.append(userScreenName);
        query.append(FILTER_SEPARATOR);
        query.append("since");
        query.append(KEY_VALUE_SEPARATOR);
        query.append(calenderSince.get(Calendar.YEAR));
        query.append(DATE_SEPARATOR);
        query.append(calenderSince.get(Calendar.MONTH) + 1);
        query.append(DATE_SEPARATOR);
        query.append(calenderSince.get(Calendar.DATE));
        query.append(FILTER_SEPARATOR);
        query.append("until");
        query.append(KEY_VALUE_SEPARATOR);
        query.append(calenderUntil.get(Calendar.YEAR));
        query.append(DATE_SEPARATOR);
        query.append(calenderUntil.get(Calendar.MONTH) + 1);
        query.append(DATE_SEPARATOR);
        query.append(calenderUntil.get(Calendar.DATE));

        // searching with query
        final Uri uri = Uri.parse(query.toString());
        Intent intentOut = new Intent(Intent.ACTION_VIEW, uri);
        intentOut.addCategory(Intent.CATEGORY_DEFAULT);
        if (useOfficialApp) {
            intentOut.setPackage(OFFICIAL_APP_PACKAGE);
        }
        startActivity(intentOut);
    }

    /**
     * show official Twitter app in Google play store.
     */
    private void showOfficialAppInPlayStore() {
        final Uri uri = Uri.parse(STORE_URL + OFFICIAL_APP_PACKAGE);
        Intent intentOut = new Intent(Intent.ACTION_VIEW, uri);
        intentOut.addCategory(Intent.CATEGORY_DEFAULT);
        startActivity(intentOut);
    }
}
