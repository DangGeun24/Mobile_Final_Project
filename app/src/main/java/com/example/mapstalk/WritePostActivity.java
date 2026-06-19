package com.example.mapstalk;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mapstalk.databinding.ActivityWritePostBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class WritePostActivity extends AppCompatActivity {

    private ActivityWritePostBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String placeId;

    private String postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWritePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        placeId = getIntent().getStringExtra("EXTRA_PLACE_ID");
        postId = getIntent().getStringExtra("EXTRA_POST_ID");

        if (postId != null) {
            binding.btnWritePost.setText("수정 완료");
            loadPostData();
        }

        binding.btnWritePost.setOnClickListener(v -> {
            String title = binding.etTitle.getText().toString().trim();
            String content = binding.etContent.getText().toString().trim();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser == null) {
                Toast.makeText(this, "로그인이 필요한 기능입니다.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "제목과 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (placeId == null) {
                placeId = "TEST_PLACE_ID_12345";
            }

            Map<String, Object> postMap = new HashMap<>();
            postMap.put("title", title);
            postMap.put("content", content);

            if (postId == null) {
                // 새 글 작성
                postMap.put("placeId", placeId);
                postMap.put("authorUid", currentUser.getUid());
                postMap.put("timestamp", Timestamp.now());

                db.collection("Posts")
                        .add(postMap)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, "게시글이 작성되었습니다.", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "작성 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                // 기존 글 수정
                db.collection("Posts").document(postId)
                        .update(postMap)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "게시글이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadPostData() {
        db.collection("Posts").document(postId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        binding.etTitle.setText(document.getString("title"));
                        binding.etContent.setText(document.getString("content"));
                    }
                });
    }
}

