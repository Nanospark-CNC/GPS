package com.nanospark.gard.twilio;

import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Singleton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import mobi.tattu.utils.persistance.datastore.DataStore;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import rx.Observable;

/**
 * Created by Leandro on 26/7/2015.
 */
@Singleton
public class MessagesClient {

    private static final String TWILIO_ACCOUNT = "AC83aacf8a55784375290210a9eb924ad4";
    private static final String TWILIO_USER = "AC83aacf8a55784375290210a9eb924ad4";
    private static final String TWILIO_PASSWD = "3f5dab33644d33cacb4bf300194299eb";

    // Wed, 08 Jul 2015 23:54:32 +0000
    private static final SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

    private MessagesApi mApi;

    public MessagesClient() {
        Gson gson = new GsonBuilder()
                .create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://api.twilio.com")
                .setConverter(new GsonConverter(gson))
                .setRequestInterceptor(mBasicAuthInterceptor)
                .setLogLevel(RestAdapter.LogLevel.FULL).setLog(msg -> Log.i("retrofit", msg))
                .build();

        mApi = restAdapter.create(MessagesApi.class);
    }

    private RequestInterceptor mBasicAuthInterceptor = request -> {
        String credentials = TWILIO_USER + ":" + TWILIO_PASSWD;
        String string = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        request.addHeader("Authorization", string);
    };

    /**
     * Returns the last new message received since the last check.
     * If more than one message was received, then only the newest is returned.
     * If no new messages, then null is returned.
     * @return
     */
    public Observable<JsonObject> getNewMessage() {
        return mApi.getMessages(TWILIO_ACCOUNT).map(response -> {
            JsonArray messages = response.getAsJsonObject().getAsJsonArray("messages");
            if (messages.size() > 0) {
                for (JsonElement message : messages) {
                    JsonObject messageObj = message.getAsJsonObject();
                    String direction = messageObj.get("direction").getAsString();
                    if ("inbound".equals(direction)) {
                        try {
                            Date timestamp = formatter.parse(messageObj.get("date_sent").getAsString());
                            boolean after = true;
                            try {
                                Calendar lastCal = Calendar.getInstance();
                                lastCal.setTime(DataStore.getInstance().getObject("LAST_TIMESTAMP", Date.class));

                                Calendar messageCal = Calendar.getInstance();
                                messageCal.setTime(timestamp);

                                after = messageCal.after(lastCal);
                            } catch (DataStore.ObjectNotFoundException e) {
                            }
                            if (after) {
                                DataStore.getInstance().putObject("LAST_TIMESTAMP", timestamp);
                                return messageObj;
                            }
                        } catch (ParseException e) {
                            System.out.println(e.getMessage());
                        }
                        break;
                    }
                }
            }
            return null;
        });
    }

}
