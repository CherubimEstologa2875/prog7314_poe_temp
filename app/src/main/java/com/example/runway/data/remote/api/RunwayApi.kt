package com.example.runway.data.remote.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// Retrofit interface describing the Runway REST API (IIE, 2026; Square, Inc., n.d.).
// Endpoints follow REST conventions (Rouse, 2020).

interface RunwayApi {
    @GET("api/v1/items")
    suspend fun getItems(
        @Query("category") category: String? = null,
        @Query("updatedSince") updatedSince: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null,
    ): PageDto<ItemDto>

    @GET("api/v1/items/{id}")
    suspend fun getItem(@Path("id") id: String): ItemDto

    @POST("api/v1/items")
    suspend fun createItem(@Body body: CreateItemDto): ItemDto

    @PATCH("api/v1/items/{id}")
    suspend fun updateItem(@Path("id") id: String, @Body body: UpdateItemDto): ItemDto

    // A soft delete.
    @DELETE("api/v1/items/{id}")
    suspend fun deleteItem(@Path("id") id: String)

    @POST("api/v1/items/{id}/wears")
    suspend fun logWear(@Path("id") id: String, @Body body: LogWearDto): WearDto

    @GET("api/v1/outfits")
    suspend fun getOutfits(
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null,
    ): PageDto<OutfitDto>

    @GET("api/v1/outfits/{id}")
    suspend fun getOutfit(@Path("id") id: String): OutfitDto

    @POST("api/v1/outfits")
    suspend fun createOutfit(@Body body: OutfitDto): OutfitDto

    @PATCH("api/v1/outfits/{id}")
    suspend fun updateOutfit(@Path("id") id: String, @Body body: UpdateOutfitDto): OutfitDto

    @DELETE("api/v1/outfits/{id}")
    suspend fun deleteOutfit(@Path("id") id: String)

    // Counts a wear against every garment in the outfit.
    @POST("api/v1/outfits/{id}/wears")
    suspend fun logOutfitWear(@Path("id") id: String, @Body body: LogOutfitWearDto): OutfitWearDto

    @GET("api/v1/outfits/{id}/ratings")
    suspend fun getOutfitRatings(@Path("id") id: String): RatingSummaryDto

    @POST("api/v1/outfits/{id}/ratings")
    suspend fun rateOutfit(@Path("id") id: String, @Body body: CreateRatingDto): RatingDto

    @GET("api/v1/wardrobe/summary")
    suspend fun getWardrobeSummary(): WardrobeSummaryDto

    @GET("api/v1/plans")
    suspend fun getPlans(@Query("from") from: String, @Query("to") to: String): List<PlanDto>

    @PUT("api/v1/plans/{date}")
    suspend fun setPlan(@Path("date") date: String, @Body body: SetPlanDto): PlanDto

    @DELETE("api/v1/plans/{date}")
    suspend fun clearPlan(@Path("date") date: String)

    @GET("api/v1/users/me")
    suspend fun getProfile(): UserDto

    @GET("api/v1/users/me/settings")
    suspend fun getSettings(): SettingsDto

    // PUT, not PATCH: the settings screen always sends the complete object.
    @PUT("api/v1/users/me/settings")
    suspend fun saveSettings(@Body body: SettingsDto): SettingsDto


}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Rouse, M., 2020. RESTful API (REST API). [online] Available at: <https://searchapparchitecture.techtarget.com/definition/RESTful-API> [Accessed 31 July 2023].
Square, Inc., n.d.. Retrofit: A type-safe HTTP client for Android and Java. [online] Available at: <https://square.github.io/retrofit/> [Accessed 31 July 2023].
*/
