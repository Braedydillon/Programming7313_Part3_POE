package com.example.tracker

import Data.Expense
import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern

class AutoScannerFragment : Fragment() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var scanResultText: TextView
    private lateinit var fabCapture: View
    private lateinit var progressBar: ProgressBar
    private lateinit var scanningOverlay: ScanningOverlayView
    private lateinit var alignmentBoxView: View

    private var imageCapture: ImageCapture? = null
    
    private var lastScannedShop: String? = null
    private var lastScannedTotal: Double? = null
    private var lastScannedDescription: String? = null
    private var lastScannedBitmap: Bitmap? = null

    private val categories = arrayOf("Groceries", "Fuel", "Restaurants", "Shopping", "Entertainment", "Health", "General")

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_auto_scanner, container, false)

        previewView = view.findViewById(R.id.previewView)
        scanResultText = view.findViewById(R.id.scanResultText)
        fabCapture = view.findViewById(R.id.fabCapture)
        progressBar = view.findViewById(R.id.progressBar)
        scanningOverlay = view.findViewById(R.id.scanningOverlay)
        alignmentBoxView = view.findViewById(R.id.alignmentBox)

        alignmentBoxView.post {
            val rect = RectF(
                alignmentBoxView.left.toFloat(),
                alignmentBoxView.top.toFloat(),
                alignmentBoxView.right.toFloat(),
                alignmentBoxView.bottom.toFloat()
            )
            scanningOverlay.setAlignmentBox(rect)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        fabCapture.setOnClickListener {
            takePhoto()
        }

        return view
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e("AutoScannerFragment", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        progressBar.visibility = View.VISIBLE
        fabCapture.isEnabled = false
        scanningOverlay.setPaused(true)
        scanResultText.text = "Processing receipt..."

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    processCapturedImage(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("AutoScannerFragment", "Photo capture failed: ${exception.message}", exception)
                    progressBar.visibility = View.GONE
                    fabCapture.isEnabled = true
                    scanningOverlay.setPaused(false)
                    scanResultText.text = "Align receipt in the box"
                    Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    @OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processCapturedImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            lastScannedBitmap = imageProxyToBitmap(imageProxy)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    extractReceiptInfo(visionText)
                    progressBar.visibility = View.GONE
                    fabCapture.isEnabled = true
                    scanningOverlay.setPaused(false)
                    scanResultText.text = "Align receipt in the box"
                    showConfirmationDialog()
                }
                .addOnFailureListener { e ->
                    Log.e("AutoScannerFragment", "Text recognition failed", e)
                    progressBar.visibility = View.GONE
                    fabCapture.isEnabled = true
                    scanningOverlay.setPaused(false)
                    scanResultText.text = "Align receipt in the box"
                    Toast.makeText(context, "Failed to scan receipt", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            progressBar.visibility = View.GONE
            fabCapture.isEnabled = true
            scanningOverlay.setPaused(false)
            imageProxy.close()
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        Log.d("AutoScannerFragment", "imageProxyToBitmap: format=${image.format}, rotation=${image.imageInfo.rotationDegrees}")
        return try {
            val bitmap = image.toBitmap()
            val rotation = image.imageInfo.rotationDegrees
            if (rotation != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e("AutoScannerFragment", "Error converting image to bitmap using toBitmap()", e)
            // Manual fallback for older formats if toBitmap fails
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.duplicate().get(bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e2: Exception) {
                Log.e("AutoScannerFragment", "Manual bitmap fallback also failed", e2)
                null
            }
        }
    }

    private fun extractReceiptInfo(visionText: Text) {
        val fullText = visionText.text
        if (fullText.isEmpty()) return

        val pricePattern = Pattern.compile("(?:R|\\$)\\s?(\\d+[.,]\\d{2})|(\\d+[.,]\\d{2})")
        
        val foundPrices = mutableListOf<Double>()
        val allLines = mutableListOf<String>()
        val foundItems = mutableListOf<String>()

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                allLines.add(line.text)
                val matcher = pricePattern.matcher(line.text)
                if (matcher.find()) {
                    val amountString = matcher.group(1) ?: matcher.group(2)
                    val amount = amountString?.replace(",", ".")?.toDoubleOrNull()
                    if (amount != null) {
                        foundPrices.add(amount)
                        
                        val matchedText = matcher.group(0) ?: ""
                        var itemName = line.text.replace(matchedText, "").trim()
                        if (itemName.isEmpty()) {
                            val idxInBlock = block.lines.indexOf(line)
                            if (idxInBlock > 0) {
                                itemName = block.lines[idxInBlock - 1].text
                            }
                        }
                        if (itemName.isNotEmpty()) {
                            foundItems.add(itemName)
                        }
                    }
                }
            }
        }

        if (foundPrices.isNotEmpty()) {
            lastScannedTotal = foundPrices.maxOrNull() ?: 0.0
            lastScannedShop = if (allLines.isNotEmpty()) allLines[0].trim() else "Unknown Shop"
            lastScannedDescription = foundItems.take(5).joinToString(", ")
        } else {
            lastScannedTotal = 0.0
            lastScannedShop = "Unknown Shop"
            lastScannedDescription = "Manual review needed"
        }
    }

    private fun showConfirmationDialog() {
        val shop = lastScannedShop ?: "Unknown Shop"
        val amount = lastScannedTotal ?: 0.0
        val desc = lastScannedDescription ?: "No description"
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val bitmap = lastScannedBitmap

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_item, null)
        val shopInput = dialogView.findViewById<EditText>(R.id.editItemName)
        val amountInput = dialogView.findViewById<EditText>(R.id.editAmount)
        val dateInput = dialogView.findViewById<EditText>(R.id.editDate)
        val notesInput = dialogView.findViewById<EditText>(R.id.editNotes)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val imgPreview = dialogView.findViewById<ImageView>(R.id.imgReceiptPreview)
        
        if (bitmap != null) {
            imgPreview.visibility = View.VISIBLE
            imgPreview.setImageBitmap(bitmap)
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        shopInput.setText(shop)
        amountInput.setText(String.format(java.util.Locale.US, "%.2f", amount))
        dateInput.setText(date)
        notesInput.setText(desc)

        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(), { _, year, month, day ->
                val selectedDate = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, day)
                dateInput.setText(selectedDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Receipt Details")
            .setView(dialogView)
            .setPositiveButton("Save to Budget") { _: DialogInterface, _: Int ->
                val finalShop = shopInput.text.toString()
                val amountText = amountInput.text.toString().replace(",", ".")
                val finalAmount = amountText.toDoubleOrNull() ?: 0.0
                val finalCategory = categorySpinner.selectedItem.toString()
                val finalDate = dateInput.text.toString()
                val finalNotes = notesInput.text.toString()
                
                // Convert bitmap to Base64 string just like in ExpensesFragment
                val base64ImageString = bitmap?.let { convertBitmapToBase64(it) }
                saveExpenseToFirebase(finalShop, finalAmount, finalCategory, finalDate, finalNotes, base64ImageString)
            }
            .setNegativeButton("Retake", null)
            .show()
    }

    private fun convertBitmapToBase64(bitmap: Bitmap): String? {
        return try {
            // Downscale to 500x500 for Firebase storage optimization
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 500, 500, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            val encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            Log.d("AutoScannerFragment", "Base64 string generated, length: ${encoded.length}")
            encoded
        } catch (e: Exception) {
            Log.e("AutoScannerFragment", "Base64 conversion failed", e)
            null
        }
    }

    private fun saveExpenseToFirebase(shopName: String, amount: Double, category: String, date: String, notes: String, base64ImageString: String?) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("users")
        val key = database.child(userId).child("expenses").push().key ?: return

        val finalDescription = if (notes.isNotEmpty()) "$shopName: $notes" else shopName
        
        if (base64ImageString != null) {
            Log.d("AutoScannerFragment", "Saving expense with image. Base64 length: ${base64ImageString.length}")
        } else {
            Log.w("AutoScannerFragment", "Saving expense WITHOUT image string.")
        }

        val expense = Expense(
            category = category,
            amount = amount,
            date = date,
            description = finalDescription,
            photoUri = base64ImageString
        )

        database.child(userId).child("expenses").child(key).setValue(expense)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Expense synced globally!", Toast.LENGTH_SHORT).show()
                    (activity as? Home)?.replaceFragment(ReportFragment())
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(context, "Database error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
}
