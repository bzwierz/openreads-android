package software.mdev.bookstracker.ui.bookslist.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayoutMediator
import software.mdev.bookstracker.R
import software.mdev.bookstracker.ui.bookslist.ListActivity
import kotlinx.android.synthetic.main.fragment_statistics.*
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL
import software.mdev.bookstracker.adapters.StatisticsAdapter
import software.mdev.bookstracker.data.db.BooksDatabase
import software.mdev.bookstracker.data.db.LanguageDatabase
import software.mdev.bookstracker.data.db.YearDatabase
import software.mdev.bookstracker.data.db.entities.Book
import software.mdev.bookstracker.data.db.entities.Year
import software.mdev.bookstracker.data.repositories.BooksRepository
import software.mdev.bookstracker.data.repositories.LanguageRepository
import software.mdev.bookstracker.data.repositories.OpenLibraryRepository
import software.mdev.bookstracker.data.repositories.YearRepository
import software.mdev.bookstracker.other.Constants
import software.mdev.bookstracker.ui.bookslist.viewmodel.BooksViewModel
import software.mdev.bookstracker.ui.bookslist.viewmodel.BooksViewModelProviderFactory
import java.text.SimpleDateFormat
import java.util.*


class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    lateinit var viewModel: BooksViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = (activity as ListActivity).booksViewModel
        view.hideKeyboard()

        var sharedPreferencesName = (activity as ListActivity).getString(R.string.shared_preferences_name)
        val sharedPref = (activity as ListActivity).getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)

        val database = BooksDatabase(view.context)
        val yearDatabase = YearDatabase(view.context)
        val languageDatabase = LanguageDatabase(view.context)

        val booksRepository = BooksRepository(database)
        val yearRepository = YearRepository(yearDatabase)
        val openLibraryRepository = OpenLibraryRepository()
        val languageRepository = LanguageRepository(languageDatabase)

        val booksViewModelProviderFactory = BooksViewModelProviderFactory(
            booksRepository,
            yearRepository,
            openLibraryRepository,
            languageRepository
        )

        viewModel = ViewModelProvider(this, booksViewModelProviderFactory).get(
            BooksViewModel::class.java
        )

        var listOfYearsFromDb: List<Year>

        viewModel.getYears()
            .observe(viewLifecycleOwner, Observer { years ->
                listOfYearsFromDb = years
                val adapter = StatisticsAdapter(this, listOfYearsFromDb)
                vpStatistics.adapter = adapter
                this.getYears(adapter)
            })

        ivMore.setOnClickListener {
            it.hideKeyboard()
            findNavController().navigate(R.id.settingsFragment, null)
        }
    }

    fun View.hideKeyboard() {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun getYears(statisticsAdapter: StatisticsAdapter) {
        viewModel.getSortedBooksByFinishDateDesc(Constants.BOOK_STATUS_READ)
            .observe(viewLifecycleOwner, Observer { books ->
                var listOfYears = mutableListOf<Year>()
                listOfYears.add(Year(
                    "0000",
                    0,
                    0,
                    0F,
                    0,
                    0,
                    "null",
                    "null",
                    "null",
                    0,
                    "null",
                    0
                ))

                var years = calculateHowManyYearsForStats(books)
                var year: Int

                var booksAllTime = 0
                var pagesAllTime = 0
                var sumRatingAllTime = 0F

                var longestBookAllTime = "null"
                var longestBookValAllTime = 0

                var quickestReadAllTime = "null"
                var quickestReadValAllTime = Long.MAX_VALUE

                var readingTimeSumAllTime = 0L
                var averageReadingTimeNumOfBooksAllTime = 0L

                for (item_year in years) {
                    var booksInYear = 0
                    var pagesInYear = 0
                    var sumRatingInYear = 0F

                    var longestBook = "null"
                    var longestBookVal = 0

                    var quickestRead = "null"
                    var quickestReadVal = Long.MAX_VALUE

                    var readingTimeSum = 0L
                    var averageReadingTimeNumOfBooks = 0L



                    for (item_book in books) {
                        if (item_book.bookFinishDate != "none" && item_book.bookFinishDate != "null") {
                            year = convertLongToYear(item_book.bookFinishDate.toLong()).toInt()
                            if (year == item_year) {
                                booksInYear++
                                booksAllTime++
                                pagesInYear += item_book.bookNumberOfPages
                                pagesAllTime += item_book.bookNumberOfPages
                                sumRatingInYear += item_book.bookRating
                                sumRatingAllTime += item_book.bookRating

                                // longest book in a year
                                if (item_book.bookNumberOfPages > longestBookVal) {
                                    longestBookVal = item_book.bookNumberOfPages
                                    var string = item_book.bookTitle + " - " + item_book.bookAuthor
                                    longestBook = string
                                }

                                // longest book all time
                                if (item_book.bookNumberOfPages > longestBookValAllTime) {
                                    longestBookValAllTime = item_book.bookNumberOfPages
                                    var string = item_book.bookTitle + " - " + item_book.bookAuthor
                                    longestBookAllTime = string
                                }


                                if (item_book.bookStartDate != "none" && item_book.bookStartDate != "null") {
                                    var readingTime = item_book.bookFinishDate.toLong() - item_book.bookStartDate.toLong()

                                    if (readingTime < quickestReadVal) {
                                        quickestReadVal = readingTime
                                        var string = item_book.bookTitle + " - " + item_book.bookAuthor
                                        quickestRead = string
                                    }

                                    if (readingTime < quickestReadValAllTime) {
                                        quickestReadValAllTime = readingTime
                                        var string = item_book.bookTitle + " - " + item_book.bookAuthor
                                        quickestReadAllTime = string
                                    }

                                    readingTimeSum += readingTime
                                    averageReadingTimeNumOfBooks ++

                                    readingTimeSumAllTime += readingTime
                                    averageReadingTimeNumOfBooksAllTime ++
                                }

                                if (item_book.bookStartDate != "none" && item_book.bookStartDate != "null") {
                                    var readingTime = item_book.bookFinishDate.toLong() - item_book.bookStartDate.toLong()

                                    if (readingTime < quickestReadVal) {
                                        quickestReadVal = readingTime
                                        var string = item_book.bookTitle + " - " + item_book.bookAuthor
                                        quickestRead = string
                                    }

                                    readingTimeSum += readingTime
                                    averageReadingTimeNumOfBooks ++
                                }
                            }
                        }
                    }

                    var avgReadingTime = "0"
                    if (averageReadingTimeNumOfBooks != 0L)
                        avgReadingTime =(readingTimeSum/averageReadingTimeNumOfBooks).toString()

                    var avgPages = 0
                    if (booksInYear != 0)
                        avgPages = (pagesInYear/booksInYear)

                    var avgRating = 0F
                    if (booksInYear != 0)
                        avgRating = (sumRatingInYear/booksInYear)

                    listOfYears.add(Year(
                        item_year.toString(),
                        booksInYear,
                        pagesInYear,
                        avgRating,
                        0,
                        0,
                        quickestRead,
                        quickestReadVal.toString(),
                        longestBook,
                        longestBookVal,
                        avgReadingTime,
                        avgPages
                    ))
                }

                var avgReadingTimeAllTime = "0"
                if (averageReadingTimeNumOfBooksAllTime != 0L)
                    avgReadingTimeAllTime =(readingTimeSumAllTime/averageReadingTimeNumOfBooksAllTime).toString()

                var avgPagesAllTime = 0
                if (booksAllTime != 0)
                    avgPagesAllTime = (pagesAllTime/booksAllTime)

                var avgRatingAllTime = 0F
                if (booksAllTime != 0)
                    avgRatingAllTime = (sumRatingAllTime/booksAllTime)

                listOfYears[0] = Year("0000",
                    booksAllTime,
                    pagesAllTime,
                    avgRatingAllTime,
                    0,
                    0,
                    quickestReadAllTime,
                    quickestReadValAllTime.toString(),
                    longestBookAllTime,
                    longestBookValAllTime,
                    avgReadingTimeAllTime,
                    avgPagesAllTime
                )

                statisticsAdapter.differ.submitList(listOfYears)

                // bounce effect on viewpager2
                vpStatistics.children.filterIsInstance<RecyclerView>().firstOrNull()?.let {
                    OverScrollDecoratorHelper.setUpOverScroll(it, ORIENTATION_HORIZONTAL)
                }

                TabLayoutMediator(tlStatistics, vpStatistics) { tab, position ->
                    when (position) {
                        0 -> tab.text = getString(R.string.statistics_all)
                        else -> tab.text = listOfYears[position].year
                    }
                }.attach()
            }
            )
    }

    private fun calculateHowManyYearsForStats(books: List<Book>): List<Int> {
        var years = listOf<Int>()
        var year: Int

        for (item in books) {
            if (item.bookFinishDate != "null" && item.bookFinishDate != "none") {
                year = convertLongToYear(item.bookFinishDate.toLong()).toInt()
                if (year !in years) {
                    years = years + year
                }
            }
        }
        return years
    }

    private fun convertLongToYear(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("yyyy")
        return format.format(date)
    }
}