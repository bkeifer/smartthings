/**
 *  Smart Porch Lights
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
    name: "Smart Porch Lights",
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "Porch lights that brighten temporarily upon your arrival.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Lights:") {
    	input "lights", "capability.switchLevel", multiple: true, required: true
	}
	section("Presence Sensor:") {
    	input "presenceSensor", "capability.presenceSensor", multiple: true, required: true
	}
    section("For how long should we brighten the lights?") {
		input "brighten", "number", title: "Minutes", defaultValue: 5, required: true
	}
    section("What level should the lights be set at normally?") {
        input "lvlDefault", "number", title: "1-100", defaultValue: 20, required: true
    }
    section("What level should the lights brighten to upon arrival?") {
        input "lvlArrival", "number", title: "1-100", defaultValue: 100, required: true
    }
    section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Warn with text message (optional)",
                description: "Phone Number", required: false
        }
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
	subscribe(presenceSensor, "presence", arrivalHandler)
    subscribe(location, "sunriseTime", sunriseHandler)
    subscribe(location, "sunsetTime", sunsetHandler)
    subscribe(app, arrivalHandler)
}


def sunriseHandler(evt) {
    lights*.off()
}


def sunsetHandler(evt) {
    lights*.setLevel(lvlDefault)
}


def arrivalHandler(evt) {
    if (evt.value == "present") {
        log.debug("Setting lights to arrival level.  Will dim in ${brighten.toString()} minutes.")
        for (light in lights) {
            if (light.currentValue("switch").toString() == "on") {
                light.setLevel(lvlArrival)
                runIn(brighten.toInteger() * 60, resetLevels)
            }
        }
    }
}


def resetLevels() {
    log.debug("Dimming lights back to normal level.")
    for (light in lights) {
        light.setLevel(lvlDefault)
    }
}

def notify(msg) {
    if (location.contactBookEnabled && recipients) {
        sendNotificationToContacts(msg, recipients)
    } else if (phone) {
        sendSms(phone, msg)
    }

}
