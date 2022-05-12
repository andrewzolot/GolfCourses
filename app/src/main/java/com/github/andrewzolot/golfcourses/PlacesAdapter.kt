package com.github.andrewzolot.golfcourses

import android.content.Context
import android.graphics.Typeface
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView

import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.data.DataBufferUtils
import com.google.android.gms.location.places.AutocompleteFilter
import com.google.android.gms.location.places.AutocompletePrediction
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.model.LatLngBounds

import java.util.ArrayList
import java.util.concurrent.TimeUnit

/**
 * AutoCompleteTextView's adapter class
 */

class PlacesAdapter(private val context: Context, private val mGoogleApiClient: GoogleApiClient,
                    private val bounds: LatLngBounds,
                    private val aFilter: AutocompleteFilter?) : BaseAdapter(), Filterable {

    private var list: ArrayList<AutocompletePrediction>? = ArrayList()

    override fun getCount(): Int {
        return list!!.size
    }

    override fun getItem(position: Int): AutocompletePrediction {
        return list!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            val layoutInflater = LayoutInflater.from(context)
            convertView = layoutInflater.inflate(R.layout.autocomplete_item, parent, false)
        }
        convertView!!.isScrollContainer = true
        val autocompletePrediction = getItem(position)
        (convertView.findViewById<View>(R.id.textView) as TextView).text = autocompletePrediction.getPrimaryText(StyleSpan(Typeface.BOLD))
        return convertView

    }

    override fun getFilter(): Filter {
        return object : Filter() {

            override fun performFiltering(p0: CharSequence?): FilterResults {
                val results = Filter.FilterResults()
                var filterData: ArrayList<AutocompletePrediction>? = ArrayList()
                if (p0 != null) {
                    filterData = getAdvice(p0)
                }
                results.values = filterData
                if (filterData != null) {
                    results.count = filterData.size
                } else
                    results.count = 0
                return results
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                if (p1 != null && p1.count > 0) {
                    list = p1.values as ArrayList<AutocompletePrediction>
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return if (resultValue is AutocompletePrediction) {
                    resultValue.getPrimaryText(null)
                }
                else super.convertResultToString(resultValue)
            }

        }

    }

    private fun getAdvice(constraint: CharSequence?): ArrayList<AutocompletePrediction>? {
        if (mGoogleApiClient.isConnected) {
            val result = Places.GeoDataApi.getAutocompletePredictions(mGoogleApiClient, constraint!!.toString(), bounds, aFilter)
            val autocompletePredictions = result.await(60, TimeUnit.SECONDS)
            val status = autocompletePredictions.status
            if (!status.isSuccess) {
                autocompletePredictions.release()
            } else {
                return DataBufferUtils.freezeAndClose(autocompletePredictions)
            }
        } else {
            return null
        }
        return null
    }

}
