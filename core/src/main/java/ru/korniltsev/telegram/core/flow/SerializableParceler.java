package ru.korniltsev.telegram.core.flow;

import android.os.Parcel;
import android.os.Parcelable;
import flow.StateParceler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SerializableParceler implements StateParceler {
    @Override
    public Parcelable wrap(Object instance) {
//        Assert.assertTrue(instance instanceof Serializable);
        return new Wrapper((Serializable) instance);
    }

    @Override
    public Object unwrap(Parcelable parcelable) {
        Wrapper w = (Wrapper) parcelable;
        return w.s;
    }

    public static class Wrapper implements Parcelable {
        final Serializable s;

        public Wrapper(Serializable s) {
            this.s = s;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                new ObjectOutputStream(output).writeObject(s);
                byte[] bs = output.toByteArray();
                dest.writeInt(bs.length);
                dest.writeByteArray(bs);
            } catch (IOException e) {
                throw new RuntimeException("ftw", e);
            }
        }
        public static final Parcelable.Creator<Wrapper> CREATOR = new Parcelable.Creator<Wrapper>() {
            @Override public Wrapper createFromParcel(Parcel in) {
                int length = in.readInt();
                byte[] bs = new byte[length];
                in.readByteArray(bs);

                try {
                    ObjectInputStream object = new ObjectInputStream(new ByteArrayInputStream(bs));
                    Object o = object.readObject();
                    return new Wrapper((Serializable) o);
                } catch (IOException e) {
                    throw new RuntimeException("ftw", e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("ftw", e);
                }
            }

            @Override public Wrapper[] newArray(int size) {
                return new Wrapper[size];
            }
        };
    }


}
