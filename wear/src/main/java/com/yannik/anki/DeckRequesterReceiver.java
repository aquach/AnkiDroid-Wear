package com.yannik.anki;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.yannik.sharedvalues.CommonIdentifiers;


public class DeckRequesterReceiver extends BroadcastReceiver {
    final String TAG = "DeckRequestReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        fireMessage(context, null, CommonIdentifiers.W2P_REQUEST_DECKS);
    }

    private void fireMessage(final Context context, final String data, final String path) {
        final GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();

        Log.d(TAG, "Firing Request " + path);
        // Send the RPC
        com.google.android.gms.common.api.PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(
                googleApiClient);
        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                for (int i = 0; i < result.getNodes().size(); i++) {
                    Node node = result.getNodes().get(i);
                    String nName = node.getDisplayName();
                    String nId = node.getId();
                    Log.d(TAG, "Firing Message with path: " + path);

                    com.google.android.gms.common.api.PendingResult<MessageApi.SendMessageResult> messageResult = Wearable
                            .MessageApi.sendMessage(
                                    googleApiClient,
                                    node.getId(),
                                    path,
                                    (data == null ? "" : data).getBytes());
                    messageResult.setResultCallback(new ResultCallback<MessageApi
                            .SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Status status = sendMessageResult.getStatus();

                            Log.d(TAG, "Status: " + status.toString());
                        }
                    });
                }
            }
        });
    }
}
