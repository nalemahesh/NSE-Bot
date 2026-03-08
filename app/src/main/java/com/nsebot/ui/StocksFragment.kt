package com.nsebot.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.nsebot.databinding.FragmentStocksBinding
import com.nsebot.viewmodel.StockViewModel

class StocksFragment : Fragment() {

    private var _binding: FragmentStocksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StockViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStocksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadFnOStocks()
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.stocks.observe(viewLifecycleOwner) { stocks ->
            binding.tvEmpty.visibility = if (stocks.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerStocks.visibility = if (stocks.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.loadFnOStocks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
