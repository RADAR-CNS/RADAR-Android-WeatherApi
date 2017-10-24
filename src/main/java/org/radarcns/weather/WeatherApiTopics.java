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

import org.radarcns.android.device.DeviceTopics;
import org.radarcns.kafka.ObservationKey;
import org.radarcns.passive.weather.LocalWeather;
import org.radarcns.topic.AvroTopic;

public class WeatherApiTopics extends DeviceTopics {
    private static WeatherApiTopics instance = null;

    private final AvroTopic<ObservationKey, LocalWeather> weatherTopic;

    public static WeatherApiTopics getInstance() {
        synchronized (DeviceTopics.class) {
            if (instance == null) {
                instance = new WeatherApiTopics();
            }
            return instance;
        }
    }

    private WeatherApiTopics() {
        weatherTopic = createTopic("weather", LocalWeather.class);
    }

    public AvroTopic<ObservationKey, LocalWeather> getWeatherTopic() {
        return weatherTopic;
    }
}
