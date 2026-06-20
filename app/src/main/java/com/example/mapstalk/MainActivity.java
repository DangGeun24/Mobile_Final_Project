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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
    private Button btnSearch, btnMyPage;
    private RecyclerView recyclerView;
    private PlaceAdapter adapter;
    private List<PlaceItem> placeList = new ArrayList<>();
    private PlacesClient placesClient;
    private FusedLocationProviderClient fusedLocationClient;

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
            Places.initialize(getApplicationContext(), "AIzaSyBxQosSU1zNzFjVnB50Cd75mwj3Wmq72dc");
        }
        placesClient = Places.createClient(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // UI 연결
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnMyPage = findViewById(R.id.btnMyPage);
        recyclerView = findViewById(R.id.recyclerView);

        // RecyclerView 설정
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlaceAdapter(placeList, item -> {
            Intent intent = new Intent(MainActivity.this, StoreInfoActivity.class);
            intent.putExtra("PLACE_ID", item.getPlaceId());
            intent.putExtra("PLACE_NAME", item.getName());
            intent.putExtra("ADDRESS", item.getAddress());
            intent.putExtra("LATITUDE", item.getLatitude());
            intent.putExtra("LONGITUDE", item.getLongitude());
            startActivity(intent);
        });
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

        // 마이페이지 버튼 클릭
        btnMyPage.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MyPageActivity.class);
            startActivity(intent);
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

        // 지도 길게 누르면 마커 직접 추가 (이름 입력창 띄우기)
        mMap.setOnMapLongClickListener(latLng -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("새로운 장소 추가");
            builder.setMessage("이 장소의 이름을 입력해주세요.");
            
            final android.widget.EditText input = new android.widget.EditText(this);
            input.setHint("예: 우리집, 비밀 아지트");
            
            android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setPadding(50, 40, 50, 10);
            layout.addView(input);
            builder.setView(layout);

            builder.setPositiveButton("추가", (dialog, which) -> {
                String placeName = input.getText().toString().trim();
                if (placeName.isEmpty()) {
                    placeName = "내가 추가한 장소";
                }
                String customId = "custom_" + latLng.latitude + "_" + latLng.longitude;
                com.google.android.gms.maps.model.Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(placeName)
                        .snippet("클릭하여 게시판을 확인하세요"));
                marker.setTag(customId);
                marker.showInfoWindow();
                Toast.makeText(this, placeName + " 마커가 추가되었습니다!", Toast.LENGTH_SHORT).show();

                // Firestore에 커스텀 장소 영구 저장
                com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    java.util.Map<String, Object> placeData = new java.util.HashMap<>();
                    placeData.put("placeId", customId);
                    placeData.put("name", placeName);
                    placeData.put("address", "사용자 직접 추가 장소");
                    placeData.put("latitude", latLng.latitude);
                    placeData.put("longitude", latLng.longitude);
                    placeData.put("creatorUid", user.getUid());
                    placeData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

                    com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("CustomPlaces").document(customId).set(placeData);
                }
            });
            builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        // 마커 클릭 시 StoreInfoActivity로 이동
        mMap.setOnMarkerClickListener(marker -> {
            Intent intent = new Intent(MainActivity.this, StoreInfoActivity.class);
            String placeId = (marker.getTag() != null) ? marker.getTag().toString() : marker.getTitle();
            intent.putExtra("PLACE_ID", placeId);
            intent.putExtra("PLACE_NAME", marker.getTitle());
            intent.putExtra("ADDRESS", marker.getSnippet());
            intent.putExtra("LATITUDE", marker.getPosition().latitude);
            intent.putExtra("LONGITUDE", marker.getPosition().longitude);
            startActivity(intent);
            return true;
        });

        // 지도 기본 아이콘(POI - 식당, 정류장 등) 클릭 이벤트 추가
        mMap.setOnPoiClickListener(poi -> {
            List<Place.Field> fields = Arrays.asList(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.ADDRESS,
                    Place.Field.LAT_LNG
            );
            FetchPlaceRequest fetchRequest = FetchPlaceRequest.newInstance(poi.placeId, fields);
            
            placesClient.fetchPlace(fetchRequest)
                    .addOnSuccessListener(fetchResponse -> {
                        Place place = fetchResponse.getPlace();
                        LatLng latLng = place.getLatLng();

                        if (latLng != null) {
                            placeList.clear();
                            mMap.clear();
                            
                            com.google.android.gms.maps.model.Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(place.getName())
                                    .snippet(place.getAddress()));
                            marker.setTag(place.getId());
                            marker.showInfoWindow();

                            PlaceItem item = new PlaceItem(
                                    place.getId(), place.getName(), place.getAddress(),
                                    latLng.latitude, latLng.longitude);
                            placeList.add(item);
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "장소 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    // 통합 검색 로직 (Firestore + Google Places)
    private void searchPlaces(String query) {
        mMap.clear();
        placeList.clear();
        adapter.notifyDataSetChanged();

        // 1. 내가 추가한 장소(Firestore) 검색
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection("CustomPlaces")
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThan("name", query + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int customPlaceCount = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String pId = doc.getString("placeId");
                        String pName = doc.getString("name");
                        String pAddress = doc.getString("address");
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");

                        if (lat != null && lng != null) {
                            com.google.android.gms.maps.model.LatLng latLng = new com.google.android.gms.maps.model.LatLng(lat, lng);
                            com.google.android.gms.maps.model.Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(pName)
                                    .snippet(pAddress));
                            marker.setTag(pId);
                            
                            placeList.add(new PlaceItem(pId, pName, pAddress, lat, lng));
                            customPlaceCount++;
                        }
                    }
                    if (customPlaceCount > 0) {
                        adapter.notifyDataSetChanged();
                        // 첫 번째 커스텀 장소로 카메라 이동
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(placeList.get(0).getLatitude(), placeList.get(0).getLongitude()), 14f));
                    }
                    
                    // 2. Google Places API 검색 실행
                    searchGooglePlaces(query, customPlaceCount == 0);
                })
                .addOnFailureListener(e -> {
                    // DB 검색 실패해도 구글 API 검색은 실행
                    searchGooglePlaces(query, true);
                });
    }

    private void searchGooglePlaces(String query, boolean shouldMoveCamera) {
        FindAutocompletePredictionsRequest request =
                FindAutocompletePredictionsRequest.builder()
                         .setQuery(query)
                         .setCountries("KR")                  // 한국으로 제한
                         .setLocationRestriction(KOREA_BOUNDS) // 한국 범위로 제한
                         .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    final int[] placesAdded = {0}; // Track how many places are successfully fetched

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
                                        com.google.android.gms.maps.model.Marker marker = mMap.addMarker(new MarkerOptions()
                                                .position(latLng)
                                                .title(placeName)
                                                .snippet(placeAddress));
                                        marker.setTag(placeId);

                                        PlaceItem item = new PlaceItem(
                                                placeId, placeName, placeAddress,
                                                latLng.latitude, latLng.longitude);
                                        placeList.add(item);
                                        adapter.notifyDataSetChanged();
                                        placesAdded[0]++;

                                        // 커스텀 검색 결과가 없었고, 이게 구글 첫 번째 결과면 카메라 이동
                                        if (shouldMoveCamera && placesAdded[0] == 1) {
                                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f));
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("PLACES_ERROR", "장소 상세 조회 실패: " + e.toString());
                                });
                    });
                })
                .addOnFailureListener(e -> {
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
