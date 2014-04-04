package com.sutd.group3.roomfinder;

import static com.sutd.group3.roomfinder.TouchImageView.State.ANIMATE_ZOOM;
import static com.sutd.group3.roomfinder.TouchImageView.State.DRAG;
import static com.sutd.group3.roomfinder.TouchImageView.State.FLING;
import static com.sutd.group3.roomfinder.TouchImageView.State.NONE;
import static com.sutd.group3.roomfinder.TouchImageView.State.ZOOM;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Scroller;

public class TouchImageView extends ImageView {

	private static final float SUPER_MIN_MULTIPLIER = .75f;
	private static final float SUPER_MAX_MULTIPLIER = 1.25f;

	private float normalizedScale;
	private Matrix matrix, prevMatrix;

	public static enum State {
		NONE, DRAG, ZOOM, FLING, ANIMATE_ZOOM
	};

	private State state;

	private float minScale;
	private float maxScale;
	private float superMinScale;
	private float superMaxScale;
	private float[] m;

	private Context context;
	private Fling fling;
	private int viewWidth, viewHeight, prevViewWidth, prevViewHeight;
	private float matchViewWidth, matchViewHeight, prevMatchViewWidth,
			prevMatchViewHeight;

	private ScaleGestureDetector mScaleDetector;
	private GestureDetector mGestureDetector;

	public TouchImageView(Context context) {
		super(context);
		sharedConstructing(context);
	}

