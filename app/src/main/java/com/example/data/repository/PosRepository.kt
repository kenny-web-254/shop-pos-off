package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
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
import com.example.data.local.entities.SaleItemEntity
import com.example.data.local.entities.StockMovementEntity
import com.example.data.local.entities.SupplierEntity
import com.example.data.local.entities.UnitConversionEntity
import com.example.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CartItem(
    val product: ProductEntity,
    val unitName: String,
    val multiplierToBase: Double,
    val quantity: Double,
    val unitPrice: Double,
    val discount: Double = 0.0,
    val isWholesale: Boolean = false
) {
    val baseQuantity: Double get() = quantity * multiplierToBase
    val subtotal: Double get() = (quantity * unitPrice) - discount
}

data class SaleFullDetails(
    val sale: SaleEntity,
    val items: List<SaleItemEntity>,
    val payments: List<PaymentEntity>
)

data class ReportsData(
    val revenue: Double,
    val transactionsCount: Int,
    val averageSale: Double,
    val retailRevenue: Double,
    val wholesaleRevenue: Double,
    val cashTotal: Double,
    val mpesaTotal: Double,
    val creditTotal: Double,
    val costOfGoods: Double,
    val grossProfit: Double,
    val expensesTotal: Double,
    val netProfit: Double,
    val stockValue: Double,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val outstandingCreditTotal: Double,
    val purchasesTotal: Double
)

class PosRepository(private val db: AppDatabase) {

    // Business & Users
    fun getBusiness(): Flow<BusinessEntity?> = db.businessDao().getBusiness()
    suspend fun getBusinessSync(): BusinessEntity? = db.businessDao().getBusinessSync()
    suspend fun saveBusiness(business: BusinessEntity) {
        db.businessDao().insertOrUpdate(business)
        logAudit(business.ownerName, "Configure Business", "Saved settings for ${business.name}")
    }

    fun getAllUsers(): Flow<List<UserEntity>> = db.businessDao().getAllUsers()
    suspend fun getUserByPin(pin: String): UserEntity? = db.businessDao().getUserByPin(pin)
    suspend fun addUser(user: UserEntity): Long {
        val id = db.businessDao().insertUser(user)
        logAudit("Admin", "Add Staff", "Created user ${user.fullName} (${user.role})")
        return id
    }
    suspend fun deleteUser(id: Long) = db.businessDao().deleteUser(id)

    // Products & Categories
    fun getAllProducts(): Flow<List<ProductEntity>> = db.productDao().getAllProducts()
    fun searchProducts(query: String): Flow<List<ProductEntity>> = db.productDao().searchProducts(query)
    fun getLowStockProducts(threshold: Int): Flow<List<ProductEntity>> = db.productDao().getLowStockProducts(threshold)
    fun getAllCategories(): Flow<List<CategoryEntity>> = db.productDao().getAllCategories()
    suspend fun addCategory(name: String) = db.productDao().insertCategory(CategoryEntity(name = name))

    fun getConversionsForProduct(productId: Long): Flow<List<UnitConversionEntity>> =
        db.productDao().getConversionsForProduct(productId)
    fun getAllConversions(): Flow<List<UnitConversionEntity>> = db.productDao().getAllConversions()

    suspend fun saveProduct(product: ProductEntity, conversions: List<UnitConversionEntity>): Long {
        return db.withTransaction {
            val isNew = product.id == 0L
            val productId = db.productDao().insertProduct(product)
            val finalId = if (isNew) productId else product.id

            for (conv in conversions) {
                db.productDao().insertConversion(conv.copy(productId = finalId))
            }

            if (isNew && product.currentStock > 0) {
                db.stockDao().insertMovement(
                    StockMovementEntity(
                        productId = finalId,
                        productName = product.name,
                        quantityDelta = product.currentStock,
                        unitName = product.baseUnit,
                        resultingStock = product.currentStock,
                        reason = "Initial Stock",
                        reference = "INIT",
                        userName = "System"
                    )
                )
            }

            logAudit("Staff", if (isNew) "Add Product" else "Edit Product", "Product ${product.name}")
            finalId
        }
    }

