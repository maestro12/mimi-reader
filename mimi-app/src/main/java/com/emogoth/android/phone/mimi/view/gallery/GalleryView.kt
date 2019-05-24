package com.emogoth.android.phone.mimi.view.gallery

import androidx.documentfile.provider.DocumentFile
import com.emogoth.android.phone.mimi.viewmodel.GalleryItem
import com.emogoth.android.phone.mimi.viewmodel.GalleryViewModel

interface GalleryView {
    fun shareImage()
    fun setItems(items: List<GalleryItem>)
    fun setViewModel(viewModel: GalleryViewModel)
    fun fullScreen(enabled: Boolean)
    fun fullScreenListener(listener: (Boolean) -> (Unit))
}