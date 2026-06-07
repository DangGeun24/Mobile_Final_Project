package com.example.mapstalk;

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
        String placeName = getIntent().getStringExtra("PLACE_NAME");
        String address = getIntent().getStringExtra("ADDRESS");
        double latitude = getIntent().getDoubleExtra("LATITUDE", 0);
        double longitude = getIntent().getDoubleExtra("LONGITUDE", 0);

        // UI 표시
        TextView tvPlaceName = findViewById(R.id.tvPlaceName);
        TextView tvAddress = findViewById(R.id.tvAddress);
        TextView tvLatLng = findViewById(R.id.tvLatLng);

        tvPlaceName.setText(placeName != null ? placeName : "장소명 없음");
        tvAddress.setText(address != null ? address : "주소 없음");
        tvLatLng.setText("위도: " + latitude + "\n경도: " + longitude);

        // 게시판 이동 버튼 (C팀원 연결 예정)
        Button btnGoBoard = findViewById(R.id.btnGoBoard);
        btnGoBoard.setOnClickListener(v -> {
            // TODO: C팀원 StoreBoardActivity 연결 예정
        });
    }
}