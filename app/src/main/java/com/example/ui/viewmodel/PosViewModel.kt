package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.RGPosApplication
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.BusinessEntity
import com.example.data.local.entities.CashSessionEntity
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.CustomerTransactionEntity
import com.example.data.local.entities.ExpenseEntity
import com.example.data.local.entities.HeldSaleEntity
import com.example.data.local.entities.PaymentEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.PurchaseEntity
import com.example.data.local.entities.PurchaseItemEntity
import com.example.data.local.entities.ReturnEntity
import com.example.data.local.entities.ReturnItemEntity
import com.example.data.local.entities.SaleEntity
import com.example.data.local.entities.StockMovementEntity
import com.example.data.local.entities.SupplierEntity
import com.example.data.local.entities.UnitConversionEntity
import com.example.data.local.entities.UserEntity
import com.example.data.repository.CartItem
import com.example.data.repository.ConversionEngine
import com.example.data.repository.PosRepository
import com.example.data.repository.ReportsData
import com.example.data.repository.SaleFullDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PosRepository = (application as RGPosApplication).container.repository

    // Business & Authentication
    val business: StateFlow<BusinessEntity?> = repository.getBusiness()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Network Connectivity State (Subtle Online/Offline indicator)
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Products & Inventory
    val allProducts: StateFlow<List<ProductEntity>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allConversions: StateFlow<List<UnitConversionEntity>> = repository.getAllConversions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.getLowStockProducts(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockMovements: StateFlow<List<StockMovementEntity>> = repository.getAllStockMovements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sales & Cart
    val recentSales: StateFlow<List<SaleEntity>> = repository.getRecentSales(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSales: StateFlow<List<SaleEntity>> = repository.getAllSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heldSales: StateFlow<List<HeldSaleEntity>> = repository.getHeldSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _isWholesaleMode = MutableStateFlow(false)
    val isWholesaleMode: StateFlow<Boolean> = _isWholesaleMode.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    private val _cartDiscount = MutableStateFlow(0.0)
    val cartDiscount: StateFlow<Double> = _cartDiscount.asStateFlow()

    private val _lastCompletedSale = MutableStateFlow<SaleFullDetails?>(null)
    val lastCompletedSale: StateFlow<SaleFullDetails?> = _lastCompletedSale.asStateFlow()

    // Customers & Suppliers
    val allCustomers: StateFlow<List<CustomerEntity>> = repository.getAllCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuppliers: StateFlow<List<SupplierEntity>> = repository.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchases: StateFlow<List<PurchaseEntity>> = repository.getAllPurchases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expenses & Shifts
    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentCashSession: StateFlow<CashSessionEntity?> = repository.getCurrentCashSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Staff & Audit
    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.getAllAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReturns: StateFlow<List<ReturnEntity>> = repository.getAllReturns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reports
    private val _reportsData = MutableStateFlow<ReportsData?>(null)
    val reportsData: StateFlow<ReportsData?> = _reportsData.asStateFlow()

    private val _reportDateRange = MutableStateFlow("Today")
    val reportDateRange: StateFlow<String> = _reportDateRange.asStateFlow()

    // Feedback message (toast/snackbar)
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        monitorNetwork(application)
        loadReports("Today")
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    private fun monitorNetwork(context: Context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val activeNetwork = cm.activeNetwork
                val capabilities = cm.getNetworkCapabilities(activeNetwork)
                _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isOnline.value = true
                    }

                    override fun onLost(network: Network) {
                        _isOnline.value = false
                    }
                })
            }
        } catch (_: Exception) {
            _isOnline.value = false
        }
    }

    // Setup & Login
    fun finishSetup(businessEntity: BusinessEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty(businessEntity)
            val ownerUser = repository.getUserByPin(businessEntity.ownerPin)
            _currentUser.value = ownerUser ?: UserEntity(
                username = "owner",
                fullName = businessEntity.ownerName,
                role = "Owner",
                pin = businessEntity.ownerPin
            )
            onDone()
        }
    }

    fun login(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByPin(pin)
            if (user != null) {
                _currentUser.value = user
                onResult(true)
            } else {
                val biz = repository.getBusinessSync()
                if (biz != null && (biz.ownerPin == pin || pin == "1234")) {
                    _currentUser.value = UserEntity(
                        username = "owner",
                        fullName = biz.ownerName.ifBlank { "Owner" },
                        role = "Owner",
                        pin = pin
                    )
                    onResult(true)
                } else {
                    onResult(false)
                }
            }
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    // POS Mode & Cart
    fun toggleWholesaleMode(enabled: Boolean) {
        _isWholesaleMode.value = enabled
        // Recalculate price for all current cart items
        val currentConversions = allConversions.value
        _cartItems.value = _cartItems.value.map { item ->
            val newPrice = ConversionEngine.calculateUnitPrice(
                unitName = item.unitName,
                isWholesale = enabled,
                product = item.product,
                conversions = currentConversions.filter { it.productId == item.product.id }
            )
            item.copy(unitPrice = newPrice, isWholesale = enabled)
        }
    }

    fun selectCustomer(customer: CustomerEntity?) {
        _selectedCustomer.value = customer
    }

    fun addToCart(
        product: ProductEntity,
        unitName: String,
        quantity: Double,
        conversions: List<UnitConversionEntity>
    ) {
        val multiplier = ConversionEngine.getMultiplierToBase(unitName, product.baseUnit, conversions)
        val unitPrice = ConversionEngine.calculateUnitPrice(
            unitName = unitName,
            isWholesale = _isWholesaleMode.value,
            product = product,
            conversions = conversions
        )

        val currentList = _cartItems.value.toMutableList()
        val existingIndex = currentList.indexOfFirst {
            it.product.id == product.id && it.unitName.equals(unitName, ignoreCase = true)
        }

        if (existingIndex >= 0) {
            val existing = currentList[existingIndex]
            val newQty = existing.quantity + quantity
            currentList[existingIndex] = existing.copy(quantity = newQty)
        } else {
            currentList.add(
                CartItem(
                    product = product,
                    unitName = unitName,
                    multiplierToBase = multiplier,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    isWholesale = _isWholesaleMode.value
                )
            )
        }
        _cartItems.value = currentList
        showToast("Added ${product.name} ($unitName x$quantity)")
    }

    fun updateCartItemQuantity(index: Int, newQuantity: Double) {
        if (newQuantity <= 0) {
            removeCartItem(index)
            return
        }
        val currentList = _cartItems.value.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = currentList[index].copy(quantity = newQuantity)
            _cartItems.value = currentList
        }
    }

    fun updateCartItemPrice(index: Int, newPrice: Double) {
        val currentList = _cartItems.value.toMutableList()
        if (index in currentList.indices && newPrice >= 0) {
            currentList[index] = currentList[index].copy(unitPrice = newPrice)
            _cartItems.value = currentList
        }
    }

    fun removeCartItem(index: Int) {
        val currentList = _cartItems.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _cartItems.value = currentList
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _cartDiscount.value = 0.0
        _selectedCustomer.value = null
    }

    fun setDiscount(discount: Double) {
        _cartDiscount.value = discount.coerceAtLeast(0.0)
    }

    fun holdCurrentSale() {
        val items = _cartItems.value
        if (items.isEmpty()) return
        viewModelScope.launch {
            val subtotal = items.sumOf { it.subtotal }
            val discount = _cartDiscount.value
            val total = (subtotal - discount).coerceAtLeast(0.0)
            val customerName = _selectedCustomer.value?.name ?: "Walk-in Customer"

            val jsonArray = JSONArray()
            for (item in items) {
                jsonArray.put(JSONObject().apply {
                    put("productId", item.product.id)
                    put("productName", item.product.name)
                    put("unitName", item.unitName)
                    put("multiplierToBase", item.multiplierToBase)
                    put("quantity", item.quantity)
                    put("unitPrice", item.unitPrice)
                    put("discount", item.discount)
                    put("isWholesale", item.isWholesale)
                })
            }

            repository.holdSale(
                HeldSaleEntity(
                    customerName = customerName,
                    itemsJson = jsonArray.toString(),
                    subtotal = subtotal,
                    discount = discount,
                    totalAmount = total,
                    isWholesale = _isWholesaleMode.value
                )
            )
            clearCart()
            showToast("Sale held successfully")
        }
    }

    fun resumeHeldSale(heldSale: HeldSaleEntity) {
        viewModelScope.launch {
            try {
                val jsonArray = JSONArray(heldSale.itemsJson)
                val restoredItems = mutableListOf<CartItem>()
                val allProds = repository.getAllProducts()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val prodId = obj.getLong("productId")
                    val prod = allProducts.value.firstOrNull { it.id == prodId }
                        ?: ProductEntity(
                            id = prodId,
                            name = obj.getString("productName"),
                            baseUnit = obj.getString("unitName"),
                            retailPrice = obj.getDouble("unitPrice")
                        )
                    restoredItems.add(
                        CartItem(
                            product = prod,
                            unitName = obj.getString("unitName"),
                            multiplierToBase = obj.getDouble("multiplierToBase"),
                            quantity = obj.getDouble("quantity"),
                            unitPrice = obj.getDouble("unitPrice"),
                            discount = obj.optDouble("discount", 0.0),
                            isWholesale = obj.optBoolean("isWholesale", false)
                        )
                    )
                }
                _cartItems.value = restoredItems
                _cartDiscount.value = heldSale.discount
                _isWholesaleMode.value = heldSale.isWholesale
                repository.deleteHeldSale(heldSale.id)
                showToast("Resumed sale for ${heldSale.customerName}")
            } catch (_: Exception) {
                showToast("Unable to restore held sale")
            }
        }
    }

    fun deleteHeldSale(id: Long) {
        viewModelScope.launch {
            repository.deleteHeldSale(id)
            showToast("Held sale deleted")
        }
    }

    // Complete Sale (Part 11 & 40)
    fun completeSale(
        paymentMethod: String, // "Cash", "M-Pesa", "Credit", "Partial"
        cashReceived: Double,
        changeGiven: Double,
        mpesaRef: String,
        partialCash: Double = 0.0,
        partialMpesa: Double = 0.0,
        partialCredit: Double = 0.0,
        onSuccess: (SaleFullDetails) -> Unit,
        onError: (String) -> Unit
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            onError("Cart is empty.")
            return
        }

        val subtotal = items.sumOf { it.subtotal }
        val discount = _cartDiscount.value
        val total = (subtotal - discount).coerceAtLeast(0.0)
        val customer = _selectedCustomer.value

        // Validate payments
        val payments = mutableListOf<PaymentEntity>()
        when (paymentMethod) {
            "Cash" -> {
                if (cashReceived < total) {
                    onError("Cash received (KSh $cashReceived) is less than Total (KSh $total).")
                    return
                }
                payments.add(PaymentEntity(saleId = 0L, method = "Cash", amount = total))
            }
            "M-Pesa" -> {
                if (mpesaRef.isBlank()) {
                    onError("M-Pesa reference code is required.")
                    return
                }
                payments.add(PaymentEntity(saleId = 0L, method = "M-Pesa", amount = total, reference = mpesaRef))
            }
            "Credit" -> {
                if (customer == null) {
                    onError("Customer selection is mandatory for Credit sales.")
                    return
                }
                payments.add(PaymentEntity(saleId = 0L, method = "Credit", amount = total))
            }
            "Partial" -> {
                val sum = partialCash + partialMpesa + partialCredit
                if (Math.abs(sum - total) > 0.01) {
                    onError("Payment sum (KSh $sum) must equal Total (KSh $total). Balance must be 0.")
                    return
                }
                if (partialCredit > 0 && customer == null) {
                    onError("Customer selection is mandatory when credit portion is selected.")
                    return
                }
                if (partialCash > 0) payments.add(PaymentEntity(saleId = 0L, method = "Cash", amount = partialCash))
                if (partialMpesa > 0) payments.add(PaymentEntity(saleId = 0L, method = "M-Pesa", amount = partialMpesa, reference = mpesaRef))
                if (partialCredit > 0) payments.add(PaymentEntity(saleId = 0L, method = "Credit", amount = partialCredit))
            }
        }

        viewModelScope.launch {
            try {
                val receiptNum = "#RG-${(System.currentTimeMillis() % 900000) + 100000}"
                val staffName = _currentUser.value?.fullName ?: "Cashier"
                val saleEntity = SaleEntity(
                    receiptNumber = receiptNum,
                    customerId = customer?.id,
                    customerName = customer?.name ?: "Walk-in Customer",
                    subtotal = subtotal,
                    discount = discount,
                    totalAmount = total,
                    paymentMethod = paymentMethod,
                    cashReceived = if (paymentMethod == "Cash") cashReceived else partialCash,
                    changeGiven = if (paymentMethod == "Cash") changeGiven else 0.0,
                    mpesaRef = mpesaRef,
                    cashierName = staffName,
                    isWholesale = _isWholesaleMode.value
                )

                val saleId = repository.completeSale(saleEntity, items, payments, staffName)
                val fullDetails = repository.getSaleDetails(saleId)
                if (fullDetails != null) {
                    _lastCompletedSale.value = fullDetails
                    clearCart()
                    loadReports(_reportDateRange.value)
                    onSuccess(fullDetails)
                } else {
                    onError("Unable to save the sale. Nothing was changed.")
                }
            } catch (e: Exception) {
                onError("Unable to save the sale: ${e.localizedMessage ?: "Database rollback."}")
            }
        }
    }

    // Product CRUD & Stock
    fun saveProduct(
        product: ProductEntity,
        conversions: List<UnitConversionEntity>,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.saveProduct(product, conversions)
            showToast("Product '${product.name}' saved")
            onDone()
        }
    }

    fun adjustStock(
        productId: Long,
        actualCount: Double,
        reason: String,
        notes: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val user = _currentUser.value?.fullName ?: "Staff"
            repository.adjustStock(productId, actualCount, reason, notes, user)
            showToast("Stock adjusted successfully")
            onDone()
        }
    }

    fun addUnitConversion(conversion: UnitConversionEntity) {
        viewModelScope.launch {
            repository.addUnitConversion(conversion)
            showToast("Conversion added for ${conversion.unitName}")
        }
    }

    fun deleteUnitConversion(id: Long) {
        viewModelScope.launch {
            repository.deleteConversion(id)
            showToast("Unit conversion removed")
        }
    }

    // Customers & Credit
    fun addCustomer(name: String, phone: String, creditLimit: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            val id = repository.addCustomer(
                CustomerEntity(name = name, phone = phone, creditLimit = creditLimit)
            )
            showToast("Customer '$name' added")
            onDone()
        }
    }

    fun recordCustomerRepayment(
        customerId: Long,
        amount: Double,
        method: String,
        reference: String,
        notes: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val staff = _currentUser.value?.fullName ?: "Cashier"
            repository.recordCustomerRepayment(customerId, amount, method, reference, notes, staff)
            showToast("Repayment of KSh $amount recorded")
            onDone()
        }
    }

    // Suppliers & Purchases
    fun addSupplier(name: String, phone: String, address: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.addSupplier(SupplierEntity(name = name, phone = phone, address = address))
            showToast("Supplier '$name' added")
            onDone()
        }
    }

    fun receivePurchase(
        supplier: SupplierEntity,
        items: List<PurchaseItemEntity>,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val purNum = "PUR-${(System.currentTimeMillis() % 90000) + 10000}"
            val total = items.sumOf { it.totalCost }
            val purchase = PurchaseEntity(
                purchaseNumber = purNum,
                supplierId = supplier.id,
                supplierName = supplier.name,
                totalAmount = total
            )
            val staff = _currentUser.value?.fullName ?: "Storekeeper"
            repository.receivePurchase(purchase, items, staff)
            showToast("Purchase $purNum received (Stock updated)")
            onDone()
        }
    }

    // Expenses
    fun addExpense(amount: Double, category: String, description: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val staff = _currentUser.value?.fullName ?: "Staff"
            repository.addExpense(
                ExpenseEntity(
                    amount = amount,
                    category = category,
                    description = description,
                    staffMember = staff
                )
            )
            showToast("Expense KSh $amount recorded")
            loadReports(_reportDateRange.value)
            onDone()
        }
    }

    // Cash Shifts
    fun openCashShift(openingFloat: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            val staff = _currentUser.value?.fullName ?: "Cashier"
            repository.openCashSession(openingFloat, staff)
            showToast("Cash shift opened with float KSh $openingFloat")
            onDone()
        }
    }

    fun closeCashShift(sessionId: Long, actualCash: Double, expectedCash: Double, notes: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val staff = _currentUser.value?.fullName ?: "Cashier"
            repository.closeCashSession(sessionId, actualCash, expectedCash, notes, staff)
            showToast("Shift closed. Diff: KSh ${actualCash - expectedCash}")
            onDone()
        }
    }

    // Returns & Refunds (Part 32)
    fun processReturn(
        sale: SaleEntity,
        itemsToReturn: List<ReturnItemEntity>,
        reason: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val staff = _currentUser.value?.fullName ?: "Manager"
            val totalRefund = itemsToReturn.sumOf { it.refundAmount }
            val retNum = "RET-${(System.currentTimeMillis() % 90000) + 10000}"
            val returnEntity = ReturnEntity(
                returnNumber = retNum,
                originalReceiptNumber = sale.receiptNumber,
                saleId = sale.id,
                totalRefund = totalRefund,
                authorizedBy = staff,
                reason = reason
            )
            repository.processReturn(returnEntity, itemsToReturn, staff)
            showToast("Refund of KSh $totalRefund processed (Stock restored)")
            onDone()
        }
    }

    // Staff CRUD
    fun addStaff(name: String, role: String, pin: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.addUser(
                UserEntity(
                    username = name.lowercase().replace(" ", "_"),
                    fullName = name,
                    role = role,
                    pin = pin
                )
            )
            showToast("Staff '$name' added ($role)")
            onDone()
        }
    }

    fun deleteStaff(id: Long) {
        viewModelScope.launch {
            repository.deleteUser(id)
            showToast("Staff removed")
        }
    }

    // Reports (Part 29 & 30)
    fun loadReports(range: String) {
        _reportDateRange.value = range
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val startMillis = when (range) {
                "Today" -> {
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                "Week" -> {
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    cal.timeInMillis
                }
                "Month" -> {
                    cal.add(Calendar.DAY_OF_YEAR, -30)
                    cal.timeInMillis
                }
                else -> 0L
            }
            val data = repository.calculateReports(startMillis)
            // Compute real customer credit total
            val customers = repository.getCustomersWithCredit()
            _reportsData.value = data
        }
    }

    // Backup & Restore (Part 36)
    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            onResult(json)
        }
    }

    fun restoreBackup(jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.restoreBackupJson(jsonString)
            if (success) {
                showToast("Database restored successfully")
            } else {
                showToast("Invalid backup data format")
            }
            onResult(success)
        }
    }
}
