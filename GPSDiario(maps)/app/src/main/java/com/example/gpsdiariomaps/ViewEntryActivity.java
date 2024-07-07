package com.example.gpsdiariomaps;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;

public class ViewEntryActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private EditText editTextTitle, editTextDescription;
    private TextView textViewLocation, textLocationLink;
    private ImageView imageViewPhoto;
    private Button btnUpdate, btnReposition;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private ProgressDialog progressDialog;
    private Location currentLocation;
    private Entry entry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_entry);

        editTextTitle = findViewById(R.id.edit_text_title);
        editTextDescription = findViewById(R.id.edit_text_description);
        textViewLocation = findViewById(R.id.text_view_location);
        textLocationLink = findViewById(R.id.text_location_link);
        imageViewPhoto = findViewById(R.id.image_view_photo);
        btnUpdate = findViewById(R.id.btn_update);
        btnReposition = findViewById(R.id.btn_reposition);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        entry = (Entry) getIntent().getSerializableExtra("entry");
        if (entry != null) {
            editTextTitle.setText(entry.getTitle());
            editTextDescription.setText(entry.getDescription());
            textViewLocation.setText("Ubicación: Lat: " + entry.getLatitude() + ", Lon: " + entry.getLongitude());
            if (entry.getPhotoPath() != null) {
                imageViewPhoto.setVisibility(View.VISIBLE);
                Glide.with(this).load(entry.getPhotoPath()).into(imageViewPhoto);
            } else {
                imageViewPhoto.setVisibility(View.GONE);
            }

            double latitude = entry.getLatitude();
            double longitude = entry.getLongitude();
            final String locationUri = "geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "(Ubicación)";
            textLocationLink.setText("Abrir en Google Maps");
            textLocationLink.setOnClickListener(v -> {
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(locationUri));
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    textLocationLink.setText("Google Maps no está instalado");
                }
            });
        }

        btnUpdate.setOnClickListener(v -> {
            String title = editTextTitle.getText().toString();
            String description = editTextDescription.getText().toString();

            if (title.isEmpty()) {
                Toast.makeText(ViewEntryActivity.this, "El título no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }

            entry.setTitle(title);
            entry.setDescription(description);
            if (currentLocation != null) {
                entry.setLatitude(currentLocation.getLatitude());
                entry.setLongitude(currentLocation.getLongitude());
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("entry", entry);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        btnReposition.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }
            showProgressDialog();
            startLocationUpdates();
        });

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLocation = location;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                textViewLocation.setText("Ubicación: Lat: " + currentLocation.getLatitude() + ", Lon: " + currentLocation.getLongitude());
                stopLocationUpdates();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(@NonNull String provider) {}

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(ViewEntryActivity.this, "Proveedor de ubicación deshabilitado", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void startLocationUpdates() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            }
            Handler handler = new Handler();
            Runnable timeoutRunnable = () -> {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    Toast.makeText(ViewEntryActivity.this, "No se pudo obtener la ubicación. Inténtelo de nuevo.", Toast.LENGTH_SHORT).show();
                    stopLocationUpdates();
                }
            };
            handler.postDelayed(timeoutRunnable, 10000); // 10 segundos de tiempo de espera
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al iniciar actualizaciones de ubicación: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Obteniendo ubicación...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

