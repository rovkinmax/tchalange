package ru.korniltsev.telegram.common.recycler.sections;

public class Item <T>{
    public final int firstPosition;
    public final long id;
    public final T data;

    public Item(int firstPosition, long id, T data) {
        this.firstPosition = firstPosition;
        this.id = id;
        this.data = data;
    }
}
