/**
 *  Smart Bathroom Fan
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
    name: "Smart Bathroom Fan",
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "Turns on/off a switch (for an exhaust fan) based on humidity.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
    page(name: "prefPage")
}


mappings {
  path("/stamp")      { action: [ GET: "checkStamp", ] }
  path("/reschedule") { action: [ GET: "reschedule", ] }
}


def prefPage() {
    def explanationText = "The threshold settings below are in percent above the 24-hour rolling average humidity detected by the sensor specified above."

    // This gets messy, abandoning for now.  If sensor is changed, state needs to be cleared.
    // if (state.ambientHumidity) {
    //     def rollingAverage = state.ambientHumidity.sum() / state.ambientHumidity.size()
    //     explanationText = "${explanationText}\nOver the past ${state.ambientHumidity.size() * 5} minutes the average humidity at this sensor is ${rollingAverage}%."
    // }

    dynamicPage(name: "prefPage", title: "Preferences", uninstall: true, install: true) {
        section("What devices are we using?") {
            input "sensor", "capability.relativeHumidityMeasurement", title: "Humidity sensor:", required: true
            input "fanSwitch", "capability.switch", title: "Fan switch", required: true
        }
        section("Thresholds") {
            paragraph(explanationText)
            input "humidityHigh", "number", title: "Turn fan on when room is this percent above average humidity:", required: true
            input "humidityLow", "number", title: "Turn fan off when room is this percent above average humidity:", required: true
            input "fanDelay", "number", title: "Turn fan off after this many minutes regardless of humidity:", required: false
        }
        section("Enable?") {
            input "enabled", "bool", title: "Enable this SmartApp?"
        }
        section("Logstash") {
            input "useLogstash", "bool", title: "Enable Logstash logging?", submitOnChange: true
            if (useLogstash) {
                input "logstash_host", "text", title: "Logstash Hostname/IP"
                input "logstash_port", "number", title: "Logstash Port"
            }
        }
    }
}


def updateState() {
	log("State updated!")
    state.timestamp = now()
}


def createSchedule() {
    try { unschedule() }
    catch (e) { log.warn("Hamster fell off the wheel.") }
    updateState()
    runEvery5Minutes(updateAmbientHumidity)
}


def installed() {
	log "Installed with settings: ${settings}"
    state.ambientHumidity = []
    state.fanOn = null
	initialize()
}


def updated() {
	log "Updated with settings: ${settings}"
    if (!state.ambientHumidity) {
        log("Initializing array.")
        state.ambientHumidity = []
    }
	unsubscribe()
	initialize()
}


def initialize() {
    subscribe(sensor, "humidity", eventHandler)
    updateAmbientHumidity()
    createSchedule()
    logURLs()
}


def poke() {
    updateAmbientHumidity()
    createSchedule()
}


def eventHandler(evt) {
    def eventValue = Double.parseDouble(evt.value.replace('%', ''))
    Float rollingAverage = state.ambientHumidity.sum() / state.ambientHumidity.size()

    if (eventValue >= (rollingAverage + humidityHigh) && fanSwitch.currentSwitch == "off" && enabled) {
        if (enabled) {
            log("Humidity (${eventValue}) is more than ${humidityHigh}% above rolling average (${rollingAverage.round(1)}%).  Turning the fan ON.")
            state.fanOn = now()
            fanSwitch.on()
            //LOLscheduler
            if(fanDelay) {
                runIn(fanDelay * 60, fanOff)
            }
        } else {
            log("Humidity (${eventValue}) is more than ${humidityHigh}% above rolling average (${rollingAverage.round(1)}%).  APP is DISABLED, so not turning the fan ON.")
        }
    } else if (eventValue <= (rollingAverage + humidityLow) && fanSwitch.currentSwitch == "on") {
        if (enabled) {
            log("Humidity (${eventValue}) is at most ${humidityLow}% above rolling average (${rollingAverage}%).  Turning the fan OFF.")
            fanOff()
        } else {
            log("Humidity (${eventValue}) is at most ${humidityLow}% above rolling average (${rollingAverage}%).  APP is DISABLED, so not turning the fan OFF.")
        }
    } else if (state.fanOn != null && fanDelay && state.fanOn + fanDelay * 60000 <= now()){
        log("Fan timer elapsed and the hamster in the wheel powering the scheduler died.  Turning the fan OFF.  Current humidity is ${eventValue}%.")
        fanOff()
    } else if (state.fanOn != null && fanSwitch.currentSwitch == "off") {
        log("Fan turned OFF by someone/something else.  Resetting.")
        state.fanOn = null
    }

    if (now() - state.timestamp > 360000) {
        log("Scheduler hamster died.  Spawning a new one.")
        poke()
    }
}


def updateAmbientHumidity() {
    updateState()

    def q = state.ambientHumidity as Queue
    q.add(sensor.currentHumidity)

    while (q.size () > 288) { q.poll() }

    state.ambientHumidity = q

    Float rollingAverage = state.ambientHumidity.sum() / state.ambientHumidity.size()
    Float triggerPoint = rollingAverage + humidityHigh
    log("Rolling average: ${rollingAverage.round(1)}% - Currently ${sensor.currentHumidity}% - Trigger at ${triggerPoint.round(1)}%.")
}


def fanOff() {
    log("Turning fan OFF due to timer.")
    state.fanOn = null
    fanSwitch.off()
}


def logURLs() {
	if (!state.accessToken) {
		try {
			createAccessToken()
			log "Token: $state.accessToken"
		} catch (e) {
			log("Error.  Is OAuth enabled?")
		}
	}
    def baseURL = "https://graph.api.smartthings.com/api/smartapps/installations"
	log("Stamp URL:  ${baseURL}/${app.id}/stamp?access_token=${state.accessToken}")
	log("Reset URL:  ${baseURL}/${app.id}/reschedule?access_token=${state.accessToken}")
}


def checkStamp() {
    def result
    // 300000 = 5 minutes in milliseconds.  Replace with a value of at least
    // 2x the frequency at which your scheduled function should run.
    if (now() - state.timestamp < 300000) {
        result = "FIRING"
    } else {
        result = "FAIL"
    }
    render contentType: "text/html", data: "<!DOCTYPE html><html><head></head><body>${result}<br><hr><br>App: ${app.name}<br>Last timestamp: ${new Date(state.timestamp)}</body></html>"
}


def reschedule() {
  createSchedule()
  log("Rescheduled via web API call!")
  render contentType: "text/html", data: "<!DOCTYPE html><html><head></head><body>Rescheduled ${app.name}</body></html>"
}


def log(msg) {
	log.debug(msg)
    if (useLogstash) {
    	def dateNow = new Date()
        def isoDateNow = dateNow.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        def json = "{"
        json += "\"date\":\"${dateNow}\","
        json += "\"isoDate\":\"${isoDateNow}\","
        json += "\"name\":\"log\","
        json += "\"message\":\"${msg}\","
        json += "\"smartapp\":\"${app.name}\","
        json += "\"program\":\"SmartThings\""
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
}
