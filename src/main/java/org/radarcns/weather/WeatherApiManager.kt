/*
 * Copyright 2017 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarcns.weather

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import okhttp3.OkHttpClient
import org.radarbase.android.device.AbstractDeviceManager
import org.radarbase.android.device.BaseDeviceState
import org.radarbase.android.device.DeviceStatusListener
import org.radarbase.android.util.NetworkConnectedReceiver
import org.radarbase.android.util.OfflineProcessor
import org.radarcns.passive.weather.LocalWeather
import org.radarcns.passive.weather.LocationType
import org.radarcns.weather.WeatherApiService.Companion.WEATHER_QUERY_INTERVAL_DEFAULT
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class WeatherApiManager(service: WeatherApiService, private val client: OkHttpClient) : AbstractDeviceManager<WeatherApiService, BaseDeviceState>(service) {

    private val processor: OfflineProcessor
    private val weatherTopic = createCache("android_local_weather", LocalWeather::class.java)
    private val networkReceiver: NetworkConnectedReceiver

    private val locationManager: LocationManager? = service.getSystemService(Context.LOCATION_SERVICE) as LocationManager?

    @get:Synchronized
    private var weatherApi: WeatherApi? = null

    init {
        processor = OfflineProcessor(service) {
            process = listOf(this@WeatherApiManager::processWeather)
            requestCode = WEATHER_UPDATE_REQUEST_CODE
            requestName = ACTION_UPDATE_WEATHER
            interval(WEATHER_QUERY_INTERVAL_DEFAULT, TimeUnit.SECONDS)
            wake = true
        }

        networkReceiver = NetworkConnectedReceiver(service)
    }

    @Synchronized
    fun setSource(source: String, apiKey: String) {
        if (source.equals(SOURCE_OPENWEATHERMAP, ignoreCase = true)) {
            weatherApi = OpenWeatherMapApi(apiKey, client)
            logger.info("WeatherApiManager created with key {}", apiKey)
        } else {
            logger.error("The weather api '{}' is not recognised. Please set a different weather api source.", source)
        }
    }

    override fun start(acceptableIds: Set<String>) {
        updateStatus(DeviceStatusListener.Status.READY)

        register(name = "OpenWeatherMap")

        logger.info("Starting WeatherApiManager")
        networkReceiver.register()
        processor.start()

        updateStatus(DeviceStatusListener.Status.CONNECTED)
    }

    private fun processWeather() {
        if (!networkReceiver.isConnected) {
            logger.warn("No internet connection. Skipping weather query.")
        }

        val location = this.lastKnownLocation
        if (location == null) {
            logger.error("Could not retrieve location. No input for Weather API")
            return
        }

        val api = this.weatherApi ?: return

        try {
            val result = api.loadCurrentWeather(location.latitude, location.longitude)

            // How location was derived
            val locationType = when(location.provider) {
                GPS_PROVIDER -> LocationType.GPS
                NETWORK_PROVIDER -> LocationType.NETWORK
                else -> LocationType.OTHER
            }

            val weatherData = LocalWeather(
                    result.timestamp,
                    currentTime,
                    result.sunRise,
                    result.sunSet,
                    result.temperature,
                    result.pressure,
                    result.humidity,
                    result.cloudiness,
                    result.precipitation,
                    result.precipitationPeriod,
                    result.weatherCondition,
                    api.sourceName,
                    locationType
            )

            logger.info("Weather: {} {} {}", result, result.sunRise, result.sunSet)
            send(weatherTopic, weatherData)
        } catch (e: IOException) {
            logger.error("Could not get weather from {} API.", api)
        }
    }


    /**
     * Get last known location from GPS, if enabled. If GPS disabled, get location from network.
     * This location could be outdated if device was turned off and moved to another location.
     * @return Location or null if location could not be determined (not available or no permission)
     */
    private val lastKnownLocation: Location?
        get() {
            if (locationManager == null) {
                logger.error("Cannot get location without a location manager.")
                updateStatus(DeviceStatusListener.Status.DISCONNECTED)
                return null
            }
            return try {
                locationManager.getLastKnownLocation(GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (ex: SecurityException) {
                logger.error("Failed to get location", ex)
                null
            }
        }

    internal fun setQueryInterval(queryInterval: Long, unit: TimeUnit) {
        processor.interval(queryInterval, unit)
    }

    @Throws(IOException::class)
    override fun close() {
        networkReceiver.unregister()
        processor.close()
        super.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WeatherApiManager::class.java)

        private const val WEATHER_UPDATE_REQUEST_CODE = 627976615

        private const val ACTION_UPDATE_WEATHER = "org.radarcns.weather.WeatherApiManager.ACTION_UPDATE_WEATHER"
        internal const val SOURCE_OPENWEATHERMAP = "openweathermap"
    }
}