package com.yannik.anki;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

/**
 * Example watch face complication data provider provides a number that can be incremented on tap.
 */
public class AnkiWearComplicationProviderService extends ComplicationProviderService {
    private static final String TAG = "AnkiWearCPS";

    private static SharedPreferences.OnSharedPreferenceChangeListener globalListener = null;
    private static int numComplications = 0;

    @Override
    public void onComplicationActivated(int complicationId, int type, ComplicationManager manager) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences("DECK_TOTALS", 0);

        final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                ComponentName componentName =
                        new ComponentName(getApplicationContext(), AnkiWearComplicationProviderService.class);

                ProviderUpdateRequester providerUpdateRequester =
                        new ProviderUpdateRequester(getApplicationContext(), componentName);
                providerUpdateRequester.requestUpdateAll();
            }
        };

        if (globalListener == null) {
            globalListener = listener;
            settings.registerOnSharedPreferenceChangeListener(listener);
        }

        numComplications++;
    }

    @Override
    public void onComplicationDeactivated(int complicationId) {
        numComplications--;

        if (numComplications == 0) {
            SharedPreferences settings = getApplicationContext().getSharedPreferences("DECK_TOTALS", 0);
            settings.unregisterOnSharedPreferenceChangeListener(globalListener);
            globalListener = null;
        }
    }

    @Override
    public void onComplicationUpdate(int complicationId, int dataType, ComplicationManager complicationManager) {
        ComplicationData complicationData = null;

        switch (dataType) {
            case ComplicationData.TYPE_SHORT_TEXT:
                Intent intent = new Intent(this, ComplicationTapBroadcastReceiver.class);

                SharedPreferences settings = getApplicationContext().getSharedPreferences("DECK_TOTALS", 0);
                final int numCards = settings.getInt("numCards", 0);
                complicationData =
                        new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                                .setIcon(Icon.createWithResource(getApplicationContext(), android.R.drawable.star_on))
                                .setShortText(ComplicationText.plainText("" + numCards))
                                .setTapAction(PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                                .build();
                break;
            default:
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Unexpected complication type " + dataType);
                }
        }

        if (complicationData != null) {
            complicationManager.updateComplicationData(complicationId, complicationData);
        } else {
            complicationManager.noUpdateRequired(complicationId);
        }
    }
}
