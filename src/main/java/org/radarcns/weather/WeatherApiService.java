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

import org.radarcns.android.RadarConfiguration;
import org.radarcns.android.device.BaseDeviceState;
import org.radarcns.android.device.DeviceManager;
import org.radarcns.android.device.DeviceService;

import static org.radarcns.android.RadarConfiguration.SOURCE_ID_KEY;
import static org.radarcns.weather.WeatherApiProvider.WEATHER_API_KEY_DEFAULT;
import static org.radarcns.weather.WeatherApiProvider.WEATHER_API_KEY_KEY;
import static org.radarcns.weather.WeatherApiProvider.WEATHER_QUERY_INTERVAL_DEFAULT;
import static org.radarcns.weather.WeatherApiProvider.WEATHER_QUERY_INTERVAL_KEY;

public class WeatherApiService extends DeviceService {
    private String sourceId;
    private long queryInterval = WEATHER_QUERY_INTERVAL_DEFAULT;
    private String apiKey = WEATHER_API_KEY_DEFAULT;

    @Override
    protected DeviceManager createDeviceManager() {
        return new WeatherApiManager(this, apiKey);
    }

    @Override
    protected BaseDeviceState getDefaultState() {
        return new BaseDeviceState();
    }

    @Override
    protected WeatherApiTopics getTopics() {
        return WeatherApiTopics.getInstance();
    }

    public String getSourceId() {
        if (sourceId == null) {
            sourceId = RadarConfiguration.getOrSetUUID(getApplicationContext(), SOURCE_ID_KEY);
        }
        return sourceId;
    }

    public long getQueryInterval() {
        return queryInterval;
    }

    @Override
    protected void onInvocation(Bundle bundle) {
        super.onInvocation(bundle);
        queryInterval = bundle.getLong(WEATHER_QUERY_INTERVAL_KEY);
        apiKey = bundle.getString(WEATHER_API_KEY_KEY);

        WeatherApiManager weatherApiManager = (WeatherApiManager) getDeviceManager();
        if (weatherApiManager != null) {
            weatherApiManager.setQueryInterval(queryInterval);
        }
    }
}
