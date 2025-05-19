package com.example.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ItemProductBinding

// Data class for SellerProduct
// Renamed from Product to avoid conflict with Product.java

data class SellerProduct(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val stock: Int = 0,
    val imageUrl: String = "",
    val status: String = ""
)

class ProductAdapter(private val products: List<SellerProduct>, private val isSeller: Boolean = false) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    interface OnEditProductListener {
        fun onEditProduct(product: SellerProduct)
    }
    private var editListener: OnEditProductListener? = null
    fun setOnEditProductListener(listener: OnEditProductListener) {
        this.editListener = listener
    }

    inner class ProductViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.binding.txtProductName.text = product.name
        holder.binding.txtProductPrice.text = "₱${product.price}"
        holder.binding.textProductStock.text = "Stock: ${product.stock}"
        Glide.with(holder.itemView.context)
            .load(product.imageUrl)
            .placeholder(R.drawable.placeholder_image)
            .into(holder.binding.imgProduct)
        if (isSeller) {
            holder.binding.btnEditProduct.visibility = android.view.View.VISIBLE
            holder.binding.btnEditProduct.setOnClickListener {
                editListener?.onEditProduct(product)
            }
        } else {
            holder.binding.btnEditProduct.visibility = android.view.View.GONE
        }
    }

    override fun getItemCount() = products.size
} 