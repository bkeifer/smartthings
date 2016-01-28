/**
 *  Holmes Smart Humidifier With WeMo
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
metadata {
	definition (name: "Holmes Smart Humidifier With WeMo", namespace: "bkeifer", author: "Brian Keifer") {
		capability "Actuator"
		capability "Polling"
		capability "Refresh"
		capability "Relative Humidity Measurement"
		capability "Switch"

        attribute "fanMode", "number"
		attribute "previousFanMode", "number"
        attribute "desiredHumidity", "string"
		attribute "waterLevel", "string"
        attribute "filterLife", "string"
        attribute "expiredFilterTime", "number"

        command "fanMax"
        command "fanHigh"
        command "fanMed"
        command "fanLow"
        command "fanMin"
        command "fanOff"

        command "hum45"
        command "hum50"
        command "hum55"
        command "hum60"
        command "humMax"

		command "resetFilterLife"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
        // multiAttributeTile(name:"humidifierMulti", type: "generic", width: 6, height: 4) {
        //     tileAttribute("device.fanMode", key: "PRIMARY_CONTROL") {
        //         attributeState("default", label: '${currentValue}')
        //         attributeState("0",   backgroundColor:"#888888")
        //         attributeState("20",  backgroundColor:"#B6CCFA")
        //         attributeState("40",  backgroundColor:"#95B6F8")
        //         attributeState("60",  backgroundColor:"#75A0F7")
        //         attributeState("80",  backgroundColor:"#548AF6")
        //         attributeState("100", backgroundColor:"#3474F5")
        //     }
        //     tileAttribute("device.desiredHumidity", key: "VALUE_CONTROL") {
        //         attributeState("default", action: "setDesiredHumidity")
        //     }
        //     tileAttribute("device.humidity", key: "SECONDARY_CONTROL") {
        //         attributeState("default", label:'Current Humidity: ${currentValue}%', unit:"%")
        //     }
        //     tileAttribute("device.fanMode", key: "SLIDER_CONTROL") {
        //         attributeState("level", action: "sendFanCommand")
        //     }
        // }

        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821"
            state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff"
        }

        standardTile("off", "device.fanMode",label:"Off") {
          state "default", label: 'OFF', action: "fanOff", icon:"st.Appliances.appliances11",backgroundColor:"#ffffff"
          state "0", label: 'OFF', action: "fanOff", icon:"st.Appliances.appliances11",backgroundColor:"#888888"
        }

        standardTile("min", "device.fanMode",label:"Min") {
          state "default", label: 'MIN', action: "fanMin", icon:"st.Appliances.appliances11",backgroundColor:"#ffffff"
          state "20", label: 'MIN', action: "fanMin", icon:"st.Appliances.appliances11",backgroundColor:"#B6CCFA"
        }

        standardTile("low", "device.fanMode",label:"Low") {
          state "default", label: 'LOW', action: "fanLow", icon:"st.Appliances.appliances11",backgroundColor:"#ffffff"
          state "40", label: 'LOW', action: "fanLow", icon:"st.Appliances.appliances11",backgroundColor:"#95B6F8"
        }

        standardTile("med", "device.fanMode") {
          state "default", label: 'MED', action: "fanMed", icon:"st.Appliances.appliances11",backgroundColor:"#ffffff"
          state "60", label: 'MED', action: "fanMed", icon:"st.Appliances.appliances11",backgroundColor:"#75A0F7"
        }

        standardTile("high", "device.fanMode") {
          state "default", label: 'HIGH', action: "fanHigh", icon:"st.Appliances.appliances11",backgroundColor:"#ffffff"
          state "80", label: 'HIGH', action: "fanHigh", icon:"st.Appliances.appliances11",backgroundColor:"#548AF6"
        }

        standardTile("max", "device.fanMode",label:"Max") {
          state "default", label: 'MAX', action: "fanMax", icon:"st.Appliances.appliances11",backgroundColor:"#ffffff"
          state "100", label: 'MAX', action: "fanMax", icon:"st.Appliances.appliances11",backgroundColor:"#3474F5"
        }

        standardTile("hum45", "device.desiredHumidity", label:"45%") {
            state "default", label: "45%", action: "hum45", icon:"st.Weather.weather12", backgroundColor:"#ffffff"
            state "45%", label: "45%", action: "hum45", icon:"st.Weather.weather12", backgroundColor:"#3333ff"
        }

        standardTile("hum50", "device.desiredHumidity", label:"50%") {
            state "default", label: "50%", action: "hum50", icon:"st.Weather.weather12", backgroundColor:"#ffffff"
            state "50%", label: "50%", action: "hum50", icon:"st.Weather.weather12", backgroundColor:"#3333ff"
        }

        standardTile("hum55", "device.desiredHumidity", label:"55%") {
            state "default", label: "55%", action: "hum55", icon:"st.Weather.weather12", backgroundColor:"#ffffff"
            state "55%", label: "55%", action: "hum55", icon:"st.Weather.weather12", backgroundColor:"#3333ff"
        }

        standardTile("hum60", "device.desiredHumidity", label:"60%") {
            state "default", label: "60%", action: "hum60", icon:"st.Weather.weather12", backgroundColor:"#ffffff"
            state "60%", label: "60%", action: "hum60", icon:"st.Weather.weather12", backgroundColor:"#3333ff"
        }

        standardTile("humMax", "device.desiredHumidity", label:"Max") {
            state "default", label: "Max", action: "humMax", icon:"st.Weather.weather12", backgroundColor:"#ffffff"
            state "Max", label: "Max", action: "humMax", icon:"st.Weather.weather12", backgroundColor:"#3333ff"
        }


        // valueTile("fanMode", "device.fanMode",decoration:"flat") {
        //     state "default", label:'Fan Speed: ${currentValue}'
        // }

//        valueTile("filterLife", "device.filterLife",decoration:"flat") {
//            state "default", label:'Filter Life:${currentValue}%'
//        }

//        valueTile("humidity", "device.humidity",decoration:"flat") {
//            state "default", label:'Humidity: ${currentValue}', unit:"%"
//        }

//        valueTile("desiredHumidity", "device.desiredHumidity",decoration:"flat") {
//            state "default", label:'Desired Humidity: ${currentValue}', unit:"%"
//        }


        standardTile("refresh", "device.refresh", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        valueTile("fanLevel", "device.fanMode", decoration: "flat") {
            state "level", label:'Fan Level: ${currentValue}'
        }
        controlTile("fanSliderControl", "device.fanMode", "slider", height: 1, width: 2) {
                 state "level", action:"sendFanCommand"
        }

        valueTile("desiredHumidity", "device.desiredHumidity", width: 2, height: 2, decoration: "flat") {
            state "desiredHumidity", label:'Setpoint: ${currentValue}'
        }

        valueTile("mainTile", "device.desiredHumidity", width: 2, height: 2, decoration: "flat") {
            state "desiredHumidity", label:'${currentValue}'
        }

        valueTile("humidity", "device.humidity", width: 2, height: 2, decoration: "flat") {
            state "humidity", label:'Current: ${currentValue}%'
        }

		valueTile("filterLife", "device.filterLife", width: 2, height: 2, decoration: "flat") {
			state "filterLife", label:'Filter: ${currentValue}%'
		}

		valueTile("waterLevel", "device.waterLevel", width: 2, height: 2, decoration: "flat") {
			state "waterLevel", label:'Water: ${currentValue}'
		}

        controlTile("humiditySliderControl", "device.desiredHumidity", "slider", height: 1, width: 2) {
            state "desiredHumidity", action:"sendHumidityCommand"
        }

        main "mainTile"
        details (["switch", "waterLevel", "refresh", "off", "min", "low", "med", "high", "max", "hum45", "hum50", "hum55", "hum60", "humMax", "humidity", "desiredHumidity", "filterLife"])
	}
}


def parse(String description) {

    def evtMessage = parseLanMessage(description)
    def evtHeader = evtMessage.header
    def evtBody = evtMessage.body
    evtBody = evtBody.replaceAll(~/&amp;/, "&")
    evtBody = evtBody.replaceAll(~/&lt;/, "<")
    evtBody = evtBody.replaceAll(~/&gt;/, ">")

    // log.debug("Header: ${evtHeader}")
    // log.debug("Body: ${evtBody}")

    if (evtHeader?.contains("SID: uuid:")) {
		def sid = (evtHeader =~ /SID: uuid:.*/) ? ( evtHeader =~ /SID: uuid:.*/)[0] : "0"
		sid -= "SID: uuid:".trim()
        log.debug "Subscription updated!  New SID: ${sid}"
    	updateDataValue("subscriptionId", sid)
    }

    if (evtBody) {
        log.debug("evtBody: ${evtBody}")
        def body = new XmlSlurper().parseText(evtBody)
        if (body == 0) {
            log.debug ("Command succeeded!")
            return [sendGetAttributesCommand()]
        } else {
            log.debug("ELSE!: ${body.Body}")
            // FanMode
            // DesiredHumidity
            // CurrentHumidity
            // WaterAdvise
            // NoWater
            // FilterLife
            // ExpiredFilterTime
            try {
                def regex = /FanMode(\d)DesiredHumidity(\d)CurrentHumidity([\d\.]+)WaterAdvise(\d)NoWater(\d)FilterLife(\d+)ExpiredFilterTime(\d)/
                // def matcher = body.Body =~

                def matchResponse = body.Body =~ /FanMode(\d)DesiredHumidity(\d)CurrentHumidity([\d\.]+)WaterAdvise(\d)NoWater(\d)FilterLife(\d+)ExpiredFilterTime(\d)/
                // log.debug("mFM: ${matchResponse[0]}")
                def result = []
                def fanMode
                def desiredHumidity
				def filterLife
				def waterLevel

                switch(matchResponse[0][1].toInteger()) {
                    case 0:
                        fanMode = "0"
                        break
                    case 1:
                        fanMode = "20"
                        break
                    case 2:
                        fanMode = "40"
                        break
                    case 3:
                        fanMode = "60"
                        break
                    case 4:
                        fanMode = "80"
                        break
                    case 5:
                        fanMode = "100"
                        break
                }

                switch(matchResponse[0][2].toInteger()) {
                    case 0:
                        desiredHumidity = "45%"
                        break
                    case 1:
                        desiredHumidity = "50%"
                        break
                    case 2:
                        desiredHumidity = "55%"
                        break
                    case 3:
                        desiredHumidity = "60%"
                        break
                    case 4:
                        desiredHumidity = "Max"
                        break
                }

                result += createEvent(name: "fanMode", value:fanMode)
                result += createEvent(name: "desiredHumidity", value:desiredHumidity)
                result += createEvent(name: "humidity", value:matchResponse[0][3])

				if (matchResponse[0][5] == "1") {
					waterLevel = "Empty"
				} else if (matchResponse[0][4] == "1") {
					waterLevel = "Low"
				} else {
					waterLevel = "OK"
				}
				result += createEvent(name: "waterLevel", value: waterLevel)

                result += createEvent(name: "waterAdvise", value:matchResponse[0][4])
                result += createEvent(name: "noWater", value:matchResponse[0][5])

				filterLife = ((matchResponse[0][6].toInteger() / 60480) * 100)
				log.debug("filterLife: ${filterLife}")
                result += createEvent(name: "filterLife", value: filterLife)

                result += createEvent(name: "expiredFilterTime", value:matchResponse[0][7])

                log.debug("Result: ${result}")
                return result
            } catch (e) {
                log.debug("Exception ${e}")
            }
            // if (curFanMode.getCount()) {
            //     log.debug("Current Fan Mode: ${curFanMode[0]}")
            // }
        }
//        result << createEvent(name: value:)
    }

}

