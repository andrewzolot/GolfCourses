package com.github.andrewzolot.golfcourses

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.github.andrewzolot.golfcourses.data.model.course.Golfcourse
import kotlinx.android.synthetic.main.dialog_info_golfcourse.view.*


/**
 * Class for displaying golf course dialog.
 */
class GolfcourseInfoDialog(private val context: Context) : View.OnClickListener {

    private val dialog: Dialog
    private var view: View

    init {
        dialog = object : Dialog(context, R.style.Theme_Dialog) {

            override fun onBackPressed() {
                super.onBackPressed()
            }

            override fun dismiss() {
                super.dismiss()
            }
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                                    LinearLayout.LayoutParams.MATCH_PARENT)
        view = buildView(context)
        dialog.setContentView(view, lp)
    }

    fun showDialog(data: Golfcourse) {
        view.nameTxt.text = data.golfName
        view.cityTxt.text = data.city
        view.streetTxt.text = data.strasse
        view.descTxt.text = data.description1
        view.holesTxt.text = data.holes
        view.artTxt.text = data.art
        view.restrTxt.text = data.restriction
        view.plzTxt.text = data.plz.toString()
        view.telTxt.text = editPhoneNumber(data.tel)
        view.faxTxt.text = data.fax
        view.webTxt.text = data.web
        view.emailTxt.text = data.email
        dialog.show()
    }


    private fun buildView(context: Context): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_info_golfcourse, null)
        view.close_button.setOnClickListener(this)
        view.telTxt.setOnClickListener(this)
        return view
    }

    override fun onClick(view: View) {
        if (view.id == R.id.close_button) dialog.dismiss()
    }

    fun editPhoneNumber(s: String?): String? {
        // filter phone's number
        if (s == null || s.length == 0) return null
        else {
            var phn = s as CharSequence
            val re = Regex("[^0-9]")
            phn = re.replace(phn, "")
            phn = Const.GERMANY_CODE + phn.substring(1, phn.length)
            return phn
        }

    }

}
