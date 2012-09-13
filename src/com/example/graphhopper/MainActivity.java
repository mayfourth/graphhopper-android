package com.example.graphhopper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.ListOverlay;
import org.mapsforge.android.maps.overlay.Marker;
import org.mapsforge.android.maps.overlay.PolygonalChain;
import org.mapsforge.android.maps.overlay.Polyline;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.map.reader.header.FileOpenResult;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Toast;
import de.jetsli.graph.routing.AStar;
import de.jetsli.graph.routing.Path;
import de.jetsli.graph.routing.RoutingAlgorithm;
import de.jetsli.graph.routing.util.FastestCalc;
import de.jetsli.graph.storage.Graph;
import de.jetsli.graph.storage.Location2IDIndex;
import de.jetsli.graph.storage.Location2IDQuadtree;
import de.jetsli.graph.storage.MMapGraph;
import de.jetsli.graph.storage.MemoryGraphSafe;
import de.jetsli.graph.util.StopWatch;

public class MainActivity extends MapActivity {

	private MapView mapView;
	private Graph graph;
	private Location2IDIndex locIndex;
	private GeoPoint start;
	// private static String area = "berlin";
	private static String area = "oberfranken";
	private static final String GRAPH_FOLDER = Environment.getExternalStorageDirectory()
			.getAbsolutePath()
			+ "/graphhopper/maps/graph-" + area + ".osm/";
	private static final String MAP_FILE = Environment.getExternalStorageDirectory()
			.getAbsolutePath()
			+ "/graphhopper/maps/" + area + ".map";
	ListOverlay pathOverlay = new ListOverlay();
	SimpleOnGestureListener listener = new SimpleOnGestureListener() {

		public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
			float x = motionEvent.getX();
			float y = motionEvent.getY();
			Projection p = mapView.getProjection();
			GeoPoint tmpPoint = p.fromPixels((int) x, (int) y);

			if (start != null) {
				Marker marker = createMarker(tmpPoint, R.drawable.flag_red);
				if (marker != null) {
					pathOverlay.getOverlayItems().add(marker);
					mapView.redraw();
				}

				calcPath(start.latitude, start.longitude, tmpPoint.latitude,
						tmpPoint.longitude);
				start = null;
			} else {
				start = tmpPoint;
				pathOverlay.getOverlayItems().clear();
				Marker marker = createMarker(start, R.drawable.flag_green);
				if (marker != null) {
					pathOverlay.getOverlayItems().add(marker);
					mapView.redraw();
				}
			}
			return true;
		}
	};
	GestureDetector gestureDetector = new GestureDetector(listener);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		mapView = new MapView(this) {

			@Override
			public boolean onTouchEvent(MotionEvent event) {
				if (gestureDetector.onTouchEvent(event))
					return true;
				return super.onTouchEvent(event);
			}
		};
		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(true);
		FileOpenResult fileOpenResult = mapView.setMapFile(new File(MAP_FILE));
		if (!fileOpenResult.isSuccess()) {
			logUserLong(fileOpenResult.getErrorMessage());
			finish();
		}
		setContentView(mapView);

		mapView.getOverlays().add(pathOverlay);
	}

	Graph getGraph() {
		if (graph == null) {
			logUser("initial loading graph...");

			// not thread safe but graph is only partially loaded into RAM
			MMapGraph g = new MMapGraph(GRAPH_FOLDER, 10);
			g.loadExisting();
			graph = g;

			// fast and read-thread safe but requires the entire graph in RAM
			// which fails for large areas (but e.g. the city Berlin is ok)
			// graph = new MemoryGraphSafe(GRAPH_FOLDER);
			log("found graph with " + g.getNodes() + " nodes");
		}
		return graph;
	}

	Location2IDIndex getLocIndex() {
		if (locIndex == null) {
			Graph g = getGraph();			
			log("creating index for graph");
			// TODO why is this so slow on android?
			locIndex = new Location2IDQuadtree(g).prepareIndex(1000);
		}
		return locIndex;
	}

	private Polyline createPolyline(Path p) {
		int locs = p.locations();
		List<GeoPoint> geoPoints = new ArrayList<GeoPoint>(locs);
		for (int i = 0; i < locs; i++) {
			geoPoints.add(toGeoPoint(p, i));
		}
		PolygonalChain polygonalChain = new PolygonalChain(geoPoints);
		Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintStroke.setStyle(Paint.Style.STROKE);
		paintStroke.setColor(Color.BLUE);
		paintStroke.setAlpha(128);
		paintStroke.setStrokeWidth(8);
		paintStroke.setPathEffect(new DashPathEffect(new float[] { 25, 15 }, 0));

		return new Polyline(polygonalChain, paintStroke);
	}

	private GeoPoint toGeoPoint(Path p, int i) {
		int index = p.location(i);
		return new GeoPoint(getGraph().getLatitude(index), getGraph().getLongitude(index));
	}

	private Marker createMarker(GeoPoint p, int resource) {
		Drawable drawable = getResources().getDrawable(resource);
		return new Marker(p, Marker.boundCenterBottom(drawable));
	}

	private volatile boolean taskRunning = false;

	public void calcPath(final double fromLat, final double fromLon, final double toLat,
			final double toLon) {
		if (!taskRunning) {
			taskRunning = true;
			new AsyncTask<Void, Void, Path>() {
				float locFindTime;
				float time;

				protected Path doInBackground(Void... v) {
					StopWatch sw = new StopWatch().start();
					log("query graph");
					int fromId = getLocIndex().findID(fromLat, fromLon);
					int toId = getLocIndex().findID(toLat, toLon);
					locFindTime = sw.stop().getSeconds();
					sw = new StopWatch().start();					
					RoutingAlgorithm algo = new AStar(getGraph())
							.setType(FastestCalc.DEFAULT);
					logUser("calculating path ...");
					Path p = algo.calcPath(fromId, toId);
					time = sw.stop().getSeconds();
					return p;
				}

				protected void onPostExecute(Path p) {
					log("found path from:" + fromLat + "," + fromLon + " to " + toLat
							+ "," + toLon + " with distance:" + p.distance()
							+ ", locations:" + p.locations() + ", time:" + time
							+ ", locFindTime:" + locFindTime);
					logUserLong("the route is " + (float) p.distance() + "km long");

					pathOverlay.getOverlayItems().add(createPolyline(p));
					mapView.redraw();
					taskRunning = false;
				}
			}.execute();
		}
	}

	private void log(String str) {
		Log.i("GH", str);
	}

	private void logUser(String str) {
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}

	private void logUserLong(String str) {
		Toast.makeText(this, str, Toast.LENGTH_LONG).show();
	}
}
