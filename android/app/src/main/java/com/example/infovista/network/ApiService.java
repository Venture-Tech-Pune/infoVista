package com.example.infovista.network;

import com.example.infovista.models.ApiResponse;
import com.example.infovista.models.AuthResponse;
import com.example.infovista.models.Notice;
import com.example.infovista.models.User;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface ApiService {
    
    // ==================== Auth Endpoints ====================
    
    @POST("api/auth/register")
    Call<ApiResponse<AuthResponse>> register(@Body Map<String, String> body);
    
    @POST("api/auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body Map<String, String> body);
    
    @GET("api/auth/me")
    Call<ApiResponse<User>> getProfile(@Header("Authorization") String token);
    
    @PUT("api/auth/update-profile")
    Call<ApiResponse<User>> updateProfile(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );
    
    @PUT("api/auth/change-password")
    Call<ApiResponse<String>> changePassword(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );
    
    // ==================== Notice Endpoints ====================
    
    @GET("api/notices")
    Call<ApiResponse<List<Notice>>> getNotices(
            @Header("Authorization") String token,
            @QueryMap Map<String, String> params
    );
    
    @GET("api/notices/active")
    Call<ApiResponse<List<Notice>>> getActiveNotices();
    
    @GET("api/notices/{id}")
    Call<ApiResponse<Notice>> getNotice(
            @Header("Authorization") String token,
            @Path("id") String id
    );
    
    @Multipart
    @POST("api/notices")
    Call<ApiResponse<Notice>> createNotice(
            @Header("Authorization") String token,
            @Part("title") RequestBody title,
            @Part("description") RequestBody description,
            @Part("priority") RequestBody priority,
            @Part("category") RequestBody category,
            @Part("displayDuration") RequestBody displayDuration,
            @Part("isActive") RequestBody isActive,
            @Part("scheduledAt") RequestBody scheduledAt,
            @Part("expiresAt") RequestBody expiresAt,
            @Part MultipartBody.Part media
    );
    
    @Multipart
    @PUT("api/notices/{id}")
    Call<ApiResponse<Notice>> updateNotice(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Part("title") RequestBody title,
            @Part("description") RequestBody description,
            @Part("priority") RequestBody priority,
            @Part("category") RequestBody category,
            @Part("displayDuration") RequestBody displayDuration,
            @Part("isActive") RequestBody isActive,
            @Part("scheduledAt") RequestBody scheduledAt,
            @Part("expiresAt") RequestBody expiresAt,
            @Part MultipartBody.Part media
    );
    
    @DELETE("api/notices/{id}")
    Call<ApiResponse<Void>> deleteNotice(
            @Header("Authorization") String token,
            @Path("id") String id
    );
}
