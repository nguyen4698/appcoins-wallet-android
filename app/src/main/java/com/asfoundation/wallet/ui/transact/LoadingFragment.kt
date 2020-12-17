package com.asfoundation.wallet.ui.transact

import androidx.fragment.app.Fragment
import com.asf.wallet.R

class LoadingFragment : Fragment(R.layout.transact_loading_view) {
  companion object {
    fun newInstance(): Fragment {
      return LoadingFragment()
    }
  }
}
