# Weather API

Application to be run on an Android 4.4 (or later) device.

This module requests current weather data from an external API and sends this to the backend. By default, the data is requested every three hours. In addition to basic weather metrics (temperature, pressure, humidity, precipitation), the module also sends the time of day of sunrise and sunset.

All data is based on the last known location of the phone. This location could be outdated. In order to prevent this, use this module in combination with the `PhoneLocationProvider` of the `org.radarcns:radar_android-phone` package.

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

### Api Key

A valid api key for the external Weather API needs to be supplied.
This key can be given through the remote config settings (`weather_api_key`) in the main Android Application or by adding a default in `WeaterhApiProvider.WEATHER_API_KEY_DEFAULT`.

## Contributing

To add a new weather source, implement the `WeatherApi` interface and add its instantiation in the `WeatherApiManager` constructor.

Code should be formatted using the [Google Java Code Style Guide](https://google.github.io/styleguide/javaguide.html), except using 4 spaces as indentation. Make a pull request once the code is working.
