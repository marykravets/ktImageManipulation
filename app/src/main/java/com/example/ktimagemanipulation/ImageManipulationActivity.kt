package com.example.ktimagemanipulation


import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.widget.Toolbar
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.LoaderCallbackInterface.SUCCESS
import org.opencv.android.OpenCVLoader
import org.opencv.android.OpenCVLoader.OPENCV_VERSION_3_0_0
import org.opencv.core.*
import org.opencv.core.CvType.CV_8U
import org.opencv.imgproc.Imgproc.*

class ImageManipulationActivity : CameraActivity(), CvCameraViewListener2 {
    private var itemPreviewRGBA: MenuItem? = null
    private var itemPreviewHist: MenuItem? = null
    private var itemPreviewCanny: MenuItem? = null
    private var itemPreviewSepia: MenuItem? = null
    private var itemPreviewSobel: MenuItem? = null
    private var itemPreviewZoom: MenuItem? = null
    private var itemPreviewPixelize: MenuItem? = null
    private var itemPreviewPosterize: MenuItem? = null
    private var openCvCameraView: CameraBridgeViewBase? = null

    private lateinit var intermediateMat: Mat
    private lateinit var sepiaKernel: Mat
    private lateinit var ranges: MatOfFloat
    private lateinit var white: Scalar
    private lateinit var mat0: Mat
    private lateinit var histSize: MatOfInt
    private lateinit var channels: Array<MatOfInt>

    private val p1: Point = Point()
    private val p2: Point = Point()
    private val size0: Size = Size()
    private val histSizeNum = 25
    private val colorsRGB: Array<Scalar> = arrayOf(
        Scalar(200.0, 0.0, 0.0, 255.0),
        Scalar(0.0, 200.0, 0.0, 255.0),
        Scalar(0.0, 0.0, 200.0, 255.0)
    )
    private val colorsHue: Array<Scalar> = arrayOf(
        Scalar(255.0, 0.0, 0.0, 255.0),
        Scalar(255.0, 60.0, 0.0, 255.0),
        Scalar(255.0, 120.0, 0.0, 255.0),
        Scalar(255.0, 180.0, 0.0, 255.0),
        Scalar(255.0, 240.0, 0.0, 255.0),
        Scalar(215.0, 213.0, 0.0, 255.0),
        Scalar(150.0, 255.0, 0.0, 255.0),
        Scalar(85.0, 255.0, 0.0, 255.0),
        Scalar(20.0, 255.0, 0.0, 255.0),
        Scalar(0.0, 255.0, 30.0, 255.0),
        Scalar(0.0, 255.0, 85.0, 255.0),
        Scalar(0.0, 255.0, 150.0, 255.0),
        Scalar(0.0, 255.0, 215.0, 255.0),
        Scalar(0.0, 234.0, 255.0, 255.0),
        Scalar(0.0, 170.0, 255.0, 255.0),
        Scalar(0.0, 120.0, 255.0, 255.0),
        Scalar(0.0, 60.0, 255.0, 255.0),
        Scalar(0.0, 0.0, 255.0, 255.0),
        Scalar(64.0, 0.0, 255.0, 255.0),
        Scalar(120.0, 0.0, 255.0, 255.0),
        Scalar(180.0, 0.0, 255.0, 255.0),
        Scalar(255.0, 0.0, 255.0, 255.0),
        Scalar(255.0, 0.0, 215.0, 255.0),
        Scalar(255.0, 0.0, 85.0, 255.0),
        Scalar(255.0, 0.0, 0.0, 255.0)
    )
    private val buff: FloatArray = FloatArray(histSizeNum)