private getCallBackAddress() {
    device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}


private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}


private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}


private getHostAddress() {
    def ip = getDataValue("ip")
    def port = getDataValue("port")
    log.debug("HOST: ${ip}:${port}")
    if (!ip || !port) {
        def parts = device.deviceNetworkId.split(":")
        if (parts.length == 2) {
            ip = parts[0]
            port = parts[1]
        } else {
            //log.warn "Can't figure out ip and port for device: ${device.id}"
        }
    }

    //convert IP/port
    ip = convertHexToIP(ip)
    port = convertHexToInt(port)
    //log.debug "Using ip: ${ip} and port: ${port} for device: ${device.id}"
    return ip + ":" + port
}


private postRequest(path, SOAPaction, body) {
    // Send  a post request
    def result = new physicalgraph.device.HubAction([
        'method': 'POST',
        'path': path,
        'body': body,
        'headers': [
        'HOST': getHostAddress(),
        'Content-type': 'text/xml; charset=utf-8',
        'SOAPAction': "\"${SOAPaction}\""
        ]
        ], device.deviceNetworkId)
    log.debug(result)
    return result
}


def poll() {
	refresh()
}


def on() {
	switch (device.latestState('previousFanMode').stringValue) {
		case "20":
			fanMin()
			break
		case "40":
			fanLow()
			break
		case "60":
			fanMed()
			break
		case "80":
			fanHigh()
			break
		case "100":
			fanMax()
			break
	}
}

