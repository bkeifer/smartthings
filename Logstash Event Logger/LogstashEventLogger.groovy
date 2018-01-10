/**
 *  Event Logger
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
    name: "Logstash Event Logger",
    namespace: "bkeifer",
    author: "Brian Keifer",
    description: "Log SmartThings events to a Logstash server",
    category: "Convenience",
    iconUrl: "http://valinor.net/images/logstash-logo-square.png",
    iconX2Url: "http://valinor.net/images/logstash-logo-square.png",
    iconX3Url: "http://valinor.net/images/logstash-logo-square.png")


preferences {
    section("Log these presence sensors:") {
        input "presences", "capability.presenceSensor", multiple: true, required: false
    }
     section("Log these switches:") {
        input "switches", "capability.switch", multiple: true, required: false
    }
     section("Log these switch levels:") {
        input "levels", "capability.switchLevel", multiple: true, required: false
    }
    section("Log these motion sensors:") {
        input "motions", "capability.motionSensor", multiple: true, required: false
    }
    section("Log these temperature sensors:") {
        input "temperatures", "capability.temperatureMeasurement", multiple: true, required: false
    }
    section("Log these humidity sensors:") {
        input "humidities", "capability.relativeHumidityMeasurement", multiple: true, required: false
    }
    section("Log these contact sensors:") {
        input "contacts", "capability.contactSensor", multiple: true, required: false
    }
    section("Log these alarms:") {
        input "alarms", "capability.alarm", multiple: true, required: false
    }
    section("Log these indicators:") {
        input "indicators", "capability.indicator", multiple: true, required: false
    }
    section("Log these CO detectors:") {
        input "codetectors", "capability.carbonMonoxideDetector", multiple: true, required: false
    }
    section("Log these smoke detectors:") {
        input "smokedetectors", "capability.smokeDetector", multiple: true, required: false
    }
    section("Log these water detectors:") {
        input "waterdetectors", "capability.waterSensor", multiple: true, required: false
    }
    section("Log these acceleration sensors:") {
        input "accelerations", "capability.accelerationSensor", multiple: true, required: false
    }
    section("Log these energy meters:") {
        input "energymeters", "capability.energyMeter", multiple: true, required: false
    }

    section ("Logstash Server") {
        input "logstash_host", "text", title: "Logstash Hostname/IP"
        input "logstash_port", "number", title: "Logstash Port"
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
    // TODO: subscribe to attributes, devices, locations, etc.
    doSubscriptions()
}

def doSubscriptions() {
    subscribe(alarms,           "alarm",                    eventHandler)
    subscribe(codetectors,      "carbonMonoxideDetector",   eventHandler)
    subscribe(contacts,         "contact",                  eventHandler)
    subscribe(indicators,       "indicator",                eventHandler)
    subscribe(modes,            "locationMode",             eventHandler)
    subscribe(motions,          "motion",                   eventHandler)
    subscribe(presences,        "presence",                 eventHandler)
    subscribe(relays,           "relaySwitch",              eventHandler)
    subscribe(smokedetectors,   "smokeDetector",            eventHandler)
    subscribe(switches,         "switch",                   eventHandler)
    subscribe(levels,           "level",                    eventHandler)
    subscribe(temperatures,     "temperature",              eventHandler)
    subscribe(waterdetectors,   "water",                    eventHandler)
    subscribe(location,         "location",                 eventHandler)
    subscribe(accelerations,    "acceleration",             eventHandler)
    subscribe(energymeters,     "power",                    eventHandler)
}

def genericHandler(evt, specificsMap) {
    def body = [
        "date": evt.date,
        "eventName": evt.name,
        "displayName": evt.displayName,
        "device": "${evt.device}",
        "deviceId": "${evt.deviceId}",
        "value": evt.value,
        "isStateChange": evt.isStateChange(),
        "id": "${evt.id}",
        "description": "${evt.description}",
        "descriptionText": "${evt.descriptionText}",
        "installedSmartAppId": "${evt.installedSmartAppId}",
        "isoDate": evt.isoDate,
        "isDigital": evt.isDigital(),
        "isPhysical": evt.isPhysical(),
        "location": "${evt.location}",
        "locationId": "${evt.locationId}",
        "unit": "${evt.unit}",
        "source": "${evt.source}",
        "program": "SmartThings"
    ] + specificsMap
    
    def result = new physicalgraph.device.HubAction(
        method: "POST",
        path: "/",
        headers: [
                "HOST" : "${logstash_host}:${logstash_port}",
                "Content-Type": "application/json"],
        "body": body,
        null,
        [callback: calledBackHandler]        
    )
    sendHubCommand(result);
} 

void calledBackHandler(physicalgraph.device.HubResponse hubResponse) {
    log.debug "Entered calledBackHandler($hubResponse)..."
    log.debug "body in calledBackHandler() is: ${hubResponse.body}"
}

def eventHandler(evt) {
    def specificsMap = [:]
    switch(evt.name) {
      case "temperature":
      case "level":
          specificsMap[evt.name+"Value"] = Integer.parseInt(evt.value);
        break
      case "power":
        specificsMap[evt.name+"Value"] = Float.parseFloat(evt.value)
        break
      case "switch":
        specificsMap[evt.name+"Value"] = evt.value == "on"?true:false
        break
      case "contact":
        specificsMap[evt.name+"Value"] = evt.value == "closed"?true:false
        break
      case "motion":
        specificsMap[evt.name+"Value"] = evt.value == "active"?true:false
        break
    }
    genericHandler(evt, specificsMap)
}
