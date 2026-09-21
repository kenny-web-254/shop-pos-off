package com.rgdev.rgpos.data.repo

import com.rgdev.rgpos.data.AuditDao
import com.rgdev.rgpos.data.AuditLogEntity
import java.security.MessageDigest

/**
 * Append-only audit trail. Each row hashes the previous one, so edits/deletions made outside the app
 * are detectable via [verifyChain]. (Tamper-EVIDENT against casual edits; not a defence against someone
 * who can rebuild the whole chain.)
 */
class AuditRepository(
    private val dao: AuditDao,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun log(userId: Long?, action: String, entityType: String? = null, entityId: Long? = null, details: String? = null) {
        val prev = dao.lastHash() ?: GENESIS
        val now = clock()
        dao.insert(
            AuditLogEntity(
                userId = userId, action = action, entityType = entityType, entityId = entityId, details = details,
                prevHash = prev, hash = hash(prev, userId, action, entityType, entityId, details, now), createdAt = now
            )
        )
    }

    suspend fun verifyChain(): Boolean {
        var prev = GENESIS
        for (r in dao.allOrdered()) {
            if (r.prevHash != prev) return false
            if (r.hash != hash(r.prevHash, r.userId, r.action, r.entityType, r.entityId, r.details, r.createdAt)) return false
            prev = r.hash
        }
        return true
    }

    private fun hash(prev: String, userId: Long?, action: String, type: String?, id: Long?, details: String?, ts: Long): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$prev|$userId|$action|$type|$id|$details|$ts".toByteArray())
            .joinToString("") { "%02x".format(it) }

    private companion object { const val GENESIS = "GENESIS" }
}
