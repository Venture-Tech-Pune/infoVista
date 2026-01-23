package com.example.infovista.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.infovista.R;
import com.example.infovista.models.ApiResponse;
import com.example.infovista.models.Notice;
import com.example.infovista.network.ApiClient;
import com.example.infovista.utils.Constants;
import com.example.infovista.utils.SharedPrefManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NoticeDetailsActivity extends AppCompatActivity {

    private TextView tvTitle, tvDescription, tvPriority, tvCategory, tvDate, tvCreator, tvDuration;
    private ImageView ivMedia;
    private MaterialButton btnEdit, btnDelete, btnUpload;
    
    private Notice notice;
    private SharedPrefManager prefManager;
    private String authToken;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice_details);

        prefManager = SharedPrefManager.getInstance(this);
        authToken = "Bearer " + prefManager.getToken();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notice Details");
        }

        // Initialize views
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvPriority = findViewById(R.id.tvPriority);
        tvCategory = findViewById(R.id.tvCategory);
        tvDate = findViewById(R.id.tvDate);
        tvCreator = findViewById(R.id.tvCreator);
        tvDuration = findViewById(R.id.tvDuration);
        ivMedia = findViewById(R.id.ivMedia);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnUpload = findViewById(R.id.btnUpload);

        // Get notice from intent
        String noticeJson = getIntent().getStringExtra("NOTICE_JSON");
        if (noticeJson != null) {
            notice = new Gson().fromJson(noticeJson, Notice.class);
            displayNotice();
        } else {
            Toast.makeText(this, "Error loading notice", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check user role for edit/delete buttons
        String role = prefManager.getUserRole();
        if (role.equals(Constants.ROLE_ADMIN) || role.equals(Constants.ROLE_MANAGER)) {
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }

        // Button listeners
        btnEdit.setOnClickListener(v -> editNotice());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnUpload.setOnClickListener(v -> confirmPublish());
    }

    private void displayNotice() {
        tvTitle.setText(notice.getTitle());
        tvDescription.setText(notice.getDescription());
        tvCategory.setText(notice.getCategory().toUpperCase());
        tvPriority.setText(notice.getPriority().toUpperCase());
        tvDuration.setText("Display Duration: " + notice.getDisplayDuration() + " seconds");

        // Priority color
        int priorityColor;
        switch (notice.getPriority().toLowerCase()) {
            case "urgent":
                priorityColor = getResources().getColor(R.color.priority_urgent);
                break;
            case "high":
                priorityColor = getResources().getColor(R.color.priority_high);
                break;
            case "medium":
                priorityColor = getResources().getColor(R.color.priority_medium);
                break;
            default:
                priorityColor = getResources().getColor(R.color.priority_low);
        }
        tvPriority.setBackgroundColor(priorityColor);

        // Date
        try {
            Date createdDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    .parse(notice.getCreatedAt());
            if (createdDate != null) {
                tvDate.setText("Created: " + dateFormat.format(createdDate));
            }
        } catch (Exception e) {
            tvDate.setText("Created: " + notice.getCreatedAt());
        }

        // Creator
        if (notice.getCreatedBy() != null) {
            tvCreator.setText("By: " + notice.getCreatedBy().getName());
        }

        // Load image
        if (notice.getMediaUrl() != null && !notice.getMediaUrl().isEmpty()
                && notice.getMediaType() != null && notice.getMediaType().equals("image")) {
            ivMedia.setVisibility(View.VISIBLE);

            // Construct proper image URL - remove leading slash from mediaUrl
            String imageUrl = Constants.BASE_URL + notice.getMediaUrl().substring(1);

            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(ivMedia);
        } else {
            ivMedia.setVisibility(View.GONE);
        }

        // Check if notice is a draft (not active) and show Upload button
        String role = prefManager.getUserRole();
        if ((role.equals(Constants.ROLE_ADMIN) || role.equals(Constants.ROLE_MANAGER)) && !notice.isActive()) {
            btnUpload.setVisibility(View.VISIBLE);
        } else {
            btnUpload.setVisibility(View.GONE);
        }
    }

    private static final int REQUEST_EDIT_NOTICE = 101;

    // ... existing onCreate ...

    private void editNotice() {
        Intent intent = new Intent(this, CreateNoticeActivity.class);
        intent.putExtra("NOTICE_JSON", new Gson().toJson(notice));
        startActivityForResult(intent, REQUEST_EDIT_NOTICE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_EDIT_NOTICE && resultCode == RESULT_OK) {
            // Refresh notice details
            fetchNoticeDetails();
        }
    }
    
    private void confirmPublish() {
        new AlertDialog.Builder(this)
                .setTitle("Upload Notice")
                .setMessage("Are you sure you want to upload this notice to the board? It will become visible to all users.")
                .setPositiveButton("Upload", (dialog, which) -> publishNotice())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void publishNotice() {
        // Update notice to set isActive = true
        try {
            okhttp3.RequestBody titlePart = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), notice.getTitle());
            okhttp3.RequestBody descriptionPart = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), notice.getDescription());
            okhttp3.RequestBody priorityPart = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), notice.getPriority());
            okhttp3.RequestBody categoryPart = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), notice.getCategory());
            okhttp3.RequestBody durationPart = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), String.valueOf(notice.getDisplayDuration()));
            okhttp3.RequestBody isActivePart = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), "true");
            okhttp3.RequestBody scheduledAtPart = notice.getScheduledAt() != null ? okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), notice.getScheduledAt()) : null;
            okhttp3.RequestBody expiresAtPart = notice.getExpiresAt() != null ? okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), notice.getExpiresAt()) : null;

            Call<ApiResponse<Notice>> call = ApiClient.getApiService().updateNotice(
                    authToken, notice.getId(), titlePart, descriptionPart, priorityPart, categoryPart, durationPart, isActivePart, scheduledAtPart, expiresAtPart, null
            );

            call.enqueue(new Callback<ApiResponse<Notice>>() {
                @Override
                public void onResponse(Call<ApiResponse<Notice>> call, Response<ApiResponse<Notice>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        notice = response.body().getData();
                        Toast.makeText(NoticeDetailsActivity.this, "Notice uploaded to board successfully!", Toast.LENGTH_LONG).show();
                        btnUpload.setVisibility(View.GONE);
                        setResult(RESULT_OK);
                    } else {
                        Toast.makeText(NoticeDetailsActivity.this, "Failed to upload notice", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Notice>> call, Throwable t) {
                    Toast.makeText(NoticeDetailsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchNoticeDetails() {
        Call<ApiResponse<Notice>> call = ApiClient.getApiService().getNotice(authToken, notice.getId());
        call.enqueue(new Callback<ApiResponse<Notice>>() {
            @Override
            public void onResponse(Call<ApiResponse<Notice>> call, Response<ApiResponse<Notice>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    notice = response.body().getData();
                    displayNotice(); // Update UI
                    Toast.makeText(NoticeDetailsActivity.this, "Notice updated", Toast.LENGTH_SHORT).show();
                    
                    // Also set result OK so Dashboard refreshes if we go back later
                    setResult(RESULT_OK);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Notice>> call, Throwable t) {
                // Ignore
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notice")
                .setMessage("Are you sure you want to delete this notice? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteNotice())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNotice() {
        Call<ApiResponse<Void>> call = ApiClient.getApiService().deleteNotice(authToken, notice.getId());
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(NoticeDetailsActivity.this, "Notice deleted successfully", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(NoticeDetailsActivity.this, "Failed to delete notice", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(NoticeDetailsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}