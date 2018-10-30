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

import java.io.IOException;

public interface WeatherApi {
    /**
     * Loads current weather at a set of coordinates.
     * @param latitude WGS84 latitude
     * @param longitude WGS84 longitude
     * @throws IOException if loading the current weather fails.
     */
    WeatherApiResult loadCurrentWeather(double latitude, double longitude) throws IOException;

    /**
     * Returns name of the source where the weather data was requested.
     * @return source name or {@code null} if none is set
     */
    String getSourceName();
}