    private val loaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            if (status == SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully")
                openCvCameraView?.enableView()
            } else {
                super.onManagerConnected(status)
            }
        }
    }

    override val cameraViewList: List<CameraBridgeViewBase?>
        get() {
            return listOf(openCvCameraView)
        }

    companion object {
        private const val TAG = "OCVSample::Activity"
        const val VIEW_MODE_RGBA = 0
        const val VIEW_MODE_HIST = 1
        const val VIEW_MODE_CANNY = 2
        const val VIEW_MODE_SEPIA = 3
        const val VIEW_MODE_SOBEL = 4
        const val VIEW_MODE_ZOOM = 5
        const val VIEW_MODE_PIXELIZE = 6
        const val VIEW_MODE_POSTERIZE = 7
        var viewMode = VIEW_MODE_RGBA
    }

    init {
        Log.i(TAG, "Instantiated new " + this.javaClass)
    }

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "called onCreate")
        super.onCreate(savedInstanceState)

        window.addFlags(FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_img_manipulation)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setActionBar(toolbar)

        openCvCameraView = findViewById(R.id.image_manipulations_activity_surface_view)

        openCvCameraView?.let {
            it.visibility = CameraBridgeViewBase.VISIBLE
            it.setCvCameraViewListener(this)
        }
    }

    public override fun onPause() {
        super.onPause()
        openCvCameraView?.disableView()
    }

    public override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OPENCV_VERSION_3_0_0, this, loaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            loaderCallback.onManagerConnected(SUCCESS)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        openCvCameraView?.disableView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.i(TAG, "called onCreateOptionsMenu")
        itemPreviewRGBA = menu.add("Preview RGBA")
        itemPreviewHist = menu.add("Histograms")
        itemPreviewCanny = menu.add("Canny")
        itemPreviewSepia = menu.add("Sepia")
        itemPreviewSobel = menu.add("Sobel")
        itemPreviewZoom = menu.add("Zoom")
        itemPreviewPixelize = menu.add("Pixelize")
        itemPreviewPosterize = menu.add("Posterize")
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "called onOptionsItemSelected; selected item: $item")
        if (item === itemPreviewRGBA) {
            viewMode = VIEW_MODE_RGBA
        }

        when {
            item === itemPreviewHist -> viewMode =
                VIEW_MODE_HIST
            item === itemPreviewCanny -> viewMode =
                VIEW_MODE_CANNY
            item === itemPreviewSepia -> viewMode =
                VIEW_MODE_SEPIA
            item === itemPreviewSobel -> viewMode =
                VIEW_MODE_SOBEL
            item === itemPreviewZoom -> viewMode =
                VIEW_MODE_ZOOM
            item === itemPreviewPixelize -> viewMode =
                VIEW_MODE_PIXELIZE
            item === itemPreviewPosterize -> viewMode =
                VIEW_MODE_POSTERIZE
        }
        return true
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        intermediateMat = Mat()
        ranges = MatOfFloat(0f, 256f)
        white = Scalar.all(255.0)
        histSize = MatOfInt(histSizeNum)
        channels = arrayOf(MatOfInt(0), MatOfInt(1), MatOfInt(2))
        mat0 = Mat()
        sepiaKernel = Mat(4, 4, CvType.CV_32F)
        sepiaKernel.put(0, 0, 0.189, 0.769, 0.393, 0.0)
        sepiaKernel.put(1, 0, 0.168, 0.686, 0.349, 0.0)
        sepiaKernel.put(2, 0, 0.131, 0.534, 0.272, 0.0)
        sepiaKernel.put(3, 0, 0.000, 0.000, 0.000, 1.0)
    }

    override fun onCameraViewStopped() {
        // Explicitly deallocate Mats
        intermediateMat.release()
    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
        val rgba = inputFrame.rgba()
        val sizeRgba = rgba.size()
        val rows = sizeRgba.height.toInt()
        val cols = sizeRgba.width.toInt()
        val left = cols / 8
        val top = rows / 8
        val width = cols * 3 / 4
        val height = rows * 3 / 4
        when (viewMode) {
            VIEW_MODE_RGBA -> {
            }
            VIEW_MODE_HIST -> {
                viewModeHist(inputFrame.rgba())
            }
            VIEW_MODE_CANNY -> {
                viewModeCanny(rgba, top, left, width, height)
            }
            VIEW_MODE_SOBEL -> {
                viewModeSobel(inputFrame.gray(), rgba, top, left, width, height)
            }
            VIEW_MODE_SEPIA -> {
                viewModeSepia(rgba, top, left, width, height)
            }
            VIEW_MODE_ZOOM -> {
                viewModeZoom(rgba, rows, cols)
            }
            VIEW_MODE_PIXELIZE -> {
                viewModePixelize(rgba, top, left, width, height)
            }
            VIEW_MODE_POSTERIZE -> {
                viewModePosterize(rgba, top, left, width, height)
            }
        }
        return rgba
    }

    private fun viewModeSepia(rgba: Mat, top: Int, left: Int, width: Int, height: Int) {
        val rgbaInnerWindow = rgba.submat(top, top + height, left, left + width)
        Core.transform(rgbaInnerWindow, rgbaInnerWindow, sepiaKernel)
        rgbaInnerWindow.release()
    }

    private fun viewModeCanny(rgba: Mat, top: Int, left: Int, width: Int, height: Int) {
        val rgbaInnerWindow = rgba.submat(top, top + height, left, left + width)
        Canny(rgbaInnerWindow, intermediateMat, 80.0, 90.0)
        cvtColor(intermediateMat, rgbaInnerWindow, COLOR_GRAY2BGRA, 4)
        rgbaInnerWindow.release()
    }

    private fun viewModeSobel(gray: Mat, rgba: Mat, top: Int, left: Int, width: Int, height: Int) {
        val grayInnerWindow = gray.submat(top, top + height, left, left + width)
        val rgbaInnerWindow = rgba.submat(top, top + height, left, left + width)
        Sobel(grayInnerWindow, intermediateMat, CV_8U, 1, 1)
        Core.convertScaleAbs(intermediateMat, intermediateMat, 10.0, 0.0)
        cvtColor(intermediateMat, rgbaInnerWindow, COLOR_GRAY2BGRA, 4)
        grayInnerWindow.release()
        rgbaInnerWindow.release()
    }

    private fun viewModePosterize(rgba: Mat, top: Int, left: Int, width: Int, height: Int) {
        /*
        cvtColor(rgbaInnerWindow, intermediateMat, COLOR_RGBA2RGB);
        pyrMeanShiftFiltering(intermediateMat, intermediateMat, 5, 50);
        cvtColor(intermediateMat, rgbaInnerWindow, COLOR_RGB2RGBA);
        */
        val rgbaInnerWindow = rgba.submat(top, top + height, left, left + width)
        Canny(rgbaInnerWindow, intermediateMat, 80.0, 90.0)
        rgbaInnerWindow.setTo(Scalar(0.0, 0.0, 0.0, 255.0), intermediateMat)
        Core.convertScaleAbs(rgbaInnerWindow, intermediateMat, 1.0 / 16, 0.0)
        Core.convertScaleAbs(intermediateMat, rgbaInnerWindow, 16.0, 0.0)
        rgbaInnerWindow.release()
    }

    private fun viewModePixelize(rgba: Mat, top: Int, left: Int, width: Int, height: Int) {
        val rgbaInnerWindow = rgba.submat(top, top + height, left, left + width)
        resize(
            rgbaInnerWindow,
            intermediateMat,
            size0,
            0.1,
            0.1,
            INTER_NEAREST
        )
        resize(
            intermediateMat,
            rgbaInnerWindow,
            rgbaInnerWindow.size(),
            0.0,
            0.0,
            INTER_NEAREST
        )
        rgbaInnerWindow.release()
    }

    private fun viewModeZoom(rgba: Mat, rows: Int, cols: Int) {
        val zoomCorner = rgba.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10)
        val mZoomWindow = rgba.submat(
            rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100,
            cols / 2 + 9 * cols / 100
        )
        resize(
            mZoomWindow,
            zoomCorner,
            zoomCorner.size(),
            0.0,
            0.0,
            INTER_LINEAR_EXACT
        )
        val wsize = mZoomWindow.size()
        rectangle(
            mZoomWindow,
            Point(1.0, 1.0),
            Point(wsize.width - 2, wsize.height - 2),
            Scalar(255.0, 0.0, 0.0, 255.0),
            2
        )
        zoomCorner.release()
        mZoomWindow.release()
    }

    private fun viewModeHist(rgba: Mat) {
        val sizeRgba = rgba.size()
        val hist = Mat()
        var thickness = (sizeRgba.width / (histSizeNum + 10) / 5).toInt()
        if (thickness > 5) {
            thickness = 5
        }
        val offset = ((sizeRgba.width - (5 * histSizeNum + 4 * 10) * thickness) / 2).toInt()
        // RGB
        var c = 0
        while (c < 3) {
            calcHist(listOf(rgba), channels[c], mat0, hist, histSize, ranges)
            Core.normalize(hist, hist, sizeRgba.height / 2, 0.0, Core.NORM_INF)
            hist[0, 0, buff]
            var h = 0
            while (h < histSizeNum) {
                p2.x = (offset + (c * (histSizeNum + 10) + h) * thickness).toDouble()
                p1.x = p2.x
                p1.y = sizeRgba.height - 1
                p2.y = p1.y - 2 - buff[h].toInt()
                line(rgba, p1, p2, colorsRGB[c], thickness)
                h++
            }
            c++
        }

        cvtColor(rgba, intermediateMat, COLOR_RGB2HSV_FULL)
        calcHist(
            listOf(intermediateMat),
            channels[2],
            mat0,
            hist,
            histSize,
            ranges
        )
        Core.normalize(hist, hist, sizeRgba.height / 2, 0.0, Core.NORM_INF)
        hist[0, 0, buff]
        run {
            var h = 0
            while (h < histSizeNum) {
                p2.x = (offset + (3 * (histSizeNum + 10) + h) * thickness).toDouble()
                p1.x = p2.x
                p1.y = sizeRgba.height - 1
                p2.y = p1.y - 2 - buff[h].toInt()
                line(rgba, p1, p2, white, thickness)
                h++
            }
        }
        // Hue
        calcHist(
            listOf(intermediateMat),
            channels[0],
            mat0,
            hist,
            histSize,
            ranges
        )
        Core.normalize(hist, hist, sizeRgba.height / 2, 0.0, Core.NORM_INF)
        hist[0, 0, buff]
        var h = 0
        while (h < histSizeNum) {
            p2.x = (offset + (4 * (histSizeNum + 10) + h) * thickness).toDouble()
            p1.x = p2.x
            p1.y = sizeRgba.height - 1
            p2.y = p1.y - 2 - buff[h].toInt()
            line(rgba, p1, p2, colorsHue[h], thickness)
            h++
        }
    }
}
