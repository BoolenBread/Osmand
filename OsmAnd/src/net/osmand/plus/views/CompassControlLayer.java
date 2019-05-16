package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import net.osmand.PlatformUtil;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import org.apache.commons.logging.Log;

public class CompassControlLayer extends OsmandMapLayer {

	private static final Log log = PlatformUtil.getLog(CompassControlLayer.class);

	private final MapActivity mapActivity;
	private OsmandApplication app;

	private Bitmap compassIconDay;
	private Bitmap compassLocation;
	private Paint bitmapPaint;
	private float cachedHeading = 0;

	public CompassControlLayer(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		app = mapActivity.getMyApplication();

		compassIconDay = BitmapFactory.decodeResource(app.getResources(), R.drawable.map_widget_compass_ruller_day);
		compassLocation = BitmapFactory.decodeResource(app.getResources(), R.drawable.map_widget_compass_ruller_location_day);

		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {

	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
		if (shouldShowCompass()) {
			final QuadPoint center = tb.getCenterPixelPoint();

			updateCompass();

			long timeStart = System.currentTimeMillis();
			canvas.drawBitmap(compassIconDay, center.x - compassIconDay.getWidth() / 2, center.y - compassIconDay.getHeight() / 2, bitmapPaint);
			long timeCenterIconEnd = System.currentTimeMillis();
			log.debug("CompassControlLayer drawCenterIcon time " + (timeCenterIconEnd - timeStart));

			canvas.rotate(cachedHeading, center.x, center.y);
			canvas.drawBitmap(compassLocation, center.x - compassLocation.getWidth() / 2, center.y - compassLocation.getHeight() / 2, bitmapPaint);
			canvas.rotate(-cachedHeading, center.x, center.y);
			long timeCompassIconEnd = System.currentTimeMillis();
			log.debug("CompassControlLayer drawCompassIcon time " + (timeCompassIconEnd - timeCenterIconEnd) + " cachedHeading " + cachedHeading);
		}
	}

	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	public void updateCompass() {
		Float heading = mapActivity.getMapViewTrackingUtilities().getHeading();
		if (heading != null && heading != cachedHeading) {
			cachedHeading = (float) Math.floor(heading);
		}
	}

	private boolean shouldShowCompass() {
		return app.getSettings().SHOW_COMPASS_RULER.get();
	}
}