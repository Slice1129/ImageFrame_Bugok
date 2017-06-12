package com.slice1129.git.imageframe;

import android.os.Handler;
import android.os.Message;
import android.os.Process;

import java.util.ArrayList;
import java.util.List;

/**
 * 后台单线程的handler
 * User: chengwangyong(chengwangyong@blinnnk.com)
 * Date: 2017/3/16
 * Time: 下午4:39
 */

/**
 * @author Slice1129 (lovezmcs@qq.com)
 * @Description: 原作者有BUG，我这边替他改了
 * 1.退出时崩溃
 * 2.多个动画同时出现，崩溃
 * @date 2017-05-24 15:21
 */
public class WorkHandler extends android.os.HandlerThread {
    private Handler workHandler = null;
    private WorkHandler workThread = null;
    private List<WorkMessageProxy> messageProxyList;

    private WorkHandler() {
        super("WorkHandler", Process.THREAD_PRIORITY_BACKGROUND);
    }

    public WorkHandler(String name) {
        super(name);
        workThread = new WorkHandler();
        workThread.start();
        workHandler = new Handler(workThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (messageProxyList != null) {
                    for (WorkMessageProxy workMessageProxy : messageProxyList) {
                        workMessageProxy.handleMessage(msg);
                    }
                }
            }
        };
    }

    public void post(Runnable run) {
        workHandler.post(run);
    }

    public void postAtFrontOfQueue(Runnable runnable) {
        workHandler.postAtFrontOfQueue(runnable);
    }

    public void postDelayed(Runnable runnable, long delay) {
        workHandler.postDelayed(runnable, delay);
    }

    public void postAtTime(Runnable runnable, long time) {
        workHandler.postAtTime(runnable, time);
    }

    public void addMessageProxy(WorkMessageProxy proxy) {
        initMessageProxyList();
        messageProxyList.add(proxy);
    }

    public void removeMessageProxy(WorkMessageProxy proxy) {
        initMessageProxyList();
        messageProxyList.remove(proxy);
    }

    private void initMessageProxyList() {
        if (messageProxyList == null) {
            messageProxyList = new ArrayList<>();
        }
    }

    /**
     * @Description: 必须调用，不然会OOM，看了作者的源码发现的BUG
     * @author Slice1129
     * @date 2017-05-24 15:20
     */
    public void onDestory() {
        if (messageProxyList != null) {
            messageProxyList.clear();
            messageProxyList = null;
        }
        workHandler = null;
        workThread = null;
    }

    public Handler getHanler() {
        return workHandler;
    }

    public interface WorkMessageProxy {
        void handleMessage(Message msg);
    }

}