def off() {
	fanOff()
}


def fanMax()  { sendFanCommand("Max")      }
def fanHigh() { sendFanCommand("High")     }
def fanMed()  { sendFanCommand("Med")      }
def fanLow()  { sendFanCommand("Low")      }
def fanMin()  { sendFanCommand("Min")      }

def fanOff()  {
	def currentFanMode = device.latestState('fanMode').stringValue
	log.debug("sending event: ${currentFanMode}")
	sendEvent(name: "previousFanMode", value: currentFanMode, displayed: false)
	sendFanCommand("Off")
}

def hum45()   { sendHumidityCommand("45%") }
def hum50()   { sendHumidityCommand("50%") }
def hum55()   { sendHumidityCommand("55%") }
def hum60()   { sendHumidityCommand("60%") }
def humMax()  { sendHumidityCommand("Max") }


def sendFanCommand(level) {
    def newLevel

    switch(level) {
        case "Off":
        case 0..5:
            newLevel = 0
            break
        case "Min":
        case 6..27:
            newLevel = 1
            break
        case "Low":
        case 28..49:
            newLevel = 2
            break
        case "Med":
        case 50..71:
            newLevel = 3
            break
        case "High":
        case 72..93:
            newLevel = 4
            break
        case "Max":
        case 94..100:
            newLevel = 5
            break
    }

    def body = """
    <?xml version="1.0" encoding="utf-8"?>
    <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    <s:Body>
    <u:SetAttributes xmlns:u="urn:Belkin:service:deviceevent:1">
    <attributeList>&lt;attribute&gt;&lt;name&gt;FanMode&lt;/name&gt;&lt;value&gt;${newLevel}&lt;/value&gt;&lt;/attribute&gt;</attributeList>
    </u:SetAttributes>
    </s:Body>
    </s:Envelope>
    """
    postRequest('/upnp/control/deviceevent1', 'urn:Belkin:service:deviceevent:1#SetAttributes', body)
}

