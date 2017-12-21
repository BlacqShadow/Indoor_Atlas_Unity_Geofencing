package ghosthunt.sp9.com.androidwrapper;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.content.Context;

import com.indooratlas.android.sdk.IAGeofence;
import com.indooratlas.android.sdk.IAGeofenceEvent;
import com.indooratlas.android.sdk.IAGeofenceListener;
import com.indooratlas.android.sdk.IAGeofenceRequest;
import com.unity3d.player.UnityPlayer;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IAOrientationListener;
import com.indooratlas.android.sdk.IAOrientationRequest;
import com.indooratlas.android.sdk.IARegion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.String;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by Rudra on 12/12/17.
 */

public class IaUnityPlugin {

    private IALocationManager mManager;
    private final Handler mHandler;
    private boolean mGeofencePlaced = false;
    private IAGeofenceListener iaGeofenceListener;

    public IaUnityPlugin(Object context, final String apiKey, final String apiSecret, final String gameObjectName, double headingSensitivity, double orientationSensitivity) throws Exception {
        final Context ctx = (Context)context;
        mHandler = new Handler(ctx.getMainLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Bundle extras = new Bundle(2);
                extras.putString(IALocationManager.EXTRA_API_KEY, apiKey);
                extras.putString(IALocationManager.EXTRA_API_SECRET, apiSecret);
                mManager = IALocationManager.create(ctx, extras);

                if (!mManager.requestLocationUpdates(IALocationRequest.create(), new IALocationListener() {
                    @Override
                    public void onLocationChanged(IALocation iaLocation) {
                        JSONObject location = new JSONObject();
                        try {
                            location.put("accuracy", iaLocation.getAccuracy());
                            location.put("altitude", iaLocation.getAltitude());
                            location.put("bearing", iaLocation.getBearing());
                            location.put("floorLevel", iaLocation.getFloorLevel());
                            location.put("hasFloorlevel", iaLocation.hasFloorLevel());
                            location.put("latitude", iaLocation.getLatitude());
                            location.put("longitude", iaLocation.getLongitude());
                            location.put("timestamp", iaLocation.getTime());
                            UnityPlayer.UnitySendMessage(gameObjectName, "onLocationChanged",
                                    location.toString());
                        } catch (JSONException e) {
                            Log.e("IAUNITY", e.toString());
                            throw new IllegalStateException(e.getMessage());
                        }
                    }


                    @Override
                    public void onStatusChanged(String provider, int status, Bundle bundle) {
                        int outputStatus = 0;
                        switch (status) {
                            case IALocationManager.STATUS_LIMITED:
                                outputStatus = 10;
                                break;
                            case IALocationManager.STATUS_OUT_OF_SERVICE:
                                outputStatus = 0;
                                break;
                            case IALocationManager.STATUS_TEMPORARILY_UNAVAILABLE:
                                outputStatus = 1;
                                break;
                            case IALocationManager.STATUS_AVAILABLE:
                                outputStatus = 2;
                                break;
                            default:
                                return;
                        }
                        try {
                            JSONObject statusObject = new JSONObject();
                            statusObject.put("status", outputStatus);
                            UnityPlayer.UnitySendMessage(gameObjectName, "onStatusChanged",
                                    statusObject.toString());
                        } catch(JSONException e) {
                            Log.e("IAUNITY", e.toString());
                            throw new IllegalStateException(e.getMessage());
                        }
                    }
                })) {
                    Log.e("IAUNITY", "Requesting location updates failed");
                }


                iaGeofenceListener = new IAGeofenceListener() {
                    @Override
                    public void onGeofencesTriggered(IAGeofenceEvent iaGeofenceEvent) {
                        List<IAGeofence> fences = iaGeofenceEvent.getTriggeringGeofences();

                        JSONObject geofence = new JSONObject();
                        try {
                            geofence.put("area", fences.get(0).getId());
                            geofence.put("transition", ((iaGeofenceEvent.getGeofenceTransition() == IAGeofence.GEOFENCE_TRANSITION_ENTER) ?
                                    "ENTER" : "EXIT"));

                            UnityPlayer.UnitySendMessage(gameObjectName, "onGeofencesTriggered",
                                    geofence.toString());
                        } catch (JSONException e) {
                            Log.e("IAUNITY", e.toString());
                            throw new IllegalStateException(e.getMessage());
                        }
                    }
                };

                placeGeofences();
                if (!mManager.registerRegionListener(new IARegion.Listener() {
                    @Override
                    public void onEnterRegion(IARegion iaRegion) {

                        UnityPlayer.UnitySendMessage(gameObjectName, "onEnterRegion",
                                regionToJson(iaRegion));


                    }

                    @Override
                    public void onExitRegion(IARegion iaRegion) {
                        UnityPlayer.UnitySendMessage(gameObjectName, "onExitRegion",
                                regionToJson(iaRegion));
                    }

                    private String regionToJson(IARegion iaRegion) {
                        try {
                            JSONObject region = new JSONObject();
                            region.put("id", iaRegion.getId());
                            region.put("name", iaRegion.getName());
                            region.put("timestamp", iaRegion.getTimestamp());
                            region.put("type", regionTypeToInt(iaRegion.getType()));
                            return region.toString();
                        } catch(JSONException e) {
                            Log.e("IAUNITY", e.toString());
                            throw new IllegalStateException(e.getMessage());
                        }
                    }

                    private int regionTypeToInt(int type) {
                        switch(type) {
                            case IARegion.TYPE_FLOOR_PLAN:
                                return 1;
                            case IARegion.TYPE_VENUE:
                                return 2;
                        }
                        return 0;
                    }
                })) {
                    Log.e("IAUNITY", "Requesting region updates failed");
                }
            }
        });
        mHandler.post(new Runnable() {
            private double headingSensitivity;
            private double orientationSensitivity;

            @Override
            public void run() {
                IAOrientationRequest mOrientationRequest = new IAOrientationRequest(headingSensitivity, orientationSensitivity);

                if (!mManager.registerOrientationListener(mOrientationRequest, new IAOrientationListener() {
                    @Override
                    public void onHeadingChanged(long timestamp, double heading) {
                        try {
                            JSONObject headingObject = new JSONObject();
                            headingObject.put("timestamp", timestamp);
                            headingObject.put("heading", heading);
                            UnityPlayer.UnitySendMessage(gameObjectName, "onHeadingChanged",
                                    headingObject.toString());
                        } catch(JSONException e) {
                            Log.e("IAUNITY", e.toString());
                            throw new IllegalStateException(e.getMessage());
                        }
                    }

                    @Override
                    public void onOrientationChange(long timestamp, double[] quaternion) {
                        try {
                            JSONObject orientation = new JSONObject();
                            orientation.put("x", quaternion[1]);
                            orientation.put("y", quaternion[2]);
                            orientation.put("z", quaternion[3]);
                            orientation.put("w", quaternion[0]);
                            orientation.put("timestamp", timestamp);
                            UnityPlayer.UnitySendMessage(gameObjectName, "onOrientationChange",
                                    orientation.toString());
                        } catch(JSONException e) {
                            Log.e("IAUNITY", e.toString());
                            throw new IllegalStateException(e.getMessage());
                        }
                    }
                })) {
                    Log.e("IAUNITY", "Registering orientation listener failed.");
                }
            }

            private Runnable init(double hs, double os) {
                headingSensitivity = hs;
                orientationSensitivity = os;
                return this;
            }
        }.init(headingSensitivity, orientationSensitivity));
    }

    public void close() {
        mManager.destroy();
    }

    private void placeGeofences()
    {

        /*
        new Vector2(-37.84811769f, 145.11430576f),
        new Vector2(-37.84802129f, 145.11431266f),
        new Vector2(-37.84792601f, 145.11432588f),
        new Vector2(-37.84792520f,145.11433971f),
        new Vector2(-37.84802397f, 145.11433594f)
        new Vector2(-37.84811399f, 145.11432856f)

        area 2
        new Vector2(-37.84772834f,145.11434047f),
		new Vector2(-37.84763059f,145.11435446f),
		new Vector2(-37.84758927f,145.11435522f),
		new Vector2(-37.84763202f,145.11436652f),
		new Vector2(-37.84773073f,145.11435600f)
         */
        ArrayList<double[]> edgesArea1 = new ArrayList<>();
        ArrayList<double[]> edgesArea2 = new ArrayList<>();
        ArrayList<double[]> edgesArea3 = new ArrayList<>();


        //Add the co-ordinates for both the areas

        /* Deakin Building B co-ordinates
        edgesArea1.add(new double[]{-37.84811769, 145.11430576});
        edgesArea1.add(new double[]{-37.84802129, 145.11431266});
        edgesArea1.add(new double[]{-37.84792601, 145.11432588});
        edgesArea1.add(new double[]{-37.84792520,145.11433971});
        edgesArea1.add(new double[]{-37.84802397, 145.11433594});
        edgesArea1.add(new double[]{-37.84811399, 145.11432856});

        edgesArea2.add(new double[]{-37.84772834,145.11434047});
        edgesArea2.add(new double[]{-37.84763059,145.11435446});
        edgesArea2.add(new double[]{-37.84758927,145.11435522});
        edgesArea2.add(new double[]{-37.84763202,145.11436652});
        edgesArea2.add(new double[]{-37.84773073,145.11435600});

        edgesArea3.add(new double[]{-37.84782655, 145.11433595});
        edgesArea3.add(new double[]{-37.84772834, 145.11434047});
        edgesArea3.add(new double[]{-37.84773073, 145.11435600});
        edgesArea3.add(new double[]{-37.84782757, 145.11435583});*/

        /* GAOL */
        edgesArea1.add(new double[]{-38.15393322, 144.36848583});
        edgesArea1.add(new double[]{-38.15392250, 144.36848982});
        edgesArea1.add(new double[]{-38.15393156, 144.36852170});
        edgesArea1.add(new double[]{-38.15394195, 144.36851726});




        edgesArea2.add(new double[]{-38.15395249, 144.36856231});
        edgesArea2.add(new double[]{-38.15394460, 144.36856633});
        edgesArea2.add(new double[]{-38.15393430, 144.36859714});
        edgesArea2.add(new double[]{-38.15394214, 144.36861193});
        edgesArea2.add(new double[]{-38.15394685, 144.36863264});
        edgesArea2.add(new double[]{-38.15396652, 144.36864697});
        edgesArea2.add(new double[]{-38.15397538, 144.36864387});



        edgesArea3.add(new double[]{-38.15394472, 144.36852781});
        edgesArea3.add(new double[]{-38.15393486, 144.36853108});
        edgesArea3.add(new double[]{-38.15394460, 144.36856633});
        edgesArea3.add(new double[]{-38.15395249, 144.36856231});
        edgesArea3.add(new double[]{-38.15395101, 144.36855000});









        // Add a circular geofence by adding points with a 10 m radius clockwise
        /*double lat_per_meter = 9e-06*Math.cos(Math.PI/180.0*location.getLatitude());
        double lon_per_meter = 9e-06;
        ArrayList<double[]> edges = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            double lat = location.getLatitude() + 10*lat_per_meter*Math.sin(-2*Math.PI*i/10);
            double lon = location.getLongitude() + 10*lon_per_meter*Math.cos(-2*Math.PI*i/10);
            edges.add(new double[]{lat, lon});
            Log.d(TAG, "Geofence: " + lat + ", " + lon);
        }

        String currentDateandTime = mDateFormat.format(new Date());

        String text = mEventLog.getText().toString();
        text += "\n" + currentDateandTime + "\t" + "Placing a geofence in current location!";
        mEventLog.setText(text);*/

        // If you want to use simple rectangle instead, uncomment the following:
        /*
        ArrayList<double[]> edges = new ArrayList<>();
        // Approximate meter to coordinate transformations
        double latMeters = 0.4488 * 1e-4;
        double lonMeters = 0.8961 * 1e-4;
        // Size of the geofence e.g. 5 by 5 meters
        double geofenceSize = 5;

        // Coordinates of the south-west corner of the geofence
        double lat1 = location.getLatitude() - 0.5 * geofenceSize * latMeters;
        double lon1 = location.getLongitude() - 0.5 * geofenceSize * lonMeters;

        // Coordiantes of the north-east corner of the geofence
        double lat2 = location.getLatitude() + 0.5 * geofenceSize * latMeters;
        double lon2 = location.getLongitude() + 0.5 * geofenceSize * lonMeters;

        // Add them in clockwise order
        edges.add(new double[]{lat1, lon1});
        edges.add(new double[]{lat2, lon1});
        edges.add(new double[]{lat2, lon2});
        edges.add(new double[]{lat1, lon2});
        */

        Log.d(TAG, "Creating geofence Area 1");
        /*IAGeofence geofenceArea1 = new IAGeofence.Builder()
                .withEdges(edges)
                .withId("My geofence")
                .withTransitionType(IAGeofence.GEOFENCE_TRANSITION_ENTER |
                        IAGeofence.GEOFENCE_TRANSITION_EXIT)
                .build();*/

        final IAGeofence geofenceArea1 = new IAGeofence.Builder().withEdges(edgesArea1).withId("Area 1").withTransitionType(IAGeofence.GEOFENCE_TRANSITION_ENTER |
                IAGeofence.GEOFENCE_TRANSITION_ENTER).build();

        Log.d(TAG, "Creating geofence Area 2");
        final IAGeofence geofenceArea2 = new IAGeofence.Builder().withEdges(edgesArea2).withId("Area 2").withTransitionType(IAGeofence.GEOFENCE_TRANSITION_ENTER |
                IAGeofence.GEOFENCE_TRANSITION_ENTER).build();

        Log.d(TAG, "Creating geofence Area 3");
        final IAGeofence geofenceArea3 = new IAGeofence.Builder().withEdges(edgesArea3).withId("Area 3").withTransitionType(IAGeofence.GEOFENCE_TRANSITION_ENTER |
                IAGeofence.GEOFENCE_TRANSITION_ENTER).build();

        List<IAGeofence> fences = new ArrayList<>();
        fences.add(geofenceArea1);
        fences.add(geofenceArea2);
        fences.add(geofenceArea3);

        Log.d(TAG, "Geofences added");
        mManager.addGeofences(new IAGeofenceRequest.Builder()
                            .withGeofences(fences).withInitialTrigger(IAGeofenceRequest.INITIAL_TRIGGER_EXIT).build(),iaGeofenceListener);
        //Log.i(TAG, "New geofence registered: " + geofence);
        /*mManager.addGeofences(new IAGeofenceRequest.Builder()
                .withGeofence(geofence)
                .withInitialTrigger(IAGeofenceRequest.INITIAL_TRIGGER_ENTER)
                .build(), this);*/
    }
}
