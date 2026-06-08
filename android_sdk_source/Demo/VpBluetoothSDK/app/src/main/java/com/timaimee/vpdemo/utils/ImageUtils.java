package com.timaimee.vpdemo.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {
    private static final String TAG = "ImageUtils";

    /**
     * 
     *
     * @param inputPath   
     * @param outputPath  
     * @param targetWidth 
     * @param targetHeight 
     * @return  true， false
     */
    public static boolean centerCropAndSave(String inputPath, String outputPath, int targetWidth, int targetHeight) {
        Bitmap sourceBitmap = BitmapFactory.decodeFile(inputPath);
        if (sourceBitmap == null) {
            Log.e(TAG, ": " + inputPath);
            return false;
        }

        try {
            Bitmap croppedBitmap = centerCrop(sourceBitmap, targetWidth, targetHeight);
            if (croppedBitmap == null) {
                return false;
            }

            // 
            return saveBitmap(croppedBitmap, outputPath);
        } finally {
            // Bitmap，
            if (sourceBitmap != null && !sourceBitmap.isRecycled()) {
                sourceBitmap.recycle();
            }
        }
    }

    /**
     * Bitmap
     *
     * @param sourceBitmap Bitmap
     * @param targetWidth 
     * @param targetHeight 
     * @return Bitmap
     */
    private static Bitmap centerCrop(Bitmap sourceBitmap, int targetWidth, int targetHeight) {
        int sourceWidth = sourceBitmap.getWidth();
        int sourceHeight = sourceBitmap.getHeight();

        float targetRatio = (float) targetWidth / targetHeight;
        float sourceRatio = (float) sourceWidth / sourceHeight;

        int cropX = 0;
        int cropY = 0;
        int cropWidth = sourceWidth;
        int cropHeight = sourceHeight;

        if (sourceRatio > targetRatio) {
            // （）
            // ，
            cropWidth = (int) (sourceHeight * targetRatio);
            cropX = (sourceWidth - cropWidth) / 2;
        } else if (sourceRatio < targetRatio) {
            // （）
            // ，
            cropHeight = (int) (sourceWidth / targetRatio);
            cropY = (sourceHeight - cropHeight) / 2;
        }

        // 1.  (Creates a sub-bitmap)
        Bitmap cropped = Bitmap.createBitmap(sourceBitmap, cropX, cropY, cropWidth, cropHeight);

        // 2.  (Resize to final target dimensions)
        // ，
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true);

        // Bitmap（Bitmap）
        if (cropped != sourceBitmap && !cropped.isRecycled()) {
            cropped.recycle();
        }

        return scaledBitmap;
    }

    /**
     *  Bitmap 
     */
    public static boolean saveBitmap(Bitmap bitmap, String outputPath) {
        FileOutputStream out = null;
        try {
            File outputFile = new File(outputPath);
            // 
            if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }

            out = new FileOutputStream(outputFile);
            //  JPEG ， 90
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            Log.d(TAG, ": " + outputPath);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "", e);
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // Bitmap
//            if (bitmap != null && !bitmap.isRecycled()) {
//                bitmap.recycle();
//            }
        }
    }

    /**
     * 
     *
     * @param bitmap
     * @param roundPx 14
     * @return
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        //  Bitmap  Bitmap
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        //  Bitmap
        Canvas canvas = new Canvas(output);
        // Configurar
        Paint paint = new Paint();
        paint.setAntiAlias(true);  // Ativar
        // 
        Path path = new Path();
        path.addCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                Math.min(bitmap.getWidth(), bitmap.getHeight()) / 2, Path.Direction.CCW);
        // 
        canvas.clipPath(path);
        //  Bitmap
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return output;
    }
}
