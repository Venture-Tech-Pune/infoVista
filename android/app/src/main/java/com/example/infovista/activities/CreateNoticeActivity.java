package com.example.infovista.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.infovista.R;
import com.example.infovista.models.ApiResponse;
import com.example.infovista.models.Notice;
import com.example.infovista.network.ApiClient;
import com.example.infovista.utils.Constants;
import com.example.infovista.utils.SharedPrefManager;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateNoticeActivity extends AppCompatActivity {

    private EditText etTitle, etDescription, etScheduledAt, etExpiresAt;
    private Spinner spinnerPriority, spinnerCategory;
    private Button btnSelectImage, btnSelectVideo, btnUpload;
    private com.google.android.material.switchmaterial.SwitchMaterial switchMute;
    private ImageView ivPreview;
    private VideoView vvPreview;
    private ProgressBar progressBar;

    private Long scheduledAtTimestamp, expiresAtTimestamp;
    private Uri selectedImageUri, selectedVideoUri;

    // Preview views
    private TextView previewTitle, previewDescription, previewCategory;
    private ImageView previewImage;
    private View previewPriorityBar;

    private String authToken;
    private boolean isEditMode = false;
    private Notice noticeToEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_notice);

        authToken = "Bearer " + SharedPrefManager.getInstance(this).getToken();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Create Notice");

        // Initialize views
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etScheduledAt = findViewById(R.id.etScheduledAt);
        etExpiresAt = findViewById(R.id.etExpiresAt);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSelectVideo = findViewById(R.id.btnSelectVideo);
//        btnSubmit = findViewById(R.id.btnSubmit);
        btnUpload = findViewById(R.id.btnUpload);
        ivPreview = findViewById(R.id.ivPreview);
        vvPreview = findViewById(R.id.vvPreview);
        switchMute = findViewById(R.id.switchMute);
        progressBar = findViewById(R.id.progressBar);

        // Setup date/time pickers
        etScheduledAt.setOnClickListener(v -> showDateTimePicker(true));
        etExpiresAt.setOnClickListener(v -> showDateTimePicker(false));

        // Initialize preview views
        previewTitle = findViewById(R.id.previewTitle);
        previewDescription = findViewById(R.id.previewDescription);
        previewCategory = findViewById(R.id.previewCategory);
        previewImage = findViewById(R.id.previewImage);
        previewPriorityBar = findViewById(R.id.previewPriorityBar);

        // Setup spinners
        setupSpinners();

        // Check for edit mode
        if (getIntent().hasExtra("NOTICE_JSON")) {
            isEditMode = true;
            String json = getIntent().getStringExtra("NOTICE_JSON");
            noticeToEdit = new com.google.gson.Gson().fromJson(json, Notice.class);
            setupEditMode();
        }

        // Setup real-time preview listeners
        setupPreviewListeners();

        // Click listeners
        btnSelectImage.setOnClickListener(v -> selectImage());
        btnSelectVideo.setOnClickListener(v -> selectVideo());
