package com.example.angitha.clarifaiimageprediction;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;
import okhttp3.OkHttpClient;

import static java.lang.String.format;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback,ClarifaiOperation.AsyncResponse{
    private static String TAG = "Resizing Tag";
    ClarifaiClient client;
    TextView testView;
    ImageView showImageView;
    ImageButton btnCapture;
    ImageButton btnStop;
    byte[] imageCaptured;
    Bitmap bitmap = null;
    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Camera.PictureCallback jpegCallback;
    private File file;
    private Bitmap bitmap_resized;
    ProgressBar pbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testView = (TextView) findViewById(R.id.textview);
        showImageView = (ImageView) findViewById(R.id.showImageView);

        btnCapture = (ImageButton) findViewById(R.id.btnCapture);
        btnStop = (ImageButton) findViewById(R.id.btnStop);

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        surfaceHolder.addCallback(this);

        // deprecated setting, but required on Android versions prior to 3.0
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                //imageCaptured = data;
                FileOutputStream outStream = null;
                try {
                    Uri selectedImage = Uri.parse(format("/sdcard/%d.jpg", System.currentTimeMillis()));
                    file = new File(format("/sdcard/%d.jpg", System.currentTimeMillis()));
                    String path = file.getAbsolutePath();

                    outStream = new FileOutputStream(file);
                    outStream.write(data);
                    outStream.close();

                    if (path != null) {
                        if (path.startsWith("content")) {
                            bitmap = decodeStrem(file, selectedImage,
                                    MainActivity.this);
                        } else {
                            bitmap = decodeFile(file, 10);
                        }
                    }
                    if (bitmap != null) {
                        surfaceView.setVisibility(View.GONE);
                        showImageView.setVisibility(View.VISIBLE);
                        showImageView.setImageBitmap(bitmap);

                        btnCapture.setVisibility(View.GONE);
                        btnStop.setVisibility(View.VISIBLE);
                        pbar.setVisibility(View.VISIBLE);

                        bitmap_resized = saveScaledPhotoToFile();

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap_resized.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        imageCaptured = stream.toByteArray();


                        ClarifaiOperation job = new ClarifaiOperation(client,imageCaptured);

                        job.execute();


                    }

                    Log.d("Log", "onPictureTaken - wrote bytes: " + data.length);


                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                }
                Toast.makeText(getApplicationContext(), "Picture Saved",Toast.LENGTH_SHORT).show();
                refreshCamera();
            }
        };

        client = new ClarifaiBuilder("HaMplFlR7N0yNOisoR9GHoQM6OvfXUGd0iuqtkjI",
                "cWH0_vw67Sphrq51o7uQdTHz0MdBvqhOJ09y7AbE").client(new OkHttpClient()).buildSync();


    }
    //===========================
    public Bitmap saveScaledPhotoToFile() {
        //Convert your photo to a bitmap
        Bitmap photoBm = (Bitmap) bitmap;
        //get its orginal dimensions
        int bmOriginalWidth = photoBm.getWidth();
        int bmOriginalHeight = photoBm.getHeight();
        double originalWidthToHeightRatio =  1.0 * bmOriginalWidth / bmOriginalHeight;
        double originalHeightToWidthRatio =  1.0 * bmOriginalHeight / bmOriginalWidth;
        //choose a maximum height
        int maxHeight = 1024;
        //choose a max width
        int maxWidth = 1024;
        //call the method to get the scaled bitmap
        photoBm = getScaledBitmap(photoBm, bmOriginalWidth, bmOriginalHeight,
                originalWidthToHeightRatio, originalHeightToWidthRatio,
                maxHeight, maxWidth);

        /**********THE REST OF THIS IS FROM Prabu's answer*******/
        //create a byte array output stream to hold the photo's bytes
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        //compress the photo's bytes into the byte array output stream
        photoBm.compress(Bitmap.CompressFormat.JPEG, 40, bytes);

        //construct a File object to save the scaled file to
        File f = new File(Environment.getExternalStorageDirectory()
                + File.separator + "Imagename.jpg");
        //create the file
        try {
            f.createNewFile();
            //create an FileOutputStream on the created file
            FileOutputStream fo = new FileOutputStream(f);
            //write the photo's bytes to the file
            fo.write(bytes.toByteArray());

            //finish by closing the FileOutputStream
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

      return photoBm;
    }

    private static Bitmap getScaledBitmap(Bitmap bm, int bmOriginalWidth, int bmOriginalHeight, double originalWidthToHeightRatio, double originalHeightToWidthRatio, int maxHeight, int maxWidth) {
        if(bmOriginalWidth > maxWidth || bmOriginalHeight > maxHeight) {
            Log.v(TAG, format("RESIZING bitmap FROM %sx%s ", bmOriginalWidth, bmOriginalHeight));

            if(bmOriginalWidth > bmOriginalHeight) {
                bm = scaleDeminsFromWidth(bm, maxWidth, bmOriginalHeight, originalHeightToWidthRatio);
            } else if (bmOriginalHeight > bmOriginalWidth){
                bm = scaleDeminsFromHeight(bm, maxHeight, bmOriginalHeight, originalWidthToHeightRatio);
            }

            Log.v(TAG, format("RESIZED bitmap TO %sx%s ", bm.getWidth(), bm.getHeight()));
        }
        return bm;
    }

    private static Bitmap scaleDeminsFromHeight(Bitmap bm, int maxHeight, int bmOriginalHeight, double originalWidthToHeightRatio) {
        int newHeight = (int) Math.max(maxHeight, bmOriginalHeight * .55);
        int newWidth = (int) (newHeight * originalWidthToHeightRatio);
        bm = Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
        return bm;
    }

    private static Bitmap scaleDeminsFromWidth(Bitmap bm, int maxWidth, int bmOriginalWidth, double originalHeightToWidthRatio) {
        //scale the width
        int newWidth = (int) Math.max(maxWidth, bmOriginalWidth * .75);
        int newHeight = (int) (newWidth * originalHeightToWidthRatio);
        bm = Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
        return bm;
    }


    //============================
    public static Bitmap decodeStrem(File fil, Uri selectedImage,
                                     Context mContext) {

        Bitmap bitmap = null;
        try {

            bitmap = BitmapFactory.decodeStream(mContext.getContentResolver()
                    .openInputStream(selectedImage));

            final int THUMBNAIL_SIZE = getThumbSize(bitmap);

            bitmap = Bitmap.createScaledBitmap(bitmap, THUMBNAIL_SIZE,
                    THUMBNAIL_SIZE, false);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(baos
                    .toByteArray()));

            return bitmap = rotateImage(bitmap, fil.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static Bitmap decodeFile(File f, int sampling) {
        try {
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(
                    new FileInputStream(f.getAbsolutePath()), null, o2);

            o2.inSampleSize = sampling;
            o2.inTempStorage = new byte[48 * 1024];

            o2.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeStream(
                    new FileInputStream(f.getAbsolutePath()), null, o2);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            bitmap = rotateImage(bitmap, f.getAbsolutePath());
            return bitmap;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Rotate image.
     *
     * @param bmp
     *            the bmp
     * @param imageUrl
     *            the image url
     * @return the bitmap
     */
    public static Bitmap rotateImage(Bitmap bmp, String imageUrl) {
        if (bmp != null) {
            ExifInterface ei;
            int orientation = 0;
            try {
                ei = new ExifInterface(imageUrl);
                orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);

            } catch (IOException e) {
                e.printStackTrace();
            }
            int bmpWidth = bmp.getWidth();
            int bmpHeight = bmp.getHeight();
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_UNDEFINED:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    break;
            }
            Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmpWidth,
                    bmpHeight, matrix, true);
            return resizedBitmap;
        } else {
            return bmp;
        }
    }


    /**
     * Gets the thumb size.
     *
     * @param bitmap
     *            the bitmap
     * @return the thumb size
     */
    public static int getThumbSize(Bitmap bitmap) {

        int THUMBNAIL_SIZE = 250;
        if (bitmap.getWidth() < 300) {
            THUMBNAIL_SIZE = 250;
        } else if (bitmap.getWidth() < 600) {
            THUMBNAIL_SIZE = 500;
        } else if (bitmap.getWidth() < 1000) {
            THUMBNAIL_SIZE = 750;
        } else if (bitmap.getWidth() < 2000) {
            THUMBNAIL_SIZE = 1500;
        } else if (bitmap.getWidth() < 4000) {
            THUMBNAIL_SIZE = 2000;
        } else if (bitmap.getWidth() > 4000) {
            THUMBNAIL_SIZE = 2000;
        }
        return THUMBNAIL_SIZE;
    }

    //============================

    public void captureImage(View v) throws IOException {
        //take the picture
        showImageView.setVisibility(View.GONE);

        camera.takePicture(null, null, jpegCallback);
    }

    public void refreshCamera() {
        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }
        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try{
            camera = Camera.open();
            camera.setDisplayOrientation(90);
        }catch (RuntimeException e){
           e.printStackTrace();
            return;
        }
        Camera.Parameters param;
        param = camera.getParameters();
        param.setPreviewSize(352, 288);
        camera.setParameters(param);
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
           e.printStackTrace();
            return;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        refreshCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    @Override
    public void processFinish(List<ClarifaiOutput<Concept>> s) {
        pbar.setVisibility(View.GONE);
        testView.setVisibility(View.VISIBLE);
        testView.setText(s.get(0).data().get(0).name());
    }

    public void quitPrediction(View view) {
        btnCapture.setVisibility(View.VISIBLE);
        btnStop.setVisibility(View.GONE);
        pbar.setVisibility(View.GONE);
    }
}
