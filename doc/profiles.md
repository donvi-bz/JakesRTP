# [J-RTP] Documentation ~ RTP Profiles Overview
###### The even more in depth guide to configuring J-RTP

An RTP Profile is made of two parts: The `distribution` portion and the `rtpProfile` portion.

A `distribution` file contains all settings related to *how* the random location is selected. This is where you define
the size and shape of the region, and where you can optionally define the where players are more or less likely to spawn
in the region. Since this file is separate from the `rtpProfile` file, it can be referenced in multiple files (or none
if you'd like).

A `rtpProfile` file contains all settings related to the RTP profile* itself. These settings can be basis things like
if it is enabled or not, or if it requires a special permission, and some advanced options like where to start looking
for a location, or how many times to try before giving up.

*Note: There is no distinction between rtp settings and rtp profiles. I Prefer the term 'profile' over 'setting' for
these, but 'settings' is used internally.*
