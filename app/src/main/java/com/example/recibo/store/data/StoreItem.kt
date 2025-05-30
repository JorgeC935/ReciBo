package com.example.recibo.store.data

data class StoreItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Int = 0,
    val imageUrl: String = "",
    val category: String = "",
    val isAvailable: Boolean = true
) {
    // Constructor vacío requerido por Firestore
    constructor() : this("", "", "", 0, "", "", true)
}