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

        attribute "fanMode", "string"

        command "fanMax"
        command "fanHigh"
        command "fanMed"
        command "fanLow"
        command "fanMin"

        command "hum45"
        command "hum50"
        command "hum55"
        command "hum60"
        command "humMax"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
            standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821"
            state "off", label:'${name}', action:"switch.off", icon:"st.switches.switch.off", backgroundColor:"#ffffff"
        }

        /*
        valueTile("mode", "device.mode",decoration:"flat", inactiveLabel: false) {
            state "default", label:'Mode: ${currentValue}'
        }

        valueTile("time", "device.time",decoration:"flat") {
            state "default", label:'Time Left: ${currentValue}'
        }
        */

        standardTile("min", "device.fanMode",label:"Min") {
          state "default", label: 'MIN', action: "fanMin", icon:"st.Appliances.applicances16",backgroundColor:"#ffffff"
          state "min", label: 'MIN', action: "fanMin", icon:"st.Appliances.applicances16",backgroundColor:"#66ff33"
        }

        standardTile("low", "device.fanMode",label:"Low") {
          state "default", label: 'LOW', action: "fanLow", icon:"st.Appliances.applicances16",backgroundColor:"#ffffff"
          state "low", label: 'LOW', action: "fanLow", icon:"st.Appliances.applicances16",backgroundColor:"#66ff33"
        }

        standardTile("med", "device.fanMode") {
          state "default", label: 'MED', action: "fanMed", icon:"st.Appliances.applicances16",backgroundColor:"#ffffff"
          state "med", label: 'MED', action: "fanMed", icon:"st.Appliances.applicances16",backgroundColor:"#66ff33"
        }

        standardTile("high", "device.fanMode") {
          state "default", label: 'HIGH', action: "fanHigh", icon:"st.Appliances.applicances16",backgroundColor:"#ffffff"
          state "high", label: 'HIGH', action: "fanHigh", icon:"st.Appliances.applicances16",backgroundColor:"#66ff33"
        }

        standardTile("max", "device.fanMode",label:"Max") {
          state "default", label: 'MAX', action: "fanMax", icon:"st.Appliances.applicances16",backgroundColor:"#ffffff"
          state "max", label: 'MAX', action: "fanMax", icon:"st.Appliances.applicances16",backgroundColor:"#66ff33"
        }

        valueTile("fanMode", "device.fanMode",decoration:"flat") {
            state "default", label:'Fan Speed: ${currentValue}'
        }

//        valueTile("filterLife", "device.filterLife",decoration:"flat") {
//            state "default", label:'Filter Life:${currentValue}%'
//        }

//        valueTile("humidity", "device.humidity",decoration:"flat") {
//            state "default", label:'Humidity: ${currentValue}', unit:"%"
//        }

//        valueTile("desiredHumidity", "device.desiredHumidity",decoration:"flat") {
//            state "default", label:'Desired Humidity: ${currentValue}', unit:"%"
//        }

        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }


        main "switch"
        //details (["switch","cookedTime","time","mode", "refresh"])
        details (["switch", "refresh", "min", "low", "med", "high", "max", "fanMode"])
		// TODO: define your main and details tiles here
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
    def evtMessage = parseLanMessage(description)
    def evtHeader = evtMessage.header
    def evtBody = evtMessage.body
    log.debug("Header: ${evtHeader}")
    log.debug("Body: ${evtBody}")

    if (evtHeader?.contains("SID: uuid:")) {
        log.debug("found new SID")
		def sid = (evtHeader =~ /SID: uuid:.*/) ? ( evtHeader =~ /SID: uuid:.*/)[0] : "0"
		sid -= "SID: uuid:".trim()
        log.debug "updating subscription sid: ${sid}"
    	updateDataValue("subscriptionId", sid)
    }

    if (evtBody) {
        def body = new XmlSlurper().parseText(evtBody)
        log.debug ("xmlbody: ${body}")
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
    log.debug("DNI: ${device.deviceNetworkId}")
    log.debug "RESULT: ${result}"
    return result
}

// handle commands
def poll() {
	log.debug "Executing 'poll'"
    // TODO: Get this IP/Port dynamically
    //subscribe("10.13.13.45:49155")
}


def on() {
    log.debug("inside on")
    def body = """
    <?xml version="1.0" encoding="utf-8"?>
    <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    <s:Body>
    <u:SetAttributes xmlns:u="urn:Belkin:service:deviceevent:1">
    <attributeList>&lt;attribute&gt;&lt;name&gt;FanMode&lt;/name&gt;&lt;value&gt;3&lt;/value&gt;&lt;/attribute&gt;&lt;attribute&gt;&lt;name&gt;DesiredHumidity&lt;/name&gt;&lt;value&gt;NULL&lt;/value&gt;&lt;/attribute&gt;&lt;attribute&gt;&lt;name&gt;CurrentHumidity&lt;/name&gt;&lt;value&gt;NULL&lt;/value&gt;&lt;/attribute&gt;&lt;attribute&gt;&lt;name&gt;WaterAdvise&lt;/name&gt;&lt;value&gt;NULL&lt;/value&gt;&lt;/attribute&gt;&lt;attribute&gt;&lt;name&gt;NoWater&lt;/name&gt;&lt;value&gt;NULL&lt;/value&gt;&lt;/attribute&gt;&lt;attribute&gt;&lt;name&gt;FilterLife&lt;/name&gt;&lt;value&gt;NULL&lt;/value&gt;&lt;/attribute&gt;&lt;attribute&gt;&lt;name&gt;ExpiredFilterTime&lt;/name&gt;&lt;value&gt;NULL&lt;/value&gt;&lt;/attribute&gt;</attributeList>
    </u:SetAttributes>
    </s:Body>
    </s:Envelope>
    """
    postRequest('/upnp/control/deviceevent1', 'urn:Belkin:service:deviceevent:1#SetAttributes', body)
}



