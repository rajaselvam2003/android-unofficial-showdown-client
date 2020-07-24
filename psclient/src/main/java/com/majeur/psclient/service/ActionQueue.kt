package com.majeur.psclient.service

import android.os.Handler
import android.os.Looper
import com.majeur.psclient.service.observer.BattleRoomMessageObserver

class ActionQueue(looper: Looper) {

    private data class Entry (val action: ()->Unit, val delay: Long)

    private val handler = Handler(looper)
    private val actions = mutableListOf<Entry>()

    private var lastAction: (()->Unit)? = null
    private var isLooping = false
    private var turnActionInQueue = false

    private var battleType = BattleRoomMessageObserver.BattleType.LIVE

    fun setBattleType(battleType: BattleRoomMessageObserver.BattleType) {
        this.battleType = battleType
    }

    fun clear() {
        stopLoop()
        turnActionInQueue = false
        lastAction = null
        actions.clear()
    }

    fun setLastAction(action: (()->Unit)?) {
        lastAction = action
    }

    fun enqueueTurnAction(action: ()->Unit) {
        enqueueAction(action)

        // Only skip to the latest turn if we are watching a live battle.
        // Otherwise, do each turn in the queue one at a time
        if (turnActionInQueue && battleType == BattleRoomMessageObserver.BattleType.LIVE) {
            loopTo(action)
            turnActionInQueue = true
        }
    }

    fun enqueueAction(action: ()->Unit) {
        enqueue(action, 0)
    }

    fun enqueueMajorAction(action: ()->Unit) {
        enqueue(action, 1500)
    }

    fun enqueueMinorAction(action: ()->Unit) {
        enqueue(action, 750)
    }

    private fun enqueue(action: ()->Unit, delay: Long) {
        actions.add(Entry(action, delay))
        if (!isLooping) startLoop()
    }

    fun startLoop() {
        isLooping = true
        handler.post(loopRunnable)
    }

    fun stopLoop() {
        isLooping = false
        handler.removeCallbacks(loopRunnable)
    }

    private fun loopTo(action: ()->Unit) {
        stopLoop()
        while (actions.first().action !== action) actions.removeAt(0).action.invoke()
        startLoop()
    }

    private val loopRunnable: Runnable = object : Runnable {

        override fun run() {
            val entry = actions.removeAt(0)
            entry.action.invoke()
            if (actions.any()) {
                handler.postDelayed(this, entry.delay)
            } else {
                stopLoop()
                turnActionInQueue = false
                lastAction?.invoke()
                lastAction = null
            }
        }
    }
}