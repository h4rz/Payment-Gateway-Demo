package com.h4rz.upipayment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.h4rz.upipayment.utils.Constants
import com.h4rz.upipayment.utils.payUsingUpi
import com.h4rz.upipayment.utils.upiPaymentResponseProcessing
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var TAG = MainActivity::class.simpleName
    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        context = this

        btnScanQr.setOnClickListener(this)
        btnSend.setOnClickListener(this)
        //btnScanQr.visibility = View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "response $resultCode")
        when (requestCode) {
            Constants.UPI_INTENT_REQUEST_CODE -> {
                if ((Activity.RESULT_OK == resultCode || resultCode == 11) && data != null) {
                    val text = data.getStringExtra("response")
                    Log.d(TAG, "onActivityResult: $text")
                    tvResponse.text = text
                    upiPaymentResponseProcessing(text, context, tvResponse)
                } else {
                    //when user simply back without payment
                    Log.d(TAG, "onActivityResult: " + "Return data is null")
                    val text = "nothing"
                    upiPaymentResponseProcessing(text, context, tvResponse)
                }
            }
        }
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.btnScanQr -> startActivity(Intent(context, ScanActivity::class.java))
            R.id.btnSend -> onPayButtonClicked()
        }
    }

    private fun onPayButtonClicked() {
        val name = etName.text.toString()
        val upiId = etUpiID.text.toString()
        val description = etNote.text.toString()
        var amount = etAmount.text.toString()
        var amountDouble = 0.0
        if (amount.isNotEmpty())
            amountDouble = amount.trim().toDouble()
        when {
            name.isEmpty() -> {
                Toast.makeText(context, " Name is invalid", Toast.LENGTH_SHORT).show()
            }
            upiId.isEmpty() -> {
                Toast.makeText(context, " UPI ID is invalid", Toast.LENGTH_SHORT).show()
            }
            description.isEmpty() -> {
                Toast.makeText(context, " Note is invalid", Toast.LENGTH_SHORT).show()
            }
            amountDouble < 1.0 -> {
                Toast.makeText(context, " Amount must be greater than 1", Toast.LENGTH_SHORT).show()
            }
            else -> {

                payUsingUpi(this, name, upiId, description, String.format("%.2f", amountDouble))
            }
        }
    }
}