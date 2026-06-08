package com.timaimee.vpdemo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.timaimee.vpdemo.R
import com.veepoo.protocol.model.enums.TCMType

class TCMAdapter(private val items: List<TCMItem>) : RecyclerView.Adapter<TCMAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTv: TextView = view.findViewById(R.id.tv_name)
        val valueTv: TextView = view.findViewById(R.id.tv_value)
        val selectSwitch: Switch = view.findViewById(R.id.switch_select)
        val btnAdd: Button = view.findViewById(R.id.btn_add)
        val btnSub: Button = view.findViewById(R.id.btn_sub)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tcm_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // 1. ，
        holder.selectSwitch.setOnCheckedChangeListener(null)

        // 2. Configurarestado
        holder.nameTv.text = item.type.description
        holder.valueTv.text = item.value.toString()
        holder.selectSwitch.isChecked = item.isSelected

        // 3. Configurar
        holder.selectSwitch.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
        }

        // 4. （，）
        holder.btnAdd.setOnClickListener {
            item.value++
            holder.valueTv.text = item.value.toString()
        }

        holder.btnSub.setOnClickListener {
            if (item.value > 0) {
                item.value--
                holder.valueTv.text = item.value.toString()
            }
        }
    }

    override fun getItemCount() = items.size
}

data class TCMItem(
    val type: TCMType,
    var isSelected: Boolean = false,
    var value: Int = 50 // 
)