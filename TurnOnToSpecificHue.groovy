/**
 *  Turn on Hue to a specific... hue.
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
    name: "Turn on Hue to a specific... hue.",
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "Use a switch (or switches) to turn on one or more color-changing bulbs to a certain color.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Light(s)") {
        input "lights", "capability.colorControl", title: "Colored Light", multiple: true, required: true
    }
    section("Switch(es)") {
        input "switches", "capability.switch", title: "Switch", multiple: true, required: true
    }
    section("Choose light effects...") {
        input "color", "enum", title: "Hue Color?", required: false, multiple:false, options: [
            "Soft White",
            "White",
            "Daylight",
            "Warm White",
            "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
        input "lightLevel", "number", title: "Light Level? (1-99)", required: true
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
    subscribe(switches, "switch", switchHandler)
}

def switchHandler(evt) {
    log.debug(evt.value)
    if(evt.value == "on") {
        def hueColor = 70
        def saturation = 100

        switch(color) {
            case "White":
                hueColor = 52
                saturation = 19
                break;
            case "Daylight":
                hueColor = 53
                saturation = 91
                break;
            case "Soft White":
                hueColor = 23
                saturation = 56
                break;
            case "Warm White":
                hueColor = 20
                saturation = 80 //83
                break;
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

        def newValue = [hue: hueColor, saturation: saturation, level: lightLevel as Integer ?: 100]
        lights*.setColor(newValue)

    } else if (evt.value == "off") {
        lights*.off()
    }
}
