package com.example.roaddamagedetection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int WAITING = 0;
    static final int SENDING = 1;
    static final int RECEIVING = 2;
    static final int DRAWING = 3;

    private int state;

    private double[] boxCoords = {0.0, 0.0, 0.0, 0.0};
    private int clss;

    private Camera mCamera;
    private CameraPreview mPreview;
    private CameraOverlay mOverlay;
    private Socket sock;
    private DataOutputStream dout;
    private DataInputStream din;

    private Tcp mTcp;

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 2;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        findViewById(R.id.detection_layout).setVisibility(View.INVISIBLE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_INTERNET);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }

        // Create an instance of Camera
        mCamera = getCameraInstance();
        if(mCamera == null){
            // couldn't get camera instance
            return;
        }

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        mOverlay = new CameraOverlay(this);
        preview.addView(mOverlay);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_INTERNET: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            Log.d("MainActivity", "Got a camera instance");
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            Log.d("MainActivity", "getCameraInstance: " + e.getMessage());
        }
        return c; // returns null if camera is unavailable
    }

    /** A basic Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
        private SurfaceHolder mHolder;
        private Camera mCamera;
        private int frameCount = 0;
        private Rect box;
        private Paint paint;
        private int JPGcounter = 0;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;
            mCamera.setPreviewCallback(this);
            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            Camera.Parameters params = mCamera.getParameters();
            if(this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
                params.set("orientation", "portrait");
                mCamera.setDisplayOrientation(90);
                params.setRotation(90);
            } else {
                params.set("orientation", "landscape");
                mCamera.setDisplayOrientation(0);
                params.setRotation(0);
            }

            mCamera.setParameters(params);
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d("MainActivity", "surfaceCreated: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e){
                Log.d("MainActivity", "surfaceChanged: " + e.getMessage());
            }
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if(frameCount % 360 == 0){
                frameCount = 0;
                // send data to server asynchronously
//                new Async(data).execute();
                if(mTcp != null){
//                    if(state == WAITING){
//                        state = SENDING;
//                    } else {
//                        frameCount++;
//                        return;
//                    }
                    // METHOD 1
//                    try {
//                        Camera.Parameters parameters = camera.getParameters();
//                        Camera.Size size = parameters.getPreviewSize();
//                        YuvImage image = new YuvImage(data, parameters.getPreviewFormat(), size.width, size.height, null);
//                        File file = new File(Environment.getExternalStorageDirectory(), "camera_preview"+JPGcounter+".jpg");
//                        JPGcounter++;
//                        FileOutputStream filecon = new FileOutputStream(file);
//                        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 90, filecon);
//                        mTcp.sendFile(file, "0.1, 2.4, 3.5".getBytes("UTF-8"));
//                    } catch (FileNotFoundException e) {
//                        Toast toast = Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT);
//                        toast.show();
//                    } catch (UnsupportedEncodingException e) {
//                        e.printStackTrace();
//                    }
                    // METHOD 2
//                    Camera.Parameters params = camera.getParameters();
//                    Camera.Size size = params.getPreviewSize();
//                    YuvImage im = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
//                    int quality = 90;
//                    Rect rect = new Rect(0, 0, size.width, size.height);
//                    FileOutputStream output = null;
//                    try {
//                        output = new FileOutputStream("/storage/emulated/0/roadDamageDetection/preview"+JPGcounter+".jpg");
//                        JPGcounter++;
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    }
//                    im.compressToJpeg(rect, quality, output);
//                    try {
//                        output.flush();
//                        output.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                    // METHOD 3
//                    try {
//                        Log.i("img size", ""+data.length);
//                        byte[][] messages = new byte[data.length][5];
//                        messages[0] = "img".getBytes("UTF-8");
//                        messages[1] = data;
//                        messages[2] = "end".getBytes("UTF-8");
//                        messages[3] = "gps".getBytes("UTF-8");
//                        messages[4] = "0.1, 2.4, 3.5".getBytes("UTF-8");
//                        mTcp.sendMessages(messages);
//                    } catch (Exception e){
//                        e.printStackTrace();
//                    }
                    // METHOD 4
                    try {
                        Log.d("data length:", ""+data.length);
                        mTcp.sendPacket(data, "0.1, 2.4, 3.5".getBytes("UTF-8"));
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
            frameCount++;
        }

    }

    public class CameraOverlay extends View {
        Rect box;
        Paint paint;

        public CameraOverlay(Context context){
            super(context);

            setWillNotDraw(false);
            setBackgroundColor(Color.TRANSPARENT);
            setAlpha(1f);

//            box = new Rect(50, 50, 550, 550);
//            paint = new Paint();
        }
        // Additional functions:

        public void onDraw(Canvas canvas){
//            paint.setStyle(Paint.Style.STROKE);
//            paint.setColor(Color.RED);
//            paint.setStrokeWidth(5);
//            paint.setAntiAlias(true);
//            canvas.drawRect(box, paint);
        }
        // onMeasure(int widthMeasureSpec, int heightMeasureSpec)


    }

    public void startDetection(View view){
        findViewById(R.id.detect_button).setVisibility(View.INVISIBLE);
        findViewById(R.id.detection_layout).setVisibility(View.VISIBLE);

        new ConnectTask().execute("");
        state = WAITING;
    }

    public void stopDetection(View view){
        findViewById(R.id.detection_layout).setVisibility(View.INVISIBLE);
        findViewById(R.id.detect_button).setVisibility(View.VISIBLE);

        if(mTcp != null){
            mTcp.stopClient();
        }
    }

    public class ConnectTask extends AsyncTask<String, String, Tcp> {

        @Override
        protected Tcp doInBackground(String... message) {

            //we create a TCPClient object
            mTcp = new Tcp(new Tcp.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            });
            mTcp.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //response received from server
            Log.d("test", "response " + values[0]);
            //process server response here....

        }
    }

//    public class ConnectTask extends AsyncTask<String, String, TcpClient> {
//
//        @Override
//        protected TcpClient doInBackground(String... message) {
//
//            //we create a TCPClient object
//            mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
//                @Override
//                //here the messageReceived method is implemented
//                public void messageReceived(String message) {
//                    //this method calls the onProgressUpdate
//                    publishProgress(message);
//                }
//            });
//            mTcpClient.run();
//
//            return null;
//        }
//
//        @Override
//        protected void onProgressUpdate(String... values) {
//            super.onProgressUpdate(values);
//            //response received from server
//            Log.d("test", "response " + values[0]);
//            //process server response here....
//
//        }
//    }
}
