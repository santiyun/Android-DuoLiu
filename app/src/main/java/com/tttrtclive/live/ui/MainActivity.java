package com.tttrtclive.live.ui;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tttrtclive.bean.EnterUserInfo;
import com.tttrtclive.live.Helper.WEChatShare;
import com.tttrtclive.live.Helper.WindowManager;
import com.tttrtclive.live.LocalConfig;
import com.tttrtclive.live.LocalConstans;
import com.tttrtclive.live.MainApplication;
import com.tttrtclive.live.R;
import com.tttrtclive.live.bean.JniObjs;
import com.tttrtclive.live.callback.MyTTTRtcEngineEventHandler;
import com.tttrtclive.live.callback.PhoneListener;
import com.tttrtclive.live.dialog.ExitRoomDialog;
import com.tttrtclive.live.utils.MyLog;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.wushuangtech.wstechapi.model.VideoCanvas;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.wushuangtech.library.Constants.CLIENT_ROLE_ANCHOR;

public class MainActivity extends BaseActivity {

    private long mUserId;
    private long mAnchorId = -1;

    private TextView mAudioSpeedShow;
    private TextView mVideoSpeedShow;
    private ImageView mAudioChannel;

    private ExitRoomDialog mExitRoomDialog;
    private AlertDialog.Builder mErrorExitDialog;
    private MyLocalBroadcastReceiver mLocalBroadcast;
    private boolean mIsMute = false;
    private boolean mIsHeadset;
    private boolean mIsPhoneComing;
    private boolean mIsSpeaker, mIsBackCamera;

    private WindowManager mWindowManager;
    private TelephonyManager mTelephonyManager;
    private PhoneListener mPhoneListener;
    private int mRole = CLIENT_ROLE_ANCHOR;
    private boolean mHasLocalView = false;
    private WEChatShare mWEChatShare;
    private long mRoomID;
    private final Object obj = new Object();
    private boolean mIsReceiveSei;
    private Map<Long, Boolean> mUserMutes = new HashMap<>();
    private List<EnterUserInfo> mUsers = new ArrayList<>();

