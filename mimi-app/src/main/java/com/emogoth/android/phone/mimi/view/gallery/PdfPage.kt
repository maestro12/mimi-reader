package com.emogoth.android.phone.mimi.view.gallery

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.emogoth.android.phone.mimi.viewmodel.GalleryViewModel
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.util.FitPolicy

@SuppressLint("ViewConstructor")
class PdfPage(context: Context, viewModel: GalleryViewModel) : GalleryPage(context, viewModel) {
    override fun onViewBind() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    val view = PDFView(context, null)
    init {
        addMainChildView(view)
    }

    override fun onComplete() {
        super.onComplete()

        if (!isAttachedToWindow) {
            return
        }

        view.fromFile(downloadItem.file)
                .pageFitPolicy(FitPolicy.WIDTH)
                .enableDoubletap(true)
                .onError { t -> Log.e(LOG_TAG, "Error loading PDF", t) }
                .load()

        loaded = true
    }
}