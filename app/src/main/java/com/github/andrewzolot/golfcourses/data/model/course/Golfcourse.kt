package com.github.andrewzolot.golfcourses.data.model.course

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Golfcourse() : Parcelable {

    @SerializedName("Art")
    @Expose
    var art: String? = null
    @SerializedName("City")
    @Expose
    var city: String? = null
    @SerializedName("Description1")
    @Expose
    var description1: String? = null
    @SerializedName("Fax")
    @Expose
    var fax: String? = null
    @SerializedName("GolfName")
    @Expose
    var golfName: String? = null
    @SerializedName("Golf_ID")
    @Expose
    var golfID: Int? = null
    @SerializedName("Holes")
    @Expose
    var holes: String? = null
    @SerializedName("LLan")
    @Expose
    var lLan: String? = null
    @SerializedName("Llon")
    @Expose
    var llon: String? = null
    @SerializedName("PLZ")
    @Expose
    var plz: Int? = null
    @SerializedName("Restriction")
    @Expose
    var restriction: String? = null
    @SerializedName("Strasse")
    @Expose
    var strasse: String? = null
    @SerializedName("Tel")
    @Expose
    var tel: String? = null
    @SerializedName("Web")
    @Expose
    var web: String? = null
    @SerializedName("email")
    @Expose
    var email: String? = null

    constructor(parcel: Parcel) : this() {
        art = parcel.readString()
        city = parcel.readString()
        description1 = parcel.readString()
        fax = parcel.readString()
        golfName = parcel.readString()
        golfID = parcel.readValue(Int::class.java.classLoader) as? Int
        holes = parcel.readString()
        lLan = parcel.readString()
        llon = parcel.readString()
        plz = parcel.readValue(Int::class.java.classLoader) as? Int
        restriction = parcel.readString()
        strasse = parcel.readString()
        tel = parcel.readString()
        web = parcel.readString()
        email = parcel.readString()
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest!!.writeString(art)
        dest.writeString(city)
        dest.writeString(description1)
        dest.writeString(fax)
        dest.writeString(golfName)
        dest.writeString(holes)
        dest.writeString(lLan)
        dest.writeString(llon)
        dest.writeString(restriction)
        dest.writeString(strasse)
        dest.writeString(tel)
        dest.writeString(web)
        dest.writeString(email)
        if (golfID != null) dest.writeInt(golfID!!)
        if (plz != null) dest.writeInt(plz!!)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Golfcourse> {
        override fun createFromParcel(parcel: Parcel): Golfcourse {
            return Golfcourse(parcel)
        }

        override fun newArray(size: Int): Array<Golfcourse?> {
            return arrayOfNulls(size)
        }
    }


}
