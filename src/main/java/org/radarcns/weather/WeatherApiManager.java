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

import org.radarcns.android.device.AbstractDeviceManager;
import org.radarcns.android.device.BaseDeviceState;
import org.radarcns.android.device.DeviceStatusListener;
import org.radarcns.android.util.NetworkConnectedReceiver;
import org.radarcns.android.util.OfflineProcessor;
import org.radarcns.kafka.ObservationKey;
import org.radarcns.passive.weather.LocalWeather;
import org.radarcns.passive.weather.LocationType;
import org.radarcns.topic.AvroTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;

public class WeatherApiManager extends AbstractDeviceManager<WeatherApiService, BaseDeviceState> {
    private static final Logger logger = LoggerFactory.getLogger(WeatherApiManager.class);

    private static final int WEATHER_UPDATE_REQUEST_CODE = 627976615;
    private static final Map<String, LocationType> LOCATION_TYPES = new HashMap<>(4);
    static {
        LOCATION_TYPES.put(GPS_PROVIDER, LocationType.GPS);
        LOCATION_TYPES.put(NETWORK_PROVIDER, LocationType.NETWORK);
    }
    private static final String ACTION_UPDATE_WEATHER = "org.radarcns.weather.WeatherApiManager.ACTION_UPDATE_WEATHER";
    static final String SOURCE_OPENWEATHERMAP = "openweathermap";

    private final OfflineProcessor processor;
    private final AvroTopic<ObservationKey, LocalWeather> weatherTopic = createTopic("android_local_weather", LocalWeather.class);
    private final NetworkConnectedReceiver networkReceiver;

    private LocationManager locationManager;
    private WeatherApi weatherApi;
    private volatile boolean doRequest;

    public WeatherApiManager(WeatherApiService service, String source, String apiKey,
            OkHttpClient client) {
        super(service);

        locationManager = (LocationManager) service.getSystemService(Context.LOCATION_SERVICE);

        processor = new OfflineProcessor.Builder(service)
                .addProcess(this::processWeather)
                .requestIdentifier(WEATHER_UPDATE_REQUEST_CODE, ACTION_UPDATE_WEATHER)
                .interval(service.getQueryIntervalSeconds(), TimeUnit.SECONDS)
                .wake(true)
                .build();

        networkReceiver = new NetworkConnectedReceiver(service,
                (isConnected, hasWifiOrEthernet) -> doRequest = isConnected);

        if (source.equals(SOURCE_OPENWEATHERMAP)) {
            weatherApi = new OpenWeatherMapApi(apiKey, client);
            logger.info("WeatherApiManager created with interval of {} seconds and key {}", service.getQueryIntervalSeconds(), apiKey);
        } else {
            logger.error("The weather api '{}' is not recognised. Please set a different weather api source.", source);
        }
    }

    @Override
    public void start(@NonNull Set<String> acceptableIds) {
        updateStatus(DeviceStatusListener.Status.READY);

        logger.info("Starting WeatherApiManager");
        networkReceiver.register();
        processor.start();

        updateStatus(DeviceStatusListener.Status.CONNECTED);
    }

    public void processWeather() {
        if (!doRequest) {
            logger.warn("No internet connection. Skipping weather query.");
        }

        Location location = this.getLastKnownLocation();
        if (location == null) {
            logger.error("Could not retrieve location. No input for Weather API");
            return;
        }

        try {
            WeatherApiResult result = weatherApi.loadCurrentWeather(location.getLatitude(), location.getLongitude());

            // How location was derived
            LocationType locationType = LOCATION_TYPES.get(location.getProvider());
            if (locationType == null) {
                locationType = LocationType.OTHER;
            }

            double timestamp = System.currentTimeMillis() / 1000d;
            LocalWeather weatherData = new LocalWeather(
                    result.getTimestamp(),
                    timestamp,
                    result.getSunRise(),
                    result.getSunSet(),
                    result.getTemperature(),
                    result.getPressure(),
                    result.getHumidity(),
                    result.getCloudiness(),
                    result.getPrecipitation(),
                    result.getPrecipitationPeriod(),
                    result.getWeatherCondition(),
                    weatherApi.getSourceName(),
                    locationType
            );

            logger.info("Weather: {} {} {}", result, result.getSunRise(), result.getSunSet());
            send(weatherTopic, weatherData);
        } catch (IOException e) {
            logger.error("Could not get weather from {} API.", weatherApi);
        }
    }

    /**
     * Get last known location from GPS, if enabled. If GPS disabled, get location from network.
     * This location could be outdated if device was turned off and moved to another location.
     * @return Location or null if location could not be determined (not available or no permission)
     */
    private Location getLastKnownLocation() {
        if (locationManager == null) {
            logger.error("Cannot get location without a location manager.");
            updateStatus(DeviceStatusListener.Status.DISCONNECTED);
            return null;
        }
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

    void setQueryInterval(long queryInterval, TimeUnit unit) {
        processor.setInterval(queryInterval, unit);
    }

    @Override
    public void close() throws IOException {
        networkReceiver.unregister();
        processor.close();
        super.close();
    }
}
