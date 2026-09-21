package com.rgdev.rgpos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rgdev.rgpos.domain.PriceSource
import com.rgdev.rgpos.domain.PriceType
import com.rgdev.rgpos.domain.Role
import java.util.UUID

// ---------- enums (Room stores them by name) ----------
enum class StockMovementType { OPENING_STOCK, PURCHASE, SALE, SALE_RETURN, DAMAGE, EXPIRED, ADJUSTMENT, STOCKTAKE, PURCHASE_RETURN, TRANSFER }
enum class SaleStatus { COMPLETED, PARTIALLY_RETURNED, RETURNED }
enum class PaymentMethod { CASH, MPESA }
enum class PaymentKind { SALE, REPAYMENT, REFUND }
enum class CustomerTxType { CREDIT_SALE, REPAYMENT, RETURN_CREDIT, ADJUSTMENT }
enum class PurchaseStatus { ORDERED, RECEIVED, CANCELLED }
enum class CashSessionStatus { OPEN, CLOSED }
enum class CashMovementType { CASH_IN, CASH_OUT }

// uuid + updatedAt on business records = ready for future cloud sync without a schema rewrite.
private fun newUuid() = UUID.randomUUID().toString()

// ---------- business / staff ----------
@Entity(tableName = "business")
data class BusinessEntity(
    @PrimaryKey val id: Long = 1,
    val name: String, val phone: String, val location: String, val ownerName: String,
    val currency: String = "KES",
    val receiptHeader: String = "",
    val receiptFooter: String = "Thank you for shopping with us!",
    val setupComplete: Boolean = false,
    val createdAt: Long
)