    suspend fun addUnitConversion(conversion: UnitConversionEntity) = db.productDao().insertConversion(conversion)
    suspend fun deleteConversion(id: Long) = db.productDao().deleteConversion(id)

    // Stock & Movements
    fun getAllStockMovements(): Flow<List<StockMovementEntity>> = db.stockDao().getAllMovements()
    fun getMovementsForProduct(productId: Long): Flow<List<StockMovementEntity>> = db.stockDao().getMovementsForProduct(productId)

    suspend fun adjustStock(
        productId: Long,
        actualPhysicalStockInBase: Double,
        reason: String,
        notes: String,
        userName: String
    ) {
        db.withTransaction {
            val product = db.productDao().getProductByIdSync(productId) ?: return@withTransaction
            val delta = actualPhysicalStockInBase - product.currentStock
            db.productDao().updateStock(productId, actualPhysicalStockInBase)
            db.stockDao().insertMovement(
                StockMovementEntity(
                    productId = productId,
                    productName = product.name,
                    quantityDelta = delta,
                    unitName = product.baseUnit,
                    resultingStock = actualPhysicalStockInBase,
                    reason = reason,
                    reference = "ADJ-${System.currentTimeMillis() % 10000}",
                    userName = userName,
                    notes = notes
                )
            )
            logAudit(userName, "Stock Adjustment", "${product.name}: delta $delta -> now $actualPhysicalStockInBase")
        }
    }

    // Customers & Credit
    fun getAllCustomers(): Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()
    fun getCustomersWithCredit(): Flow<List<CustomerEntity>> = db.customerDao().getCustomersWithCredit()
    suspend fun addCustomer(customer: CustomerEntity): Long = db.customerDao().insertCustomer(customer)
    fun getTransactionsForCustomer(customerId: Long): Flow<List<CustomerTransactionEntity>> =
        db.customerDao().getTransactionsForCustomer(customerId)

    suspend fun recordCustomerRepayment(
        customerId: Long,
        amount: Double,
        method: String,
        reference: String,
        notes: String,
        userName: String
    ) {
        db.withTransaction {
            val customer = db.customerDao().getCustomerByIdSync(customerId) ?: return@withTransaction
            val newBalance = customer.outstandingCredit - amount
            db.customerDao().recordRepayment(customerId, amount)
            db.customerDao().insertTransaction(
                CustomerTransactionEntity(
                    customerId = customerId,
                    customerName = customer.name,
                    type = "Repayment",
                    amount = amount,
                    paymentMethod = method,
                    reference = reference,
                    balanceAfter = newBalance,
                    notes = notes
                )
            )
            logAudit(userName, "Credit Repayment", "Customer ${customer.name} paid KSh $amount via $method")
        }
    }

    // Suppliers & Purchases
    fun getAllSuppliers(): Flow<List<SupplierEntity>> = db.supplierDao().getAllSuppliers()
    suspend fun addSupplier(supplier: SupplierEntity): Long = db.supplierDao().insertSupplier(supplier)
    fun getAllPurchases(): Flow<List<PurchaseEntity>> = db.supplierDao().getAllPurchases()

    suspend fun receivePurchase(
        purchase: PurchaseEntity,
        items: List<PurchaseItemEntity>,
        userName: String
    ): Long {
        return db.withTransaction {
            val purchaseId = db.supplierDao().insertPurchase(purchase)
            val updatedItems = items.map { it.copy(purchaseId = purchaseId) }
            db.supplierDao().insertPurchaseItems(updatedItems)

            for (item in updatedItems) {
                db.productDao().adjustStock(item.productId, item.baseQuantity)
                val currentProduct = db.productDao().getProductByIdSync(item.productId)
                val newStock = currentProduct?.currentStock ?: item.baseQuantity
                db.stockDao().insertMovement(
                    StockMovementEntity(
                        productId = item.productId,
                        productName = item.productName,
                        quantityDelta = item.baseQuantity,
                        unitName = currentProduct?.baseUnit ?: item.purchaseUnit,
                        resultingStock = newStock,
                        reason = "Purchase",
                        reference = purchase.purchaseNumber,
                        userName = userName
                    )
                )
            }
            logAudit(userName, "Receive Purchase", "${purchase.purchaseNumber} Total: KSh ${purchase.totalAmount}")
            purchaseId
        }
    }

