package com.example.gpsdiariomaps;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.EntryViewHolder> {

    private final List<Entry> entryList;
    private final Context context;

    public EntryAdapter(Context context, List<Entry> entryList) {
        this.context = context;
        this.entryList = entryList;
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entry, parent, false);
        return new EntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        Entry entry = entryList.get(position);
        holder.textTitle.setText(entry.getTitle());
        holder.textDescription.setText(entry.getDescription());
        holder.textLocation.setText("Ubi: Lat: " + entry.getLatitude() + ", Lon: " + entry.getLongitude());

        if (entry.getPhotoPath() != null) {
            holder.imagePhotoPreview.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext()).load(entry.getPhotoPath()).into(holder.imagePhotoPreview);
        } else {
            holder.imagePhotoPreview.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ViewEntryActivity.class);
            intent.putExtra("entry", entry);
            ((MainActivity) context).startActivityForResult(intent, MainActivity.REQUEST_VIEW_ENTRY);
        });

        holder.textLocation.setOnClickListener(v -> {
            Intent intent = new Intent(context, MapsActivity.class);
            intent.putExtra("latitude", entry.getLatitude());
            intent.putExtra("longitude", entry.getLongitude());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return entryList.size();
    }

    public static class EntryViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textDescription, textLocation;
        ImageView imagePhotoPreview;

        public EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            textLocation = itemView.findViewById(R.id.text_location);
            imagePhotoPreview = itemView.findViewById(R.id.image_photo_preview);
        }
    }
}
