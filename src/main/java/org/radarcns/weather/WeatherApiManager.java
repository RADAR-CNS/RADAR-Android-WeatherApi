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

public class WeatherApiManager extends AbstractDeviceManager<WeatherApiService, BaseDeviceState> implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(WeatherApiManager.class);

    private static final int WEATHER_UPDATE_REQUEST_CODE = 12345678;
    private static final String ACTION_UPDATE_WEATHER = "org.radarcns.weather.WeatherApiManager.ACTION_UPDATE_WEATHER";

    private final SharedPreferences preferences;
    private final OfflineProcessor processor;
    private final DataCache<MeasurementKey, Weather> weatherTable;

    private String apiKey;

    public WeatherApiManager(WeatherApiService service) {
        super(service, service.getDefaultState(), service.getDataHandler(), service.getUserId(), service.getSourceId());

        preferences = service.getSharedPreferences(WeatherApiManager.class.getName(), Context.MODE_PRIVATE);
        weatherTable = getCache(service.getTopics().getWeatherTopic());

        processor = new OfflineProcessor(service, this, WEATHER_UPDATE_REQUEST_CODE,
                ACTION_UPDATE_WEATHER, service.getQueryInterval(), false);

//        this.apiKey = apiKey;
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
        // TODO
        // Get location with LocationManager

        // Get Weather data

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
