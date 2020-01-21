/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/

package org.apache.cordova.splashscreen;

import android.graphics.Rect;
import android.graphics.Point;
import android.widget.FrameLayout;
import android.widget.VideoView;
import android.media.MediaPlayer;
import android.net.Uri;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.content.res.Resources;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

public class SplashScreen extends CordovaPlugin {
    private static final String LOG_TAG = "SplashScreen";
    // Cordova 3.x.x has a copy of this plugin bundled with it (SplashScreenInternal.java).
    // Enable functionality only if running on 4.x.x.
    private static final boolean HAS_BUILT_IN_SPLASH_SCREEN = Integer.valueOf(CordovaWebView.CORDOVA_VERSION.split("\\.")[0]) < 4;
    private static final int DEFAULT_SPLASHSCREEN_DURATION = 3000;
    private static final int DEFAULT_FADE_DURATION = 500;
    private static Dialog splashDialog;
    private static ProgressDialog spinnerDialog;
    private static boolean firstShow = true;
    private static boolean lastHideAfterDelay; // https://issues.apache.org/jira/browse/CB-9094

    /**
     * Displays the splash drawable.
     */
    private VideoView videoHolder;
    /**
     * Remember last device orientation to detect orientation changes.
     */
    private int orientation;

    // Helper to be compile-time compatible with both Cordova 3.x and 4.x.
    private View getView() {
        try {
            return (View)webView.getClass().getMethod("getView").invoke(webView);
        } catch (Exception e) {
            return (View)webView;
        }
    }

    @Override
    protected void pluginInitialize() {
        if (HAS_BUILT_IN_SPLASH_SCREEN) {
            return;
        }
        // Make WebView invisible while loading URL
        getView().setVisibility(View.INVISIBLE);
        int drawableId = preferences.getInteger("SplashDrawableId", 0);
        if (drawableId == 0) {
            String splashResource = preferences.getString("SplashScreen", "screen");
            if (splashResource != null) {
                drawableId = cordova.getActivity().getResources().getIdentifier(splashResource, "drawable", cordova.getActivity().getClass().getPackage().getName());
                if (drawableId == 0) {
                    drawableId = cordova.getActivity().getResources().getIdentifier(splashResource, "drawable", cordova.getActivity().getPackageName());
                }
                preferences.set("SplashDrawableId", drawableId);
            }
        }

        // Save initial orientation.
        orientation = cordova.getActivity().getResources().getConfiguration().orientation;

        if (firstShow) {
            boolean autoHide = preferences.getBoolean("AutoHideSplashScreen", true);
            showSplashScreen(autoHide);
        }

        if (preferences.getBoolean("SplashShowOnlyFirstTime", true)) {
            firstShow = false;
        }
    }

    /**
     * Shorter way to check value of "SplashMaintainAspectRatio" preference.
     */


    @Override
    public void onPause(boolean multitasking) {
        if (HAS_BUILT_IN_SPLASH_SCREEN) {
            return;
        }
        // hide the splash screen to avoid leaking a window
        this.removeSplashScreen(true);
    }

    @Override
    public void onDestroy() {
        if (HAS_BUILT_IN_SPLASH_SCREEN) {
            return;
        }
        // hide the splash screen to avoid leaking a window
        this.removeSplashScreen(true);
        // If we set this to true onDestroy, we lose track when we go from page to page!
        //firstShow = true;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("hide")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    webView.postMessage("splashscreen", "hide");
                }
            });
        } else if (action.equals("show")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    webView.postMessage("splashscreen", "show");
                }
            });
        } else {
            return false;
        }

        callbackContext.success();
        return true;
    }

    @Override
    public Object onMessage(String id, Object data) {
        if (HAS_BUILT_IN_SPLASH_SCREEN) {
            return null;
        }
        if ("splashscreen".equals(id)) {
            if ("hide".equals(data.toString())) {
                this.removeSplashScreen(false);
            } else {
                this.showSplashScreen(false);
            }
        } else if ("spinner".equals(id)) {
            if ("stop".equals(data.toString())) {
                getView().setVisibility(View.VISIBLE);
            }
        } else if ("onReceivedError".equals(id)) {
            //this.spinnerStop();
        }
        return null;
    }

    private void removeSplashScreen(final boolean forceHideImmediately) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (splashDialog != null && splashDialog.isShowing()) {
                    splashDialog.dismiss();
                    splashDialog = null;
                    videoHolder = null;

                }
            }
        });
    }

    /**
     * Shows the splash screen over the full Activity
     */
    @SuppressWarnings("deprecation")
    private void showSplashScreen(final boolean hideAfterDelay) {
        // If the splash dialog is showing don't try to show it again
        if (splashDialog != null && splashDialog.isShowing()) {
            return;
        }
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                // Get reference to display
                Display display = cordova.getActivity().getWindowManager().getDefaultDisplay();
                final Context context = webView.getContext();
                // Create and show the dialog
                videoHolder = new VideoView(context);
                //Uri video = Uri.parse("android.resource://com.telenorhealth.tonicapp/raw/splash");
                Resources activityRes = cordova.getActivity().getResources();
                int closeResId = activityRes.getIdentifier("start_animation", "raw", cordova.getActivity().getPackageName());
                Uri video = Uri.parse("android.resource://" + cordova.getActivity().getClass().getPackage().getName() + "/"+ closeResId/*R.raw.start_animation*/);
                videoHolder.setVideoURI(video);
                videoHolder.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        final Handler handler = new Handler(); //hide splash after delay
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                removeSplashScreen(true);
                            }
                        }, 2000);
                    }
                });

                videoHolder.start();

                //splashDialog = new Dialog(context, android.R.style.videosplash);
                // splashDialog = new Dialog(context, android.R.style.Theme_DeviceDefault_Dialog);
                splashDialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);

                splashDialog.getWindow().setGravity(Gravity.CENTER_HORIZONTAL);
                //splashDialog.getWindow().setGravity(Gravity.CENTER_VERTICAL);


                videoHolder.setZOrderOnTop(true);
                videoHolder.post(new Runnable() { //video size and xy
                    @Override
                    public void run() {
                        //display size determination
                        Display ddisplay = splashDialog.getWindow().getWindowManager().getDefaultDisplay();
                        Rect wRect = new Rect();
                        splashDialog.getWindow().getDecorView().getWindowVisibleDisplayFrame(wRect);
                        Point dsize = new Point();
                        ddisplay.getSize(dsize);
                        int sheight = wRect.bottom;
                        int swidth = dsize.x;
                        //FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(swidth,sheight+250);
                        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(swidth+160,sheight+100);
                        lp.gravity=Gravity.CENTER;
                        videoHolder.setLayoutParams(lp);
                    }
                });
                splashDialog.setContentView(videoHolder);
                splashDialog.setCancelable(false);
                splashDialog.show();
                //add Video view background as a backup. if the video does not play
                final int screenResId = activityRes.getIdentifier("screen", "drawable", cordova.getActivity().getPackageName());
                final Handler delayer = new Handler();
                delayer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        videoHolder.setBackgroundResource(screenResId/*R.drawable.screen*/);
                    }
                }, 1000);
            }
        });
    }
}
