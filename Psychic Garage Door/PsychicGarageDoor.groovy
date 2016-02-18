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

    section ("Time Interval") {
        input "startTime", "time", title: "Starting at:", required: false
        input "endTime", "time", title: "Ending at:", required: false
    }

    section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to", required: false) {
            input "phone", "phone", title: "Warn with text message (optional)",
                description: "Phone Number", required: false
        }
    }

    section (hideable: true, "Advanced Settings") {
        input "controlSwitch", "capability.switch", title: "Only run if ALL of the following switches are on:", multiple: true, required: false
        input "logstash_host", "text", title: "Logstash Hostname/IP"
        input "logstash_port", "number", title: "Logstash Port"
    }

}

def installed() {
	stash "Installed with settings: ${settings}"
	state.currentPresence = presenceSensor.latestValue("presence")
	state.presenceLeft = 0
    state.lastOpen = 0
	initialize()
}

def updated() {
	stash "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(presenceSensor, "presence", presenceHandler)
    subscribe(app, presenceHandler)
    subscribe(interiorDoorSensor, "contact.open", interiorDoorHandler)
    state.waitingForInteriorDoor = false
}

def presenceHandler(evt) {
    if (timeInRange()) {
        stash ("presenceHandler - Time is in range")
        if (controlSwitchesAllOn) {
            stash ("presenceHandler - Control switches all on")
            stash ("Presence detected! ${evt.value}")
            if(evt.value == "present" && state.currentPresence == "not present") {
            	log.debug ("evt.value is present")
        		def gracePeriod = now() - ( grace * 60 * 1000 )
                log.debug ("diff: ${diff}")
                log.debug ("Grace period: ${gracePeriod}")

                if (gracePeriod > lastOpen) {
           	        stash ("Grace period check passed")
        		    if (garageDoorSensor.latestValue("contact") == "closed") {
        			    notify("Opening garage door!")
        			    stash("Opening door!")
                        state.waitingForInteriorDoor = true
                        state.lastOpen = now()
        			    relay.on()
                    }
                }
            	state.currentPresence = evt.value
        	} else if (evt.value == "not present") {
            	stash("Updating presenceLeft")
            	state.currentPresence = evt.value
        		state.presenceLeft = now()
        	}
        }
    }
}

def interiorDoorHandler(evt) {
    if (timeInRange()) {
        stash ("interiorDoorHandler - Time is in range")
        if (controlSwitchesAllOn) {
            stash ("interiorDoorHandler - Control switches all on")
            def timeDifference = now() - state.lastOpen
            if (garageDoorSensor.latestValue("contact") == "open") {
                stash ("Garage door is open.  Checking time differentials.")
                if (timeDifference > 30000 && timeDifference < 300000 && state.waitingForInteriorDoor == true) {
                    stash ("Interior door opened within proper window.  Closing door!")
                    relay.on()
                    state.waitingForInteriorDoor = false
                }
            } else {
                stash ("Garage door is not open.  Ignoring.")
            }
        }
    }
}

def notify(msg) {
    if (location.contactBookEnabled && recipients) {
        sendNotificationToContacts(msg, recipients)
    } else if (phone) {
        sendSms(phone, msg)
    }

}

def controlSwitchesAllOn() {
    def allOn = true
    controlSwitch?.each {
        if (it.currentSwitch != "on") {
            allOn = false
        }
    }
    return allOn
}

private timeInRange() {
    def rightNow  = now()
    def startTime = timeToday(settings."startTime").time
    def endTime   = timeToday(settings."endTime").time

    return (startTime > endTime ?
                    ( rightNow >= startTime || rightNow <= endTime) :
                    ( rightNow >= startTime && rightNow <= endTime))
}

def stash(msg) {
	log.debug(msg)
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
