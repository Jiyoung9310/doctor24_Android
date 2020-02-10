package com.nexters.doctor24.todoc.ui.detailed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nexters.doctor24.todoc.R
import com.nexters.doctor24.todoc.base.BaseFragment
import com.nexters.doctor24.todoc.databinding.DetailedFragmentBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

internal class DetailedFragment: BaseFragment<DetailedFragmentBinding, DetailedViewModel>(){

    override val layoutResId: Int
        get() = R.layout.detailed_fragment
    override val viewModel: DetailedViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.detailed_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.reqDetailedInfo("hospital","A1119764")

        binding.apply {
            vm = viewModel
        }
    }

}
