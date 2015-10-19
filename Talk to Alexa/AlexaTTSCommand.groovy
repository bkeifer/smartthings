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
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "Use a text-to-speech device to control Amazon Echo",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Talk to Alexa via this speaker:") {
		input "speech", "capability.musicPlayer", required: true, multiple: false
	}
    section("Your Echo's wake word:") {
        input "wakeWord", "enum", required: true, options: ["Alexa", "Amazon"]
    }
    section("Command to give Alexa (omit the wake word):") {
        input "command", "text", required: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    spawnChildDevice(command)
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    // atomicState.ttsCommand = textToSpeech("${settings.wakeWord}, ${settings.command}")
    atomicState.ttsCommand = textToSpeech("${settings.command}")
    def childSwitch = getChildDevices()
    // def updatedName = "Echo - ${settings.command}"
    def updatedName = "${settings.command}"
    log.debug("Child device label: ${childSwitch.label}")
    log.debug("New device label: ${updatedName}")

    // if (childSwitch.label != updatedName) {
    childSwitch.label = updatedName
    // }

	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(app, switchHandler)
    def childSwitch = getChildDevices()
    subscribe(childSwitch, "switch.on", switchHandler)
}

def spawnChildDevice(command) {
    def now = new Date()
    def stamp = now.getTime()
    def deviceID = "TalkToAlexa_${stamp}"
    // atomicState.deviceName = "Echo - ${command}"
    atomicState.deviceName = "${command}"
    log.debug("Unique Device ID: ${deviceID}")
    log.debug("Device Name: ${atomicState.deviceName}")

    def child = addChildDevice("bkeifer", "Momentary Button Tile [BTK]", deviceID, null, [name: atomicState.deviceName])
    log.debug("Child device: ${child}")

}

def switchHandler(evt) {
//    def ttsCommand = "${settings.wakeWord}, ${settings.command}"
    log.debug("Speaking: ${settings.command}")
    log.debug("State: ${atomicState}")
    //speech.playText(ttsCommand)
    // speech.playTrackAndRestore(atomicState.ttsCommand.uri, atomicState.ttsCommand.duration, volume)
    speech.playTrackAndRestore(atomicState.ttsCommand.uri, atomicState.ttsCommand.duration)

}
