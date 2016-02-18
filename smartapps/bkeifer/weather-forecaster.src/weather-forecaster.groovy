/**
 *  Weather Forecaster
 *
 *  Copyright 2016 Brian Keifer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Weather Forecaster",
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "TODO: Describe me.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
    section("Forecasts") {
        app(name:"childApps", appName: "Weather Forecaster Child", namespace: "bkeifer", title: "New Forecast", multiple: true)
    }
    section("Location") {
        input "latitude", "text", title: "Latitude", required: true
        input "longitude", "text", title: "Longitude", requred: true
    }
    section("Forecast API Key") {
        input "apikey", "text", required: false
    }
}


def installed() {
	log.debug "Installed with settings: ${settings}"
    state.forecast = []

	initialize()
}


def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}


def initialize() {
    state.forecast = []

    // subscribe(app, getForecast)
    subscribe(app, appTouch)
}


def appTouch(evt) {
    getForecast()
    checkForSnow()
    log.debug("1")
    log.debug("2")
    log.debug("3")
    log.debug("4")
    log.debug("5")
    log.debug("6")
    log.debug("7")
    log.debug("8")
    log.debug("9")
    log.debug("10")
    log.debug("11")
    log.debug("12")
    log.debug("13")
    log.debug("14")
    log.debug("15")
    log.debug("16")
    log.debug("17")
    log.debug("18")
    log.debug("19")
    log.debug("20")
}


def getChildDeviceType(forecastType) {
    switch (forecastType) {
        case "Rain":
        case "Snow":
            return "switch"
            break
        case "Humidity":
        case "Cloud Cover (percentage)":
        case "Wind Speed (mph)":
        case "Ozone":
            return "dimmer"
            break
    }
}


def checkForSnow() {
    def minutesUntilRain = null
    def precipToCheck = "snow"
    [state.minutely, state.hourly, state.daily].each {
        log.debug("###########################################################")
        it.each {
            if (it["precipType"] == precipToCheck) {
                def time = ((long)it["time"]) * 1000
                def date = new Date(time)//.format(location.timeZone)
                log.debug("time: ${time}")
                log.debug("FOUND AT ${date} - ${it["precipProbability"]} - ${it["precipIntensity"]}")
            }
        }
    }
}


def getWeatherTypes() {
    return ["Rain", "Snow", "Humidity", "Cloud Cover (percentage)", "Wind Speed (mph)", "Ozone"]
}


def getForecast() {
    def params = [
        uri: "https://api.forecast.io",
        path: "/forecast/${apikey}/40.496754,-75.438682"
    ]
    log.debug("params: ${params}")
	try {
        httpGet(params) { resp ->
            if (resp.data) {
                // log.debug "Response Data = ${resp.data}"
                // log.debug "Response Status = ${resp.status}"
                // resp.headers.each {
                //     log.debug "header: ${it.name}: ${it.value}"
                // }
                resp.getData().each {
                    // log.debug("it: ${it}")
                    if (it.key == "minutely") {
                        def x = it.value["data"]
                        def minutely = []
                        x.each { xkey ->
                            minutely.add(["time":                   xkey["time"],
                                          "precipProbability":      xkey["precipProbability"],
                                          "precipIntensity":        xkey["precipIntensity"],
                                          "precipIntensityError":   xkey["precipIntensityError"],
                                          "precipType":             xkey["precipType"]
                            ])
                        }
                        state.minutely = minutely
                    } else if (it.key == "hourly") {
                        def x = it.value["data"]
                        def hourly = []
                        x.each { xkey ->
                            hourly.add([
                                "time":                 xkey["time"],
                                "summary":              xkey["summary"],
                                "pressure":             xkey["pressure"],
                                "cloudCover":           xkey["cloudCover"],
                                "visibility":           xkey["visibility"],
                                "apparentTemperature":  xkey["apparentTemperature"],
                                "precipIntensity":      xkey["precipIntensity"],
                                "precipProbability":    xkey["precipProbability"],
                                "precipType":           xkey["precipType"],
                                "temperature":          xkey["temperature"],
                                "dewPoint":             xkey["dewPoint"],
                                "humidity":             xkey["humidity"],
                                "ozone":                xkey["ozone"],
                                "windSpeed":            xkey["windSpeed"],
                                "windBearing":          xkey["windBearing"]
                            ])
                        }
                        state.hourly = hourly
                    } else if (it.key == "daily") {
                        def x = it.value["data"]
                        def daily = []
                        x.each { xkey ->
                            daily.add([
                                "time":                       xkey["time"],
                                "summary":                    xkey["summary"],
                                "pressure":                   xkey["pressure"],
                                "cloudCover":                 xkey["cloudCover"],
                                "visibility":                 xkey["visibility"],
                                "apparentTemperature":        xkey["apparentTemperature"],
                                "apparentTemperatureMin":     xkey["apparentTemperatureMin"],
                                "apparentTemperatureMinTime": xkey["apparentTemperatureMinTime"],
                                "apparentTemperatureMax":     xkey["apparentTemperatureMax"],
                                "apparentTemperatureMaxTime": xkey["apparentTemperatureMaxTime"],
                                "precipIntensity":            xkey["precipIntensity"],
                                "precipIntensityMax":         xkey["precipIntensityMax"],
                                "precipIntensityMaxTime":     xkey["precipIntensityMaxTime"],
                                "precipProbability":          xkey["precipProbability"],
                                "precipType":                 xkey["precipType"],
                                "precipAccumulation":         xkey["precipAccumulation"],
                                "temperature":                xkey["temperature"],
                                "temperatureMin":             xkey["temperatureMin"],
                                "temperatureMinTime":         xkey["temperatureMinTime"],
                                "temperatureMax":             xkey["temperatureMax"],
                                "temperatureMaxTime":         xkey["temperatureMaxTime"],
                                "dewPoint":                   xkey["dewPoint"],
                                "humidity":                   xkey["humidity"],
                                "ozone":                      xkey["ozone"],
                                "windSpeed":                  xkey["windSpeed"],
                                "windBearing":                xkey["windBearing"],
                                "moonPhase":                  xkey["moonPhase"]
                            ])
                        }
                        state.daily = daily
                    }
                }
            }
            if(resp.status == 200) {
                // log.debug "getForecast Request was OK"
            } else {
                log.error "getForecast Request got http status ${resp.status}"
            }
        }
    } catch(e) {
        log.debug e
    }
}
