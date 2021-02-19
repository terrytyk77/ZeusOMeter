package com.example.zeusometer.services

import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

object http {

    fun get(context: Context, url: String, toBeRun: (m: JSONObject?) -> Unit){
        //Create the request queue
        val requestQueue = Volley.newRequestQueue(context)

        val stringRequest  = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { res ->
                toBeRun(res)
            },
            { error ->
                println(error)
            }
        )

        //Adding the Requests to The Request Queue
        requestQueue.add(stringRequest)
    }



    fun post(context: Context, body : JSONObject, url: String, toBeRun: (m: JSONObject?) -> Unit){

        //Create the request queue
        val requestQueue = Volley.newRequestQueue(context)

        val postRequest  = JsonObjectRequest(
            Request.Method.POST,
            url,
            body,
            { res ->
                toBeRun(res)
            },
            { error ->
                println(error)
            }
        )

        //Adding the Requests to The Request Queue
        requestQueue.add(postRequest)
    }

}


