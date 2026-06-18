package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.DrawerContent
import com.example.ui.components.MusicPlayerOverlay
import com.example.ui.components.SlidingDrawerContainer
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MusicViewModel
import com.example.viewmodel.SortType
import kotlinx.coroutines.launch

enum class NavigationTarget {
    MAIN_MUSIC_SHEET, SETTINGS
}

enum class MusicFragmentsTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SONGS("Songs", Icons.Default.MusicNote),
    ARTISTS("Artists", Icons.Default.Person),
    ALBUMS("Albums", Icons.Default.Album),
    FOLDERS("Folders", Icons.Default.Folder),
    PLAYLISTS("Playlists", Icons.Default.LibraryMusic)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppEntry()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppEntry() {
    val viewModel: MusicViewModel = viewModel()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Screen State Routing
    var currentTargetScreen by remember { mutableStateOf(NavigationTarget.MAIN_MUSIC_SHEET) }
    var currentSelectedTab by remember { mutableStateOf(MusicFragmentsTab.SONGS) }

    // Search bar state UI toggles
    var isSearchingActive by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortType by viewModel.sortType.collectAsState()

    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
    val tracks by viewModel.allTracks.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()

    // Permission Launcher Setup
    val targetPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val reqPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onPermissionGranted()
            Toast.makeText(context, "Storage permissions authorized! Scanning music files.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissions denied. Falling back to public streaming tracks.", Toast.LENGTH_LONG).show()
        }
    }

    // Auto trigger permission request on first start
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            reqPermissionLauncher.launch(targetPermission)
        }
    }

    // Sort Drop-Down Menus Toggles
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    // Outer Sliding Drawer container encapsulates everything
    SlidingDrawerContainer(
        isOpen = isDrawerOpen,
        onClose = { viewModel.closeDrawer() },
        drawerContent = {
            DrawerContent(
                viewModel = viewModel,
                onNavigateToSettings = { currentTargetScreen = NavigationTarget.SETTINGS },
                onCloseDrawer = { viewModel.closeDrawer() }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (currentTargetScreen == NavigationTarget.SETTINGS) {
                            IconButton(onClick = { currentTargetScreen = NavigationTarget.MAIN_MUSIC_SHEET }) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back to player")
                            }
                        } else {
                            IconButton(
                                onClick = { viewModel.toggleDrawer() },
                                modifier = Modifier.testTag("hamburger_menu_button")
                            ) {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = "Open sliding menu")
                            }
                        }
                    },
                    title = {
                        if (currentTargetScreen == NavigationTarget.SETTINGS) {
                            Text(
                                "Settings",
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.testTag("app_title")
                            )
                        } else if (isSearchingActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text("Search songs, artists, albums...") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        viewModel.updateSearchQuery("")
                                        isSearchingActive = false
                                    }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Stop search")
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("toolbar_search_input")
                            )
                        } else {
                            Text(
                                text = "Amusic",
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.testTag("app_title")
                            )
                        }
                    },
                    actions = {
                        if (currentTargetScreen == NavigationTarget.MAIN_MUSIC_SHEET && !isSearchingActive) {
                            // Search Button
                            IconButton(
                                onClick = { isSearchingActive = true },
                                modifier = Modifier.testTag("start_search_button")
                            ) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search audio")
                            }

                            // Sort Selection Options Menu Trigger
                            Box {
                                IconButton(
                                    onClick = { isSortMenuExpanded = true },
                                    modifier = Modifier.testTag("sort_options_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort audios list"
                                    )
                                }
                                DropdownMenu(
                                    expanded = isSortMenuExpanded,
                                    onDismissRequest = { isSortMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Sort by Tracker Name") },
                                        leadingIcon = { Icon(Icons.Default.MusicNote, null) },
                                        onClick = {
                                            viewModel.updateSortType(SortType.NAME)
                                            isSortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sort by Artist Composition") },
                                        leadingIcon = { Icon(Icons.Default.Person, null) },
                                        onClick = {
                                            viewModel.updateSortType(SortType.ARTIST)
                                            isSortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sort by Date Modified") },
                                        leadingIcon = { Icon(Icons.Default.Timelapse, null) },
                                        onClick = {
                                            viewModel.updateSortType(SortType.MODIFIED_DATE)
                                            isSortMenuExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sort by Track Length/Duration") },
                                        leadingIcon = { Icon(Icons.Default.AccessTime, null) },
                                        onClick = {
                                            viewModel.updateSortType(SortType.DURATION)
                                            isSortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                    )
                )
            },
            bottomBar = {
                if (currentTargetScreen == NavigationTarget.MAIN_MUSIC_SHEET) {
                    Column {
                        // Floating overlay controller mini player if a track is active
                        MusicPlayerOverlay(viewModel = viewModel)

                        // Elegant Navigation Bar for switching fragments
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            MusicFragmentsTab.values().forEach { tab ->
                                NavigationBarItem(
                                    selected = currentSelectedTab == tab,
                                    onClick = { currentSelectedTab = tab },
                                    icon = {
                                        Icon(imageVector = tab.icon, contentDescription = tab.title)
                                    },
                                    label = {
                                        Text(tab.title)
                                    },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (currentTargetScreen == NavigationTarget.SETTINGS) {
                    // Render settings layout
                    SettingsScreen(viewModel = viewModel)
                } else {
                    // MAIN MUSIC VIEWS WITH PERMISSION BANNER INTEGRATIONS
                    Column(modifier = Modifier.fillMaxSize()) {
                        
                        // Storage permission prompt card rendered if unauthorized
                        if (!hasPermission) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Permission requirements info",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Local files catalog is limited.",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Approve audio permission to index, list, and play your local device files.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { reqPermissionLauncher.launch(targetPermission) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Allow", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // Content screens router depending on bottom bar fragments selection
                        Box(modifier = Modifier.weight(1f)) {
                            when (currentSelectedTab) {
                                MusicFragmentsTab.SONGS -> SongsScreen(
                                    viewModel = viewModel,
                                    tracks = tracks,
                                    searchQuery = searchQuery,
                                    sortType = sortType,
                                    onTrackSelect = { selectedTrack, folderList ->
                                        viewModel.selectAndPlayTrack(selectedTrack, folderList)
                                    }
                                )
                                MusicFragmentsTab.ARTISTS -> ArtistsScreen(
                                    viewModel = viewModel,
                                    tracks = tracks,
                                    searchQuery = searchQuery,
                                    onTrackSelect = { selectedTrack, list ->
                                        viewModel.selectAndPlayTrack(selectedTrack, list)
                                    }
                                )
                                MusicFragmentsTab.ALBUMS -> AlbumsScreen(
                                    viewModel = viewModel,
                                    tracks = tracks,
                                    searchQuery = searchQuery,
                                    onTrackSelect = { selectedTrack, list ->
                                        viewModel.selectAndPlayTrack(selectedTrack, list)
                                    }
                                )
                                MusicFragmentsTab.FOLDERS -> FoldersScreen(
                                    viewModel = viewModel,
                                    tracks = tracks,
                                    searchQuery = searchQuery,
                                    onTrackSelect = { selectedTrack, list ->
                                        viewModel.selectAndPlayTrack(selectedTrack, list)
                                    }
                                )
                                MusicFragmentsTab.PLAYLISTS -> PlaylistsScreen(
                                    viewModel = viewModel,
                                    tracks = tracks,
                                    onTrackSelect = { selectedTrack, list ->
                                        viewModel.selectAndPlayTrack(selectedTrack, list)
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