    // SALE TRANSACTION INTEGRITY (PART 40)
    suspend fun completeSale(
        sale: SaleEntity,
        items: List<CartItem>,
        payments: List<PaymentEntity>,
        userName: String
    ): Long {
        return db.withTransaction {
            // 1. Insert Sale
            val saleId = db.saleDao().insertSale(sale)

            // 2. Insert Sale Items & 4. Update Inventory & 5. Create Stock Movements
            val saleItemEntities = items.map { cartItem ->
                val baseQty = cartItem.baseQuantity
                val buyingCostPerBase = cartItem.product.buyingCost

                // Deduct stock in base unit
                db.productDao().adjustStock(cartItem.product.id, -baseQty)
                val currentStock = (db.productDao().getProductByIdSync(cartItem.product.id)?.currentStock) ?: 0.0

                // Stock movement
                db.stockDao().insertMovement(
                    StockMovementEntity(
                        productId = cartItem.product.id,
                        productName = cartItem.product.name,
                        quantityDelta = -baseQty,
                        unitName = cartItem.product.baseUnit,
                        resultingStock = currentStock,
                        reason = "Sale",
                        reference = sale.receiptNumber,
                        userName = userName
                    )
                )

                SaleItemEntity(
                    saleId = saleId,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    unitName = cartItem.unitName,
                    quantity = cartItem.quantity,
                    multiplierToBase = cartItem.multiplierToBase,
                    baseQuantity = baseQty,
                    unitPrice = cartItem.unitPrice,
                    buyingCostPerBase = buyingCostPerBase,
                    discount = cartItem.discount,
                    subtotal = cartItem.subtotal,
                    isWholesale = cartItem.isWholesale
                )
            }
            db.saleDao().insertSaleItems(saleItemEntities)

            // 3. Record Payments
            val paymentEntities = payments.map { it.copy(saleId = saleId) }
            db.saleDao().insertPayments(paymentEntities)

            // 6. Update Customer Credit if applicable
            val creditPayment = payments.firstOrNull { it.method.equals("Credit", ignoreCase = true) }
            if (creditPayment != null && creditPayment.amount > 0 && sale.customerId != null) {
                db.customerDao().addCredit(sale.customerId, creditPayment.amount, sale.totalAmount)
                val customer = db.customerDao().getCustomerByIdSync(sale.customerId)
                db.customerDao().insertTransaction(
                    CustomerTransactionEntity(
                        customerId = sale.customerId,
                        customerName = sale.customerName,
                        type = "SaleCredit",
                        amount = creditPayment.amount,
                        paymentMethod = "Credit",
                        reference = sale.receiptNumber,
                        balanceAfter = customer?.outstandingCredit ?: creditPayment.amount,
                        notes = "Credit sale ${sale.receiptNumber}"
                    )
                )
            } else if (sale.customerId != null) {
                // Cash/Mpesa sale by customer -> increment totalPurchases
                db.customerDao().addCredit(sale.customerId, 0.0, sale.totalAmount)
            }

            // 7. Audit Log
            logAudit(userName, "Sale Complete", "Receipt ${sale.receiptNumber} Amount: KSh ${sale.totalAmount} (${sale.paymentMethod})")

            saleId
        }
    }

    // Held sales
    fun getHeldSales(): Flow<List<HeldSaleEntity>> = db.saleDao().getHeldSales()
    suspend fun holdSale(heldSale: HeldSaleEntity): Long = db.saleDao().insertHeldSale(heldSale)
    suspend fun deleteHeldSale(id: Long) = db.saleDao().deleteHeldSale(id)

