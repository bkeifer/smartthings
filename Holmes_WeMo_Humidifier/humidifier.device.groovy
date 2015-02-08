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
        //attribute "cookedTime", string
    }

    // simulator metadata
    simulator {}

        // UI tile definitions
        tiles {
            standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
                state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821"
                state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff"

            }

            /*
            valueTile("mode", "device.mode",decoration:"flat", inactiveLabel: false) {
            state "default", label:'Mode: ${currentValue}'
        }

        valueTile("time", "device.time",decoration:"flat") {
        state "default", label:'Time Left: ${currentValue}'
    }

    valueTile("cookedTime", "device.cookedTime",decoration:"flat") {
    state "default", label:'Cooked Time: ${currentValue}'
}
*/

standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
    state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
}


main "switch"
//details (["switch","cookedTime","time","mode", "refresh"])
details (["switch", "refresh"])
}
}





// parse events into attributes
def parse(String description){
    def map = stringToMap(description)
    def headerString = new String(map.headers.decodeBase64())
    //log.debug("HeaderString: ${headerString}")
    def result = []


    // update subscriptionId
    if (headerString.contains("SID: uuid:")) {
        def sid = (headerString =~ /SID: uuid:.*/) ? ( headerString =~ /SID: uuid:.*/)[0] : "0"
        sid -= "SID: uuid:".trim()
        //log.debug('Update subscriptionID: '+ sid)
        updateDataValue("subscriptionId", sid)
    }
    //log.debug("body: ${new String(map.body.decodeBase64())}")
    // parse the rest of the message
    if (map.body) {
        def bodyString = new String(map.body.decodeBase64())
        def body = new XmlSlurper().parseText(bodyString)
        log.debug("bodyString: ${bodyString}")
        log.debug("body: ${body}")
        log.debug("body.text(): ${body.text()}")

        if(body.attribute.value) {
            def filterLife = body.attribute.value.text()
            log.trace "FILTER LIFE: ${filterLife}"
        }
        //Set Crockpot State Response
        if(body?.Body?.SetCrockpotStateResponse?.time?.text()){
            def crockpotTime = body?.Body?.SetCrockpotStateResponse?.time?.text()
            log.trace "Got SetCrockpotStateResponse = $crockpotTime"
            result << createEvent(name: "time", value: crockpotTime)
        }

        else if (body?.Body?.SetCrockpotStateResponse?.text()){
            def response = body?.Body?.SetCrockpotStateResponse?.text()
            log.trace "Set Response - $response"
        }

        //TimeSync Response
        else if (body?.property?.TimeSyncRequest?.text()) {

            log.trace "Got TimeSyncRequest - ignoring"
            //result << timeSyncResponse()
        }

        //Get Crockpot State Response
        else if (body?.Body?.GetCrockpotStateResponse?.text)	{

            if(body?.Body?.GetCrockpotStateResponse?.time?.text()){
                def crockpotTime = body?.Body?.GetCrockpotStateResponse?.time?.text()
                log.trace "Got GetCrockpotStateResponse Time = $crockpotTime"
                result << createEvent(name: "time", value: crockpotTime)
            }


            if(body?.Body?.GetCrockpotStateResponse?.mode?.text()){
                def modeNum = body?.Body?.GetCrockpotStateResponse?.mode?.text()
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

            log.trace "Got GetCrockpotStateResponse Mode = ${crockpotMode}"
            result << createEvent(name: "mode", value: crockpotMode)

        }

        if(body?.Body?.GetCrockpotStateResponse?.cookedTime?.text()){
            def crockpotCookedTime = body?.Body?.GetCrockpotStateResponse?.cookedTime?.text()
            log.trace "Got GetCrockpotStateResponse cookedTime = ${crockpotCookedTime}"
            result << createEvent(name: "cookedTime", value: crockpotCookedTime)

        }

        if (body?.property?.GetCrockpotStateResponse?.text())
        {
            def value = body?.property?.GetCrockpotStateResponse?.text()
            log.trace "Notify: crockpotState = ${value}"

        }
        } //end getCrockpotState

        //Other Responses
        else {
            log.trace "Unknown Response - ${body?.text()}"
        }

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
                new physicalgraph.device.HubAction([
                    'method': 'POST',
                    'path': path,
                    'body': body,
                    'headers': [
                    'HOST': getHostAddress(),
                    'Content-type': 'text/xml; charset=utf-8',
                    'SOAPAction': "\"${SOAPaction}\""
                    ]
                    ], device.deviceNetworkId)
                }

                def poll() {
                    def body = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                    <s:Body>
                    <u:GetCrockpotState xmlns:u="urn:Belkin:service:basicevent:1"></u:GetCrockpotState>
                    </s:Body>
                    </s:Envelope>
                    """
                    postRequest('/upnp/control/basicevent1', 'urn:Belkin:service:basicevent:1#GetCrockpotState', body)

                }

                def on() {
                    def body = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" SOAP-ENV:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                    <SOAP-ENV:Body>
                    <m:SetCrockpotState xmlns:m="urn:Belkin:service:basicevent:1">
                    <mode>${cookingMode}</mode><time>${cookingTime}</time>
                    </m:SetCrockpotState>
                    </SOAP-ENV:Body>
                    </SOAP-ENV:Envelope>
                    """
                    postRequest('/upnp/control/basicevent1', 'urn:Belkin:service:basicevent:1#SetCrockpotState', body)

                }

                def off() {
                    def body = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" SOAP-ENV:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                    <SOAP-ENV:Body>
                    <m:SetCrockpotState xmlns:m="urn:Belkin:service:basicevent:1">
                    <mode>0</mode><time>0</time>
                    </m:SetCrockpotState>
                    </SOAP-ENV:Body>
                    </SOAP-ENV:Envelope>
                    """
                    postRequest('/upnp/control/basicevent1', 'urn:Belkin:service:basicevent:1#SetCrockpotState', body)
                }

                def refresh() {
                    //log.debug "Executing WeMo Switch 'subscribe', then 'timeSyncResponse', then 'poll'"
                    poll()
                }

                def subscribe(hostAddress) {
                    //log.debug "Executing 'subscribe()'"
                    def address = getCallBackAddress()
                    new physicalgraph.device.HubAction("""SUBSCRIBE /upnp/event/basicevent1 HTTP/1.1
                    HOST: ${hostAddress}
                    CALLBACK: <http://${address}/>
                    NT: upnp:event
                    TIMEOUT: Second-5400
                    User-Agent: CyberGarage-HTTP/1.0


                    """, physicalgraph.device.Protocol.LAN)
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
