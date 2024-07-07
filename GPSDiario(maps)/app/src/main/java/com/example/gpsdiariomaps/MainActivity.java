package com.example.gpsdiariomaps;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_VIEW_ENTRY = 3;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final String PREFS_NAME = "TravelEntriesPrefs";
    private static final String ENTRIES_KEY = "Entries";

    private LocationManager locationManager;
    private LocationListener locationListener;
    private ProgressDialog progressDialog;
    private Location currentLocation;
    private List<Entry> travelEntries;
    private EntryAdapter adapter;
    private Handler handler;
    private Runnable timeoutRunnable;
    private SharedPreferences sharedPreferences;
    private String currentPhotoPath;
    private ImageView imagePreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        FloatingActionButton fabAddEntry = findViewById(R.id.fab_add_entry);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);

        travelEntries = new ArrayList<>(getEntriesFromPreferences());
        adapter = new EntryAdapter(this, travelEntries);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        handler = new Handler();

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLocation = location;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                handler.removeCallbacks(timeoutRunnable); // Remove timeout callback
                Toast.makeText(MainActivity.this, "Ubicación obtenida", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(MainActivity.this, "Proveedor de ubicación deshabilitado", Toast.LENGTH_SHORT).show();
            }
        };

        fabAddEntry.setOnClickListener(v -> showAddEntryDialog());

        // Solicitar permisos de ubicación al inicio
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            }
            // Establecer tiempo de espera para obtener la ubicación
            timeoutRunnable = () -> {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "No se pudo obtener la ubicación. Inténtelo de nuevo.", Toast.LENGTH_SHORT).show();
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

    private void showAddEntryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar Entrada de Viaje");

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_entry, null);
        builder.setView(dialogView);

        EditText editTextTitle = dialogView.findViewById(R.id.edit_text_title);
        EditText editTextDescription = dialogView.findViewById(R.id.edit_text_description);
        Button btnGetLocation = dialogView.findViewById(R.id.btn_get_location);
        Button btnAddPhoto = dialogView.findViewById(R.id.btn_add_photo);
        imagePreview = dialogView.findViewById(R.id.image_preview);

        btnGetLocation.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }
            showProgressDialog();
            startLocationUpdates();
        });

        btnAddPhoto.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Toast.makeText(this, "Error al crear el archivo de la foto", Toast.LENGTH_SHORT).show();
                }
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this, "com.example.gpsdiariomaps.fileprovider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String title = editTextTitle.getText().toString();
            String description = editTextDescription.getText().toString();
            if (title.isEmpty()) {
                Toast.makeText(this, "El título no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentLocation != null) {
                Entry entry = new Entry(title, description, currentLocation.getLatitude(), currentLocation.getLongitude(), currentPhotoPath);
                travelEntries.add(entry);
                adapter.notifyDataSetChanged();
                saveEntriesToPreferences();
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private File createImageFile() throws IOException {
        // Crear un archivo de imagen con un nombre de archivo único
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        // Guardar la ruta del archivo de la foto
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private Set<Entry> getEntriesFromPreferences() {
        Set<String> entryStrings = sharedPreferences.getStringSet(ENTRIES_KEY, new HashSet<>());
        Set<Entry> entries = new HashSet<>();
        for (String entryString : entryStrings) {
            String[] parts = entryString.split("\\|");
            if (parts.length >= 5) {
                String title = parts[0];
                String description = parts[1];
                double latitude = Double.parseDouble(parts[2]);
                double longitude = Double.parseDouble(parts[3]);
                String photoPath = parts[4];
                entries.add(new Entry(title, description, latitude, longitude, photoPath));
            }
        }
        return entries;
    }

    private void saveEntriesToPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Set<String> entryStrings = new HashSet<>();
        for (Entry entry : travelEntries) {
            entryStrings.add(entry.getTitle() + "|" + entry.getDescription() + "|" + entry.getLatitude() + "|" + entry.getLongitude() + "|" + entry.getPhotoPath());
        }
        editor.putStringSet(ENTRIES_KEY, entryStrings);
        editor.apply();
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Obteniendo ubicación...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(this, "Foto guardada en: " + currentPhotoPath, Toast.LENGTH_SHORT).show();
            imagePreview.setVisibility(View.VISIBLE);
            imagePreview.setImageURI(Uri.parse(currentPhotoPath));
        } else if (requestCode == REQUEST_VIEW_ENTRY && resultCode == RESULT_OK) {
            Entry updatedEntry = (Entry) data.getSerializableExtra("entry");
            for (int i = 0; i < travelEntries.size(); i++) {
                if (travelEntries.get(i).getId() == updatedEntry.getId()) {
                    travelEntries.set(i, updatedEntry);
                    adapter.notifyItemChanged(i);
                    saveEntriesToPreferences();
                    break;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        travelEntries.clear();
        travelEntries.addAll(getEntriesFromPreferences());
        adapter.notifyDataSetChanged();
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
