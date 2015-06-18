package ru.korniltsev.telegram.auth.country;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

/**
 * Created by korniltsev on 23/04/15.
 */
public class Countries {
    public static final String RU_CODE = "RU";

    private List<Entry> data;
    private Map<String, Entry> countryCodeToCountry = new HashMap<String, Entry>();


    public Countries() {

    }

    public Entry getForCode(String code, Context ctx) {
        getData(ctx);
        return countryCodeToCountry.get(code);
    }

    public List<Entry> getData(Context ctx) {
        if (data == null) {
            parse(ctx);
        }
        return data;
    }

    private void parse(Context ctx) {
        InputStream is = null;
        try {
            is = ctx.getAssets().open("countries.txt");
            ArrayList<Entry> res = new ArrayList<Entry>();
            Scanner s = new Scanner(is);
            while (s.hasNextLine()) {
                String[] c = s.nextLine().split(";");
                String phoneCode = "+" + c[0];
                String countryCode = c[1];
                String countryName = c[2];
                Entry e = new Entry(countryName, countryCode, phoneCode);
                res.add(e);
                countryCodeToCountry.put(countryCode, e);
            }
            Collections.sort(res);
            this.data = Collections.unmodifiableList(res);
        } catch (IOException e) {
            this.data = Collections.emptyList();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                }
            }
        }

    }

    public static class Entry implements Comparable<Entry> , Serializable{
        public final String phoneCode;
        public final String code;
        public final String name;
        public final String firstLetter;

        public Entry(String name, String code, String phoneCode) {
            this.name = name;
            this.code = code;
            this.phoneCode = phoneCode;
            firstLetter = String.valueOf(name.charAt(0));
        }


        @Override
        public int compareTo(Entry that) {
            return this.name.compareTo(that.name);
        }
    }
}
