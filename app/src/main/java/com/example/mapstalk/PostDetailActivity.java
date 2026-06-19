package com.example.mapstalk;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PostDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvDate, tvContent;
    private Button btnEdit, btnDelete;
    
    private FirebaseFirestore db;
    private String postId;
    private String placeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        db = FirebaseFirestore.getInstance();
        postId = getIntent().getStringExtra("EXTRA_POST_ID");

        tvTitle = findViewById(R.id.tvTitle);
        tvDate = findViewById(R.id.tvDate);
        tvContent = findViewById(R.id.tvContent);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        if (postId == null) {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnDelete.setOnClickListener(v -> deletePost());
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, WritePostActivity.class);
            intent.putExtra("EXTRA_POST_ID", postId);
            intent.putExtra("EXTRA_PLACE_ID", placeId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPostDetail();
    }

    private void loadPostDetail() {
        db.collection("Posts").document(postId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String title = document.getString("title");
                        String content = document.getString("content");
                        String authorUid = document.getString("authorUid");
                        placeId = document.getString("placeId");
                        
                        tvTitle.setText(title != null ? title : "제목 없음");
                        tvContent.setText(content != null ? content : "내용 없음");

                        Timestamp timestamp = document.getTimestamp("timestamp");
                        if (timestamp != null) {
                            Date date = timestamp.toDate();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                            tvDate.setText(sdf.format(date));
                        }

                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null && user.getUid().equals(authorUid)) {
                            btnEdit.setVisibility(View.VISIBLE);
                            btnDelete.setVisibility(View.VISIBLE);
                        } else {
                            btnEdit.setVisibility(View.GONE);
                            btnDelete.setVisibility(View.GONE);
                        }
                    } else {
                        Toast.makeText(this, "삭제되었거나 존재하지 않는 글입니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "글을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void deletePost() {
        new AlertDialog.Builder(this)
                .setTitle("삭제 확인")
                .setMessage("정말 이 글을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    db.collection("Posts").document(postId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("취소", null)
                .show();
    }
}
