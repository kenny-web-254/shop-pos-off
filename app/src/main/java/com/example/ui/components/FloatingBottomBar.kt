package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.MainTab
import com.example.ui.theme.RgAccent
import com.example.ui.theme.RgAccentSubtle
import com.example.ui.theme.RgCardBorder
import com.example.ui.theme.RgSurfaceCard
import com.example.ui.theme.RgTextMuted
import com.example.ui.theme.RgTextPrimary
import com.example.ui.theme.RgTextSecondary

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun FloatingBottomBar(
    navController: NavController,
    cartItemCount: Int = 0,
    heldSaleCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route
    val selectedTab = MainTab.values().firstOrNull { it.route == currentDestination } ?: MainTab.HOME

    FloatingBottomBar(
        selectedTab = selectedTab,
        onTabSelected = { tab ->
            navController.navigate(tab.route) {
                // Pop up to the start destination of the graph to avoid building up a large stack of destinations
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                // Avoid multiple copies of the same destination when reselecting the same item
                launchSingleTop = true
                // Restore state when reselecting a previously selected item
                restoreState = true
            }
        },
        cartItemCount = cartItemCount,
        heldSaleCount = heldSaleCount,
        modifier = modifier
    )
}

@Composable
fun FloatingBottomBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    cartItemCount: Int = 0,
    heldSaleCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xF2101520),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x3300F5A0)),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = tab == selectedTab
                    val iconSelected = when (tab) {
                        MainTab.HOME -> Icons.Filled.Dashboard
                        MainTab.POS -> Icons.Filled.PointOfSale
                        MainTab.INVENTORY -> Icons.Filled.Inventory2
                        MainTab.REPORTS -> Icons.Filled.Analytics
                        MainTab.MORE -> Icons.Filled.MoreHoriz
                    }
                    val iconUnselected = when (tab) {
                        MainTab.HOME -> Icons.Outlined.Dashboard
                        MainTab.POS -> Icons.Outlined.PointOfSale
                        MainTab.INVENTORY -> Icons.Outlined.Inventory2
                        MainTab.REPORTS -> Icons.Outlined.Analytics
                        MainTab.MORE -> Icons.Outlined.MoreHoriz
                    }

                    val animScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.06f else 1.0f,
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        label = "scale"
                    )

                    val tintColor by animateColorAsState(
                        targetValue = if (isSelected) RgAccent else RgTextMuted,
                        animationSpec = tween(200),
                        label = "tint"
                    )

                    val pillBgColor by animateColorAsState(
                        targetValue = if (isSelected) RgAccentSubtle else Color.Transparent,
                        animationSpec = tween(200),
                        label = "pill"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .testTag("nav_tab_${tab.name.lowercase()}")
                            .clip(RoundedCornerShape(18.dp))
                            .background(pillBgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onTabSelected(tab)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .scale(animScale)
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = if (isSelected) iconSelected else iconUnselected,
                                contentDescription = tab.title,
                                tint = tintColor,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(20.dp)
                            )

                            // Dynamic badge for POS (Cart Items)
                            if (tab == MainTab.POS && cartItemCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = RgAccent,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF080B10)),
                                    modifier = Modifier.size(14.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (cartItemCount > 9) "9+" else cartItemCount.toString(),
                                            color = Color(0xFF080B10),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = tab.title,
                            color = tintColor,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
