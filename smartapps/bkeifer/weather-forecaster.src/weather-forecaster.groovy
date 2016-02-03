/**
 *  Weather Forecaster
 *
 *  Copyright 2016 Brian Keifer
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
    name: "Weather Forecaster",
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "TODO: Describe me.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
    section("Location") {
        input "latitude", "number", description: "Latitude", required: true
        input "longitude", "number", description: "Longitude", requred: true
    }
    section("Forecast API Key") {
        input "apikey", "text", required: false
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
    subscribe(app, getForecast)
}

def getForecast() {
    def params = [
        uri: "https://api.forecast.io",
        path: "/forecast/${apikey}/40.496754,-75.438682"
    ]

	try {
        httpGet(params) { resp ->
            if (resp.data) {
                log.debug "Response Data = ${resp.data}"
                log.debug "Response Status = ${resp.status}"
                resp.headers.each {
                    log.debug "header: ${it.name}: ${it.value}"
                }
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
