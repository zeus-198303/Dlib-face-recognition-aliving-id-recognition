package com.google.android.mgc.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class PreviewBorderView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String DEFAULT_TIPS_TEXT = "请将身份证照片面置于框内扫描，并尽量对齐边框";
    private static final int DEFAULT_TIPS_TEXT_SIZE = 18;
    private static final int DEFAULT_TIPS_TEXT_COLOR = Color.GREEN;
    private int mScreenH;
    private int mScreenW;
    private Canvas mCanvas;
    private Paint mPaint;
    private Paint mPaintG;
    private Paint mPaintLine;
    private SurfaceHolder mHolder;
    private Thread mThread;
    /**
     * 自定义属性
     */
    private float tipTextSize;
    private int tipTextColor;
    private String tipText;

    public PreviewBorderView(Context context) {
        this(context, null);
    }

    public PreviewBorderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewBorderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        init();
    }

    /**
     *
     * @param context Context
     * @param attrs   AttributeSet
     */
    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreviewBorderView);
        try {
            tipTextSize = a.getDimension(R.styleable.PreviewBorderView_tipTextSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_TIPS_TEXT_SIZE, getResources().getDisplayMetrics()));
            tipTextColor = a.getColor(R.styleable.PreviewBorderView_tipTextColor, DEFAULT_TIPS_TEXT_COLOR);
            tipText = a.getString(R.styleable.PreviewBorderView_tipText);
            if (tipText == null) {
                tipText = DEFAULT_TIPS_TEXT;
            }
        } finally {
            a.recycle();
        }


    }

    private void init() {
        this.mHolder = getHolder();
        this.mHolder.addCallback(this);
        this.mHolder.setFormat(PixelFormat.TRANSPARENT);
        setZOrderOnTop(true);
        setZOrderMediaOverlay(true);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setColor(Color.WHITE);
        this.mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        this.mPaintLine = new Paint();
        this.mPaintLine.setColor(Color.WHITE);
        this.mPaintLine.setStrokeWidth(3.0F);
        this.mPaintG = new Paint();
        this.mPaintG.setColor(Color.GREEN);
        this.mPaintG.setAlpha(50);
        this.mPaintG.setStrokeWidth(3.0F);

        setKeepScreenOn(true);
    }


    private void draw() {
        try {
            this.mCanvas = this.mHolder.lockCanvas();
            this.mCanvas.drawARGB(100, 0, 0, 0);
//            this.mScreenW = (this.mScreenH * 4 / 3);
            Log.e("TAG", "mScreenW:" + mScreenW + " mScreenH:" + mScreenH);
            int idleft = (this.mScreenW  - this.mScreenH) * 1 / 2;
            int idtop =  this.mScreenH * 1 / 6;
            int idright = (this.mScreenW  + this.mScreenH )* 1 / 2 ;
            int idbottom=  this.mScreenH * 1 / 6 +this.mScreenH * 54/85;
            int IDw = idright - idleft;
            int IDh = idbottom - idtop;

            this.mCanvas.drawRect(new RectF(idleft, idtop, idright, idbottom), this.mPaint);

            //name
           // this.mCanvas.drawRect(new RectF(idleft+IDw*195/1000, idtop+IDh*52/631, idleft+IDw*628/1000, idtop+IDh*165/631), this.mPaintG);
            //pic
            //this.mCanvas.drawRect(new RectF(idleft+IDw*617/1000, idtop+IDh*103/631, idleft+IDw*934/1000, idtop+IDh*469/631), this.mPaintG);
            //num
            //this.mCanvas.drawRect(new RectF(idleft+IDw*340/1000, idtop+IDh*485/631,  idleft+IDw*940/1000, idtop+IDh*599/631), this.mPaintG);

            this.mCanvas.drawLine(idleft,  idtop, idleft, idtop + 150, this.mPaintLine);
            this.mCanvas.drawLine(idleft,  idtop, idleft + 150, idtop, this.mPaintLine);
            this.mCanvas.drawLine(idright, idtop, idright, idtop + 150, this.mPaintLine);
            this.mCanvas.drawLine(idright, idtop, idright - 150, idtop, this.mPaintLine);
            this.mCanvas.drawLine(idleft, idbottom, idleft, idbottom - 150, this.mPaintLine);
            this.mCanvas.drawLine(idleft, idbottom, idleft + 150, idbottom, this.mPaintLine);
            this.mCanvas.drawLine(idright, idbottom, idright, idbottom - 150, this.mPaintLine);
            this.mCanvas.drawLine(idright, idbottom, idright - 150, idbottom, this.mPaintLine);

            mPaintLine.setTextSize(tipTextSize);
            mPaintLine.setAntiAlias(true);
            mPaintLine.setDither(true);
            float length = mPaintLine.measureText(tipText);

            this.mCanvas.drawText(tipText, this.mScreenW / 2 - length / 2, this.mScreenH * 1 / 2 - tipTextSize, mPaintLine);
            Log.e("TAG", "left:" + (idleft));
            Log.e("TAG", "top:" + (idtop));
            Log.e("TAG", "right:" + (idright));
            Log.e("TAG", "bottom:" + (idbottom));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (this.mCanvas != null) {
                this.mHolder.unlockCanvasAndPost(this.mCanvas);
            }
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //获得宽高，开启子线程绘图
        this.mScreenW = getWidth();
        this.mScreenH = getHeight();
        this.mThread = new Thread(this);
        this.mThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            mThread.interrupt();
            mThread = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        draw();
    }
}
