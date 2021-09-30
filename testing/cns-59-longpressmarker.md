# Tests for acceptance of branch 'cns-59-longpressmarker'

## Objective:

Fix long-press-to-delete functionality so that it captures nearby
Features and Markers (geolocated pins) added by mobile user, and ONLY
nearby ones. Also, if multiple features/markers are captured by the
long press, the app now presents a scrollable list of things you might
be trying to delete, instead of iterating blind.

## Prerequisites:

1. Find or add some features and geocoded markers to the map. At least
one should be really far away from the others (e.g., Boston to DC far
away, or more).

2. Using web interface, make sure your map includes at least one of
every feature type (symbol, text, triangle, polygon, circle, etc.)


## Tests:

1. Zoom out so that your features/markers are on top of each
other. Long press on the pile of features/markers.
    * Confirm that they are all included in the dialog's list.
    * Confirm that the faraway feature is not included.
    * Confirm that selecting one of the listed features prompts for confirmation of the co*rrect feature.
    * Confirm that affirming deletion actually deletes the feature.
    * Confirm that the 'undo' function works (if it was a feature).
    * Repeat 1-4 with a Marker instead; there should be no 'undo' function offered.

2. Navigate to your faraway feature/marker. Long press on it.
    * Confirm that only a confirmation prompt appears, not a list with other features/markers.

3. Mess around with your zoom and try long pressing at the edges of
features/markers. Are you happy with the sensitivity?

4. Confirm that everything you deleted on mobile is gone on web.

5. Log back in as a read-only user.
    * Confirm that deletion is NOT offered if you long press on features.
    * Confirm that deletion IS offered if you long press on geocoded markers.

6. Be evil and send Curran feedback.

7. Not really related, but I am dying to know whether your device
draws a geocoded marker entered as (-90, 0) on the Equator.