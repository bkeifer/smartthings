/**
 *  Laundry Monitor
 *
 *  Copyright 2014 Brandon Miller
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

import groovy.time.*

definition(
    name: "Laundry Monitor",
    namespace: "bmmiller",
    author: "Brandon Miller",
    description: "This application is a modification of the SmartThings Laundry Monitor SmartApp.  Instead of using a vibration sensor, this utilizes Power (Wattage) draw from an Aeon Smart Energy Meter.",
    category: "Convenience",
    iconUrl: "http://www.vivevita.com/wp-content/uploads/2009/10/recreation_sign_laundry.png",
    iconX2Url: "http://www.vivevita.com/wp-content/uploads/2009/10/recreation_sign_laundry.png")


preferences {
	section("Tell me when this washer/dryer has stopped..."){
		input "sensor1", "capability.powerMeter"
	}

    section("Notifications") {
		input "sendPushMessage", "bool", title: "Push Notifications?"
		input "phone", "phone", title: "Send a text message?", required: false
	}

	section("System Variables"){
    	input "minimumWattage", "decimal", title: "Minimum running wattage", required: false, defaultValue: 50
        input "minimumOffTime", "decimal", title: "Minimum amount of below wattage time to trigger off (secs)", required: false, defaultValue: 60
        input "message", "text", title: "Notification message", description: "Laundry is done!", required: true
	}

	section ("Additionally", hidden: hideOptionsSection(), hideable: true) {
        input "switches", "capability.switch", title: "Turn on these switches?", required:false, multiple:true
	    input "speech", "capability.musicPlayer", title:"Speak message via: ", multiple: true, required: false
	}

    section ("Logstash Server") {
        input "logstash_host", "text", title: "Logstash Hostname/IP"
        input "logstash_port", "number", title: "Logstash Port"
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    atomicState.running = false

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    atomicState.sound = safeTextToSpeech(message)
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(sensor1, "power", powerInputHandler)
}

def powerInputHandler(evt) {
	def latestPower = sensor1.currentValue("power")
    log.debug "Power updated: ${latestPower}W"
    if (!atomicState.isRunning && latestPower > minimumWattage) {
        atomicState.isRunning = true
        atomicState.startedAt = now()
        atomicState.stoppedAt = null
        stash "Cycle started.  Latest Power: ${latestPower}"
        dumpState()
    } else if (atomicState.isRunning && latestPower < minimumWattage) {
        stash "Power low."
        if (!atomicState.waiting) {
            stash "Entering wait mode."
            runIn(minimumOffTime, cycleDone)
            atomicState.waiting = true
        }
    } else if (atomicState.waiting && latestPower > minimumWattage) {
        stash "Cycle continuing!"
        atomicState.waiting = false
        unschedule()
    }
}

def cycleDone() {
    atomicState.isRunning = false
    atomicState.waiting = false
    atomicState.stoppedAt = now()
    dumpState()
    stash "startedAt: ${atomicState.startedAt}, stoppedAt: ${atomicState.stoppedAt}"
    stash message

    if (phone) {
        sendSms phone, message
    } else {
        sendPush message
    }

    if (switches) {
        switches*.on()
    }
    if (speech) {
        speech.playTrack(state.sound.uri)
    }

    atomicState.startedAt = null
    atomicState.stoppedAt = null
}

private textToSpeechT(message){
    if (message) {
    	if (ttsApiKey){
            [uri: "x-rincon-mp3radio://api.voicerss.org/" + "?key=$ttsApiKey&hl=en-us&r=0&f=48khz_16bit_mono&src=" + URLEncoder.encode(message, "UTF-8").replaceAll(/\+/,'%20') +"&sf=//s3.amazonaws.com/smartapp-" , duration: "${5 + Math.max(Math.round(message.length()/12),2)}"]
        }else{
        	message = message.length() >100 ? message[0..90] :message
        	[uri: "x-rincon-mp3radio://www.translate.google.com/translate_tts?tl=en&client=t&q=" + URLEncoder.encode(message, "UTF-8").replaceAll(/\+/,'%20') +"&sf=//s3.amazonaws.com/smartapp-", duration: "${5 + Math.max(Math.round(message.length()/12),2)}"]
     	}
    }else{
    	[uri: "https://s3.amazonaws.com/smartapp-media/tts/633e22db83b7469c960ff1de955295f57915bd9a.mp3", duration: "10"]
    }
}

private safeTextToSpeech(message) {
	message = message?:"You selected the Text to Speach Function but did not enter a Message"

    try {
        textToSpeech(message)
    }
    catch (Throwable t) {
        log.error t
        textToSpeechT(message)
    }
}


private dumpState() {
    log.debug(atomicState)
    log.debug("---------------------------------------")
}

private hideOptionsSection() {
  (phone || switches) ? false : true
}

def stash(msg) {
	log.debug(msg)
	def dateNow = new Date()
    def isoDateNow = dateNow.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    def json = "{"
    json += "\"date\":\"${dateNow}\","
    json += "\"isoDate\":\"${isoDateNow}\","
    json += "\"name\":\"log\","
    json += "\"message\":\"${msg}\","
    json += "\"smartapp\":\"${app.name}\""
    json += "}"
    def params = [
    	uri: "http://${logstash_host}:${logstash_port}",
        body: json
    ]
    try {
        httpPostJson(params)
    } catch ( groovyx.net.http.HttpResponseException ex ) {
       	log.debug "Unexpected response error: ${ex.statusCode}"
    }
}