def sendHumidityCommand(level) {
    def newLevel

    switch(level) {
        case "45%":
        case 0..20:
            newLevel = 0
            break
        case "50%":
        case 21-40:
            newLevel = 1
            break
        case "55%":
        case 41-60:
            newLevel = 2
            break
        case "60%":
        case 61-80:
            newLevel = 3
            break
        case "Max":
        case 81-100:
            newLevel = 4
            break
    }

    def body = """
    <?xml version="1.0" encoding="utf-8"?>
    <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    <s:Body>
    <u:SetAttributes xmlns:u="urn:Belkin:service:deviceevent:1">
    <attributeList>&lt;attribute&gt;&lt;name&gt;DesiredHumidity&lt;/name&gt;&lt;value&gt;${newLevel}&lt;/value&gt;&lt;/attribute&gt;</attributeList>
    </u:SetAttributes>
    </s:Body>
    </s:Envelope>
    """
    postRequest('/upnp/control/deviceevent1', 'urn:Belkin:service:deviceevent:1#SetAttributes', body)
}

def sendGetAttributesCommand() {
    log.debug("sendGetAttributesCommand()")
    def body = """
    <?xml version="1.0" encoding="utf-8"?>
    <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    <s:Body>
    <u:GetAttributes xmlns:u="urn:Belkin:service:deviceevent:1">
    </u:GetAttributes>
    </s:Body>
    </s:Envelope>
    """
    postRequest('/upnp/control/deviceevent1', 'urn:Belkin:service:deviceevent:1#GetAttributes', body)
}


