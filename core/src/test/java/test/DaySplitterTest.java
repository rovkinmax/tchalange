package test;

import org.drinkless.td.libcore.telegram.TdApi;
import org.junit.Before;
import org.junit.Test;
import ru.korniltsev.telegram.core.rx.DaySplitter;
import ru.korniltsev.telegram.core.rx.RxChat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DaySplitterTest {
    private DaySplitter daySplitter;
    private Calendar cal;
    private int counter;

    @Before
    public void setup() {
        daySplitter = new DaySplitter();
        cal = Calendar.getInstance();
        counter = 0;
    }
    @Test
    public void testDifferentDays(){

        TdApi.Message messageA = createMessage();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        TdApi.Message messageB = createMessage();
        boolean sameDay = daySplitter.hasTheSameDay(messageA, messageB);
        assertFalse(sameDay);
    }

    @Test
    public void testSameDayDays(){
        cal.set(Calendar.HOUR_OF_DAY, 4);

        TdApi.Message messageA = createMessage();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        TdApi.Message messageB = createMessage();
        boolean sameDay = daySplitter.hasTheSameDay(messageA, messageB);
        assertTrue(sameDay);
    }

    @Test
    public void testSameTimeHasSamDays(){
        TdApi.Message message = createMessage();
        TdApi.Message message2 = createMessage();
        boolean sameDay = daySplitter.hasTheSameDay(message, message2);
        assertTrue(sameDay);
    }

    @Test
    public void testSplitter() {

        List<TdApi.Message> ms = new ArrayList<>();
        TdApi.Message a = createMessage();
        ms.add(a);

        cal.add(Calendar.DAY_OF_YEAR, 1);
        TdApi.Message b = createMessage();
        ms.add(b);

        List<RxChat.ChatListItem> split = daySplitter.split(ms);
        assertEquals(4, split.size());
        assertThat(split.get(1), instanceOf(RxChat.DaySeparatorItem.class));
        assertThat(split.get(3), instanceOf(RxChat.DaySeparatorItem.class));
        assertThat(split.get(0), instanceOf(RxChat.MessageItem.class));
        assertThat(split.get(2), instanceOf(RxChat.MessageItem.class));
        assertEquals(a.id, ((RxChat.MessageItem) split.get(0)).msg.id);
        assertEquals(b.id, ((RxChat.MessageItem) split.get(2)).msg.id);
    }

    @Test
    public void testMerge(){

        ArrayList<TdApi.Message> ms = new ArrayList<>();
        ms.add(createMessage());

        ArrayList<TdApi.Message> ms2 = new ArrayList<>();
        ms2.add(createMessage());

        List<RxChat.ChatListItem> split1 = daySplitter.split(ms);
        List<RxChat.ChatListItem> split2 = daySplitter.split(ms2);

        daySplitter.append(split1, split2);

        assertThat(split2.size(), equalTo(3));
        assertThat(split2.get(0), instanceOf(RxChat.MessageItem.class));
        assertThat(split2.get(1), instanceOf(RxChat.MessageItem.class));
        assertThat(split2.get(2), instanceOf(RxChat.DaySeparatorItem.class));
    }

    @Test
    public void testMergeToEmpty(){

        ArrayList<TdApi.Message> ms = new ArrayList<>();
        ms.add(createMessage());


        List<RxChat.ChatListItem> split1 = daySplitter.split(ms);
        List<RxChat.ChatListItem> split2 = daySplitter.split(new ArrayList<TdApi.Message>());

        daySplitter.append(split1, split2);

        assertThat(split2.size(), equalTo(2));
        assertThat(split2.get(0), instanceOf(RxChat.MessageItem.class));
        assertThat(split2.get(1), instanceOf(RxChat.DaySeparatorItem.class));
    }
    @Test
    public void testMergeFromEmpty(){

        ArrayList<TdApi.Message> ms = new ArrayList<>();
        ms.add(createMessage());


        List<RxChat.ChatListItem> split1 = daySplitter.split(new ArrayList<TdApi.Message>());
        List<RxChat.ChatListItem> split2 = daySplitter.split(ms);

        daySplitter.append(split1, split2);

        assertThat(split2.size(), equalTo(2));
        assertThat(split2.get(0), instanceOf(RxChat.MessageItem.class));
        assertThat(split2.get(1), instanceOf(RxChat.DaySeparatorItem.class));
    }

    @Test
    public void test(){
        cal.set(Calendar.HOUR_OF_DAY, 4);

        RxChat.DaySeparatorItem separator1 = daySplitter.createSeparator(createMessage());
        RxChat.DaySeparatorItem separator2 = daySplitter.createSeparator(createMessage());

        cal.add(Calendar.HOUR_OF_DAY, 1);
        RxChat.DaySeparatorItem separator3 = daySplitter.createSeparator(createMessage());

        cal.add(Calendar.DAY_OF_YEAR, 1);
        TdApi.Message msg4 = createMessage();
        RxChat.DaySeparatorItem separator4 = daySplitter.createSeparator(msg4);

        assertThat(separator1.id, equalTo(separator2.id));
        assertThat(separator1.time, equalTo(separator2.time));

        assertThat(separator1.id, equalTo(separator3.id));
        assertThat(separator1.time, equalTo(separator3.time));

        assertThat(separator3.id, not(equalTo(separator4.id)));
        assertThat(separator3.time, not(equalTo(separator4.time)));
    }

    private TdApi.Message createMessage() {
        TdApi.Message msg3 = new TdApi.Message();
        msg3.date = getTimeInSecs(cal);
        msg3.id = counter++;
        return msg3;
    }

    @Test
    public void testPrependEmpty(){
        ArrayList<RxChat.ChatListItem> ms = new ArrayList<>();
        List<RxChat.ChatListItem> prepended = daySplitter.prepend(new TdApi.Message(), ms);

        assertThat(ms.size(), equalTo(2));

        assertThat(prepended.size(), equalTo(2));

    }

    @Test
    public void testPrependSameDay() {
        cal.set(Calendar.HOUR_OF_DAY, 4);
        TdApi.Message message = createMessage();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        TdApi.Message message2 = createMessage();

        ArrayList<TdApi.Message> ms = new ArrayList<>();
        ms.add(message2);
        List<RxChat.ChatListItem> items = daySplitter.split(ms);

        List<RxChat.ChatListItem> prepended = daySplitter.prepend(message, items);

        assertThat(items.size(), equalTo(3));
        assertThat(prepended.size(), equalTo(1));
    }

    @Test
    public void testPrependDifferentDay(){

        TdApi.Message message = createMessage();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        TdApi.Message message2 = createMessage();

        ArrayList<TdApi.Message> ms = new ArrayList<>();
        ms.add(message2);
        List<RxChat.ChatListItem> items = daySplitter.split(ms);
        List<RxChat.ChatListItem> prepended = daySplitter.prepend(message, items);

        assertThat(items.size(), equalTo(4));
        assertThat(prepended.size(), equalTo(2));
    }


    private int getTimeInSecs(Calendar cal) {
        return (int) cal.getTimeInMillis()/1000;
    }
}
