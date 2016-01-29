definition(
    name: "Monitoring And Notification Of Switches",
    namespace: "bkeifer/MANOS",
    author: "Brian Keifer",
    description: "MANOS... the SmartApp of Fate®",
    category: "My Apps",
    singleInstance: true,
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    // The parent app preferences are pretty simple: just use the app input for the child app.
    page(name: "mainPage", title: "Monitors and Notifications", install: true, uninstall: true,submitOnChange: true) {
        section {
            app(name: "freezeMonitor", appName: "Freeze Monitor", namespace: "bkeifer/freezeMonitor", title: "New Freeze Monitor", multiple: true)
        }
        section {
            app(name: "contactMonitor", appName: "Contact Sensor Monitor", namespace: "bkeifer/contactMonitor", title: "New Contact Sensor Monitor", multiple: true)
        }
        // section {
        //     app(name: "recurringEventMonitor", appName: "Recurring Event Monitor", namespace: "bkeifer/recurringEventMonitor", title: "New Recurring Event Monitor", multiple: true)
        // }
        // section {
        //     app(name: "manosNotification", appname: "MANOS Notifiation", namespace: "bkeifer/manosNotification", title: "New Notification", multiple: true)
        // }
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
