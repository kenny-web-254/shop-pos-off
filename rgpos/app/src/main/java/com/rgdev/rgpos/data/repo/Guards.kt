package com.rgdev.rgpos.data.repo

import com.rgdev.rgpos.data.AppDatabase
import com.rgdev.rgpos.data.UserEntity
import com.rgdev.rgpos.domain.Permission
import com.rgdev.rgpos.domain.PermissionDeniedException
import com.rgdev.rgpos.domain.RolePermissions

/** Role is always read from the database here, never trusted from the caller. */
internal suspend fun AppDatabase.requireUser(userId: Long, permission: Permission, deniedMessage: String): UserEntity {
    val user = userDao().getById(userId)?.takeIf { it.active }
        ?: throw PermissionDeniedException("This user account is not active.")
    if (!RolePermissions.can(user.role, permission)) throw PermissionDeniedException(deniedMessage)
    return user
}
