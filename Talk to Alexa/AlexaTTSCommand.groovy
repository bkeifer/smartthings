/**
 *  Alexa TTS Command
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
    name: "Alexa TTS Command",
    namespace: "bkeifer/AlexaTTSCommand",
    parent: "bkeifer/TalkToAlexa:Talk to Alexa",
    author: "Brian Keifer",
    description: "Use a text-to-speech device to control Amazon Echo",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page name: "mainPage", title: "New Text-to-speech Command", install: false, uninstall: true, nextPage: "namePage"
    page name: "namePage", title: "New Text-to-speech Command", install: true, uninstall: true
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        section {
            inputSpeaker()
            inputWakeWord()
            inputCommand()
        }
    }
}

def namePage() {
    if (!overrideLabel) {
        // if the user selects to not change the label, give a default label
        def l = settings.command
        log.debug "will set default label of $l"
        app.updateLabel(l)
    }
    dynamicPage(name: "namePage") {
        if (overrideLabel) {
            section("TTS Command Name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } else {
            section("TTS Command Name") {
                paragraph app.label
            }
        }
        section {
            input "overrideLabel", "bool", title: "Edit TTS command name", defaultValue: "false", required: "false", submitOnChange: true
        }
    }

}

def inputSpeaker() {
    input "speech", "capability.musicPlayer", title: "Talk to Echo via this speaker:", required: true, multiple: false
}

def inputWakeWord() {
    input "wakeWord", "enum", title: "Your Echo's wake word:", required: true, options: ["Alexa", "Amazon"], defaultValue: "Alexa"
}

def inputCommand() {
    input "command", "text", title: "Command to give Echo (omit the wake word):", required: true
}

def defaultLabel() {
    return settings.command
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    spawnChildDevice(command)
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

    state.ttsCommand = textToSpeech("${settings.wakeWord}, ${settings.command}")

    def childDevices = getChildDevices()

    for (childSwitch in childDevices) {
        def updatedName = "Echo - ${settings.command}"

        if (childSwitch.label != updatedName) {
            log.debug("Name udpated!  Changing label to: ${updatedName}")
            childSwitch.label = updatedName
        } else {
            log.debug("Name not updated.")
        }
    }

	unsubscribe()
	initialize()
}

def initialize() {
    if (!overrideLabel) {
        app.updateLabel(defaultLabel())
    }

    subscribe(app, switchHandler)
    def childSwitch = getChildDevices()
    subscribe(childSwitch, "switch.on", switchHandler)
}

def spawnChildDevice(command) {
    def now = new Date()
    def stamp = now.getTime()
    def deviceID = "TalkToAlexa_${stamp}"
    def deviceLabel = "Echo - ${command}"
    def child = addChildDevice("smartthings", "Momentary Button Tile", deviceID, null, [name: deviceID, label: deviceLabel])
}

def switchHandler(evt) {
    log.debug("Speaking: ${settings.command}")
    speech.playTrackAndRestore(state.ttsCommand.uri, state.ttsCommand.duration)
}
