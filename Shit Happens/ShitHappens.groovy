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
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


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
    section("How far out should we look for freezing temps?") {
        input "hours", "number", title: "Hours", required: false, description: 24
    }
    section("Days between calendar alerts...") {
        input "caldays", "number", title: "Days", required: false
    }
    section("Switch to use to reset calendar alert") {
        input "calswitch", "capability.momentary", title: "Switch", required: false, multiple: false
    }
    section("Forecast API Key") {
        input "apikey", "text", required: false
    }
}


mappings {
  path("/stamp") {
    action: [
      GET: "html",
    ]
  }
  path("/reschedule") {
    action: [
      GET: "reschedule",
    ]
  }
}


def installed() {
    log.debug "Installed with settings: ${settings}"
    state.alerts = [:]
    state.contacts = [:]
    state.forecast = []
    state.dogmeds = -1
    initialize()
}


def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}


def initialize() {
    log.trace("initialize")
    subscribe(app, appTouch)
    subscribe(contacts, "contact.open", contactOpenHandler)
    subscribe(contacts, "contact.closed", contactClosedHandler)
    subscribe(calswitch, "momentary.pushed", calendarResetHandler)
	logURLs()
    createSchedule()
}


def createSchedule() {
	unschedule()
    checkAll()
    getForecast()
    runEvery5Minutes(checkAll)
    runEvery15Minutes(getForecast)
}


def contactOpenHandler(evt) {
    log.trace("contactOpenHandler")
    state.contacts[evt.displayName] = false
    state.alerts["contact"] = true
    flashToOn("Red")
    log.debug(state)
}


def contactClosedHandler(evt) {
    log.trace("contactClosedHandler")
    state.contacts[evt.displayName] = true
    log.debug(state.contacts)
    checkDoors()
    flash("Green")
    log.debug("continuing")
    updateHues()
}


def calendarResetHandler(evt) {
    log.trace("calendarResetHandler")
    state.dogmeds = now()
    state.alerts["dogmeds"] = false
    checkAll()
}

def flash(color) {
    3.times {
        turnOnToColor(color)
        pause(500)
        hues*.off()
        pause(500)
    }
}


def flashToOn(color) {
    2.times {
        turnOnToColor(color)
        pause(500)
        hues*.off()
        pause(500)
    }
    turnOnToColor(color)
}


def checkCalendar() {
    log.trace("checkCalendar")
    log.debug("CAL: ${state.alerts["dogmeds"]}")

    def elapsed = (now() - state["dogmeds"]) / 1000 / 60 / 60 / 24
    log.debug("Calendar Alert Elapsed Time: ${elapsed} days" )
    if (state.dogmeds == -1 || state.dogmeds == null || elapsed > caldays) {
        state.alerts["dogmeds"] = true
    } else {
        state.alerts["dogmeds"] = false
    }
}


def checkDoors() {
    log.trace("checkDoors")
    int openContacts = 0
    contacts.each {
        if (it.currentState("contact").value == "closed") {
            state.contacts[it.displayName] == true
        } else {
            state.contacts[it.displayName] == false
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
    log.debug(state)
}


def turnOnToColor(color, delay = 0) {
    log.trace("turnOnToColor(${color}) delay: ${delay}")
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
        hues*.on(delay: delay)
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
                                        //log.info("new low: ${templist[i]} at ${new Date( ((long)timelist[i])*1000 ) }")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(resp.status == 200) {
                log.debug "getLowTemp Request was OK"
            } else {
                log.error "getLowTemp Request got http status ${resp.status}"
            }
        }
    } catch(e) {
        log.debug e
    }
    return lowForecast
}


def appTouch(evt) {
    log.debug("TOUCHED!")
    log.trace("STATE: ${state}")
    log.trace("NOW: ${now()}")
    log.trace("DIFF: ${now() - state["dogmeds"]}")
    checkAll()
    log.debug(generateURL("stamp"))
}


def checkAll() {
    log.trace("checkAll")
    checkForFreeze()
    checkDoors()
    checkCalendar()
    updateHues()
    atomicState.timestamp = now()
}


def updateHues(delay = 0) {
    log.trace("updateHues delay:${delay}")
    log.debug("state.alerts[\"contact\"]: ${state.alerts["contact"]}")
    if(state.alerts["contact"] == true) {
        flash("Red")
    } else if (state.alerts["freeze"]) {
        turnOnToColor("Blue", delay)
    } else if(state.alerts["dogmeds"] == true) {
        turnOnToColor("Purple", delay)
    } else {
        log.debug("turning off!  delay: ${delay}")
        hues*.off(delay: delay)
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


def getForecast() {
    def params = [
        uri: "https://api.forecast.io",
        path: "/forecast/${apikey}/40.496754,-75.438682"
    ]

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

                                for (int i = 0; i <= 48; i++) {
                                    state.forecast[i] = templist[i]
                                }
                            }
                        }
                    }
                }
            }
            if(resp.status == 200) {
                log.debug "getForecast Request was OK"
            } else {
                log.error "getForecast Request got http status ${resp.status}"
            }
        }
    } catch(e) {
        log.debug e
    }
}


def generateURL(path) {
	if (!state.accessToken) {
		try {
			createAccessToken()
			log.debug "Creating new Access Token: $state.accessToken"
		} catch (ex) {
			log.error "Did you forget to enable OAuth in SmartApp IDE settings for ActiON Dashboard?"
			log.error ex
		}
	}

	["https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/$path", "?access_token=${state.accessToken}"]
}


def logURLs() {
	if (!state.accessToken) {
		try {
			createAccessToken()
			log.debug "Token: $state.accessToken"
		} catch (e) {
			log.debug("Error.  Is OAuth enabled?")
		}
	}
    def baseURL = "https://graph.api.smartthings.com/api/smartapps/installations"
	log.debug "Stamp URL:  ${baseURL}/${app.id}/stamp?access_token=${state.accessToken}"
	log.debug "Reset URL:  ${baseURL}/${app.id}/reschedule?access_token=${state.accessToken}"
}


def html() {
    def result
    log.trace("forecast: ${atomicState.fctimestamp}")
    log.trace("now: ${now()}")
    log.trace("stamp: ${atomicState.timestamp}")
    log.trace("diff: ${now() - atomicState.timestamp}")
    if (now() - atomicState.timestamp < 1200000) {
        result = "FIRING"
    } else {
        result = "FAIL"
    }
    render contentType: "text/html", data: "<!DOCTYPE html><html><head></head><body>${result}<br><hr><br>App: ${app.name} - Main<br>Last timestamp: ${new Date(atomicState.timestamp)}</body></html>"
}

def reschedule() {
  createSchedule()
  log.trace("Rescheduled via web API call!")
  render contentType: "text/html", data: "<!DOCTYPE html><html><head></head><body>Rescheduled ${app.name}</body></html>"
}
