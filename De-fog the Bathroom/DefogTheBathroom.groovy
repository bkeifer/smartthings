/**
 *  De-fog the Bathroom
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
    name: "De-fog the Bathroom",
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "Turns on/off a switch (for an exhaust fan) based on humidity",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Which humidity sensor should we use?") {
        input "sensor", "capability.relativeHumidityMeasurement", required: true
    }
    section("Which switch controls the fan?") {
        input "fanSwitch", "capability.switch", required: true
    }
    section("Turn the fan on if humidity is above:") {
        input "humidityHigh", "number", title: "%", required: true
    }
    section("Turn off when humidity goes below:") {
        input "humidityLow", "number", title: "%", required: true
    }

    section ("Logstash Server") {
        input "logstash_host", "text", title: "Logstash Hostname/IP"
        input "logstash_port", "number", title: "Logstash Port"
    }
}

def installed() {
	stash "Installed with settings: ${settings}"
    state.ambientHumidity = []
	initialize()
}

def updated() {
	stash "Updated with settings: ${settings}"
    if (!state.ambientHumidity) {
        log.debug("Initializing array.")
        state.ambientHumidity = []
    }

	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(sensor, "humidity", eventHandler)
    subscribe(app, appTouch)
    runEvery5Minutes(updateAmbientHumidity)
}

def appTouch(evt) {
    updateAmbientHumidity()
}

def eventHandler(evt) {
    def eventValue = Double.parseDouble(evt.value.replace('%', ''))

    if (eventValue >= humidityHigh && fanSwitch.currentSwitch == "off") {
        stash("Humidity (${eventValue}) is above threshold of ${humidityHigh}.  Turning the fan ON.")
        fanSwitch.on()
    } else if (eventValue <= humidityLow && fanSwitch.currentSwitch == "on") {
        stash("Humidity (${eventValue}) is below lower threshold of ${humidityHigh}.  Turning the fan OFF.")
        fanSwitch.off()
    }
}

def updateAmbientHumidity() {
    def q = state.ambientHumidity as Queue
    q.add(sensor.currentHumidity)

    while (q.size () > 288) {
        q.poll()
    }

    state.ambientHumidity = q

    stash("Rolling ambient humidity average: ${state.ambientHumidity.sum() / state.ambientHumidity.size()}")

}

def stash(msg) {
	log.debug(msg)
	def dateNow = new Date()
    // def isoDateNow = dateNow.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", location.timeZone)
    def isoDateNow = dateNow.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    def json = "{"
    json += "\"date\":\"${dateNow}\","
    json += "\"isoDate\":\"${isoDateNow}\","
    json += "\"name\":\"log\","
    json += "\"message\":\"${msg}\","
    json += "\"smartapp\":\"${app.name}\""
    json += "}"
    def params = [
    	uri: "http://${logstash_host}:${logstash_port}",
        body: json
    ]
    try {
        httpPostJson(params)
    } catch ( groovyx.net.http.HttpResponseException ex ) {
       	log.debug "Unexpected response error: ${ex.statusCode}"
    }
}
