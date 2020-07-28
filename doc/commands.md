# [J-RTP] Documentation ~ Commands 
###### The guide to J-RTP commands: What they are, and how to use them.

## `/rtp` - The general random teleport command.

**General**  
It teleports you to a random location.   
Usage:      `/rtp`  
Aliases:    `/wild`  
Permission: `jakesrtp.use` defaults to: `all`  

**Extended usages...**  

`/rtp [playerName]` which requires extra perm `jakesRtp.others`   
This will teleport the given user randomly, acting as if they ran `/rtp`.

`/rtp <playername> [worldName]` which requires extra perm `jakesRtp.others`   
This will teleport the given user randomly, acting as if they ran `/rtp` while 
standing in at the spawn point of the given world.

## `/rtp-admin` - The general admin command for this plugin.

**General**  
All the admin functionality (of J-RTP) in its own place  
Usage:      `/rtp-admin <reload|status>`  
Permission: `jakesrtp.admin` defaults to: `op`

# [J-RTP] Documentation ~ Commands (Format option two)
###### The guide to J-RTP commands: What they are, and how to use them.

## `/rtp` - The general random teleport command.
 
| | Base Example |
| ---- | ---- |
| Usage:      | `/rtp` &nbsp;&#124;&nbsp; `/wild` 
| Permission: | `jakesrtp.use` defaults to: `all` 
| Description:| Teleports you to a random location. 

| | ForceRtp Example 1 | 
| ---- | ---- |
| Usage       | `/rtp <playerName>` 
| Permissions | `jakesrtp.use` & `jakesRtp.others`   
| Description | This will teleport the given user randomly, acting as if they ran `/rtp`.

| | ForceRtp Example 2 |
| ---- | ---- | 
| Usage       | `/rtp <playername> <worldName>`
| Permissions | `jakesrtp.use` & `jakesRtp.others`   
| Description | This will teleport the given user randomly, acting as if they ran `/rtp` while standing at the spawn point of the given world.

## `/rtp-admin` - The general admin command for this plugin.

| | Base Example |
| ---- | ---- |
| Usage       | `/rtp-admin <reload`&#124;`status>`  
| Permission  |`jakesrtp.admin` defaults to: `op only`
| Description | All the admin functionality (of J-RTP) in its own place
