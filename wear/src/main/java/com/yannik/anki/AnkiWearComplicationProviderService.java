package com.yannik.anki;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

import com.yannik.sharedvalues.CommonIdentifiers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.yannik.sharedvalues.CommonIdentifiers.P2W_COLLECTION_LIST_DECK_COUNT;
import static com.yannik.sharedvalues.CommonIdentifiers.P2W_COLLECTION_LIST_DECK_ID;

public class AnkiWearComplicationProviderService extends ComplicationProviderService {
    private static final String TAG = "AnkiWearCPS";

    @Override
    public void onComplicationActivated(int complicationId, int type, ComplicationManager manager) {
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        LocalBroadcastManager.getInstance(this).registerReceiver(new MessageReceiver(), messageFilter);

        PendingIntent broadcastToNumCardsUpdater = PendingIntent.getBroadcast(this, 0, new Intent(this, DeckRequesterReceiver.class), 0);
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        final int intervalSeconds = 600;
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), intervalSeconds * 1000, broadcastToNumCardsUpdater);
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

    private static class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            JSONObject js = null;
            String message = intent.getStringExtra("message");
            String path = intent.getStringExtra("path");
            if (message != null && path.equals(CommonIdentifiers.P2W_COLLECTION_LIST)) {
                int numCardsToStudy = 0;

                try {
                    js = new JSONObject(message);
                    JSONArray collectionNames = js.names();

                    for (int i = 0; i < collectionNames.length(); i++) {
                        try {
                            String colName = collectionNames.getString(i);
                            JSONObject deckObject = js.getJSONObject(colName);
                            long deckID = deckObject.getLong(P2W_COLLECTION_LIST_DECK_ID);
                            final JSONArray deckCounts = new JSONArray(deckObject.getString(P2W_COLLECTION_LIST_DECK_COUNT));

                            numCardsToStudy += deckCounts.getInt(0) + deckCounts.getInt(1) + deckCounts.getInt(2);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException " + e);
                }

                SharedPreferences settings = context.getSharedPreferences("DECK_TOTALS", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt("numCards", numCardsToStudy);
                editor.commit();

                ComponentName componentName = new ComponentName(context, AnkiWearComplicationProviderService.class);
                ProviderUpdateRequester providerUpdateRequester = new ProviderUpdateRequester(context, componentName);
                providerUpdateRequester.requestUpdateAll();
            }
        }
    }
}
