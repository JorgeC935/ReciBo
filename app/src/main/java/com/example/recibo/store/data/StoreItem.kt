package com.example.recibo.store.data

import com.google.firebase.Timestamp

data class StoreItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Int = 0, // En puntos
    val category: String = "",
    val imageUrl: String = "",
    val isActive: Boolean = true,
    val stock: Int = -1, // -1 = stock ilimitado
    val createdAt: Timestamp = Timestamp.now()
)