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
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;

import org.radarcns.android.data.DataCache;
import org.radarcns.android.device.AbstractDeviceManager;
import org.radarcns.android.device.BaseDeviceState;
import org.radarcns.android.device.DeviceStatusListener;
import org.radarcns.android.util.OfflineProcessor;
import org.radarcns.key.MeasurementKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;

public class WeatherApiManager extends AbstractDeviceManager<WeatherApiService, BaseDeviceState> implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(WeatherApiManager.class);

    private static final int WEATHER_UPDATE_REQUEST_CODE = 627976615;
    private static final String ACTION_UPDATE_WEATHER = "org.radarcns.weather.WeatherApiManager.ACTION_UPDATE_WEATHER";

    private final OfflineProcessor processor;
    private final DataCache<MeasurementKey, WeatherCurrent> weatherTable;

    private LocationManager locationManager;
    private WeatherApi weatherApi;
    private final String apiKey;

    public WeatherApiManager(WeatherApiService service, String apiKey) {
        super(service, service.getDefaultState(), service.getDataHandler(), service.getUserId(), service.getSourceId());

        weatherTable = getCache(service.getTopics().getWeatherTopic());

        locationManager = (LocationManager) service.getSystemService(Context.LOCATION_SERVICE);

        processor = new OfflineProcessor(service, this, WEATHER_UPDATE_REQUEST_CODE,
                ACTION_UPDATE_WEATHER, service.getQueryInterval(), true);

        this.apiKey = apiKey;
        weatherApi = new OpenWeatherMapApi(apiKey);

        logger.info("WeatherApiManager created with interval of {} seconds and key {}", service.getQueryInterval(), apiKey);
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
        Location location = this.getLastKnownLocation();
        if (location == null) {
            logger.error("Could not retrieve location. No input for Weather API");
            return;
        }

        try {
            weatherApi.loadCurrentWeather(location.getLatitude(), location.getLongitude());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Could not get weather from {} API.", weatherApi.getSourceName());
            return;
        }

        // How location was derived
        LocationType locationType;
        switch (location.getProvider()) {
            case GPS_PROVIDER:
                locationType = LocationType.GPS;
                break;
            case NETWORK_PROVIDER:
                locationType = LocationType.NETWORK;
                break;
            default:
                locationType = LocationType.OTHER;
        }

        double timestamp = System.currentTimeMillis() / 1000d;
        WeatherCurrent weatherData = new WeatherCurrent(
                weatherApi.getTimestamp()
                ,timestamp
                ,weatherApi.getSunRise()
                ,weatherApi.getSunSet()
                ,weatherApi.getTemperature()
                ,weatherApi.getPressure()
                ,weatherApi.getHumidity()
                ,weatherApi.getCloudiness()
                ,weatherApi.getPrecipitation()
                ,weatherApi.getPrecipitationPeriod()
                ,weatherApi.getWeatherCondition()
                ,weatherApi.getSourceName()
                ,locationType
        );

        logger.info("Weather: {} {} {}", weatherApi.toString(), weatherApi.getSunRise(), weatherApi.getSunSet());
        send(weatherTable, weatherData);
    }

    /**
     * Get last known location from GPS, if enabled. If GPS disabled, get location from network.
     * This location could be outdated if device was turned off and moved to another location.
     * @return Location or null if location could not be determined (not available or no permission)
     */
    private Location getLastKnownLocation() {
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

    void setQueryInterval(long queryInterval) {
        processor.setInterval(queryInterval);
    }

    @Override
    public void close() throws IOException {
        processor.close();
        super.close();
    }
}
