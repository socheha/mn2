package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cash_accounts")
data class CashAccountEntity(
    @PrimaryKey
    val accountType: String, // "TUNAI", "BCA", "MANDIRI", "BRI", "BNI", "BANK_LAIN", "GOPAY", "OVO", "DANA", "SHOPEEPAY", "LINKAJA", "BANK"
    val accountName: String, // "Kas Tunai Toko", "Bank BCA", "E-Wallet GoPay", etc.
    val saldo: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class AccountTypeInfo(
    val type: String,
    val name: String,
    val category: String // "TUNAI", "BANK", "E-WALLET"
)

object CashAccountDefaults {
    val ALL_DEFAULT_ACCOUNTS = listOf(
        AccountTypeInfo("TUNAI", "Kas Tunai Toko", "TUNAI"),
        AccountTypeInfo("BCA", "Bank BCA", "BANK"),
        AccountTypeInfo("MANDIRI", "Bank Mandiri", "BANK"),
        AccountTypeInfo("BRI", "Bank BRI", "BANK"),
        AccountTypeInfo("BNI", "Bank BNI", "BANK"),
        AccountTypeInfo("BANK_LAIN", "Bank Lain / Transfer", "BANK"),
        AccountTypeInfo("GOPAY", "E-Wallet GoPay", "E-WALLET"),
        AccountTypeInfo("OVO", "E-Wallet OVO", "E-WALLET"),
        AccountTypeInfo("DANA", "E-Wallet DANA", "E-WALLET"),
        AccountTypeInfo("SHOPEEPAY", "E-Wallet ShopeePay", "E-WALLET"),
        AccountTypeInfo("LINKAJA", "E-Wallet LinkAja", "E-WALLET")
    )

    fun getAccountName(type: String): String {
        return ALL_DEFAULT_ACCOUNTS.find { it.type.equals(type, ignoreCase = true) }?.name
            ?: if (type == "BANK") "Kas Bank / Transfer" else type
    }

    fun getAccountCategory(type: String): String {
        return ALL_DEFAULT_ACCOUNTS.find { it.type.equals(type, ignoreCase = true) }?.category
            ?: if (type == "TUNAI") "TUNAI" else "BANK"
    }
}
