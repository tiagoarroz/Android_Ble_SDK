package com.timaimee.vpdemo.activity

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inuker.bluetooth.library.Code
import com.timaimee.vpdemo.R
import com.timaimee.vpdemo.adapter.TCMAdapter
import com.timaimee.vpdemo.adapter.TCMItem
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.listener.base.IBleWriteResponse
import com.veepoo.protocol.listener.data.ITCMDataListener
import com.veepoo.protocol.model.datas.TCMDataReport
import com.veepoo.protocol.model.enums.TCMType

class TCMActivity : AppCompatActivity(), ITCMDataListener {

    private lateinit var tcmAdapter: TCMAdapter
    private val tcmItems = TCMType.values().map { TCMItem(it) }

    //  SDK  mVpOperate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tcm)

        // 
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        tcmAdapter = TCMAdapter(tcmItems)
        recyclerView.adapter = tcmAdapter

        // Ler
        findViewById<Button>(R.id.btn_read).setOnClickListener {
            readTcmData()
        }

        // Enviar
        findViewById<Button>(R.id.btn_send).setOnClickListener {
            sendTcmData()
        }
    }

    /**
     * Enviardados TCM
     */
    private fun sendTcmData() {
        // dados Map<TCMType, Int>
        val selectedData = tcmItems.filter { it.isSelected }
            .associate { it.type to it.value }

        if (selectedData.isEmpty()) {
            Toast.makeText(this, "dados", Toast.LENGTH_SHORT).show()
            return
        }

        VPOperateManager.getInstance().setJE136PTCMCustomData({ code ->
            if (code == Code.REQUEST_SUCCESS) {
                Log.d("TCM", "Enviar")
            }
        }, selectedData, this)
    }

    /**
     * Lerdispositivodados TCM
     */
    private fun readTcmData() {
        VPOperateManager.getInstance().readJE136PTCMCustomData({
            // 
        }, this)
    }

    // --- ITCMDataListener  ---

    override fun onTCMDataResponse(report: TCMDataReport) {
        runOnUiThread {
            Log.d("TCM", "dispositivo，Timestamp: ${report.timestamp}")
            //  report Atualizar UI
            report.metrics.forEach { (type, value) ->
                val item = tcmItems.find { it.type == type }
                item?.let {
                    it.value = value
                    it.isSelected = true
                }
            }
            tcmAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Ler", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onTCMWriteSuccess() {
        runOnUiThread {
            Toast.makeText(this, "dispositivodados TCM", Toast.LENGTH_SHORT).show()
        }
    }
}