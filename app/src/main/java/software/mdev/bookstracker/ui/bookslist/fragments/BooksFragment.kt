package software.mdev.bookstracker.ui.bookslist.fragments

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.android.synthetic.main.fragment_books.*
import software.mdev.bookstracker.R
import software.mdev.bookstracker.adapters.BookListAdapter
import software.mdev.bookstracker.data.db.entities.Book
import software.mdev.bookstracker.other.Constants
import software.mdev.bookstracker.ui.bookslist.ListActivity
import software.mdev.bookstracker.ui.bookslist.dialogs.AddEditBookDialog
import software.mdev.bookstracker.ui.bookslist.viewmodel.BooksViewModel


class BooksFragment : Fragment(R.layout.fragment_books) {

    lateinit var viewModel: BooksViewModel
    private var wiggleBlocker = true
    lateinit var listActivity: ListActivity
    private lateinit var barcodeLauncher: ActivityResultLauncher<ScanOptions>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listActivity = activity as ListActivity

        var listOfTabs = context?.resources?.let {
            listOf(
                it.getString(R.string.readFragment),
                it.getString(R.string.inProgressFragment),
                it.getString(R.string.toReadFragment)
            )
        }

        val adapter = BookListAdapter(this)
        vpBooks.adapter = adapter

        adapter?.differ?.submitList(listOfTabs)

        TabLayoutMediator(tlBooks, vpBooks) { tab, position ->
            tab.text = listOfTabs?.get(position)
        }.attach()

        vpBooks.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (!wiggleBlocker) {
                    var anim = AnimationUtils.loadAnimation(context, R.anim.shake_1_light)
                    fabAddBook.startAnimation(anim)
                } else
                    wiggleBlocker = false
            }
        })

        fabAddBook.setOnClickListener {
            var anim = AnimationUtils.loadAnimation(context, R.anim.shake_1)
            fabAddBook.startAnimation(anim)
            showBottomSheetDialog(vpBooks.currentItem)
        }

        barcodeLauncher = registerForActivityResult(
            ScanContract()
        ) { result: ScanIntentResult ->
            if (result.contents != null)
                addSearchGoToFrag(result.contents, vpBooks.currentItem)
        }
    }

    private fun showBottomSheetDialog(currentItem: Int) {
        if (context != null) {
            val bottomSheetDialog =
                BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme)
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_add_books)

            bottomSheetDialog.findViewById<LinearLayout>(R.id.llAddManual)
                ?.setOnClickListener {
                    addManualGoToFrag(currentItem)
                    bottomSheetDialog.dismiss()
                }

            bottomSheetDialog.findViewById<LinearLayout>(R.id.llAddScan)
                ?.setOnClickListener {
                    openCodeScanner()
                    bottomSheetDialog.dismiss()
                }

            bottomSheetDialog.findViewById<LinearLayout>(R.id.llAddSearch)
                ?.setOnClickListener {
                    addSearchGoToFrag("manual_search", currentItem)
                    bottomSheetDialog.dismiss()
                }

            bottomSheetDialog.show()
        }
    }

    private fun addManualGoToFrag(currentItem: Int) {
        val emptyBook = Book("", "")
        val addEditBookDialog = AddEditBookDialog()

        if (addEditBookDialog != null) {
            addEditBookDialog!!.arguments = Bundle().apply {
                putSerializable(Constants.SERIALIZABLE_BUNDLE_BOOK, emptyBook)
                putSerializable(Constants.SERIALIZABLE_BUNDLE_BOOK_SOURCE, Constants.NO_SOURCE)
                putSerializable(Constants.SERIALIZABLE_BUNDLE_ACCENT, (activity as ListActivity).getAccentColor(activity as ListActivity, true))
                putInt(Constants.SERIALIZABLE_BUNDLE_BOOK_STATUS, currentItem)
            }
            addEditBookDialog!!.show(childFragmentManager, AddEditBookDialog.TAG)
        }
    }

    private fun openCodeScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt(listActivity.getString(R.string.tbScannerTip))
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(true)
        barcodeLauncher.launch(options)
    }

    private fun addSearchGoToFrag(isbn: String = "manual_search", currentItem: Int) {
        val bundle = Bundle().apply {
            putString(Constants.SERIALIZABLE_BUNDLE_ISBN, isbn)
            putInt(Constants.SERIALIZABLE_BUNDLE_BOOK_STATUS, currentItem)
        }

        findNavController().navigate(
            R.id.action_booksFragment_to_addBookSearchFragment,
            bundle
        )
    }
}