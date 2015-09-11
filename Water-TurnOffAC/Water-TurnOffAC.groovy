/**
 *  Water - Turn off the Air Conditioning
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
    name: "Water - Turn off the Air Conditioning",
    namespace: "",
    author: "Brian Keifer",
    description: "Use a water sensor to monitor the HVAC drip pan and turn the AC off before it overflows",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
	section("Water Sensor") {
        input "water", "capability.waterSensor", title: "Which Water Sensor?"
 	}
    section ("Thermostat(s)") {
    	input "thermostat", "capability.thermostat", title: "Which Thermostat(s)?", multiple: true
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
	subscribe(water, "water.wet", waterHandler)
}

def waterHandler(evt) {
	thermostat*.off()
    if (location.contactBookEnabled && recipients) {
        sendNotificationToContacts("Water in AC drip pan!  Turning off the AC!", recipients)
    } else if (phone) {
        sendSms(phone, "Water in AC drip pan!  Turning off the AC!")
    }
}
