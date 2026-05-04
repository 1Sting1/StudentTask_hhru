package com.student.task.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.student.task.domain.model.HolidayCategory
import com.student.task.domain.usecase.GetHolidaysPageUseCase
import com.student.task.presentation.model.CardState
import com.student.task.presentation.model.HolidayUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HolidayViewModel @Inject constructor(
    private val getHolidaysPageUseCase: GetHolidaysPageUseCase
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private var currentPage = 0
    private var totalCount = 0
    private val allHolidays = mutableListOf<HolidayUiModel>()
    private var selectedCategory: HolidayCategory? = null

    init {
        loadInitial()
    }

    private fun loadInitial(showFullscreenLoading: Boolean = true) {
        viewModelScope.launch {
            if (showFullscreenLoading) {
                _screenState.value = ScreenState.Loading
            } else {
                val currentData = _screenState.value as? ScreenState.Data
                if (currentData != null) {
                    _screenState.value = currentData.copy(isRefreshing = true)
                }
            }
            totalCount = getHolidaysPageUseCase.getTotalCount()

            getHolidaysPageUseCase(page = 0).fold(
                onSuccess = { holidays ->
                    allHolidays.clear()
                    allHolidays.addAll(holidays.map { HolidayUiModel(it) })
                    currentPage = 0
                    publishDataState(isLoadingMore = false, isRefreshing = false)
                },
                onFailure = { error ->
                    if (showFullscreenLoading) {
                        _screenState.value = ScreenState.Error(
                            message = error.message ?: "Неизвестная ошибка"
                        )
                    } else {
                        val currentData = _screenState.value as? ScreenState.Data
                        if (currentData != null) {
                            _screenState.value = currentData.copy(isRefreshing = false)
                        }
                    }
                }
            )
        }
    }

    fun loadNextPage() {
        val current = _screenState.value
        if (current !is ScreenState.Data || current.isLoadingMore || !current.hasMorePages) return

        viewModelScope.launch {
            _screenState.value = current.copy(isLoadingMore = true)

            getHolidaysPageUseCase(page = currentPage + 1).fold(
                onSuccess = { holidays ->
                    currentPage++
                    allHolidays.addAll(holidays.map { HolidayUiModel(it) })
                    publishDataState(isLoadingMore = false, isRefreshing = current.isRefreshing)
                },
                onFailure = { error ->
                    _screenState.value = current.copy(isLoadingMore = false)
                }
            )
        }
    }

    fun retry() {
        loadInitial(showFullscreenLoading = true)
    }

    fun refresh() {
        if (_screenState.value is ScreenState.Data) {
            loadInitial(showFullscreenLoading = false)
        } else {
            loadInitial(showFullscreenLoading = true)
        }
    }

    fun selectCategory(category: HolidayCategory?) {
        selectedCategory = category
        val current = _screenState.value
        if (current is ScreenState.Data) {
            _screenState.value = current.copy(
                holidays = filteredHolidays(),
                selectedCategory = selectedCategory
            )
        }
    }

    fun toggleCardState(holidayId: Int) {
        val current = _screenState.value
        if (current !is ScreenState.Data) return

        val updatedList = allHolidays.map { uiModel ->
            if (uiModel.holiday.id == holidayId) {
                val newState = when (uiModel.cardState) {
                    CardState.Default -> CardState.Expanded
                    CardState.Expanded -> CardState.Default
                    CardState.Favorite -> CardState.Favorite
                }
                uiModel.copy(cardState = newState)
            } else {
                uiModel
            }
        }
        allHolidays.clear()
        allHolidays.addAll(updatedList)
        _screenState.value = current.copy(holidays = filteredHolidays())
    }

    fun toggleFavorite(holidayId: Int) {
        val current = _screenState.value
        if (current !is ScreenState.Data) return

        val updatedList = allHolidays.map { uiModel ->
            if (uiModel.holiday.id == holidayId) {
                val newState = when (uiModel.cardState) {
                    CardState.Favorite -> CardState.Default
                    else -> CardState.Favorite
                }
                uiModel.copy(cardState = newState)
            } else {
                uiModel
            }
        }
        allHolidays.clear()
        allHolidays.addAll(updatedList)
        _screenState.value = current.copy(holidays = filteredHolidays())
    }

    private fun filteredHolidays(): List<HolidayUiModel> {
        val category = selectedCategory
        if (category == null) return allHolidays.toList()
        return allHolidays.filter { it.holiday.category == category }
    }

    private fun publishDataState(isLoadingMore: Boolean, isRefreshing: Boolean) {
        _screenState.value = ScreenState.Data(
            holidays = filteredHolidays(),
            isLoadingMore = isLoadingMore,
            isRefreshing = isRefreshing,
            hasMorePages = allHolidays.size < totalCount,
            currentPage = currentPage,
            selectedCategory = selectedCategory
        )
    }
}
