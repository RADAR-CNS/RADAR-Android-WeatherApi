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

import android.support.annotation.NonNull;

import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.OpenWeatherMap;

import org.json.JSONException;
import org.radarcns.passive.weather.WeatherCondition;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import okhttp3.OkHttpClient;

class OpenWeatherMapApi implements WeatherApi {
    private OpenWeatherMap owm;
    private static final String SOURCE_NAME = "OpenWeatherMap";

    OpenWeatherMapApi(String apiKey, OkHttpClient client) {
        owm = new OpenWeatherMap(OpenWeatherMap.UNITS_METRIC, OpenWeatherMap.LANGUAGE_ENGLISH,
                apiKey, client);
    }

    @Override
    public OpenWeatherMapApiResult loadCurrentWeather(double latitude, double longitude) throws IOException {
        CurrentWeather cw;
        try {
            cw = owm.currentWeatherByCoordinates((float) latitude, (float) longitude);
        } catch (JSONException ex) {
            throw new IOException("Could not parse weather data from the OpenWeatherMap API " +
                    "for latitude " + latitude + " and longitude " + longitude, ex);
        }

        if (cw.isValid()) {
            return new OpenWeatherMapApiResult(cw);
        } else {
            throw new IOException("Could not get weather data from the OpenWeatherMap API " +
                    "for latitude " + latitude + " and longitude " + longitude);
        }
    }

    private static class OpenWeatherMapApiResult implements WeatherApiResult {
        private final double timestamp;
        private final Float temperature;
        private final Integer sunSet;
        private final Float pressure;
        private final Float humidity;
        private final Float precipitation;
        private final Integer precipitationPeriod;
        private final Integer sunRise;
        private final CurrentWeather cw;

        OpenWeatherMapApiResult(CurrentWeather cw) {
            this.cw = cw;
            this.timestamp = System.currentTimeMillis() / 1000d;

            CurrentWeather.Main main = cw.getMainInstance();
            if (main != null) {
                temperature = main.hasTemperature() ? main.getTemperature() : null;
                pressure = main.hasPressure() ? main.getPressure() : null;
                humidity = main.hasHumidity() ? main.getHumidity() : null;
            } else {
                temperature = null;
                pressure = null;
                humidity = null;
            }

            CurrentWeather.Sys sys = cw.getSysInstance();
            if (sys != null) {
                sunRise = getTimeOfDayFromDate(sys.getSunriseTime());
                sunSet = getTimeOfDayFromDate(sys.getSunsetTime());
            } else {
                sunRise = null;
                sunSet = null;
            }

            precipitation = compute3hPrecipitation(cw.getRainInstance(), cw.getSnowInstance());
            precipitationPeriod = precipitation != null ? 3 : null;
        }

        @Override
        public double getTimestamp() {
            return timestamp;
        }

        @Override
        public Float getTemperature() {
            return temperature;
        }

        @Override
        public Float getPressure() {
            return pressure;
        }

        @Override
        public Float getHumidity() {
            return humidity;
        }

        @Override
        public Float getCloudiness() {
            CurrentWeather.Clouds clouds = cw.getCloudsInstance();
            return clouds != null && clouds.hasPercentageOfClouds() ? clouds.getPercentageOfClouds() : null;
        }

        @Override
        public Float getPrecipitation() {
            return precipitation;
        }

        @Override
        public Integer getPrecipitationPeriod() {
            return precipitationPeriod;
        }

        @NonNull
        @Override
        public WeatherCondition getWeatherCondition() {
            if (!cw.hasWeatherInstance()) {
                return WeatherCondition.UNKNOWN;
            }
            // Get weather code of primary weather condition instance
            int code = cw.getWeatherInstance(0).getWeatherCode();
            return translateWeatherCode(code);
        }

        @Override
        public Integer getSunRise() {
            return sunRise;
        }

        @Override
        public Integer getSunSet() {
            return sunSet;
        }

        @Override
        public String toString() {
            return cw.toString();
        }
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    private static Float compute3hPrecipitation(CurrentWeather.Rain rain, CurrentWeather.Snow snow) {
        if (rain != null || snow != null) {
            BigDecimal totalPrecipitation = BigDecimal.ZERO;
            if (rain != null && rain.hasRain3h()) {
                totalPrecipitation = totalPrecipitation.add(new BigDecimal(String.valueOf(rain.getRain3h())));
            }

            if (snow != null && snow.hasSnow3h()) {
                totalPrecipitation = totalPrecipitation.add(new BigDecimal(String.valueOf(snow.getSnow3h())));
            }
            return totalPrecipitation.floatValue();
        } else {
            return null;
        }
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
     * Get the time of day in minutes precision from a date object
     * in the current time zone of the device.
     * @param date a date object
     * @return whole minutes from midnight in current timezone
     */
    private static Integer getTimeOfDayFromDate(Date date) {
        if (date == null || date.getTime() == 0) {
            return null;
        }

        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        c.setTime(date);
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }
}
