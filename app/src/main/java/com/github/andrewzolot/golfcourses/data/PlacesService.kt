package com.github.andrewzolot.golfcourses.data

import com.github.andrewzolot.golfcourses.data.model.places.PlaceByCoordReq
import retrofit2.Call

import retrofit2.http.*

/**
 * Created by Zolotuev
 */

interface PlacesService {
    @GET("/maps/api/place/nearbysearch/json")
    fun getData(@Query("location") location: String,
                @Query("radius")  radius: String,
                @Query("key") key: String): Call<PlaceByCoordReq>

}
