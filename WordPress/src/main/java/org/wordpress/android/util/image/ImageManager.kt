@file:Suppress("DEPRECATION")

package org.wordpress.android.util.image

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import android.util.Base64
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.ImageView.ScaleType.CENTER
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.ImageView.ScaleType.FIT_END
import android.widget.ImageView.ScaleType.FIT_START
import android.widget.ImageView.ScaleType.FIT_XY
import android.widget.ImageView.ScaleType.MATRIX
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.request.target.BaseTarget
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.ViewTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.WordPress
import org.wordpress.android.networking.MShot
import org.wordpress.android.ui.media.VideoLoader
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.image.ImageType.VIDEO
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton for asynchronous image fetching/loading with support for placeholders, transformations and more.
 */

@Singleton
class ImageManager @Inject constructor(
    private val placeholderManager: ImagePlaceholderManager,
    private val videoLoader: VideoLoader?
) {
    interface RequestListener<T> {
        /**
         * Called when an exception occurs during a load
         *
         * @param e The maybe {@code null} exception containing information about why the request failed.
         * @param model The model we were trying to load when the exception occurred.
         */
        fun onLoadFailed(e: Exception?, model: Any?)

        /**
         * Called when a load completes successfully
         *
         * @param resource The resource that was loaded for the target.
         * @param model The specific model that was used to load the image.
         */
        fun onResourceReady(resource: T, model: Any?)
    }

    /**
     * Return true if this [Context] is available.
     * Availability is defined as the following:
     * + [Context] is not null
     * + [Context] is not destroyed (tested with [FragmentActivity.isDestroyed] or [Activity.isDestroyed])
     */
    @Suppress("ReturnCount")
    private fun Context?.isAvailable(): Boolean {
        if (this == null) {
            return false
        } else if (this !is Application) {
            if (this is FragmentActivity) {
                return !this.isDestroyed
            } else if (this is Activity) {
                return !this.isDestroyed
            }
        }
        return true
    }

    /**
     * Loads an image from the "imgUrl" into the ImageView. Adds a placeholder and an error placeholder depending
     * on the ImageType.
     *
     * If no URL is provided, it only loads the placeholder
     */
    @JvmOverloads
    fun load(imageView: ImageView, imageType: ImageType, imgUrl: String = "", scaleType: ScaleType = CENTER) {
        val context = imageView.context
        if (!context.isAvailable()) return
        Glide.with(context)
            .load(imgUrl)
            .addFallback(imageType)
            .addPlaceholder(imageType)
            .applyScaleType(scaleType)
            .into(imageView)
            .clearOnDetach()
    }

    /**
     * Loads the first frame from the "videoUrl" as an image into the ImageView.
     * Adds a placeholder and an error placeholder depending on the ImageType.
     *
     * If no URL is provided, it only loads the placeholder
     */
    @JvmOverloads
    fun loadThumbnailFromVideoUrl(
        scope: CoroutineScope,
        imageView: ImageView,
        videoUrl: String = "",
        scaleType: ScaleType = CENTER,
        requestListener: RequestListener<Drawable>? = null
    ) {
        val context = imageView.context
        val imageType = VIDEO
        if (!context.isAvailable()) return
        videoLoader?.runIfMediaNotTooBig(scope,
            videoUrl,
            loadAction = {
                if (!context.isAvailable()) return@runIfMediaNotTooBig
                Glide.with(context)
                    .load(videoUrl)
                    .addFallback(imageType)
                    .addPlaceholder(imageType)
                    .applyScaleType(scaleType)
                    .attachRequestListener(requestListener)
                    .apply(RequestOptions().frame(0))
                    .into(imageView)
                    .clearOnDetach()
            },
            fallbackAction = {
                if (!context.isAvailable()) return@runIfMediaNotTooBig
                val fallbackDrawable = placeholderManager.getErrorResource(imageType)?.let {
                    ColorDrawable(ContextCompat.getColor(context, it))
                }
                Glide.with(context)
                    .load(fallbackDrawable)
                    .addPlaceholder(imageType)
                    .addFallback(imageType)
                    .into(imageView)
                    .clearOnDetach()
            }) ?: throw java.lang.IllegalArgumentException("Video loader has to be set")
    }

    /**
     * Loads an image from the "imgUrl" into the AppWidgetTarget. Adds a placeholder and an error placeholder depending
     * on the ImageType.
     *
     * If no URL is provided, it only loads the placeholder
     */
    @JvmOverloads
    fun load(
        awt: AppWidgetTarget,
        context: Context,
        imageType: ImageType,
        imgUrl: String = "",
        scaleType: ScaleType = CENTER,
        width: Int? = null,
        height: Int? = null
    ) {
        if (!context.isAvailable()) return
        Glide.with(context)
            .asBitmap()
            .load(imgUrl)
            .addFallback(imageType)
            .addPlaceholder(imageType)
            .applyScaleType(scaleType)
            .applySize(width, height)
            .into(awt)
    }

    /**
     * Loads an image from the "imgUrl" into the ImageView and applies circle transformation. Adds placeholder and
     * error placeholder depending on the ImageType.
     */
    @JvmOverloads
    fun loadIntoCircle(
        imageView: ImageView,
        imageType: ImageType,
        imgUrl: String,
        requestListener: RequestListener<Drawable>? = null,
        version: Int? = null
    ) {
        val context = imageView.context
        if (!context.isAvailable()) return
        Glide.with(context)
            .load(imgUrl)
            .addFallback(imageType)
            .addPlaceholder(imageType)
            .circleCrop()
            .attachRequestListener(requestListener)
            .addSignature(version)
            .into(imageView)
            .clearOnDetach()
    }

    /**
     * Loads a base64 string without prefix (data:image/png;base64,) into the ImageView and applies circle
     * transformation. Adds placeholder and error placeholder depending on the ImageType.
     */
    @JvmOverloads
    fun loadBase64IntoCircle(
        imageView: ImageView,
        imageType: ImageType,
        base64ImageData: String,
        requestListener: RequestListener<Drawable>? = null,
        version: Int? = null
    ) {
        val context = imageView.context
        if (!context.isAvailable()) return

        val imageData: ByteArray
        try {
            imageData = Base64.decode(base64ImageData, Base64.DEFAULT)
        } catch (ex: IllegalArgumentException) {
            AppLog.e(AppLog.T.UTILS, String.format("Cant parse base64 image data:" + ex.message))
            return
        }

        Glide.with(context)
            .load(imageData)
            .addFallback(imageType)
            .addPlaceholder(imageType)
            .circleCrop()
            .attachRequestListener(requestListener)
            .addSignature(version)
            .into(imageView)
            .clearOnDetach()
    }

    /**
     * Loads an image from the "imgUrl" into the ImageView with a corner radius. Adds placeholder and
     * error placeholder depending on the ImageType.
     */
    @JvmOverloads
    fun loadImageWithCorners(
        imageView: ImageView,
        imageType: ImageType,
        imgUrl: String,
        cornerRadius: Int,
        requestListener: RequestListener<Drawable>? = null
    ) {
        val context = imageView.context
        if (!context.isAvailable()) return

        Glide.with(context)
            .load(imgUrl)
            .transform(CenterCrop(), RoundedCorners(cornerRadius))
            .addFallback(imageType)
            .addPlaceholder(imageType)
            .attachRequestListener(requestListener)
            .into(imageView)
            .clearOnDetach()
    }

    /**
     * Loads an image from the "imgUrl" into the ImageView. Adds a placeholder and an error placeholder depending
     * on the ImageType. Attaches the ResultListener so the client can manually show/hide progress and error
     * views or add a PhotoViewAttacher(adds support for pinch-to-zoom gesture). Optionally adds
     * thumbnailUrl - mostly used for loading low resolution images.
     *
     * Unless you necessarily need to react on the request result, preferred way is to use one of the load(...) methods.
     */
    fun loadWithResultListener(
        imageView: ImageView,
        imageType: ImageType,
        imgUrl: String,
        scaleType: ScaleType = CENTER,
        thumbnailUrl: String? = null,
        requestListener: RequestListener<Drawable>
    ) {
        val context = imageView.context
        if (!context.isAvailable()) return
        Glide.with(context)
            .load(Uri.parse(imgUrl))
            .addFallback(imageType)
            .addPlaceholder(imageType)
            .addThumbnail(context, thumbnailUrl, requestListener)
            .applyScaleType(scaleType)
            .attachRequestListener(requestListener)
            .into(imageView)
            .clearOnDetach()
    }

    /**
     * Loads an image from the "imgUrl" into the ImageView animating it with the provided Glide animation.
     * Adds a placeholder and an error placeholder depending on the ImageType and attaches a ResultListener.
     */
    fun animateWithResultListener(
        imageView: ImageView,
        imageType: ImageType,
        imgUrl: String,
        transitionOptions: TransitionOptions<*, in Drawable>,
        requestListener: RequestListener<Drawable>
    ) {
        val context = imageView.context
        if (!context.isAvailable()) return
        Glide.with(context)
            .load(Uri.parse(imgUrl))
            .addFallback(imageType)
            .addPlaceholder(imageType)
            .applyScaleType(CENTER)
            .attachRequestListener(requestListener)
            .transition(transitionOptions)
            .into(imageView)
            .clearOnDetach()
    }

    /**
     * Preloads an image from the provided `imgUrl`.
     */
    fun preload(context: Context, imgUrl: String) {
        if (!context.isAvailable()) return
        try {
            Glide.with(context)
                .downloadOnly()
                .load(Uri.parse(imgUrl))
                .submit()
                .get() // This makes each call blocking, so subsequent calls can be cancelled if needed.
        } catch (e: ExecutionException) {
            // This is a best effort preload, so we don't want to crash the app if an `ExecutionException` is thrown.
            AppLog.e(AppLog.T.UTILS, "Error preloading image $imgUrl: $e")
        }
    }

    /**
     * Preloads an [MShot].
     *
     * This is needed because the mshot service redirects to a loading gif image when the thumbnail is not ready.
     * The loading is handled by [org.wordpress.android.networking.GlideMShotsLoader]
     */
    fun preload(context: Context, design: MShot) {
        if (!context.isAvailable()) return
        try {
            Glide.with(context)
                .downloadOnly()
                .load(design)
                .submit()
                .get() // This makes each call blocking, so subsequent calls can be cancelled if needed.
        } catch (e: ExecutionException) {
            // This is a best effort preload, so we don't want to crash the app if an `ExecutionException` is thrown.
            AppLog.e(AppLog.T.UTILS, "Error preloading MShot: $e")
        }
    }

    /**
     * Loads an [MShot] into an [ImageView] and attaches a [RequestListener].
     *
     * This is needed because the mshot service redirects to a loading gif image when the thumbnail is not ready.
     * The loading is handled by [org.wordpress.android.networking.GlideMShotsLoader]
     */
    fun loadWithResultListener(view: ImageView, design: MShot, requestListener: RequestListener<Drawable>) {
        val context = view.context
        if (!context.isAvailable()) return
        Glide.with(context)
            .load(design)
            .addFallback(ImageType.THEME)
            .addPlaceholder(ImageType.THEME)
            .applyScaleType(FIT_CENTER)
            .attachRequestListener(requestListener)
            .into(view)
            .clearOnDetach()
    }

    /**
     * Loads an image from the "imgUri" into the ImageView. Doing this allows content and remote URIs to
     * interchangeable.
     * Adds a placeholder and an error placeholder depending
     * on the ImageType. Attaches the ResultListener so the client can manually show/hide progress and error
     * views or add a PhotoViewAttacher(adds support for pinch-to-zoom gesture). Optionally adds
     * thumbnailUrl - mostly used for loading low resolution images.
     *
     * Unless you necessarily need to react on the request result, preferred way is to use one of the load(...) methods.
     */
    fun loadWithResultListener(
        imageView: ImageView,
        imageType: ImageType,
        imgUri: Uri,
        scaleType: ScaleType = CENTER,
        thumbnailUrl: String? = null,
        requestListener: RequestListener<Drawable>
    ) {
        val context = imageView.context
        if (!context.isAvailable()) return
        Glide.with(context)
            .load(imgUri)
            .addFallback(imageType)
            .addPlaceholder(imageType)
            .addThumbnail(context, thumbnailUrl, requestListener)
            .applyScaleType(scaleType)
            .attachRequestListener(requestListener)
            .into(imageView)
            .clearOnDetach()
    }

    /**
     * Loads a File either using a file path obtained from the media store (for local images),
     * or using Glide's disk cache (for remote images). Using Uri allows content and remote URIs to be interchangeable.
     *
     * We can use asFile() asynchronously on the ui thread or synchronously on a background thread.
     * This function uses the asynchronous api which takes a Target argument to invoke asFile().
     */
    fun loadIntoFileWithResultListener(
        imgUri: Uri,
        requestListener: RequestListener<File>
    ) {
        val context = WordPress.getContext()
        if (!context.isAvailable()) return
        Glide.with(context)
            .asFile()
            .load(imgUri)
            .attachRequestListener(requestListener)
            .into(
                // Used just to invoke asFile() and ignored thereafter.
                object : CustomTarget<File>() {
                    override fun onLoadCleared(placeholder: Drawable?) {}
                    override fun onResourceReady(resource: File, transition: Transition<in File>?) {}
                }
            )
    }

    /**
     * Loads the Bitmap into the ImageView.
     */
    @JvmOverloads
    fun load(
        imageView: ImageView,
        bitmap: Bitmap,
        scaleType: ScaleType = CENTER,
        requestListener: RequestListener<Drawable>? = null
    ) {
        val context = imageView.context
        if (!context.isAvailable()) return
        Glide.with(context)
            .load(bitmap)
            .applyScaleType(scaleType)
            .attachRequestListener(requestListener)
            .into(imageView)
            .clearOnDetach()
    }

    /**
     * Loads the Drawable into the ImageView.
     */
    @JvmOverloads
    fun load(imageView: ImageView, drawable: Drawable, scaleType: ScaleType = CENTER) {
        val context = imageView.context
        if (!context.isAvailable()) return
        Glide.with(context)
            .load(drawable)
            .applyScaleType(scaleType)
            .into(imageView)
            .clearOnDetach()
    }

    /**
     * Loads the DrawableResource into the ImageView.
     */
    @JvmOverloads
    fun load(imageView: ImageView, @DrawableRes resourceId: Int, scaleType: ScaleType = CENTER) {
        val context = imageView.context
        if (!context.isAvailable()) return
        Glide.with(context)
            .load(ContextCompat.getDrawable(context, resourceId))
            .applyScaleType(scaleType)
            .into(imageView)
            .clearOnDetach()
    }

    /**
     * Loads an image from the "imgUrl" into the ViewTarget. Adds a placeholder and an error placeholder depending
     * on the ImageType.
     *
     * Use this method with caution and only when you necessarily need it(in other words, don't use it
     * when you need to load an image into an ImageView).
     */
    @Suppress("DEPRECATION")
    fun loadIntoCustomTarget(viewTarget: ViewTarget<TextView, Drawable>, imageType: ImageType, imgUrl: String) {
        val context = WordPress.getContext()
        if (!context.isAvailable()) return
        Glide.with(context)
            .load(imgUrl)
            .addFallback(imageType)
            .addPlaceholder(imageType)
            .into(viewTarget)
            .clearOnDetach()
    }

    /**
     * Loads an image from the "imgUrl" into the ViewTarget.
     *
     * Use this method with caution and only when you necessarily need it(in other words, don't use it
     * when you need to load an image into an ImageView).
     */
    @Suppress("DEPRECATION")
    fun loadAsBitmapIntoCustomTarget(
        context: Context,
        target: BaseTarget<Bitmap>,
        imgUrl: String,
        scaleType: ScaleType = CENTER
    ) {
        if (!context.isAvailable()) return
        Glide.with(context)
            .asBitmap()
            .load(imgUrl)
            .applyScaleType(scaleType)
            .into(target)
    }

    /**
     * Cancel any pending requests and free any resources that may have been
     * loaded for the view.
     */
    fun cancelRequestAndClearImageView(imageView: ImageView) {
        val context = imageView.context
        if (context is Activity && (context.isFinishing || context.isDestroyed)) {
            return
        }
        Glide.with(imageView.context).clear(imageView)
    }

    /**
     * Cancel any pending requests and free any resources that may have been
     * loaded for the view.
     */
    @Suppress("DEPRECATION")
    fun <T : Any> cancelRequest(context: Context, target: BaseTarget<T>?) {
        Glide.with(context).clear(target)
    }

    private fun <T : Any> RequestBuilder<T>.applyScaleType(
        scaleType: ScaleType
    ): RequestBuilder<T> {
        return when (scaleType) {
            ScaleType.CENTER_CROP -> this.centerCrop()
            ScaleType.CENTER_INSIDE -> this.centerInside()
            FIT_CENTER -> this.fitCenter()
            CENTER -> this
            FIT_END,
            FIT_START,
            FIT_XY,
            MATRIX -> {
                AppLog.e(
                    AppLog.T.UTILS,
                    String.format(Locale.ENGLISH, "ScaleType %s is not supported.", scaleType.toString())
                )
                this
            }
        }
    }

    private fun <T : Any> RequestBuilder<T>.applySize(width: Int?, height: Int?): RequestBuilder<T> {
        return if (width != null && height != null) {
            this.override(width, height)
        } else {
            this
        }
    }

    private fun <T : Any> RequestBuilder<T>.addPlaceholder(imageType: ImageType): RequestBuilder<T> {
        val placeholderImageRes = placeholderManager.getPlaceholderResource(imageType)
        return if (placeholderImageRes == null) {
            this
        } else {
            this.placeholder(placeholderImageRes)
        }
    }

    private fun <T : Any> RequestBuilder<T>.addFallback(imageType: ImageType): RequestBuilder<T> {
        val errorImageRes = placeholderManager.getErrorResource(imageType)
        return if (errorImageRes == null) {
            this
        } else {
            this.error(errorImageRes)
        }
    }

    /**
     * Changing the signature invalidates cache.
     */
    private fun <T : Any> RequestBuilder<T>.addSignature(signature: Int?): RequestBuilder<T> {
        return if (signature == null) {
            this
        } else {
            this.signature(ObjectKey(signature))
        }
    }

    private fun RequestBuilder<Drawable>.addThumbnail(
        context: Context,
        thumbnailUrl: String?,
        listener: RequestListener<Drawable>
    ): RequestBuilder<Drawable> {
        return if (TextUtils.isEmpty(thumbnailUrl)) {
            this
        } else {
            val thumbnailRequest = Glide
                .with(context)
                .load(thumbnailUrl)
                .downsample(DownsampleStrategy.AT_MOST)
                .attachRequestListener(listener)
            return this.thumbnail(thumbnailRequest)
        }
    }

    private fun <T : Any> RequestBuilder<T>.attachRequestListener(
        requestListener: RequestListener<T>?
    ): RequestBuilder<T> {
        return if (requestListener == null) {
            this
        } else {
            this.listener(object : com.bumptech.glide.request.RequestListener<T> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<T>,
                    isFirstResource: Boolean
                ): Boolean {
                    requestListener.onLoadFailed(e, model)
                    return false
                }

                override fun onResourceReady(
                    resource: T,
                    model: Any,
                    target: Target<T>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    requestListener.onResourceReady(resource, model)
                    return false
                }
           })
        }
    }

    @Deprecated("Object for backward compatibility with code which doesn't support DI")
    companion object {
        @JvmStatic
        @Deprecated("Use injected ImageManager")
        val instance: ImageManager by lazy { ImageManager(ImagePlaceholderManager(), null) }
    }
}
