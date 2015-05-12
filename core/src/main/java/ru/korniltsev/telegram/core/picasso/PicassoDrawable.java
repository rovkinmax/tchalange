/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.korniltsev.telegram.core.picasso;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.widget.ImageView;
import com.squareup.picasso.Picasso;
import junit.framework.Assert;

import static com.squareup.picasso.Picasso.LoadedFrom.MEMORY;
import static junit.framework.Assert.assertTrue;

public final class PicassoDrawable extends Drawable {
  // Only accessed from main thread.
  private static final Paint DEBUG_PAINT = new Paint();
  private static final float FADE_DURATION = 200f; //ms


  private final Bitmap bitmap;
  private final float density;
  private final Picasso.LoadedFrom loadedFrom;

//  Drawable placeholder;

  long startTimeMillis;
  boolean animating;
  int alpha = 0xFF;

  final Paint mPaint ;

  public PicassoDrawable(Context context, Bitmap bitmap,
                         Picasso.LoadedFrom loadedFrom, boolean noFade, Paint mPaint) {
    this.bitmap = bitmap;
    this.mPaint = mPaint;

    //    mPaint = new Bitmap
    //    super(context.getResources(), bitmap);

    this.density = context.getResources().getDisplayMetrics().density;

    this.loadedFrom = loadedFrom;

    boolean fade = loadedFrom != MEMORY && !noFade;
    if (fade) {
      animating = true;
      startTimeMillis = SystemClock.uptimeMillis();
    }
  }

  @Override public void draw(Canvas canvas) {
    if (!animating) {
      drawBitmap(canvas);
    } else {
      float normalized = (SystemClock.uptimeMillis() - startTimeMillis) / FADE_DURATION;
      if (normalized >= 1f) {
        animating = false;
        drawBitmap(canvas);
      } else {
        int partialAlpha = (int) (alpha * normalized);
        mPaint.setAlpha(partialAlpha);
        drawBitmap(canvas);
        mPaint.setAlpha(alpha);

        invalidateSelf();
      }
    }

  }



  private void drawBitmap(Canvas canvas) {
    canvas.drawBitmap(bitmap, 0, 0, mPaint);
  }

  @Override public void setAlpha(int alpha) {

  }

  @Override public void setColorFilter(ColorFilter cf) {

  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  @Override protected void onBoundsChange(Rect bounds) {
    assertTrue(bounds.width() == bitmap.getWidth());
    assertTrue(bounds.height() == bitmap.getHeight());
    super.onBoundsChange(bounds);
  }


}
