package com.slice1129.git.imageframe;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.RawRes;
import android.text.TextUtils;

import java.io.File;
import java.lang.ref.SoftReference;

/**
 * User: chengwangyong(chengwangyong@blinnnk.com)
 * Date: 2017/5/1
 * Time: 上午11:09
 */
public class ImageFrameProxy implements WorkHandler.WorkMessageProxy {
    private static final int FILE = 0;
    private static final int RES = 1;
    private Resources resources;
    private int width;
    private int height;
    private boolean isLoading;
    private WorkHandler workHandler;

    public ImageFrameProxy() {
        workHandler = new WorkHandler("ADB");
    }

    private ImageCache imageCache;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (onImageLoadListener != null) {
                onImageLoadListener.onImageLoad(bitmapDrawable);
            }
            if (workHandler != null) {
                synchronized (this) {
                    if (workHandler != null) {
                        switch (msg.what) {
                            case RES:
                                load((int[]) msg.obj);
                                break;
                            case FILE:
                                load((File[]) msg.obj);
                                break;
                        }
                    }
                }
            }

        }
    };
    private volatile float frameTime;
    private volatile int index;
    private volatile BitmapDrawable bitmapDrawable;
    private OnImageLoadListener onImageLoadListener;
    private boolean loop;


    /**
     * load frame form file directory;
     *
     * @param width        request width
     * @param height       request height
     * @param fileDir      file directory
     * @param fps          The number of broadcast images per second
     * @param onPlayFinish finish callback
     */
    public void loadImage(final String fileDir, int width, int height, int fps,
                          OnImageLoadListener onPlayFinish) {
        if (!isLoading) {
            isLoading = true;
            this.width = width;
            this.height = height;
            loadImage(fileDir, fps, onPlayFinish);
        }

    }

    /**
     * load frame form file directory;
     *
     * @param fileDir      file directory
     * @param fps          The number of broadcast images per second
     * @param onPlayFinish finish callback
     */
    public void loadImage(final String fileDir, int fps, OnImageLoadListener onPlayFinish) {
        if (!isLoading && !TextUtils.isEmpty(fileDir) && fps > 0) {
            isLoading = true;
            File dir = new File(fileDir);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                loadImage(files, fps, onPlayFinish);
            }
        }
    }

    /**
     * load frame form file files Array;
     *
     * @param width        request width
     * @param height       request height
     * @param files        files Array
     * @param fps          The number of broadcast images per second
     * @param onPlayFinish finish callback
     */
    public void loadImage(final File[] files, int width, int height, int fps,
                          OnImageLoadListener onPlayFinish) {
        if (!isLoading) {
            isLoading = true;
            this.width = width;
            this.height = height;
            loadImage(files, fps, onPlayFinish);
        }

    }


    /**
     * load frame form file files Array;
     *
     * @param files        files Array
     * @param fps          The number of broadcast images per second
     * @param onPlayFinish finish callback
     */
    public void loadImage(final File[] files, int fps, OnImageLoadListener onPlayFinish) {
        if (!isLoading) {
            isLoading = true;
            if (imageCache == null) {
                imageCache = new ImageCache();
            }
            this.onImageLoadListener = onPlayFinish;
            index = 0;
            frameTime = 1000f / fps + 0.5f;
            workHandler.addMessageProxy(this);
            load(files);
        }

    }

    /**
     * load frame form file resources Array;
     *
     * @param resArray     resources Array
     * @param width        request width
     * @param height       request height
     * @param fps          The number of broadcast images per second
     * @param onPlayFinish finish callback
     */
    public void loadImage(Resources resources, @RawRes int[] resArray, int width, int height, int fps,
                          OnImageLoadListener onPlayFinish) {
        if (!isLoading) {
            isLoading = true;
            this.width = width;
            this.height = height;
            loadImage(resources, resArray, fps, onPlayFinish);
        }

    }

    /**
     * load frame form file resources Array;
     *
     * @param resArray     resources Array
     * @param fps          The number of broadcast images per second
     * @param onPlayFinish finish callback
     */
    public void loadImage(Resources resources, @RawRes int[] resArray, int fps,
                          OnImageLoadListener onPlayFinish) {
        if (!isLoading) {
            isLoading = true;
            this.resources = resources;
            if (imageCache == null) {
                imageCache = new ImageCache();
            }
            this.onImageLoadListener = onPlayFinish;
            index = 0;
            frameTime = 1000f / fps + 0.5f;
            workHandler.addMessageProxy(this);
            load(resArray);
        }

    }


    /**
     * loop play frame
     *
     * @param loop true is loop
     */
    public ImageFrameProxy setLoop(boolean loop) {
        if (!isLoading) {
            this.loop = loop;
        }
        return this;
    }


    /**
     * stop play frame
     */
    public void stop() {
        workHandler.onDestory();
        handler.removeCallbacksAndMessages(null);
        resources = null;
        isLoading = false;
    }

    private void load(final File[] files) {
        Message message = Message.obtain();
        message.obj = files;
        message.what = FILE;
        workHandler.getHanler().sendMessage(message);
    }

    private void load(@RawRes int[] res) {
        Message message = Message.obtain();
        message.obj = res;
        message.what = RES;
        workHandler.getHanler().sendMessage(message);
    }


    private void loadInThreadFromFile(final File[] files) {
        if (index < files.length) {
            File file = files[index];
            if (file.isFile() && isPicture(file)) {
                if (bitmapDrawable != null) {
                    imageCache.mReusableBitmaps.add(new SoftReference<>(bitmapDrawable.getBitmap()));
                }
                long start = System.currentTimeMillis();
                bitmapDrawable =
                        BitmapLoadUtils.decodeSampledBitmapFromFile(file.getAbsolutePath(), width, height,
                                imageCache);
                long end = System.currentTimeMillis();
                float updateTime = (frameTime - (end - start)) > 0 ? (frameTime - (end - start)) : 0;
                Message message = Message.obtain();
                message.what = FILE;
                message.obj = files;
                handler.sendMessageAtTime(message,
                        index == 0 ? 0 : (int) (SystemClock.uptimeMillis() + updateTime));
                index++;
            } else {
                index++;
                loadInThreadFromFile(files);
            }
        } else {
            if (loop) {
                index = 0;
                loadInThreadFromFile(files);
            } else {
                index++;
                bitmapDrawable = null;
                frameTime = 0;
                if (onImageLoadListener != null) {
                    onImageLoadListener.onPlayFinish();
                }
                isLoading = false;
                onImageLoadListener = null;
            }

        }
    }


    private void loadInThreadFromRes(final int[] resIds) {
        if (index < resIds.length) {
            int resId = resIds[index];
            if (bitmapDrawable != null) {
                imageCache.mReusableBitmaps.add(new SoftReference<>(bitmapDrawable.getBitmap()));
            }
            long start = System.currentTimeMillis();
            bitmapDrawable =
                    BitmapLoadUtils.decodeSampledBitmapFromRes(resources, resId, width,
                            height,
                            imageCache);
            long end = System.currentTimeMillis();
            float updateTime = (frameTime - (end - start)) > 0 ? (frameTime - (end - start)) : 0;
            Message message = Message.obtain();
            message.what = RES;
            message.obj = resIds;
            handler.sendMessageAtTime(message,
                    index == 0 ? 0 : (int) (SystemClock.uptimeMillis() + updateTime));
            index++;
        } else {
            if (loop) {
                index = 0;
                loadInThreadFromRes(resIds);
            } else {
                index++;
                bitmapDrawable = null;
                frameTime = 0;
                if (onImageLoadListener != null) {
                    onImageLoadListener.onPlayFinish();
                }
                isLoading = false;
                onImageLoadListener = null;
            }

        }
    }

    private boolean isPicture(File file) {
        return file.getName().endsWith("png") || file.getName().endsWith("jpg");
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case RES:
                loadInThreadFromRes((int[]) msg.obj);
                break;
            case FILE:
                loadInThreadFromFile((File[]) msg.obj);
                break;
        }

    }

    public interface OnImageLoadListener {
        void onImageLoad(BitmapDrawable drawable);

        void onPlayFinish();
    }
}
