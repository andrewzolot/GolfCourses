package com.github.andrewzolot.golfcourses.data

import com.github.andrewzolot.golfcourses.data.model.course.Golfcourse
import retrofit2.Call

import retrofit2.http.*

/**
 * Created by Zolotuev
 */

interface GolfcoursesService {
    @GET("/getCourses")
    fun getData(@Query("longitude") longitude: Double,
                @Query("latitude")  latitude: Double,
                @Query("radius") radius: Int): Call<List<Golfcourse>>

}
