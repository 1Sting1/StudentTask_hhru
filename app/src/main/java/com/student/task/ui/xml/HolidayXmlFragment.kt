package com.student.task.ui.xml

import android.os.Bundle
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.student.task.R
import com.student.task.databinding.FragmentHolidayXmlBinding
import com.student.task.domain.model.HolidayCategory
import com.student.task.presentation.HolidayViewModel
import com.student.task.presentation.ScreenState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HolidayXmlFragment : Fragment() {

    private var _binding: FragmentHolidayXmlBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HolidayViewModel by viewModels()

    private val adapter = HolidayAdapter(
        onCardClick = { holidayId -> viewModel.toggleCardState(holidayId) },
        onFavoriteClick = { holidayId -> viewModel.toggleFavorite(holidayId) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHolidayXmlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilters()
        binding.retryButton.setOnClickListener { viewModel.retry() }
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        observeState()
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                if (lastVisibleItem >= totalItemCount - 2) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.screenState.collect { state ->
                    when (state) {
                        is ScreenState.Loading -> showLoading()
                        is ScreenState.Error -> showError(state.message)
                        is ScreenState.Data -> showData(state)
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.swipeRefresh.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
        binding.emptyLayout.visibility = View.GONE
        binding.loadingLayout.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = false
    }

    private fun showError(message: String) {
        binding.swipeRefresh.visibility = View.GONE
        binding.loadingLayout.visibility = View.GONE
        binding.emptyLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE

        binding.errorMessage.text = message
        binding.swipeRefresh.isRefreshing = false
    }

    private fun showData(state: ScreenState.Data) {
        binding.loadingLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
        binding.swipeRefresh.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = state.isRefreshing
        binding.emptyLayout.visibility = if (state.holidays.isEmpty()) View.VISIBLE else View.GONE

        adapter.submitHolidays(state.holidays, state.isLoadingMore)
        updateFilterSelection(state.selectedCategory)
    }

    private fun setupFilters() {
        addFilterChip(text = "Все", category = null)
        HolidayCategory.entries.forEach { category ->
            addFilterChip(
                text = "${category.emoji} ${category.displayName}",
                category = category
            )
        }
        updateFilterSelection(null)
    }

    private fun addFilterChip(text: String, category: HolidayCategory?) {
        val chip = TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            setPadding(28, 14, 28, 14)
            setOnClickListener { viewModel.selectCategory(category) }
        }
        val params = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.marginEnd = 12
        chip.layoutParams = params
        chip.tag = category
        binding.filterContainer.addView(chip)
    }

    private fun updateFilterSelection(selectedCategory: HolidayCategory?) {
        for (i in 0 until binding.filterContainer.childCount) {
            val chip = binding.filterContainer.getChildAt(i) as TextView
            val isSelected = chip.tag == selectedCategory
            if (isSelected) {
                chip.setBackgroundResource(R.drawable.bg_category_badge)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            } else {
                chip.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
                chip.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
