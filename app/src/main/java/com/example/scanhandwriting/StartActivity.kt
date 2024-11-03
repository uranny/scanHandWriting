package com.example.scanhandwriting

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.SpinnerAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.scanhandwriting.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    private lateinit var myAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 리소스 배열을 onCreate에서 초기화
        val items = resources.getStringArray(R.array.spinner_array)
        myAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)

        // 스피너 설정
        setupSpinner(binding.startLangSpinner, false)
        setupSpinner(binding.targetLangSpinner, true)
        binding.targetLangSpinner.setSelection(1)

        binding.startBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupSpinner(spinner: Spinner, isTarget: Boolean) {
        spinner.adapter = myAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateSpinnerData(isTarget, position)
                Log.d("spinner", "${position}")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 아무 항목도 선택되지 않았을 때 호출되는 부분입니다.
            }
        }
    }

    private fun updateSpinnerData(isTarget: Boolean, position: Int) {
        if (isTarget) {
            SpinnerData.targetRang = position
            Log.d("selectSpinner", "${SpinnerData.targetRang}")
        } else {
            SpinnerData.startRang = position
            Log.d("selectSpinner", "${SpinnerData.startRang}")
        }
    }
}
