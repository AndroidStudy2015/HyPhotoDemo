package com.example.hyphotodemo.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;

import androidx.appcompat.widget.AppCompatImageView;


public class MyPhotoViewNoDoubleTipOK extends AppCompatImageView implements
    ViewTreeObserver.OnGlobalLayoutListener,
    ScaleGestureDetector.OnScaleGestureListener,
    View.OnTouchListener {

    /**
     * 确保只是执行一次的flag
     */
    private boolean mOnce;

    /**
     * 初始化时候的缩放值
     */
    private float mInitScale;
    /**
     * 缩放的最小值
     */
    private float mMinScale;
    /**
     * 双击放大时，达到的缩放值
     */
    private float mMidScale;
    /**
     * 缩放的最大值
     */
    private float mMaxScale;
    private Matrix mScaleMatrix;
    private ScaleGestureDetector scaleGestureDetector;

    private boolean isCheckLeftAndRight;
    private boolean isCheckTopAndBottom;

    //    --------------------------自由移动-------------------------------------

    /**
     * 上一次多点触控的数量（防止图片跳跃式改变）
     */
    private int mLastPointCount;

    private float mLastX;
    private float mLastY;


    private int mTouchSlop;
    private boolean isCanDrag;


    public MyPhotoViewNoDoubleTipOK(Context context) {
        this(context, null);

    }

    public MyPhotoViewNoDoubleTipOK(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScaleMatrix = new Matrix();
        super.setScaleType(ScaleType.MATRIX);

        scaleGestureDetector = new ScaleGestureDetector(context, this);
        setOnTouchListener(this);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init(context);
    }

    private void init(Context context) {

    }

    /**
     * ViewTreeObserver.OnGlobalLayoutListener
     * 获取ImageView加载完成图片的时机
     */
    @Override
    public void onGlobalLayout() {
        if (!mOnce) {
            //  得到控件的宽和高
            int width = getWidth();
            int height = getHeight();

            //  得到我们的图片、以及宽和高

            Drawable d = getDrawable();
            if (d == null) {
                return;
            }
            int dw = d.getIntrinsicWidth();
            int dh = d.getIntrinsicHeight();


            // 确定图片缩放值
            float scale = 1.0f;
            if (dw > width && dh < height) {
                scale = width * 1.0f / dw;
                Log.e("ccc", 1 + "===");

            }

            if (dh > height && dw < width) {
                scale = height * 1.0f / dh;
                Log.e("ccc", 2 + "===");

            }

            if ((dw > width && dh > height) || (dw < width && dh < height)) {
                scale = Math.max(width * 1.0f / dw, height * 1.0f / dw);
                Log.e("ccc", 3 + "===");

            }


            Log.e("ccc", width + "width");
            Log.e("ccc", height + "height");
            Log.e("ccc", dw + "dw");
            Log.e("ccc", dh + "dh");
            Log.e("ccc", scale + "scale");
            // 初始化缩放值
            mInitScale = scale;
            mMaxScale = mInitScale * 4;
            mMidScale = mInitScale * 2;
            mMinScale = mInitScale * 0.5f;

            // 移动图片 到控件中心
            int dx = getWidth() / 2 - dw / 2;
            int dy = getHeight() / 2 - dh / 2;


            mScaleMatrix.postTranslate(dx, dy);
            mScaleMatrix.postScale(mInitScale, mInitScale, width / 2, height / 2);
            setImageMatrix(mScaleMatrix);
            mOnce = true;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        float scale = getScale();
        float scaleFactor = scaleGestureDetector.getScaleFactor();
        Log.e("ccc", scaleFactor + "scaleFactor");

        if (getDrawable() == null) {
            return true;
        }

        // 缩放范围控制
        if ((scale < mMaxScale && scaleFactor > 1.0f)
            || (scale > mMinScale && scaleFactor < 1.0f)) {

            if (scale * scaleFactor < mMinScale) {
                scaleFactor = mMinScale / scale;
            }

            if (scale * scaleFactor > mMaxScale) {
                scaleFactor = mMaxScale / scale;
            }

            mScaleMatrix.postScale(
                scaleFactor,
                scaleFactor,
                scaleGestureDetector.getFocusX(),
                scaleGestureDetector.getFocusY()
            );

            checkBorderAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);

        }

        return true;// true
    }

    /**
     * 缩放的时候进行边界控制以及位置位置控制,防止出现白边
     */
    private void checkBorderAndCenterWhenScale() {
        RectF rectF = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();


        if (rectF.width() >= width) {
            if (rectF.left > 0) {
                deltaX = -rectF.left;
            }

            if (rectF.right < width) {
                deltaX = width - rectF.right;
            }
        }

        if (rectF.height() > height) {
            if (rectF.top > 0) {
                deltaY = -rectF.top;
            }

            if (rectF.bottom < height) {
                deltaY = height - rectF.bottom;
            }
        }


        //小于控件宽高，让其居中

        if (rectF.width() < width) {
            deltaX = width / 2f - rectF.right + rectF.width() / 2f;
        }

        if (rectF.height() < height) {
            deltaY = height / 2f - rectF.bottom + rectF.height() / 2f;
        }


        mScaleMatrix.postTranslate(deltaX, deltaY);
    }


    /**
     * 获取图片放大缩小以后的宽和高，以及l,t,r,b
     *
     * @return
     */
    private RectF getMatrixRectF() {
        Matrix matrix = mScaleMatrix;
        RectF rectF = new RectF();

        Drawable d = getDrawable();
        if (d != null) {
            rectF.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(rectF);
        }

        return rectF;

    }


    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return true;// 这里必须是true,如果是false，将不会触发onScale的逻辑
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        scaleGestureDetector.onTouchEvent(motionEvent);
        //        移动图片
        float x = 0;
        float y = 0;
        int pointerCount = motionEvent.getPointerCount();
        // 遍历所有点 ，得到中心点位置
        for (int i = 0; i < pointerCount; i++) {
            x += motionEvent.getX(i);
            y += motionEvent.getY(i);
        }
        x /= pointerCount;
        y /= pointerCount;
        // 如果手指数量发生了改变，那么记录一下，防止一根手指离开屏幕时会发生突变
        if (mLastPointCount != pointerCount) {
            isCanDrag = false;//手指数量改变了，不要随便去移动，会突变
            mLastX = x;
            mLastY = y;
        }
        mLastPointCount = pointerCount;

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx = x - mLastX;
                float dy = y - mLastY;

                if (!isCanDrag) {
                    isCanDrag = isMoveAction(dx, dy);
                }
                if (isCanDrag) {

                    RectF rectF = getMatrixRectF();

                    if (getDrawable() != null) {

                        isCheckLeftAndRight = isCheckTopAndBottom = true;
                        //   如果宽度小于控件宽度，禁止横向移动x
                        if (rectF.width() < getWidth()) {
                            isCheckLeftAndRight = false;
                            dx = 0;
                        }

                        if (rectF.height() < getHeight()) {
                            isCheckTopAndBottom = false;
                            dy = 0;
                        }
                        mScaleMatrix.postTranslate(dx, dy);
                        checkBorderAndCenterWhenTranslate();
                        setImageMatrix(mScaleMatrix);
                    }


                }


                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLastPointCount = 0;
                break;

}
        return true;// 必须是true
            }

    /**
     * 当移动时，进行边界检查
     */
    private void checkBorderAndCenterWhenTranslate() {


        RectF rectF = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        if (rectF.top > 0 && isCheckTopAndBottom) {
            deltaY = -rectF.top;
        }


        if (rectF.bottom < height && isCheckTopAndBottom) {
            deltaY = height - rectF.bottom;
        }


        if (rectF.left > 0 && isCheckLeftAndRight) {
            deltaX = -rectF.left;
        }


        if (rectF.right < width && isCheckLeftAndRight) {
            deltaX = width - rectF.right;
        }
        mScaleMatrix.postTranslate(deltaX, deltaY);
    }

    /**
     * 是不是足以能够触发move
     *
     * @param dx
     * @param dy
     * @return
     */
    private boolean isMoveAction(float dx, float dy) {
        return Math.sqrt(dx * dx + dy * dy) > mTouchSlop;

    }


    /**
     * 获取当前图片的缩放值
     *
     * @return
     */
    private float getScale() {
        float[] values = new float[9];
        mScaleMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }


}
