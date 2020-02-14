package com.nexters.doctor24.todoc.ui.map

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
import com.nexters.doctor24.todoc.R
import com.nexters.doctor24.todoc.api.error.ErrorHandler
import com.nexters.doctor24.todoc.base.*
import com.nexters.doctor24.todoc.data.map.response.ResMapAddress
import com.nexters.doctor24.todoc.data.marker.MarkerTypeEnum
import com.nexters.doctor24.todoc.data.marker.response.ResMapMarker
import com.nexters.doctor24.todoc.databinding.NavermapFragmentBinding
import com.nexters.doctor24.todoc.ui.map.category.CategoryAdapter
import com.nexters.doctor24.todoc.ui.map.category.CategoryViewModel
import com.nexters.doctor24.todoc.ui.map.category.categoryItemList
import com.nexters.doctor24.todoc.ui.map.marker.MapMarkerManager
import com.nexters.doctor24.todoc.ui.map.marker.group.GroupMarkerListDialog
import com.nexters.doctor24.todoc.ui.map.preview.PreviewFragment
import com.nexters.doctor24.todoc.ui.map.preview.PreviewViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber


internal class NaverMapFragment : BaseFragment<NavermapFragmentBinding, NaverMapViewModel>(),
    OnMapReadyCallback, MapMarkerManager.MarkerClickListener, CategoryAdapter.CategoryListener {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        private const val LAYOUT_SPAN_COUNT = 5
    }

    private lateinit var locationSource: FusedLocationSource
    override val layoutResId: Int
        get() = R.layout.navermap_fragment
    override val viewModel: NaverMapViewModel by viewModel()
    val viewModelTime: TimeViewModel by viewModel()
    private val categoryViewModel : CategoryViewModel by viewModel()
    private val previewViewModel : PreviewViewModel by viewModel()

    private lateinit var naverMap: NaverMap
    private lateinit var markerManager: MapMarkerManager
    private val categoryAdapter: CategoryAdapter by lazy { CategoryAdapter() }
    private val bottomSheetCategory: BottomSheetDialog by lazy {
        BottomSheetDialog(context!!, R.style.PreviewBottomSheetDialog)
    }

    private var isSelected = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            vm = viewModel
            vmTime = viewModelTime
        }

        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)

        binding.mapView.apply {
            onCreate(savedInstanceState)
            getMapAsync(this@NaverMapFragment)
        }

        initView()
        initObserve()
        setBottomSheet()
    }

    private fun setBottomSheet() {

        val bottomSheetBehavior = BottomSheetBehavior.from<View>(binding.mapBottom)
        var bgShape: GradientDrawable = binding.linearLayout.background as GradientDrawable

        bgShape.setColor(Color.WHITE)

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int){
                when(newState) {
                    BottomSheetBehavior.STATE_EXPANDED ->{
                        viewModelTime.setBottomSheetState(newState)
                    }
                    BottomSheetBehavior.STATE_COLLAPSED ->{
                        viewModelTime.setBottomSheetState(newState)
                    }
                }
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    bgShape.setColor(Color.WHITE)
                } else {
                    bgShape.setColor(Color.argb((100*2.55).toInt(),239, 242, 248))
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset > 0 && slideOffset < 1) {
                    bgShape.setColor(Color.argb((100*2.55).toInt(),239, 242, 248))
                    bgShape.alpha = floatToAlpha(slideOffset)
                }

                if(slideOffset == 0.toFloat())
                    bgShape.setColor(Color.WHITE)
            }
        })
    }

    private fun floatToAlpha(float : Float) : Int {
        return (((float * 100).toInt()) * 2.55).toInt()
    }

    private fun initView() {
        val markerTypes = MarkerTypeEnum.values()
        binding.tab.apply {
            markerTypes.forEach {
                val tabView =
                    LayoutInflater.from(context).inflate(R.layout.item_tab, null) as TextView
                tabView.text = it.title
                tabView.setCompoundDrawablesRelativeWithIntrinsicBounds(it.icon, 0, 0, 0)
                addTab(newTab().setCustomView(tabView))
            }
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    tab?.let {
                        val view = it.customView as TextView
                        view.setTextColor(resources.getColor(R.color.white))
                        viewModel.onChangeTab(markerTypes[it.position])
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    tab?.let {
                        val view = it.customView as TextView
                        view.setTextColor(resources.getColor(R.color.grey_2))
                    }
                }

                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let {
                        val view = it.customView as TextView
                        view.setTextColor(resources.getColor(R.color.white))
                        viewModel.onChangeTab(markerTypes[it.position])
                    }
                }
            })
        }

        initCategoryView()
    }

    private fun initCategoryView() {
        val view = layoutInflater.inflate(R.layout.bottom_category_dialog, null)
        val categoryView = view.findViewById<RecyclerView>(R.id.recycler_view_category)
        val refresh = view.findViewById<TextView>(R.id.text_category_reset)
        val btnClose = view.findViewById<ImageView>(R.id.button_close)
        categoryView.apply {
            adapter = categoryAdapter.apply {
                listener = this@NaverMapFragment
                submitList(categoryItemList)
            }
            layoutManager = GridLayoutManager(context, LAYOUT_SPAN_COUNT)
        }
        refresh.setOnClickListener { categoryViewModel.onClickRefresh() }
        btnClose.setOnClickListener { categoryViewModel.onClickClose() }
        bottomSheetCategory.setContentView(view)
    }

    private fun initObserve() {
        viewModel.hospitalMarkerDatas.observe(viewLifecycleOwner, EventObserver {
            if (it.isEmpty()) {
                Toast.makeText(context, "현재 운영중인 병원이 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                markerManager.setMarker(it)
            }
        })

        viewModel.tabChangeEvent.observe(viewLifecycleOwner, Observer {
            viewModel.reqMarker(naverMap.cameraPosition.target, naverMap.cameraPosition.zoom)
        })

        viewModel.currentLocation.observe(viewLifecycleOwner, Observer {
            showRefresh()
        })

        viewModel.currentZoom.observe(viewLifecycleOwner, Observer {
            showRefresh()
        })

        viewModel.categoryEvent.observe(viewLifecycleOwner, Observer {
            bottomSheetCategory.show()
        })

        viewModel.previewCloseEvent.observe(viewLifecycleOwner, Observer {
            deSelectMarker()
        })

        viewModel.refreshEvent.observe(viewLifecycleOwner, Observer {
            binding.btnRefresh.isVisible = false
        })

        viewModel.currentCategory.observe(viewLifecycleOwner, Observer {

            if(it == null || it.isEmpty()) {
                if(categoryAdapter.childViewList.isNotEmpty()){
                    categoryAdapter.childViewList[0].isChecked = true
                }
            }
        })

        categoryViewModel.currentSelectItem.observe(viewLifecycleOwner, Observer {
            viewModel.reqMarkerWithCategory(it)
            bottomSheetCategory.dismiss()
        })

        categoryViewModel.refreshEvent.observe(viewLifecycleOwner, Observer {
            categoryAdapter.childViewList[0].isChecked = true
        })

        categoryViewModel.categoryCloseEvent.observe(viewLifecycleOwner, Observer {
            bottomSheetCategory.dismiss()
        })
    }

    private fun showRefresh() {
        if(!binding.btnRefresh.isVisible && !isSelected) {
            deSelectMarker()
            binding.btnRefresh.apply {
                isVisible = true
                startAnimation((AnimationUtils.loadAnimation(context, R.anim.anim_slide_in_down)))
            }
        }
    }

    override fun onClickCategory(category: String) {
        categoryViewModel.onSelectCategory(category)
    }

    override fun markerClick(marker: Marker) {
        deSelectMarker()
        marker.tag?.let{
            if((it as ArrayList<ResMapMarker>).isNotEmpty()) {
                val medicalData = Bundle().apply {
                    putParcelable(PreviewFragment.KEY_MEDICAL, it[0])
                    viewModel.currentMyLocation?.let { loc->
                        Timber.d("MapApps - $loc")
                        putDoubleArray(PreviewFragment.KEY_MY_LOCATION, doubleArrayOf(loc.latitude, loc.longitude))
                    }
                }
                PreviewFragment().apply {
                    setStyle(DialogFragment.STYLE_NORMAL, R.style.PreviewBottomSheetDialog)
                    arguments = medicalData
                }.show(childFragmentManager, PreviewFragment.TAG)
            }
        }
        markerManager.getMarkerItem(marker)?.run {
            if (markerManager.isEqualsSelectMarker(this)) return
            selectMarker(this)
        }
        moveMarkerBoundary(marker)
    }

    override fun markerBundleClick(marker: Marker) {
        deSelectMarker()

        Timber.d("marker tag : ${marker.tag}")
        marker.tag?.let {
            if ((it as ArrayList<ResMapMarker>).isNotEmpty()) {
                val groupData = Bundle().apply {
                    putParcelableArrayList(GroupMarkerListDialog.KEY_LIST, it)
                }
                GroupMarkerListDialog().apply {
                    arguments = groupData
                }.show(childFragmentManager, GroupMarkerListDialog.TAG)
            }

        }
        moveMarkerBoundary(marker)

    }

    private fun moveMarkerBoundary(marker: Marker) {
        val cameraUpdate = CameraUpdate.scrollTo(marker.position).animate(CameraAnimation.Easing)
        naverMap.setContentPadding(0, 0, 0, 550)
        naverMap.moveCamera(cameraUpdate)
    }

    private fun selectMarker(markerItem: MarkerUIData?) {
        markerItem?.run {
            binding.btnRefresh.isVisible = false
            isSelected = true
            markerManager.selectMarker(this)
            /*when {
                mapViewModel.currentState == ListState.Default -> mapViewModel.setListState(isMarkerClick = true)
            }

            geoJson?.run { mapViewModel.requestGeoAndListInfo(this) }

            val currentZoom = gMap?.cameraPosition?.zoom?.toInt() ?: 13
            val listType = getBottomCheckedType(this, mapListLayout.currentChecked())
            val searchTitle = complexName?.let { it } ?: name ?: ""

            if (!searchTitle.isNullOrEmpty()) mainSearchViewModel.setFilterSearchDesc(searchTitle)

            mapListLayout.setRdButton(listType)
            requestListAPI(listType, this, 1, currentZoom)

            if (!UserInfo.getInstance().filterMgr.isAptType) {
                mapViewModel.saveClusterClickHistory(this, currentZoom)
            }*/
        }
    }

    private fun deSelectMarker() {
        markerManager.deSelectMarker()
        naverMap.setContentPadding(0, 0, 0, 0)
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map
        map.uiSettings.apply {
            isCompassEnabled = false
            isRotateGesturesEnabled = false
            isZoomControlEnabled = false
            isLocationButtonEnabled = false
            isTiltGesturesEnabled = false
        }
        map.apply {
            locationSource = this@NaverMapFragment.locationSource
            locationTrackingMode = LocationTrackingMode.Follow
            isNightModeEnabled = true
            setBackgroundResource(NaverMap.DEFAULT_BACKGROUND_DRWABLE_DARK)
            mapType = NaverMap.MapType.Navi
            minZoom = 12.0
            maxZoom = 17.0
        }

        binding.tab.getTabAt(0)?.select()
        binding.buttonLocation.apply {
            setBackgroundResource(R.drawable.ic_current_location)
            this.map = map
        }
        markerManager =
            MapMarkerManager(context!!, naverMap).apply { listener = this@NaverMapFragment }

        map.setOnMapClickListener { pointF, latLng -> deSelectMarker() }
        map.addOnCameraIdleListener {
            viewModel.onChangedLocation(map.cameraPosition.target)
            viewModel.onChangedZoom(map.cameraPosition.zoom)
        }
        map.addOnLocationChangeListener {
            viewModel.onChangedMyLocation(it)
        }
    }

    private fun handleResponse(result: Result<ResMapAddress>) {
        when (result) {
            //comment this Success check if you are observing data from DB
            is Success<ResMapAddress> -> {
                result.data.addressData?.get(0)?.region?.area2?.areaName?.let {
                    Toast.makeText(context, "현재 구 주소 : $it", Toast.LENGTH_SHORT).show()
                }
            }
            is Error -> {
                view?.let { view ->
                    ErrorHandler.handleError(
                        view,
                        result
                    )
                }
            }
            is Progress -> {
                binding.progressBar.isVisible = result.isLoading
            }
        }
    }

}