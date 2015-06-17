package ru.korniltsev.telegram.auth.code;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMSReceiver /*extends BroadcastReceiver*/ {
    public static List<String> getMessages( Intent intent) {
        Bundle bundle = intent.getExtras();

        if (null == bundle) {
            return Collections.emptyList();
        }
        Object messages[];
        messages = (Object[]) bundle.get("pdus");
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
