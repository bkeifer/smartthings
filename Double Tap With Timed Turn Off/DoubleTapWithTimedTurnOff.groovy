/**
 *  Double Tap
 *
 *  Author: SmartThings
 */
definition(
    name: "Double Tap With Timed Turn Off",
    namespace: "bkeifer",
    author: "SmartThings and Brian Keifer",
    description: "Turn on or off any number of switches when an existing switch is tapped twice in a row.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section("When this switch is double-tapped...") {
		input "master", "capability.switch", title: "Where?"
	}
	section("Turn on or off all of these switches as well") {
		input "switches", "capability.switch", multiple: true, required: false
	}
	section("And turn off but not on all of these switches") {
		input "offSwitches", "capability.switch", multiple: true, required: false
	}
	section("And turn on but not off all of these switches") {
		input "onSwitches", "capability.switch", multiple: true, required: false
	}
    section("Turn off delay") {
    	input "delay", "number", required: true
	}
}

def installed()
{
	log.trace "installed()"
	subscribe(master, "switch", switchHandler, [filterEvents: false])
}

def updated()
{
	log.trace "updated()"
	unsubscribe()
	subscribe(master, "switch", switchHandler, [filterEvents: false])
}

def switchHandler(evt) {
	log.info "switchHandler($evt.name: $evt.value, type: '${evt.event.type}', isPhysical: ${evt.isPhysical()} (${evt.event.isPhysical()}), isDigital: ${evt.isDigital()})"
	log.info evt.event.encodeAsJSON()

	// use Event rather than DeviceState because we may be changing DeviceState to only store changed values
	def recentStates = master.eventsSince(new Date(now() - 4000), [all:true, max: 10]).findAll{it.name == "switch"}
	log.debug "${recentStates?.size()} STATES FOUND, LAST AT ${recentStates ? recentStates[0].dateCreated : ''}"

	if (evt.isPhysical()) {
		if (evt.value == "on" && lastTwoStatesWere("on", recentStates, evt)) {
			log.debug "detected two taps, turn on other light(s)"
			onSwitches()*.on()
			log.debug "Will turn off in ${delay} minutes."
			runIn(delay * 60, "timerOff", [overwrite: false])
		} else if (evt.value == "off" && lastTwoStatesWere("off", recentStates, evt)) {
			log.debug "detected two taps, turn off other light(s)"
			offSwitches()*.off()
		}
	}
	else {
		log.trace "Skipping digital on/off event"
	}
}

private onSwitches() {
	(switches + onSwitches).findAll{it}
}

private offSwitches() {
	(switches + offSwitches).findAll{it}
}

private timerOff() {
	log.debug "Turning off after timer expired."
    offSwitches()*.off()
}

private lastTwoStatesWere(value, states, evt) {
	def result = false
	if (states) {

		log.trace "unfiltered: [${states.collect{it.dateCreated + ':' + it.value}.join(', ')}]"
		def onOff = states.findAll { it.isPhysical() || !it.type }
		log.trace "filtered:   [${onOff.collect{it.dateCreated + ':' + it.value}.join(', ')}]"

		// This test was needed before the change to use Event rather than DeviceState. It should never pass now.
		if (onOff[0].date.before(evt.date)) {
			log.warn "Last state does not reflect current event, evt.date: ${evt.dateCreated}, state.date: ${onOff[0].dateCreated}"
			result = evt.value == value && onOff[0].value == value
		}
		else {
			result = onOff.size() > 1 && onOff[0].value == value && onOff[1].value == value
		}
	}
	result
}
