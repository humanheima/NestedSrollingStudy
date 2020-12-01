package com.example.nestedsrollingstudy

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onClick(view: View) {

        when (view.id) {
            R.id.btnNestedRv_1 -> {
                NestedScrollRVOneActivity.launch(this)
            }
            R.id.btnNestedRv_2 -> {
                NestedScrollRVTwoActivity.launch(this)
            }
            R.id.btnNestedScroll_1 -> {
                NestedScrollVersionOneActivity.launch(this)
            }
            R.id.btnNestedScroll_2 -> {
                NestedScrollVersion2Activity.launch(this)

            }
        }

    }
}