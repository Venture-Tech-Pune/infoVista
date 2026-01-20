package com.example.infovista.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.infovista.R;
import com.example.infovista.adapters.NoticeAdapter;
import com.example.infovista.models.ApiResponse;
import com.example.infovista.models.Notice;
import com.example.infovista.network.ApiClient;
import com.example.infovista.utils.Constants;
import com.example.infovista.utils.SharedPrefManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NoticeAdapter noticeAdapter;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvWelcome;
    private SwipeRefreshLayout swipeRefresh;
    private FloatingActionButton fabCreate;
    
    private SharedPrefManager prefManager;
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        prefManager = SharedPrefManager.getInstance(this);
        authToken = "Bearer " + prefManager.getToken();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvWelcome = findViewById(R.id.tvWelcome);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        fabCreate = findViewById(R.id.fabCreate);

        // Welcome message
        tvWelcome.setText("Welcome, " + prefManager.getUserName());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        noticeAdapter = new NoticeAdapter(this, new ArrayList<>(), notice -> {
            // Open notice details
            Intent intent = new Intent(DashboardActivity.this, NoticeDetailsActivity.class);
            intent.putExtra("NOTICE_JSON", new Gson().toJson(notice));
            startActivity(intent);
        });
        recyclerView.setAdapter(noticeAdapter);

        // FAB click listener
        fabCreate.setOnClickListener(v -> {
            String role = prefManager.getUserRole();
            if (role.equals(Constants.ROLE_ADMIN) || role.equals(Constants.ROLE_MANAGER)) {
                startActivity(new Intent(DashboardActivity.this, CreateNoticeActivity.class));
            } else {
                Toast.makeText(this, "Only Admin and Manager can create notices", Toast.LENGTH_SHORT).show();
            }
        });

        // Board Preview card click listener
        findViewById(R.id.cardBoardPreview).setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, NoticeBoardPreviewActivity.class));
        });

        // Swipe to refresh
        swipeRefresh.setOnRefreshListener(() -> loadNotices());

        // Load notices
        loadNotices();
    }

    private void loadNotices() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        Map<String, String> params = new HashMap<>();
        params.put("page", "1");
        params.put("limit", "50");

        Call<ApiResponse<List<Notice>>> call = ApiClient.getApiService().getNotices(authToken, params);
        call.enqueue(new Callback<ApiResponse<List<Notice>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Notice>>> call, Response<ApiResponse<List<Notice>>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Notice>> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        List<Notice> notices = apiResponse.getData();

                        if (notices.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            noticeAdapter.updateNotices(notices);
                        }
                    }
                } else {
                    Toast.makeText(DashboardActivity.this, "Failed to load notices", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Notice>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                tvEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(DashboardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotices(); // Refresh on resume
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            prefManager.logout();
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.action_refresh) {
            loadNotices();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
