package com.google.android.mgc.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.Utils;

import static org.opencv.core.Core.mean;

public class IDCameraActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private String TAG ="IDCameraActivity";
    private boolean debug = true;
    private TessBaseAPI baseApi;
    private TessBaseAPI chiApi;
    private CameraManager cameraManager;
    private boolean hasSurface;
    private Button btn_close, light;
    private boolean toggleLight = false;
    private TextView tv_lightstate;
    private String sdPath;
    private Long opentime;
    private FaceDet mFaceDet;
    private int thresh = 110;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        opentime = System.currentTimeMillis();
        sdPath = Constants.getDLibDirectoryPath()+ "/tessdata";
        try {
            copyAssetFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        baseApi = new TessBaseAPI();
        chiApi  = new TessBaseAPI();

        baseApi.init(Constants.getDLibDirectoryPath(), "eng");
        baseApi.setVariable("tessedit_char_whitelist", "0123456789X");

        chiApi.init(Constants.getDLibDirectoryPath(), "chi_sim");

        setContentView(R.layout.id_activity_camera);
        tv_lightstate = (TextView) findViewById(R.id.tv_openlight);
         initLayoutParams();

        // Init face dete
        if (mFaceDet == null) {
            mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
        }
    }

     private void initLayoutParams() {
        btn_close = (Button) findViewById(R.id.btn_close);
        light = (Button) findViewById(R.id.light);
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                onBackPressed();

            }
        });
        light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long time = System.currentTimeMillis();
                if (time - opentime > 2000) {
                    opentime = time;
                    if (!toggleLight) {
                        toggleLight = true;
                        tv_lightstate.setText("关闭闪关灯");
                        cameraManager.openLight();
                    } else {
                        toggleLight = false;
                        tv_lightstate.setText("打开闪关灯");
                        cameraManager.offLight();
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraManager = new CameraManager();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();

        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
             surfaceHolder.addCallback(this);
        }

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    /**
     *
     * @param surfaceHolder SurfaceHolder
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder, this);
            cameraManager.startPreview(this);
        } catch (Exception ioe) {
            Log.d("zk", ioe.toString());

        }
    }

    @Override
    protected void onPause() {
        cameraManager.stopPreview();
        cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    private boolean copyAssetFile() throws Exception {

        String dir = sdPath;
        String filePath = sdPath + "/eng.traineddata";
        String filePathChi = sdPath + "/chi_sim.traineddata";
        File f = new File(dir);
        if (!f.exists()) {
            f.mkdirs();
        }

        if (!new File(filePath).exists()) {
            FileUtils.copyFileFromRawToOthers(IDCameraActivity.this, R.raw.eng, filePath);
        }

        if (!new File(filePathChi).exists()) {
            FileUtils.copyFileFromRawToOthers(IDCameraActivity.this, R.raw.chi_sim, filePathChi);
        }

        return false;
    }

    public String doOcr(Bitmap bitmap) {
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        baseApi.setImage(bitmap);
        String text = baseApi.getUTF8Text();
        baseApi.clear();
        return text;
    }

    public String doNameOcr(Bitmap bitmap) {
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        chiApi.setImage(bitmap);
        String text = chiApi.getUTF8Text();
        chiApi.clear();
        return text;
    }


    @Override
    public void onBackPressed() {
        if (baseApi != null)
            baseApi.end();
        if (chiApi != null)
            chiApi.end();

        super.onBackPressed();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        ByteArrayOutputStream baos;
        byte[] rawImage;
        Bitmap bitmap;
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = true;
        YuvImage yuvimage = new YuvImage(
                data,
                ImageFormat.NV21,
                previewSize.width,
                previewSize.height,
                null);
        baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 95, baos);
        Log.d(TAG, "preview w:"+previewSize.width +" h:"+previewSize.height);
        rawImage = baos.toByteArray();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);

        if (bitmap == null) {
            Log.d(TAG, "bitmap is null");
            return;
        } else {
            int height = bitmap.getHeight();
            int width = bitmap.getWidth();

            long startTime = System.currentTimeMillis();
            Matrix matrix = new Matrix();
            matrix.preScale(0.5f, 0.5f);

            final Bitmap bitmapRoi = Bitmap.createBitmap(bitmap, (width-height)/2, height/6,
                    height,(int)(height * 0.635f), matrix, false);

            final int b1width = bitmapRoi.getWidth();
            final int b1height = bitmapRoi.getHeight();
            List<VisionDetRet> faceList = mFaceDet.detect(bitmapRoi);
            long  endTime = System.currentTimeMillis();
            Log.d(TAG, " face Time cost: " + String.valueOf((endTime - startTime)) + " ms");

            if(faceList.size() <= 0) return;
            int faceL = 0,faceT = 0,faceW = 0,faceH= 0;
            for(int i= 0; i< faceList.size();i++)
            {
                Log.d(TAG, " face :"+faceList.get(i).toString() );
                faceW = faceList.get(i).getRight()- faceList.get(i).getLeft();
                faceH = faceList.get(i).getBottom()- faceList.get(i).getTop();
                faceL = faceList.get(i).getLeft() - (int)( 0.4f*faceW);
                faceT = faceList.get(i).getTop() - (int)( 0.4*faceH);

                if(faceL < 0 ) faceL = 0;
                if(faceT < 0 ) faceT = 0;

                if(faceL + faceW*1.8f >bitmapRoi.getWidth())
                    faceW = b1width-faceL;
                else
                    faceW = (int)(faceW*1.8f);

                if(faceT + faceH*1.8f >bitmapRoi.getHeight())
                    faceH = b1height-faceT;
                else
                    faceH = (int)(faceH*1.8f);
            }

            //FACE should be right positon align
            if(faceL < 0.617f*b1width ||  faceW < 0.25f*bitmapRoi.getWidth())
            {
                return;
            }

            //name xywh 0.195,0.082,0.433,0.180
            //id        0.34,0.77,0.607,0.180
             startTime = System.currentTimeMillis();
            //=============== test begin
            Mat rgbaInner = new Mat();
            Bitmap bit_er = Bitmap.createBitmap(bitmapRoi, 0, 0, bitmapRoi.getWidth(), bitmapRoi.getHeight());
            Utils.bitmapToMat(bitmapRoi,rgbaInner);
            Mat grayImage = new Mat();
            //Mat cannyimg  = new Mat();
            Mat binary  = new Mat();
            Mat erode  = new Mat();
            Imgproc.cvtColor(rgbaInner, grayImage, Imgproc.COLOR_RGB2GRAY, 4);
            Imgproc.threshold(grayImage, binary, thresh, 200, Imgproc.THRESH_BINARY);
            //Imgproc.adaptiveThreshold(grayImage, cannyimg, 255, Imgproc.THRESH_OTSU,Imgproc.THRESH_BINARY, 3, 0);
            Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(13,13));
            Imgproc.erode(binary, erode, erodeElement);

            Utils.matToBitmap(erode,bit_er);
            if(debug)savebitmap(bit_er,"erode.jpg");

            org.opencv.core.Rect roi = new org.opencv.core.Rect(0, 0, (int)(erode.width()*0.5f), erode.height());
            Mat meancheck  = new Mat(erode,roi);
            Scalar meanvalue = mean(meancheck);

            thresh +=5*((int)meanvalue.val[0] - 155)/10 ;
            Log.d(TAG, " meanvalue :"+meanvalue.val[0] +" thresh:"+thresh);

            List<MatOfPoint> contours=new ArrayList<>();
            Imgproc.findContours(erode,contours,new Mat(),Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE);

            if(contours.size() <= 0) return;
            MatOfPoint temp_contour =  contours.get(0);

            org.opencv.core.Rect idid = new org.opencv.core.Rect(0,0,0,0);
            org.opencv.core.Rect idname = new org.opencv.core.Rect(0,0,0,0);
            for (int idx = 0; idx < contours.size(); idx++) {
                temp_contour = contours.get(idx);
                MatOfPoint2f point2f=new MatOfPoint2f(temp_contour.toArray());

                RotatedRect RoRect = Imgproc.minAreaRect(point2f);
                org.opencv.core.Rect rect = RoRect.boundingRect();
                if(debug)Log.d(TAG, " Rect :"+rect.toString() );

                //may three more chinese charactor or two chinese charactor
                if(RoRect.center.x > b1width*0.175  && RoRect.center.y <0.25*b1height &&
                    rect.height < b1height*0.12 && rect.height > b1height*0.087) // 30 ~40 pix
                {
                    // three or more charactor chinese name
                    if(rect.width>rect.height*1.5 &&(idname.width ==  0 || idname.y > rect.y))
                        idname = rect;

                    // two charactor they are seperate more distance need linkup
                    if(Math.abs(rect.width -rect.height) < 0.1f*rect.height)
                    {
                        if(idname.width ==  0)
                            idname = rect;
                        else if(Math.abs(idname.width -idname.height) < 0.1*idname.height)
                        {
                            if(RoRect.center.y > idname.y &&
                                RoRect.center.y < idname.y+idname.height &&
                                Math.abs(RoRect.center.x -idname.x) < 2.5*rect.height)
                            {
                                if(idname.x < rect.x)
                                    idname.width = rect.x+rect.width - idname.x;
                                else
                                {
                                    idname.width = idname.x + idname.width - rect.x;
                                    idname.x = rect.x;
                                    idname.y = rect.y;
                                }
                            }
                        }
                    }
                }
                // bottom  right thin and long for number
                if(rect.y> b1height * 0.770 && rect.x > b1width * 0.340&&
                    rect.width > 0.5*b1width &&
                    rect.height < 0.12*b1height && rect.height > 0.064f*b1height)
                    idid = rect;
            }
            endTime = System.currentTimeMillis();
            Log.d(TAG, "id pic handle Time cost: " + String.valueOf((endTime - startTime)) + " ms");

            if(idid.width == 0) return;
            Bitmap bit_id = Bitmap.createBitmap(bitmapRoi, idid.x, idid.y, idid.width, idid.height);
            //bit_id = gray2Binary(bit_id);
            startTime = System.currentTimeMillis();
            String id = doOcr(bit_id);
            Log.d(TAG, "id string is :"+id);
            endTime = System.currentTimeMillis();
            Log.d(TAG, "id ocr Time cost: " + String.valueOf((endTime - startTime)) + " ms");

            if (id.length() != 18)
                return;
            if(debug)savebitmap(bit_id,"id.jpg");

            if(idname.width == 0)return;

            startTime = System.currentTimeMillis();
            Bitmap bitname = Bitmap.createBitmap(bitmapRoi, idname.x, idname.y, idname.width, idname.height);
            String name = doNameOcr(bitname);
            endTime = System.currentTimeMillis();
            Log.d(TAG, "name ocr Time cost: " + String.valueOf((endTime - startTime)) + " ms");

            if(debug)savebitmap(bitname,"name.jpg");
            Log.d(TAG, "name string is :"+name);
            //=============== test end

            //if (name.length() > 4)
            {
                Intent i = new Intent();
                i.putExtra("name", name);
                i.putExtra("id", id);
                setResult(RESULT_OK, i);

                Bitmap bit_face = Bitmap.createBitmap(bitmapRoi, faceL, faceT, faceW, faceH);
                savebitmap(bit_face,"face.jpg");

                onBackPressed();
            }
        }

    }

    public void savebitmap(Bitmap bp,String name)
    {
        File filePic;
        String savePath = Constants.getDLibDirectoryPath() + "/IDimage/";
        try {
            filePic = new File(savePath + name);
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            bp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return ;
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    //super.onManagerConnected(status);
                } break;
            }
        }
    };
}
