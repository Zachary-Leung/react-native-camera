/**
 * Created by Fabrice Armisen (farmisen@gmail.com) on 1/4/16.
 */

package com.lwansbrough.RCTCamera;

import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RCTCamera {

    private static final RCTCamera ourInstance = new RCTCamera();
    private final HashMap<Integer, CameraInfoWrapper> _cameraInfos;
    private final HashMap<Integer, Integer> _cameraTypeToIndex;
    private final Map<Number, Camera> _cameras;
    private int _orientation = -1;
    private int _actualDeviceOrientation = 0;

    public static RCTCamera getInstance() {
        return ourInstance;
    }

    public Camera acquireCameraInstance(int type) {
        if (null == _cameras.get(type) && null != _cameraTypeToIndex.get(type)) {
            try {
                Camera camera = Camera.open(_cameraTypeToIndex.get(type));
                _cameras.put(type, camera);
                adjustPreviewLayout(type);
            } catch (Exception e) {
                System.console().printf("acquireCameraInstance: %s", e.getLocalizedMessage());
            }
        }
        return _cameras.get(type);
    }

    public void releaseCameraInstance(int type) {
        if (null != _cameras.get(type)) {
            _cameras.get(type).release();
            _cameras.remove(type);
        }
    }

    public int getPreviewWidth(int type) {
        CameraInfoWrapper cameraInfo = _cameraInfos.get(type);
        if (null == cameraInfo) {
            return 0;
        }
        return cameraInfo.previewWidth;
    }

    public int getPreviewHeight(int type) {
        CameraInfoWrapper cameraInfo = _cameraInfos.get(type);
        if (null == cameraInfo) {
            return 0;
        }
        return cameraInfo.previewHeight;
    }

    public float getBestPictureSizeRatio(int type){
        int width = Integer.MAX_VALUE,height=Integer.MAX_VALUE;
        Camera camera = acquireCameraInstance(type);
        Camera.Size result = null;
        if(camera == null) {
            return 1;
        }
        Camera.Parameters params = camera.getParameters();
        for (Camera.Size size : params.getSupportedPictureSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        Log.d("snake","TYPE:"+type+"getBestPictureSizeRatio W:"+result.width+"H:"+result.height);
        return (float) result.height/result.width;//以竖屏计算比例
    }

    public Camera.Size getBestPreviewSize(int type, int width, int height)
    {
        Camera camera = _cameras.get(type);
        Camera.Size result = null;
        if(camera == null) {
            return null;
        }
        Camera.Parameters params = camera.getParameters();
        float Ratio = getBestPictureSizeRatio(type);
        for (Camera.Size size : params.getSupportedPreviewSizes()) {
//            Log.d("snake","TYPE:"+type+" getBestPreviewSize W:"+size.width+"H:"+size.height);
            float newRatio = (float) size.height/size.width;
//            Log.d("snake","TYPE:"+type+" getBestPreviewSize newRatio:"+newRatio);
            //防止摄像头分辨率过高造成变形，预览宽高不得比大于两都最少值（默认传入width:1920,height:1080）
            if (size.width <= height && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    float resultRatio = (float)result.height/result.width;
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;
                    if (newArea>resultArea&&newRatio ==Ratio) {
                        result = size;
                    }else if(newRatio==Ratio&&resultRatio!=Ratio){
                        result=size;
                    }
                }
            }
        }
//        Log.d("snake","TYPE:"+type+"getBestPreviewSize W:"+result.width+"H:"+result.height);
        return result;
    }

    public Camera.Size getBestPictureSize(int type, int width, int height)
    {
        Camera camera = _cameras.get(type);
        Camera.Size result = null;
        if(camera == null) {
            return null;
        }
        Camera.Parameters params = camera.getParameters();
        for (Camera.Size size : params.getSupportedPictureSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
//        Log.d("snake","TYPE:"+type+"getBestPictureSize W:"+result.width+"H:"+result.height);
        return result;
    }

    public Camera.Size getSmallestPictureSize(int type)
    {
        Camera camera = _cameras.get(type);
        Camera.Size result = null;
        if(camera == null) {
            return null;
        }
        Camera.Parameters params = camera.getParameters();
        for (Camera.Size size : params.getSupportedPictureSizes()) {
            if (result == null) {
                result = size;
            } else {
                int resultArea = result.width * result.height;
                int newArea = size.width * size.height;

                if (newArea < resultArea) {
                    result = size;
                }
            }
        }
        return result;
    }

    public void setOrientation(int orientation) {
        if (_orientation == orientation) {
            return;
        }
        _orientation = orientation;
        adjustPreviewLayout(RCTCameraModule.RCT_CAMERA_TYPE_FRONT);
        adjustPreviewLayout(RCTCameraModule.RCT_CAMERA_TYPE_BACK);
    }

    public void setActualDeviceOrientation(int actualDeviceOrientation) {
        _actualDeviceOrientation = actualDeviceOrientation;
        adjustPreviewLayout(RCTCameraModule.RCT_CAMERA_TYPE_FRONT);
        adjustPreviewLayout(RCTCameraModule.RCT_CAMERA_TYPE_BACK);
    }

    public void setCaptureQuality(int cameraType, String captureQuality) {
        Camera camera = _cameras.get(cameraType);
        if (null == camera) {
            return;
        }

        Camera.Parameters parameters = camera.getParameters();
        Camera.Size pictureSize = null;
        switch (captureQuality) {
            case "low":
                pictureSize = getSmallestPictureSize(cameraType); // select the lowest res
                break;
            case "medium":
                List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
                pictureSize = sizes.get(sizes.size() / 2);
                break;
            case "high":
                pictureSize = getBestPictureSize(cameraType, Integer.MAX_VALUE, Integer.MAX_VALUE); // select the highest res
                break;
        }

        if (pictureSize != null) {
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
            camera.setParameters(parameters);
        }
    }

    public void setTorchMode(int cameraType, int torchMode) {
        Camera camera = _cameras.get(cameraType);
        if (null == camera) {
            return;
        }

        Camera.Parameters parameters = camera.getParameters();
        String value = parameters.getFlashMode();
        switch (torchMode) {
            case RCTCameraModule.RCT_CAMERA_TORCH_MODE_ON:
                value = Camera.Parameters.FLASH_MODE_TORCH;
                break;
            case RCTCameraModule.RCT_CAMERA_TORCH_MODE_OFF:
                value = Camera.Parameters.FLASH_MODE_OFF;
                break;
        }

        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(value)) {
            parameters.setFlashMode(value);
            camera.setParameters(parameters);
        }
    }

    public void setFlashMode(int cameraType, int flashMode) {
        Camera camera = _cameras.get(cameraType);
        if (null == camera) {
            return;
        }

        Camera.Parameters parameters = camera.getParameters();
        String value = parameters.getFlashMode();
        switch (flashMode) {
            case RCTCameraModule.RCT_CAMERA_FLASH_MODE_AUTO:
                value = Camera.Parameters.FLASH_MODE_AUTO;
                break;
            case RCTCameraModule.RCT_CAMERA_FLASH_MODE_ON:
                value = Camera.Parameters.FLASH_MODE_ON;
                break;
            case RCTCameraModule.RCT_CAMERA_FLASH_MODE_OFF:
                value = Camera.Parameters.FLASH_MODE_OFF;
                break;
        }
        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(value)) {
            parameters.setFlashMode(value);
            camera.setParameters(parameters);
        }
    }

    private void adjustPreviewLayout(int type) {
        Camera camera = _cameras.get(type);
        if (null == camera) {
            return;
        }

        CameraInfoWrapper cameraInfo = _cameraInfos.get(type);
        int displayRotation;
        int rotation;
        int orientation = cameraInfo.info.orientation;
        if (cameraInfo.info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (orientation + _actualDeviceOrientation * 90) % 360;
            displayRotation = (720 - orientation - _actualDeviceOrientation * 90) % 360;
        } else {
            rotation = (orientation - _actualDeviceOrientation * 90 + 360) % 360;
            displayRotation = rotation;
        }
        cameraInfo.rotation = rotation;
        // TODO: take in account the _orientation prop

//        Log.d("snake","displayRotation: "+displayRotation);
        camera.setDisplayOrientation(displayRotation);

        Camera.Parameters parameters = camera.getParameters();
        parameters.setRotation(cameraInfo.rotation);


        // set preview size
        // defaults to highest resolution available
        Camera.Size optimalPreviewSize = getBestPreviewSize(type, 1920, 1080);
        int width = optimalPreviewSize.width;
        int height = optimalPreviewSize.height;

        if(RCTCameraView.screenHigh >0){
            if(height > RCTCameraView.screenHigh || width > RCTCameraView.screenWidth){
                height = RCTCameraView.screenHigh;
                width = RCTCameraView.screenWidth;
            }
            parameters.setPreviewSize(width, height);
            try {
                camera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (cameraInfo.rotation == 0 || cameraInfo.rotation == 180) {
                cameraInfo.previewWidth = width;
                cameraInfo.previewHeight = height;
            } else {
                cameraInfo.previewWidth = height;
                cameraInfo.previewHeight = width;
            }
        }
    }

    private RCTCamera() {
        _cameras = new HashMap<>();
        _cameraInfos = new HashMap<>();
        _cameraTypeToIndex = new HashMap<>();

        // map camera types to camera indexes and collect cameras properties
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && _cameraInfos.get(RCTCameraModule.RCT_CAMERA_TYPE_FRONT) == null) {
                _cameraInfos.put(RCTCameraModule.RCT_CAMERA_TYPE_FRONT, new CameraInfoWrapper(info));
                _cameraTypeToIndex.put(RCTCameraModule.RCT_CAMERA_TYPE_FRONT, i);
                acquireCameraInstance(RCTCameraModule.RCT_CAMERA_TYPE_FRONT);
                releaseCameraInstance(RCTCameraModule.RCT_CAMERA_TYPE_FRONT);
            } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK && _cameraInfos.get(RCTCameraModule.RCT_CAMERA_TYPE_BACK) == null) {
                _cameraInfos.put(RCTCameraModule.RCT_CAMERA_TYPE_BACK, new CameraInfoWrapper(info));
                _cameraTypeToIndex.put(RCTCameraModule.RCT_CAMERA_TYPE_BACK, i);
                acquireCameraInstance(RCTCameraModule.RCT_CAMERA_TYPE_BACK);
                releaseCameraInstance(RCTCameraModule.RCT_CAMERA_TYPE_BACK);
            }
        }
    }

    private class CameraInfoWrapper {
        public final Camera.CameraInfo info;
        public int rotation = 0;
        public int previewWidth = -1;
        public int previewHeight = -1;

        public CameraInfoWrapper(Camera.CameraInfo info) {
            this.info = info;
        }
    }
}
