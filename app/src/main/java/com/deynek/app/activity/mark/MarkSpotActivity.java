package com.deynek.app.activity.mark;

import android.app.Dialog;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.deynek.app.R;
import com.deynek.app.model.MyActivity;
import com.deynek.app.session.ApplicationStateManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;

public class MarkSpotActivity extends MyActivity {

    private GoogleMap map;
    private boolean initialPositionSet = false;
    private Marker pinMarker;
    private ArrayList<Marker> markers = new ArrayList<Marker>();

    private String provider;
    private LocationListener locationListener;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, this);
        setContentView(R.layout.activity_mark_spot);

        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        // Showing status
        if (status != ConnectionResult.SUCCESS) { // Google Play Services are not available
            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();
        }

        // Google Play Services are available
        else {
            // assign map object
            map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            map.setMyLocationEnabled(true);
            map.setBuildingsEnabled(true);
            map.setIndoorEnabled(true);

            // get LocationManager object from System Service LOCATION_SERVICE
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            provider = locationManager.getBestProvider(criteria, true);
            Location lastKnownLocation = locationManager.getLastKnownLocation(provider);

            // zoom to last known position
            if (lastKnownLocation != null)
                focusOnLocation(lastKnownLocation);

            // create location listener
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.d("Map", "location changed, accuracy: " + location.getAccuracy());
                    if(!initialPositionSet && location.getAccuracy() < 35){
                        initialPositionSet = true;
                        focusOnLocation(map.getMyLocation());
                        Log.d("Map", "initial positions set");
                    }
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {
                }

                @Override
                public void onProviderEnabled(String s) {
                }

                @Override
                public void onProviderDisabled(String s) {
                }
            };

            startGPS();

            // set listeners to see if user started interacting with the map
            map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng point) {
                    Log.d("Map", "onMapClick clicked: " + point.toString());
                    drawPin(point);
                }
            });
        }
    }

    private void drawPin(LatLng point) {

        if(pinMarker != null)
            pinMarker.remove();

        pinMarker = map.addMarker(new MarkerOptions()
                .position(point)
                        // .snippet("Lat:" + cameraPosition.getLatitude() + "Lng:" + location.getLongitude())
                        // .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .title("My spot")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_blue_marker))
                .draggable(true));
    }

    // centers camera and zoom in
    private void focusOnLocation(Location loc) {
        CameraUpdate center =
                CameraUpdateFactory.newLatLng(new LatLng(loc.getLatitude(), loc.getLongitude()));
        map.moveCamera(center);
        map.animateCamera(CameraUpdateFactory.zoomTo(19));
    }

    public void onParkedButtonClick(View v) {
        ApplicationStateManager.saveState(ApplicationStateManager.STATES.PARKED);
        Intent i = new Intent(getApplicationContext(), MarkedActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }

    public void startGPS(){
        locationManager.requestLocationUpdates(provider, 20000, 0, locationListener);
    }

    public void stopGPS(){
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopGPS();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopGPS();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopGPS();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startGPS();
    }
}
