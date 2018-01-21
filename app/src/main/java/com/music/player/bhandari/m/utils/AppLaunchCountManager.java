package com.music.player.bhandari.m.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.music.player.bhandari.m.MyApp;
import com.music.player.bhandari.m.R;
import com.music.player.bhandari.m.UIElementHelper.TypeFaceHelper;

/**
 * Created by Amit Bhandari on 2/21/2017.
 */

public class AppLaunchCountManager {



    private final static int DAYS_UNTIL_PROMPT = 1;//Min number of days
    private final static int DAYS_UNTIL_INTER_AD = 1;
    private final static int HOURS_UNTIL_BANNER_ADS = 1;
    private final static int DAYS_UNTIL_RATE_ASK = 1;

    public static void app_launched(Context mContext) {
        SharedPreferences prefs = mContext.getSharedPreferences("apprater", 0);
        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter
        long launch_count = prefs.getLong("launch_count", 0) + 1;
        editor.putLong("launch_count", launch_count);

        // Get date of first launch
        Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong("date_firstlaunch", date_firstLaunch);
        }

        // Wait at least n days before opening
        if (!prefs.getBoolean("dontshowagain", false)) {
            if (launch_count % 5 == 0) {
                if (System.currentTimeMillis() >= date_firstLaunch +
                        (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                    showRateDialog(mContext, editor);
                }
            }
        }

        editor.apply();
    }

    private static void showRateDialog(final Context mContext, final SharedPreferences.Editor editor) {

        new MaterialDialog.Builder(mContext)
                .typeface(TypeFaceHelper.getTypeFace(mContext),TypeFaceHelper.getTypeFace(mContext))
                .title("Hello there!")
                .content("This is Amit (developer of AB Music) and I hope you" +
                        " are enjoying AB Music as much as I enjoyed developing it. Please consider rating and leaving review for "
                        + mContext.getString(R.string.app_name)
                        + " on store, you will bring smile on my face. Thank you in advance!")
                .positiveText("Rate now!")
                .neutralText("Never")
                .negativeText("Later maybe")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        final String appPackageName = mContext.getPackageName(); // getPackageName() from Context or Activity object
                        try {
                            mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                        } catch (android.content.ActivityNotFoundException anfe) {
                            mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                        }
                    }
                })
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (editor != null) {
                            editor.putBoolean("dontshowagain", true);
                            editor.commit();
                        }
                    }
                })
                .show();


    }

    public static void nowPlayingLaunched(){
        SharedPreferences prefs = MyApp.getContext().getSharedPreferences("apprater", 0);
        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter
        long launch_count = prefs.getLong("launch_count_now_playing", 0) + 1;
        editor.putLong("launch_count_now_playing", launch_count);

        editor.apply();
    }

    public static long getNowPlayingLaunchCount(){
        SharedPreferences prefs = MyApp.getContext().getSharedPreferences("apprater", 0);
        if(prefs==null){
            return -1;
        }
        return prefs.getLong("launch_count_now_playing", -1);
    }

    public static void instantLyricsLaunched(){
        SharedPreferences prefs = MyApp.getContext().getSharedPreferences("apprater", 0);
        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter
        long launch_count = prefs.getLong("launch_count_instantLyrics", 0) + 1;
        editor.putLong("launch_count_instantLyrics", launch_count);

        editor.apply();
    }

    public static long getInstantLyricsCount(){
        SharedPreferences prefs = MyApp.getContext().getSharedPreferences("apprater", 0);
        if(prefs==null){
            return -1;
        }
        return prefs.getLong("launch_count_instantLyrics", -1);
    }

    public static boolean isEligibleForInterstialAd() {
        SharedPreferences prefs = MyApp.getContext().getSharedPreferences("apprater", 0);
        Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
        return date_firstLaunch != 0 && System.currentTimeMillis() >= date_firstLaunch + (DAYS_UNTIL_INTER_AD * 24 * 60 * 60 * 1000);
    }

    public static boolean isEligibleForRatingAsk(){
        SharedPreferences prefs = MyApp.getContext().getSharedPreferences("apprater", 0);
        Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
        return date_firstLaunch != 0 && System.currentTimeMillis() >= date_firstLaunch + (DAYS_UNTIL_RATE_ASK * 24 * 60 * 60 * 1000);
    }

    public static boolean isEligibleForBannerAds(){
        SharedPreferences prefs = MyApp.getContext().getSharedPreferences("apprater", 0);
        Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
        return date_firstLaunch != 0 && System.currentTimeMillis() >= date_firstLaunch + (HOURS_UNTIL_BANNER_ADS * 60 * 60 * 1000);
    }

}