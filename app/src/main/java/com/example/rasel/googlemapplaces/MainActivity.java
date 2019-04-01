package com.example.rasel.googlemapplaces;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private FusedLocationProviderClient client;
    private Location lastLocation;
    private List<MyClusterItem> items = new ArrayList<>();
    private ClusterManager<MyClusterItem>clusterManager;

    private GeoDataClient geoDataClient;
    private PlaceDetectionClient placeDetectionClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        geoDataClient = Places.getGeoDataClient(this,null);
        placeDetectionClient = Places.getPlaceDetectionClient(this,null);


        client = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();


        GoogleMapOptions options = new GoogleMapOptions();
        options.zoomControlsEnabled(true);

        SupportMapFragment mapFragment = SupportMapFragment.newInstance(options);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction().replace(R.id.mapContainer, mapFragment);
        ft.commit();
        mapFragment.getMapAsync(this);

    }

    private void getLastLocation() {
        if (checkPermission())
        client.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()){
                    lastLocation = task.getResult();
                    LatLng latLng = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    map.addMarker(new MarkerOptions().title("My Current Location").snippet("Islamic University").position(latLng));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
                }
            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (checkPermission())
        map.setMyLocationEnabled(true);

        clusterManager = new ClusterManager<MyClusterItem>(this,map);
        
        map.setOnMarkerClickListener(clusterManager);
        map.setOnCameraIdleListener(clusterManager);
        
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
//                map.addMarker(new MarkerOptions().title("Random").snippet("Islamic University").position(latLng));
//                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                items.add(new MyClusterItem(latLng));
                clusterManager.addItems(items);
                clusterManager.cluster();
            }
        });

    }

    public boolean checkPermission(){
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},199);
            return true;
        }return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        
        if (requestCode == 199 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Granted", Toast.LENGTH_SHORT).show();
        }
    }

    public void findCurrentPlaces(final MenuItem item){
        if (checkPermission());
        placeDetectionClient.getCurrentPlace(null).addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                if (task.isSuccessful() && task.getResult()!= null){
                    PlaceLikelihoodBufferResponse response =task.getResult();
                    int count = response.getCount()
;
                    String[] names = new String[count];
                    String[] addresses = new String[count];
                    LatLng[] latLngs = new LatLng[count];

                    for (int i=0;i<count;i++){
                        PlaceLikelihood likelihood = response.get(i);
                        names[i]= (String) likelihood.getPlace().getName();
                        addresses[i]= (String) likelihood.getPlace().getAddress();
                        latLngs[i] = likelihood.getPlace().getLatLng();

                        items.add(new MyClusterItem(latLngs[i],names[i],addresses[i]));
                    }
                    clusterManager.addItems(items);
                    clusterManager.cluster();
                    response.release();

                    openDialog(names,addresses,latLngs);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openDialog(final String[] names, final String[] addresses, final LatLng[] latlngs) {

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { // which is position
                LatLng latLng = latlngs[which];
                String address = addresses[which];
                String title = names[which];

                map.clear();
                map.addMarker(new MarkerOptions().position(latLng)
                        .title(title)
                        .snippet(address));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));


            }
        };

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("pick A Place")
                .setItems(names,listener)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

}
