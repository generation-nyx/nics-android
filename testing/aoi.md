# Tests for acceptance of branch 'aoi'

## Objective:

Adding Areas of Interest (AOI) hazard detection to NICS mobile.
Users can mark wfs layers and markup features as hazards. The mobile app
will alert the users when they enter into a hazardous area defined by the hazard 
geometry and they will be constantly updated of where any nearby hazards currently
are relative to their location.

## Prerequisites:

1. Using web interface, make sure your map includes at least one of
every feature type (symbol, text, triangle, polygon, circle, etc.). 

2. Mark each one of them as hazards.

3. Add a few more features randomly, but don't mark them as hazards.

4. Place at least 2 features in the same location as the mobile device's location 
and mark them both as hazards from the web UI.

5. When you first login to the mobile app, go to the settings and change "Hazard Detection Frequency"
to 5 seconds. This will make testing it easier and also good to test that the settings work.
    * Also, set wfs frequency to 30 seconds.
    * Set map/chat frequency to 15 seconds.

## Tests:

1. Join the incident/collabroom that you added the markup features to. 
    * There should be three notifications that come when you join the room. There should be
    the mobile tracking, nearby hazards, and a separate notification alerting you on the two 
    hazards that were defined on the web UI.
    * Verify that sound only occurs for the hazard alert notification. The mobile tracking and nearby
    hazards should just appear with vibration.
    * The nearby hazards notification should be showing red text saying "you are currently inside of 
    2 hazards".

2. Click the hazard alert notification. It should bring you to the map view and zoom into the 
user's location. The camera will be set at the total LatLngBounds of all of the intersecting hazards. 
The yellow hazard geometries should also be showing on the map.

3. Change your location and try to move out of one of the hazards, but stay inside of one of them.
    * On the next update, the nearby hazard notification should change to only say "you are currently
    inside of 1 hazard".
    * If you now leave both hazards, the nearby hazard notification should update to only have the nearby hazards
    listed and shouldn't say that you are inside any of them.

4. Edit a feature from mobile and submit it. Verify that the hazard geometry is updated to reflect the new
markup geometry. Do the same with editing a feature from the web side.

5. From the web UI, edit the hazard radius. Verify that the hazard geometry reflects the change on the next markup
update on mobile.

6. From the web UI, completely remove a hazard from a feature. Verify on mobile that the hazard is removed.

7. Add one of each feature type from mobile and verify that the hazards appear alongside them when submitting.

8. Try turning off the geofencing service from the settings page. It should remove the nearby hazards
 notification completely. It should also remove all of the yellow hazards on the map.
    * Close the app completely and reopen it. The geofencing notifications should not appear and when opening the map, 
    the hazard button should not appear either. 
    * Turn the service back on and the notification should come back and you should start getting hazard
    updates again.
    * Also, try to disable the geofencing service from the notification's "Disable" button. Verify that
    it turns off completely and turns back on.
    
9. From the web UI, mark a room layer as a hazard. 
    * On mobile make sure to join the room with that room layer. The layer features and hazards should download in the background.
    * Open up the map view. If you turn on the hazards view, the layer feature hazards will show up in yellow without the actual layer feature. 
    * If you then toggle on the layer using the menu, the layer features should appear over the hazard representations.
    * These hazards should act exactly the same as the markup feature hazards (entering them will trigger a notification, etc.)
    
10. Try removing the hazard from the web UI for the layer. Wait for the next refresh and verify it is removed.