package com.example.nestedsrollingstudy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_nested_scroll_rv_one.*
import java.util.*

/**
 * Crete by dumingwei on 2020-02-15
 * Desc: StickyNavLayout嵌套RecyclerView
 *
 */
class NestedScrollRVOneActivity : AppCompatActivity() {

    private val mDatas: MutableList<String> = ArrayList()
    private val mTitle: String = "Defaut Value"

    companion object {

        fun launch(context: Context) {
            val intent = Intent(context, NestedScrollRVOneActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nested_scroll_rv_one)
        initDatas()
    }

    private fun initDatas() {
        for (i in 0..49) {
            mDatas.add("$mTitle -> $i")
        }
        nestedScrollContent.layoutManager = LinearLayoutManager(this)
        val adapter = RvAdapter(mDatas)
        nestedScrollContent.adapter = adapter
    }

}
