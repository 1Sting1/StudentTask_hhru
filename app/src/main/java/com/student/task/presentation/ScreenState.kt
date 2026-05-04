package com.student.task.presentation

import com.student.task.domain.model.HolidayCategory
import com.student.task.presentation.model.HolidayUiModel

sealed class ScreenState {

    /**
     * Начальная загрузка данных.
     */
    data object Loading : ScreenState()

    /**
     * Ошибка загрузки данных.
     */
    data class Error(val message: String) : ScreenState()

    /**
     * Данные успешно загружены.
     */
    data class Data(
        val holidays: List<HolidayUiModel>,
        val isLoadingMore: Boolean,
        val isRefreshing: Boolean,
        val hasMorePages: Boolean,
        val currentPage: Int,
        val selectedCategory: HolidayCategory?
    ) : ScreenState()
}
