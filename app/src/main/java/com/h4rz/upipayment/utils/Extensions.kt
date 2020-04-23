package com.h4rz.upipayment.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import java.net.URLDecoder
import kotlin.random.Random

/**
 * Created by Harsh Rajgor on 23/04/20.
 */

fun payUsingUpi(activity: Activity, name: String, upiId: String, note: String, amount: String, merchantCode: String = "", transactionRefId: String = Random.nextLong().toString()) {
    val uriBuilder = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", name)
            //.appendQueryParameter("tid", transactionId)
            .appendQueryParameter("tr", transactionRefId.substring(6))
            .appendQueryParameter("tn", note)
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
    if (merchantCode.isNotEmpty())
        uriBuilder.appendQueryParameter("mc", merchantCode)
    val uri = uriBuilder.build()
    Log.d("URI GENERATED", URLDecoder.decode(uri.toString(), "UTF-8"))
    val upiPayIntent = Intent(Intent.ACTION_VIEW)
    upiPayIntent.data = uri

    // will always show a dialog to user to choose an app
    val chooser = Intent.createChooser(upiPayIntent, "Pay with")

    // check if intent resolves
    if (null != chooser.resolveActivity(activity.packageManager)) {
        activity.startActivityForResult(chooser, Constants.UPI_INTENT_REQUEST_CODE)
    } else {
        Toast.makeText(activity, "No UPI app found, please install one to continue", Toast.LENGTH_SHORT).show()
    }
}

fun isConnectionAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val netInfo = connectivityManager.activeNetworkInfo
    if (netInfo != null && netInfo.isConnected
            && netInfo.isConnectedOrConnecting
            && netInfo.isAvailable) {
        return true
    }
    return false
}

fun upiPaymentResponseProcessing(data: String, context: Context, textView: TextView) {
    if (isConnectionAvailable(context)) {
        Log.d("UPI Response Processing", "upiPaymentDataOperation: $data")
        var paymentCancel = ""
        var status = ""
        var approvalRefNo = ""
        val response = data.split("&").toTypedArray()
        for (i in response.indices) {
            val equalStr = response[i].split("=").toTypedArray()
            if (equalStr.size >= 2) {
                if (equalStr[0].equals("Status", true)) {
                    status = equalStr[1]
                } else if (equalStr[0].equals("ApprovalRefNo", true) || equalStr[0].equals("txnRef", true)) {
                    approvalRefNo = equalStr[1]
                }
            } else {
                paymentCancel = "Payment cancelled by user."
            }
        }
        when {
            status.equals("success", true) -> {
                //Code to handle successful transaction here.
                Toast.makeText(context, "Transaction successful.", Toast.LENGTH_SHORT).show()
                textView.append("\nPayment Successful: $approvalRefNo")
            }
            "Payment cancelled by user.".equals(paymentCancel, true) -> {
                Toast.makeText(context, "Payment cancelled by user.", Toast.LENGTH_SHORT).show()
                textView.append("\nCancelled by user: $approvalRefNo")
            }
            else -> {
                Toast.makeText(context, "Transaction failed.Please try again", Toast.LENGTH_SHORT).show()
                textView.append("\nfailed payment: $approvalRefNo")
            }
        }
    } else {
        Toast.makeText(context, "Internet connection is not available. Please check and try again", Toast.LENGTH_SHORT).show()
    }
}