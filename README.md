# Jake's Random Teleporter `[J-RTP]`
What does it do? • Teleports a player to a random location by when they type `/rtp` or `/wild`, or when they join the server for the first time • Tons of configuration from basic shape and size to multiple configs and distribution patterns

## A brief list of what you can configure.
* Which worlds the config is enabled in
* What shape that the points will appear in...
    * Square
    * Circle
* The maximum distance to take a player from the center
* The minimum distance to take a player from the center
* Where the random teleport is centered...
    * At the world's spawn
    * At the players current location
    * At a specific x and z
* Will the locations be chosen such that they are evenly distributed or with a Gaussian distribution...
* If Gaussian distribution is enabled, these settings can be used:
    * Shrink - Inversly related to the standard deviation of the distribution (larger values make it denser)
    * Center - A number from 0 to 1 where 0 centers the points at the min distance, 0.5 is between min and max, 
    and 1 is at max
* The number of seconds a player must wait before using the /rtp command again
* Do new players get teleported randomly when they join for the first time? *
* The lowest y-value that a player can get teleported to
* Some stuff about how many spots to check for safety, though these shouldn't need to be modified for a normal world.
###### _* <sub>Denotes static settings. (Settings that are not per config, and generally are only applicable to one config at a time)</sub>_
###### _All of this is per individual config. Multiple configs can exist at the same time_

Oh, and if you want more than one config, you just copy and paste the config section, change the name, and suddenly you have 2! An example of this is sitting at the bottom of the default generated config.

## The config (and all its documentation) ~ _Config v1.7_
```yaml
# - - - - - - - - - - - - - - - - - - - - - -
# → JakesRTP Config!  Have fun configuring! ←
# - - - - - - - - - - - - - - - - - - - - - -
# Old configs may not run correctly.
# Please don't edit config-version.
config-version: 1.7
# Should the plugin try and load an old config? Whenever a change is made to the structure of the config, the
#   version is increased. On startup, the plugin compares the version of the existing config to the version
#   that it is expecting, and will either (a) attempt to load the older version of the config regardless of
#   if the versions match, or (b) back up the existing config, and load the new one in its place.
# If this is set to true, there is a large chance that the plugin will crash on load, BUT that may actually be
#   the preferred course of action as it makes it very apparent that the config needs to be updated. Then it
#   can be temporarily set to false so that the plugin can load in the new config, and all the old values that
#   still apply can be copied over.
run-old-configs: false
# ↑ IMPORTANT SETTING ↑
rtp-on-first-join:    # Should new players get randomly teleported when they first join?
  enabled: false      # If this is false, they will not, and the values of 'world' and 'settings' will not be read.
  settings: default   # The rtp settings by name to teleport the player with.
  world: world        # The world the player will land in. This world MUST be included in the settings enabled worlds.
# End of static settings. All settings below are modular
# To use the modular rtp settings, simply copy and paste the 'random-teleport-settings default' section (including all
#   child elements), change the name of the settings (ie, change 'default' to 'potato' or something), and your done!
random-teleport-settings default: # The name of this config section is 'default', each section must have a unique name
  enabled: true   # Should we load this config?
  enabled-worlds: # Nether and End worlds are not currently supported, so please don't list them here
    - world
    - yer  # You can remove this one, it's here as an example
  shape:
    allowed-values:
      a: "Square (radius & center)"
      b: "Circle (radius & center)"
    value: a
  defining-points:
    radius-center:
      radius:
        max: 2000
        min: 1000
      center:
        allowed-values:
          a: "World spawn"
          b: "Player's current location"
          c: "The x and z as set in config"
        value: a
        x: 0
        z: 0
      gaussian-distribution:  # Enabling this will make the points no longer evenly distributed, but instead follow
        enabled: false        # a gaussian centered between the min and max radius (represented by setting center to
        shrink: 4             # 0 for min, 1 for max, or anything in between). Shrink makes the distribution denser,
        center: 0.25          # my preference is to keep it near 3 for a center of 0 or 1, and 6 for a center of 0.5
  cooldown:      # Cooldown between uses. Can be bypassed with permission node jakesrtp.nocooldown
    seconds: 30  # Default: 30
  ############## # Everything past this point in the config probably does not need to get changed
  low-bound:     # The lowest point a player can be RTP'd to.
    value: 48    # Default: 48
  check-radius:  # How many blocks away from the initial spot to check.
    x-z: 2       # Default: 2    | Max spots to check per attempt is equal to:
    vert: 2      # Default: 2    | (2*vert+1)*(2*(x-z)+1)^2
  max-attempts:  # If the random location, and all (by default 125) close spots are found to be unsafe,
    value: 5     # how many attempts can we make? Minimum: 1; Default: 5

```

## Random points examples
The random coordinate generator I made for this plugin does a real good job at evenly distributing points over an area, assuming you _want_ them to be distributed evenly.
You can choose from having the points evenly distributed throughout the given area, or have a Gaussian distribution where the points are more densely distributed around a specific radius.
Here are some examples of possible distributions, though it is worth noting that every value can be changed, and these are far from the only options. 
The examples displayed are just to give an idea of what the settings can do.
![Image](pics/distributionExamples.png "icon")
And here are some settings that I like.
![Image](pics/x%20Circle%20250%20to%201000%20-%20Normal%20distribution%20(4-0.25).png "icon")
![Image](pics/x%20Square%20250%20to%201000%20-%20Normal%20distribution%20(4-0.25).png "icon")
###### All images are made with points generated by the plugin, but plotted with gnuPlot.

## Permissions
* jakesrtp.use
    * description: Allows the use of the base "/rtp" command
    * default: true
*jakesrtp.noCooldown
    * description: Allows the user to ignore the cool-down timer
    * default: op
* jakesrtp.others
    * description: Allows the use of "/rtp" on other players
    * default: op
* jakesrtp.admin
    * description: Allows the usage of the "/rtp-admin" command.
    * default: op

## Commands
`/rtp` and `/wild` both make the user randomly teleport  
`/rtp [playerName]` will randomly teleport the given player (assuming you have permission)
`/rtp-admin` reload will reload the config from the file(assuming you have permission)