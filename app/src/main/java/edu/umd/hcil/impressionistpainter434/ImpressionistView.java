package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.view.VelocityTracker;
import java.text.MessageFormat;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();
    private Bitmap _imageViewBitmap;
    private VelocityTracker velocityTracker = null;
    private Path path = new Path();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;
    private MyColor col = MyColor.NORMAL;

    protected enum MyColor {
        NORMAL, DARK, BRIGHT
    }

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){

        _imageView = imageView;
        _imageViewBitmap = _imageView.getDrawingCache();
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        if(_offScreenCanvas != null) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
            invalidate();
        }
    }

    public void setColorMod(MyColor in) {
        col = in;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
        if (_imageView != null) {
            _imageViewBitmap = _imageView.getDrawingCache();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        float curTouchX = motionEvent.getX();
        float curTouchY = motionEvent.getY();
        int curTouchXRounded = (int) curTouchX;
        int curTouchYRounded = (int) curTouchY;
        int ptr = motionEvent.getPointerId(motionEvent.getActionIndex());


        if (curTouchXRounded < 0) curTouchXRounded = 0;
        if (curTouchYRounded < 0) curTouchYRounded = 0;
        if (curTouchXRounded >= _imageViewBitmap.getWidth()) curTouchXRounded = _imageViewBitmap.getWidth() - 1;
        if (curTouchYRounded >= _imageViewBitmap.getHeight()) curTouchYRounded = _imageViewBitmap.getHeight() - 1;
        int pixel = _imageViewBitmap.getPixel(curTouchXRounded, curTouchYRounded);
        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                }
                else {
                    velocityTracker.clear();
                }
                velocityTracker.addMovement(motionEvent);
                break;
            case MotionEvent.ACTION_MOVE:

                switch(col) {
                    case NORMAL:
                        _paint.setColor(pixel);
                        break;
                    case BRIGHT:
                        float[] hsv = new float[3];
                        int color = pixel;
                        Color.colorToHSV(color, hsv);
                        hsv[2] *= 2f; // value component
                        color = Color.HSVToColor(hsv);

                        _paint.setColor(color);
                        break;
                    case DARK:
                        hsv = new float[3];
                        color = pixel;
                        Color.colorToHSV(color, hsv);
                        hsv[2] *= .8f; // value component
                        color = Color.HSVToColor(hsv);
                        _paint.setColor(color);
                        break;
                }

                velocityTracker.addMovement(motionEvent);
                velocityTracker.computeCurrentVelocity(1000);
                double velocity = calculateVelocity(ptr);
                float brushRad = calculateBrushSize(velocity);
                int historySize = motionEvent.getHistorySize();

                for (int i = 0; i < historySize; i++) {
                    float touchX = motionEvent.getHistoricalX(i);
                    float touchY = motionEvent.getHistoricalY(i);

                    if (_brushType == _brushType.Square) {
                        switch(col) {
                            case NORMAL:
                                _paint.setColor(pixel);
                                break;
                            case BRIGHT:
                                float[] hsv = new float[3];
                                int color = pixel;
                                Color.colorToHSV(color, hsv);
                                hsv[2] *= 2f; // value component
                                color = Color.HSVToColor(hsv);

                                _paint.setColor(color);
                                break;
                            case DARK:
                                hsv = new float[3];
                                color = pixel;
                                Color.colorToHSV(color, hsv);
                                hsv[2] *= .8f; // value component
                                color = Color.HSVToColor(hsv);
                                _paint.setColor(color);
                                break;
                        }
                        _offScreenCanvas.drawRect(touchX, touchY, touchX + brushRad, touchY + brushRad, _paint);
                    } else if (_brushType == _brushType.Circle) {
                        switch(col) {
                            case NORMAL:
                                _paint.setColor(pixel);
                                break;
                            case BRIGHT:
                                float[] hsv = new float[3];
                                int color = pixel;
                                Color.colorToHSV(color, hsv);
                                hsv[2] *= 2f; // value component
                                color = Color.HSVToColor(hsv);

                                _paint.setColor(color);
                                break;
                            case DARK:
                                hsv = new float[3];
                                color = pixel;
                                Color.colorToHSV(color, hsv);
                                hsv[2] *= .8f; // value component
                                color = Color.HSVToColor(hsv);
                                _paint.setColor(color);
                                break;
                        }
                        _offScreenCanvas.drawCircle(touchX, touchY, brushRad, _paint);
                    } else if (_brushType == _brushType.CircleSplatter) {
                        int dotsToDrawAtATime = 20;

                        Random r = new Random();
                        for (int j = 0; j < dotsToDrawAtATime; j++){

                            int x = (int) (touchX + r.nextGaussian()*brushRad);
                            int y = (int) (touchY + r.nextGaussian()*brushRad);
                            switch(col) {
                                case NORMAL:
                                    _paint.setColor(pixel);
                                    break;
                                case BRIGHT:
                                    float[] hsv = new float[3];
                                    int color = pixel;
                                    Color.colorToHSV(color, hsv);
                                    hsv[2] *= 2f; // value component
                                    color = Color.HSVToColor(hsv);

                                    _paint.setColor(color);
                                    break;
                                case DARK:
                                    hsv = new float[3];
                                    color = pixel;
                                    Color.colorToHSV(color, hsv);
                                    hsv[2] *= .8f; // value component
                                    color = Color.HSVToColor(hsv);
                                    _paint.setColor(color);
                                    break;
                            }
                            _offScreenCanvas.drawPoint(x, y, _paint);
                        }

                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                velocityTracker.recycle();
                velocityTracker = null;
                break;
        }
        invalidate();
        return true;
    }


    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }

    public int modColor (int col, int val) {
            int num = Integer.valueOf(String.valueOf(col), 16);
            int r = (num >> 16) + val;
            int b = ((num >> 8) & 0x00FF) + val;
            int g = (num & 0x0000FF) + val;
            int newColor = g | (b << 8) | (r << 16);
            return newColor;

    }

    public Bitmap get_offScreenBitmap() {
        return _offScreenBitmap;
    }

    public double calculateVelocity (int pointerId) {
        float xVelocity = VelocityTrackerCompat.getXVelocity(velocityTracker, pointerId);
        float yVelocity = VelocityTrackerCompat.getYVelocity(velocityTracker, pointerId);
        double zVelocity = Math.sqrt(Math.pow(xVelocity, 2) + Math.pow(yVelocity, 2));
        Log.d("VELOCITY", "Velocity = " + zVelocity);
        if(zVelocity > 3000) {
            return 3000;
        } else {
            return zVelocity;
        }
    }

    public float calculateBrushSize(double speed) {
        float brushSize = Math.round((50 * speed) / 3000);
        if(brushSize < 5) {
            return 5;
        } else {
            return brushSize;
        }
    }
}
