package com.hfxief.utils.image;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * HFSampleProject
 * com.hfxief.utils.image
 *
 * @Author: xie
 * @Time: 2017/1/4 11:35
 * @Description:
 */


public class ImageZoom {
    public static List<String> zoomBitMapFromPath(Context context, List<String> pathList) {
        List<String> newPath = new ArrayList<>();
        for (int i = 0; i < pathList.size(); i++) {
            File f = new Compressor.Builder(context)
                    .setMaxWidth(480)
                    .setMaxHeight(800)
                    .setQuality(80)
                    .setCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setDestinationDirectoryPath(context.getCacheDir().getAbsolutePath())
                    .build()
                    .compressToFile(new File(pathList.get(i)));
            newPath.add(f.getAbsolutePath());
        }
        return newPath;
    }

    public static List<File> zoomBitMapFromPathAsFile(Context context, List<String> pathList) {
        List<File> newPath = new ArrayList<>();
        for (int i = 0; i < pathList.size(); i++) {
            File f = new Compressor.Builder(context)
                    .setMaxWidth(800)
                    .setMaxHeight(800)
                    .setQuality(100)
                    .setCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setDestinationDirectoryPath(context.getCacheDir().getAbsolutePath())
                    .build()
                    .compressToFile(new File(pathList.get(i)));
            newPath.add(f);
        }
        return newPath;
    }
}
