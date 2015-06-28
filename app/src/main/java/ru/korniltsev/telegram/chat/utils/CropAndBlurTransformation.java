//package ru.korniltsev.telegram.chat.utils;
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.support.v8.renderscript.Allocation;
//import android.support.v8.renderscript.Element;
//import android.support.v8.renderscript.RenderScript;
//import android.support.v8.renderscript.ScriptIntrinsicBlur;
//import com.squareup.picasso.Transformation;
//
//public class CropAndBlurTransformation implements Transformation{
//    final int width;
//    final int height;
//    final Context appContext;
//
//    public CropAndBlurTransformation(Context appContext, int width, int height) {
//        this.appContext = appContext;
//        this.width = width;
//        this.height = height;
//    }
//
//    @Override
//    public Bitmap transform(Bitmap source) {
//        return source;
////        Bitmap blured = blurBitmap(source);
////        float sourceRatio = (float)source.getWidth() / source.getHeight();
////        float targetRatio = (float) width / height;
////        int newBitmapHeight;
////        int newBitmapWidth;
////        if (sourceRatio < targetRatio) {
////            newBitmapWidth = width;
////            newBitmapHeight = (int) (newBitmapWidth / sourceRatio);
////
////        } else {
////            newBitmapHeight = height;
////            newBitmapWidth = (int) (sourceRatio * newBitmapHeight);
////        }
////        Bitmap scaled = Bitmap.createScaledBitmap(blured, newBitmapWidth, newBitmapHeight, true);
////        blured.recycle();
////        return scaled;//blurBitmap(scaled);
//    }
//
//    public Bitmap blurBitmap(Bitmap bitmap){
//
//
//        //Let's create an empty bitmap with the same size of the bitmap we want to blur
//        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
//
//
//        //Instantiate a new Renderscript
//        RenderScript rs = RenderScript.create(appContext);
//
//        //Create an Intrinsic Blur Script using the Renderscript
//        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
//
//
//        //Create the in/out Allocations with the Renderscript and the in/out bitmaps
//        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
//        Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);
//
//
//        //Set the radius of the blur
//        blurScript.setRadius(25.f);
//
//
//        //Perform the Renderscript
//        blurScript.setInput(allIn);
//        blurScript.forEach(allOut);
//
//
//        //Copy the final bitmap created by the out Allocation to the outBitmap
//        allOut.copyTo(outBitmap);
//
//
//        //recycle the original bitmap
//        bitmap.recycle();
//
//
//        //After finishing everything, we destroy the Renderscript.
//        rs.destroy();
//
//
//        return outBitmap;
//
//    }
//
//
//    @Override
//    public String key() {
//        return "CropAndBlurTransformation";
//    }
//}
