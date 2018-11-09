package com.hfxief.utils.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.ImageView;

import com.hfxief.utils.ConstUtils;
import com.hfxief.utils.file.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;

/**
 * @Title: ImageUtil
 * @date 2016/7/19 17:37
 * @auther xie
 * @Description: 描述
 */
public class ImageUtil {
    private static ArrayMap<String, Pair<Integer, Integer>> imageSizeMap = new ArrayMap<>();

    private ImageUtil() {

    }

    private static String generateFilePath(Context context, String parentPath, Uri uri, String extension) {
        File file = new File(parentPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath() + File.separator + FileUtils.splitFileName(FileUtils.getFileName(context, uri))[0] + "." + extension;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;

        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    public static Bitmap getScaledBitmap(Context context, Uri imageUri, float maxWidth, float maxHeight) {
        String filePath = FileUtils.getRealPathFromURI(context, imageUri);
        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

        //by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
        //you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

        float imgRatio = (float) actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

        //width and height values are set maintaining the aspect ratio of the image
        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;
            }
        }

        //setting inSampleSize value allows to load a scaled down version of the original image
        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

        //inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

        //this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];

        try {
            //load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        //check the rotation of the image and display it properly
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
            } else if (orientation == 3) {
                matrix.postRotate(180);
            } else if (orientation == 8) {
                matrix.postRotate(270);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(),
                    matrix, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return scaledBitmap;
    }

    public static File compressImage(Context context, Uri imageUri, float maxWidth, float maxHeight, Bitmap.CompressFormat compressFormat, int quality, String parentPath) {
        FileOutputStream out = null;
        String filename = generateFilePath(context, parentPath, imageUri, compressFormat.name().toLowerCase());
        try {
            out = new FileOutputStream(filename);

            //write the compressed bitmap at the destination specified by filename.
            ImageUtil.getScaledBitmap(context, imageUri, maxWidth, maxHeight).compress(compressFormat, quality, out);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignored) {
            }
        }

        return new File(filename);
    }

    /**
     * <pre>
     * parse image ratio from imageurl with regex as follows:
     * (\d+)-(\d+)(_?q\d+)?(\.[jpg|png|gif])
     * (\d+)x(\d+)(_?q\d+)?(\.[jpg|png|gif])
     * samples urls:
     * http://img.alicdn.com/tps/i1/TB1x623LVXXXXXZXFXXzo_ZPXXX-372-441.png --> return 372/441
     * http://img.alicdn.com/tps/i1/TB1P9AdLVXXXXa_XXXXzo_ZPXXX-372-441.png --> return 372/441
     * http://img.alicdn.com/tps/i1/TB1NZxRLFXXXXbwXFXXzo_ZPXXX-372-441.png --> return 372/441
     * http://img07.taobaocdn.com/tfscom/T10DjXXn4oXXbSV1s__105829.jpg_100x100.jpg --> return 100/100
     * http://img07.taobaocdn.com/tfscom/T10DjXXn4oXXbSV1s__105829.jpg_100x100q90.jpg --> return 100/100
     * http://img07.taobaocdn.com/tfscom/T10DjXXn4oXXbSV1s__105829.jpg_100x100q90.jpg_.webp --> return 100/100
     * http://img03.taobaocdn.com/tps/i3/T1JYROXuRhXXajR_DD-1680-446.jpg_q50.jpg --> return 1680/446
     * </pre>
     *
     * @param imageUrl image url
     * @return ratio of with to height parsed from url
     */
    public static Pair<Integer, Integer> getImageSize(String imageUrl) {
        if (TextUtils.isEmpty(imageUrl)) {
            return null;
        }
        if (imageSizeMap.get(imageUrl) != null) {
            return imageSizeMap.get(imageUrl);
        }
        try {
            Matcher matcher = ConstUtils.REGEX_IMG_1.matcher(imageUrl);
            String widthStr;
            String heightStr;
            if (matcher.find()) {
                if (matcher.groupCount() >= 2) {
                    widthStr = matcher.group(1);
                    heightStr = matcher.group(2);
                    if (widthStr.length() < 5 && heightStr.length() < 5) {
                        int urlWidth = Integer.parseInt(widthStr);
                        int urlHeight = Integer.parseInt(heightStr);
                        Pair<Integer, Integer> result = new Pair<>(urlWidth, urlHeight);
                        imageSizeMap.put(imageUrl, result);
                        return result;
                    }
                }
            } else {
                matcher = ConstUtils.REGEX_IMG_2.matcher(imageUrl);
                if (matcher.find()) {
                    if (matcher.groupCount() >= 2) {
                        widthStr = matcher.group(1);
                        heightStr = matcher.group(2);
                        if (widthStr.length() < 5 && heightStr.length() < 5) {
                            int urlWidth = Integer.parseInt(widthStr);
                            int urlHeight = Integer.parseInt(heightStr);
                            Pair<Integer, Integer> result = new Pair<>(urlWidth, urlHeight);
                            imageSizeMap.put(imageUrl, result);
                            return result;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * create a custom ImageView instance
     *
     * @param context activity context
     * @return an instance
     */
    public static ImageView createImageInstance(Context context, Constructor imageViewConstructor) {
        if (imageViewConstructor != null) {
            try {
                return (ImageView) imageViewConstructor.newInstance(context);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