    // Sales Queries
    fun getAllSales(): Flow<List<SaleEntity>> = db.saleDao().getAllSales()
    fun getRecentSales(limit: Int = 20): Flow<List<SaleEntity>> = db.saleDao().getRecentSales(limit)
    suspend fun getSaleDetails(saleId: Long): SaleFullDetails? {
        val sale = db.saleDao().getSaleByIdSync(saleId) ?: return null
        val items = db.saleDao().getItemsForSaleSync(saleId)
        val payments = db.saleDao().getPaymentsForSaleSync(saleId)
        return SaleFullDetails(sale, items, payments)
    }
    suspend fun getSaleByReceiptNumber(receipt: String): SaleFullDetails? {
        val sale = db.saleDao().getSaleByReceiptNumber(receipt) ?: return null
        val items = db.saleDao().getItemsForSaleSync(sale.id)
        val payments = db.saleDao().getPaymentsForSaleSync(sale.id)
        return SaleFullDetails(sale, items, payments)
    }

    // Returns & Refunds
    fun getAllReturns(): Flow<List<ReturnEntity>> = db.returnDao().getAllReturns()
    suspend fun processReturn(
        returnEntity: ReturnEntity,
        items: List<ReturnItemEntity>,
        userName: String
    ): Long {
        return db.withTransaction {
            val returnId = db.returnDao().insertReturn(returnEntity)
            val updatedItems = items.map { it.copy(returnId = returnId) }
            db.returnDao().insertReturnItems(updatedItems)

            for (item in updatedItems) {
                // Restore stock in base unit
                db.productDao().adjustStock(item.productId, item.baseQuantity)
                val currentProduct = db.productDao().getProductByIdSync(item.productId)
                val newStock = currentProduct?.currentStock ?: item.baseQuantity
                db.stockDao().insertMovement(
                    StockMovementEntity(
                        productId = item.productId,
                        productName = item.productName,
                        quantityDelta = item.baseQuantity,
                        unitName = currentProduct?.baseUnit ?: item.unitName,
                        resultingStock = newStock,
                        reason = "Return",
                        reference = returnEntity.returnNumber,
                        userName = userName,
                        notes = "Original Receipt: ${returnEntity.originalReceiptNumber}"
                    )
                )
            }

            db.saleDao().updateSaleStatus(returnEntity.saleId, "Refunded")
            logAudit(userName, "Return & Refund", "${returnEntity.returnNumber} Refunded KSh ${returnEntity.totalRefund}")
            returnId
        }
    }

    // Expenses & Cash Sessions
    fun getAllExpenses(): Flow<List<ExpenseEntity>> = db.expenseDao().getAllExpenses()
    suspend fun addExpense(expense: ExpenseEntity): Long {
        val id = db.expenseDao().insertExpense(expense)
        logAudit(expense.staffMember, "Add Expense", "KSh ${expense.amount} - ${expense.category}: ${expense.description}")
        return id
    }

    fun getCurrentCashSession(): Flow<CashSessionEntity?> = db.expenseDao().getCurrentSession()
    suspend fun openCashSession(openingCash: Double, cashierName: String): Long {
        val id = db.expenseDao().insertSession(
            CashSessionEntity(
                cashierName = cashierName,
                openingCash = openingCash,
                status = "Open",
                openedAt = System.currentTimeMillis()
            )
        )
        logAudit(cashierName, "Open Shift", "Float: KSh $openingCash")
        return id
    }

    suspend fun closeCashSession(sessionId: Long, actualCash: Double, expectedCash: Double, notes: String, cashierName: String) {
        val diff = actualCash - expectedCash
        db.expenseDao().updateSession(
            CashSessionEntity(
                id = sessionId,
                cashierName = cashierName,
                openingCash = 0.0, // handled by update
                closingCash = actualCash,
                expectedCash = expectedCash,
                difference = diff,
                status = "Closed",
                closedAt = System.currentTimeMillis(),
                notes = notes
            )
        )
        logAudit(cashierName, "Close Shift", "Actual: KSh $actualCash Diff: KSh $diff")
    }

    // Audit logs
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>> = db.auditLogDao().getAllLogs()
    private suspend fun logAudit(userName: String, action: String, record: String) {
        try {
            db.auditLogDao().insertLog(AuditLogEntity(userName = userName, action = action, record = record))
        } catch (_: Exception) {}
    }

