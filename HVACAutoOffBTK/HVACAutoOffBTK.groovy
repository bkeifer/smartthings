/**
 *  HVAC Auto Off
 *
 *  Author: dianoga7@3dgo.net
 *  Date: 2013-07-21
 */

// Automatically generated. Make future change here.
definition(
    name: "Thermostat Auto Off [BTK]",
    namespace: "bkeifer",
    author: "dianoga7@3dgo.net",
    description: "Automatically turn off thermostat when windows/doors open. Turn it back on when everything is closed up.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    oauth: true
)

preferences {
	section("Control") {
		input("thermostat", "capability.thermostat", title: "Thermostat")
	}

    section("Open/Close") {
    	input("sensors", "capability.contactSensor", title: "Sensors", multiple: true)
        input("delay", "number", title: "Delay (seconds)")
    }
    section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Warn with text message (optional)",
                description: "Phone Number", required: false
        }
    }
}

def installed() {
	stash "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	stash "Updated with settings: ${settings}"

	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	state.changed = false
    state.pending = false
	subscribe(sensors, "contact", "sensorChange")
}

def sensorChange(evt) {
	stash("Sensor Changed: ${evt.value}")
    if(evt.value == 'open' && !state.pending) {
    	unschedule()
        state.pending = true
        runIn(delay, 'turnOff')
    } else if(evt.value == 'closed' && (state.changed || state.pending)) {
    	// All closed?
        def isOpen = false
        for(sensor in sensors) {
        	if(sensor.id != evt.deviceId && sensor.currentValue('contact') == 'open') {
        		isOpen = true
            }
        }

        if(!isOpen) {
        	unschedule()
        	if (state.pending) {
            	state.pending = false
			} else if (state.changed) {
        		runIn(delay, 'restore')
            }
        }
    }
}

def turnOff() {
	stash "Turning off thermostat due to contact open"
    notify("Turning off thermostat due to open door!")
	state.thermostatMode = thermostat.currentValue("thermostatMode")
	thermostat.off()
    state.pending = false
    state.changed = true
    stash "State: $state"
}

def restore() {
	notify("All doors closed.  Turning thermostat back on!")
    stash "Setting thermostat to ${state.thermostatMode}"
    thermostat.setThermostatMode(state.thermostatMode)
    state.changed = false
}

def notify(msg) {
    if (location.contactBookEnabled && recipients) {
        sendNotificationToContacts(msg, recipients)
    } else if (phone) {
        sendSms(phone, msg)
    }
}

def stash(msg) {
	log.debug(msg)
	TimeZone.setDefault(TimeZone.getTimeZone('UTC'))
	def dateNow = new Date()
    def isoDateNow = dateNow.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    def json = "{"
    json += "\"date\":\"${dateNow}\","
    json += "\"isoDate\":\"${isoDateNow}\","
    json += "\"name\":\"log\","
    json += "\"message\":\"${msg}\","
    json += "\"program\":\"SmartThings\""
    json += "}"
    def params = [
    	uri: "http://graphite.valinor.net:5279",
        body: json
    ]
    try {
        httpPostJson(params)
    } catch ( groovyx.net.http.HttpResponseException ex ) {
       	log.debug "Unexpected response error: ${ex.statusCode}"
    }
}
