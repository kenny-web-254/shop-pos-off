package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.repository.PosRepository

interface AppContainer {
    val database: AppDatabase
    val repository: PosRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val repository: PosRepository by lazy {
        PosRepository(database)
    }
}
