definition(
    name: "Virtual Device Manager",
    namespace: "bkeifer/VDM",
    author: "Brian Keifer",
    description: "Create virtual devices for use in routines and other SmartApps",
    category: "My Apps",
    singleInstance: true,
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    // The parent app preferences are pretty simple: just use the app input for the child app.
    page(name: "mainPage", title: "Installed Switches", install: true, uninstall: true,submitOnChange: true) {
        section {
            app(name: "virtualSwitch", appName: "VDM Child - Switch", namespace: "bkeifer/VDMChildSwitch", title: "New Virtual Switch", multiple: true)
        }
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
    // nothing needed here, since the child apps will handle preferences/subscriptions
    // this just logs some messages for demo/information purposes
    log.debug "there are ${childApps.size()} child smartapps"
    childApps.each {child ->
            log.debug "child app: ${child.label}"
    }
}
