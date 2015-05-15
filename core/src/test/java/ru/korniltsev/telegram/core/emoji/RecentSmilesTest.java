package ru.korniltsev.telegram.core.emoji;

import android.content.SharedPreferences;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

public class RecentSmilesTest {

    @Test
    public void testParsing() {
//        SharedPreferences pref = mock(SharedPreferences.class);
        /*doReturn("1,3,2")
                .when(pref)
                .getString(RecentSmiles.PREF_RECENT_SMILES, "");
        RecentSmiles recent = new RecentSmiles(pref);
        List<RecentSmiles.Entry> sortedRecentEmoji = recent.getSortedRecentEmoji();

        assertEquals(sortedRecentEmoji.size(), 3);
        assertEquals(sortedRecentEmoji.get(0).code, 1);
        assertEquals(sortedRecentEmoji.get(1).code, 3);
        assertEquals(sortedRecentEmoji.get(2).code, 2);*/
        fail();
    }

}