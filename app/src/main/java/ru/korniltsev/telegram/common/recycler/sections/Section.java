package ru.korniltsev.telegram.common.recycler.sections;

import ru.korniltsev.telegram.auth.country.Adapter;
import ru.korniltsev.telegram.auth.country.Countries;

import java.util.ArrayList;
import java.util.List;

public class Section<T> extends Item<T> {
    public final String firstChar;
    public Section(int firstPosition, long id, T data, String firstChar) {
        super(firstPosition, id, data);
        this.firstChar = firstChar;
    }

    public static  <T> List<Item<T>> prepareListOf(List<T> data, SectionFactory<T> factory) {
        final ArrayList<Item<T>> res = new ArrayList<>();
        T previous = null;
        int lastSectionPos = -1;
        for (T it : data) {
            final String itSection = factory.sectionForItem(it);
            if (previous == null ||
                    !factory.sectionForItem(previous).equals(itSection)) {
                lastSectionPos = res.size();
                long sectionId = getSectionId(itSection);
                res.add(new Section<>(lastSectionPos, sectionId, it, itSection));
            }
            res.add(new Item<T>( lastSectionPos, factory.id(it), it));
            previous = it;
        }

        return res;
    }

    private static String firstCharOf(String itSection) {
        if (itSection.isEmpty()) {
            return "";
        } else {
            return itSection.substring(0,1);
        }
    }

    private static int getSectionId(String itSection) {
        if (itSection.isEmpty()) {
            return Integer.MIN_VALUE;
        } else {
            return itSection.charAt(0);
        }
    }

    public interface SectionFactory<T>{
        //at most one char section
        String sectionForItem(T t);
        //positive id
        long id(T t);
    }
}
