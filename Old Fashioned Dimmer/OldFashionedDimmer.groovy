/**
 *  Old Fashioned Dimmer
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
    name: "Old Fashioned Dimmer",
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "Makes a dimmer behave \"normally\" when operated manually.  It will ignore states/levels set by other SmartApps.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Watch this switch...") {
		input "dimmerSwitch", "capability.switchLevel"
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
    subscribe(dimmerSwitch, "level", switchHandler)
    subscribe(dimmerSwitch, "switch", switchHandler)
}

def switchHandler(evt) {
    log.debug("name: ${evt.name}")
    log.debug("physical: ${evt.isPhysical()}")
    log.debug("value: ${evt.value}")
    if (evt.isPhysical()) {
        switch (evt.name) {
            case "level":
                state.previousLevel = evt.value.toInteger()
                break
            case "switch":
                if (evt.value == "on") {
                    dimmerSwitch.setLevel(state.previousLevel)
                }
                break
        }
    }
}
