package com.example.nestedsrollingstudy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_tab.*
import java.util.*

class TabFragment : Fragment() {

    private var mTitle: String? = "Defaut Value"

    private val mDatas: MutableList<String> = ArrayList()

    companion object {
        const val TITLE = "title"
        fun newInstance(title: String?): TabFragment {
            val tabFragment = TabFragment()
            val bundle = Bundle()
            bundle.putString(TITLE, title)
            tabFragment.arguments = bundle
            return tabFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mTitle = arguments!!.getString(TITLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        for (i in 0..49) {
            mDatas.add("$mTitle -> $i")
        }
        rv_in_stickynavlayout.layoutManager = LinearLayoutManager(activity)
        val adapter = RvAdapter(mDatas)
        rv_in_stickynavlayout.adapter = adapter
    }

}