//        btnSubmit.setOnClickListener(v -> submitNotice(false)); // Save Draft (Inactive)
        btnUpload.setOnClickListener(v -> submitNotice(true));  // Upload (Active)
    }

    private void setupPreviewListeners() {
        // Title change listener
        etTitle.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Description change listener
        etDescription.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Spinner listeners
        spinnerPriority.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updatePreview();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updatePreview();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void updatePreview() {
        // Update title
        String title = etTitle.getText().toString().trim();
        previewTitle.setText(title.isEmpty() ? "Notice Title" : title);

        // Update description
        String description = etDescription.getText().toString().trim();
        previewDescription.setText(description.isEmpty() ? "Notice description..." : description);

        // Update category
        String category = spinnerCategory.getSelectedItem().toString();
        previewCategory.setText("• " + category.toUpperCase());

        // Update priority bar color
        String priority = spinnerPriority.getSelectedItem().toString().toLowerCase();
        int priorityColor;
        switch (priority) {
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
        previewPriorityBar.setBackgroundColor(priorityColor);
        previewCategory.setTextColor(priorityColor);
    }

    private void setupSpinners() {
        // Priority spinner
        String[] priorities = {"Low", "Medium", "High", "Urgent"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);
        spinnerPriority.setSelection(1); // Default to Medium

        // Category spinner
        String[] categories = {"General", "Academic", "Event", "Emergency", "Announcement"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
    }

    private void setupEditMode() {
        getSupportActionBar().setTitle("Edit Notice");
//        btnSubmit.setText("Update Draft");
        btnUpload.setText("Update & Publish");

        if (noticeToEdit != null) {
            etTitle.setText(noticeToEdit.getTitle());
            etDescription.setText(noticeToEdit.getDescription());
            
            // Set spinners
            setSpinnerValue(spinnerPriority, noticeToEdit.getPriority());
            setSpinnerValue(spinnerCategory, noticeToEdit.getCategory());

            // Set schedule/expiry if available
            if (noticeToEdit.getScheduledAt() != null) {
                etScheduledAt.setText(formatDisplayDate(noticeToEdit.getScheduledAt()));
                // ISO string to timestamp if possible (omitted for brevity, will set on picker)
            }
            if (noticeToEdit.getExpiresAt() != null) {
                etExpiresAt.setText(formatDisplayDate(noticeToEdit.getExpiresAt()));
            }

            // Load existing image if available
            if (noticeToEdit.getMediaUrl() != null && !noticeToEdit.getMediaUrl().isEmpty() 
                    && "image".equals(noticeToEdit.getMediaType())) {
                ivPreview.setVisibility(View.VISIBLE);
                
                String baseUrl = Constants.BASE_URL.replace("/api/", ""); // Basic cleanup
                // Use the fix: remove leading slash if present
                String mediaUrl = noticeToEdit.getMediaUrl().startsWith("/") 
                        ? noticeToEdit.getMediaUrl().substring(1) 
                        : noticeToEdit.getMediaUrl();
                        
                String imageUrl = Constants.BASE_URL + mediaUrl;
                
                com.bumptech.glide.Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(ivPreview);
            }
        }
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        if (value == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void selectImage() {
        // Check permission based on Android version
        String permission;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    Constants.REQUEST_PERMISSION);
        } else {
            openImagePicker();
        }
    }

    private void selectVideo() {
        // Check permission based on Android version
        String permission;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_VIDEO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    Constants.REQUEST_PERMISSION + 1); // Use different request code
        } else {
            openVideoPicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, Constants.REQUEST_IMAGE_PICK);
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, Constants.REQUEST_VIDEO_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            selectedVideoUri = null;
            
            ivPreview.setVisibility(View.VISIBLE);
            vvPreview.setVisibility(View.GONE);
            switchMute.setVisibility(View.GONE);
            ivPreview.setImageURI(selectedImageUri);

            // Update preview image
            previewImage.setVisibility(View.VISIBLE);
            previewImage.setImageURI(selectedImageUri);
        } else if (requestCode == Constants.REQUEST_VIDEO_PICK && resultCode == RESULT_OK && data != null) {
            selectedVideoUri = data.getData();
            selectedImageUri = null;

            ivPreview.setVisibility(View.GONE);
            vvPreview.setVisibility(View.VISIBLE);
            switchMute.setVisibility(View.VISIBLE);
            vvPreview.setVideoURI(selectedVideoUri);
            vvPreview.start();

            // Preview image (thumbnail or placeholder for video)
            previewImage.setVisibility(View.VISIBLE);
            previewImage.setImageResource(R.drawable.ic_video_placeholder);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == Constants.REQUEST_PERMISSION + 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openVideoPicker();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDateTimePicker(boolean isSchedule) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(java.util.Calendar.YEAR, year);
            calendar.set(java.util.Calendar.MONTH, month);
            calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);

            new android.app.TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(java.util.Calendar.MINUTE, minute);
                calendar.set(java.util.Calendar.SECOND, 0);

                long timestamp = calendar.getTimeInMillis();
                long now = System.currentTimeMillis();

                if (isSchedule) {
                    if (timestamp < now - 60000) { // Allow 1 minute grace
                        Toast.makeText(this, "Schedule time cannot be in the past", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    scheduledAtTimestamp = timestamp;
                    etScheduledAt.setText(android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", calendar).toString());
                } else {
                    if (timestamp < now) {
                        Toast.makeText(this, "Expiry time cannot be in the past", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (scheduledAtTimestamp != null && timestamp <= scheduledAtTimestamp) {
                        Toast.makeText(this, "Expiry time must be after schedule time", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    expiresAtTimestamp = timestamp;
                    etExpiresAt.setText(android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", calendar).toString());
                }
            }, calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE), true).show();
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private String formatDisplayDate(String isoString) {
        try {
            java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = isoFormat.parse(isoString);
            java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            return displayFormat.format(date);
        } catch (Exception e) {
            return isoString;
        }
    }

    private String toIsoString(Long timestamp) {
        if (timestamp == null) return null;
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date(timestamp));
    }

    private void submitNotice(boolean isActive) {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priority = spinnerPriority.getSelectedItem().toString().toLowerCase();
        String category = spinnerCategory.getSelectedItem().toString().toLowerCase();

        // Validation
        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        // Hardcoded duration
        int duration = 10; 

        setLoading(true);

        // Prepare request parts
        RequestBody titlePart = RequestBody.create(MediaType.parse("text/plain"), title);
        RequestBody descriptionPart = RequestBody.create(MediaType.parse("text/plain"), description);
        RequestBody priorityPart = RequestBody.create(MediaType.parse("text/plain"), priority);
        RequestBody categoryPart = RequestBody.create(MediaType.parse("text/plain"), category);
        RequestBody durationPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(duration));
        RequestBody isActivePart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(isActive));
        RequestBody isMutedPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(switchMute.isChecked()));

        String scheduledAtIso = toIsoString(scheduledAtTimestamp);
        String expiresAtIso = toIsoString(expiresAtTimestamp);

        RequestBody scheduledAtPart = scheduledAtIso != null ? RequestBody.create(MediaType.parse("text/plain"), scheduledAtIso) : null;
        RequestBody expiresAtPart = expiresAtIso != null ? RequestBody.create(MediaType.parse("text/plain"), expiresAtIso) : null;

        MultipartBody.Part mediaPart = null;
        Uri selectedUri = selectedImageUri != null ? selectedImageUri : selectedVideoUri;
        
        if (selectedUri != null) {
            // Use FileUtils to get actual file from content URI
            File file = com.example.infovista.utils.FileUtils.getFileFromUri(this, selectedUri);
            if (file != null && file.exists()) {
                // Get actual MIME type from content resolver
                String mimeType = getContentResolver().getType(selectedUri);
                if (mimeType == null) {
                    mimeType = selectedImageUri != null ? "image/jpeg" : "video/mp4"; // Default fallback
                }
                RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
                mediaPart = MultipartBody.Part.createFormData("media", file.getName(), requestFile);
            } else {
                Toast.makeText(this, "Failed to process media", Toast.LENGTH_SHORT).show();
                setLoading(false);
                return;
            }
        }

        // API call
        Call<ApiResponse<Notice>> call;
        if (isEditMode) {
            call = ApiClient.getApiService().updateNotice(
                    authToken, noticeToEdit.getId(), titlePart, descriptionPart, priorityPart, categoryPart, durationPart, isActivePart, isMutedPart, scheduledAtPart, expiresAtPart, mediaPart
            );
        } else {
            call = ApiClient.getApiService().createNotice(
                    authToken, titlePart, descriptionPart, priorityPart, categoryPart, durationPart, isActivePart, isMutedPart, scheduledAtPart, expiresAtPart, mediaPart
            );
        }

        call.enqueue(new Callback<ApiResponse<Notice>>() {
            @Override
            public void onResponse(Call<ApiResponse<Notice>> call, Response<ApiResponse<Notice>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Notice> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        Notice createdNotice = apiResponse.getData();
                        String msg = isActive ? "Notice published successfully!" : "Draft saved successfully!";
                        if (isEditMode) msg = "Notice updated successfully!";
                        
                        Toast.makeText(CreateNoticeActivity.this, msg, Toast.LENGTH_SHORT).show();
                        
                        if (!isEditMode) {
                            // Navigate/finish as appropriate. Maybe just finish.
                            // If user clicked Upload, we might want to still go to details?
                            // For simplicity, let's just finish as it returns to dashboard list logic usually.
                            // But original code went to details. Let's keep that only if needed.
                            // Actually user said: "when i click on the save draft it gets upload it should not happen... only for unsheduled not for schedule"
                            // Wait, the user mentioned scheduling. If scheduled, isActive might still be true but effective status pending.
                            // However, assuming isActive=false covers "Draft".
                        }
                        
                        setResult(RESULT_OK); // Signal success
                        finish(); // Go back
                    } else {
                        Toast.makeText(CreateNoticeActivity.this,
                                apiResponse.getMessage() != null ? apiResponse.getMessage() : "Operation failed",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CreateNoticeActivity.this, "Operation failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Notice>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CreateNoticeActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
//        btnSubmit.setEnabled(!loading);
        btnUpload.setEnabled(!loading);
        btnSelectImage.setEnabled(!loading);
        etTitle.setEnabled(!loading);
        etDescription.setEnabled(!loading);
        spinnerPriority.setEnabled(!loading);
        spinnerCategory.setEnabled(!loading);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