// THIS WORKS.  DO NOT FUCK THIS UP.
// def fanHigh() {
//     log.debug("inside fanHigh")
//     def body = """
//     <?xml version="1.0" encoding="utf-8"?>
//     <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
//     <s:Body>
//     <u:SetAttributes xmlns:u="urn:Belkin:service:deviceevent:1">
//     <attributeList>&lt;attribute&gt;&lt;name&gt;FanMode&lt;/name&gt;&lt;value&gt;4&lt;/value&gt;&lt;/attribute&gt;</attributeList>
//     </u:SetAttributes>
//     </s:Body>
//     </s:Envelope>
//     """
//     postRequest('/upnp/control/deviceevent1', 'urn:Belkin:service:deviceevent:1#SetAttributes', body)
// }

def fanMin() {
    sendFanCommand("1")
}
def fanLow() {
    sendFanCommand("2")
}
def fanMed() {
    sendFanCommand("3")
}
def fanHigh() {
    sendFanCommand("4")
}
def fanMax() {
    sendFanCommand("5")
}


def sendFanCommand(String level) {
    def body = """
    <?xml version="1.0" encoding="utf-8"?>
    <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    <s:Body>
    <u:SetAttributes xmlns:u="urn:Belkin:service:deviceevent:1">
    <attributeList>&lt;attribute&gt;&lt;name&gt;FanMode&lt;/name&gt;&lt;value&gt;${level}&lt;/value&gt;&lt;/attribute&gt;</attributeList>
    </u:SetAttributes>
    </s:Body>
    </s:Envelope>
    """
    postRequest('/upnp/control/deviceevent1', 'urn:Belkin:service:deviceevent:1#SetAttributes', body)
}

// def fanMax() {
//     log.debug("FanMax pressed")
//     def bodyMap = [attributeList:'&lt;attribute&gt;&lt;name&gt;FanMode&lt;/name&gt;&lt;value&gt;5&lt;/value&gt;&lt;/attribute&gt;']
//     sendCommand('/upnp/control/deviceevent1', 'urn:Belkin:service:deviceevent:1', 'SetAttributes', bodyMap)
// }

def getAttributes() {

}

def off() {
    log.debug("inside off")
    def body = """
    <?xml version="1.0" encoding="utf-8"?>
    <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    <s:Body>
    <u:SetAttributes xmlns:u="urn:Belkin:service:deviceevent:1">
    <attributeList>&lt;attribute&gt;&lt;name&gt;FanMode&lt;/name&gt;&lt;value&gt;0&lt;/value&gt;&lt;/attribute&gt;&lt;attribute&gt;&lt;name&gt;DesiredHumidity&lt;/name&gt;&lt;value&gt;NULL&lt;/value&gt;&lt;/attribute&gt;&lt;attribute&gt;&lt;name&gt;CurrentHumidity&lt;/name&gt;&lt;value&gt;NULL&lt;/value&gt;&lt;/attribute&gt;&lt;attribute&gt;&lt;name&gt;WaterAdvise&lt;/name&gt;&lt;value&gt;NULL&lt;/value&gt;&lt;/attribute&gt;&lt;attribute&gt;&lt;name&gt;NoWater&lt;/name&gt;&lt;value&gt;NULL&lt;/value&gt;&lt;/attribute&gt;&lt;attribute&gt;&lt;name&gt;FilterLife&lt;/name&gt;&lt;value&gt;NULL&lt;/value&gt;&lt;/attribute&gt;&lt;attribute&gt;&lt;name&gt;ExpiredFilterTime&lt;/name&gt;&lt;value&gt;NULL&lt;/value&gt;&lt;/attribute&gt;</attributeList>
    </u:SetAttributes>
    </s:Body>
    </s:Envelope>
    """
    postRequest('/upnp/control/deviceevent1', 'urn:Belkin:service:deviceevent:1#SetAttributes', body)
}

def doAction(action, service, path, Map body = [InstanceID:0, BinaryState:0]) {
    def result = new physicalgraph.device.HubSoapAction(
        path:    path,
        urn:     "urn:Belkin:service:$service:1",
        action:  action,
        body:    body,
        headers: [Host:getHostAddress()]
    )
    return result

}

def refresh() {
    //log.debug "Executing WeMo Switch 'subscribe', then 'timeSyncResponse', then 'poll'"
    log.debug("Refresh requested!")
    poll()
    //subscribeAction("/upnp/event/basicevent1")
    //off()
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

def sendCommand(path,urn,action,body){
	log.debug "Send command called with path: ${path} , urn: ${urn}, action: ${action} , body: ${body}"
    def address = getCallBackAddress()
    def ip = getHostAddress()
    log.debug("cBA: ${address}")
    log.debug("gHA: ${ip}")
	def result = new physicalgraph.device.HubSoapAction(
		path:    path,
		urn:     urn,
		action:  action,
		body:    body,
        headers: [
            HOST: "10.13.13.45:49155",
            CALLBACK: "<http://${address}/>",
            NT: "upnp:event",
            TIMEOUT: "Second-28800"
        ]
//		headers: [Host:getHostAddress(), CONNECTION: "close"]
    )
    log.debug(result)
	return result
}
