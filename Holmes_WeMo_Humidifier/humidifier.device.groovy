/**
*  Wemo Crockpot Switch (Connect)
*
*  Copyright 2014 Nicolas Cerveaux
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

preferences {

    //input "setpoint", "number", title: "Desired Humidity", description: "(percent)", required: false, displayDuringSetup: true
    //input "cookingTime", "number", title: "Cooking Cycle Time (min)", description: "Default 0", defaultValue: '0', required: false, displayDuringSetup: true
    //input "cookingMode", "number", title: "Cooking Mode - 50=Warm,51=Low,52=High", description: "Default 50", defaultValue: '50', required: false, displayDuringSetup: true

}


metadata {
    // Automatically generated. Make future change here.
    definition (name: "Holmes WeMo Humidifier", namespace: "bkeifer", author: "Brian Keifer") {
        //capability "Energy Meter"
        capability "Actuator"
        capability "Switch"
        capability "Polling"
        capability "Refresh"

        command "subscribe"
        command "resubscribe"
        command "unsubscribe"
        //command "levelUp"
        //command "levelDown"

        //attribute "time" ,string
        //attribute "mode", string
        attribute "filterLife", integer
        attribute "desiredHumidity", string
        attribute "humidity", string
        attribute "fanMode", string
    }

    // simulator metadata
    simulator {}

    // UI tile definitions
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

        valueTile("fanMode", "device.fanMode",decoration:"flat") {
            state "default", label:'Fan Speed: ${currentValue}'
        }

        valueTile("filterLife", "device.filterLife",decoration:"flat") {
            state "default", label:'Filter Life:${currentValue}%'
        }

        valueTile("humidity", "device.humidity",decoration:"flat") {
            state "default", label:'Humidity: ${currentValue}', unit:"%"
        }

        valueTile("desiredHumidity", "device.desiredHumidity",decoration:"flat") {
            state "default", label:'Desired Humidity: ${currentValue}', unit:"%"
        }

        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }


        main "switch"
        //details (["switch","cookedTime","time","mode", "refresh"])
        details (["switch", "refresh", "fanMode", "humidity", "desiredHumidity", "filterLife"])
    }
}



// parse events into attributes
def parse(String description){

    def msg = parseLanMessage(description)
    def headersAsString = msg.header
    log.debug("HAS: ${headersAsString}")
    //log.debug("desc: ${description}")

    def map = stringToMap(description)
    def decodedHeaders = map.headers.decodeBase64()
    def headerString = decodedHeaders.toString()

    def result = []

    // update subscriptionId
    if (headersAsString.contains("SID: uuid:")) {
        def sid = (headersAsString =~ /SID: uuid:.*/) ? ( headersAsString =~ /SID: uuid:.*/)[0] : "0"
        sid -= "SID: uuid:".trim()
        log.debug('Update subscriptionID: '+ sid)
        updateDataValue("subscriptionId", sid)
    }
    //log.debug("body: ${new String(map.body.decodeBase64())}")
    // parse the rest of the message
    if (map.body) {
        def bodyString = new String(map.body.decodeBase64())
        def body = new XmlSlurper().parseText(bodyString)

        if (body.text() =~ /UPnPError/) {
            log.trace("Error recieved!")
        } else {
            log.debug("bodyString: ${bodyString}")
            log.debug("body.text(): ${body.text()}")
            log.debug("body.size(): ${body.size()}")

            def attrName = ( body.text() =~ /<attribute><name>(\w+)<\/name><value>([\w\.]+)<\/value>/ )
            log.debug("attrName[0]: ${attrName[0]}")
            log.debug("attrName[0][2]: ${attrName[0][2]}")

            if ( attrName[0][1] == "FilterLife" ) {
                int maxFilterLife = 60480
                def percent = (attrName[0][2].toInteger() / maxFilterLife) * 100
                log.trace("Filter Life: ${percent}")
                result << createEvent(name: "filterLife", value: percent.toInteger())
            }

            if ( attrName[0][1] == "CurrentHumidity" ) {
                log.trace("Humidity: ${attrName[0][2]}")
                result << createEvent(name: "humidity", value: attrName[0][2])
            }

            if ( attrName[0][1] == "FanMode" ) {
                def fanModes = ["Off", "Min", "Low", "Med", "High", "Max"]
                log.trace("Fan Mode: ${fanModes[attrName[0][2].toInteger()]}")
                result << createEvent(name: "fanMode", value: fanModes[attrName[0][2].toInteger()] )
            }

            if ( attrName[0][1] == "DesiredHumidity" ) {
                def setPoints = ["45%", "50%", "55%", "60%", "MAX"]
                log.trace("Desired Humidity: ${setPoints[attrName[0][2].toInteger()]}")
                result << createEvent(name: "desiredHumidity", value: setPoints[attrName[0][2].toInteger()] )
            }

        }
        //if(body?.Body?.GetJardenStatus?.filterLife?.text()) {
        //    def filterLife = body.attribute.value.text()
        //    log.trace "FILTER LIFE: ${filterLife}"
        //}
        /*
        //Set Crockpot State Response
        if(body?.Body?.SetHumidifierStateResponse?.time?.text()){
            def crockpotTime = body?.Body?.SetHumidifierStateResponse?.time?.text()
            log.trace "Got SetHumidifierStateResponse = $crockpotTime"
            result << createEvent(name: "time", value: crockpotTime)
        }

        else if (body?.Body?.SetHumidifierStateResponse?.text()){
            def response = body?.Body?.SetHumidifierStateResponse?.text()
            log.trace "Set Response - $response"
        }

        //TimeSync Response
        else if (body?.property?.TimeSyncRequest?.text()) {

            log.trace "Got TimeSyncRequest - ignoring"
            //result << timeSyncResponse()
        }

        //Get Crockpot State Response
        else if (body?.Body?.GetHumidifierStateResponse?.text)	{

            if(body?.Body?.GetHumidifierStateResponse?.time?.text()){
                def crockpotTime = body?.Body?.GetHumidifierStateResponse?.time?.text()
                log.trace "Got GetHumidifierStateResponse Time = $crockpotTime"
                result << createEvent(name: "time", value: crockpotTime)
            }


            if(body?.Body?.GetHumidifierStateResponse?.mode?.text()){
                def modeNum = body?.Body?.GetHumidifierStateResponse?.mode?.text()
                def crockpotMode =''

                if (modeNum == '0') {
                    crockpotMode = 'Off'
                    sendEvent(name:"switch",value:"off")
                }
                else if (modeNum == '50') {
                    crockpotMode = 'Warm'
                    sendEvent(name:"switch",value:"on")
                }
                else if (modeNum == '51') {
                    crockpotMode = 'Low'
                    //sendEvent(name:"switch",value:"low")
                }
                else if (modeNum == '52') {
                    crockpotMode = 'High'
                    //sendEvent(name:"switch",value:"high")
                }
                else { crockpotMode = 'Unk - ${modeNum}'

            }

            log.trace "Got GetHumidifierStateResponse Mode = ${crockpotMode}"
            result << createEvent(name: "mode", value: crockpotMode)

        }

        if(body?.Body?.GetHumidifierStateResponse?.cookedTime?.text()){
            def crockpotCookedTime = body?.Body?.GetHumidifierStateResponse?.cookedTime?.text()
            log.trace "Got GetHumidifierStateResponse cookedTime = ${crockpotCookedTime}"
            result << createEvent(name: "cookedTime", value: crockpotCookedTime)

        }

        if (body?.property?.GetHumidifierStateResponse?.text())
        {
            def value = body?.property?.GetHumidifierStateResponse?.text()
            log.trace "Notify: crockpotState = ${value}"

        }

        } //end getCrockpotState

        //Other Responses
        else {
            log.trace "Unknown Response - ${body?.text()}"
        }
        */
    } // end if map.body

    result
} // end parse


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

    return result
    log.debug "RESULT: ${result}"
}


