package com.example.mapstalk;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private EditText etSearch;
    private Button btnSearch;
    private RecyclerView recyclerView;
    private PlaceAdapter adapter;
    private List<PlaceItem> placeList = new ArrayList<>();
    private PlacesClient placesClient;

    private static final int LOCATION_PERMISSION_REQUEST = 1000;

    // 한국 범위 좌표
    private static final RectangularBounds KOREA_BOUNDS =
            RectangularBounds.newInstance(
                    new LatLng(33.0, 124.5),  // 남서쪽
                    new LatLng(38.9, 131.9)   // 북동쪽
            );

    // 한국 중심 좌표
    private static final LatLng KOREA_CENTER = new LatLng(36.5, 127.5);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Places API 초기화
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "YOUR_API_KEY");
        }
        placesClient = Places.createClient(this);

        // UI 연결
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        recyclerView = findViewById(R.id.recyclerView);

        // RecyclerView 설정
        adapter = new PlaceAdapter(placeList, item -> {
            Intent intent = new Intent(MainActivity.this, StoreInfoActivity.class);
            intent.putExtra("PLACE_ID", item.getPlaceId());
            intent.putExtra("PLACE_NAME", item.getName());
            intent.putExtra("ADDRESS", item.getAddress());
            intent.putExtra("LATITUDE", item.getLatitude());
            intent.putExtra("LONGITUDE", item.getLongitude());
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 지도 초기화
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // 검색 버튼 클릭
        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                searchPlaces(query);
            } else {
                Toast.makeText(this, "검색어를 입력해주세요", Toast.LENGTH_SHORT).show();
            }
        });

        // 위치 권한 요청
        checkLocationPermission();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // 기본 위치: 한국 중심
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(KOREA_CENTER, 7f));

        // 위치 권한 있으면 내 위치 버튼 활성화
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        // 지도 길게 누르면 마커 직접 추가
        mMap.setOnMapLongClickListener(latLng -> {
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("내가 추가한 장소")
                    .snippet(latLng.latitude + ", " + latLng.longitude));
            Toast.makeText(this, "마커가 추가되었습니다!", Toast.LENGTH_SHORT).show();
        });

        // 마커 클릭 시 StoreInfoActivity로 이동
        mMap.setOnMarkerClickListener(marker -> {
            Intent intent = new Intent(MainActivity.this, StoreInfoActivity.class);
            intent.putExtra("PLACE_NAME", marker.getTitle());
            intent.putExtra("ADDRESS", marker.getSnippet());
            intent.putExtra("LATITUDE", marker.getPosition().latitude);
            intent.putExtra("LONGITUDE", marker.getPosition().longitude);
            startActivity(intent);
            return true;
        });
    }

    // Places API 검색 - 한국으로 제한
    private void searchPlaces(String query) {
        FindAutocompletePredictionsRequest request =
                FindAutocompletePredictionsRequest.builder()
                        .setQuery(query)
                        .setCountries("KR")                  // 한국으로 제한
                        .setLocationRestriction(KOREA_BOUNDS) // 한국 범위로 제한
                        .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    placeList.clear();
                    mMap.clear();

                    response.getAutocompletePredictions().forEach(prediction -> {
                        String placeId = prediction.getPlaceId();
                        String placeName = prediction.getPrimaryText(null).toString();
                        String placeAddress = prediction.getSecondaryText(null).toString();

                        List<Place.Field> fields = Arrays.asList(
                                Place.Field.ID,
                                Place.Field.NAME,
                                Place.Field.ADDRESS,
                                Place.Field.LAT_LNG
                        );
                        FetchPlaceRequest fetchRequest =
                                FetchPlaceRequest.newInstance(placeId, fields);

                        placesClient.fetchPlace(fetchRequest)
                                .addOnSuccessListener(fetchResponse -> {
                                    Place place = fetchResponse.getPlace();
                                    LatLng latLng = place.getLatLng();

                                    if (latLng != null) {
                                        mMap.addMarker(new MarkerOptions()
                                                .position(latLng)
                                                .title(placeName)
                                                .snippet(placeAddress));

                                        PlaceItem item = new PlaceItem(
                                                placeId, placeName, placeAddress,
                                                latLng.latitude, latLng.longitude);
                                        placeList.add(item);
                                        adapter.notifyDataSetChanged();

                                        // 첫 결과로 카메라 이동
                                        if (placeList.size() == 1) {
                                            mMap.moveCamera(CameraUpdateFactory
                                                    .newLatLngZoom(latLng, 14f));
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("PLACES_ERROR",
                                            "장소 상세 조회 실패: " + e.toString());
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "검색 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    android.util.Log.e("PLACES_ERROR", "검색 실패 상세: " + e.toString());
                });
    }

    // 위치 권한 체크
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                    }
                }
            }
        }
    }
}