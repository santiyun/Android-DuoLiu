package com.tttrtclive.live.callback;

import android.content.Context;
import android.content.Intent;

import com.tttrtclive.live.LocalConstans;
import com.tttrtclive.live.bean.JniObjs;
import com.tttrtclive.live.ui.MainActivity;
import com.tttrtclive.live.utils.MyLog;
import com.wushuangtech.expansion.bean.LocalAudioStats;
import com.wushuangtech.expansion.bean.LocalVideoStats;
import com.wushuangtech.expansion.bean.RemoteAudioStats;
import com.wushuangtech.expansion.bean.RemoteVideoStats;
import com.wushuangtech.wstechapi.TTTRtcEngineEventHandler;

import java.util.ArrayList;
import java.util.List;

import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_AUDIO_ROUTE;
import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_AUDIO_VOLUME_INDICATION;
import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_CONNECTLOST;
import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_ENTER_ROOM;
import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_ERROR;
import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_LOCAL_AUDIO_STATE;
import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_LOCAL_VIDEO_STATE;
import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_MUTE_AUDIO;
import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_REMOTE_AUDIO_STATE;
import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_REMOTE_VIDEO_STATE;
import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_REMOVE_FIRST_FRAME_COME;
import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_SEI;
import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_SPEAK_MUTE_AUDIO;
import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_USER_JOIN;
import static com.tttrtclive.live.LocalConstans.CALL_BACK_ON_USER_OFFLINE;


/**
 * Created by wangzhiguo on 17/10/24.
 */

public class MyTTTRtcEngineEventHandler extends TTTRtcEngineEventHandler {

    public static final String TAG = "MyTTTRtcEngineEventHandler_MULTI_LIVE";
    public static final String MSG_TAG = "MyTTTRtcEngineEventHandlerMSG_MULTI_LIVE";
    private boolean mIsSaveCallBack;
    private List<JniObjs> mSaveCallBack;
    private Context mContext;

    public MyTTTRtcEngineEventHandler(Context mContext) {
        this.mContext = mContext;
        mSaveCallBack = new ArrayList<>();
    }

    @Override
    public void onJoinChannelSuccess(String channel, long uid, int elapsed) {
        MyLog.i("wzg", "onJoinChannelSuccess.... channel ： " + channel + " | uid : " + uid);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_ENTER_ROOM;
        mJniObjs.mChannelName = channel;
        mJniObjs.mUid = uid;
        sendMessage(mJniObjs);
        mIsSaveCallBack = true;
    }

