package com.example.infovista.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.infovista.R;
import com.example.infovista.adapters.BoardNoticeAdapter;
import com.example.infovista.models.ApiResponse;
import com.example.infovista.models.Notice;
import com.example.infovista.network.ApiClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NoticeBoardPreviewActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BoardNoticeAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyState;
    private ProgressBar progressBar;
    private TextView tvNoticeCount, tvTime;
    private TextView tvEmptyTime, tvEmptyDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice_board_preview);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);
        tvNoticeCount = findViewById(R.id.tvNoticeCount);
        tvTime = findViewById(R.id.tvTime);
        tvEmptyTime = findViewById(R.id.tvEmptyTime);
        tvEmptyDate = findViewById(R.id.tvEmptyDate);

        // Update time
        updateTime();
        updateEmptyStateTime();

        // Setup RecyclerView
        adapter = new BoardNoticeAdapter();
        recyclerView.setAdapter(adapter);

        // Setup swipe refresh
        swipeRefresh.setOnRefreshListener(this::loadNotices);

        // Load notices
        loadNotices();
    }

    private void updateTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
        tvTime.setText(sdf.format(new java.util.Date()));

        // Update time every minute
        tvTime.postDelayed(this::updateTime, 60000);
    }

    private void updateEmptyStateTime() {
        java.util.Date now = new java.util.Date();
        
        // Update large clock time
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
        tvEmptyTime.setText(timeFormat.format(now));
        
        // Update date
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("EEEE, MMMM dd, yyyy", java.util.Locale.getDefault());
        tvEmptyDate.setText(dateFormat.format(now));
        
        // Update every second for clock
        tvEmptyTime.postDelayed(this::updateEmptyStateTime, 1000);
    }

    private void loadNotices() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        Call<ApiResponse<List<Notice>>> call = ApiClient.getApiService().getActiveNotices();
        call.enqueue(new Callback<ApiResponse<List<Notice>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Notice>>> call, Response<ApiResponse<List<Notice>>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Notice> notices = response.body().getData();

                    if (notices == null || notices.isEmpty()) {
                        tvNoticeCount.setText("0 Active Notices");
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        // Update notice count
                        int count = notices.size();
                        tvNoticeCount.setText(count + " Active Notice" + (count != 1 ? "s" : ""));

                        emptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);

                        // Calculate grid columns based on notice count
                        int columns = calculateColumns(notices.size());
                        recyclerView.setLayoutManager(new GridLayoutManager(NoticeBoardPreviewActivity.this, columns));

                        adapter.setNotices(notices);
                    }
                } else {
                    Toast.makeText(NoticeBoardPreviewActivity.this, "Failed to load notices", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Notice>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(NoticeBoardPreviewActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int calculateColumns(int noticeCount) {
        if (noticeCount == 0 || noticeCount == 1) {
            return 1;
        } else if (noticeCount == 2) {
            return 2;
        } else if (noticeCount <= 4) {
            return 2;
        } else {
            return 3; // 3 columns for 5+ notices
        }
    }
}