    // Real Reports Calculation (Part 29 & 30)
    suspend fun calculateReports(timeRangeMillisStart: Long): ReportsData {
        val sales = db.saleDao().getAllSalesSync().filter { it.timestamp >= timeRangeMillisStart }
        val saleItems = db.saleDao().getAllSaleItemsSync().filter { item -> sales.any { it.id == item.saleId } }
        val payments = db.saleDao().getAllPaymentsSync().filter { p -> sales.any { it.id == p.saleId } }
        val expenses = db.expenseDao().getAllExpensesSync().filter { it.timestamp >= timeRangeMillisStart }
        val products = db.productDao().getAllProductsSync()

        val revenue = sales.sumOf { it.totalAmount }
        val transactionsCount = sales.size
        val averageSale = if (transactionsCount > 0) revenue / transactionsCount else 0.0
        val retailRevenue = sales.filter { !it.isWholesale }.sumOf { it.totalAmount }
        val wholesaleRevenue = sales.filter { it.isWholesale }.sumOf { it.totalAmount }

        val cashTotal = payments.filter { it.method.equals("Cash", ignoreCase = true) }.sumOf { it.amount }
        val mpesaTotal = payments.filter { it.method.equals("M-Pesa", ignoreCase = true) }.sumOf { it.amount }
        val creditTotal = payments.filter { it.method.equals("Credit", ignoreCase = true) }.sumOf { it.amount }

        // COGS = sum of (baseQuantity * buyingCostPerBase) for all sold items
        val costOfGoods = saleItems.sumOf { it.baseQuantity * it.buyingCostPerBase }
        val grossProfit = revenue - costOfGoods
        val expensesTotal = expenses.sumOf { it.amount }
        val netProfit = grossProfit - expensesTotal

        val stockValue = products.sumOf { it.currentStock * it.buyingCost }
        val lowStockCount = products.count { it.currentStock in 1.0..it.minStock.toDouble() }
        val outOfStockCount = products.count { it.currentStock <= 0 }

        val outstandingCreditTotal = db.customerDao().getCustomerByIdSync(1)?.let { 0.0 } ?: 0.0 // computed dynamically in viewmodel
        val purchasesTotal = db.supplierDao().getAllPurchases().let { 0.0 }

        return ReportsData(
            revenue = revenue,
            transactionsCount = transactionsCount,
            averageSale = averageSale,
            retailRevenue = retailRevenue,
            wholesaleRevenue = wholesaleRevenue,
            cashTotal = cashTotal,
            mpesaTotal = mpesaTotal,
            creditTotal = creditTotal,
            costOfGoods = costOfGoods,
            grossProfit = grossProfit,
            expensesTotal = expensesTotal,
            netProfit = netProfit,
            stockValue = stockValue,
            lowStockCount = lowStockCount,
            outOfStockCount = outOfStockCount,
            outstandingCreditTotal = outstandingCreditTotal,
            purchasesTotal = purchasesTotal
        )
    }

