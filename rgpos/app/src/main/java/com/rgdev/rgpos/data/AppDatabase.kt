package com.rgdev.rgpos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BusinessEntity::class, UserEntity::class, SettingEntity::class, ReceiptCounterEntity::class,
        CategoryEntity::class, UnitEntity::class, SupplierEntity::class, ProductEntity::class,
        ProductUnitEntity::class, ProductPriceEntity::class, CustomerEntity::class, CustomerPriceEntity::class,
        StockMovementEntity::class, CustomerTransactionEntity::class, SupplierTransactionEntity::class,
        PurchaseEntity::class, PurchaseItemEntity::class, SaleEntity::class, SaleItemEntity::class,
        PaymentEntity::class, ExpenseCategoryEntity::class, ExpenseEntity::class,
        CashSessionEntity::class, CashMovementEntity::class, ReturnEntity::class, ReturnItemEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun unitDao(): UnitDao
    abstract fun productDao(): ProductDao
    abstract fun productUnitDao(): ProductUnitDao
    abstract fun priceDao(): PriceDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun customerDao(): CustomerDao
    abstract fun customerTransactionDao(): CustomerTransactionDao
    abstract fun supplierDao(): SupplierDao
    abstract fun supplierTransactionDao(): SupplierTransactionDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun saleDao(): SaleDao
    abstract fun paymentDao(): PaymentDao
    abstract fun receiptCounterDao(): ReceiptCounterDao
    abstract fun auditDao(): AuditDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "rgpos.db")
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addCallback(Guard)
                .build()

        /** Defence in depth: even a bug in app code can't create negative stock/balances or edit history. */
        private val TRIGGERS = listOf(
            "CREATE TRIGGER IF NOT EXISTS trg_no_negative_stock BEFORE UPDATE OF stockBase ON products " +
                "WHEN NEW.stockBase < 0 BEGIN SELECT RAISE(ABORT, 'NEGATIVE_STOCK'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_no_negative_customer_balance BEFORE UPDATE OF balanceCents ON customers " +
                "WHEN NEW.balanceCents < 0 BEGIN SELECT RAISE(ABORT, 'NEGATIVE_CUSTOMER_BALANCE'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_audit_no_update BEFORE UPDATE ON audit_logs BEGIN SELECT RAISE(ABORT, 'AUDIT_IMMUTABLE'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_audit_no_delete BEFORE DELETE ON audit_logs BEGIN SELECT RAISE(ABORT, 'AUDIT_IMMUTABLE'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_stock_mv_no_update BEFORE UPDATE ON stock_movements BEGIN SELECT RAISE(ABORT, 'LEDGER_IMMUTABLE'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_stock_mv_no_delete BEFORE DELETE ON stock_movements BEGIN SELECT RAISE(ABORT, 'LEDGER_IMMUTABLE'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_cust_tx_no_update BEFORE UPDATE ON customer_transactions BEGIN SELECT RAISE(ABORT, 'LEDGER_IMMUTABLE'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_cust_tx_no_delete BEFORE DELETE ON customer_transactions BEGIN SELECT RAISE(ABORT, 'LEDGER_IMMUTABLE'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_sale_items_no_delete BEFORE DELETE ON sale_items BEGIN SELECT RAISE(ABORT, 'SALES_IMMUTABLE'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_sales_no_delete BEFORE DELETE ON sales BEGIN SELECT RAISE(ABORT, 'SALES_IMMUTABLE'); END",
            "CREATE TRIGGER IF NOT EXISTS trg_payments_no_delete BEFORE DELETE ON payments BEGIN SELECT RAISE(ABORT, 'PAYMENTS_IMMUTABLE'); END"
        )

        private val SEED_UNITS = listOf(
            "Piece" to "pc", "Packet" to "pkt", "Box" to "box", "Carton" to "ctn", "Crate" to "crt",
            "Dozen" to "doz", "Bag" to "bag", "Bale" to "bale", "Roll" to "roll", "Pair" to "pr",
            "Set" to "set", "Kg" to "kg", "Gram" to "g", "Litre" to "L", "Millilitre" to "ml", "Metre" to "m"
        )
        private val SEED_EXPENSE_CATEGORIES =
            listOf("Rent", "Electricity", "Transport", "Salaries", "Airtime", "Repairs", "Supplies", "Other")

        private object Guard : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                SEED_UNITS.forEach { (n, a) -> db.execSQL("INSERT OR IGNORE INTO units(name, abbreviation) VALUES('$n', '$a')") }
                SEED_EXPENSE_CATEGORIES.forEach { db.execSQL("INSERT OR IGNORE INTO expense_categories(name, active) VALUES('$it', 1)") }
                TRIGGERS.forEach { db.execSQL(it) }
            }
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys = ON")
                TRIGGERS.forEach { db.execSQL(it) }   // idempotent; also covers restored backups
            }
        }
    }
}
