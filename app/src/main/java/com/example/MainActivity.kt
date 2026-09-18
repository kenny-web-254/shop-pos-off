package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.entities.ProductEntity
import com.example.ui.components.FloatingBottomBar
import com.example.ui.navigation.MainTab
import com.example.ui.navigation.Screen
import com.example.ui.screens.AddEditProductScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LaunchScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RgBackground
import com.example.ui.viewmodel.PosViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: PosViewModel) {
    // Collect Core State Flows
    val business by viewModel.business.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    // Products & Inventory
    val allProducts by viewModel.allProducts.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val allConversions by viewModel.allConversions.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val stockMovements by viewModel.stockMovements.collectAsState()

    // Cart & Sales
    val cartItems by viewModel.cartItems.collectAsState()
    val isWholesale by viewModel.isWholesaleMode.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val heldSales by viewModel.heldSales.collectAsState()
    val cartDiscount by viewModel.cartDiscount.collectAsState()
    val recentSales by viewModel.recentSales.collectAsState()
    val allSales by viewModel.allSales.collectAsState()

    // Operations & Intelligence
    val reportsData by viewModel.reportsData.collectAsState()
    val selectedReportRange by viewModel.reportDateRange.collectAsState()
    val cashSession by viewModel.currentCashSession.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()
    val suppliers by viewModel.allSuppliers.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()
    val returns by viewModel.allReturns.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    // Navigation Controllers & State
    val rootNavController = rememberNavController()
    val tabNavController = rememberNavController()
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var moreInitialModal by remember { mutableStateOf<String?>(null) }

    Surface(
        color = RgBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        NavHost(
            navController = rootNavController,
            startDestination = Screen.Launch.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Launch.route) {
                LaunchScreen(
                    business = business,
                    onNavigateToSetup = {
                        rootNavController.navigate(Screen.Setup.route) {
                            popUpTo(Screen.Launch.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        rootNavController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Launch.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Setup.route) {
                SetupScreen(
                    onFinishSetup = { newBusiness ->
                        viewModel.finishSetup(newBusiness) {
                            rootNavController.navigate(Screen.Main.route) {
                                popUpTo(Screen.Setup.route) { inclusive = true }
                            }
                        }
                    },
                    onRestoreBackupClick = {
                        rootNavController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Setup.route) { inclusive = true }
                        }
                        moreInitialModal = "BACKUP"
                        tabNavController.navigate(MainTab.MORE.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    business = business,
                    isOnline = isOnline,
                    onLoginSuccess = {
                        rootNavController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onVerifyPin = { pin, onResult ->
                        viewModel.login(pin, onResult)
                    }
                )
            }

            composable(Screen.AddEditProduct.route) {
                val prodConversions = editingProduct?.let { p ->
                    allConversions.filter { it.productId == p.id }
                } ?: emptyList()

                AddEditProductScreen(
                    existingProduct = editingProduct,
                    existingConversions = prodConversions,
                    categories = allCategories,
                    onBack = {
                        rootNavController.popBackStack()
                    },
                    onSave = { prod, convs ->
                        viewModel.saveProduct(prod, convs) {
                            rootNavController.popBackStack()
                        }
                    }
                )
            }

            composable(Screen.Main.route) {
                val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
                val currentTabRoute = navBackStackEntry?.destination?.route ?: MainTab.HOME.route
                val currentTab = MainTab.values().firstOrNull { it.route == currentTabRoute } ?: MainTab.HOME

                Scaffold(
                    containerColor = RgBackground,
                    bottomBar = {
                        FloatingBottomBar(
                            navController = tabNavController,
                            cartItemCount = cartItems.size,
                            heldSaleCount = heldSales.size
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = tabNavController,
                            startDestination = MainTab.HOME.route,
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(MainTab.HOME.route) {
                                HomeScreen(
                                    business = business,
                                    currentUser = currentUser,
                                    isOnline = isOnline,
                                    reportsData = reportsData,
                                    recentSales = recentSales,
                                    lowStockProducts = lowStockProducts,
                                    customersWithCredit = customers.filter { it.outstandingCredit > 0 },
                                    onNavigateTab = { targetTab ->
                                        tabNavController.navigate(targetTab.route) {
                                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onOpenAddProduct = {
                                        editingProduct = null
                                        rootNavController.navigate(Screen.AddEditProduct.route)
                                    },
                                    onOpenPurchases = {
                                        moreInitialModal = "SUPPLIERS"
                                        tabNavController.navigate(MainTab.MORE.route) {
                                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onOpenCustomers = {
                                        moreInitialModal = "CUSTOMERS"
                                        tabNavController.navigate(MainTab.MORE.route) {
                                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onOpenSaleDetails = { _ ->
                                        tabNavController.navigate(MainTab.REPORTS.route) {
                                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }

                            composable(MainTab.POS.route) {
                                PosScreen(
                                    business = business,
                                    isOnline = isOnline,
                                    products = allProducts,
                                    categories = allCategories,
                                    conversions = allConversions,
                                    customers = customers,
                                    cartItems = cartItems,
                                    isWholesale = isWholesale,
                                    selectedCustomer = selectedCustomer,
                                    heldSales = heldSales,
                                    cartDiscount = cartDiscount,
                                    onToggleWholesale = { viewModel.toggleWholesaleMode(it) },
                                    onSelectCustomer = { viewModel.selectCustomer(it) },
                                    onAddToCart = { prod, unit, qty, convs ->
                                        viewModel.addToCart(prod, unit, qty, convs)
                                    },
                                    onUpdateCartQty = { idx, qty ->
                                        viewModel.updateCartItemQuantity(idx, qty)
                                    },
                                    onUpdateCartPrice = { idx, prc ->
                                        viewModel.updateCartItemPrice(idx, prc)
                                    },
                                    onRemoveCartItem = { idx ->
                                        viewModel.removeCartItem(idx)
                                    },
                                    onClearCart = {
                                        viewModel.clearCart()
                                    },
                                    onSetDiscount = { disc ->
                                        viewModel.setDiscount(disc)
                                    },
                                    onHoldSale = {
                                        viewModel.holdCurrentSale()
                                    },
                                    onResumeHeldSale = { held ->
                                        viewModel.resumeHeldSale(held)
                                    },
                                    onDeleteHeldSale = { heldId ->
                                        viewModel.deleteHeldSale(heldId)
                                    },
                                    onCompleteSale = { method, cashRec, change, mpesaRef, pCash, pMpesa, pCredit, onSuccess, onError ->
                                        viewModel.completeSale(
                                            paymentMethod = method,
                                            cashReceived = cashRec,
                                            changeGiven = change,
                                            mpesaRef = mpesaRef,
                                            partialCash = pCash,
                                            partialMpesa = pMpesa,
                                            partialCredit = pCredit,
                                            onSuccess = onSuccess,
                                            onError = onError
                                        )
                                    }
                                )
                            }

                            composable(MainTab.INVENTORY.route) {
                                InventoryScreen(
                                    products = allProducts,
                                    conversions = allConversions,
                                    movements = stockMovements,
                                    isOnline = isOnline,
                                    onOpenAddProduct = {
                                        editingProduct = null
                                        rootNavController.navigate(Screen.AddEditProduct.route)
                                    },
                                    onOpenEditProduct = { prod ->
                                        editingProduct = prod
                                        rootNavController.navigate(Screen.AddEditProduct.route)
                                    },
                                    onAdjustStock = { prodId, count, reason, notes, onDone ->
                                        viewModel.adjustStock(prodId, count, reason, notes) {
                                            onDone()
                                        }
                                    }
                                )
                            }

                            composable(MainTab.REPORTS.route) {
                                ReportsScreen(
                                    reportsData = reportsData,
                                    selectedRange = selectedReportRange,
                                    isOnline = isOnline,
                                    onSelectRange = { range ->
                                        viewModel.loadReports(range)
                                    }
                                )
                            }

                            composable(MainTab.MORE.route) {
                                MoreScreen(
                                    business = business,
                                    currentUser = currentUser,
                                    isOnline = isOnline,
                                    cashSession = cashSession,
                                    customers = customers,
                                    suppliers = suppliers,
                                    expenses = expenses,
                                    returns = returns,
                                    users = users,
                                    auditLogs = auditLogs,
                                    allSales = allSales,
                                    products = allProducts,
                                    initialActiveModal = moreInitialModal,
                                    onOpenShift = { floatAmt ->
                                        viewModel.openCashShift(floatAmt) {}
                                    },
                                    onCloseShift = { sessId, actualCash, expectedCash, notes ->
                                        viewModel.closeCashShift(sessId, actualCash, expectedCash, notes) {}
                                    },
                                    onAddCustomer = { name, phone, limit ->
                                        viewModel.addCustomer(name, phone, limit) {}
                                    },
                                    onRecordRepayment = { custId, amt, method, ref, notes ->
                                        viewModel.recordCustomerRepayment(custId, amt, method, ref, notes) {}
                                    },
                                    onAddSupplier = { name, phone, address ->
                                        viewModel.addSupplier(name, phone, address) {}
                                    },
                                    onReceivePurchase = { supplier, items ->
                                        viewModel.receivePurchase(supplier, items) {}
                                    },
                                    onAddExpense = { amt, cat, desc ->
                                        viewModel.addExpense(amt, cat, desc) {}
                                    },
                                    onProcessReturn = { sale, items, reason ->
                                        viewModel.processReturn(sale, items, reason) {}
                                    },
                                    onAddStaff = { name, role, pin ->
                                        viewModel.addStaff(name, role, pin) {}
                                    },
                                    onDeleteStaff = { staffId ->
                                        viewModel.deleteStaff(staffId)
                                    },
                                    onExportBackup = { callback ->
                                        viewModel.exportBackup(callback)
                                    },
                                    onRestoreBackup = { json, callback ->
                                        viewModel.restoreBackup(json, callback)
                                    },
                                    onLogout = {
                                        viewModel.logout()
                                        rootNavController.navigate(Screen.Login.route) {
                                            popUpTo(Screen.Main.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
