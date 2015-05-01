package ru.korniltsev.telegram.core.flow;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * Created by korniltsev on 23/04/15.
 */
public class FlowLikeActivity extends FragmentActivity{
    protected FlowLike flow;

    public void setFlow(FlowLike flow) {
        this.flow = flow;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }
}
