package com.manyBeats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class SimpleBeatVisualizer extends View
{
	Paint paint = new Paint();
	private float _msg = 1;

	public SimpleBeatVisualizer(Context context)
	{
		super(context);
	}

	public SimpleBeatVisualizer(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public SimpleBeatVisualizer(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	@Override
	public void onDraw(Canvas canvas)
	{
		int alpha = (int) (255 * _msg);
		int ar = Color.argb(alpha, 255, 255, 255);
		float w = canvas.getWidth();
		float h = canvas.getHeight();
		paint.setColor(ar);
		canvas.drawRect(new Rect(0, 0, (int) w, (int) h), paint);
	}

	public void update(float msg)
	{
		_msg = msg;
		invalidate();
	}
}
