package com.example.zeusometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.zeusometer.services.http
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject

class MainActivity : AppCompatActivity(), LocationListener {
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2

    //Dialogue Values   ||
        private var dialogueTitle = ""
        private var dialogueInfo = ""
        private var dialogueBackground = R.drawable.grass
    //------------------||

    //Arrays Holding the Info  ||
        private var rainyDay = arrayListOf<ClimateChangeData>()
        private var sunnyDay = arrayListOf<ClimateChangeData>()
        private var partiallyDay = arrayListOf<ClimateChangeData>()
        private var snowyDay = arrayListOf<ClimateChangeData>()
        private var cloudyDay = arrayListOf<ClimateChangeData>()
    //-------------------------||

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fillInfoInArray()

        getCurrentLocation()

        getLocation.setOnClickListener {
            getSearchedLocation(locationInput.text.toString())
            locationInput.text?.clear()
            hideKeyboard(getLocation)
        }

        getCurrentLocation.setOnClickListener {
            getCurrentLocation()
            hideKeyboard(getCurrentLocation)
        }

        getInfo.isEnabled = false
        getInfo.setOnClickListener {

            //Title
            val title = TextView(this)
            title.text = dialogueTitle
            title.setPadding(20,80,20,20)
            title.gravity = Gravity.CENTER
            title.setTextColor(Color.WHITE)
            title.setTypeface(title.typeface, Typeface.BOLD)
            title.textSize = 24f

            //Message
            val messageInfo = TextView(this)
            messageInfo.text = dialogueInfo
            messageInfo.setPadding(70,40,70,25)
            messageInfo.gravity = Gravity.CENTER
            messageInfo.setTextColor(Color.WHITE);
            messageInfo.setLineSpacing(1f, 1.25f)
            messageInfo.textSize = 18f

            //Background
            val background = AppCompatResources.getDrawable(this, dialogueBackground)
            background?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.parseColor("#99000000"), BlendModeCompat.SRC_ATOP)

            val builder = MaterialAlertDialogBuilder(this)

            builder
            .setBackground(background)
            .setCustomTitle(title)
            .setView(messageInfo)
            .setPositiveButton("Ok", null)

            val dialog = builder.create()
            dialog.show()

