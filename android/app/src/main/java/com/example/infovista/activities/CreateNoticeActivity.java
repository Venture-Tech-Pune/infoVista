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
import android.widget.TextView;
import android.widget.Toast;

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

    private EditText etTitle, etDescription, etDuration;
    private Spinner spinnerPriority, spinnerCategory;
    private Button btnSelectImage, btnSubmit;
    private ImageView ivPreview;
    private ProgressBar progressBar;

    // Preview views
    private TextView previewTitle, previewDescription, previewCategory, previewDuration;
    private ImageView previewImage;
    private View previewPriorityBar;

    private Uri selectedImageUri;
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
        etDuration = findViewById(R.id.etDuration);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSubmit = findViewById(R.id.btnSubmit);
        ivPreview = findViewById(R.id.ivPreview);
        progressBar = findViewById(R.id.progressBar);

        // Initialize preview views
        previewTitle = findViewById(R.id.previewTitle);
        previewDescription = findViewById(R.id.previewDescription);
        previewCategory = findViewById(R.id.previewCategory);
        previewDuration = findViewById(R.id.previewDuration);
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
        btnSubmit.setOnClickListener(v -> submitNotice());
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

        // Duration change listener
        etDuration.addTextChangedListener(new android.text.TextWatcher() {
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

        // Update duration
        String durationStr = etDuration.getText().toString().trim();
        String duration = durationStr.isEmpty() ? "10" : durationStr;
        previewDuration.setText(duration + "s");

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
        btnSubmit.setText("Update Notice");

        if (noticeToEdit != null) {
            etTitle.setText(noticeToEdit.getTitle());
            etDescription.setText(noticeToEdit.getDescription());
            etDuration.setText(String.valueOf(noticeToEdit.getDisplayDuration()));

            // Set spinners
            setSpinnerValue(spinnerPriority, noticeToEdit.getPriority());
            setSpinnerValue(spinnerCategory, noticeToEdit.getCategory());

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

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, Constants.REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            ivPreview.setVisibility(View.VISIBLE);
            ivPreview.setImageURI(selectedImageUri);

            // Update preview image
            previewImage.setVisibility(View.VISIBLE);
            previewImage.setImageURI(selectedImageUri);
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
        }
    }

    private void submitNotice() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();
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

        int duration = 10; // Default
        if (!durationStr.isEmpty()) {
            duration = Integer.parseInt(durationStr);
        }

        setLoading(true);

        // Prepare request parts
        RequestBody titlePart = RequestBody.create(MediaType.parse("text/plain"), title);
        RequestBody descriptionPart = RequestBody.create(MediaType.parse("text/plain"), description);
        RequestBody priorityPart = RequestBody.create(MediaType.parse("text/plain"), priority);
        RequestBody categoryPart = RequestBody.create(MediaType.parse("text/plain"), category);
        RequestBody durationPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(duration));
        RequestBody isActivePart = RequestBody.create(MediaType.parse("text/plain"), isEditMode ? "true" : "false");

        MultipartBody.Part mediaPart = null;
        if (selectedImageUri != null) {
            // Use FileUtils to get actual file from content URI
            File file = com.example.infovista.utils.FileUtils.getFileFromUri(this, selectedImageUri);
            if (file != null && file.exists()) {
                // Get actual MIME type from content resolver
                String mimeType = getContentResolver().getType(selectedImageUri);
                if (mimeType == null) {
                    mimeType = "image/jpeg"; // Default fallback
                }
                RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
                mediaPart = MultipartBody.Part.createFormData("media", file.getName(), requestFile);
            } else {
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                setLoading(false);
                return;
            }
        }

        // API call
        Call<ApiResponse<Notice>> call;
        if (isEditMode) {
            call = ApiClient.getApiService().updateNotice(
                    authToken, noticeToEdit.getId(), titlePart, descriptionPart, priorityPart, categoryPart, durationPart, isActivePart, mediaPart
            );
        } else {
            call = ApiClient.getApiService().createNotice(
                    authToken, titlePart, descriptionPart, priorityPart, categoryPart, durationPart, isActivePart, mediaPart
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
                        String msg = isEditMode ? "Notice updated successfully!" : "Draft saved successfully!";
                        Toast.makeText(CreateNoticeActivity.this, msg, Toast.LENGTH_SHORT).show();
                        
                        if (!isEditMode) {
                            // Navigate to NoticeDetailsActivity for upload
                            Intent intent = new Intent(CreateNoticeActivity.this, NoticeDetailsActivity.class);
                            intent.putExtra("NOTICE_JSON", new com.google.gson.Gson().toJson(createdNotice));
                            startActivity(intent);
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
        btnSubmit.setEnabled(!loading);
        btnSelectImage.setEnabled(!loading);
        etTitle.setEnabled(!loading);
        etDescription.setEnabled(!loading);
        etDuration.setEnabled(!loading);
        spinnerPriority.setEnabled(!loading);
        spinnerCategory.setEnabled(!loading);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
