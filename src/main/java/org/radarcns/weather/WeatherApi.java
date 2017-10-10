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

import org.radarcns.passive.weather.WeatherCondition;

public interface WeatherApi {

    /**
     * Loads current weather at a set of coordinates.
     * @param latitude
     * @param longitude
     * @throws Exception
     */
    void loadCurrentWeather(Double latitude, Double longitude) throws Exception;

    /**
     * returns timestamp of last weather load in seconds UTC
     * @return timestamp
     */
    Double getTimestamp();

    /**
     * Returns temperature in degrees Celsius. Or null if unknown.
     * @return temperature
     */
    Float getTemperature();

    /**
     * Returns current pressure in hPa. Or null if unknown.
     * @return pressure
     */
    Float getPressure();

    /**
     * Returns current humidity in percentage. Or null if unknown.
     * @return humidity
     */
    Float getHumidity();

    /**
     * Returns current cloudiness in percentage. Or null if unknown.
     * @return cloudiness
     */
    Float getCloudiness();

    /**
     * Returns precipitation of the last x hours in millimeter. Or null if unknown.
     * @return precipitation
     */
    Float getPrecipitation();

    /**
     * Returns hours over which the precipitation was measured. Or null if unknown.
     * @return precipitation
     */
    Integer getPrecipitationPeriod();

    /**
     * Returns the current weather condition (Clear, Cloudy, Rainy, etc.). Or null if unknown.
     * @return weather condition
     */
    WeatherCondition getWeatherCondition();

    /**
     * Returns the current time of day of sunrise in hours after midnight. Or null if unknown.
     * @return sunrise
     */
    Integer getSunRise();

    /**
     * Returns the current time of day of sunset in hours after midnight. Or null if unknown.
     * @return sunset
     */
    Integer getSunSet();

    /**
     * Returns name of the source where the weather data was requested.
     * @return source name
     */
    String getSourceName();

}
