package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.BusinessDao
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.ReturnDao
import com.example.data.local.dao.SaleDao
import com.example.data.local.dao.StockDao
import com.example.data.local.dao.SupplierDao
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

@Database(
    entities = [
        BusinessEntity::class,
        UserEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        UnitConversionEntity::class,
        StockMovementEntity::class,
        CustomerEntity::class,
        CustomerTransactionEntity::class,
        SupplierEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PaymentEntity::class,
        HeldSaleEntity::class,
        ExpenseEntity::class,
        CashSessionEntity::class,
        ReturnEntity::class,
        ReturnItemEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun productDao(): ProductDao
    abstract fun stockDao(): StockDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun returnDao(): ReturnDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rg_pos_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
