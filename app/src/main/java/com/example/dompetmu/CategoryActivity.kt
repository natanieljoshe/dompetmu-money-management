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
import java.util.UUID

class CategoryActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var btnAddCategory: MaterialButton
    private lateinit var categoryAdapter: CategoryAdapter
    private var categoryList = mutableListOf<Category>()

    private val PREF_NAME = "dompetmu_prefs"
    private val KEY_CATEGORIES = "key_categories"
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Master Kategori"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvCategories = findViewById(R.id.rvCategories)
        btnAddCategory = findViewById(R.id.btnAddCategory)

        loadCategories()

        btnAddCategory.setOnClickListener {
            showCategoryDialog(null)
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
        val json = prefs.getString(KEY_CATEGORIES, null) ?: return getDefaultCategories()
        val type = object : TypeToken<MutableList<Category>>() {}.type
        return gson.fromJson(json, type) ?: getDefaultCategories()
    }

    private fun saveCategories(categories: List<Category>) {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val json = gson.toJson(categories)
        prefs.edit().putString(KEY_CATEGORIES, json).apply()
    }

    private fun getDefaultCategories(): MutableList<Category> {
        val defaultList = mutableListOf(
            Category("1", "Gaji", "INCOME"),
            Category("2", "Bonus", "INCOME"),
            Category("3", "Makanan", "EXPENSE"),
            Category("4", "Transportasi", "EXPENSE"),
            Category("5", "Belanja", "EXPENSE")
        )
        saveCategories(defaultList)
        return defaultList
    }

    private fun loadCategories() {
        categoryList = getCategories()
        categoryAdapter = CategoryAdapter(
            categoryList,
            onEdit = { category -> showCategoryDialog(category) },
            onDelete = { category -> deleteCategory(category) }
        )
        rvCategories.layoutManager = LinearLayoutManager(this)
        rvCategories.adapter = categoryAdapter
    }

    private fun showCategoryDialog(category: Category?) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_category, null)
        builder.setView(dialogView)

        val etCategoryName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)

        val types = arrayOf("INCOME", "EXPENSE")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinnerType.adapter = adapter

        if (category != null) {
            etCategoryName.setText(category.name)
            val typeIndex = types.indexOf(category.type)
            if (typeIndex != -1) spinnerType.setSelection(typeIndex)
            builder.setTitle("Ubah Kategori")
        } else {
            builder.setTitle("Tambah Kategori")
        }

        builder.setPositiveButton("Simpan") { _, _ ->
            val name = etCategoryName.text.toString().trim()
            val type = spinnerType.selectedItem.toString()

            if (name.isEmpty()) {
                Toast.makeText(this, "Nama kategori tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (category == null) {
                val newCategory = Category(UUID.randomUUID().toString(), name, type)
                categoryList.add(newCategory)
            } else {
                val index = categoryList.indexOfFirst { it.id == category.id }
                if (index != -1) {
                    categoryList[index] = Category(category.id, name, type)
                }
            }

            saveCategories(categoryList)
            loadCategories()
            Toast.makeText(this, "Kategori berhasil disimpan", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun deleteCategory(category: Category) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Kategori")
            .setMessage("Apakah Anda yakin ingin menghapus ${category.name}?")
            .setPositiveButton("Ya") { _, _ ->
                categoryList.remove(category)
                saveCategories(categoryList)
                loadCategories()
                Toast.makeText(this, "Kategori dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    inner class CategoryAdapter(
        private val list: List<Category>,
        private val onEdit: (Category) -> Unit,
        private val onDelete: (Category) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvCategoryName)
            val tvType: TextView = view.findViewById(R.id.tvCategoryType)
            val btnEdit: ImageButton = view.findViewById(R.id.btnEditCategory)
            val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteCategory)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
            return CategoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val category = list[position]
            holder.tvName.text = category.name
            holder.tvType.text = "Tipe: ${category.type}"

            holder.btnEdit.setOnClickListener { onEdit(category) }
            holder.btnDelete.setOnClickListener { onDelete(category) }
        }

        override fun getItemCount() = list.size
    }
}
