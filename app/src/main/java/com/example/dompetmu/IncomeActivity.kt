package com.example.dompetmu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class IncomeActivity : AppCompatActivity() {

    private lateinit var rvIncome: RecyclerView
    private lateinit var btnAddIncome: MaterialButton
    private lateinit var incomeAdapter: IncomeAdapter
    private var incomeList = mutableListOf<TransactionItem>()

    private val PREF_NAME = "dompetmu_prefs"
    private val KEY_CATEGORIES = "key_categories"
    private val KEY_TRANSACTIONS = "key_transactions"
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Pemasukan"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvIncome = findViewById(R.id.rvIncome)
        btnAddIncome = findViewById(R.id.btnAddIncome)

        loadIncomes()

        btnAddIncome.setOnClickListener {
            showIncomeDialog(null)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getCategories(): MutableList<Category> {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val json = prefs.getString(KEY_CATEGORIES, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Category>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    private fun getTransactions(): MutableList<TransactionItem> {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val json = prefs.getString(KEY_TRANSACTIONS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<TransactionItem>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    private fun saveTransactions(transactions: List<TransactionItem>) {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val json = gson.toJson(transactions)
        prefs.edit().putString(KEY_TRANSACTIONS, json).apply()
    }

    private fun addTransaction(item: TransactionItem) {
        val list = getTransactions()
        list.add(item)
        saveTransactions(list)
    }

    private fun updateTransaction(item: TransactionItem) {
        val list = getTransactions()
        val index = list.indexOfFirst { it.id == item.id }
        if (index != -1) {
            list[index] = item
            saveTransactions(list)
        }
    }

    private fun deleteTransaction(id: String) {
        val list = getTransactions()
        list.removeAll { it.id == id }
        saveTransactions(list)
    }

    private fun loadIncomes() {
        val allTransactions = getTransactions()
        incomeList = allTransactions.filter { it.type == "INCOME" }.toMutableList()

        incomeAdapter = IncomeAdapter(
            incomeList,
            onEdit = { item -> showIncomeDialog(item) },
            onDelete = { item -> deleteIncome(item) }
        )
        rvIncome.layoutManager = LinearLayoutManager(this)
        rvIncome.adapter = incomeAdapter
    }

    private fun showIncomeDialog(item: TransactionItem?) {
        val categories = getCategories().filter { it.type == "INCOME" }
        if (categories.isEmpty()) {
            Toast.makeText(this, "Buat kategori Pemasukan terlebih dahulu!", Toast.LENGTH_LONG).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_transaction, null)
        builder.setView(dialogView)

        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etNote = dialogView.findViewById<EditText>(R.id.etNote)

        val categoryNames = categories.map { it.name }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryNames)
        spinnerCategory.adapter = spinnerAdapter

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        etDate.setText(currentDate)

        if (item != null) {
            etAmount.setText(item.amount.toString())
            etDate.setText(item.date)
            etNote.setText(item.note)
            val catIndex = categories.indexOfFirst { it.id == item.categoryId }
            if (catIndex != -1) spinnerCategory.setSelection(catIndex)
            builder.setTitle("Ubah Pemasukan")
        } else {
            builder.setTitle("Tambah Pemasukan")
        }

        builder.setPositiveButton("Simpan") { _, _ ->
            val amountStr = etAmount.text.toString().trim()
            val date = etDate.text.toString().trim()
            val note = etNote.text.toString().trim()

            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Jumlah tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val amount = amountStr.toDoubleOrNull() ?: 0.0
            val selectedCategory = categories[spinnerCategory.selectedItemPosition]

            if (item == null) {
                val newItem = TransactionItem(
                    id = UUID.randomUUID().toString(),
                    categoryId = selectedCategory.id,
                    categoryName = selectedCategory.name,
                    amount = amount,
                    date = date,
                    note = note,
                    type = "INCOME"
                )
                addTransaction(newItem)
            } else {
                val updatedItem = TransactionItem(
                    id = item.id,
                    categoryId = selectedCategory.id,
                    categoryName = selectedCategory.name,
                    amount = amount,
                    date = date,
                    note = note,
                    type = "INCOME"
                )
                updateTransaction(updatedItem)
            }

            loadIncomes()
            Toast.makeText(this, "Pemasukan berhasil disimpan", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun deleteIncome(item: TransactionItem) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Pemasukan")
            .setMessage("Apakah Anda yakin ingin menghapus pemasukan ini?")
            .setPositiveButton("Ya") { _, _ ->
                deleteTransaction(item.id)
                loadIncomes()
                Toast.makeText(this, "Pemasukan dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    inner class IncomeAdapter(
        private val list: List<TransactionItem>,
        private val onEdit: (TransactionItem) -> Unit,
        private val onDelete: (TransactionItem) -> Unit
    ) : RecyclerView.Adapter<IncomeAdapter.IncomeViewHolder>() {

        inner class IncomeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val tvNote: TextView = view.findViewById(R.id.tvNote)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvAmount: TextView = view.findViewById(R.id.tvAmount)
            val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
            return IncomeViewHolder(view)
        }

        override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
            val item = list[position]
            holder.tvCategory.text = item.categoryName
            holder.tvNote.text = item.note.ifEmpty { "-" }
            holder.tvDate.text = item.date
            holder.tvAmount.text = "+ Rp ${item.amount}"
            holder.tvAmount.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))

            holder.btnEdit.setOnClickListener { onEdit(item) }
            holder.btnDelete.setOnClickListener { onDelete(item) }
        }

        override fun getItemCount() = list.size
    }
}
