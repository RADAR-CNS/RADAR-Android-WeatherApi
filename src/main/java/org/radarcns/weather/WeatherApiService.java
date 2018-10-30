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

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.radarcns.android.device.BaseDeviceState;
import org.radarcns.android.device.DeviceService;
import org.radarcns.config.ServerConfig;
import org.radarcns.producer.rest.RestClient;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

import static org.radarcns.weather.WeatherApiProvider.WEATHER_API_KEY_DEFAULT;
import static org.radarcns.weather.WeatherApiProvider.WEATHER_API_KEY_KEY;
import static org.radarcns.weather.WeatherApiProvider.WEATHER_API_SOURCE_DEFAULT;
import static org.radarcns.weather.WeatherApiProvider.WEATHER_API_SOURCE_KEY;
import static org.radarcns.weather.WeatherApiProvider.WEATHER_QUERY_INTERVAL_DEFAULT;
import static org.radarcns.weather.WeatherApiProvider.WEATHER_QUERY_INTERVAL_KEY;

public class WeatherApiService extends DeviceService<BaseDeviceState> {
    private long queryInterval = WEATHER_QUERY_INTERVAL_DEFAULT;
    private String apiSource = WEATHER_API_SOURCE_DEFAULT;
    private String apiKey = WEATHER_API_KEY_DEFAULT;
    private OkHttpClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        client = RestClient.global()
                .server(new ServerConfig())
                .build()
                .getHttpClient();
    }

    @Override
    protected WeatherApiManager createDeviceManager() {
        return new WeatherApiManager(this, apiSource, apiKey, client);
    }

    @Override
    protected BaseDeviceState getDefaultState() {
        return new BaseDeviceState();
    }

    long getQueryIntervalSeconds() {
        return queryInterval;
    }

    @Override
    protected void onInvocation(@NonNull Bundle bundle) {
        super.onInvocation(bundle);
        queryInterval = bundle.getLong(WEATHER_QUERY_INTERVAL_KEY);
        apiSource = bundle.getString(WEATHER_API_SOURCE_KEY);
        apiKey = bundle.getString(WEATHER_API_KEY_KEY);

        WeatherApiManager weatherApiManager = (WeatherApiManager) getDeviceManager();
        if (weatherApiManager != null) {
            weatherApiManager.setQueryInterval(queryInterval, TimeUnit.SECONDS);
        }
    }
}
