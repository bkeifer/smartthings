# Smart Bathroom Fan
--------------------
Bathroom fan automation can be tricky.  If your house is anything like mine, it's not exactly air tight.  Seasonal changes in humidity can make it difficult to set specific relative humidity percentages at which to turn your bathroom exhaust fan on or off.

This SmartApp attempts to put some intelligence into the process by sampling the humidity in the room every five minutes and maintaining a rolling 24-hour average humidity.  Your thresholds are then specified based on this average.

Features
--------
---
This SmartApp does the following:

- Turns on the fan when the humidity is more than X percent greater than the 24-hour average.
- Turns off the fan when the humidity falls to within Y percent of the 24-hour average OR after an optional (but recommended) specified maximum amount of time.

GitHub Integration
------------------
----
For easier updating, add the GitHub repository in your IDE account:
 - Owner: bkeifer
 - Name: smartthings
 - Branch: master


Disclaimer
----------
---
This SmartApp is provided with no warranty, either expressed or implied and by installing it you agree that I am not accountable for its actions.  It will probably break.  It might eat your homework.  It will almost certainly pee on the living room rug when company is over.

In short, don't blame me.  I told you so.
