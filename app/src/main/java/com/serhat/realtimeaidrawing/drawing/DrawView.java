package com.serhat.realtimeaidrawing.drawing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;


import androidx.core.content.ContextCompat;

import com.serhat.realtimeaidrawing.R;
import com.serhat.realtimeaidrawing.listener.DrawingListener;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;


public class DrawView extends View {

    private static final float TOUCH_TOLERANCE = 4;
    private float mX;
    private float mY;
    private Path mPath;
    private Paint mPaint;

    private final ArrayList<Stroke> paths = new ArrayList<>();
    private final ArrayList<Stroke> undonePaths = new ArrayList<>();
    public int currentColour;
    public float strokeWidth;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    Paint background;
    public int backgroundColour;
    private boolean isEraser = false;

    private int containerWidth;
    private int containerHeight;
    float[] mv = new float[9];
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    PointF start = new PointF();
    float currentScale;
    float curX;
    float curY;

    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;

    int mode = NONE;
    float targetX;
    float targetY;
    float targetScale;
    float targetScaleX;
    float targetScaleY;
    float scaleChange;
    float targetRatio;
    boolean isAnimating = false;
    float oldDist = 1f;
    PointF mid = new PointF();

    private final Handler mHandler = new Handler();

    float minScale;
    float maxScale = 8.0f;

    private final GestureDetector gestureDetector;

    public static final int DEFAULT_SCALE_FIT_INSIDE = 0;
    private int defaultScale;
    Rect clipBounds;

    public boolean isZoom = false;

    private DrawingListener drawingListener;


    public DrawView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        initPaints();
        gestureDetector = new GestureDetector(context, new MyGestureDetector());
    }
    public DrawView(Context context, AttributeSet attributes) {
        super(context, attributes);
        initPaints();
        gestureDetector = new GestureDetector(context, new MyGestureDetector());
        defaultScale = DrawView.DEFAULT_SCALE_FIT_INSIDE;
    }

    public boolean isCanvasClear() {
        return paths.isEmpty();
    }

    public void setDrawingListener(DrawingListener listener) {
        this.drawingListener = listener;
    }

    private void notifyDrawingFinished(String base64Image) {
        if (drawingListener != null) {
            drawingListener.onDrawingFinished(base64Image);
        }
    }

    public String captureCanvasAsBase64() {
        if (mBitmap != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return "data:image/png;base64," + Base64.encodeToString(byteArray,
                    Base64.DEFAULT);
        }
        return null;
    }

    public void setUpDrawing() {
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setAlpha(0xff);
        backgroundColour = ContextCompat.getColor(getContext(), R.color.paint_back_color);
    }

    private void initPaints() {
        background = new Paint();
        setUpDrawing();
    }


    //    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int size = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