def poll() {
    // TODO: Get this ip/port dynamically
    subscribe("10.13.13.45:49153")
}


def on() {
    // def body = """
    // <?xml version="1.0" encoding="utf-8"?>
    // <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" SOAP-ENV:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    // <SOAP-ENV:Body>
    // <m:SetBinaryState xmlns:m="urn:Belkin:service:basicevent:1">
    // <BinaryState>1</BinaryState>
    // </m:SetBinaryState>
    // </SOAP-ENV:Body>
    // </SOAP-ENV:Envelope>
    // """
    // postRequest('/upnp/control/basicevent1', 'urn:Belkin:service:basicevent:1#SetBinaryState', body)
    off()
}


// THIS SECTION SENDS NETWORK TRAFFIC
    // def off() {
    //     log.debug("inside off")
    //     def body = """
    //     <?xml version="1.0" encoding="utf-8"?>
    //     <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" SOAP-ENV:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    //     <SOAP-ENV:Body>
    //     <m:SetBinaryState xmlns:m="urn:Belkin:service:basicevent:1">
    //     <BinaryState>0</BinaryState>
    //     </m:SetBinaryState>
    //     </SOAP-ENV:Body>
    //     </SOAP-ENV:Envelope>
    //     """
    //     log.debug("doaction")
    //     doAction("SetBinaryState", "basicevent", "/upnp/control/basicevent1", [BinaryState:0])
    // }


