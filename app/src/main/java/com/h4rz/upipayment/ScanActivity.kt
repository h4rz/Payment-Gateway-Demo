package com.h4rz.upipayment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.ResultPoint
import com.h4rz.upipayment.utils.Constants
import com.h4rz.upipayment.utils.payUsingUpi
import com.h4rz.upipayment.utils.upiPaymentResponseProcessing
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import kotlinx.android.synthetic.main.activity_scan.*
import kotlin.random.Random

class ScanActivity : AppCompatActivity(), BarcodeCallback {

    private lateinit var capture: CaptureManager
    private lateinit var context: Context
    private lateinit var qrString: String
    private lateinit var callback: BarcodeCallback
    private var TAG = ScanActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        initializeVariables()
        initializeBarcode(savedInstanceState)
    }

    private fun initializeBarcode(savedInstanceState: Bundle?) {
        capture = CaptureManager(this, zxingBarcodeScanner)
        capture.initializeFromIntent(intent, savedInstanceState)
        zxingBarcodeScanner.decodeSingle(callback)
    }

    private fun initializeVariables() {
        context = this
        qrString = ""
        callback = this
    }

    override fun barcodeResult(result: BarcodeResult?) {
        tvStatus.text = ""
        qrString = result?.text ?: ""
        Log.d(TAG, qrString)
        if (qrString.isNotEmpty())
            showAmountPickerDialog()
        else
            onError("Invalid QR Code")
    }

    private fun onError(error: String) {
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
    }

    private fun showAmountPickerDialog() {
        val alertDialog = AlertDialog.Builder(context)
                .setMessage("Enter Amount :")
                .setCancelable(false)
        val input = EditText(context)
        val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        input.layoutParams = lp
        alertDialog.setView(input)
        alertDialog.setPositiveButton("OK")
        { _, _ ->
            callUPIGateway(input.text.toString().trim().toDouble())
        }
        alertDialog.setNegativeButton("Cancel")
        { dialog, _ ->
            dialog.dismiss()
        }

        alertDialog.show()
    }

    private fun callUPIGateway(amount: Double) {
        val am = String.format("%.2f", amount)
        val uri = Uri.parse(qrString).buildUpon().appendQueryParameter("am", am).appendQueryParameter("cu", "INR").build()
        val authority = uri.authority
        val scheme = uri.scheme
        val upiID = uri.getQueryParameter("pa")
        val merchantName = uri.getQueryParameter("pn") ?: "User"
        val merchantCode = uri.getQueryParameter("mc") ?: ""
        val description = "Paying $amount"
        val transactionRefId = Random.nextLong().toString()
        if (upiID.isNullOrEmpty())
            onError("Invalid QR Code")
        else {
            payUsingUpi(this, merchantName, upiID, description, am, merchantCode, transactionRefId)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "response $resultCode")
        when (requestCode) {
            Constants.UPI_INTENT_REQUEST_CODE -> {
                if ((Activity.RESULT_OK == resultCode || resultCode == 11) && data != null) {
                    val text = data.getStringExtra("response")
                    Log.d(TAG, "onActivityResult: $text")
                    tvStatus.text = text
                    upiPaymentResponseProcessing(text, context, tvStatus)
                } else {
                    //when user simply back without payment
                    Log.d(TAG, "onActivityResult: " + "Return data is null")
                    val text = "nothing"
                    upiPaymentResponseProcessing(text, context, tvStatus)
                }
            }
        }
    }

    override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {

    }

    override fun onSaveInstanceState(outState: Bundle) {
        capture.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return zxingBarcodeScanner.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
        zxingBarcodeScanner.decodeSingle(callback)
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

}
