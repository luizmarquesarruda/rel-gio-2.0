package com.luizmarquesarruda.relogio20.domain.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SessionEndReason { COMPLETED, ORPHANED, STALLED }

data class ManagedSession(
    val id: String,
    val startedAtMs: Long,
    val lastEventMs: Long,
    val endReason: SessionEndReason? = null,
    val endedAtMs: Long? = null,
)

sealed interface SessionState {
    data object Idle : SessionState

    data class Active(
        val session: ManagedSession,
    ) : SessionState

    data class Finished(
        val session: ManagedSession,
    ) : SessionState
}

internal class SessionManager(
    private val stalledAfterMs: Long,
) {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = mutableState.asStateFlow()
    private val history = mutableListOf<ManagedSession>()

    init {
        require(stalledAfterMs > 0L)
    }

    @Synchronized
    fun start(
        id: String,
        nowMs: Long,
    ): ManagedSession {
        require(id.isNotBlank() && nowMs >= 0L)
        (mutableState.value as? SessionState.Active)?.session?.let {
            finishInternal(it, SessionEndReason.ORPHANED, nowMs)
        }
        val next = ManagedSession(id, nowMs, nowMs)
        mutableState.value = SessionState.Active(next)
        return next
    }

    @Synchronized
    fun adopt(
        id: String,
        startedAtMs: Long,
        nowMs: Long,
    ) {
        require(id.isNotBlank() && startedAtMs >= 0L && nowMs >= startedAtMs)
        val current = (mutableState.value as? SessionState.Active)?.session
        if (current?.id == id) return
        if (current != null) finishInternal(current, SessionEndReason.ORPHANED, nowMs)
        mutableState.value = SessionState.Active(ManagedSession(id, startedAtMs, nowMs))
    }

    @Synchronized
    fun recordEvent(nowMs: Long) {
        val current = (mutableState.value as? SessionState.Active)?.session ?: return
        if (nowMs >= current.startedAtMs) {
            mutableState.value = SessionState.Active(current.copy(lastEventMs = nowMs))
        }
    }

    @Synchronized
    fun watchdogTick(nowMs: Long): Boolean {
        val current = (mutableState.value as? SessionState.Active)?.session ?: return false
        if (nowMs - current.lastEventMs <= stalledAfterMs) return false
        finishInternal(current, SessionEndReason.STALLED, nowMs)
        return true
    }

    @Synchronized
    fun finalize(
        reason: SessionEndReason,
        nowMs: Long,
    ): Boolean {
        val current = (mutableState.value as? SessionState.Active)?.session ?: return false
        if (nowMs < current.startedAtMs) return false
        finishInternal(current, reason, nowMs)
        return true
    }

    @Synchronized
    fun finishedSessions(): List<ManagedSession> = history.toList()

    private fun finishInternal(
        session: ManagedSession,
        reason: SessionEndReason,
        nowMs: Long,
    ) {
        if (history.any { it.id == session.id }) return
        val finished = session.copy(endReason = reason, endedAtMs = nowMs)
        history += finished
        mutableState.value = SessionState.Finished(finished)
    }
}
