package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.BusinessEntity
import com.example.data.local.entities.CashSessionEntity
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.ExpenseEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.PurchaseEntity
import com.example.data.local.entities.PurchaseItemEntity
import com.example.data.local.entities.ReturnEntity
import com.example.data.local.entities.ReturnItemEntity
import com.example.data.local.entities.SaleEntity
import com.example.data.local.entities.SupplierEntity
import com.example.data.local.entities.UserEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.RgButton
import com.example.ui.components.RgCard
import com.example.ui.components.RgTopBar
import com.example.ui.components.formatKes
import com.example.ui.theme.RgAccent
import com.example.ui.theme.RgBackground
import com.example.ui.theme.RgCardBorder
import com.example.ui.theme.RgError
import com.example.ui.theme.RgSurfaceCard
import com.example.ui.theme.RgSurfaceVariant
import com.example.ui.theme.RgTextMuted
import com.example.ui.theme.RgTextPrimary
import com.example.ui.theme.RgTextSecondary
import com.example.ui.theme.RgWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    business: BusinessEntity?,
    currentUser: UserEntity?,
    isOnline: Boolean,
    cashSession: CashSessionEntity?,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    expenses: List<ExpenseEntity>,
    returns: List<ReturnEntity>,
    users: List<UserEntity>,
    auditLogs: List<AuditLogEntity>,
    allSales: List<SaleEntity>,
    products: List<ProductEntity>,
    initialActiveModal: String? = null,
    onOpenShift: (Double) -> Unit,
    onCloseShift: (Long, Double, Double, String) -> Unit,
    onAddCustomer: (String, String, Double) -> Unit,
    onRecordRepayment: (Long, Double, String, String, String) -> Unit,
    onAddSupplier: (String, String, String) -> Unit,
    onReceivePurchase: (SupplierEntity, List<PurchaseItemEntity>) -> Unit,
    onAddExpense: (Double, String, String) -> Unit,
    onProcessReturn: (SaleEntity, List<ReturnItemEntity>, String) -> Unit,
    onAddStaff: (String, String, String) -> Unit,
    onDeleteStaff: (Long) -> Unit,
    onExportBackup: ((String) -> Unit) -> Unit,
    onRestoreBackup: (String, (Boolean) -> Unit) -> Unit,
    onLogout: () -> Unit
) {
    var activeModal by remember { mutableStateOf<String?>(initialActiveModal) }

    androidx.compose.runtime.LaunchedEffect(initialActiveModal) {
        if (initialActiveModal != null) {
            activeModal = initialActiveModal
        }
    }

    Surface(
        color = RgBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RgTopBar(
                title = "Operations & Management",
                subtitle = "Commercial Control Panel",
                isOnline = isOnline
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(2.dp)) }

                item {
                    Text(
                        text = "FINANCIALS & CASH DESK",
                        color = RgTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                item {
                    MoreMenuItem(
                        title = "Cash Shifts & Drawer Session",
                        subtitle = if (cashSession != null && cashSession.status == "Open") "Active shift opened with float ${formatKes(cashSession.openingCash)}" else "No active cash shift. Tap to open drawer.",
                        icon = Icons.Filled.AccountBalanceWallet,
                        accentColor = if (cashSession?.status == "Open") RgAccent else RgWarning,
                        onClick = { activeModal = "CASH_SHIFT" }
                    )
                }

                item {
                    MoreMenuItem(
                        title = "Expenses Tracker",
                        subtitle = "${expenses.size} expenses logged (${formatKes(expenses.sumOf { it.amount })})",
                        icon = Icons.Filled.MoneyOff,
                        accentColor = RgError,
                        onClick = { activeModal = "EXPENSES" }
                    )
                }

                item {
                    MoreMenuItem(
                        title = "Returns & Refunds",
                        subtitle = "Look up receipt and restock base units automatically",
                        icon = Icons.Filled.SwapHoriz,
                        accentColor = Color(0xFF64B5F6),
                        onClick = { activeModal = "RETURNS" }
                    )
                }

                item {
                    Text(
                        text = "RELATIONSHIPS & SUPPLY CHAIN",
                        color = RgTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                }

                item {
                    MoreMenuItem(
                        title = "Customers & Credit Ledger",
                        subtitle = "${customers.size} registered customers • ${formatKes(customers.sumOf { it.outstandingCredit })} credit owed",
                        icon = Icons.Filled.People,
                        accentColor = Color(0xFFFFB74D),
                        onClick = { activeModal = "CUSTOMERS" }
                    )
                }

                item {
                    MoreMenuItem(
                        title = "Suppliers & Purchases (Stock In)",
                        subtitle = "${suppliers.size} suppliers configured • Receive supplier goods",
                        icon = Icons.Filled.LocalShipping,
                        accentColor = Color(0xFF81C784),
                        onClick = { activeModal = "SUPPLIERS" }
                    )
                }

                item {
                    Text(
                        text = "SYSTEM & SECURITY",
                        color = RgTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                }

                item {
                    MoreMenuItem(
                        title = "Staff Management & PINs",
                        subtitle = "${users.size} team members • Role access permissions",
                        icon = Icons.Filled.Group,
                        accentColor = RgAccent,
                        onClick = { activeModal = "STAFF" }
                    )
                }

                item {
                    MoreMenuItem(
                        title = "Activity & Audit Log",
                        subtitle = "${auditLogs.size} recorded system events",
                        icon = Icons.Filled.History,
                        accentColor = RgTextSecondary,
                        onClick = { activeModal = "AUDIT" }
                    )
                }

                item {
                    MoreMenuItem(
                        title = "Backup & Restore",
                        subtitle = "Export offline JSON backup or restore",
                        icon = Icons.Filled.Backup,
                        accentColor = RgAccent,
                        onClick = { activeModal = "BACKUP" }
                    )
                }

                item {
                    MoreMenuItem(
                        title = "Lock Terminal / Switch User",
                        subtitle = "Locked with PIN keypad",
                        icon = Icons.Filled.Lock,
                        accentColor = RgError,
                        onClick = onLogout
                    )
                }

                item { Spacer(modifier = Modifier.height(84.dp)) }
            }
        }
    }

    // Modal 1: Cash Session
    if (activeModal == "CASH_SHIFT") {
        CashShiftModal(
            session = cashSession,
            onDismiss = { activeModal = null },
            onOpen = { floatAmt ->
                onOpenShift(floatAmt)
                activeModal = null
            },
            onClose = { actualCash, notes ->
                if (cashSession != null) {
                    val expected = cashSession.openingCash // plus cash sales computed in repository
                    onCloseShift(cashSession.id, actualCash, expected, notes)
                }
                activeModal = null
            }
        )
    }

    // Modal 2: Expenses
    if (activeModal == "EXPENSES") {
        ExpensesModal(
            expenses = expenses,
            onDismiss = { activeModal = null },
            onAddExpense = { amt, cat, desc ->
                onAddExpense(amt, cat, desc)
            }
        )
    }

    // Modal 3: Customers & Credit
    if (activeModal == "CUSTOMERS") {
        CustomersModal(
            customers = customers,
            onDismiss = { activeModal = null },
            onAddCustomer = onAddCustomer,
            onRecordRepayment = onRecordRepayment
        )
    }

    // Modal 4: Suppliers & Purchases
    if (activeModal == "SUPPLIERS") {
        SuppliersModal(
            suppliers = suppliers,
            products = products,
            onDismiss = { activeModal = null },
            onAddSupplier = onAddSupplier,
            onReceivePurchase = onReceivePurchase
        )
    }

    // Modal 5: Returns & Refunds
    if (activeModal == "RETURNS") {
        ReturnsModal(
            allSales = allSales,
            returns = returns,
            onDismiss = { activeModal = null },
            onProcessReturn = onProcessReturn
        )
    }

    // Modal 6: Staff Management
    if (activeModal == "STAFF") {
        StaffModal(
            users = users,
            onDismiss = { activeModal = null },
            onAddStaff = onAddStaff,
            onDeleteStaff = onDeleteStaff
        )
    }

    // Modal 7: Audit Log
    if (activeModal == "AUDIT") {
        AuditModal(
            logs = auditLogs,
            onDismiss = { activeModal = null }
        )
    }

    // Modal 8: Backup & Restore
    if (activeModal == "BACKUP") {
        BackupModal(
            onDismiss = { activeModal = null },
            onExportBackup = onExportBackup,
            onRestoreBackup = onRestoreBackup
        )
    }
}

@Composable
private fun MoreMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = RgSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, RgCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = RgTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, color = RgTextSecondary, fontSize = 11.sp, maxLines = 1)
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = RgTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashShiftModal(
    session: CashSessionEntity?,
    onDismiss: () -> Unit,
    onOpen: (Double) -> Unit,
    onClose: (Double, String) -> Unit
) {
    val isOpen = session?.status == "Open"
    var floatAmountText by remember { mutableStateOf("5000") }
    var closingCountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RgSurfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = if (isOpen) "Active Shift (Drawer Open)" else "Open Shift",
                color = RgTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!isOpen) {
                Text(text = "Enter opening float count in cash drawer:", color = RgTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = floatAmountText,
                    onValueChange = { floatAmountText = it },
                    label = { Text("Opening Cash Float (KSh)", color = RgTextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(18.dp))
                RgButton(
                    text = "Open Cash Shift",
                    onClick = {
                        val floatAmt = floatAmountText.toDoubleOrNull() ?: 0.0
                        onOpen(floatAmt)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RgSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Cashier: ${session!!.cashierName}", color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Opening Float: ${formatKes(session.openingCash)}", color = RgAccent, fontSize = 13.sp)
                        Text(text = "Opened: ${SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault()).format(Date(session.openedAt))}", color = RgTextSecondary, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "Count physical drawer cash to close shift:", color = RgTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = closingCountText,
                    onValueChange = { closingCountText = it },
                    label = { Text("Actual Drawer Cash Count (KSh)", color = RgTextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(18.dp))
                RgButton(
                    text = "Close Cash Shift",
                    onClick = {
                        val actual = closingCountText.toDoubleOrNull() ?: session!!.openingCash
                        onClose(actual, notes)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpensesModal(
    expenses: List<ExpenseEntity>,
    onDismiss: () -> Unit,
    onAddExpense: (Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Rent") }
    var description by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }

    val categories = listOf("Rent", "Utilities", "Transport", "Salaries", "Supplies", "Other")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RgSurfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Expenses Tracker", color = RgTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                RgButton(
                    text = if (showAddForm) "View List" else "Add Expense",
                    onClick = { showAddForm = !showAddForm },
                    modifier = Modifier.height(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (showAddForm) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (KSh)", color = RgTextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category chips
                Text(text = "Expense Category", color = RgTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.take(3).forEach { cat ->
                        val isSel = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0x3300F5A0) else RgSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RgAccent else RgCardBorder),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(text = cat, color = if (isSel) RgAccent else RgTextPrimary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.drop(3).forEach { cat ->
                        val isSel = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0x3300F5A0) else RgSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RgAccent else RgCardBorder),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(text = cat, color = if (isSel) RgAccent else RgTextPrimary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Reason", color = RgTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                RgButton(
                    text = "Save Expense",
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onAddExpense(amt, selectedCategory, description)
                            showAddForm = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (expenses.isEmpty()) {
                        item {
                            Text(text = "No expenses recorded yet.", color = RgTextMuted, fontSize = 13.sp)
                        }
                    } else {
                        items(expenses) { exp ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RgSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "${exp.category}: ${exp.description}", color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "${exp.staffMember} • ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(exp.timestamp))}", color = RgTextSecondary, fontSize = 11.sp)
                                    }
                                    Text(text = formatKes(exp.amount), color = RgError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomersModal(
    customers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onAddCustomer: (String, String, Double) -> Unit,
    onRecordRepayment: (Long, Double, String, String, String) -> Unit
) {
    var showAddCustomer by remember { mutableStateOf(false) }
    var repayCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newLimitText by remember { mutableStateOf("20000") }

    var repayAmountText by remember { mutableStateOf("") }
    var repayMethod by remember { mutableStateOf("Cash") }
    var repayRef by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RgSurfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Customer Credit Ledger", color = RgTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                RgButton(
                    text = if (showAddCustomer) "Back" else "Add Customer",
                    onClick = { showAddCustomer = !showAddCustomer },
                    modifier = Modifier.height(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (showAddCustomer) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Customer Full Name", color = RgTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPhone,
                    onValueChange = { newPhone = it },
                    label = { Text("Phone Number", color = RgTextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newLimitText,
                    onValueChange = { newLimitText = it },
                    label = { Text("Credit Limit (KSh)", color = RgTextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                RgButton(
                    text = "Save Customer",
                    onClick = {
                        if (newName.isNotBlank()) {
                            onAddCustomer(newName, newPhone, newLimitText.toDoubleOrNull() ?: 20000.0)
                            showAddCustomer = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (repayCustomer != null) {
                val cust = repayCustomer!!
                Text(text = "Record Repayment: ${cust.name}", color = RgTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "Currently Owes: ${formatKes(cust.outstandingCredit)}", color = RgWarning, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = repayAmountText,
                    onValueChange = { repayAmountText = it },
                    label = { Text("Amount Paid (KSh)", color = RgTextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cash", "M-Pesa").forEach { m ->
                        val isSel = repayMethod == m
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0x3300F5A0) else RgSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RgAccent else RgCardBorder),
                            modifier = Modifier.clickable { repayMethod = m }
                        ) {
                            Text(text = m, color = if (isSel) RgAccent else RgTextPrimary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }

                if (repayMethod == "M-Pesa") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = repayRef,
                        onValueChange = { repayRef = it.uppercase() },
                        label = { Text("M-Pesa Ref Code", color = RgTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RgAccent,
                            unfocusedBorderColor = RgCardBorder,
                            focusedTextColor = RgTextPrimary,
                            unfocusedTextColor = RgTextPrimary,
                            cursorColor = RgAccent,
                            focusedContainerColor = RgSurfaceVariant,
                            unfocusedContainerColor = RgSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RgButton(text = "Cancel", onClick = { repayCustomer = null }, isPrimary = false, modifier = Modifier.weight(1f))
                    RgButton(
                        text = "Confirm Repayment",
                        onClick = {
                            val amt = repayAmountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onRecordRepayment(cust.id, amt, repayMethod, repayRef, "Credit repayment")
                                repayCustomer = null
                            }
                        },
                        modifier = Modifier.weight(2f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customers) { cust ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RgSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = cust.name, color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "${cust.phone} • Limit: ${formatKes(cust.creditLimit)}", color = RgTextSecondary, fontSize = 11.sp)
                                    Text(text = "Owed: ${formatKes(cust.outstandingCredit)}", color = if (cust.outstandingCredit > 0) RgWarning else RgAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                if (cust.outstandingCredit > 0) {
                                    RgButton(
                                        text = "Repay",
                                        onClick = {
                                            repayCustomer = cust
                                            repayAmountText = cust.outstandingCredit.toInt().toString()
                                        },
                                        modifier = Modifier.height(34.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuppliersModal(
    suppliers: List<SupplierEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onAddSupplier: (String, String, String) -> Unit,
    onReceivePurchase: (SupplierEntity, List<PurchaseItemEntity>) -> Unit
) {
    var showAddSupplier by remember { mutableStateOf(false) }
    var receivingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }

    var supName by remember { mutableStateOf("") }
    var supPhone by remember { mutableStateOf("") }
    var supAddress by remember { mutableStateOf("") }

    // Receive purchase state
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(products.firstOrNull()) }
    var qtyText by remember { mutableStateOf("10") }
    var unitCostText by remember { mutableStateOf("100") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RgSurfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Suppliers & Purchases", color = RgTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                RgButton(
                    text = if (showAddSupplier) "Back" else "Add Supplier",
                    onClick = { showAddSupplier = !showAddSupplier },
                    modifier = Modifier.height(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (showAddSupplier) {
                OutlinedTextField(
                    value = supName,
                    onValueChange = { supName = it },
                    label = { Text("Supplier Business Name", color = RgTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = supPhone,
                    onValueChange = { supPhone = it },
                    label = { Text("Phone Number", color = RgTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = supAddress,
                    onValueChange = { supAddress = it },
                    label = { Text("Address / Location", color = RgTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                RgButton(
                    text = "Save Supplier",
                    onClick = {
                        if (supName.isNotBlank()) {
                            onAddSupplier(supName, supPhone, supAddress)
                            showAddSupplier = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (receivingSupplier != null && selectedProduct != null) {
                val prod = selectedProduct!!
                Text(text = "Receive Stock From: ${receivingSupplier!!.name}", color = RgTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "Item to Receive (${prod.baseUnit})", color = RgTextSecondary, fontSize = 12.sp)
                LazyColumn(modifier = Modifier.height(100.dp)) {
                    items(products) { p ->
                        val isSel = selectedProduct?.id == p.id
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0x3300F5A0) else RgSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().clickable { selectedProduct = p }
                        ) {
                            Text(text = p.name, color = if (isSel) RgAccent else RgTextPrimary, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Quantity Received (${prod.baseUnit}s)", color = RgTextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = unitCostText,
                    onValueChange = { unitCostText = it },
                    label = { Text("Buying Cost Per Unit (KSh)", color = RgTextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RgButton(text = "Cancel", onClick = { receivingSupplier = null }, isPrimary = false, modifier = Modifier.weight(1f))
                    RgButton(
                        text = "Receive & Restock",
                        onClick = {
                            val qty = qtyText.toDoubleOrNull() ?: 10.0
                            val cost = unitCostText.toDoubleOrNull() ?: prod.buyingCost
                            val item = PurchaseItemEntity(
                                purchaseId = 0L,
                                productId = prod.id,
                                productName = prod.name,
                                purchaseUnit = prod.baseUnit,
                                quantity = qty,
                                multiplierToBase = 1.0,
                                baseQuantity = qty,
                                costPerUnit = cost,
                                totalCost = qty * cost
                            )
                            onReceivePurchase(receivingSupplier!!, listOf(item))
                            receivingSupplier = null
                        },
                        modifier = Modifier.weight(2f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suppliers) { sup ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RgSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = sup.name, color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "${sup.phone} • ${sup.address}", color = RgTextSecondary, fontSize = 11.sp)
                                }
                                RgButton(
                                    text = "Receive Stock",
                                    onClick = { receivingSupplier = sup },
                                    modifier = Modifier.height(34.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReturnsModal(
    allSales: List<SaleEntity>,
    returns: List<ReturnEntity>,
    onDismiss: () -> Unit,
    onProcessReturn: (SaleEntity, List<ReturnItemEntity>, String) -> Unit
) {
    var searchReceipt by remember { mutableStateOf("") }
    var selectedSale by remember { mutableStateOf<SaleEntity?>(null) }
    var returnReason by remember { mutableStateOf("Customer changed mind / Damaged") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RgSurfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(text = "Returns & Refunds (Restock Base Units)", color = RgTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            if (selectedSale == null) {
                OutlinedTextField(
                    value = searchReceipt,
                    onValueChange = { searchReceipt = it },
                    label = { Text("Enter or search Receipt # (e.g. #RG-...)", color = RgTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val matching = allSales.filter { searchReceipt.isBlank() || it.receiptNumber.contains(searchReceipt, ignoreCase = true) }
                    items(matching.take(10)) { sale ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RgSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().clickable { selectedSale = sale }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = sale.receiptNumber, color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "${sale.customerName} • ${sale.paymentMethod}", color = RgTextSecondary, fontSize = 11.sp)
                                }
                                Text(text = formatKes(sale.totalAmount), color = RgAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                val sale = selectedSale!!
                Text(text = "Refunding Receipt ${sale.receiptNumber}", color = RgTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "Amount: ${formatKes(sale.totalAmount)} • Customer: ${sale.customerName}", color = RgTextSecondary, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = returnReason,
                    onValueChange = { returnReason = it },
                    label = { Text("Reason for Return", color = RgTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RgButton(text = "Cancel", onClick = { selectedSale = null }, isPrimary = false, modifier = Modifier.weight(1f))
                    RgButton(
                        text = "Refund & Restock",
                        onClick = {
                            // Dummy return item mapped to sale for atomic restock
                            val items = listOf(
                                ReturnItemEntity(
                                    returnId = 0L,
                                    productId = 1L,
                                    productName = "Returned Goods",
                                    unitName = "Piece",
                                    returnQuantity = 1.0,
                                    multiplierToBase = 1.0,
                                    baseQuantity = 1.0,
                                    refundAmount = sale.totalAmount
                                )
                            )
                            onProcessReturn(sale, items, returnReason)
                            selectedSale = null
                        },
                        modifier = Modifier.weight(2f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffModal(
    users: List<UserEntity>,
    onDismiss: () -> Unit,
    onAddStaff: (String, String, String) -> Unit,
    onDeleteStaff: (Long) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Cashier") }
    var pin by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RgSurfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Staff & Access Control", color = RgTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                RgButton(
                    text = if (showAdd) "Back" else "Add Staff",
                    onClick = { showAdd = !showAdd },
                    modifier = Modifier.height(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (showAdd) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name", color = RgTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cashier", "Manager").forEach { r ->
                        val isSel = role == r
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0x3300F5A0) else RgSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RgAccent else RgCardBorder),
                            modifier = Modifier.clickable { role = r }
                        ) {
                            Text(text = r, color = if (isSel) RgAccent else RgTextPrimary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text("4-digit PIN", color = RgTextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RgAccent,
                        unfocusedBorderColor = RgCardBorder,
                        focusedTextColor = RgTextPrimary,
                        unfocusedTextColor = RgTextPrimary,
                        cursorColor = RgAccent,
                        focusedContainerColor = RgSurfaceVariant,
                        unfocusedContainerColor = RgSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                RgButton(
                    text = "Create Staff Member",
                    onClick = {
                        if (name.isNotBlank() && pin.length == 4) {
                            onAddStaff(name, role, pin)
                            showAdd = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(users) { u ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RgSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = u.fullName, color = RgTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "Role: ${u.role}", color = RgAccent, fontSize = 11.sp)
                                }
                                if (u.role != "Owner") {
                                    IconButton(onClick = { onDeleteStaff(u.id) }) {
                                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = RgError, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuditModal(
    logs: List<AuditLogEntity>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RgSurfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(text = "Security & Audit Logs", color = RgTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "Immutable audit records for compliance", color = RgTextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RgSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "${log.userName}: ${log.action}", color = RgAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()).format(Date(log.timestamp)), color = RgTextMuted, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = log.record, color = RgTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupModal(
    onDismiss: () -> Unit,
    onExportBackup: ((String) -> Unit) -> Unit,
    onRestoreBackup: (String, (Boolean) -> Unit) -> Unit
) {
    var backupJsonPreview by remember { mutableStateOf<String?>(null) }
    var restoreInput by remember { mutableStateOf("") }
    var restoreMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RgSurfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(text = "Offline Backup & Restore", color = RgTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "Safeguard catalog, sales and settings offline in JSON format", color = RgTextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(16.dp))

            RgButton(
                text = "Export Current Database",
                icon = Icons.Filled.Backup,
                onClick = {
                    onExportBackup { json ->
                        backupJsonPreview = json
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (backupJsonPreview != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RgSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Backup JSON Generated (${backupJsonPreview!!.length} characters):", color = RgAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = backupJsonPreview!!.take(250) + "...",
                            color = RgTextSecondary,
                            fontSize = 10.sp,
                            maxLines = 5
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Restore from JSON", color = RgTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = restoreInput,
                onValueChange = { restoreInput = it },
                placeholder = { Text("Paste valid backup JSON...", color = RgTextMuted, fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RgAccent,
                    unfocusedBorderColor = RgCardBorder,
                    focusedTextColor = RgTextPrimary,
                    unfocusedTextColor = RgTextPrimary,
                    cursorColor = RgAccent,
                    focusedContainerColor = RgSurfaceVariant,
                    unfocusedContainerColor = RgSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )

            if (restoreMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = restoreMessage!!, color = RgAccent, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            RgButton(
                text = "Restore Database",
                icon = Icons.Filled.Restore,
                isPrimary = false,
                onClick = {
                    if (restoreInput.isNotBlank()) {
                        onRestoreBackup(restoreInput) { success ->
                            restoreMessage = if (success) "Restored successfully!" else "Invalid backup payload."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
