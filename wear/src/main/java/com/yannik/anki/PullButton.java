package com.yannik.anki;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PullButton extends RelativeLayout {

    private final ImageButton icon;
    private final TextView textView;
    private final TextView easeTextView;
    OnClickListener ocl;
    private float homePosition, extendedPosition, homeAlpha = 0.7f, extendedAlpha = 1;
    private int minMovementDistance = 50;
    private Point displaySize = new Point();
    private int exitY = 0;
    private int imageResId = -1;
    private boolean upsideDown;

    public PullButton(Context context) {
        this(context, null);
    }

    public PullButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PullButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.pull_button, this);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        display.getSize(displaySize);


        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PullButton);
        final int N = a.getIndexCount();

        icon = (ImageButton) findViewById(R.id.icon);
        textView = (TextView) findViewById(R.id.textView);
        easeTextView = (TextView) findViewById(R.id.ease_text);


        for (int i = 0; i < N; ++i) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.PullButton_icon:
                    imageResId = a.getResourceId(attr, R.drawable.close_button);
                    break;
                case R.styleable.PullButton_text:
                    String text = a.getString(attr);
                    textView.setText(text);
                    break;
                case R.styleable.PullButton_upsideDown:
                    upsideDown = a.getBoolean(attr, false);
                    break;
                case R.styleable.PullButton_ease_text:
                    easeTextView.setText(a.getString(attr));
                    break;
            }
        }
        a.recycle();


        ViewTreeObserver vto = getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (upsideDown) {
                    textView.setY(0);
                    icon.setY(textView.getHeight());
                    icon.setRotation(180);
                    easeTextView.setY(easeTextView.getY() + textView.getHeight());
                }


                if (upsideDown) {
                    homePosition = 0 - textView.getHeight();
                    extendedPosition = 0;
                    exitY = displaySize.y + 10;
                } else {
                    homePosition = displaySize.y - icon.getHeight();
                    extendedPosition = displaySize.y - getHeight();
                    exitY = -getHeight() - 10;
                }

                setY(homePosition);

                ViewTreeObserver obs = getViewTreeObserver();
                obs.removeOnGlobalLayoutListener(this);
            }
        });


        if (imageResId != -1) {
            icon.setImageResource(imageResId);
        }

        minMovementDistance = displaySize.y * 3 / 4;

        setAlpha(homeAlpha);

        this.animate().setInterpolator(new LinearInterpolator());
        this.setOnTouchListener(new SwipeTouchListener());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    public void moveToRight() {
        setX(displaySize.x / 2 + getWidth() / 2);
    }

    public void moveToLeft() {
        setX(displaySize.x / 2 - getWidth() - getWidth() / 2);
    }

    public void moveToCenter() {
        setX(displaySize.x / 2 - getWidth() / 2);
    }

    public void setOnSwipeListener(OnClickListener ocl) {
        this.ocl = ocl;
    }

    public void setText(String text) {
        textView.setText(text);
    }

    public void show() {
        setY(homePosition);
        setAlpha(homeAlpha);
        setVisibility(View.VISIBLE);
    }

    public void onPull() {
        if (ocl != null) ocl.onClick(PullButton.this);
    }

    class SwipeTouchListener implements OnTouchListener {
        private float yDiff;

        private VelocityTracker mVelocityTracker;

        @Override
        public boolean onTouch(final View v, MotionEvent event) {
            float viewPositionY = v.getY();
            float eventPositionY = event.getRawY();
            if (upsideDown) {
                viewPositionY = displaySize.y - viewPositionY - v.getHeight();
                eventPositionY = displaySize.y - eventPositionY;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    yDiff = eventPositionY - viewPositionY;

                    mVelocityTracker = VelocityTracker.obtain();
                    event.offsetLocation(0, homePosition - viewPositionY);
                    mVelocityTracker.addMovement(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!upsideDown) {
                        v.setY(eventPositionY - yDiff);
                    } else {
                        v.setY(displaySize.y - (eventPositionY - yDiff) - v.getHeight());
                    }
                    event.offsetLocation(0, homePosition - viewPositionY);
                    mVelocityTracker.addMovement(event);

                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    mVelocityTracker.computeCurrentVelocity(1);
                    float yVelocity = mVelocityTracker.getYVelocity();

                    if (viewPositionY < minMovementDistance && yVelocity >= 0) {
                        onPull();
                    } else if (viewPositionY + v.getHeight() < displaySize.y) {
                        v.animate().setStartDelay(0).y(extendedPosition).alpha(extendedAlpha).setListener(null);
                    } else {
                        v.animate().setStartDelay(0).y(homePosition).alpha(homeAlpha).setListener(null);
                    }

                    mVelocityTracker.recycle();
                    break;
            }


            return true;
        }
    }
}
