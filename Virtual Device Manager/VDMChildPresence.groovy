/**
 *  VDM Child - Presence
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
    name: "VSM Child - Presence",
    namespace: "bkeifer/VDMChildPresence",
    parent: "bkeifer/VDM:Virtual Device Manager",
    author: "Brian Keifer",
    description: "VDM Child SmartApp to create new virtual switches.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "namePage", nextPage: "devicePage")
    page(name: "devicePage")
}

def devicePage() {
    dynamicPage(name: "devicePage", title: "New Virtual Presence Device", install: true, uninstall: childCreated()) {
        if (!childCreated()) {
            section { inputDeviceType() }
        } else {
            section { paragraph "Presence devices currently can not be converted to a different type after installation.\n\n${app.label}" }
        }
    }
}

def namePage() {
    dynamicPage(name: "namePage", title: "New Virtual Presence Device", install: false, uninstall: childCreated()) {
        section {
            label title: "Device Label:", required: true
        }
    }

}

def inputDeviceType() {
    input "deviceType", "enum", title: "Device Type:", required: true, options: ["On/Off Button Tile", "Momentary Button Tile"], defaultValue: "On/Off Button Tile"
}


def installed() {
    spawnChildDevice(app.label, settings.deviceType)
	initialize()
}


def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}


def initialize() {

}

def spawnChildDevice(deviceLabel, deviceType) {
    app.updateLabel(deviceLabel)
    if (!childCreated()) {
        def child = addChildDevice("smartthings", deviceType, getDeviceID(), null, [name: getDeviceID(), label: deviceLabel, completedSetup: true])
    }
}

def uninstalled() {
    getChildDevices().each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

private childCreated() {
    if (getChildDevice(getDeviceID())) {
        return true
    } else {
        return false
    }
}

private getDeviceID() {
    return "VSM_${app.id}"
}
