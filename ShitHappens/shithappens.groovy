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
    section("Light level...") {
        input "level", "number", title: "In percent", required: false
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
    unschedule()
    log.trace("initialize")
    state.alerts = [:]
    state.contacts = [:]

    subscribe(app, appTouch)
    subscribe(contacts, "contact.open", contactOpenHandler)
    subscribe(contacts, "contact.closed", contactClosedHandler)

    runEvery5Minutes(checkAll)
}


def contactOpenHandler(evt) {
    log.trace("contactOpenHandler")
    state.contacts[evt.displayName] = false
    state.alerts["contact"] = true
    turnOnToColor("Red")
    log.debug(state)
}


def contactClosedHandler(evt) {
    log.trace("contactClosedHandler")
    state.contacts[evt.displayName] = true
    log.debug(state.contacts)
    turnOnToColor("Green")
    runIn(60, updateHues)
    checkDoors()
}

def checkDoors() {
    int openContacts = 0

    log.trace("checkDoors")
    state.contacts.each {
        //log.debug("CONTACT: ${it.key} - ${it.value}")
        if (it.value == false) {
            openContacts++
        }
    }
    if (openContacts > 0) {
        state.alerts["contact"] = true
        log.debug("Doors still open")
    } else {
         log.debug("All doors closed")
         state.alerts["contact"] = false
    }
    log.trace(state)
}


def turnOnToColor(color) {
    log.trace("turnOnToColor(${color})")
    def hueColor = 70
    def saturation = 100
    def lightLevel = 100
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
    if (color != "On - Custom Color") {
        def newValue = [hue: hueColor, saturation: saturation, level: lightLevel as Integer ?: 100]
        hues*.setColor(newValue)
        log.debug "new value = $newValue"
        } else {
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
                                def templist = y["temperature"]
                                def timelist = y["time"]

                                for (int i = 0; i <= 12; i++) {
                                    if ( templist[i] < lowForecast) {
                                        lowForecast = templist[i]
                                        log.info("new low: ${templist[i]} at ${new Date( ((long)timelist[i])*1000 ) }")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(resp.status == 200) {
                log.debug "Request was OK"
            } else {
                log.error "Request got http status ${resp.status}"
            }
        }
    } catch(e) {
        log.debug e
    }
    return lowForecast
}


def appTouch(evt) {
    checkAll()
    log.trace(state)
}


def checkAll() {
    log.trace("checkAll")
    checkForFreeze()
    checkDoors()
    updateHues()
}


def updateHues() {
    log.trace("updateHues")
    log.debug("state.alerts[\"contact\"]: ${state.alerts["contact"]}")
    if(state.alerts["contact"]) {
        turnOnToColor("Red")
    } else if (state.alerts["freeze"]) {
        turnOnToColor("Blue")
    } else {
        hues*.off()
    }
}


def checkForFreeze() {
    log.trace("checkForFreeze")
    if (getLowTemp() < lowtemp ) {
        log.debug("Freeze warning found, turning on lights.")
        state.alerts["freeze"] = true
    } else {
        state.alerts["freeze"] = false
        log.debug("The pipes are safe.  For now...")
    }
    log.trace(state)
}
