package vmio.com.blemultipleconnect.service;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;

import static android.media.ToneGenerator.MAX_VOLUME;

public class FlashLightHelper {
    public static final int FlashingCicleTime = 200;
    public static final int FlashingOnTime  = 100;
    public static final int FlashingDuration = 1500;
    public void flash(Context context, FlashLightCallback callback) {
        // Beep tone woth Max volume
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float percent = 0.7f;
        int seventyVolume = (int) (maxVolume);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, seventyVolume, 0);
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, MAX_VOLUME);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CameraManager camManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String cameraId = null; // Usually back camera is at 0 position.
            try {
                if (camManager != null) {
                    cameraId = camManager.getCameraIdList()[0];
                }
                int totalTime = 0;
                while (totalTime < FlashingDuration){
                    camManager.setTorchMode(cameraId, true);   // Turn ON
                    tone.startTone(ToneGenerator.TONE_CDMA_ANSWER);
                    Thread.sleep(FlashingOnTime);
                    totalTime += FlashingOnTime;
                    camManager.setTorchMode(cameraId, false);  // Turn OFF
                    tone.stopTone();
                    Thread.sleep(FlashingCicleTime);
                    totalTime += FlashingCicleTime;
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            callback.onFlashFinish();
            tone.release();
        }
    }

    public void ringtone(Context context, long duration, long onTime, long cicleTime, FlashLightCallback callback) {
        // Beep tone woth Max volume
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int seventyVolume = (int) (maxVolume);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, seventyVolume, 0);
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, MAX_VOLUME);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                int totalTime = 0;
                while (totalTime < duration){
                    tone.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE);
                    Thread.sleep(onTime);
                    totalTime += onTime;

                    tone.stopTone();
                    Thread.sleep(cicleTime);
                    totalTime += cicleTime;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            callback.onFlashFinish();
            tone.release();
        }
    }

    public interface FlashLightCallback{
        void onFlashFinish();
    }
}
