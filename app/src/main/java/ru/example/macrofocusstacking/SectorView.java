package ru.example.macrofocusstacking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SectorView extends View {
    private Paint paint;
    private Paint paintBackground;
    private RectF oval;
    private int colorBackground;
    private float sweepAngle;

    public SectorView(Context context) {
        super(context);
        init();
    }

    public SectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SectorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(24);
        paint.setColor(Color.parseColor("#ffffff"));

        paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBackground.setStyle(Paint.Style.STROKE);
        paintBackground.setStrokeWidth(24);
        paintBackground.setColor(Color.parseColor("#1f1f1f"));

        sweepAngle = 0.0f;

        oval = new RectF();
    }

    public void setSector(float sweepAngle) {
        paintBackground.setColor(colorBackground);
        this.sweepAngle = sweepAngle;
        invalidate();
    }

    public void invisibleSector() {
        paintBackground.setColor(Color.TRANSPARENT);
        sweepAngle = 0.0f;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.rotate(-90, getWidth() / 2f, getHeight() / 2f);

        oval.set(0 + paint.getStrokeWidth(), 0 + paint.getStrokeWidth(), getWidth() - paint.getStrokeWidth(), getHeight() - paint.getStrokeWidth());

        canvas.drawArc(oval, 0f, sweepAngle, false, paint);

        canvas.drawArc(oval, sweepAngle, 360.0f - sweepAngle, false, paintBackground);
    }
}