    public static int mCurrentAudioRoute;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initEngine();
        initDialog();
        mTelephonyManager = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        mPhoneListener = new PhoneListener(this);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        // 启用 sdk 上报所有说话者的音量大小
        mTTTEngine.enableAudioVolumeIndication(300, 3);
        // 设置 SDK 的本地视频等级或参数
        if (mRole == Constants.CLIENT_ROLE_BROADCASTER) {
            // 若角色为副播，视频质量等级设置为120P
            mTTTEngine.setVideoProfile(Constants.TTTRTC_VIDEOPROFILE_120P, false);
        } else {
            // 若角色为主播，视频质量根据登录界面的设置参数决定
            if (LocalConfig.mLocalVideoProfile != 0) {
                TTTRtcEngine.getInstance().setVideoProfile(LocalConfig.mLocalVideoProfile, false);
            } else {
                if (LocalConfig.mLocalHeight != 0 && LocalConfig.mLocalWidth != 0 &&
                        LocalConfig.mLocalBitRate != 0 && LocalConfig.mLocalFrameRate != 0) {
                    TTTRtcEngine.getInstance().setVideoProfile(LocalConfig.mLocalHeight, LocalConfig.mLocalWidth,
                            LocalConfig.mLocalBitRate, LocalConfig.mLocalFrameRate);
                } else {
                    mTTTEngine.setVideoProfile(Constants.TTTRTC_VIDEOPROFILE_360P, false);
                }
            }
        }
        MyLog.d("MainActivity onCreate ...");
    }

    @Override
    public void onBackPressed() {
        mExitRoomDialog.show();
    }

    @Override
    protected void onDestroy() {
        if (mPhoneListener != null && mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
            mPhoneListener = null;
            mTelephonyManager = null;
        }
        unregisterReceiver(mLocalBroadcast);

        mTTTEngine.muteLocalAudioStream(false);
        if (mIsBackCamera) {
            mTTTEngine.switchCamera();
        }
        LocalConfig.mIsMacAnchor = false;
        LocalConfig.mIsPCAnchor = false;
        super.onDestroy();
        MyLog.d("MainActivity onDestroy... ");
    }

    private void initView() {
        mAudioSpeedShow = findViewById(R.id.main_btn_audioup);
        mVideoSpeedShow = findViewById(R.id.main_btn_videoup);
        mAudioChannel = findViewById(R.id.main_btn_audio_channel);

        Intent intent = getIntent();
        mRoomID = intent.getLongExtra("ROOM_ID", 0);
        mUserId = intent.getLongExtra("USER_ID", 0);
        mRole = intent.getIntExtra("ROLE", CLIENT_ROLE_ANCHOR);
        String localChannelName = getString(R.string.ttt_prefix_channel_name) + ":" + mRoomID;
        ((TextView) findViewById(R.id.main_btn_title)).setText(localChannelName);

        if (mRole == CLIENT_ROLE_ANCHOR) {
            // 打开本地预览视频，并开始推流
            String localUserName = getString(R.string.ttt_prefix_user_name) + ":" + mUserId;
            mAnchorId = mUserId;
            ((TextView) findViewById(R.id.main_btn_host)).setText(localUserName);
            SurfaceView mSurfaceView = mTTTEngine.CreateRendererView(this);
            mTTTEngine.setupLocalVideo(new VideoCanvas(0, Constants.RENDER_MODE_HIDDEN, mSurfaceView), getRequestedOrientation());
            ((ConstraintLayout) findViewById(R.id.local_view_layout)).addView(mSurfaceView);
        }

        findViewById(R.id.main_btn_exit).setOnClickListener((v) -> mExitRoomDialog.show());

        mAudioChannel.setOnClickListener(v -> {
            if (mRole != CLIENT_ROLE_ANCHOR) return;
            mIsMute = !mIsMute;
            if (mIsHeadset)
                mAudioChannel.setImageResource(mIsMute ? R.drawable.mainly_btn_muted_headset_selector : R.drawable.mainly_btn_headset_selector);
            else
                mAudioChannel.setImageResource(mIsMute ? R.drawable.mainly_btn_mute_speaker_selector : R.drawable.mainly_btn_speaker_selector);
            mTTTEngine.muteLocalAudioStream(mIsMute);
        });
        if (mRole != CLIENT_ROLE_ANCHOR)
            findViewById(R.id.main_btn_switch_camera).setVisibility(View.GONE);

        findViewById(R.id.main_btn_switch_camera).setOnClickListener(v -> {
            mTTTEngine.switchCamera();
            mIsBackCamera = !mIsBackCamera;
        });

        findViewById(R.id.main_button_share).setOnClickListener(v -> {
            findViewById(R.id.main_share_layout).setVisibility(View.VISIBLE);
            throw new RuntimeException("sss");
        });

        mWEChatShare = new WEChatShare(this);
        findViewById(R.id.main_share_layout).findViewById(R.id.friend).setOnClickListener(v -> {
            if (LocalConfig.VERSION_FLAG == LocalConstans.VERSION_WHITE) {
                mWEChatShare.sendText(SendMessageToWX.Req.WXSceneSession, mRoomID,
                        "http://wushuangtech.com/live.html?flv=http://pull.wushuangtech.com/sdk/" + mRoomID + ".flv&hls=http://pull.wushuangtech.com/sdk/" + mRoomID + ".m3u8");
            } else {
                mWEChatShare.sendText(SendMessageToWX.Req.WXSceneSession, mRoomID, getWXLink());
            }
            findViewById(R.id.main_share_layout).setVisibility(View.GONE);
        });

        findViewById(R.id.friend_circle).setOnClickListener(v -> {
            if (LocalConfig.VERSION_FLAG == LocalConstans.VERSION_WHITE) {
                mWEChatShare.sendText(SendMessageToWX.Req.WXSceneTimeline, mRoomID,
                        "http://wushuangtech.com/live.html?flv=http://pull.wushuangtech.com/sdk/" + mRoomID + ".flv&hls=http://pull.wushuangtech.com/sdk/" + mRoomID + ".m3u8");
            } else {
                mWEChatShare.sendText(SendMessageToWX.Req.WXSceneSession, mRoomID, getWXLink());
            }

            findViewById(R.id.main_share_layout).setVisibility(View.GONE);
        });

        findViewById(R.id.shared_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                // 将文本内容放到系统剪贴板里。
                cm.setText(getWXLink());
                Toast.makeText(mContext, getString(R.string.ttt_copy_success), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.local_view_layout).setOnClickListener(v -> {
            if (findViewById(R.id.main_share_layout).getVisibility() == View.VISIBLE)
                findViewById(R.id.main_share_layout).setVisibility(View.GONE);
        });

        findViewById(R.id.friend_circle_close).setOnClickListener(v -> {
            findViewById(R.id.main_share_layout).setVisibility(View.GONE);
        });

    }

    public void setTextViewContent(TextView textView, int resourceID, String value) {
        String string = getResources().getString(resourceID);
        String result = String.format(string, value);
        textView.setText(result);
    }

    private void initEngine() {
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addCategory("ttt.test.interface");
        filter.addAction("ttt.test.interface.string");
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);
        ((MainApplication) getApplicationContext()).mMyTTTRtcEngineEventHandler.setIsSaveCallBack(false);
    }

    private void initDialog() {
        mExitRoomDialog = new ExitRoomDialog(mContext, R.style.NoBackGroundDialog);
        mExitRoomDialog.setCanceledOnTouchOutside(false);
        mExitRoomDialog.mConfirmBT.setOnClickListener(v -> {
            exitRoom();
            mExitRoomDialog.dismiss();
        });
        mExitRoomDialog.mDenyBT.setOnClickListener(v -> mExitRoomDialog.dismiss());


        //添加确定按钮
        mErrorExitDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.ttt_error_exit_dialog_title))//设置对话框标题
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ttt_confirm), (dialog, which) -> {//确定按钮的响应事件
                    exitRoom();
                });
    }

    private void initData() {
        mWindowManager = new WindowManager(this);
        if (mCurrentAudioRoute != Constants.AUDIO_ROUTE_SPEAKER) {
            mIsHeadset = true;
            mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
        }
    }

    public void exitRoom() {
        MyLog.d("exitRoom was called!... leave room");
        mTTTEngine.leaveChannel();
        startActivity(new Intent(mContext, SplashActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    public String getWXLink() {
        return "http://3ttech.cn/3tplayer.html?flv=http://pull.3ttech.cn/sdk/" + mRoomID + ".flv&hls=http://pull.3ttech.cn/sdk/" + mRoomID + ".m3u8";
    }

    /**
     * Author: wangzg <br/>
     * Time: 2017-11-21 18:08:37<br/>
     * Description: 显示因错误的回调而退出的对话框
     *
     * @param message the message 错误的原因
     */
    public void showErrorExitDialog(String message) {
        if (!TextUtils.isEmpty(message)) {
            String msg = getString(R.string.ttt_error_exit_dialog_prefix_msg) + ": " + message;
            mErrorExitDialog.setMessage(msg);//设置显示的内容
            mErrorExitDialog.show();
        }
    }

    private class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyTTTRtcEngineEventHandler.TAG.equals(action)) {
                JniObjs mJniObjs = intent.getParcelableExtra(MyTTTRtcEngineEventHandler.MSG_TAG);
                MyLog.d("UI onReceive callBack... mJniType : " + mJniObjs.mJniType);
                switch (mJniObjs.mJniType) {
                    case LocalConstans.CALL_BACK_ON_USER_KICK:
                        String message = "";
                        int errorType = mJniObjs.mErrorType;
                        if (errorType == Constants.ERROR_KICK_BY_HOST) {
                            message = getResources().getString(R.string.ttt_error_exit_kicked);
                        } else if (errorType == Constants.ERROR_KICK_BY_PUSHRTMPFAILED) {
                            message = getResources().getString(R.string.ttt_error_exit_push_rtmp_failed);
                        } else if (errorType == Constants.ERROR_KICK_BY_SERVEROVERLOAD) {
                            message = getResources().getString(R.string.ttt_error_exit_server_overload);
                        } else if (errorType == Constants.ERROR_KICK_BY_MASTER_EXIT) {
                            message = getResources().getString(R.string.ttt_error_exit_anchor_exited);
                        } else if (errorType == Constants.ERROR_KICK_BY_RELOGIN) {
                            message = getResources().getString(R.string.ttt_error_exit_relogin);
                        } else if (errorType == Constants.ERROR_KICK_BY_NEWCHAIRENTER) {
                            message = getResources().getString(R.string.ttt_error_exit_other_anchor_enter);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOAUDIODATA) {
                            message = getResources().getString(R.string.ttt_error_exit_noaudio_upload);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOVIDEODATA) {
                            message = getResources().getString(R.string.ttt_error_exit_novideo_upload);
                        } else if (errorType == Constants.ERROR_TOKEN_EXPIRED) {
                            message = getResources().getString(R.string.ttt_error_exit_token_expired);
                        }

                        showErrorExitDialog(message);
                        break;
                    case LocalConstans.CALL_BACK_ON_CONNECTLOST:
                        showErrorExitDialog(getString(R.string.ttt_error_network_disconnected));
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_JOIN:
                        long uid = mJniObjs.mUid;
                        int identity = mJniObjs.mIdentity;
                        EnterUserInfo userInfo = new EnterUserInfo(uid, identity, "");
                        mUsers.add(userInfo);
                        if (identity == CLIENT_ROLE_ANCHOR) {
                            mAnchorId = uid;
                            String localAnchorName = getString(R.string.ttt_role_anchor) + "ID: " + mRoomID;
                            ((TextView) findViewById(R.id.main_btn_host)).setText(localAnchorName);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_OFFLINE:
                        long offLineUserID = mJniObjs.mUid;
                        if (LocalConfig.mLocalRole == CLIENT_ROLE_ANCHOR) {
                            mWindowManager.removeAndSendSei(mUserId, offLineUserID);
                        } else {
                            mWindowManager.remove(offLineUserID);
                        }
                        int existIndex = -1;
                        for (int i = 0; i < mUsers.size(); i++) {
                            EnterUserInfo temp = mUsers.get(i);
                            if (temp.mUid == offLineUserID) {
                                existIndex = i;
                            }
                        }

                        if (existIndex != -1) {
                            mUsers.remove(existIndex);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_MUTE_VIDEO:
                        long muteVideoUid = mJniObjs.mUid;
                        String muteVideoDevID = mJniObjs.mDeviceID;
                        boolean muteEnableVideo = mJniObjs.mIsEnableVideo;
                        if (mRole == CLIENT_ROLE_ANCHOR) {
                            for (int i = 0; i < mUsers.size(); i++) {
                                EnterUserInfo temp = mUsers.get(i);
                                if (temp.mUid == muteVideoUid) {
                                    temp.mDeviceID = muteVideoDevID;
                                    if (muteEnableVideo) {
                                        mWindowManager.addAndSendSei(mUserId, temp);
                                    }
                                    break;
                                }
                            }
                        } else {
                            if (!muteEnableVideo) {
                                mWindowManager.remove(muteVideoDevID);
                            }
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_SEI:
                        MyLog.d("CALL_BACK_ON_SEI", "start parse sei....");
                        TreeSet<com.tttrtclive.bean.EnterUserInfo> mInfos = new TreeSet<>();
                        try {
                            JSONObject jsonObject = new JSONObject(mJniObjs.mSEI);
                            JSONArray jsonArray = jsonObject.getJSONArray("pos");
                            String anchorDevid = (String) jsonObject.get("mid");
                            MyLog.d("CALL_BACK_ON_SEI", "----------------第一步------------------------");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonobject2 = (JSONObject) jsonArray.get(i);
                                String devid = jsonobject2.getString("id");
                                long userId;
                                int index = devid.indexOf(":");
                                if (index > 0) {
                                    userId = Long.parseLong(devid.substring(0, index));
                                } else {
                                    userId = Long.parseLong(devid);
                                }
                                MyLog.d("CALL_BACK_ON_SEI", "遍历 userId : " + userId);
                                if (userId == mAnchorId) {
                                    MyLog.d("CALL_BACK_ON_SEI", "找到主播 index: " + index);
                                    if (index < 0) {
                                        LocalConfig.mIsPCAnchor = false;
                                        LocalConfig.mIsMacAnchor = false;
                                    } else {
                                        LocalConfig.mIsMacAnchor = true;
                                    }
                                    break;
                                }
                            }
                            MyLog.d("CALL_BACK_ON_SEI", "结果 mIsPCAnchor : " + LocalConfig.mIsPCAnchor +
                                    " | mIsMacAnchor : " + LocalConfig.mIsMacAnchor);
                            MyLog.d("CALL_BACK_ON_SEI", "----------------第一步------------------------");
                            MyLog.d("CALL_BACK_ON_SEI", "----------------第二步------------------------");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonobject2 = (JSONObject) jsonArray.get(i);
                                String devid = jsonobject2.getString("id");
                                float y = Float.valueOf(jsonobject2.getString("y"));
                                long userId;
                                int index = devid.indexOf(":");
                                if (index > 0) {
                                    userId = Long.parseLong(devid.substring(0, index));
                                } else {
                                    userId = Long.parseLong(devid);
                                }
                                MyLog.d("CALL_BACK_ON_SEI", "遍历 userId : " + userId);
                                if (userId != mAnchorId) {
                                    MyLog.d("CALL_BACK_ON_SEI", "找到一个不是主播的用户 y : " + y);
                                    if (y == 0) {
                                        LocalConfig.mIsPCAnchor = true;
                                        LocalConfig.mIsMacAnchor = false;
                                    }
                                    break;
                                }
                            }
                            MyLog.d("CALL_BACK_ON_SEI", "结果 mIsPCAnchor : " + LocalConfig.mIsPCAnchor +
                                    " | mIsMacAnchor : " + LocalConfig.mIsMacAnchor);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonobject2 = (JSONObject) jsonArray.get(i);
                                String devid = jsonobject2.getString("id");
                                float x = Float.valueOf(jsonobject2.getString("x"));
                                float y = Float.valueOf(jsonobject2.getString("y"));
                                float w = Float.valueOf(jsonobject2.getString("w"));
                                float h = Float.valueOf(jsonobject2.getString("h"));

                                long userId;
                                int index = devid.indexOf(":");
                                if (index > 0) {
                                    userId = Long.parseLong(devid.substring(0, index));
                                } else {
                                    userId = Long.parseLong(devid);
                                }
                                EnterUserInfo temp = new EnterUserInfo(userId, Constants.CLIENT_ROLE_BROADCASTER, devid);
                                temp.setXYLocation(y, x, w);
                                if (w != 1 && h != 1) {
                                    mInfos.add(temp);
                                } else {
                                    if (!mHasLocalView) {
                                        mHasLocalView = true;
                                        SurfaceView mSurfaceView = mTTTEngine.CreateRendererView(MainActivity.this);
                                        mTTTEngine.setupRemoteVideo(new VideoCanvas(userId, devid, Constants.RENDER_MODE_HIDDEN, mSurfaceView));
                                        ((ConstraintLayout) findViewById(R.id.local_view_layout)).addView(mSurfaceView);
                                    }
                                }
                                MyLog.d("CALL_BACK_ON_SEI", "parse EnterUserInfo : " + temp.toString() + " | width : " + w + " | height : " + h);

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        int count = 0;
                        for (EnterUserInfo temp : mInfos) {
                            temp.mShowIndex = count;
                            count++;
                        }

                        Iterator<EnterUserInfo> iterator = mInfos.iterator();
                        while (iterator.hasNext()) {
                            EnterUserInfo next = iterator.next();
                            MyLog.d("CALL_BACK_ON_SEI", "user list : " + next.mUid + " | deviceID : " + next.mDeviceID + " | index : " + next.mShowIndex);
                            mWindowManager.add(mUserId, next.mUid, next.mDeviceID, getRequestedOrientation(), next.mShowIndex);
                        }

                        synchronized (obj) {
                            if (mUserMutes.size() > 0) {
                                Set<Map.Entry<Long, Boolean>> entries = mUserMutes.entrySet();
                                Iterator<Map.Entry<Long, Boolean>> iterator2 = entries.iterator();
                                while (iterator2.hasNext()) {
                                    Map.Entry<Long, Boolean> next = iterator2.next();
                                    mWindowManager.muteAudio(next.getKey(), next.getValue());
                                }
                            }
                            mUserMutes.clear();
                            mIsReceiveSei = true;
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_AUDIO_STATE:
                        if (mJniObjs.mRemoteAudioStats.getUid() != mAnchorId) {
                            String audioString = getResources().getString(R.string.ttt_audio_downspeed);
                            String audioResult = String.format(audioString, String.valueOf(mJniObjs.mRemoteAudioStats.getReceivedBitrate()));
                            mWindowManager.updateAudioBitrate(mJniObjs.mRemoteAudioStats.getUid(), audioResult);
                        } else
                            setTextViewContent(mAudioSpeedShow, R.string.ttt_audio_downspeed, String.valueOf(mJniObjs.mRemoteAudioStats.getReceivedBitrate()));
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_VIDEO_STATE:
                        if (mJniObjs.mRemoteVideoStats.getUid() != mAnchorId) {
                            String videoString = getResources().getString(R.string.ttt_video_downspeed);
                            String videoResult = String.format(videoString, String.valueOf(mJniObjs.mRemoteVideoStats.getReceivedBitrate()));
                            mWindowManager.updateVideoBitrate(mJniObjs.mRemoteVideoStats.getUid(), videoResult);
                        } else
                            setTextViewContent(mVideoSpeedShow, R.string.ttt_video_downspeed, String.valueOf(mJniObjs.mRemoteVideoStats.getReceivedBitrate()));
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_AUDIO_STATE:
                        if (mRole == CLIENT_ROLE_ANCHOR)
                            setTextViewContent(mAudioSpeedShow, R.string.ttt_audio_upspeed, String.valueOf(mJniObjs.mLocalAudioStats.getSentBitrate()));
                        else {
                            String localAudioString = getResources().getString(R.string.ttt_audio_upspeed);
                            String localAudioResult = String.format(localAudioString, String.valueOf(mJniObjs.mLocalAudioStats.getSentBitrate()));
                            mWindowManager.updateAudioBitrate(mUserId, localAudioResult);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_VIDEO_STATE:
                        if (mRole == CLIENT_ROLE_ANCHOR)
                            setTextViewContent(mVideoSpeedShow, R.string.ttt_video_upspeed, String.valueOf(mJniObjs.mLocalVideoStats.getSentBitrate()));
                        else {
                            String localVideoString = getResources().getString(R.string.ttt_video_upspeed);
                            String localVideoResult = String.format(localVideoString, String.valueOf(mJniObjs.mLocalVideoStats.getSentBitrate()));
                            mWindowManager.updateVideoBitrate(mUserId, localVideoResult);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_MUTE_AUDIO:
                        long muteUid = mJniObjs.mUid;
                        boolean mIsMuteAuido = mJniObjs.mIsDisableAudio;
                        MyLog.i("OnRemoteAudioMuted CALL_BACK_ON_MUTE_AUDIO start! .... " + mJniObjs.mUid
                                + " | mIsMuteAuido : " + mIsMuteAuido);
                        if (muteUid == mAnchorId) {
//                            mIsMute = mIsMuteAuido;
//                            if (mIsHeadset)
//                                mAudioChannel.setImageResource(mIsMuteAuido ? R.drawable.mainly_btn_muted_headset_selector : R.drawable.mainly_btn_headset_selector);
//                            else
//                                mAudioChannel.setImageResource(mIsMuteAuido ? R.drawable.mainly_btn_mute_speaker_selector : R.drawable.mainly_btn_speaker_selector);
                        } else {
                            if (mRole != Constants.CLIENT_ROLE_ANCHOR) {
                                if (mIsReceiveSei) {
                                    mWindowManager.muteAudio(muteUid, mIsMuteAuido);
                                } else {
                                    mUserMutes.put(muteUid, mIsMuteAuido);
                                }
                            } else {
                                mWindowManager.muteAudio(muteUid, mIsMuteAuido);
                            }
                        }
                        break;

                    case LocalConstans.CALL_BACK_ON_AUDIO_ROUTE:
                        int mAudioRoute = mJniObjs.mAudioRoute;
                        if (mAudioRoute == Constants.AUDIO_ROUTE_SPEAKER || mAudioRoute == Constants.AUDIO_ROUTE_HEADPHONE) {
                            mIsHeadset = false;
                            mAudioChannel.setImageResource(mIsMute ? R.drawable.mainly_btn_mute_speaker_selector : R.drawable.mainly_btn_speaker_selector);
                        } else {
                            mIsHeadset = true;
                            mAudioChannel.setImageResource(mIsMute ? R.drawable.mainly_btn_muted_headset_selector : R.drawable.mainly_btn_headset_selector);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_COME:
                        mIsPhoneComing = true;
                        mIsSpeaker = mTTTEngine.isSpeakerphoneEnabled();
                        if (mIsSpeaker) {
                            mTTTEngine.setEnableSpeakerphone(false);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_IDLE:
                        if (mIsPhoneComing) {
                            if (mIsSpeaker) {
                                mTTTEngine.setEnableSpeakerphone(true);
                            }
                            mIsPhoneComing = false;
                        }
                    case LocalConstans.CALL_BACK_ON_AUDIO_VOLUME_INDICATION:
                        if (mIsMute) return;
                        int volumeLevel = mJniObjs.mAudioLevel;
                        if (mJniObjs.mUid == mUserId) {
                            if (mIsHeadset) {
                                if (volumeLevel >= 0 && volumeLevel <= 3) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_small_selector);
                                } else if (volumeLevel > 3 && volumeLevel <= 6) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_middle_selector);
                                } else if (volumeLevel > 6 && volumeLevel <= 9) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_big_selector);
                                }
                            } else {
                                if (volumeLevel >= 0 && volumeLevel <= 3) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_small_selector);
                                } else if (volumeLevel > 3 && volumeLevel <= 6) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_middle_selector);
                                } else if (volumeLevel > 6 && volumeLevel <= 9) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_big_selector);
                                }
                            }
                        } else {
                            mWindowManager.updateSpeakState(mJniObjs.mUid, mJniObjs.mAudioLevel);
                        }
                        break;
                }
            }
        }
    }

}
