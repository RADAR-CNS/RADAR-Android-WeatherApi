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

package org.radarcns.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;

import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.OpenWeatherMap;

import org.json.JSONException;
import org.radarcns.android.data.DataCache;
import org.radarcns.android.device.AbstractDeviceManager;
import org.radarcns.android.device.BaseDeviceState;
import org.radarcns.android.device.DeviceStatusListener;
import org.radarcns.android.util.OfflineProcessor;
import org.radarcns.key.MeasurementKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;

public class WeatherApiManager extends AbstractDeviceManager<WeatherApiService, BaseDeviceState> implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(WeatherApiManager.class);

    private static final int WEATHER_UPDATE_REQUEST_CODE = 12345678;
    private static final String ACTION_UPDATE_WEATHER = "org.radarcns.weather.WeatherApiManager.ACTION_UPDATE_WEATHER";

    private final OfflineProcessor processor;
    private final DataCache<MeasurementKey, Weather> weatherTable;

    private LocationManager locationManager;
    private final String apiKey;

    public WeatherApiManager(WeatherApiService service, String apiKey) {
        super(service, service.getDefaultState(), service.getDataHandler(), service.getUserId(), service.getSourceId());

        weatherTable = getCache(service.getTopics().getWeatherTopic());

        locationManager = (LocationManager) service.getSystemService(Context.LOCATION_SERVICE);

        processor = new OfflineProcessor(service, this, WEATHER_UPDATE_REQUEST_CODE,
                ACTION_UPDATE_WEATHER, service.getQueryInterval(), false);

        this.apiKey = apiKey;

        updateStatus(DeviceStatusListener.Status.READY);
    }

    @Override
    public void start(@NonNull Set<String> acceptableIds) {
        logger.info("Starting WeatherApiManager");
        processor.start();

        updateStatus(DeviceStatusListener.Status.CONNECTED);
    }

    @Override
    public void run() {
        // Get last known location
        Location location = this.getLocation();

        if (location == null) {
            logger.error("Could not retrieve location for Weather API");
            return;
        }

        final Double lat = location.getLatitude();
        final Double lon = location.getLongitude();
        logger.info("Location: ({},{}) from {}", lat, lon, location.getProvider());

        // Get Weather data
        OpenWeatherMap weatherApi = new OpenWeatherMap(OpenWeatherMap.Units.METRIC, apiKey);
        CurrentWeather weatherApiResult;
        try {
            weatherApiResult = weatherApi.currentWeatherByCoordinates(lat.floatValue(), lon.floatValue());
        } catch (JSONException e) {
            e.printStackTrace();
            logger.error("Could not get weather from API.");
            return;
        }

        logger.info(weatherApiResult.toString());

        this.sendWeather(weatherApiResult, location.getProvider());
    }

    /**
     * Get last known location from GPS, if enabled. If GPS disabled, get location from network.
     * This location could be outdated if device was turned off and moved to another location.
     * @return Location or null if location could not be determined (not available or no permission)
     */
    private Location getLocation() {
        Location location;
        try {
            location = locationManager.getLastKnownLocation(GPS_PROVIDER);

            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            return location;
        } catch (SecurityException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void sendWeather(CurrentWeather weatherApiResult, String locationProvider) {
        // Precipitation from either rain or snow
        float precipitation = getPrecipitation(weatherApiResult);

        // Translation of weather condition
        WeatherCondition weatherCondition = null;
        if (weatherApiResult.hasWeatherInstance()) {
            // Get weather code of first weather condition instance
            int code = weatherApiResult.getWeatherInstance(0).getWeatherCode();
            weatherCondition = translateWeatherCode(code);
        }

        // Time of day of sunrise and sunset.
        final CurrentWeather.Sys sys = weatherApiResult.getSysInstance();
        double sunrise = getTimeOfDayFromDate(sys.getSunriseTime());
        double sunset = getTimeOfDayFromDate(sys.getSunsetTime());

        // How location was derived
        LocationType locationType;
        switch (locationProvider) {
            case GPS_PROVIDER:
                locationType = LocationType.GPS;
                break;
            case NETWORK_PROVIDER:
                locationType = LocationType.NETWORK;
                break;
            default:
                locationType = LocationType.OTHER;
        }

        // Instances
        final CurrentWeather.Main main = weatherApiResult.getMainInstance();
        final CurrentWeather.Clouds clouds = weatherApiResult.getCloudsInstance();

        double timestamp = System.currentTimeMillis();
        Weather weatherData = new Weather(timestamp, timestamp
                ,sunrise
                ,sunset
                ,main.hasTemperature() ? main.getTemperature() : null
                ,main.hasPressure() ? main.getPressure() : null
                ,main.hasHumidity() ? main.getHumidity() : null
                ,(weatherApiResult.hasCloudsInstance() && clouds.hasPercentageOfClouds()) ? clouds.getPercentageOfClouds() : null
                ,precipitation
                ,weatherCondition
                ,"OpenWeatherMap"
                ,locationType
                );

        send(weatherTable, weatherData);
    }

    private static float getPrecipitation(CurrentWeather weatherApiResult) {
        float totalPrecipitation = 0;

        final CurrentWeather.Rain rain = weatherApiResult.getRainInstance();
        if (weatherApiResult.hasRainInstance() && rain.hasRain3h()) {
            totalPrecipitation += rain.getRain3h();
        }

        final CurrentWeather.Snow snow = weatherApiResult.getSnowInstance();
        if (weatherApiResult.hasSnowInstance() && snow.hasSnow3h()) {
            totalPrecipitation += snow.getSnow3h();
        }

        return totalPrecipitation;
    }

    private static WeatherCondition translateWeatherCode(int code) {
        if (code >= 200 && code < 300) {
            return WeatherCondition.THUNDER;
        } else if (code >= 300 && code < 400) {
            return WeatherCondition.DRIZZLE;
        } else if (code >= 500 && code < 600) {
            return WeatherCondition.RAINY;
        } else if (code >= 600 && code < 700) {
            return WeatherCondition.SNOWY;
        } else if (code == 701 || code == 721 || code == 741) {
            return WeatherCondition.FOGGY;
        } else if (code == 800) {
            return WeatherCondition.CLEAR;
        } else if (code > 800 && code < 900) {
            return WeatherCondition.CLOUDY;
        } else if (code == 900 || code == 901 || code == 902 || code == 905 || code >= 957) {
            // tornado, tropical storm, hurricane, windy and high wind to hurricane
            return WeatherCondition.STORM;
        } else if (code == 906) {
            // hail
            return WeatherCondition.ICY;
        } else {
            return WeatherCondition.OTHER;
        }
    }

    /**
     * Get the time of day in hours from a date object up to seconds precision.
     * Assumes date object was made in the current timezone.
     * @param date
     * @return
     */
    private static double getTimeOfDayFromDate(Date date) {
        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        c.setTime(date);
        return c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60d + c.get(Calendar.SECOND) / 3600d;
    }

    void setQueryInterval(long queryInterval) {
        processor.setInterval(queryInterval);
    }

    @Override
    public void close() throws IOException {
        processor.close();
        super.close();
    }
}
