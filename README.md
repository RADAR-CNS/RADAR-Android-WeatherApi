# Weather API

Application to be run on an Android 4.4 (or later) device.

This module requests current weather data from an external API and sends this to the backend. By default, the data is requested every three hours. In addition to basic weather metrics (temperature, pressure, humidity, precipitation), the module also sends the time of day of sunrise and sunset.

All data is based on the last known location of the phone. This location could be outdated. In order to prevent this, use this module in combination with the `PhoneLocationProvider` of the `org.radarcns:radar-android-phone` package.

The following weather API is implemented:
 - [OpenWeatherMap](https://openweathermap.org/current)

## Installation

First, add the plugin code to your application:

```gradle
repositories {
    maven { url 'http://dl.bintray.com/radar-cns/org.radarcns' }
}

dependencies {
    compile 'org.radarcns:radar-android-weatherapi:0.1-alpha.1'
}
```

## Configuration
To enable this plugin add `.weather.WeatherApiProvider` to the `device_services_to_connect` property of the configuration in Firebase or `remote_config_defaults.xml`)

The following parameters are available:

| Parameter | Type | Default | Description |
| --------- | ---- | ------- | ----------- |
| `weather_api_key` | string | | The API key for the given API source. See below for a description of how a key can be retrieved. |
| `weather_api_source` | string | "openweathermap" | The name of the API where the weather data will be requested from. The only supported API for now is openweathermap.  |
| `weather_query_interval_seconds` | int (s) | 10,800 (=3 hours) | Interval between successive requests to the weather API. |

Data is sent to the `android_local_weather` topic using the `org.radarcns.passive.weather.LocalWeather` schema.

### OpenWeatherMap API key
The api key for access to the OpenWeatherMap API can be retrieved by [signing up for free](http://openweathermap.org/price#weather). Note that the free plan is subject to a maximum number of calls per minute and has a limited data update frequency.

## Contributing

To add a new weather source, implement the `WeatherApi` interface and add its instantiation in the `WeatherApiManager` constructor.

Code should be formatted using the [Google Java Code Style Guide](https://google.github.io/styleguide/javaguide.html), except using 4 spaces as indentation. Make a pull request once the code is working.
