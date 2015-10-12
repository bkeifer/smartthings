/**
 *  Mutually Exclusive Switches
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
    name: "Mutually Exclusive Switches",
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "Turn off a switch (or group of switches) when a switch from another group is on.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Group 1:") {
        input "group1", "capability.switch", multiple: true, required: true
    }
    section("Group 2:") {
        input "group2", "capability.switch", multiple: true, required: true
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
	initialize()
}


def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}


def initialize() {
    subscribe(group1, "switch", group1Handler)
    subscribe(group2, "switch", group2Handler)
}


def group1Handler(evt) {
    if (evt.value == "on") {
        log.debug("Switch in group 1 is on.  Turning off group 2.")
        group2*.off()
    }
}


def group2Handler(evt) {
    if (evt.value == "on") {
        log.debug("Switch in group 2 is on.  Turning off group 1.")
        group1*.off()
    }
}


def notify(msg) {
    if (location.contactBookEnabled && recipients) {
        sendNotificationToContacts(msg, recipients)
    } else if (phone) {
        sendSms(phone, msg)
    }

}