	public TouchImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		sharedConstructing(context);
	}

	public TouchImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		sharedConstructing(context);
	}

	private void sharedConstructing(Context context) {
		super.setClickable(true);
		this.context = context;
		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		mGestureDetector = new GestureDetector(context, new GestureListener());
		matrix = new Matrix();
		prevMatrix = new Matrix();
		m = new float[9];
		normalizedScale = 1;
		minScale = 1;
		maxScale = 6;
		superMinScale = SUPER_MIN_MULTIPLIER * minScale;
		superMaxScale = SUPER_MAX_MULTIPLIER * maxScale;
		setImageMatrix(matrix);
		setScaleType(ScaleType.MATRIX);
		setState(NONE);
		setOnTouchListener(new TouchImageViewListener());
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putParcelable("instanceState", super.onSaveInstanceState());
		bundle.putFloat("saveScale", normalizedScale);
		bundle.putFloat("matchViewHeight", matchViewHeight);
		bundle.putFloat("matchViewWidth", matchViewWidth);
		bundle.putInt("viewWidth", viewWidth);
		bundle.putInt("viewHeight", viewHeight);
		matrix.getValues(m);
		bundle.putFloatArray("matrix", m);
		return bundle;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		if (state instanceof Bundle) {
			Bundle bundle = (Bundle) state;
			normalizedScale = bundle.getFloat("saveScale");
			m = bundle.getFloatArray("matrix");
			prevMatrix.setValues(m);
			prevMatchViewHeight = bundle.getFloat("matchViewHeight");
			prevMatchViewWidth = bundle.getFloat("matchViewWidth");
			prevViewHeight = bundle.getInt("viewHeight");
			prevViewWidth = bundle.getInt("viewWidth");
			super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
			return;
		}

		super.onRestoreInstanceState(state);
	}

	public void setMaxZoom(float max) {
		maxScale = max;
		superMaxScale = SUPER_MAX_MULTIPLIER * maxScale;
	}

	public void setMinZoom(float min) {
		minScale = min;
		superMinScale = SUPER_MIN_MULTIPLIER * minScale;
	}

	public PointF getDrawablePointFromTouchPoint(float x, float y) {
		return transformCoordTouchToBitmap(x, y, true);
	}

	public PointF getDrawablePointFromTouchPoint(PointF p) {
		return transformCoordTouchToBitmap(p.x, p.y, true);
	}

	private void fixTrans() {
		matrix.getValues(m);
		float transX = m[Matrix.MTRANS_X];
		float transY = m[Matrix.MTRANS_Y];

		float fixTransX = getFixTrans(transX, viewWidth, getImageWidth());
		float fixTransY = getFixTrans(transY, viewHeight, getImageHeight());

		if (fixTransX != 0 || fixTransY != 0) {
			matrix.postTranslate(fixTransX, fixTransY);
		}
	}

	private void fixScaleTrans() {
		fixTrans();
		matrix.getValues(m);
		if (getImageWidth() < viewWidth) {
			m[Matrix.MTRANS_X] = (viewWidth - getImageWidth()) / 2;
		}

		if (getImageHeight() < viewHeight) {
			m[Matrix.MTRANS_Y] = (viewHeight - getImageHeight()) / 2;
		}
		matrix.setValues(m);
	}

	private float getFixTrans(float trans, float viewSize, float contentSize) {
		float minTrans, maxTrans;

		if (contentSize <= viewSize) {
			minTrans = 0;
			maxTrans = viewSize - contentSize;

		} else {
			minTrans = viewSize - contentSize;
			maxTrans = 0;
		}

		if (trans < minTrans)
			return -trans + minTrans;
		if (trans > maxTrans)
			return -trans + maxTrans;
		return 0;
	}

	private float getFixDragTrans(float delta, float viewSize, float contentSize) {
		if (contentSize <= viewSize) {
			return 0;
		}
		return delta;
	}

	private float getImageWidth() {
		return matchViewWidth * normalizedScale;
	}

	private float getImageHeight() {
		return matchViewHeight * normalizedScale;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Drawable drawable = getDrawable();
		if (drawable == null || drawable.getIntrinsicWidth() == 0
				|| drawable.getIntrinsicHeight() == 0) {
			return;
		}

		int drawableWidth = drawable.getIntrinsicWidth();
		int drawableHeight = drawable.getIntrinsicHeight();
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		viewWidth = setViewSize(widthMode, widthSize, drawableWidth);
		viewHeight = setViewSize(heightMode, heightSize, drawableHeight);

		setMeasuredDimension(viewWidth, viewHeight);

		float scaleX = (float) viewWidth / drawableWidth;
		float scaleY = (float) viewHeight / drawableHeight;
		float scale = Math.min(scaleX, scaleY);

		float redundantYSpace = viewHeight - (scale * drawableHeight);
		float redundantXSpace = viewWidth - (scale * drawableWidth);
		matchViewWidth = viewWidth - redundantXSpace;
		matchViewHeight = viewHeight - redundantYSpace;

		if (normalizedScale == 1) {
			matrix.setScale(scale, scale);
			matrix.postTranslate(redundantXSpace / 2, redundantYSpace / 2);

		} else {
			prevMatrix.getValues(m);
			m[Matrix.MSCALE_X] = matchViewWidth / drawableWidth
					* normalizedScale;
			m[Matrix.MSCALE_Y] = matchViewHeight / drawableHeight
					* normalizedScale;

			float transX = m[Matrix.MTRANS_X];
			float transY = m[Matrix.MTRANS_Y];

			float prevActualWidth = prevMatchViewWidth * normalizedScale;
			float actualWidth = getImageWidth();
			translateMatrixAfterRotate(Matrix.MTRANS_X, transX,
					prevActualWidth, actualWidth, prevViewWidth, viewWidth,
					drawableWidth);

			float prevActualHeight = prevMatchViewHeight * normalizedScale;
			float actualHeight = getImageHeight();
			translateMatrixAfterRotate(Matrix.MTRANS_Y, transY,
					prevActualHeight, actualHeight, prevViewHeight, viewHeight,
					drawableHeight);

			matrix.setValues(m);
		}
		fixTrans();
		setImageMatrix(matrix);
	}

	private int setViewSize(int mode, int size, int drawableWidth) {
		int viewSize;
		switch (mode) {
		case MeasureSpec.EXACTLY:
			viewSize = size;
			break;

		case MeasureSpec.AT_MOST:
			viewSize = Math.min(drawableWidth, size);
			break;

		case MeasureSpec.UNSPECIFIED:
			viewSize = drawableWidth;
			break;

		default:
			viewSize = size;
			break;
		}
		return viewSize;
	}

	private void translateMatrixAfterRotate(int axis, float trans,
			float prevImageSize, float imageSize, int prevViewSize,
			int viewSize, int drawableSize) {
		if (imageSize < viewSize) {
			m[axis] = (viewSize - (drawableSize * m[Matrix.MSCALE_X])) * 0.5f;

		} else if (trans > 0) {
			m[axis] = -((imageSize - viewSize) * 0.5f);

		} else {
			float percentage = (Math.abs(trans) + (0.5f * prevViewSize))
					/ prevImageSize;
			m[axis] = -((percentage * imageSize) - (viewSize * 0.5f));
		}
	}

	private void setState(State state) {
		this.state = state;
	}

	private class GestureListener extends
			GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			return performClick();
		}

		@Override
		public void onLongPress(MotionEvent e) {
			performLongClick();
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			if (fling != null) {
				fling.cancelFling();
			}
			fling = new Fling((int) velocityX, (int) velocityY);
			compatPostOnAnimation(fling);
			return super.onFling(e1, e2, velocityX, velocityY);
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			boolean consumed = false;
			if (state == NONE) {
				float targetZoom = (normalizedScale == minScale) ? maxScale
						: minScale;
				DoubleTapZoom doubleTap = new DoubleTapZoom(normalizedScale,
						targetZoom, e.getX(), e.getY(), false);
				compatPostOnAnimation(doubleTap);
				consumed = true;
			}
			return consumed;
		}
	}

	private class TouchImageViewListener implements OnTouchListener {

		private PointF last = new PointF();

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			mScaleDetector.onTouchEvent(event);
			mGestureDetector.onTouchEvent(event);
			PointF curr = new PointF(event.getX(), event.getY());

			if (state == NONE || state == DRAG || state == FLING) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					last.set(curr);
					if (fling != null)
						fling.cancelFling();
					setState(DRAG);
					break;

				case MotionEvent.ACTION_MOVE:
					if (state == DRAG) {
						float deltaX = curr.x - last.x;
						float deltaY = curr.y - last.y;
						float fixTransX = getFixDragTrans(deltaX, viewWidth,
								getImageWidth());
						float fixTransY = getFixDragTrans(deltaY, viewHeight,
								getImageHeight());
						matrix.postTranslate(fixTransX, fixTransY);
						fixTrans();
						last.set(curr.x, curr.y);
					}
					break;

				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_POINTER_UP:
					setState(NONE);
					break;
				}
			}

			setImageMatrix(matrix);
			invalidate();
			return true;
		}
	}

	private class ScaleListener extends
			ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			setState(ZOOM);
			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			scaleImage(detector.getScaleFactor(), detector.getFocusX(),
					detector.getFocusY(), true);
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			super.onScaleEnd(detector);
			setState(NONE);
			boolean animateToZoomBoundary = false;
			float targetZoom = normalizedScale;
			if (normalizedScale > maxScale) {
				targetZoom = maxScale;
				animateToZoomBoundary = true;

			} else if (normalizedScale < minScale) {
				targetZoom = minScale;
				animateToZoomBoundary = true;
			}

			if (animateToZoomBoundary) {
				DoubleTapZoom doubleTap = new DoubleTapZoom(normalizedScale,
						targetZoom, viewWidth / 2, viewHeight / 2, true);
				compatPostOnAnimation(doubleTap);
			}
		}
	}

	private void scaleImage(float deltaScale, float focusX, float focusY,
			boolean stretchImageToSuper) {

		float lowerScale, upperScale;
		if (stretchImageToSuper) {
			lowerScale = superMinScale;
			upperScale = superMaxScale;

		} else {
			lowerScale = minScale;
			upperScale = maxScale;
		}

		float origScale = normalizedScale;
		normalizedScale *= deltaScale;
		if (normalizedScale > upperScale) {
			normalizedScale = upperScale;
			deltaScale = upperScale / origScale;
		} else if (normalizedScale < lowerScale) {
			normalizedScale = lowerScale;
			deltaScale = lowerScale / origScale;
		}

		matrix.postScale(deltaScale, deltaScale, focusX, focusY);
		fixScaleTrans();
	}

	private class DoubleTapZoom implements Runnable {

		private long startTime;
		private static final float ZOOM_TIME = 500;
		private float startZoom, targetZoom;
		private float bitmapX, bitmapY;
		private boolean stretchImageToSuper;
		private AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
		private PointF startTouch;
		private PointF endTouch;

		DoubleTapZoom(float startZoom, float targetZoom, float focusX,
				float focusY, boolean stretchImageToSuper) {
			setState(ANIMATE_ZOOM);
			startTime = System.currentTimeMillis();
			this.startZoom = startZoom;
			this.targetZoom = targetZoom;
			this.stretchImageToSuper = stretchImageToSuper;
			PointF bitmapPoint = transformCoordTouchToBitmap(focusX, focusY,
					false);
			this.bitmapX = bitmapPoint.x;
			this.bitmapY = bitmapPoint.y;
			startTouch = transformCoordBitmapToTouch(bitmapX, bitmapY);
			endTouch = new PointF(viewWidth / 2, viewHeight / 2);
		}

		@Override
		public void run() {
			float t = interpolate();
			float deltaScale = calculateDeltaScale(t);
			scaleImage(deltaScale, bitmapX, bitmapY, stretchImageToSuper);
			translateImageToCenterTouchPosition(t);
			fixScaleTrans();
			setImageMatrix(matrix);

			if (t < 1f) {
				compatPostOnAnimation(this);

			} else {
				setState(NONE);
			}
		}

		private void translateImageToCenterTouchPosition(float t) {
			float targetX = startTouch.x + t * (endTouch.x - startTouch.x);
			float targetY = startTouch.y + t * (endTouch.y - startTouch.y);
			PointF curr = transformCoordBitmapToTouch(bitmapX, bitmapY);
			matrix.postTranslate(targetX - curr.x, targetY - curr.y);
		}

		private float interpolate() {
			long currTime = System.currentTimeMillis();
			float elapsed = (currTime - startTime) / ZOOM_TIME;
			elapsed = Math.min(1f, elapsed);
			return interpolator.getInterpolation(elapsed);
		}

		private float calculateDeltaScale(float t) {
			float zoom = startZoom + t * (targetZoom - startZoom);
			return zoom / normalizedScale;
		}
	}

	private PointF transformCoordTouchToBitmap(float x, float y,
			boolean clipToBitmap) {
		matrix.getValues(m);
		float origW = getDrawable().getIntrinsicWidth();
		float origH = getDrawable().getIntrinsicHeight();
		float transX = m[Matrix.MTRANS_X];
		float transY = m[Matrix.MTRANS_Y];
		float finalX = ((x - transX) * origW) / getImageWidth();
		float finalY = ((y - transY) * origH) / getImageHeight();

		if (clipToBitmap) {
			finalX = Math.min(Math.max(x, 0), origW);
			finalY = Math.min(Math.max(y, 0), origH);
		}

		return new PointF(finalX, finalY);
	}

	private PointF transformCoordBitmapToTouch(float bx, float by) {
		matrix.getValues(m);
		float origW = getDrawable().getIntrinsicWidth();
		float origH = getDrawable().getIntrinsicHeight();
		float px = bx / origW;
		float py = by / origH;
		float finalX = m[Matrix.MTRANS_X] + getImageWidth() * px;
		float finalY = m[Matrix.MTRANS_Y] + getImageHeight() * py;
		return new PointF(finalX, finalY);
	}

	private class Fling implements Runnable {

		Scroller scroller;
		int currX, currY;

		Fling(int velocityX, int velocityY) {
			setState(FLING);
			scroller = new Scroller(context);
			matrix.getValues(m);

			int startX = (int) m[Matrix.MTRANS_X];
			int startY = (int) m[Matrix.MTRANS_Y];
			int minX, maxX, minY, maxY;

			if (getImageWidth() > viewWidth) {
				minX = viewWidth - (int) getImageWidth();
				maxX = 0;

			} else {
				minX = maxX = startX;
			}

			if (getImageHeight() > viewHeight) {
				minY = viewHeight - (int) getImageHeight();
				maxY = 0;

			} else {
				minY = maxY = startY;
			}

			scroller.fling(startX, startY, (int) velocityX, (int) velocityY,
					minX, maxX, minY, maxY);
			currX = startX;
			currY = startY;
		}

		public void cancelFling() {
			if (scroller != null) {
				setState(NONE);
				scroller.forceFinished(true);
			}
		}

		@Override
		public void run() {
			if (scroller.isFinished()) {
				scroller = null;
				return;
			}

			if (scroller.computeScrollOffset()) {
				int newX = scroller.getCurrX();
				int newY = scroller.getCurrY();
				int transX = newX - currX;
				int transY = newY - currY;
				currX = newX;
				currY = newY;
				matrix.postTranslate(transX, transY);
				fixTrans();
				setImageMatrix(matrix);
				compatPostOnAnimation(this);
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void compatPostOnAnimation(Runnable runnable) {
		if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
			postOnAnimation(runnable);
		} else {
			postDelayed(runnable, 1000 / 60);
		}
	}
}