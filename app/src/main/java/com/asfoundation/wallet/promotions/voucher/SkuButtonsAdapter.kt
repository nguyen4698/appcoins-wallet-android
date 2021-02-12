package com.asfoundation.wallet.promotions.voucher

import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Button
import android.widget.GridView
import androidx.recyclerview.widget.RecyclerView
import com.asf.wallet.R
import rx.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicReference


class SkuButtonsAdapter(
    val context: Context,
    val buttonModels: List<SkuButtonModel>,
    val onSkuClick: PublishSubject<Any>
) :
    RecyclerView.Adapter<SkuButtonsViewHolder>() {

  private var inflater: LayoutInflater
  private var activatedButton: AtomicReference<Button?> = AtomicReference()

  init {
    inflater = LayoutInflater.from(context)
  }

  interface OnClick {
    fun onClick()
  }

  override fun onBindViewHolder(holder: SkuButtonsViewHolder, position: Int) {
    holder.bind(buttonModels.get(position), activatedButton, onSkuClick)
  }

  override fun getItemCount(): Int {
    return buttonModels.size
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkuButtonsViewHolder {
    val button: Button =
        inflater.inflate(R.layout.e_voucher_details_diamonds_button, null) as Button
    val factor: Float =
        parent?.getContext()
            ?.getResources()
            ?.getDisplayMetrics()?.density ?: 0.0.toFloat()
    button.layoutParams = AbsListView.LayoutParams(GridView.AUTO_FIT, (factor * 48).toInt())

    return SkuButtonsViewHolder(button)
  }
}

class SkuButtonsViewHolder(private val button: Button) : RecyclerView.ViewHolder(button) {
  fun bind(skuButtonModel: SkuButtonModel, activatedButton: AtomicReference<Button?>,
           onSkuClick: PublishSubject<Any>) {
    button.text = skuButtonModel.title

    button.setOnClickListener {
      if (activatedButton == null) {
        activatedButton.set(button)
      } else {
        activatedButton.get()
            ?.setActivated(false)
        activatedButton.set(button)
      }

      onSkuClick.onNext(0)
      button.setActivated(true)
    }

  }
}

class MarginItemDecoration(private val spaceSize: Int) : RecyclerView.ItemDecoration() {
  override fun getItemOffsets(
      outRect: Rect, view: View,
      parent: RecyclerView,
      state: RecyclerView.State
  ) {
    with(outRect) {
      top = spaceSize
      left = spaceSize
      right = spaceSize
      bottom = spaceSize
    }
  }
}