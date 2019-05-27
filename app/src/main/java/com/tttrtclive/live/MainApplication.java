package com.tttrtclive.live;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Environment;

import com.tttrtclive.live.callback.MyTTTRtcEngineEventHandler;
import com.tttrtclive.live.utils.CrashHandler;
import com.wushuangtech.utils.PviewLog;
import com.wushuangtech.wstechapi.TTTRtcEngine;

import java.io.File;
import java.util.Random;

public class MainApplication extends Application {

    public MyTTTRtcEngineEventHandler mMyTTTRtcEngineEventHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        Random mRandom = new Random();
        LocalConfig.mLocalUserID = mRandom.nextInt(999999);

        //1.设置SDK的回调接收类
        mMyTTTRtcEngineEventHandler = new MyTTTRtcEngineEventHandler(getApplicationContext());
        //2.创建SDK的实例对象
        TTTRtcEngine mTTTEngine = TTTRtcEngine.create(getApplicationContext(), <APPID引用位置>,
                false, mMyTTTRtcEngineEventHandler);
        if (mTTTEngine == null) {
            System.exit(0);
        }

        if (!isApkDebugable()) {
            //开启日志
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File externalStorageDirectory = Environment.getExternalStorageDirectory();
                String abs = externalStorageDirectory.toString() + "/3T_Live_Log";
                mTTTEngine.setLogFile(abs);
            } else {
                PviewLog.i("Collection log failed! , No permission!");
            }
        }
        CrashHandler mCrashHandler = new CrashHandler(getApplicationContext());
        mCrashHandler.init();
    }

    public boolean isApkDebugable() {
        try {
            ApplicationInfo info = this.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception ignored) {
        }
        return false;
    }
}
