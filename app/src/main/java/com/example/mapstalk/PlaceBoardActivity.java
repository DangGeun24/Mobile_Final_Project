package com.example.mapstalk;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlaceBoardActivity extends AppCompatActivity {

    private RecyclerView rvPosts;
    private Button btnWritePost;
    private TextView tvBoardTitle;
    private LinearLayout paginationContainer;
    
    private PostAdapter adapter;
    private List<Map<String, Object>> allPostList = new ArrayList<>();
    private List<Map<String, Object>> displayPostList = new ArrayList<>();
    private FirebaseFirestore db;
    private String placeId;

    private int currentPage = 0; // 0-indexed internally
    private static final int PAGE_SIZE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_board);

        placeId = getIntent().getStringExtra("EXTRA_PLACE_ID");
        String placeName = getIntent().getStringExtra("EXTRA_PLACE_NAME");
        
        if (placeId == null) {
            placeId = "UNKNOWN_PLACE";
        }
        if (placeName == null || placeName.isEmpty()) {
            placeName = placeId;
        }

        db = FirebaseFirestore.getInstance();

        tvBoardTitle = findViewById(R.id.tvBoardTitle);
        rvPosts = findViewById(R.id.rvPosts);
        btnWritePost = findViewById(R.id.btnWritePost);
        paginationContainer = findViewById(R.id.paginationContainer);

        tvBoardTitle.setText(placeName + " 게시판");

        adapter = new PostAdapter(displayPostList);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(adapter);

        btnWritePost.setOnClickListener(v -> {
            Intent intent = new Intent(PlaceBoardActivity.this, WritePostActivity.class);
            intent.putExtra("EXTRA_PLACE_ID", placeId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPostsFromFirebase();
    }

    private void fetchPostsFromFirebase() {
        db.collection("Posts")
                .whereEqualTo("placeId", placeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allPostList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> data = document.getData();
                        data.put("postId", document.getId());
                        allPostList.add(data);
                    }
                    
                    if (!allPostList.isEmpty()) {
                        // 메모리상에서 최신순으로 정렬
                        allPostList.sort((p1, p2) -> {
                            Object t1 = p1.get("timestamp");
                            Object t2 = p2.get("timestamp");
                            if (t1 instanceof com.google.firebase.Timestamp && t2 instanceof com.google.firebase.Timestamp) {
                                return ((com.google.firebase.Timestamp) t2).compareTo((com.google.firebase.Timestamp) t1);
                            }
                            return 0;
                        });

                        // 글 고유 번호(No.) 부여 (최신 글이 가장 큰 번호)
                        int totalPosts = allPostList.size();
                        for (int i = 0; i < totalPosts; i++) {
                            allPostList.get(i).put("postNumber", totalPosts - i);
                        }
                        
                        currentPage = 0;
                        loadPageLocally(currentPage);
                        buildPaginationUI();
                    } else {
                        // 빈 화면 처리
                        currentPage = 0;
                        displayPostList.clear();
                        adapter.notifyDataSetChanged();
                        paginationContainer.removeAllViews();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "글을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    Log.e("PlaceBoard", "Error loading posts", e);
                });
    }

    private void loadPageLocally(int pageIndex) {
        displayPostList.clear();
        int startIndex = pageIndex * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, allPostList.size());
        
        if (startIndex < allPostList.size()) {
            displayPostList.addAll(allPostList.subList(startIndex, endIndex));
        }
        adapter.notifyDataSetChanged();
        rvPosts.scrollToPosition(0);
    }

    private void buildPaginationUI() {
        paginationContainer.removeAllViews();
        int totalPages = (int) Math.ceil((double) allPostList.size() / PAGE_SIZE);

        for (int i = 0; i < totalPages; i++) {
            final int pageIndex = i;
            TextView tvPage = new TextView(this);
            tvPage.setText(String.valueOf(i + 1));
            tvPage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tvPage.setGravity(Gravity.CENTER);
            
            int padding = (int) (12 * getResources().getDisplayMetrics().density);
            tvPage.setPadding(padding, padding, padding, padding);

            if (i == currentPage) {
                tvPage.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                tvPage.setTypeface(null, Typeface.BOLD);
            } else {
                tvPage.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                tvPage.setTypeface(null, Typeface.NORMAL);
            }

            tvPage.setOnClickListener(v -> {
                currentPage = pageIndex;
                loadPageLocally(currentPage);
                buildPaginationUI(); // 번호 색상 업데이트를 위해 다시 그리기
            });

            paginationContainer.addView(tvPage);
        }
    }

}
