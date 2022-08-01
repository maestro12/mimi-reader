package com.emogoth.android.phone.mimi.viewmodel

import androidx.lifecycle.ViewModel
import com.mimireader.chanlib.models.ChanCatalog
import io.reactivex.Single

class CatalogViewModel : ViewModel() {
    private val dataSource = ChanDataSource()

    fun fetchCatalog(boardName: String): Single<ChanCatalog> {
        return dataSource.fetchCatalog(boardName)
    }
}