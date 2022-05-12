package com.github.andrewzolot.golfcourses.data.model.places

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class PlaceByCoordReq {

    @SerializedName("html_attributions")
    @Expose
    var htmlAttributions: List<Any>? = null
    @SerializedName("results")
    @Expose
    var results: List<Result>? = null
    @SerializedName("status")
    @Expose
    var status: String? = null

}
