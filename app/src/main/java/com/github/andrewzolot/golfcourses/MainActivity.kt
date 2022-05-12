package com.github.andrewzolot.golfcourses

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import android.location.LocationManager
import android.location.Location
import com.google.android.gms.location.places.Places
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.StrictMode
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.github.andrewzolot.golfcourses.data.GolfcoursesService
import com.github.andrewzolot.golfcourses.data.GolfcoursesServiceGenerator
import com.github.andrewzolot.golfcourses.data.PlacesService
import com.github.andrewzolot.golfcourses.data.PlacesServiceGenerator
import com.github.andrewzolot.golfcourses.data.model.course.Golfcourse
import com.github.andrewzolot.golfcourses.data.model.places.PlaceByCoordReq
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.PlaceBuffer
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.maps.android.ui.IconGenerator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.hole_marker.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity :    FragmentActivity(),
                        OnMapReadyCallback,
                        GoogleApiClient.OnConnectionFailedListener,
                        GoogleMap.OnMarkerClickListener{

    private lateinit var mMapFragment: SupportMapFragment
    private lateinit var mGolfcourseInfoDialog: GolfcourseInfoDialog
    private val boundsGermany = LatLngBounds(LatLng(47.30084, 6.117557), LatLng(54.909609, 14.300768))
    private var mCoursesList = ArrayList<Golfcourse>()
    private var mMarkersOptionsList = ArrayList<MarkerOptions>()
    private var mCurRadius = 10 // current radius
    private val mCurLocation = Location("") // current user's location
    private lateinit var mMap: GoogleMap
    private lateinit var geoAnimation: AnimationDrawable
    private val animDuration = 600L // animation duration for popup windows
    private val REQUEST_LOCATION = 0
    private val TAG = "MainActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        // set map's fragment
        mMapFragment = SupportMapFragment.newInstance()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.map_container, mMapFragment)
        fragmentTransaction.commit()
        mMapFragment.getMapAsync(this)

        val mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build()

        val placesAdapter = PlacesAdapter(this, mGoogleApiClient, boundsGermany, null)
        placeAutoCompleteTxt.setAdapter(placesAdapter)

        geoAnimation = detectLocationBtn.drawable as AnimationDrawable
        // call animation's stop to prevent unexpected button's animation on old devices
        geoAnimation.stop()

        mGolfcourseInfoDialog = GolfcourseInfoDialog(this)

        // set touch listener for search window to detect gestures (simple implementation)
        searchLayout.setOnTouchListener(object: View.OnTouchListener{
            var oldX = 0f
            var oldY = 0f
            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                when (p1?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        oldX= p1.getX()
                        oldY= p1.getY()
                    }
                    MotionEvent.ACTION_MOVE ->{
                        val newX = p1.getX()
                        val newY = p1.getY()
                        val deltaX = oldX - newX
                        val deltaY = oldY - newY
                        if (Math.abs(deltaY) > Math.abs(deltaX))
                            if (deltaY > 0) showSearchWindow(false)
                            else showSearchWindow(true)
                    }
                }
                return true
            }
        })

        detectLocationBtn.setOnClickListener {
            if (!checkForGeoLocationEnabled()) showGeoLocationEnableDialog()
            else {
                if (checkPermission()) getLastLocation()
            }
        }


        placeAutoCompleteTxt.onItemClickListener = object : AdapterView.OnItemClickListener{
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val item = placesAdapter.getItem(p2)
                val placeId = item.getPlaceId()
                val placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId!!)
                placeResult.setResultCallback(updatePlaceDetails)
            }
        }

        radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{

            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                // set min seekbar_progress value to "1"
                if (p1 < 1) {
                    p0?.progress = 1
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                if (p0 != null){
                    mCurRadius = p0.progress
                    radiusValueTxt.setText(mCurRadius.toString())
                }
            }
        })

        submitBtn.setOnClickListener(object : View.OnClickListener{

            override fun onClick(p0: View?) {
                hideKeyboard(placeAutoCompleteTxt)
                submitBtn.startAnimation()
                downloadGolfCourses(mCurLocation.longitude, mCurLocation.latitude, mCurRadius)
            }

        })

        showSearchBtn.setOnClickListener(object : View.OnClickListener{

            override fun onClick(p0: View?) {
                showSearchWindow(true)
            }

        })

        zoomInBtn.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                mMap.animateCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom + 0.7f))
            }

        })

        zoomOutBtn.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                mMap.animateCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom - 0.7f))
            }

        })


        if (savedInstanceState != null) {
            mCoursesList = savedInstanceState.getParcelableArrayList(Const.COURSE_KEY)
            mCurRadius = savedInstanceState.getInt(Const.CUR_RADIUS_KEY)
            if (mCurRadius > 1) {
                radiusValueTxt.setText(mCurRadius.toString())
                radiusSeekBar.progress = mCurRadius
            }
            mCurLocation.longitude = savedInstanceState.getDouble(Const.CUR_LON_KEY)
            mCurLocation.latitude = savedInstanceState.getDouble(Const.CUR_LAT_KEY)
        }
    }

    override fun onMapReady(p0: GoogleMap?) {
        if (p0 != null){
            mMap = p0
            mMap.setOnMarkerClickListener(this)

            val width = getResources().getDisplayMetrics().widthPixels
            val height = getResources().getDisplayMetrics().heightPixels
            // 10% padding
            val padding =(width * 0.1).toInt()
            mMap.setLatLngBoundsForCameraTarget(boundsGermany)
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsGermany, width, height, padding))
            mMap.setMinZoomPreference(mMap.getCameraPosition().zoom)
            //set markers after screen rotation
            if (mCurLocation.latitude > 0 || mCurLocation.latitude < 0) {
                val l = ArrayList<Golfcourse>()
                l.addAll(mCoursesList)
                addMarkersToMap(l)
            }
        }
    }

    override fun onConnectionFailed(p0: ConnectionResult) {

    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        showSearchWindow(false)
        val position = p0?.tag as Int
        if (position > 0) mGolfcourseInfoDialog.showDialog(mCoursesList.get(position - 1))
        return false
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelableArrayList(Const.COURSE_KEY, mCoursesList)
        outState?.putInt(Const.CUR_RADIUS_KEY, mCurRadius)
        outState?.putDouble(Const.CUR_LAT_KEY, mCurLocation.latitude)
        outState?.putDouble(Const.CUR_LON_KEY, mCurLocation.longitude)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation()
                    Log.d(TAG,"Permission granted")
                } else {
                    Log.d(TAG,"Permission denied")
                }
                return
            }
        }
    }

    private fun getLastLocation(){
        Log.d(TAG,"Getting last location")
        startAnimateClick(true)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
                .addOnSuccessListener { location : Location? ->
                    if (location != null){
                        mCurLocation.latitude = location.latitude
                        mCurLocation.longitude = location.longitude
                        val loc: String = mCurLocation.latitude.toString() + ","+ mCurLocation.longitude.toString()
                        downloadPlaceData(loc, mCurRadius.toString(), resources.getString(R.string.service_key))
                    }
                    else showGeoDataUnavailableMessage()
                }
                .addOnFailureListener(object: OnFailureListener{
                    override fun onFailure(p0: java.lang.Exception) {
                        showGeoDataUnavailableMessage()
                        p0.printStackTrace()
                    }

                })
    }


    private fun showGeoDataUnavailableMessage(){
        startAnimateClick(false)
        val mes = Snackbar.make(main_container, R.string.geodata_unavailable_message, Snackbar.LENGTH_LONG)
        mes.setTextColor(Color.WHITE)
        mes.show()
    }

    private fun checkPermission(): Boolean{
        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"Permission already granted")
            return true
        }
        else{
            requestPermission()
            return false
        }
    }

    private fun requestPermission(){
        ActivityCompat.requestPermissions(this,
                Array(2){Manifest.permission.ACCESS_FINE_LOCATION; Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_LOCATION)
    }

    /**
     * Callback for result from google places API
     */
    private val updatePlaceDetails = ResultCallback<PlaceBuffer> { places ->
        if (!places.status.isSuccess) {
            places.release()
        }
        val place = places.get(0)
        mCurLocation.longitude = place.latLng.longitude
        mCurLocation.latitude = place.latLng.latitude
        addMarkersToMap(null)
        places.release()
    }

    private fun checkForGeoLocationEnabled(): Boolean{
        val lm = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gps_enabled = false
        var network_enabled = false
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
        }
        return !(!gps_enabled && !network_enabled)
    }

    private fun showGeoLocationEnableDialog(){
        // notify user
        val dialog = AlertDialog.Builder(this)
        dialog.setMessage(this.getResources().getString(R.string.location_not_enabled))
        dialog.setPositiveButton(this.getResources().getString(R.string.open_settings), DialogInterface.OnClickListener { paramDialogInterface, paramInt ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            this.startActivity(intent)
        })
        dialog.setNegativeButton(this.getString(R.string.cancel_action),
                { paramDialogInterface, paramInt ->  paramDialogInterface.dismiss()
                })
        dialog.show()
        startAnimateClick(false)
    }

    /**
     * Method for downloading info about place detected by device's geolocation
     */
    private fun downloadPlaceData(location : String, radius : String, key : String) {

        PlacesServiceGenerator.createService(PlacesService::class.java).getData(location, radius, key)
                .enqueue(object : Callback<PlaceByCoordReq>{

                    override fun onFailure(call: Call<PlaceByCoordReq>?, t: Throwable?) {
                        startAnimateClick(false)
                        addMarkersToMap(null)
                        val mes = Snackbar.make(main_container, R.string.internet_unavailable_message_2, Snackbar.LENGTH_LONG)
                        mes.setTextColor(Color.WHITE)
                        mes.show()
                    }

                    override fun onResponse(call: Call<PlaceByCoordReq>?, response: Response<PlaceByCoordReq>?) {
                        if (response != null && response.isSuccessful) {
                            startAnimateClick(false)
                            val list = response.body()?.results
                            if (list != null && !list.isEmpty()){
                                placeAutoCompleteTxt.setText(list.get(list.size - 1).name, false)
                                placeAutoCompleteTxt.setSelection(placeAutoCompleteTxt.text.length)
                                addMarkersToMap(null)
                            }
                        }
                    }

                })
    }

    /**
     * Method for downloading golf markers
     */
    private fun downloadGolfCourses(longitude : Double, latitude : Double, radius : Int) {

        GolfcoursesServiceGenerator.createService(GolfcoursesService::class.java).getData(longitude, latitude, radius)
                .enqueue(object : Callback<List<Golfcourse>>{

                    override fun onFailure(call: Call<List<Golfcourse>>?, t: Throwable?) {
                        submitBtn.revertAnimation()
                        val mes = Snackbar.make(main_container, R.string.internet_unavailable_message_1, Snackbar.LENGTH_LONG)
                        mes.setTextColor(Color.WHITE)
                        mes.show()
                    }

                    override fun onResponse(call: Call<List<Golfcourse>>?, response: Response<List<Golfcourse>>?) {
                        if (response?.body() != null && response.isSuccessful) {
                            val list = response.body()
                            if (list != null){
                                addMarkersToMap(list)
                                submitBtn.revertAnimation()
                                showSearchWindow(false)
                            }
                        }
                    }

                })
    }

    /**
     * Method add space before holes count to align it in the center of golf marker
     */
    private fun alignHolesCount(count: String?) : String? {
        if (count?.length == 1) return "  $count"
        else return count
    }

    /**
     * Method return user location marker
     */
    private fun createCurrentLocationMarker(): MarkerOptions{
        val location = LatLng(mCurLocation.latitude, mCurLocation.longitude)
        val markerOptions = MarkerOptions().position(location)
        markerOptions.title("You")
        return markerOptions
    }

    /**
     * Method return golf marker
     */
    private fun createGolfMarker(item: Golfcourse) : MarkerOptions? {
        val longitude = item.llon?.replace(',','.')?.toDouble()
        val latitude = item.lLan?.replace(',','.')?.toDouble()
        if (longitude != null && latitude != null) {
            val location = LatLng(latitude, longitude)
            val holeMarker = LayoutInflater.from(this).inflate(R.layout.hole_marker, null)
            holeMarker.amu_text.setText(alignHolesCount(item.holes))
            val mIconGenerator = IconGenerator(this)
            mIconGenerator.setContentView(holeMarker)
            mIconGenerator.setBackground(ContextCompat.getDrawable(this, R.drawable.ic_golf_hole))
            val icon = mIconGenerator.makeIcon()
            val mOptions = MarkerOptions().position(location).icon(BitmapDescriptorFactory.fromBitmap(icon))
            return mOptions
        }
        else return null
    }


    private fun addMarkersToMap(list: List<Golfcourse>?){
        mMap.clear()
        mCoursesList.clear()
        mMarkersOptionsList.clear()
        if (list != null) mCoursesList.addAll(list)
        val curLocationMo = createCurrentLocationMarker()
        mMarkersOptionsList.add(curLocationMo)
        for (item in mCoursesList) {
            val mO = createGolfMarker(item)
            if (mO != null) mMarkersOptionsList.add(mO)
        }
        val markersBoundsBuilder = LatLngBounds.Builder()
        var gTag = 0
        for (item in mMarkersOptionsList){
            mMap.addMarker(item).tag = gTag
            markersBoundsBuilder.include(item.position)
            gTag ++
        }
        val width = getResources().getDisplayMetrics().widthPixels
        val height = getResources().getDisplayMetrics().heightPixels
        val padding = (width * 0.1).toInt()
        val bounds = markersBoundsBuilder.build()
        val cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding)
        mMap.animateCamera(cu)
    }

    private fun showSearchWindow(show: Boolean) {
        val value: Float = if (show) 0f else 0f - searchLayout.height
        val animation = ObjectAnimator.ofFloat(searchLayout, "translationY", value)
        animation.duration = animDuration
        animation.start()
    }

    private fun startAnimateClick(start: Boolean){
        if (start) geoAnimation.start()
        else geoAnimation.stop()
    }

    private fun Snackbar.setTextColor(color: Int): Snackbar {
        val tv = view.findViewById(android.support.design.R.id.snackbar_text) as TextView
        tv.setTextColor(color)
        return this
    }

    private fun hideKeyboard(view: View){
        val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0)
    }

}
