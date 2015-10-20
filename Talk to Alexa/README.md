# Talk to Alexa
This parent/child SmartApp creates virtual momentary button tiles which, when activated via another SmartApp or a routine, use a text-to-speech device to issue a command to your Amazon Echo.

## Files

* **AlexaTTSCommand.groovy** - Child app that creates and modifies the individual virtual buttons
* **TalkToAlexa.groovy** - Parent app that acts as a container for the individual TTS command child apps
* **MomentaryButtonTileBTK.groovy** - A copy of the SmartThings Momentary Button Tile with its namespace modified for compatibility with this SmartApp

## Installation

* Install both SmartApps as well as the customized Momentary Button Tile device type.
* Publish the device type and the parent (Talk to Alexa) SmartApp for yourself.
* The child SmartApp (Alexa TTS Command) need not be published.

## Configuration

* Within the SmartThings app on your mobile device, install the "Talk to Alexa" SmartApp from the "My Apps" category.
* TTS commands can be created from within this app and will automatically create the device needed to trigger the command from your other SmartApps and/or routines.
