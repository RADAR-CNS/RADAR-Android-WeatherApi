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
import org.radarcns.android.device.DeviceServiceProvider;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class WeatherApiProvider extends DeviceServiceProvider<BaseDeviceState> {
    public static final String PREFIX =  "org.radarcns.weather.";
    public static final String WEATHER_QUERY_INTERVAL =  "weather_query_interval_seconds";
    public static final String WEATHER_QUERY_INTERVAL_KEY =  PREFIX + WEATHER_QUERY_INTERVAL;
    public static final long WEATHER_QUERY_INTERVAL_DEFAULT = TimeUnit.HOURS.toSeconds(1);

    @Override
    public Class<?> getServiceClass() {
        return WeatherApiProvider.class;
    }

    @Override
    public String getDisplayName() {
        return getActivity().getString(R.string.openWeatherApiServcieDisplayName);
    }

    @Override
    public List<String> needsPermissions() {
        return Arrays.asList(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION);
    }

    @Override
    public boolean isDisplayable() {
        return false;
    }

    @Override
    protected void configure(Bundle bundle) {
        super.configure(bundle);
        RadarConfiguration config = getConfig();
        bundle.putLong(WEATHER_QUERY_INTERVAL_KEY, config.getLong(
                WEATHER_QUERY_INTERVAL, WEATHER_QUERY_INTERVAL_DEFAULT));
    }
}