def off() {
    log.debug("inside off")
    def body = """
    <?xml version="1.0" encoding="utf-8"?>
    <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    <s:Body>
    <u:GetCrockpotState xmlns:u="urn:Belkin:service:crockpotevent:1">
    </u:GetCrockpotState>
    </s:Body>
    </s:Envelope>
    """
    postRequest('/upnp/control/crockpot1', 'urn:Belkin:service:crockpotevent:1#GetCrockpotState', body)
}

// def off() {
//     log.debug("inside off")
//     doAction("GetBinaryState", "basicevent", "/upnp/control/basicevent", [:])
// }

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
    /*
    String x
    def hubAddress = getCallBackAddress()

    def notifyURL = "&lt;" + "http://" + hubAddress + "/notify&gt;"

    x = """SUBSCRIBE /upnp/event/basicevent1 HTTP/1.1
    HOST: ${hostAddress}
    CALLBACK: &lt;http://${hubAddress}/&gt;
    NT: upnp:event
    TIMEOUT: Second-5400
    User-Agent: CyberGarage-HTTP/1.0


    """

    log.debug("x: ${x}")

    def result = new physicalgraph.device.HubAction(
        method: "SUBSCRIBE",
        path: "/upnp/event/basicevent1",
        headers: [
            HOST: getHostAddress(),
            CALLBACK: "<http://${getCallBackAddress()}/notify>",
            NT: "upnp:event",
            TIMEOUT: "Second-3600"
        ]
    )
    log.debug(result)
*/
    /////////////////////////////////////////////////////
    /*
    def action = """SUBSCRIBE /upnp/event/basicevent1 HTTP/1.1
    HOST: ${hostAddress}
    CALLBACK: &lt;http://${hubAddress}/&gt;
    NT: upnp:event
    TIMEOUT: Second-5400
    User-Agent: CyberGarage-HTTP/1.0


    """
    log.debug("ACTION: ${action}")
    new physicalgraph.device.HubAction(action, physicalgraph.device.Protocol.LAN)
    */

    /*
    new physicalgraph.device.HubAction("""SUBSCRIBE /upnp/event/basicevent1 HTTP/1.1
    HOST: ${hostAddress}
    CALLBACK: <http://${address}/>
    NT: upnp:event
    TIMEOUT: Second-5400
    User-Agent: CyberGarage-HTTP/1.0


    """, physicalgraph.device.Protocol.LAN)
    */
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