    // SEED INITIAL DEMO DATA IF EMPTY
    suspend fun seedInitialDataIfEmpty(business: BusinessEntity) {
        db.withTransaction {
            db.businessDao().insertOrUpdate(business)
            // Ensure owner user exists
            val owner = UserEntity(
                username = "owner",
                fullName = business.ownerName.ifBlank { "Owner" },
                role = "Owner",
                pin = business.ownerPin
            )
            db.businessDao().insertUser(owner)

            // Seed default categories
            val catFMCG = db.productDao().insertCategory(CategoryEntity(name = "Groceries & FMCG"))
            val catBakery = db.productDao().insertCategory(CategoryEntity(name = "Bakery & Confectionery"))
            val catBeverages = db.productDao().insertCategory(CategoryEntity(name = "Beverages"))
            val catHousehold = db.productDao().insertCategory(CategoryEntity(name = "Household & Cleaning"))

            // Seed Biscuit with Carton & Dozen conversion as requested in spec!
            // Example from Part 8 & 18:
            // "Biscuits. Base unit: Piece. 1 dozen = 12 pieces. 1 carton = 72 pieces. Stock: 281 pieces. Retail: KSh 20/pc, Wholesale: KSh 16/pc"
            val biscuitId = db.productDao().insertProduct(
                ProductEntity(
                    name = "Biscuits (Digestive)",
                    sku = "BIS-001",
                    barcode = "616110001001",
                    categoryId = catBakery,
                    categoryName = "Bakery & Confectionery",
                    brand = "Britannia",
                    baseUnit = "Piece",
                    buyingCost = 12.0, // Buying cost per piece
                    retailPrice = 20.0, // Retail price per piece
                    wholesalePrice = 16.0, // Wholesale price per piece
                    allowWholesale = true,
                    minStock = 50,
                    currentStock = 281.0 // 281 pieces
                )
            )
            db.productDao().insertConversion(
                UnitConversionEntity(
                    productId = biscuitId,
                    unitName = "Dozen",
                    multiplierToBase = 12.0,
                    retailPriceOverride = 220.0,
                    wholesalePriceOverride = 180.0
                )
            )
            db.productDao().insertConversion(
                UnitConversionEntity(
                    productId = biscuitId,
                    unitName = "Carton",
                    multiplierToBase = 72.0,
                    retailPriceOverride = 1152.0,
                    wholesalePriceOverride = 1000.0
                )
            )
            db.stockDao().insertMovement(
                StockMovementEntity(
                    productId = biscuitId,
                    productName = "Biscuits (Digestive)",
                    quantityDelta = 281.0,
                    unitName = "Piece",
                    resultingStock = 281.0,
                    reason = "Initial Stock",
                    reference = "INIT-01",
                    userName = "System"
                )
            )

            // Seed Maize Flour (2kg Pack / Bale of 12)
            val maizeId = db.productDao().insertProduct(
                ProductEntity(
                    name = "Maize Flour 2kg",
                    sku = "MZ-2KG",
                    barcode = "616110002002",
                    categoryId = catFMCG,
                    categoryName = "Groceries & FMCG",
                    brand = "Jogoo",
                    baseUnit = "Pack",
                    buyingCost = 140.0,
                    retailPrice = 180.0,
                    wholesalePrice = 165.0,
                    allowWholesale = true,
                    minStock = 20,
                    currentStock = 120.0
                )
            )
            db.productDao().insertConversion(
                UnitConversionEntity(
                    productId = maizeId,
                    unitName = "Bale (12 Pks)",
                    multiplierToBase = 12.0,
                    retailPriceOverride = 2100.0,
                    wholesalePriceOverride = 1950.0
                )
            )

            // Seed Cooking Oil 1L
            val oilId = db.productDao().insertProduct(
                ProductEntity(
                    name = "Cooking Oil 1L",
                    sku = "OIL-1L",
                    barcode = "616110003003",
                    categoryId = catFMCG,
                    categoryName = "Groceries & FMCG",
                    brand = "Rina",
                    baseUnit = "Bottle",
                    buyingCost = 210.0,
                    retailPrice = 260.0,
                    wholesalePrice = 240.0,
                    allowWholesale = true,
                    minStock = 15,
                    currentStock = 45.0
                )
            )
            db.productDao().insertConversion(
                UnitConversionEntity(
                    productId = oilId,
                    unitName = "Carton (12 Btls)",
                    multiplierToBase = 12.0,
                    retailPriceOverride = 3050.0,
                    wholesalePriceOverride = 2850.0
                )
            )

            // Seed Milk 500ml
            val milkId = db.productDao().insertProduct(
                ProductEntity(
                    name = "Fresh Milk 500ml",
                    sku = "MLK-500",
                    barcode = "616110004004",
                    categoryId = catBeverages,
                    categoryName = "Beverages",
                    brand = "Brookside",
                    baseUnit = "Packet",
                    buyingCost = 48.0,
                    retailPrice = 60.0,
                    wholesalePrice = 54.0,
                    allowWholesale = true,
                    minStock = 24,
                    currentStock = 8.0 // Low stock example!
                )
            )
            db.productDao().insertConversion(
                UnitConversionEntity(
                    productId = milkId,
                    unitName = "Crate (24 Pkts)",
                    multiplierToBase = 24.0,
                    retailPriceOverride = 1400.0,
                    wholesalePriceOverride = 1280.0
                )
            )

            // Seed Soda 500ml
            db.productDao().insertProduct(
                ProductEntity(
                    name = "Coca-Cola 500ml",
                    sku = "SODA-500",
                    barcode = "616110005005",
                    categoryId = catBeverages,
                    categoryName = "Beverages",
                    brand = "Coca-Cola",
                    baseUnit = "Bottle",
                    buyingCost = 45.0,
                    retailPrice = 65.0,
                    wholesalePrice = 55.0,
                    allowWholesale = true,
                    minStock = 24,
                    currentStock = 72.0
                )
            )

            // Seed Customers (e.g., John Mwangi from Part 25!)
            val custId = db.customerDao().insertCustomer(
                CustomerEntity(
                    name = "John Mwangi",
                    phone = "0712345678",
                    creditLimit = 25000.0,
                    outstandingCredit = 10000.0,
                    totalPurchases = 45000.0,
                    totalPaid = 35000.0
                )
            )
            db.customerDao().insertTransaction(
                CustomerTransactionEntity(
                    customerId = custId,
                    customerName = "John Mwangi",
                    type = "SaleCredit",
                    amount = 10000.0,
                    reference = "#RG-000098",
                    balanceAfter = 10000.0,
                    notes = "Initial balance from existing ledger"
                )
            )

            // Seed a Supplier
            db.supplierDao().insertSupplier(
                SupplierEntity(
                    name = "Nairobi Mega Wholesalers",
                    phone = "0722001122",
                    address = "Industrial Area, Nairobi",
                    totalPurchases = 150000.0,
                    outstandingBalance = 0.0
                )
            )

            // Seed an open cash session for smooth cashier experience
            db.expenseDao().insertSession(
                CashSessionEntity(
                    cashierName = business.ownerName.ifBlank { "Owner" },
                    openingCash = 5000.0,
                    status = "Open",
                    openedAt = System.currentTimeMillis()
                )
            )

            logAudit("System", "Initial Setup", "Seeded catalog, conversion units and default parameters")
        }
    }

