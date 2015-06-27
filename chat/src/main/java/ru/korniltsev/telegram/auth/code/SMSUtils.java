package ru.korniltsev.telegram.auth.code;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.telephony.SmsMessage;
import com.crashlytics.android.core.CrashlyticsCore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SMSUtils  {
    public static List<String> getMessages( Intent intent) {
        try {
            return getMessagesInsecure(intent);
        } catch (SecurityException e) {
            return Collections.emptyList();
        } catch (Exception e) {
            CrashlyticsCore.getInstance()
                    .logException(e);
            return Collections.emptyList();
        }
    }

    @NonNull
    private static List<String> getMessagesInsecure(Intent intent) {
        Bundle bundle = intent.getExtras();

        if (null == bundle) {
            return Collections.emptyList();
        }
        Object messages[] = (Object[]) bundle.get("pdus");
        if (messages == null) {
            return Collections.emptyList();
        }
        Map<String, String> smses = new HashMap<>();
        for (Object message : messages) {
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) message);
            String smsText = nullless(sms.getDisplayMessageBody());
            String smsSender = nullless(sms.getOriginatingAddress());

            String prevText = smses.remove(smsSender);
            if (prevText != null) {
                smsText = prevText.concat(smsText);
            }
            smses.put(smsSender, smsText);
        }

        return new ArrayList<>(smses.values());
    }

    private static String nullless(String text) {
        if (text == null) {
            return "";
        }
        return text;
    }

//    @Override
//    public void onReceive(Context context, Intent intent) {
//        List<String> messages = getMessages(intent);
//        System.out.println();
//    }
}
