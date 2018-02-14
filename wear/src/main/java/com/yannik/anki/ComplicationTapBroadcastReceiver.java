package com.yannik.anki;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

public class ComplicationTapBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent newIntent = new Intent();
        newIntent.setClassName("com.yannik.wear.anki", "com.yannik.anki.WearMainActivity");
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(newIntent);
    }

}