    @Override
    public void onError(final int errorType) {
        MyLog.i("wzg", "onError.... errorType ： " + errorType + "mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_ERROR;
        mJniObjs.mErrorType = errorType;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }


    @Override
    public void onUserKicked(long uid, int reason) {
        MyLog.i("wzg", "onUserKicked.... uid ： " + uid + "reason : " + reason + "mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = LocalConstans.CALL_BACK_ON_USER_KICK;
        mJniObjs.mErrorType = reason;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }


    @Override
    public void onUserJoined(long nUserId, int identity, int elapsed) {
        MyLog.i("wzg", "onUserJoined.... nUserId ： " + nUserId + " | identity : " + identity
                + " | mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_USER_JOIN;
        mJniObjs.mUid = nUserId;
        mJniObjs.mIdentity = identity;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onUserOffline(long nUserId, int reason) {
        MyLog.i("wzg", "onUserOffline.... nUserId ： " + nUserId + " | reason : " + reason);

        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_USER_OFFLINE;
        mJniObjs.mUid = nUserId;
        mJniObjs.mReason = reason;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onReconnectServerFailed() {
        MyLog.i("wzg", "onReconnectServerFailed.... ");
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_CONNECTLOST;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onUserEnableVideo(long uid, String deviceID, boolean enabled) {
        MyLog.i("wzg", "onUserEnableVideo.... uid : " + uid + " | deviceID : " + deviceID + " | enabled : " + enabled);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = LocalConstans.CALL_BACK_ON_USER_MUTE_VIDEO;
        mJniObjs.mUid = uid;
        mJniObjs.mDeviceID = deviceID;
        mJniObjs.mIsEnableVideo = enabled;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onAudioVolumeIndication(long nUserID, int audioLevel, int audioLevelFullRange) {
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_AUDIO_VOLUME_INDICATION;
        mJniObjs.mUid = nUserID;
        mJniObjs.mAudioLevel = audioLevel;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onFirstRemoteVideoFrame(long uid, int width, int height, int elapsed) {
        MyLog.i("wzg", "onFirstRemoteVideoFrame.... uid ： " + uid + " | width : " + width + " | height : " + height);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_REMOVE_FIRST_FRAME_COME;
        mJniObjs.mUid = uid;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onRemoteVideoStats(RemoteVideoStats stats) {
//        MyLog.i("wzg", "onRemoteVideoStats.... uid : " + stats.getUid() + " | bitrate : " + stats.getReceivedBitrate()
//                + " | framerate : " + stats.getReceivedFrameRate());
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_REMOTE_VIDEO_STATE;
        mJniObjs.mRemoteVideoStats = stats;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onRemoteAudioStats(RemoteAudioStats stats) {
//        MyLog.i("wzg", "RemoteAudioStats.... uid : " + stats.getUid() + " | bitrate : " + stats.getReceivedBitrate());
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_REMOTE_AUDIO_STATE;
        mJniObjs.mRemoteAudioStats = stats;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onLocalVideoStats(LocalVideoStats stats) {
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_LOCAL_VIDEO_STATE;
        mJniObjs.mLocalVideoStats = stats;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onLocalAudioStats(LocalAudioStats stats) {
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_LOCAL_AUDIO_STATE;
        mJniObjs.mLocalAudioStats = stats;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onSetSEI(String sei) {
        MyLog.i("wzg", "onSei.... sei : " + sei);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_SEI;
        mJniObjs.mSEI = sei;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onUserMuteAudio(long uid, boolean muted) {
        MyLog.i("wzg", "OnRemoteAudioMuted.... uid : " + uid + " | muted : " + muted + " | mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_MUTE_AUDIO;
        mJniObjs.mUid = uid;
        mJniObjs.mIsDisableAudio = muted;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onSpeakingMuted(long uid, boolean muted) {
        MyLog.i("wzg", "onSpeakingMuted.... uid : " + uid + " | muted : " + muted + " | mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_SPEAK_MUTE_AUDIO;
        mJniObjs.mUid = uid;
        mJniObjs.mIsDisableAudio = muted;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onAudioRouteChanged(int routing) {
        MyLog.i("wzg", "onAudioRouteChanged.... routing : " + routing);
        MainActivity.mCurrentAudioRoute = routing;
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_AUDIO_ROUTE;
        mJniObjs.mAudioRoute = routing;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onClientRoleChanged(long uid, int userRole) {
        super.onClientRoleChanged(uid, userRole);
        MyLog.i("wzg", "onUserRoleChanged... userID : " + uid + " userRole : " + userRole);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = LocalConstans.CALL_BACK_ON_USER_ROLE_CHANGED;
        mJniObjs.mUid = uid;
        mJniObjs.mIdentity = userRole;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    private void sendMessage(JniObjs mJniObjs) {
        Intent i = new Intent();
        i.setAction(TAG);
        i.putExtra(MSG_TAG, mJniObjs);
        i.setExtrasClassLoader(JniObjs.class.getClassLoader());
        mContext.sendBroadcast(i);
    }

    public void setIsSaveCallBack(boolean mIsSaveCallBack) {
        this.mIsSaveCallBack = mIsSaveCallBack;
        if (!mIsSaveCallBack) {
            for (int i = 0; i < mSaveCallBack.size(); i++) {
                sendMessage(mSaveCallBack.get(i));
            }
            mSaveCallBack.clear();
        }
    }

    private void saveCallBack(JniObjs mJniObjs) {
        if (mIsSaveCallBack) {
            mSaveCallBack.add(mJniObjs);
        }
    }
}
