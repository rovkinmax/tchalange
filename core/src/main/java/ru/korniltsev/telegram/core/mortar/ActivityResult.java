package ru.korniltsev.telegram.core.mortar;

import android.content.Intent;

public class ActivityResult {
    public final int request;
    public final int result;
    public final Intent data;

    public ActivityResult(int request, int result, Intent data) {
        this.request = request;
        this.result = result;
        this.data = data;
    }
}