package com.github.andrewzolot.golfcourses.data

import com.github.andrewzolot.golfcourses.Const
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Class generator to generate retrofit service
 */

object GolfcoursesServiceGenerator {

    private val builder = Retrofit.Builder()
            .baseUrl(Const.BASE_URL_GOLFCOURSES)
            .addConverterFactory(GsonConverterFactory.create())

    private val retrofit = builder.build()

    fun <S> createService(serviceClass: Class<S>): S {
        return retrofit.create(serviceClass)
    }
}
