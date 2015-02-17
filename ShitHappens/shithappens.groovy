/**
 *  Shit Happens
 *
 *  Copyright 2015 Brian Keifer
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
    name: "Shit Happens",
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "Use one (or more) Hue lights to warn you about bad things happening.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Control these bulbs...") {
            input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:true, multiple:true
    }
    section("Warn for these contact sensors...") {
        input "contacts", "capability.contactSensor", title: "Contacts", required: false, multiple: true
    }
    section("Low temperature to trigger warning...") {
        input "lowtemp", "number", title: "In degrees F", required: false
    }
    section("Forecast API Key") {
        input "apikey", "text", required: false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    // TODO: subscribe to attributes, devices, locations, etc.
    log.trace("subscribing to app/apptouch")
    subscribe(app, appTouch)
}


def turnOnToColor(color) {
    def hueColor = 70
    def saturation = 100
    switch(color) {
            case "Blue":
                hueColor = 70
                break;
            case "Green":
                hueColor = 39
                break;
            case "Yellow":
                hueColor = 25
                break;
            case "Orange":
                hueColor = 10
                break;
            case "Purple":
                hueColor = 75
                break;
            case "Pink":
                hueColor = 83
                break;
            case "Red":
                hueColor = 100
                break;
    }
    if (color != "On - Custom Color")
    {
        def newValue = [hue: hueColor, saturation: saturation, level: lightLevel as Integer ?: 100]
        hues*.setColor(newValue)
        log.debug "new value = $newValue"
    }
    else
    {
        hues*.on()
    }
}

def getLowTemp() {
    def params = [
        uri: "https://api.forecast.io",
        path: "/forecast/${apikey}/40.496754,-75.438682"
    ]
    int lowForecast = 1000  // Ow!

    try {
        httpGet(params) { resp ->
            if (resp.data) {
                //log.debug "Response Data = ${resp.data}"
                //log.debug "Response Status = ${resp.status}"

                // resp.headers.each {
                //     log.debug "header: ${it.name}: ${it.value}"
                // }
                resp.getData().each {
                    if (it.key == "hourly") {
                        def x = it.value
                        x.each { xkey ->
                            if (xkey.key == "data") {
                                def y = xkey.value
                                def templist = y["temperature"][0..24]
                                templist.each { hourlyTemp ->
                                    if (hourlyTemp < lowForecast) {
                                        log.info("new low: ${hourlyTemp}")
                                        lowForecast = hourlyTemp
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(resp.status == 200) {
                log.debug "Request was OK"
            }
            else {
                log.error "Request got http status ${resp.status}"
            }
        }
    } catch(e) {
        log.debug e
    }
    return lowForecast
}


def appTouch(evt) {
    //hues*.off()

    log.trace("Getting forecast")
    if (getLowTemp() < lowtemp ) {
        log.debug("Freeze warning found, turning on lights.")
        turnOnToColor("Blue")
    } else {
        log.debug("Nothing to write home about.")
    }
}
