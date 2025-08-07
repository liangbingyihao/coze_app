package sdk.chat.demo.robot.extensions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import com.bytedance.speech.speechengine.SpeechEngine;

import java.io.ByteArrayOutputStream;

public class SpeechStreamRecorder {

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_NUM = 2;
    private static final int BYTES_PER_SAMPLE = 2;
    private static final float BUFFER_SIZE_IN_SECONDS = 0.08f;
    private static final int DEFAULT_PACKAGE_DURATION = 100;

    private AudioRecord mRecorder;
    private Thread mWorker = null;
    private int mBufferSize = 0;
    private int mPackageDuration = DEFAULT_PACKAGE_DURATION;

    private String mViewId = "";
    private SpeechEngine mSpeechEngine = null;
    private final String tagAsr = "doubaoasr";

    public int GetStreamSampleRate() {
        return SAMPLE_RATE;
    }

    public int GetStreamChannel() {
        return CHANNEL_NUM;
    }

    public void SetSpeechEngine(String viewId, SpeechEngine speechEngine) {
        mViewId = viewId;
        mSpeechEngine = speechEngine;
    }

    @SuppressLint("MissingPermission")
    public boolean Start() {
        if (!InitStreamRecorder()) {
            return false;
        }
        if (null != mWorker) {
            if (mWorker.isAlive()) {
                Log.w(tagAsr, "Already start!");
                return true;
            }
            mWorker = null;
        }
//        mPackageDuration = SettingsActivity.getSettings(mViewId).getInt(R.string.config_stream_package_duration, DEFAULT_PACKAGE_DURATION);

        mWorker = new RecorderThread();
        mWorker.start();
        Log.i(tagAsr, "Stream Recorder Started.");
        return true;
    }

    public void Stop() {
        if (null == mWorker) {
            Log.w(tagAsr, "Not start yet!");
            return;
        }
        mWorker.interrupt();

        try {
            mWorker.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

        mWorker = null;
        Log.i(tagAsr, "Stream Recorder Stopped.");
    }

    private final class RecorderThread extends Thread {
        @Override
        public void run() {
            if (mRecorder == null) {
                return;
            }
            mRecorder.startRecording();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            int nread = 0;
            long totalPackageSize = (long)SAMPLE_RATE * CHANNEL_NUM * BYTES_PER_SAMPLE * mPackageDuration / 1000;
            while (!isInterrupted() && nread >= 0) {
                byte[] buffer = new byte[mBufferSize];
                bos.reset();
                long curPackageSize = 0;
                while (!isInterrupted() && nread >= 0 && curPackageSize < totalPackageSize) {
                    nread = mRecorder.read(buffer, 0, mBufferSize);
                    if (nread > 0) {
                        Log.i(tagAsr, "Current package size: " + curPackageSize + ", total package size: " + totalPackageSize);
                        curPackageSize += nread;
                        bos.write(buffer, 0, nread);
                    } else if (nread < 0) {
                        Log.e(tagAsr, "Recorder error.");
                    }
                }
                if (!isInterrupted()) {
                    buffer = bos.toByteArray();
                    int ret = mSpeechEngine.feedAudio(buffer, buffer.length);
                    if (ret != 0) {
                        Log.e(tagAsr, "Feed audio failed.");
                        break;
                    }
                }
            }
            mRecorder.stop();
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private boolean InitStreamRecorder() {
        if (mRecorder != null) {
            return true;
        }

        mBufferSize = Math.round(SAMPLE_RATE * BUFFER_SIZE_IN_SECONDS * BYTES_PER_SAMPLE * CHANNEL_NUM);
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                CHANNEL_NUM == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        minBufferSize = Math.max(minBufferSize, mBufferSize);

//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return false;
//        }
        mRecorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                CHANNEL_NUM == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, minBufferSize * 10);

        if (mRecorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.e(tagAsr, "Failed to initialize stream recorder.");
            mRecorder.release();
            mRecorder = null;
            return false;
        }
        return true;
    }
}
