package com.example.xyzreader.data;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.text.format.Time;
import android.util.Log;

import com.example.xyzreader.remote.RemoteEndpointUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.example.xyzreader.data.UpdaterService.UpdateResult.RESULT_FAILED_INTERNET;
import static com.example.xyzreader.data.UpdaterService.UpdateResult.RESULT_FAILED_SERVER;
import static com.example.xyzreader.data.UpdaterService.UpdateResult.RESULT_OK;
import static com.example.xyzreader.data.UpdaterService.UpdateResult.RESULT_REFRESHING;

public class UpdaterService extends IntentService {
    private static final String TAG = "UpdaterService";

    public static final String BROADCAST_ACTION_STATE_CHANGE
            = "com.example.xyzreader.intent.action.STATE_CHANGE";
    public static final String EXTRA_REFRESHING
            = "com.example.xyzreader.intent.extra.REFRESHING";

    public interface UpdateResult {
        int RESULT_OK = 200;
        int RESULT_REFRESHING = 0;
        int RESULT_FAILED_SERVER = 500;
        int RESULT_FAILED_INTERNET = 400;
    }

    public UpdaterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Time time = new Time();

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Log.w(TAG, "Not online, not refreshing.");
            sendStickyBroadcast(
                    new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, RESULT_FAILED_INTERNET));
            return;
        }

        sendStickyBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, RESULT_REFRESHING));

        // Don't even inspect the intent, we only do one thing, and that's fetch content.
        ArrayList<ContentProviderOperation> cpo = new ArrayList<ContentProviderOperation>();

        Uri dirUri = ItemsContract.Items.buildDirUri();

        // Delete all items
        cpo.add(ContentProviderOperation.newDelete(dirUri).build());

        int result = RESULT_OK;

        try {
            JSONArray array = RemoteEndpointUtil.fetchJsonArray();
            if (array == null) {
                throw new JSONException("Invalid parsed item array" );
            }

            for (int i = 0; i < array.length(); i++) {
                ContentValues values = new ContentValues();
                JSONObject object = array.getJSONObject(i);
                values.put(ItemsContract.Items.SERVER_ID, object.getString("id" ));
                values.put(ItemsContract.Items.AUTHOR, object.getString("author" ));
                values.put(ItemsContract.Items.TITLE, object.getString("title" ));
                values.put(ItemsContract.Items.BODY, object.getString("body" ));
                values.put(ItemsContract.Items.THUMB_URL, object.getString("thumb" ));
                values.put(ItemsContract.Items.PHOTO_URL, object.getString("photo" ));
                values.put(ItemsContract.Items.ASPECT_RATIO, object.getString("aspect_ratio" ));
                time.parse3339(object.getString("published_date"));
                values.put(ItemsContract.Items.PUBLISHED_DATE, time.toMillis(false));
                cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
            }

            getContentResolver().applyBatch(ItemsContract.CONTENT_AUTHORITY, cpo);

        } catch (JSONException | RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Error updating content.", e);
            result = RESULT_FAILED_SERVER;
        }

        sendStickyBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, result));
    }
}
