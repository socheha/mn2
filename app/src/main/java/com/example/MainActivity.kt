package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Settings
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MenuKasScreen
import com.example.ui.screens.MenuBarangMasukScreen
import com.example.ui.screens.MenuBarangScreen
import com.example.ui.screens.MenuHutangSupplierScreen
import com.example.ui.screens.MenuPengaturanScreen
import com.example.ui.screens.MenuPenjualanHarianScreen
import com.example.ui.screens.MenuPiutangPelangganScreen
import com.example.ui.screens.MenuRekapScreen
import com.example.ui.screens.MenuStokTokoScreen
import com.example.ui.theme.SmartStockTheme

data class NavTabItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartStockTheme {
                var currentRoute by remember { mutableStateOf("dashboard") }

                val navItems = listOf(
                    NavTabItem("dashboard", "Dashboard", Icons.Default.Dashboard),
                    NavTabItem("barang", "Barang", Icons.Default.Inventory2),
                    NavTabItem("stok_toko", "Stok Toko", Icons.Default.Storefront),
                    NavTabItem("kas", "Kas & Bank", Icons.Default.AccountBalance),
                    NavTabItem("barang_masuk", "Barang Masuk", Icons.Default.LocalShipping),
                    NavTabItem("penjualan", "Penjualan", Icons.Default.PointOfSale),
                    NavTabItem("piutang", "Piutang", Icons.Default.AccountBalanceWallet),
                    NavTabItem("hutang", "Hutang", Icons.Default.MoneyOff),
                    NavTabItem("rekap", "Rekap", Icons.Default.Assessment),
                    NavTabItem("pengaturan", "Pengaturan", Icons.Default.Settings)
                )

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = R.drawable.ic_app_logo),
                                        contentDescription = "Logo MN SmartStock",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color.White, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "MN SmartStock",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "ALAT JARINGAN WIFI • TOKO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { currentRoute = "dashboard" },
                                    modifier = Modifier.testTag("top_bar_dashboard_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dashboard,
                                        contentDescription = "Dashboard",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = Color.White
                            )
                        )
                    },
                    bottomBar = {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ScrollableTabRow(
                                selectedTabIndex = navItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0),
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                edgePadding = 8.dp,
                                indicator = { tabPositions ->
                                    val index = navItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
                                    if (index < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            Modifier.tabIndicatorOffset(tabPositions[index]),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .testTag("app_navigation_bar")
                            ) {
                                navItems.forEach { item ->
                                    val isSelected = currentRoute == item.route
                                    Tab(
                                        selected = isSelected,
                                        onClick = { currentRoute = item.route },
                                        modifier = Modifier.testTag("nav_tab_${item.route}"),
                                        text = {
                                            Text(
                                                text = item.title,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp
                                            )
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.title,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentRoute) {
                            "dashboard" -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToMenu = { route -> currentRoute = route }
                            )
                            "barang" -> MenuBarangScreen(viewModel = viewModel)
                            "stok_toko" -> MenuStokTokoScreen(viewModel = viewModel)
                            "kas" -> MenuKasScreen(viewModel = viewModel)
                            "barang_masuk" -> MenuBarangMasukScreen(viewModel = viewModel)
                            "penjualan" -> MenuPenjualanHarianScreen(viewModel = viewModel)
                            "piutang" -> MenuPiutangPelangganScreen(viewModel = viewModel)
                            "hutang" -> MenuHutangSupplierScreen(viewModel = viewModel)
                            "rekap" -> MenuRekapScreen(viewModel = viewModel)
                            "pengaturan" -> MenuPengaturanScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
