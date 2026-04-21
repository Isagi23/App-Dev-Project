package com.example.canteenmanagementsystem.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SpoonacularService {
    @GET("recipes/autocomplete")
    Call<List<AutocompleteResponse>> autocomplete(
            @Query("query") String query,
            @Query("number") int number,
            @Query("apiKey") String apiKey
    );

    class AutocompleteResponse {
        public int id;
        public String title;
        public String imageType;
    }
}