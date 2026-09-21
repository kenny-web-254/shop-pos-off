package com.rgdev.rgpos.domain

enum class Role { OWNER, MANAGER, CASHIER, STOREKEEPER }

enum class Permission {
    RECORD_SALE, OVERRIDE_PRICE, RECEIVE_CREDIT_PAYMENT, ADD_CUSTOMER, MANAGE_CUSTOMERS,
    MANAGE_PRODUCTS, SET_BUYING_PRICE, VIEW_COST, RECEIVE_STOCK, ADJUST_STOCK,
    APPROVE_REFUND, MANAGE_EXPENSES, VIEW_REPORTS, MANAGE_CASH_SESSION,
    MANAGE_STAFF, MANAGE_SETTINGS, BACKUP_RESTORE
}

/** Roles are fixed and live in code (not the DB), so they can't be edited by tampering with data. */
object RolePermissions {
    private val map: Map<Role, Set<Permission>> = mapOf(
        Role.OWNER to Permission.values().toSet(),
        Role.MANAGER to setOf(
            Permission.RECORD_SALE, Permission.OVERRIDE_PRICE, Permission.RECEIVE_CREDIT_PAYMENT,
            Permission.ADD_CUSTOMER, Permission.MANAGE_CUSTOMERS, Permission.MANAGE_PRODUCTS,
            Permission.SET_BUYING_PRICE, Permission.VIEW_COST, Permission.RECEIVE_STOCK,
            Permission.ADJUST_STOCK, Permission.APPROVE_REFUND, Permission.MANAGE_EXPENSES,
            Permission.VIEW_REPORTS, Permission.MANAGE_CASH_SESSION
        ),
        Role.CASHIER to setOf(
            Permission.RECORD_SALE, Permission.RECEIVE_CREDIT_PAYMENT,
            Permission.ADD_CUSTOMER, Permission.MANAGE_CASH_SESSION
        ),
        Role.STOREKEEPER to setOf(Permission.RECEIVE_STOCK, Permission.ADJUST_STOCK)
    )

    fun can(role: Role, permission: Permission): Boolean = map[role]?.contains(permission) == true
}
