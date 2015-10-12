/**
 *  Psychic Garage Door
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
    name: "Psychic Garage Door",
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "Garage doors that READ YOUR MIND!*\n\n*Garage door does not actually read your mind.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Garage Door Sensor:") {
    	input "garageDoorSensor", "capability.contactSensor", multiple: false, required: true
	}
	section("Garage Door Relay:") {
    	input "relay", "capability.relaySwitch", multiple: false, required: true
	}
	section("Presence Sensor:") {
    	input "presenceSensor", "capability.presenceSensor", multiple: false, required: true
	}
    section("Interior Door Sensor:") {
        input "interiorDoorSensor", "capability.contactSensor", multiple: false, required: false
    }
    section("Grace period:") {
		input "grace", "number", title: "Minutes", required: true
	}
    section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to", required: false) {
            input "phone", "phone", title: "Warn with text message (optional)",
                description: "Phone Number", required: false
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	state.currentPresence = presenceSensor.latestValue("presence")
	state.presenceLeft = 0
    state.lastOpen = 0
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(presenceSensor, "presence", presenceHandler)
    subscribe(app, presenceHandler)
    subscribe(interiorDoorSensor, "contact.open", interiorDoorHandler)
	log.debug("s.cp: ${state.currentPresence}")
}

def presenceHandler(evt) {
    log.debug ("Presence detected! ${evt.value}")
    if(evt.value == "present" && state.currentPresence == "not present") {
    	log.debug ("evt.value is present")
		def gracePeriod = now() - ( grace * 60 * 1000 )
        log.debug ("diff: ${diff}")
        log.debug ("Grace period: ${gracePeriod}")

        if (gracePeriod > lastOpen) {
   	        log.debug ("Grace period check passed")
		    if (garageDoorSensor.latestValue("contact") == "closed") {
			    notify("Opening garage door!")
			    log.debug("Opening door!")
                state.lastOpen = now()
			    relay.on()
            }
        }
    	state.currentPresence = evt.value
	} else if (evt.value == "not present") {
    	log.debug("Updating presenceLeft")
    	state.currentPresence = evt.value
		state.presenceLeft = now()
	}
}

def interiorDoorHandler(evt) {
    def timeDifference = now() - state.lastOpen
    if (garageDoorSensor.latestValue("contact") == "open") {
        log.debug ("Garage door is open.  Checking time differentials.")
        if (timeDifference > 30000 && timeDifference < 300000 ) {
            log.debug ("Interior door opened within proper window.  Closing door!")
            relay.on()
        }
    } else {
        log.debug ("Garage door is not open.  Ignoring.")
    }
}

def notify(msg) {
    if (location.contactBookEnabled && recipients) {
        sendNotificationToContacts(msg, recipients)
    } else if (phone) {
        sendSms(phone, msg)
    }

}