@Entity(tableName = "users", indices = [Index(value = ["name"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val role: Role,
    val pinHash: String, val pinSalt: String,
    val active: Boolean = true,
    val failedAttempts: Int = 0, val lockedUntil: Long = 0,
    val createdAt: Long
)

@Entity(tableName = "settings")
data class SettingEntity(@PrimaryKey val key: String, val value: String)

@Entity(tableName = "receipt_counters")
data class ReceiptCounterEntity(@PrimaryKey val day: String, val last: Int)

// ---------- catalogue ----------
@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val active: Boolean = true)

@Entity(tableName = "units", indices = [Index(value = ["name"], unique = true)])
data class UnitEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val abbreviation: String)

@Entity(
    tableName = "suppliers",
    indices = [Index(value = ["uuid"], unique = true), Index(value = ["name"])]
)
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val name: String, val phone: String? = null, val address: String? = null,
    val balanceCents: Long = 0,            // what we owe this supplier
    val active: Boolean = true,
    val createdAt: Long, val updatedAt: Long
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UnitEntity::class, parentColumns = ["id"], childColumns = ["baseUnitId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = SupplierEntity::class, parentColumns = ["id"], childColumns = ["supplierId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["sku"], unique = true),
        Index(value = ["barcode"], unique = true),
        Index(value = ["productCode"], unique = true),
        Index(value = ["name"]), Index(value = ["categoryId"]),
        Index(value = ["baseUnitId"]), Index(value = ["supplierId"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val name: String,
    val sku: String? = null, val barcode: String? = null, val productCode: String? = null,
    val categoryId: Long? = null, val brand: String? = null, val supplierId: Long? = null,
    val baseUnitId: Long,
    val stockBase: Long = 0,               // ALWAYS in base units
    val minStockBase: Long = 0,
    val avgCostMicro: Long = 0,            // weighted-average buying cost per BASE unit, micro-KSh
    val active: Boolean = true,            // archived products keep their history
    val createdAt: Long, val updatedAt: Long
)

/** Unit conversions: 1 [unitId] = [factorToBase] base units, for this product. */
@Entity(
    tableName = "product_units",
    foreignKeys = [
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UnitEntity::class, parentColumns = ["id"], childColumns = ["unitId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["productId", "unitId"], unique = true), Index(value = ["unitId"])]
)
data class ProductUnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long, val unitId: Long, val factorToBase: Long
)

@Entity(
    tableName = "product_prices",
    foreignKeys = [
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UnitEntity::class, parentColumns = ["id"], childColumns = ["unitId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["productId", "unitId", "priceType", "minQty"], unique = true), Index(value = ["unitId"])]
)
data class ProductPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long, val unitId: Long,
    val priceType: PriceType, val minQty: Long = 1, val priceCents: Long
)

// ---------- customers ----------
@Entity(
    tableName = "customers",
    indices = [Index(value = ["uuid"], unique = true), Index(value = ["name"]), Index(value = ["phone"])]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val name: String, val phone: String? = null, val address: String? = null,
    val creditLimitCents: Long? = null,    // null = unlimited, 0 = no credit
    val balanceCents: Long = 0,            // what the customer owes us (kept in step with the ledger)
    val defaultPriceType: PriceType = PriceType.RETAIL,
    val notes: String? = null,
    val active: Boolean = true,
    val createdAt: Long, val updatedAt: Long
)

@Entity(
    tableName = "customer_prices",
    foreignKeys = [
        ForeignKey(entity = CustomerEntity::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UnitEntity::class, parentColumns = ["id"], childColumns = ["unitId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["customerId", "productId", "unitId"], unique = true), Index(value = ["productId"]), Index(value = ["unitId"])]
)
data class CustomerPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long, val productId: Long, val unitId: Long, val priceCents: Long
)

// ---------- ledgers (append-only; triggers block UPDATE/DELETE) ----------
@Entity(
    tableName = "stock_movements",
    foreignKeys = [ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["productId", "createdAt"]), Index(value = ["refType", "refId"])]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long, val type: StockMovementType,
    val quantityBase: Long,                // signed: + in, - out
    val balanceAfterBase: Long,
    val reason: String,
    val refType: String? = null, val refId: Long? = null,
    val userId: Long?, val createdAt: Long
)

@Entity(
    tableName = "customer_transactions",
    foreignKeys = [
        ForeignKey(entity = CustomerEntity::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = SaleEntity::class, parentColumns = ["id"], childColumns = ["saleId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["uuid"], unique = true), Index(value = ["customerId", "createdAt"]), Index(value = ["saleId"])]
)
data class CustomerTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val customerId: Long, val type: CustomerTxType,
    val amountCents: Long,                 // + increases what they owe, - reduces it
    val balanceAfterCents: Long,
    val saleId: Long? = null, val note: String? = null,
    val userId: Long?, val createdAt: Long
)

@Entity(
    tableName = "supplier_transactions",
    foreignKeys = [ForeignKey(entity = SupplierEntity::class, parentColumns = ["id"], childColumns = ["supplierId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["supplierId", "createdAt"])]
)
data class SupplierTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val amountCents: Long,                 // + we owe more (purchase), - we paid
    val balanceAfterCents: Long,
    val purchaseId: Long? = null, val note: String? = null,
    val userId: Long?, val createdAt: Long
)

// ---------- purchasing ----------
@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(entity = SupplierEntity::class, parentColumns = ["id"], childColumns = ["supplierId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["uuid"], unique = true), Index(value = ["supplierId"]), Index(value = ["userId"]), Index(value = ["createdAt"])]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val supplierId: Long? = null, val invoiceNo: String? = null,
    val status: PurchaseStatus = PurchaseStatus.RECEIVED,
    val totalCents: Long, val paidCents: Long,
    val note: String? = null,
    val userId: Long, val createdAt: Long, val receivedAt: Long? = null
)

@Entity(
    tableName = "purchase_items",
    foreignKeys = [
        ForeignKey(entity = PurchaseEntity::class, parentColumns = ["id"], childColumns = ["purchaseId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UnitEntity::class, parentColumns = ["id"], childColumns = ["unitId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["purchaseId"]), Index(value = ["productId"]), Index(value = ["unitId"])]
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long, val productId: Long, val unitId: Long,
    val quantity: Long,                    // in the unit bought (e.g. 5 cartons)
    val unitFactor: Long,                  // snapshot of the conversion at that time
    val quantityBase: Long,                // quantity * unitFactor
    val unitCostCents: Long, val lineTotalCents: Long
)

// ---------- sales ----------
@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(entity = CustomerEntity::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CashSessionEntity::class, parentColumns = ["id"], childColumns = ["cashSessionId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [
        Index(value = ["uuid"], unique = true), Index(value = ["receiptNo"], unique = true),
        Index(value = ["customerId"]), Index(value = ["userId"]),
        Index(value = ["cashSessionId"]), Index(value = ["createdAt"])
    ]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val receiptNo: String,
    val customerId: Long? = null, val userId: Long, val cashSessionId: Long? = null,
    val priceType: PriceType,
    val subtotalCents: Long, val discountCents: Long, val totalCents: Long,
    val cashAppliedCents: Long, val cashTenderedCents: Long, val changeCents: Long,
    val mpesaCents: Long, val creditCents: Long,
    val costCents: Long,                   // COGS snapshot at time of sale
    val status: SaleStatus = SaleStatus.COMPLETED,
    val note: String? = null,
    val createdAt: Long
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(entity = SaleEntity::class, parentColumns = ["id"], childColumns = ["saleId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UnitEntity::class, parentColumns = ["id"], childColumns = ["unitId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["saleId"]), Index(value = ["productId"]), Index(value = ["unitId"])]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long, val productId: Long, val productName: String,
    val unitId: Long, val unitName: String, val unitFactor: Long,
    val quantityBase: Long,                // display qty = quantityBase / unitFactor
    val unitPriceCents: Long,              // price of ONE selling unit
    val discountCents: Long, val lineTotalCents: Long,
    val costCents: Long,                   // COGS snapshot for this line
    val priceSource: PriceSource
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(entity = SaleEntity::class, parentColumns = ["id"], childColumns = ["saleId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CustomerEntity::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CashSessionEntity::class, parentColumns = ["id"], childColumns = ["cashSessionId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [
        Index(value = ["mpesaRef"], unique = true),   // an M-Pesa code can only ever be used once
        Index(value = ["saleId"]), Index(value = ["customerId"]),
        Index(value = ["userId"]), Index(value = ["cashSessionId"]), Index(value = ["createdAt"])
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long? = null, val customerId: Long? = null,
    val kind: PaymentKind, val method: PaymentMethod,
    val amountCents: Long,                 // always positive; kind says direction
    val mpesaRef: String? = null,
    val cashSessionId: Long? = null,
    val userId: Long, val createdAt: Long
)

// ---------- expenses / cash ----------
@Entity(tableName = "expense_categories", indices = [Index(value = ["name"], unique = true)])
data class ExpenseCategoryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val active: Boolean = true)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(entity = ExpenseCategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CashSessionEntity::class, parentColumns = ["id"], childColumns = ["cashSessionId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["uuid"], unique = true), Index(value = ["categoryId"]), Index(value = ["userId"]), Index(value = ["cashSessionId"]), Index(value = ["incurredAt"])]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val categoryId: Long, val amountCents: Long, val description: String,
    val paidFromCash: Boolean = true, val cashSessionId: Long? = null,
    val userId: Long, val incurredAt: Long, val createdAt: Long
)

@Entity(
    tableName = "cash_sessions",
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["userId"]), Index(value = ["status"])]
)
data class CashSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long, val openedAt: Long, val closedAt: Long? = null,
    val openingCashCents: Long,
    val expectedCashCents: Long? = null, val actualCashCents: Long? = null, val cashDifferenceCents: Long? = null,
    val expectedMpesaCents: Long? = null, val actualMpesaCents: Long? = null, val mpesaDifferenceCents: Long? = null,
    val status: CashSessionStatus = CashSessionStatus.OPEN, val note: String? = null
)

@Entity(
    tableName = "cash_movements",
    foreignKeys = [
        ForeignKey(entity = CashSessionEntity::class, parentColumns = ["id"], childColumns = ["cashSessionId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["cashSessionId"]), Index(value = ["userId"])]
)
data class CashMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashSessionId: Long, val type: CashMovementType, val amountCents: Long,
    val reason: String, val userId: Long, val createdAt: Long
)

// ---------- returns ----------
@Entity(
    tableName = "returns",
    foreignKeys = [
        ForeignKey(entity = SaleEntity::class, parentColumns = ["id"], childColumns = ["saleId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["approvedByUserId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["uuid"], unique = true), Index(value = ["saleId"]), Index(value = ["userId"]), Index(value = ["approvedByUserId"])]
)
data class ReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = newUuid(),
    val saleId: Long, val userId: Long, val approvedByUserId: Long,
    val totalRefundCents: Long, val refundCashCents: Long, val refundMpesaCents: Long, val creditAdjustCents: Long,
    val reason: String, val createdAt: Long
)

@Entity(
    tableName = "return_items",
    foreignKeys = [
        ForeignKey(entity = ReturnEntity::class, parentColumns = ["id"], childColumns = ["returnId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = SaleItemEntity::class, parentColumns = ["id"], childColumns = ["saleItemId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["returnId"]), Index(value = ["saleItemId"]), Index(value = ["productId"])]
)
data class ReturnItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnId: Long, val saleItemId: Long, val productId: Long,
    val quantityBase: Long, val refundCents: Long,
    val restocked: Boolean, val damaged: Boolean
)

// ---------- audit ----------
@Entity(tableName = "audit_logs", indices = [Index(value = ["createdAt"]), Index(value = ["entityType", "entityId"])])
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long?, val action: String,
    val entityType: String? = null, val entityId: Long? = null, val details: String? = null,
    val prevHash: String, val hash: String, val createdAt: Long
)