            val buttonPositive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            buttonPositive.textSize = 20f
            buttonPositive.setTypeface(buttonPositive.typeface, Typeface.BOLD)
            buttonPositive.setTextColor(ContextCompat.getColor(this, R.color.purple_200))

            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                val test1 = IntArray(2)
                buttonPositive.getLocationInWindow(test1)
                val height = (test1[1] + buttonPositive.height) +  235
                dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height);
            }, 0)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getSearchedLocation(locInput: String){
        //Ask The API to Convert name Location in Coordinates
        if(locInput.isEmpty()){
            Toast.makeText(this, "Location Not Found", Toast.LENGTH_SHORT).show()
        }
        else{
            http.get(this, "https://api.mapbox.com/geocoding/v5/mapbox.places/${locInput}.json?access_token=pk.eyJ1IjoidGVycnl0eWs3NyIsImEiOiJja2V2ajJyc2QzdTBwMnJwaTJwejN2cmQ3In0.JhSJPzxPU7UZnFEkJx_5QA&limit=1"){
                if(it?.getJSONArray("features")?.length() == 0){
                    Toast.makeText(this, "Location Not Found", Toast.LENGTH_SHORT).show()
                }
                else{
                    val locLongitude = it?.getJSONArray("features")?.getJSONObject(0)?.getJSONObject("geometry")?.getJSONArray("coordinates")?.getDouble(0)
                    val locLatitude = it?.getJSONArray("features")?.getJSONObject(0)?.getJSONObject("geometry")?.getJSONArray("coordinates")?.getDouble(1)
                    //Ask the API to give weather news of the Coordinates
                    http.get(this, "http://api.weatherstack.com/current?access_key=baa6bc79c2aa1c425c728dbd362b56a9&query=${locLatitude},${locLongitude}&units=m"){ res ->
                        displayInfo(res!!)
                    }
                }
            }
        }
    }

    private fun getCurrentLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        }
        else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onLocationChanged(location: Location) {
        http.get(this, "http://api.weatherstack.com/current?access_key=baa6bc79c2aa1c425c728dbd362b56a9&query=${location.latitude},${location.longitude}&units=m"){
            displayInfo(it!!)
            locationManager.removeUpdates(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun displayInfo(it: JSONObject){
        val weatherLayout = findViewById<RelativeLayout>(R.id.backgroundWeather)

        if(it.getJSONObject("current").getString("is_day") == "yes"){
            weatherLayout.setBackgroundResource(R.drawable.day)
        }
        else{
            weatherLayout.setBackgroundResource(R.drawable.night)
        }

        temperatureView.text = "${it.getJSONObject("current").getString("temperature")}°C / ${((it.getJSONObject("current").getString("temperature").toInt() * 9/5) + 32)}°F"
        locationViewText.text = it.getJSONObject("location").getString("country")
        windView.text = "${it.getJSONObject("current").getInt("wind_speed")} km/h, ${it.getJSONObject("current").getString("wind_dir")}"
        humidityView.text = "${it.getJSONObject("current").getInt("humidity")}%"
        precipitationView.text = "${it.getJSONObject("current").getInt("precip")} mm"

        when(it.getJSONObject("current").getInt("weather_code")) {
            389, 386, 359, 356, 353, 314, 311, 308, 305, 302, 299, 296, 293, 176, 284, 281, 266, 263, 185, 230, 200, 248, 143 -> {
                forecastImage.setImageResource(R.drawable.ic_rain)
                val randomInfo = (0 until rainyDay.size).random()
                dialogueTitle = rainyDay[randomInfo].Title
                dialogueInfo = rainyDay[randomInfo].Info
                dialogueBackground = rainyDay[randomInfo].Background
                tipOfTheDay.text = "Zeus is Angry and sent you ${it.getJSONObject("current").getJSONArray("weather_descriptions").getString(0)}, use an umbrella"
            }
            113 -> {
                forecastImage.setImageResource(R.drawable.ic_sun)
                val randomInfo = (0 until sunnyDay.size).random()
                dialogueTitle = sunnyDay[randomInfo].Title
                dialogueInfo = sunnyDay[randomInfo].Info
                dialogueBackground = sunnyDay[randomInfo].Background
                tipOfTheDay.text = "Zeus blessed you with a ${it.getJSONObject("current").getJSONArray("weather_descriptions").getString(0)} day, enjoy it!"
            }
            116 -> {
                forecastImage.setImageResource(R.drawable.ic_partialsun)
                val randomInfo = (0 until partiallyDay.size).random()
                dialogueTitle = partiallyDay[randomInfo].Title
                dialogueInfo = partiallyDay[randomInfo].Info
                dialogueBackground = partiallyDay[randomInfo].Background
                tipOfTheDay.text = "Zeus is watching. ${it.getJSONObject("current").getJSONArray("weather_descriptions").getString(0)} day, get somme sun while you can!"
            }
            395, 392, 371, 368, 338, 335, 332, 329, 326, 323, 227, 179, 365, 362, 320, 317, 182, 377, 374, 350, 260 -> {
                forecastImage.setImageResource(R.drawable.ic_snowy)
                val randomInfo = (0 until snowyDay.size).random()
                dialogueTitle = snowyDay[randomInfo].Title
                dialogueInfo = snowyDay[randomInfo].Info
                dialogueBackground = snowyDay[randomInfo].Background
                tipOfTheDay.text = "Zeus is feeling cold today, ${it.getJSONObject("current").getJSONArray("weather_descriptions").getString(0)} incoming, use a jacket"
            }
            119, 122 -> {
                forecastImage.setImageResource(R.drawable.ic_cloudy)
                val randomInfo = (0 until cloudyDay.size).random()
                dialogueTitle = cloudyDay[randomInfo].Title
                dialogueInfo = cloudyDay[randomInfo].Info
                dialogueBackground = cloudyDay[randomInfo].Background
                tipOfTheDay.text = "Zeus is not in good mood, ${it.getJSONObject("current").getJSONArray("weather_descriptions").getString(0)} incoming, get ready for darkness"
            }
            else -> {
                forecastImage.setImageResource(R.drawable.ic_none)
                dialogueTitle = ""
                dialogueInfo = ""
                dialogueBackground = R.drawable.grass
                tipOfTheDay.text = "Zeus is confused"
            }
        }
        tipOfTheDay.alpha = 1f
        getInfo.isEnabled = true
    }

    private fun hideKeyboard(v: View) {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(v.applicationWindowToken, 0)
    }

    private fun fillInfoInArray(){
        rainyDay.add(ClimateChangeData("Read a book", "Not feeling like going out?\n\nTry reading a book. You'll not only help reduce the Carbon Footprint, but will also improve your brain's health\n\n(Prevents age-related cognitive decline, reduces stress, helps with sleep ...)", R.drawable.book))
        rainyDay.add(ClimateChangeData("Reuse Rain Water", "Do you know you can reuse Rain Water?\n\nEven if you live in an apartment building, you are still able to catch the water and reuse it to water lawns, gardens and houseplants. To composting, Car Washing, Toilet Flushing, Clothes Washing etc.", R.drawable.rain_saving))
        rainyDay.add(ClimateChangeData("Ride the bus", "Raining too much to walk or use a bike?\n\nTry to Ride a bus or any kind of public transportation.\n\nPublic transportation reduces the gasoline consumption, and the Carbon Footprint.\n\nThe more people choose this option the easier it will be to Reduce Road Congestion.", R.drawable.public_transportation))

        sunnyDay.add(ClimateChangeData("Hang-dry your clothes instead of using the dryer", "Its Sunny outside, why not use it to dry your clothes?\n\nDo you know that there is more than 90 million clothes dryers in de USA?\n\nIf all Americans line-dried instead for half a year, the output of carbon dioxide would be 3.3%  lower.", R.drawable.dry_clothes))
        sunnyDay.add(ClimateChangeData("Start walking", "Its such a beautiful day outside, why not go for a walk?\n\nWalking will not only help saving our Planet by reducing the Carbon Footprint, but will also increase your physical and mental health.", R.drawable.walk))
        sunnyDay.add(ClimateChangeData("Just ride a bike", "You know that using a bike to move around can save the world?\n\nA 2015 study shows that if we switched the urban trips with bicycling (cutting car usage), by 2050 we could have reduced the CO2 emissions by 50% all around the world.", R.drawable.bicycle))

        partiallyDay.add(ClimateChangeData("Pick up trash", "Maybe is not the best day to enjoy the sun?\n\nBut you can still enjoy the day outside by avoiding debris from hurting animals or ending up in streams and waterways.\n\nBring some small bags and pick up the trash that you might find on your way. Don't forget to divide them into recyclables and normal trash before you toss them.", R.drawable.trash))
        partiallyDay.add(ClimateChangeData("Recycle", "Worried about the climate changes of the Planet?\n\nWant to do something about it? Start recycling today.\n\nRecycling helps reduce the amount of waste sent to the landfills and incinerators, conserves the natural resources, prevents pollution and saves energy", R.drawable.recycle))
        partiallyDay.add(ClimateChangeData("Upcycle your furniture", "Not the best weather to go out and have fun?\n\nTry decorating your house with your own made furniture.\n\nUpcycled furniture can be innovative and environmentally smart, and let's not forget that you can personalize it 100% to your taste.\n\nTry using recycled materials (for example: pallets) or recomposing your old furniture.", R.drawable.upcycle))

        snowyDay.add(ClimateChangeData("Hack your thermostat", "You know thermostats consume lots of energy?\n\nBy just simply adjusting your thermostat to run 2 degrees cooler (winter) or 2 degrees warmer (summer), will dramatically increase energy savings, and guess what?\n\nYou will most likely not even notice that much of a difference.", R.drawable.thermostat))
        snowyDay.add(ClimateChangeData("Add solar panels to your house", "Thinking about sustainable ways of getting energy?\n\n Ever thought about investing in the solar industry, and add solar panels to your home?\n\nBesides being a renewable resource of extraordinary scale, it also helps reduce the emissions of Greenhouse gases.", R.drawable.solar_panel))
        snowyDay.add(ClimateChangeData("Design your workspace around natural light", " Staying at home for long periods of time? Need to have a light going on all day?\n\nIf you have an alternative spot in your house that can get better natural light, try to work your space around it.\n\nBy turning off some lights you will be reducing the electricity usage by a lot, and you will also extend the life of light bulbs.", R.drawable.natural_light))

        cloudyDay.add(ClimateChangeData("Bring your own shopping bags", "Too cloudy to go for a walk outside?\n\nPrefer shopping instead? Don't forget to bring your own shopping bags.\n\nPlastic bags are incredibly destructive to the environment, not only they take hundreds of years to decompose, but they also contaminate the soils and waterways, and contribute a lot to the marine life extinction", R.drawable.shopping_bag))
        cloudyDay.add(ClimateChangeData("Shop local", "It's a good day for shopping?\n\nTry to shop locally, you won't need to use transportation to get there, reducing its costs and the carbon emissions.\n\nAnd will also be supporting local businesses, providing community jobs", R.drawable.local_shop))
        cloudyDay.add(ClimateChangeData("Buy vintage", "Do you like shopping?\n\nHave you ever tried to buy vintage? You'll find stuff that are unique and not produced anymore, and at lower prices than you would pay for something new.\n\nBut you will also be recycling old stuff and giving them a new life, reducing the amount of materials needed to produce new stuff", R.drawable.vintage))
    }
}



