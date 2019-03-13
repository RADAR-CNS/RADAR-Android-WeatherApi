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

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import org.radarbase.android.device.BaseDeviceState
import org.radarbase.android.device.DeviceServiceProvider
import java.util.*

class WeatherApiProvider : DeviceServiceProvider<BaseDeviceState>() {
    override val description: String?
        get() = radarService?.getString(R.string.weather_api_description)

    override val serviceClass: Class<WeatherApiService> = WeatherApiService::class.java

    override val displayName: String
        get() = radarService!!.getString(R.string.weatherApiServcieDisplayName)

    override val permissionsNeeded: List<String> = Arrays.asList(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION)

    override val featuresNeeded: List<String> = listOf(PackageManager.FEATURE_LOCATION)

    override val sourceProducer: String = "OpenWeatherMap"

    override val sourceModel: String = "API"

    override val version: String = BuildConfig.VERSION_NAME

    override val isDisplayable: Boolean = false
}
