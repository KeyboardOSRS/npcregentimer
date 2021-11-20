# NPC Regen Timer RuneLite plugin

## Install
Add to `runelite\runelite-client\src\main\java\net\runelite\client\plugins`

You must manually compile the RuneLite client

## Usage
Hold Shift + Right Click the NPC you wish to track

You must manually figure out the npcs regen rate / death animation time, and sync the timer manually by incrementing / decrementing the timer

You should set "Ticks per death animation" to 3 in the following example:
* tick 0: damage hitsplat
* tick 1: npc death anim
* tick 2: npc death anim
* tick 3: npc is despawned

## Known bugs
The timer will pause for 1 tick and go out of sync if the npc goes out of view (such as you teleporting away)

This plugin doesn't take into account death speed-up or slow-down, for example the npc being d-clawed/d-bowed or moving on the tick it dies. The timer will go out of sync if this happens
