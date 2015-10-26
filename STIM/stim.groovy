/**
 *  SmartThings Infrastructure Monitor
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
    name: "SmartThings Infrastructure Monitor",
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "Monitors response times within the SmartThings cloud",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Event Target") {
		input "button", "capability.momentary", title:"Select device to monitor", multiple:false, required:true
	}
    section ("Graphite Server") {
    	input "graphite_host", "text", title: "Graphite Hostname/IP"
        input "graphite_port", "number", title: "Graphite Port"
    }
}

mappings {
  path("/stamp") {
    action: [
      GET: "html",
    ]
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
    unschedule()
	atomicState.start = 0
	atomicState.finish = 0
    atomicState.rtt = 0

    subscribe(app, appTouch)
	subscribe(button, "momentary.pushed", testHandler)
    schedule("0 * * * * ?", "getBenchmark")
    runEvery5Minutes(tick)
	getBenchmark()
}

def testHandler(evt) {
	atomicState.finish = now()
	log.debug("atomicState.start: ${atomicState.start} - as seen from event handler pre-pause")
	atomicState.rtt = atomicState.finish - atomicState.start
    log.debug("atomicState.finish: ${atomicState.finish}")
    log.debug("atomicState.rtt: ${atomicState.rtt}")
    logData(atomicState.rtt)
}


def appTouch(evt) {
	getBenchmark()
    log.debug(generateURL("stamp"))
}

def generateURL(path) {
	log.debug "resetOauth: $resetOauth"
	if (resetOauth) {
		log.debug "Reseting Access Token"
		state.accessToken = null
	}

	if (!resetOauth && !state.accessToken || resetOauth && !state.accessToken) {
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

def getBenchmark() {
	log.debug("--------------------------------------")
	atomicState.start = now()
	log.debug("atomicState.start: ${atomicState.start} - as seen from scheduled function")
    button.push()
}

def tick() {
    log.debug("Tick: ${now()}")
    state.timestamp = now()
}


def html() {
    def result
    if (now() - state.timestamp < 1200000) {
        result = "FIRING"
    } else {
        result = "FAIL"
    }
    render contentType: "text/html", data: "<!DOCTYPE html><html><head></head><body>${result}<br><hr><br>Last timestamp: ${new Date(state.timestamp)}</body></html>"
}


private logData(rtt) {
	def json = "{\"metric\":\"STEventRTT\",\"value\":\"${rtt as int}\",\"measure_time\":\"${(atomicState.finish/1000).toInteger()}\"}"
	def params = [
       	uri: "http://${graphite_host}:${graphite_port}/publish/performance",
        body: json
    ]
    //log.debug(json)
    try {
      	httpPostJson(params)// {response -> parseHttpResponse(response)}
    }
	catch ( groovyx.net.http.HttpResponseException ex ) {
      	log.debug "Unexpected response error: ${ex.statusCode}"
    }
}
