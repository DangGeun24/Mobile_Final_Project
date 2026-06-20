package com.example.mapstalk;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class StoreInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_info);

        // MainActivity에서 넘겨받은 데이터
        String placeId = getIntent().getStringExtra("PLACE_ID");
        String placeName = getIntent().getStringExtra("PLACE_NAME");
        String address = getIntent().getStringExtra("ADDRESS");
        double latitude = getIntent().getDoubleExtra("LATITUDE", 0);
        double longitude = getIntent().getDoubleExtra("LONGITUDE", 0);
        
        if (placeId == null || placeId.isEmpty()) {
            placeId = placeName;
        }

        final String finalPlaceId = placeId;
        final String finalPlaceName = placeName;

        // UI 표시
        TextView tvPlaceNameView = findViewById(R.id.tvPlaceName);
        TextView tvAddressView = findViewById(R.id.tvAddress);
        TextView tvLatLngView = findViewById(R.id.tvLatLng);
        android.widget.LinearLayout layoutCustomPlaceActions = findViewById(R.id.layoutCustomPlaceActions);
        Button btnEditPlace = findViewById(R.id.btnEditPlace);
        Button btnDeletePlace = findViewById(R.id.btnDeletePlace);

        tvPlaceNameView.setText(placeName != null ? placeName : "장소명 없음");
        tvAddressView.setText(address != null ? address : "주소 없음");
        tvLatLngView.setText("위도: " + latitude + "\n경도: " + longitude);

        // 커스텀 장소 소유자 확인
        if (finalPlaceId.startsWith("custom_")) {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("CustomPlaces").document(finalPlaceId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String creatorUid = documentSnapshot.getString("creatorUid");
                                if (user.getUid().equals(creatorUid)) {
                                    layoutCustomPlaceActions.setVisibility(android.view.View.VISIBLE);
                                    
                                    btnEditPlace.setOnClickListener(v -> showEditPlaceDialog(finalPlaceId, tvPlaceNameView));
                                    btnDeletePlace.setOnClickListener(v -> showDeletePlaceDialog(finalPlaceId));
                                }
                            }
                        });
            }
        }

        // 게시판 이동 버튼 (목록 화면으로 연결)
        Button btnGoBoard = findViewById(R.id.btnGoBoard);
        
        btnGoBoard.setOnClickListener(v -> {
            Intent intent = new Intent(StoreInfoActivity.this, PlaceBoardActivity.class);
            intent.putExtra("EXTRA_PLACE_ID", finalPlaceId);
            intent.putExtra("EXTRA_PLACE_NAME", finalPlaceName);
            startActivity(intent);
        });
    }

    private void showEditPlaceDialog(String placeId, TextView tvPlaceName) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("장소 이름 수정");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(tvPlaceName.getText().toString());
        input.setSelection(input.getText().length());
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("수정", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(tvPlaceName.getText().toString())) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("CustomPlaces").document(placeId)
                        .update("name", newName)
                        .addOnSuccessListener(aVoid -> {
                            android.widget.Toast.makeText(this, "장소 이름이 수정되었습니다.", android.widget.Toast.LENGTH_SHORT).show();
                            tvPlaceName.setText(newName);
                        });
            }
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeletePlaceDialog(String placeId) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("장소 삭제")
            .setMessage("정말로 이 장소를 삭제하시겠습니까?\n(이 장소의 게시판은 검색되지 않습니다.)")
            .setPositiveButton("삭제", (dialog, which) -> {
                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("CustomPlaces").document(placeId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            android.widget.Toast.makeText(this, "장소가 삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show();
                            finish(); // 액티비티 닫기
                        });
            })
            .setNegativeButton("취소", (dialog, which) -> dialog.cancel())
            .show();
    }
}
