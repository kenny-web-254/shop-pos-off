package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Launch : Screen("launch")
    object Setup : Screen("setup")
    object Login : Screen("login")
    object Main : Screen("main")

    // Sub-screens
    object ReceiptPreview : Screen("receipt_preview")
    object ProductDetails : Screen("product_details/{productId}") {
        fun createRoute(productId: Long) = "product_details/$productId"
    }
    object AddEditProduct : Screen("add_edit_product?productId={productId}") {
        fun createRoute(productId: Long = 0L) = "add_edit_product?productId=$productId"
    }
    object UnitConversion : Screen("unit_conversion/{productId}") {
        fun createRoute(productId: Long) = "unit_conversion/$productId"
    }
    object StockTake : Screen("stock_take")
    object StockMovements : Screen("stock_movements")
    object Purchases : Screen("purchases")
    object NewPurchase : Screen("new_purchase")
    object Suppliers : Screen("suppliers")
    object Customers : Screen("customers")
    object CustomerCredit : Screen("customer_credit/{customerId}") {
        fun createRoute(customerId: Long) = "customer_credit/$customerId"
    }
    object Expenses : Screen("expenses")
    object CashSession : Screen("cash_session")
    object MpesaReconciliation : Screen("mpesa_reconciliation")
    object ReturnsRefunds : Screen("returns_refunds")
    object StaffManagement : Screen("staff_management")
    object AuditLog : Screen("audit_log")
    object Settings : Screen("settings")
    object BackupRestore : Screen("backup_restore")
}

enum class MainTab(val title: String, val subtitle: String, val route: String) {
    HOME("HOME", "Dashboard", "tab_home"),
    POS("POS", "Sales", "tab_pos"),
    INVENTORY("INVENTORY", "Products & Stock", "tab_inventory"),
    REPORTS("REPORTS", "Analytics", "tab_reports"),
    MORE("MORE", "Operations", "tab_more")
}
