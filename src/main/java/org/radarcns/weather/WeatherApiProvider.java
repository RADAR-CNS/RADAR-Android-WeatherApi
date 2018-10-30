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

import android.content.pm.PackageManager;
import android.os.Bundle;

import android.support.annotation.NonNull;
import org.radarcns.android.RadarConfiguration;
import org.radarcns.android.device.BaseDeviceState;
import org.radarcns.android.device.DeviceServiceProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static org.radarcns.weather.WeatherApiManager.SOURCE_OPENWEATHERMAP;

public class WeatherApiProvider extends DeviceServiceProvider<BaseDeviceState> {
    private static final String PREFIX =  "org.radarcns.weather.";
    private static final String WEATHER_QUERY_INTERVAL =  "weather_query_interval_seconds";
    static final String WEATHER_QUERY_INTERVAL_KEY =  PREFIX + WEATHER_QUERY_INTERVAL;
    private static final String WEATHER_API_SOURCE =  "weather_api_source";
    static final String WEATHER_API_SOURCE_KEY =  PREFIX + WEATHER_API_SOURCE;
    private static final String WEATHER_API_KEY =  "weather_api_key";
    static final String WEATHER_API_KEY_KEY =  PREFIX + WEATHER_API_KEY;

    static final long WEATHER_QUERY_INTERVAL_DEFAULT = TimeUnit.HOURS.toSeconds(3);
    static final String WEATHER_API_SOURCE_DEFAULT = SOURCE_OPENWEATHERMAP;
    static final String WEATHER_API_KEY_DEFAULT = "";

    @Override
    public String getDescription() {
        return getRadarService().getString(R.string.weather_api_description);
    }

    @Override
    public Class<?> getServiceClass() {
        return WeatherApiService.class;
    }

    @Override
    public String getDisplayName() {
        return getRadarService().getString(R.string.weatherApiServcieDisplayName);
    }

    @NonNull
    @Override
    public List<String> needsPermissions() {
        return Arrays.asList(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION);
    }

    @NonNull
    @Override
    public List<String> needsFeatures() {
        return Collections.singletonList(PackageManager.FEATURE_LOCATION);
    }

    @NonNull
    @Override
    public String getDeviceProducer() {
        return "OpenWeatherMap";
    }

    @NonNull
    @Override
    public String getDeviceModel() {
        return "API";
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public boolean isDisplayable() {
        return false;
    }

    @Override
    protected void configure(Bundle bundle) {
        super.configure(bundle);
        RadarConfiguration config = getConfig();
        bundle.putLong(WEATHER_QUERY_INTERVAL_KEY,
                config.getLong(WEATHER_QUERY_INTERVAL, WEATHER_QUERY_INTERVAL_DEFAULT));

        bundle.putString(WEATHER_API_KEY_KEY,
                config.getString(WEATHER_API_KEY, WEATHER_API_KEY_DEFAULT));

        bundle.putString(WEATHER_API_SOURCE_KEY,
                config.getString(WEATHER_API_SOURCE, WEATHER_API_SOURCE_DEFAULT));
    }
}
