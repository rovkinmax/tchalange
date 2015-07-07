package ru.korniltsev.telegram.auth.country;

import android.content.Context;
import android.support.annotation.Nullable;
import com.crashlytics.android.core.CrashlyticsCore;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

/**
 * Created by korniltsev on 23/04/15.
 */
@Singleton
public class Countries {
    public static final String RU_CODE = "RU";

    private List<Entry> data;
    private Map<String, Entry> countryCodeToCountry = new HashMap<>();
    private Map<String, Entry> phoneCodeToCountry = new HashMap<>();


    @Inject
    public Countries(Context appContext) {
        InputStream is = null;
        try {
            is = appContext.getAssets().open("countries.txt");
            ArrayList<Entry> res = new ArrayList<Entry>();
            Scanner s = new Scanner(is);
            int i =0;
            while (s.hasNextLine()) {
                String[] c = s.nextLine().split(";");
                String phoneCode = "+" + c[0];
                String countryCode = c[1];
                String countryName = c[2];
                Entry e = new Entry(i++, countryName, countryCode, phoneCode);
                res.add(e);
                countryCodeToCountry.put(countryCode, e);
                phoneCodeToCountry.put(phoneCode, e);
            }
            Collections.sort(res, new Comparator<Entry>() {
                @Override
                public int compare(Entry lhs, Entry rhs) {
                    return lhs.name.compareTo(rhs.name);
                }
            });
            this.data = Collections.unmodifiableList(res);
        } catch (IOException e) {
            this.data = Collections.emptyList();
            CrashlyticsCore.getInstance()
                    .logException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public Entry getForCode(String code) {
        return countryCodeToCountry.get(code);
    }

    public List<Entry> getData() {
        return data;
    }

    @Nullable
    public Entry getForPhonePrefix(String phonePrefix) {
        return phoneCodeToCountry.get(phonePrefix);
    }

    public static class Entry  implements Serializable{
        public final int position;
        public final String phoneCode;
        public final String code;
        public final String name;
        public final String firstLetter;

        public Entry(int pos, String name, String code, String phoneCode) {
            this.position = pos;
            this.name = name;
            this.code = code;
            this.phoneCode = phoneCode;
            firstLetter = String.valueOf(name.charAt(0));
        }

        public String localizedName(){
            final Locale locale = Locale.getDefault();
            if (locale != null
                    && locale.getLanguage().equals("ru")
                    && name.equals("Russian Federation")) {
                return "Россия";
            } else {
                return name;
            }
        }

    }
}
