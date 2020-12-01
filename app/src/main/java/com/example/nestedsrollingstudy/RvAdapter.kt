package com.example.nestedsrollingstudy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.nested_rv_item.view.*

/**
 * Created by dumingwei on 2020/12/1.
 *
 * Desc:
 */
class RvAdapter(val mData: MutableList<String>) : RecyclerView.Adapter<RvAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.nested_rv_item, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        with(holder.itemView) {
            tv_id_info.text = mData[position]
        }
    }


    class VH(mView: View) : RecyclerView.ViewHolder(mView)

}