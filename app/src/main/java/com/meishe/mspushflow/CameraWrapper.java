package com.meishe.mspushflow;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.util.Iterator;
import java.util.List;

/**
 * * All rights reserved,Designed by www.meishesdk.com
 *
 * @Author : lpf
 * @CreateDate : 2022/6/10 下午4:37
 * @Description : 相机包装类
 * @Copyright :www.meishesdk.com Inc.All rights reserved.
 */
public class CameraWrapper implements SurfaceHolder.Callback, Camera.PreviewCallback {


    private static final String TAG = "CameraWrapper";
    private Activity mActivity;
    /*高*/
    private int mHeight;
    /*宽*/
    private int mWidth;

    /*相机前后摄像头*/
    private int mCameraId;
    /*Camera1 预览采集图像数据*/
    private Camera mCamera;
    /*缓冲数据*/
    private byte[] mBuffer;
    /*surface 帮助类*/
    private SurfaceHolder mSurfaceHolder;
    /*后面预览的画面，把此预览的画面 的数据回调出现*/
    private PreviewCallback mPreviewCallback;
    /*旋转标识*/
    private int mRotation;
    /*宽高发生变化回调这个接口*/
    private OnChangedSizeListener mOnChangedSizeListener;

    public CameraWrapper(Activity activity, int cameraId, int width, int height) {
        mActivity = activity;
        mCameraId = cameraId;
        mWidth = width;
        mHeight = height;
    }

    public void switchCamera() {
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        stopPreview();
        startPreview();
    }


    /**
     * 开启预览
     * 数据预览的数据格式：有NV21 这个就是yuv420  最好不要使用RGBA格式预览
     *  yuv 更加高校
     *  RGBA 32位  yuv是 16位
     */
    private void startPreview() {
        /*获得camera对象*/
        mCamera=Camera.open(mCameraId);
        /*配置camera的属性*/
        Camera.Parameters parameters = mCamera.getParameters();
        /*设置预览数据格式为nv21  yuv420类型的子集*/
        parameters.setPreviewFormat(ImageFormat.NV21);
        /*设置摄像头分辨率*/
        setPreviewSize(parameters);
        /*设置摄像头 图像传感器的角度、方向*/
        setPreviewOrientation();

        mCamera.setParameters(parameters);
        /*初始化缓存大小 yuv420 4个y 对应一个uv  是2：1的关系*/
        mBuffer=new byte[mWidth*mHeight*3/2];
        /*数据缓存区*/
        mCamera.addCallbackBuffer(mBuffer);
        mCamera.setPreviewCallbackWithBuffer(mPreviewCallback);

        /*设置预览画面*/
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mOnChangedSizeListener!=null){
            mOnChangedSizeListener.onChanged(mWidth,mHeight);
        }

        /*开启预览*/
        mCamera.startPreview();

    }

    /**
     * 旋转画面 （预览默认是协的）
     */
    private void setPreviewOrientation() {
        Camera.CameraInfo cameraInfo=new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId,cameraInfo);
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation){
            case Surface.ROTATION_0:
                degrees = 0;
                break;
                /*左边是头部(home键在右边)*/
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
                /*头部在右边*/
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                break;
        }
        /*系统源码也是这么写的*/
        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    private void setPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size size = supportedPreviewSizes.get(0);
        int width = size.width;
        int height = size.height;

        // 选择一个与设置的差距最小的支持分辨率
        int m = Math.abs(width*height - mWidth * mHeight);
        supportedPreviewSizes.remove(0);
        Iterator<Camera.Size> iterator = supportedPreviewSizes.iterator();
        while (iterator.hasNext()){
            Camera.Size next = iterator.next();
            int n = Math.abs(next.width*next.height - mWidth * mHeight);
            if (n<m){
                m=n;
                size=next;
            }
        }

        mWidth = size.width;
        mHeight = size.height;
        parameters.setPreviewSize(mWidth, mHeight);
        Log.d(TAG, "预览分辨率w--->" + size.width + " h----->" + size.height);
    }

    /**
     * 停止预览
     */
    private void stopPreview() {
        if (mCamera!=null){
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera=null;
        }
    }



    public void setPreviewDisplay(SurfaceHolder surfaceHolder){
       mSurfaceHolder= surfaceHolder;
       mSurfaceHolder.addCallback(this);
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        stopPreview();
        startPreview();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        stopPreview();
    }


    /**
     * 相机预览回调
     *
     * @param bytes
     * @param camera
     */
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        Log.d(TAG,"预览数据--->"+bytes);
    }


    public interface OnChangedSizeListener {

        /**
         * 画面变化回调
         *
         * @param width
         * @param height
         */
        void onChanged(int width, int height);

    }
}