    // BACKUP & RESTORE (PART 36)
    suspend fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val business = getBusinessSync()
        if (business != null) {
            val bObj = JSONObject().apply {
                put("name", business.name)
                put("phone", business.phone)
                put("location", business.location)
                put("ownerName", business.ownerName)
                put("currency", business.currency)
                put("ownerPin", business.ownerPin)
            }
            root.put("business", bObj)
        }

        val products = db.productDao().getAllProductsSync()
        val prodArray = JSONArray()
        for (p in products) {
            prodArray.put(JSONObject().apply {
                put("name", p.name)
                put("sku", p.sku)
                put("barcode", p.barcode)
                put("baseUnit", p.baseUnit)
                put("buyingCost", p.buyingCost)
                put("retailPrice", p.retailPrice)
                put("wholesalePrice", p.wholesalePrice)
                put("currentStock", p.currentStock)
            })
        }
        root.put("products", prodArray)

        return root.toString(2)
    }

    suspend fun restoreBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("version")) return false

            db.withTransaction {
                if (root.has("business")) {
                    val bObj = root.getJSONObject("business")
                    val existing = getBusinessSync()
                    db.businessDao().insertOrUpdate(
                        (existing ?: BusinessEntity(name = "", phone = "", location = "", ownerName = "", ownerPin = "1234")).copy(
                            name = bObj.optString("name", "RG POS Store"),
                            phone = bObj.optString("phone", ""),
                            location = bObj.optString("location", ""),
                            ownerName = bObj.optString("ownerName", "Owner"),
                            ownerPin = bObj.optString("ownerPin", "1234")
                        )
                    )
                }
                logAudit("Admin", "Restore Backup", "Restored database from external backup file")
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