//        int newMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
//        super.onMeasure(newMeasureSpec, newMeasureSpec);
//    }
    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        containerWidth = width;
        containerHeight = height;

        if (mBitmap != null) {
            mCanvas = new Canvas(mBitmap);
            int imgHeight = mBitmap.getHeight();
            int imgWidth = mBitmap.getWidth();

            float scale;
            int initX = 0;
            int initY = 0;

            if (defaultScale == DrawView.DEFAULT_SCALE_FIT_INSIDE) {
                if (imgWidth > containerWidth) {
                    scale = (float) containerWidth / imgWidth;
                    float newHeight = imgHeight * scale;
                    initY = (containerHeight - (int) newHeight) / 2;

                    matrix.setScale(scale, scale);
                    matrix.postTranslate(0, initY);
                } else {
                    scale = (float) containerHeight / imgHeight;
                    float newWidth = imgWidth * scale;
                    initX = (containerWidth - (int) newWidth) / 2;

                    matrix.setScale(scale, scale);
                    matrix.postTranslate(initX, 0);
                }

                curX = initX;
                curY = initY;

                currentScale = scale;
                minScale = scale;
            } else {
                if (imgWidth > containerWidth) {
                    initY = (containerHeight - imgHeight) / 2;
                    matrix.postTranslate(0, initY);
                } else {
                    initX = (containerWidth - imgWidth) / 2;
                    matrix.postTranslate(initX, 0);
                }

                curX = initX;
                curY = initY;

                currentScale = 1.0f;
                minScale = 1.0f;
            }


            invalidate();
        }
    }

    public void setUpCanvas(int width, int height) {
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        if (currentColour == 0) {
            currentColour = ContextCompat.getColor(getContext(), R.color.pencil_color);
        }
        strokeWidth = 80f;
        fitScaleInitial();
    }

    private void fitScaleInitial() {
        containerWidth = getWidth();
        containerHeight = getHeight();

        if (mBitmap != null) {
            mCanvas = new Canvas(mBitmap);
            int imgHeight = mBitmap.getHeight();
            int imgWidth = mBitmap.getWidth();

            float scale;
            int initX = 0;
            int initY = 0;

            if (imgWidth > containerWidth) {
                scale = (float) containerWidth / imgWidth;
                float newHeight = imgHeight * scale;
                initY = (containerHeight - (int) newHeight) / 2;

                matrix.setScale(scale, scale);
                matrix.postTranslate(0, initY);
            } else {
                scale = (float) containerHeight / imgHeight;
                float newWidth = imgWidth * scale;
                initX = (containerWidth - (int) newWidth) / 2;

                matrix.setScale(scale, scale);
                matrix.postTranslate(initX, 0);
            }

            curX = initX;
            curY = initY;

            currentScale = scale;
            minScale = scale;
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        if (mBitmap != null) {
            if (mCanvas == null) {
                mCanvas = new Canvas(mBitmap);
            }
        }
        clipBounds = canvas.getClipBounds();
        canvas.drawBitmap(mBitmap, matrix, background);
        canvas.concat(matrix);

        mCanvas.drawColor(backgroundColour);
        for (Stroke currentPath : paths) {
            if (currentPath.isErasePath) {
                mPaint.setColor(backgroundColour);
            } else {
                mPaint.setColor(currentPath.colour);
            }
            mPaint.setStrokeWidth(currentPath.strokeWidth);
            mCanvas.drawPath(currentPath.path, mPaint);
            invalidate();
        }

        canvas.restore();
        invalidate();
    }

    private void checkImageConstraints() {
        if (mBitmap == null) {
            return;
        }

        float[] mvals = new float[9];
        matrix.getValues(mvals);

        currentScale = mvals[0];

        if (currentScale < minScale) {
            float deltaScale = minScale / currentScale;
            float px = (float) containerWidth / 2;
            float py = (float) containerHeight / 2;
            matrix.postScale(deltaScale, deltaScale, px, py);
            invalidate();
        }

        matrix.getValues(mvals);
        currentScale = mvals[0];
        curX = mvals[2];
        curY = mvals[5];

        int rangeLimitX = containerWidth - (int) (mBitmap.getWidth() * currentScale);
        int rangeLimitY = containerHeight - (int) (mBitmap.getHeight() * currentScale);


        boolean toMoveX = false;
        boolean toMoveY = false;

        if (rangeLimitX < 0) {
            if (curX > 0) {
                targetX = 0;
                toMoveX = true;
            } else if (curX < rangeLimitX) {
                targetX = rangeLimitX;
                toMoveX = true;
            }
        } else {
            targetX = (float) rangeLimitX / 2;
            toMoveX = true;
        }

        if (rangeLimitY < 0) {
            if (curY > 0) {
                targetY = 0;
                toMoveY = true;
            } else if (curY < rangeLimitY) {
                targetY = rangeLimitY;
                toMoveY = true;
            }
        } else {
            targetY = (float) rangeLimitY / 2;
            toMoveY = true;
        }

        if (toMoveX || toMoveY) {
            if (!toMoveY) {
                targetY = curY;
            }
            if (!toMoveX) {
                targetX = curX;
            }

            isAnimating = true;
            mHandler.removeCallbacks(mUpdateImagePositionTask);
            mHandler.postDelayed(mUpdateImagePositionTask, 100);
        }
    }

    public void changeBackground(int colour) {
        backgroundColour = colour;
        mCanvas.drawColor(backgroundColour);
        for (Stroke currentPath : paths) {
            if (currentPath.isErasePath) {
                mPaint.setColor(backgroundColour);
            } else {
                mPaint.setColor(currentPath.colour);
            }
            mPaint.setStrokeWidth(currentPath.strokeWidth);
            mCanvas.drawPath(currentPath.path, mPaint);
        }
        invalidate();
    }

    private void touchStart(float x, float y) {
        undonePaths.clear();
        mPath = new Path();

        Stroke currentPath;
        if (isEraser) {
            currentPath = new Stroke(backgroundColour, strokeWidth, mPath, true);
        } else {
            currentPath = new Stroke(currentColour, strokeWidth, mPath, false);
        }
        paths.add(currentPath);

        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        mPath.lineTo(mX, mY);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        matrix.getValues(mv);
        float touchX = (event.getX() * (1 / mv[4]) - (mv[2] / mv[4]));
        float touchY = (event.getY() * (1 / mv[4]) - (mv[5] / mv[4]));

        if (!isZoom) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStart(touchX, touchY);
                    break;
                case MotionEvent.ACTION_MOVE:
                    touchMove(touchX, touchY);
                    break;
                case MotionEvent.ACTION_UP:
                    touchUp();
                    notifyDrawingFinished(captureCanvasAsBase64());
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    break;
                default:
                    return false;
            }
            invalidate();
        } else {
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            }

            if (isAnimating) {
                return true;
            }

            float[] mvals = new float[9];
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    if (!isAnimating) {
                        savedMatrix.set(matrix);
                        start.set(event.getX(), event.getY());
                        mode = DRAG;
                    }
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = spacing(event);
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix);
                        midPoint(mid, event);
                        mode = ZOOM;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;

                    matrix.getValues(mvals);
                    curX = mvals[2];
                    curY = mvals[5];
                    currentScale = mvals[0];

                    if (!isAnimating) {
                        checkImageConstraints();
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG && !isAnimating) {
                        matrix.set(savedMatrix);
                        float diffX = event.getX() - start.x;
                        float diffY = event.getY() - start.y;

                        matrix.postTranslate(diffX, diffY);

                        matrix.getValues(mvals);
                        curX = mvals[2];
                        curY = mvals[5];
                        currentScale = mvals[0];
                    } else if (mode == ZOOM && !isAnimating) {
                        float newDist = spacing(event);
                        if (newDist > 10f) {
                            matrix.set(savedMatrix);
                            float scale = newDist / oldDist;
                            matrix.getValues(mvals);
                            currentScale = mvals[0];

                            if (currentScale * scale <= minScale) {
                                matrix.postScale(minScale / currentScale,
                                        minScale / currentScale, mid.x, mid.y);
                            } else if (currentScale * scale >= maxScale) {
                                matrix.postScale(maxScale / currentScale,
                                        maxScale / currentScale, mid.x, mid.y);
                            } else {
                                matrix.postScale(scale, scale, mid.x, mid.y);
                            }


                            matrix.getValues(mvals);
                            curX = mvals[2];
                            curY = mvals[5];
                            currentScale = mvals[0];
                        }
                    }

                    break;
            }
        }
        invalidate();
        return true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    public void setColour(int colour) {
        currentColour = colour;
    }

    public void setStrokeWidth(float width) {
        strokeWidth = width;
    }

    public void clearCanvas() {
        if (!paths.isEmpty()) {
            paths.clear();
        }
        invalidate();
    }

    public void undo() {
        if (!paths.isEmpty()) {
            undonePaths.add(paths.remove(paths.size() - 1));
        } else {
            Toast.makeText(getContext(), "Cant Undo", Toast.LENGTH_SHORT).show();
        }
        invalidate();
    }

    public void redo() {
        if (!undonePaths.isEmpty()) {
            paths.add(undonePaths.remove(undonePaths.size() - 1));
        } else {
            Toast.makeText(getContext(), "Cant Redo", Toast.LENGTH_SHORT).show();
        }
        invalidate();
    }

    public Bitmap save() {
        return mBitmap;
    }

    public void setEraser(boolean eraser) {
        isEraser = eraser;
    }

    private final Runnable mUpdateImagePositionTask = new Runnable() {
        public void run() {
            float[] mvals;

            if (Math.abs(targetX - curX) < 5 && Math.abs(targetY - curY) < 5) {
                isAnimating = false;
                mHandler.removeCallbacks(mUpdateImagePositionTask);

                mvals = new float[9];
                matrix.getValues(mvals);

                currentScale = mvals[0];
                curX = mvals[2];
                curY = mvals[5];

                float diffX = (targetX - curX);
                float diffY = (targetY - curY);

                matrix.postTranslate(diffX, diffY);
            } else {
                isAnimating = true;
                mvals = new float[9];
                matrix.getValues(mvals);

                currentScale = mvals[0];
                curX = mvals[2];
                curY = mvals[5];

                float diffX = (targetX - curX) * 0.3f;
                float diffY = (targetY - curY) * 0.3f;

                matrix.postTranslate(diffX, diffY);
                mHandler.postDelayed(this, 25);
            }

            invalidate();
        }
    };

    private final Runnable mUpdateImageScale = new Runnable() {
        public void run() {
            float transitionalRatio = targetScale / currentScale;
            float dx;
            if (Math.abs(transitionalRatio - 1) > 0.05) {
                isAnimating = true;
                if (targetScale > currentScale) {
                    dx = transitionalRatio - 1;
                    scaleChange = 1 + dx * 0.2f;

                    currentScale *= scaleChange;

                    if (currentScale > targetScale) {
                        currentScale = currentScale / scaleChange;
                        scaleChange = 1;
                    }
                } else {
                    dx = 1 - transitionalRatio;
                    scaleChange = 1 - dx * 0.5f;
                    currentScale *= scaleChange;

                    if (currentScale < targetScale) {
                        currentScale = currentScale / scaleChange;
                        scaleChange = 1;
                    }
                }


                if (scaleChange != 1) {
                    matrix.postScale(scaleChange, scaleChange, targetScaleX, targetScaleY);
                    mHandler.postDelayed(mUpdateImageScale, 15);
                    invalidate();
                } else {
                    isAnimating = false;
                    scaleChange = 1;
                    matrix.postScale(targetScale / currentScale,
                            targetScale / currentScale, targetScaleX, targetScaleY);
                    currentScale = targetScale;
                    mHandler.removeCallbacks(mUpdateImageScale);
                    invalidate();
                    checkImageConstraints();
                }
            } else {
                isAnimating = false;
                scaleChange = 1;
                matrix.postScale(targetScale / currentScale,
                        targetScale / currentScale, targetScaleX, targetScaleY);
                currentScale = targetScale;
                mHandler.removeCallbacks(mUpdateImageScale);
                invalidate();
                checkImageConstraints();
            }
        }
    };

    class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent event) {
            if (isAnimating) {
                return true;
            }

            scaleChange = 1;
            isAnimating = true;
            targetScaleX = event.getX();
            targetScaleY = event.getY();

            if (Math.abs(currentScale - maxScale) > 0.1) {
                targetScale = maxScale;
            } else {
                targetScale = minScale;
            }
            targetRatio = targetScale / currentScale;
            mHandler.removeCallbacks(mUpdateImageScale);
            mHandler.post(mUpdateImageScale);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }
    }
}