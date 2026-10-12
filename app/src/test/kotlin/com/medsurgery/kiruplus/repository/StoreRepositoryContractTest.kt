package com.medsurgery.kiruplus.repository

import com.medsurgery.kiruplus.domain.store.StoreProduct
import com.medsurgery.kiruplus.domain.store.StoreRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreRepositoryContractTest {

    private fun makeProduct(id: String, inStock: Boolean = true, visible: Boolean = true) = StoreProduct(
        id = id,
        title = "Producto $id",
        price = "1200",
        currency = "MXN",
        stockStatus = if (inStock) "instock" else "outofstock",
        isVisible = visible,
        sortOrder = id.hashCode(),
    )

    private val p1 = makeProduct("prod-atlas")
    private val p2 = makeProduct("prod-kiru-pro", inStock = false)
    private val catalog = listOf(p1, p2)

    // --- fetchVisibleProducts ---

    @Test
    fun `fetchVisibleProducts returns all visible products`() = runTest {
        val repo = FakeStoreRepository(products = catalog)
        val result = repo.fetchVisibleProducts()
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
    }

    @Test
    fun `fetchVisibleProducts returns empty list when catalog empty`() = runTest {
        val repo = FakeStoreRepository()
        val result = repo.fetchVisibleProducts()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `fetchVisibleProducts returns failure on error`() = runTest {
        val repo = FakeStoreRepository(shouldFail = true)
        assertTrue(repo.fetchVisibleProducts().isFailure)
    }

    // --- fetchProduct ---

    @Test
    fun `fetchProduct returns correct product by id`() = runTest {
        val repo = FakeStoreRepository(products = catalog)
        val result = repo.fetchProduct("prod-atlas")
        assertTrue(result.isSuccess)
        assertEquals("prod-atlas", result.getOrThrow()?.id)
        assertTrue(result.getOrThrow()?.isInStock == true)
    }

    @Test
    fun `fetchProduct returns null for unknown id`() = runTest {
        val repo = FakeStoreRepository(products = catalog)
        val result = repo.fetchProduct("does-not-exist")
        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun `fetchProduct reflects stock status`() = runTest {
        val repo = FakeStoreRepository(products = catalog)
        val product = repo.fetchProduct("prod-kiru-pro").getOrThrow()
            ?: error("expected product")
        assertTrue(!product.isInStock)
    }

    @Test
    fun `fetchProduct returns failure on error`() = runTest {
        val repo = FakeStoreRepository(shouldFail = true)
        assertTrue(repo.fetchProduct("any").isFailure)
    }

    // --- price helpers ---

    @Test
    fun `StoreProduct priceAsDouble parses correctly`() {
        val product = makeProduct("p1")
        assertEquals(1200.0, product.priceAsDouble, 0.001)
    }
}

private class FakeStoreRepository(
    private val products: List<StoreProduct> = emptyList(),
    private val shouldFail: Boolean = false,
) : StoreRepository {

    override suspend fun fetchVisibleProducts(): Result<List<StoreProduct>> =
        if (shouldFail) Result.failure(RuntimeException("network error"))
        else Result.success(products.filter { it.isVisible })

    override suspend fun fetchProduct(id: String): Result<StoreProduct?> =
        if (shouldFail) Result.failure(RuntimeException("network error"))
        else Result.success(products.firstOrNull { it.id == id })
}