def refresh() {
    //log.debug "Executing WeMo Switch 'subscribe', then 'timeSyncResponse', then 'poll'"
    log.debug("Refresh requested!")
    sendGetAttributesCommand()
    //poll()
}

def sendCommand(path,urn,action,body){
	log.debug "Send command called with path: ${path} , urn: ${urn}, action: ${action} , body: ${body}"
	def result = new physicalgraph.device.HubSoapAction(
        path:    path,
        urn:     urn,
        action:  action,
        body:    body,
        headers: [Host:getHostAddress(), CONNECTION: "close"]
    )
    log.debug("SCR: ${result}")
    return result
}

private subscribeAction(path, callbackPath="") {
    log.trace "subscribe($path, $callbackPath)"
    def address = getCallBackAddress()
    address = "10.13.13.5"
    def ip = getHostAddress()

    def result = new physicalgraph.device.HubAction(
        method: "SUBSCRIBE",
        path: path,
        headers: [
            HOST: ip,
            CALLBACK: "<http://${address}/>",
            NT: "upnp:event",
            TIMEOUT: "Second-28800"
        ]
    )

    log.trace "SUBSCRIBE $path"
    log.trace "RESULT: ${result}"
    result
}

def subscribe(hostAddress) {
    log.debug "Subscribing to ${hostAddress}"
    subscribeAction("/upnp/event/basicevent1")
}


def subscribe() {
    subscribe(getHostAddress())
}


def subscribe(ip, port) {
    def existingIp = getDataValue("ip")
    def existingPort = getDataValue("port")
    if (ip && ip != existingIp) {
        log.debug "Updating ip from $existingIp to $ip"
        updateDataValue("ip", ip)
    }
    if (port && port != existingPort) {
        log.debug "Updating port from $existingPort to $port"
        updateDataValue("port", port)
    }

    subscribe("${ip}:${port}")
}


def resubscribe() {
    //log.debug "Executing 'resubscribe()'"
    def sid = getDeviceDataByName("subscriptionId")

    new physicalgraph.device.HubAction("""SUBSCRIBE /upnp/event/basicevent1 HTTP/1.1
    HOST: ${getHostAddress()}
    SID: uuid:${sid}
    TIMEOUT: Second-5400


    """, physicalgraph.device.Protocol.LAN)

}


def unsubscribe() {
    def sid = getDeviceDataByName("subscriptionId")
    new physicalgraph.device.HubAction("""UNSUBSCRIBE publisher path HTTP/1.1
    HOST: ${getHostAddress()}
    SID: uuid:${sid}


    """, physicalgraph.device.Protocol.LAN)
}
