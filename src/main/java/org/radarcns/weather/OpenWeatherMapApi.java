package org.radarcns.weather;
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
 *
 * Created by Maxim on 06-09-17.
 */

import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.OpenWeatherMap;

import org.json.JSONException;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

class OpenWeatherMapApi implements WeatherApi {
    private OpenWeatherMap owm;
    private CurrentWeather cw;
    private static final String SOURCE_NAME = "OpenWeatherMap";
    private Double timestamp;

    OpenWeatherMapApi(String apiKey) {
        owm = new OpenWeatherMap(OpenWeatherMap.Units.METRIC, apiKey);
    }

    @Override
    public void loadCurrentWeather(Double latitude, Double longitude) throws Exception {
        try {
            cw = owm.currentWeatherByCoordinates(latitude.floatValue(), longitude.floatValue());
        } catch (JSONException ex) {
            throw new Exception("Could not parse weather data from the OpenWeatherMap API " +
                    "for latitude " + Double.toString(latitude) +
                    " and longitude " + Double.toString(longitude)
            );
        }

        if (cw.isValid()) {
            this.timestamp = System.currentTimeMillis() / 1000d;
        } else {
            throw new Exception("Could not get weather data from the OpenWeatherMap API " +
                    "for latitude " + Double.toString(latitude) +
                    " and longitude " + Double.toString(longitude)
            );
        }
    }

    @Override
    public Double getTimestamp() {
        return timestamp;
    }

    @Override
    public Float getTemperature() {
        CurrentWeather.Main weather = cw.getMainInstance();
        return (cw.hasMainInstance() && weather.hasTemperature()) ? weather.getTemperature() : null;
    }

    @Override
    public Float getHumidity() {
        CurrentWeather.Main weather = cw.getMainInstance();
        return (cw.hasMainInstance() && weather.hasPressure()) ? weather.getPressure() : null;
    }

    @Override
    public Float getPressure() {
        CurrentWeather.Main weather = cw.getMainInstance();
        return (cw.hasMainInstance() && weather.hasHumidity()) ? weather.getHumidity() : null;
    }

    @Override
    public Float getCloudiness() {
        CurrentWeather.Clouds clouds = cw.getCloudsInstance();
        return (cw.hasCloudsInstance() && clouds.hasPercentageOfClouds()) ? clouds.getPercentageOfClouds() : null;
    }

    @Override
    public Float getPrecipitation3h() {
        // Total precipitation from rain and snow
        float totalPrecipitation = 0;

        CurrentWeather.Rain rain = cw.getRainInstance();
        if (cw.hasRainInstance() && rain.hasRain3h()) {
            totalPrecipitation += rain.getRain3h();
        }

        CurrentWeather.Snow snow = cw.getSnowInstance();
        if (cw.hasSnowInstance() && snow.hasSnow3h()) {
            totalPrecipitation += snow.getSnow3h();
        }

        return totalPrecipitation;
    }

    @Override
    public WeatherCondition getWeatherCondition() {
        WeatherCondition weatherCondition = null;
        if (cw.hasWeatherInstance()) {
            // Get weather code of primary weather condition instance
            int code = cw.getWeatherInstance(0).getWeatherCode();
            weatherCondition = translateWeatherCode(code);
        }
        return weatherCondition;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public Double getSunRise() {
        CurrentWeather.Sys sys = cw.getSysInstance();
        return getTimeOfDayFromDate(sys.getSunriseTime());
    }

    @Override
    public Double getSunSet() {
        CurrentWeather.Sys sys = cw.getSysInstance();
        return getTimeOfDayFromDate(sys.getSunsetTime());
    }

    private static WeatherCondition translateWeatherCode(int code) {
        if (code >= 200 && code < 300) {
            return WeatherCondition.THUNDER;
        } else if (code >= 300 && code < 400) {
            return WeatherCondition.DRIZZLE;
        } else if (code >= 500 && code < 600) {
            return WeatherCondition.RAINY;
        } else if (code >= 600 && code < 700) {
            return WeatherCondition.SNOWY;
        } else if (code == 701 || code == 721 || code == 741) {
            return WeatherCondition.FOGGY;
        } else if (code == 800) {
            return WeatherCondition.CLEAR;
        } else if (code > 800 && code < 900) {
            return WeatherCondition.CLOUDY;
        } else if (code == 900 || code == 901 || code == 902 || code == 905 || code >= 957) {
            // tornado, tropical storm, hurricane, windy and high wind to hurricane
            return WeatherCondition.STORM;
        } else if (code == 906) {
            // hail
            return WeatherCondition.ICY;
        } else {
            return WeatherCondition.OTHER;
        }
    }

    /**
     * Get the time of day in hours from a date object up to seconds precision.
     * Assumes date object was made in the current timezone.
     * @param date a date object
     * @return hours from midnight in current timezone
     */
    private static double getTimeOfDayFromDate(Date date) {
        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        c.setTime(date);
        return c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60d + c.get(Calendar.SECOND) / 3600d;
    }
}
