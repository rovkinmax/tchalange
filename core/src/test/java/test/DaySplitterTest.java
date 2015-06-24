package test;

import org.drinkless.td.libcore.telegram.TdApi;
import org.junit.Before;
import org.junit.Test;
import ru.korniltsev.telegram.core.rx.DaySplitter;
import ru.korniltsev.telegram.core.rx.RxChat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

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
//        cal.setTimeZone(TimeZone.);
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
        assertThat(separator1.day, equalTo(separator2.day));

        assertThat(separator1.id, equalTo(separator3.id));
        assertThat(separator1.day, equalTo(separator3.day));

        assertThat(separator3.id, not(equalTo(separator4.id)));
        assertThat(separator3.day, not(equalTo(separator4.day)));
    }

    private TdApi.Message createMessage() {
        TdApi.Message msg3 = new TdApi.Message();
        msg3.date = getTimeInSecs(cal);
        msg3.id = counter++;
        return msg3;
    }


    private int getTimeInSecs(Calendar cal) {
        return (int) (cal.getTimeInMillis()/1000);
    }
}
