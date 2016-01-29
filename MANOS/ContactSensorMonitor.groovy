/**
 *  Contact Sensor Monitor
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
    name: "Contact Sensor Monitor",
    namespace: "bkeifer/contactMonitor",
    parent: "bkeifer/MANOS:Monitoring And Notification Of Switches",
    author: "Brian Keifer",
    description: "Contact Sensor Monitor plugin for MANOS, the SmartApp of Fate\u00AE.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page name: "mainPage", title: "New Contact Sensor Monitor", install: false, uninstall: true, nextPage: "namePage"
    page name: "namePage", title: "New Contact Sensor Monitor", install: true, uninstall: true
}


def mainPage() {
    dynamicPage(name: "mainPage") {
        section {
            inputContacts()
        }
    }
}


def namePage() {
    if (!overrideLabel) {
        // If the user chooses not to change the label, give a default label.
        def l = "Contact Sensor Monitor: " + settings.lowtemp + " degrees"
        log.debug("Will set default label of ${l}")
        app.updateLabel(l)
    }
    dynamicPage(name: "namePage") {
        if (overrideLabel) {
            section("Contact Sensor Monitor Name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } else {
            section("Contact Sensor Monitor Name") {
                paragraph app.label
            }
        }
        section {
            input "overrideLabel", "bool", title: "Edit Monitor Name", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}


def inputContacts() {
    input "contacts", "capability.contactSensor", title: "Contact Sensors", required: false, multiple: true
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
    state.contacts = [:]

    subscribe(contacts, "contact.open", contactOpenHandler)
    subscribe(contacts, "contact.closed", contactClosedHandler)

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
