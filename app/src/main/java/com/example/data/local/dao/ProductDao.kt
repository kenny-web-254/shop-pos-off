package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.UnitConversionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsSync(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun getProductById(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductByIdSync(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE isArchived = 0 AND (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isArchived = 0 AND currentStock <= :threshold ORDER BY currentStock ASC")
    fun getLowStockProducts(threshold: Int): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isArchived = 0 AND currentStock <= 0 ORDER BY name ASC")
    fun getOutOfStockProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET currentStock = :newStock WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Double)

    @Query("UPDATE products SET currentStock = currentStock + :delta WHERE id = :productId")
    suspend fun adjustStock(productId: Long, delta: Double)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("SELECT * FROM unit_conversions WHERE productId = :productId")
    fun getConversionsForProduct(productId: Long): Flow<List<UnitConversionEntity>>

    @Query("SELECT * FROM unit_conversions WHERE productId = :productId")
    suspend fun getConversionsForProductSync(productId: Long): List<UnitConversionEntity>

    @Query("SELECT * FROM unit_conversions")
    fun getAllConversions(): Flow<List<UnitConversionEntity>>

    @Query("SELECT * FROM unit_conversions")
    suspend fun getAllConversionsSync(): List<UnitConversionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversion(conversion: UnitConversionEntity): Long

    @Query("DELETE FROM unit_conversions WHERE id = :id")
    suspend fun deleteConversion(id: Long)

    @Query("DELETE FROM unit_conversions WHERE productId = :productId AND unitName = :unitName")
    suspend fun deleteConversionByName(productId: Long, unitName: String)
}
