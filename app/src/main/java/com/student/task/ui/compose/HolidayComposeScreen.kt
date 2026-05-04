package com.student.task.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.student.task.domain.model.HolidayCategory
import com.student.task.presentation.HolidayViewModel
import com.student.task.presentation.ScreenState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayComposeScreen() {
    val viewModel: HolidayViewModel = hiltViewModel()
    val screenState by viewModel.screenState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Праздники России 2026",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = screenState) {
                is ScreenState.Loading -> {
                    LoadingState()
                }

                is ScreenState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.retry() }
                    )
                }

                is ScreenState.Data -> {
                    HolidayList(
                        state = state,
                        onFilterSelected = { viewModel.selectCategory(it) },
                        onCardClick = { viewModel.toggleCardState(it) },
                        onFavoriteClick = { viewModel.toggleFavorite(it) },
                        onLoadMore = { viewModel.loadNextPage() },
                        onRefresh = { viewModel.refresh() }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HolidayList(
    state: ScreenState.Data,
    onFilterSelected: (HolidayCategory?) -> Unit,
    onCardClick: (Int) -> Unit,
    onFavoriteClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit
) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= state.holidays.size - 2
        }
    }

    LaunchedEffect(shouldLoadMore, state.holidays.size) {
        if (shouldLoadMore && state.hasMorePages && !state.isLoadingMore) {
            onLoadMore()
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                FilterChips(
                    selectedCategory = state.selectedCategory,
                    onFilterSelected = onFilterSelected
                )
            }

            if (state.holidays.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .fillParentMaxHeight()
                    ) {
                        EmptyState()
                    }
                }
            } else {
                items(
                    items = state.holidays,
                    key = { it.holiday.id }
                ) { holidayUiModel ->
                    HolidayCard(
                        uiModel = holidayUiModel,
                        onClick = { onCardClick(holidayUiModel.holiday.id) },
                        onFavoriteClick = { onFavoriteClick(holidayUiModel.holiday.id) }
                    )
                }
            }

            if (state.isLoadingMore) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Загрузка...")
                    }
                }
            }

            if (!state.hasMorePages && state.holidays.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Все праздники загружены \uD83C\uDF89",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(72.dp)
                        .padding(vertical = 6.dp)
                        .graphicsLayer { this.alpha = 0.35f + alpha * 0.45f }
                ) {
                    androidx.compose.material3.Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxSize()
                    ) {}
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Загружаем праздники...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(text = "⚠️", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(onClick = onRetry) {
                Text("Повторить")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "\uD83D\uDD0D", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = "Праздники не найдены",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun FilterChips(
    selectedCategory: HolidayCategory?,
    onFilterSelected: (HolidayCategory?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onFilterSelected(null) },
                label = { Text("Все") }
            )
        }
        items(HolidayCategory.entries) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    if (selectedCategory == category) onFilterSelected(null)
                    else onFilterSelected(category)
                },
                label = { Text("${category.emoji} ${category.displayName}") }
            )
        }
    }
}
