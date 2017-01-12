package com.lqr.videorecordview;

import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * CSDN_LQR
 * <p>
 * 视频录制控件
 */
public class LQRVideoRecordView extends SurfaceView implements MediaRecorder.OnErrorListener {

    private SurfaceHolder mSurfaceHolder;
    private MediaRecorder mMediaRecorder;
    private Camera mCamera;
    private Timer mTimer;
    private OnRecordStausChangeListener mOnRecordStausChangeListener;// 录制状态变化回调接口

    private int mXpx;//视频分辨率宽度
    private int mYpx;//视频分辨率高度
    private int mOutFormat;//0是mp4，1是3gp
    private int mRecordMaxTime;// 一次拍摄最长时间
    private int mVideoEncodingBitRate;//声音的编码位率
    private int mVideoFrameRate;//录制的视频帧率

    private String mOutputDirPath = Environment.getExternalStorageDirectory()
            + File.separator + "lqr/video/";//输出目录
    private String mSuffix;//视频文件后缀

    private boolean mIsOpenCamera;// 是否一开始就打开摄像头
    private int mTimeCount;// 时间计数
    private File mVecordFile = null;// 视频输出文件

    public LQRVideoRecordView(Context context) {
        this(context, null);
    }

    public LQRVideoRecordView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LQRVideoRecordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LQRVideoRecordView, defStyleAttr, 0);
        mXpx = typedArray.getInteger(R.styleable.LQRVideoRecordView_vrv_x_px, 320);// 默认320
        mYpx = typedArray.getInteger(R.styleable.LQRVideoRecordView_vrv_y_px, 240);// 默认240
        mOutFormat = typedArray.getInteger(R.styleable.LQRVideoRecordView_vrv_out_format, 1);//默认3gp
        mRecordMaxTime = typedArray.getInteger(R.styleable.LQRVideoRecordView_vrv_record_max_time, 10);//默认最长10秒
        mVideoEncodingBitRate = typedArray.getInteger(R.styleable.LQRVideoRecordView_vrv_video_encoding_bit_rate, 1 * 1024 * 1024);
        mVideoFrameRate = typedArray.getInteger(R.styleable.LQRVideoRecordView_vrv_video_frame_rate, 10);
        mIsOpenCamera = typedArray.getBoolean(R.styleable.LQRVideoRecordView_vrv_is_open_camera, true);
        typedArray.recycle();

        switch (mOutFormat) {
            case 0:
                mSuffix = ".mp4";
                break;
            case 1:
                mSuffix = ".3gp";
                break;
        }

        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(new CustomCallBack());
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    private class CustomCallBack implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (!mIsOpenCamera)
                return;
            openCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (!mIsOpenCamera)
                return;
            freeCameraResource();
        }
    }

    /**
     * 初始化摄像头
     */
    public void openCamera() {
        try {
            if (mCamera != null) {
                freeCameraResource();
            }
            try {
                mCamera = Camera.open();
            } catch (Exception e) {
                e.printStackTrace();
                freeCameraResource();
            }
            if (mCamera == null)
                return;

            setCameraParams();
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
            mCamera.unlock();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置摄像头为竖屏
     */
    private void setCameraParams() {
        if (mCamera != null) {

//            Camera.Parameters params = mCamera.getParameters();
//            params.set("orientation", "portrait");
//            mCamera.setParameters(params);

            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size preSize = getCloselyPreSize(true, getWidth(), getHeight(), parameters.getSupportedPreviewSizes());
            parameters.setPreviewSize(preSize.width, preSize.height);
            mCamera.setParameters(parameters);
        }
    }

    /**
     * 通过对比得到与宽高比最接近的预览尺寸（如果有相同尺寸，优先选择）
     *
     * @param isPortrait    是否竖屏
     * @param surfaceWidth  需要被进行对比的原宽
     * @param surfaceHeight 需要被进行对比的原高
     * @param preSizeList   需要对比的预览尺寸列表
     * @return 得到与原宽高比例最接近的尺寸
     */
    public static Camera.Size getCloselyPreSize(boolean isPortrait, int surfaceWidth, int surfaceHeight, List<Camera.Size> preSizeList) {
        int reqTmpWidth;
        int reqTmpHeight;
        // 当屏幕为垂直的时候需要把宽高值进行调换，保证宽大于高
        if (isPortrait) {
            reqTmpWidth = surfaceHeight;
            reqTmpHeight = surfaceWidth;
        } else {
            reqTmpWidth = surfaceWidth;
            reqTmpHeight = surfaceHeight;
        }
        //先查找preview中是否存在与surfaceview相同宽高的尺寸
        for (Camera.Size size : preSizeList) {
            if ((size.width == reqTmpWidth) && (size.height == reqTmpHeight)) {
                return size;
            }
        }

        // 得到与传入的宽高比最接近的size
        float reqRatio = ((float) reqTmpWidth) / reqTmpHeight;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        Camera.Size retSize = null;
        for (Camera.Size size : preSizeList) {
            curRatio = ((float) size.width) / size.height;
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }

        return retSize;
    }

    /**
     * 释放摄像头资源
     */
    private void freeCameraResource() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.lock();
            mCamera.release();
            mCamera = null;
        }
    }

    private void createRecordDir() {
        //录制的视频保存文件夹
        File sampleDir = new File(mOutputDirPath);//录制视频的保存地址
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        //创建文件
        try {
            mVecordFile = File.createTempFile("recording", mSuffix, sampleDir);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CSDN_LQR", "创建视频文件失败");
        }
    }

    private void initRecord() throws IOException {
        mMediaRecorder = new MediaRecorder();

        try {
            mMediaRecorder.reset();
            if (mCamera != null)
                mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setOnErrorListener(this);
            mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);//视频源
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);//音频源
            mMediaRecorder.setOrientationHint(90);//输出旋转90度，保持坚屏录制

            switch (mOutFormat) {
                case 0:
                    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);// 视频输出格式
                    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);// 音频格式
                    mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);// 视频录制格式
                    break;
                case 1:
                    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
                    mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                    break;
            }

            //设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
            mMediaRecorder.setVideoSize(mXpx, mYpx);
            // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
            mMediaRecorder.setVideoFrameRate(mVideoFrameRate);
            mMediaRecorder.setVideoEncodingBitRate(mVideoEncodingBitRate);

            // mediaRecorder.setMaxDuration(Constant.MAXVEDIOTIME * 1000);
            mMediaRecorder.setOutputFile(mVecordFile.getAbsolutePath());

            mMediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mMediaRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始录制视频
     */
    public void record(final OnRecordStausChangeListener onRecordStausChangeListener) {
        this.mOnRecordStausChangeListener = onRecordStausChangeListener;
        createRecordDir();
        try {
            if (mOnRecordStausChangeListener != null)
                mOnRecordStausChangeListener.onRecordStart();

            if (!mIsOpenCamera)//如果未打开摄像头，则打开
                openCamera();
            initRecord();
            mTimeCount = 0;//时间计数器重新赋值
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mTimeCount++;
                    if (mOnRecordStausChangeListener != null)
                        mOnRecordStausChangeListener.onRecording(mTimeCount, mRecordMaxTime);
                    if (mTimeCount == mRecordMaxTime) {//达到指定时间，停止拍摄
                        stop();
                        if (mOnRecordStausChangeListener != null)
                            mOnRecordStausChangeListener.onRecrodFinish();
                    }
                }
            }, 0, 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止录制并释放相机资源
     */
    public void stop() {
        stopRecord();
        releaseRecord();
        freeCameraResource();
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        if (mTimer != null)
            mTimer.cancel();
        if (mMediaRecorder != null) {
            //设置后不会崩
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.setPreviewDisplay(null);
            try {
                mMediaRecorder.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 释放资源
     */
    private void releaseRecord() {
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            try {
                mMediaRecorder.release();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mMediaRecorder = null;
    }

    /**
     * 录制状态变化回调接口
     */
    public interface OnRecordStausChangeListener {
        public void onRecrodFinish();

        public void onRecording(int timeCount, int recordMaxTime);

        public void onRecordStart();
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        try {
            if (mr != null)
                mr.reset();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return 已经录制的秒数
     */
    public int getTimeCount() {
        return mTimeCount;
    }

    /**
     * @return 录制的视频文件
     */
    public File getVecordFile() {
        return mVecordFile;
    }

    public boolean isOpenCamera() {
        return mIsOpenCamera;
    }

    public void setOpenCamera(boolean openCamera) {
        mIsOpenCamera = openCamera;
    }

    public String getSuffix() {
        return mSuffix;
    }

    public void setSuffix(String suffix) {
        mSuffix = suffix;
    }

    public String getOutputDirPath() {
        return mOutputDirPath;
    }

    public void setOutputDirPath(String outputDirPath) {
        mOutputDirPath = outputDirPath;
    }

    public int getVideoFrameRate() {
        return mVideoFrameRate;
    }

    public void setVideoFrameRate(int videoFrameRate) {
        mVideoFrameRate = videoFrameRate;
    }

    public int getVideoEncodingBitRate() {
        return mVideoEncodingBitRate;
    }

    public void setVideoEncodingBitRate(int videoEncodingBitRate) {
        mVideoEncodingBitRate = videoEncodingBitRate;
    }

    public int getRecordMaxTime() {
        return mRecordMaxTime;
    }

    public void setRecordMaxTime(int recordMaxTime) {
        mRecordMaxTime = recordMaxTime;
    }

    public int getOutFormat() {
        return mOutFormat;
    }

    public void setOutFormat(int outFormat) {
        mOutFormat = outFormat;
    }

    public int getYpx() {
        return mYpx;
    }

    public void setYpx(int ypx) {
        mYpx = ypx;
    }

    public int getXpx() {
        return mXpx;
    }

    public void setXpx(int xpx) {
        mXpx = xpx;
    }
}
