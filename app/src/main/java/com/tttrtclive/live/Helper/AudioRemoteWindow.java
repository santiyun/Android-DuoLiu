package com.tttrtclive.live.Helper;

import android.content.Context;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tttrtclive.live.R;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.wushuangtech.wstechapi.model.VideoCanvas;

public class AudioRemoteWindow extends RelativeLayout {

    private TTTRtcEngine mTTTEngine;
    private Context mContext;

    public long mId = -1;
    public String mDeviceID = "";
    private boolean mIsMuted;
    private boolean mLocalIsMuted = false;

    private ImageView mSpeakImage;
    private ImageView mCameraImage;
    private TextView mVideoMuteTV;
    private TextView mIdView;
    private TextView mAudioBitrate;
    private TextView mVideoBitrate;
    private ConstraintLayout mVideoLayout;
    public int mIndex;

    public AudioRemoteWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        View v = LayoutInflater.from(context).inflate(R.layout.audio_remote_window, this, true);
        mSpeakImage = v.findViewById(R.id.speakimage);
        mCameraImage = v.findViewById(R.id.remote_btn_switch_camera);
        mVideoMuteTV = v.findViewById(R.id.remote_view_mute_video);
        mIdView = v.findViewById(R.id.id);
        mAudioBitrate = v.findViewById(R.id.audiorate);
        mVideoBitrate = v.findViewById(R.id.videorate);
        mVideoLayout = v.findViewById(R.id.video_layout);

        mTTTEngine = TTTRtcEngine.getInstance();
    }

    public void show(long id, String mDeviceID) {
        show(-1, id,mDeviceID,  0);
    }

    public void show(long localId, long id, String mDeviceID, int oritation) {
        if (TextUtils.isEmpty(mDeviceID)) {
            return ;
        }
        mId = id;
        this.mDeviceID = mDeviceID;
        mIdView.setText("" + mId);
        mSpeakImage.setVisibility(View.VISIBLE);
        mIdView.setVisibility(View.VISIBLE);
        mAudioBitrate.setVisibility(View.VISIBLE);
        mVideoBitrate.setVisibility(View.VISIBLE);
        mVideoMuteTV.setVisibility(View.VISIBLE);

        if (localId == id) {
            mSpeakImage.setOnClickListener(v -> {
                mLocalIsMuted = !mLocalIsMuted;
                mSpeakImage.setImageResource(mLocalIsMuted ? R.drawable.mainly_btn_mute_speaker_selector : R.drawable.mainly_btn_speaker_selector);
                mTTTEngine.muteLocalAudioStream(mLocalIsMuted);
            });
            mCameraImage.setVisibility(View.VISIBLE);
            mCameraImage.setOnClickListener(v -> {
                mTTTEngine.switchCamera();
            });
        }

        SurfaceView mSurfaceView = mTTTEngine.CreateRendererView(mContext);
        mSurfaceView.setZOrderMediaOverlay(true);
        if (localId == id) {
            mTTTEngine.setupLocalVideo(new VideoCanvas(0, Constants.RENDER_MODE_HIDDEN, mSurfaceView), oritation);
        } else {
            mTTTEngine.setupRemoteVideo(new VideoCanvas(mId, mDeviceID, Constants.RENDER_MODE_HIDDEN, mSurfaceView));
        }
        mVideoLayout.addView(mSurfaceView);
        mSpeakImage.setImageResource(mIsMuted ? R.drawable.jinyan : R.drawable.mainly_btn_speaker_selector);

        mVideoMuteTV.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Object mMuteTag = mVideoMuteTV.getTag();
                if (mMuteTag == null || mMuteTag.equals("unmuted")) {
                    mTTTEngine.muteRemoteVideoStream(mId, mDeviceID, true);
                    mVideoMuteTV.setTag("muted");
                    mVideoMuteTV.setTextColor(Color.GREEN);
                    mVideoMuteTV.setText("打开视频");
                } else {
                    mTTTEngine.muteRemoteVideoStream(mId, mDeviceID, false);
                    mVideoMuteTV.setTag("unmuted");
                    mVideoMuteTV.setTextColor(Color.RED);
                    mVideoMuteTV.setText("禁止视频");
                }
            }
        });
    }

    public void hide() {
        mId = -1;
        mDeviceID = "";
        mSpeakImage.setVisibility(View.INVISIBLE);
        mCameraImage.setVisibility(View.INVISIBLE);
        mIdView.setVisibility(View.INVISIBLE);
        mAudioBitrate.setVisibility(View.INVISIBLE);
        mVideoBitrate.setVisibility(View.INVISIBLE);
        mVideoMuteTV.setVisibility(View.INVISIBLE);
        mVideoLayout.removeAllViews();
    }

    public void mute(boolean mute) {
        mIsMuted = mute;
        mSpeakImage.setImageResource(mute ? R.drawable.jinyan : R.drawable.mainly_btn_speaker_selector);
    }

    public void updateAudioBitrate(String bitrate) {
        mAudioBitrate.setText(bitrate);
    }

    public void updateVideoBitrate(String bitrate) {
        mVideoBitrate.setText(bitrate);
    }

    public void updateSpeakState(int volumeLevel) {
        if (mIsMuted) return;

        if (volumeLevel >= 0 && volumeLevel <= 3) {
            mSpeakImage.setImageResource(R.drawable.mainly_btn_speaker_selector);
        } else if (volumeLevel > 3 && volumeLevel <= 6) {
            mSpeakImage.setImageResource(R.drawable.mainly_btn_speaker_middle_selector);
        } else if (volumeLevel > 6 && volumeLevel <= 9) {
            mSpeakImage.setImageResource(R.drawable.mainly_btn_speaker_big_selector);
        }
    }

}
