/**
 *  Freeze Monitor
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
    name: "Freeze Monitor",
    namespace: "bkeifer/freezeMonitor",
    parent: "bkeifer/MANOS:Monitoring And Notification Of Switches",
    author: "Brian Keifer",
    description: "Freeze Monitor plugin for MANOS, the SmartApp of Fate\u00AE.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page name: "mainPage", title: "New Freeze Monitor", install: false, uninstall: true, nextPage: "namePage"
    page name: "namePage", title: "New Freeze Monitor", install: true, uninstall: true
}

//     section("Low temperature to trigger warning...") {
//         input "lowtemp", "number", title: "In degrees F", required: false
//     }
//     section("How far out should we look for freezing temps?") {
//         input "hours", "number", title: "Hours", required: false, description: 24
//     }
//     section("Forecast API Key") {
//         input "apikey", "text", required: false
//     }
// }

def mainPage() {
    dynamicPage(name: "mainPage") {
        section {
            inputLowtemp()
            inputHours()
            inputAPIKey()
        }
    }
}


def namePage() {
    if (!overrideLabel) {
        // If the user chooses not to change the label, give a default label.
        def l = "Freeze Monitor: " + settings.lowtemp + " degrees"
        log.debug("Will set default label of ${l}")
        app.updateLabel(l)
    }
    dynamicPage(name: "namePage") {
        if (overrideLabel) {
            section("Freeze Monitor Name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } else {
            section("Freeze Monitor Name") {
                paragraph app.label
            }
        }
        section {
            input "overrideLabel", "bool", title: "Edit Monitor Name", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}


def inputLowtemp() {
    input "lowtemp", "number", title: "In degrees F", required: true, defaultValue: 32
}


def inputHours() {
    input "hours", "number", title: "Hours", required: true, defaultValue: 24
}


def inputAPIKey() {
    input "apikey", "text", title: "Forecast.io API Key", required: true
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
    state.forecast = []
    runEvery5Minutes(checkCondition)
    runEvery15Minutes(getForecast)
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


def checkCondition() {
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
