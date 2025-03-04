package io.legado.app.ui.book.source.debug

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivitySourceDebugBinding
import io.legado.app.help.source.exploreKinds
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.launch
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch
import splitties.views.onClick
import splitties.views.onLongClick

class BookSourceDebugActivity : VMBaseActivity<ActivitySourceDebugBinding, BookSourceDebugModel>() {

    override val binding by viewBinding(ActivitySourceDebugBinding::inflate)
    override val viewModel by viewModels<BookSourceDebugModel>()

    private val adapter by lazy { BookSourceDebugAdapter(this) }
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it?.let {
            startSearch(it)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchView()
        viewModel.init(intent.getStringExtra("key")) {
            initHelpView()
        }
        viewModel.observe { state, msg ->
            launch {
                adapter.addItem(msg)
                if (state == -1 || state == 1000) {
                    binding.rotateLoading.hide()
                }
            }
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.adapter = adapter
        binding.rotateLoading.loadingColor = accentColor
    }

    private fun initSearchView() {
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.search_book_key)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                openOrCloseHelp(false)
                startSearch(query ?: "我的")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            openOrCloseHelp(hasFocus)
        }
        openOrCloseHelp(true)
    }

    @SuppressLint("SetTextI18n")
    private fun initHelpView() {
        viewModel.bookSource?.ruleSearch?.checkKeyWord?.let {
            if (it.isNotBlank()) {
                binding.textMy.text = it
            }
        }
        binding.textMy.onClick {
            searchView.setQuery(binding.textMy.text, true)
        }
        binding.textXt.onClick {
            searchView.setQuery(binding.textXt.text, true)
        }
        binding.textFx.onClick {
            if (!binding.textFx.text.startsWith("ERROR:")) {
                searchView.setQuery(binding.textFx.text, true)
            }
        }
        binding.textInfo.onClick {
            if (!searchView.query.isNullOrBlank()) {
                searchView.setQuery(searchView.query, true)
            }
        }
        binding.textToc.onClick {
            val query = searchView.query
            if (query.isNullOrBlank() || query.length <= 2) {
                searchView.setQuery("++", false)
            } else {
                if (!query.startsWith("++")) {
                    searchView.setQuery("++$query", true)
                } else {
                    searchView.setQuery(query, true)
                }
            }
        }
        binding.textContent.onClick {
            val query = searchView.query
            if (query.isNullOrBlank() || query.length <= 2) {
                searchView.setQuery("--", false)
            } else {
                if (!query.startsWith("--")) {
                    searchView.setQuery("--$query", true)
                } else {
                    searchView.setQuery(query, true)
                }
            }
        }
        launch {
            val exploreKinds = viewModel.bookSource?.exploreKinds()?.filter {
                !it.url.isNullOrBlank()
            }
            exploreKinds?.firstOrNull()?.let {
                binding.textFx.text = "${it.title}::${it.url}"
                if (it.title.startsWith("ERROR:")) {
                    adapter.addItem("获取发现出错\n${it.url}")
                    openOrCloseHelp(false)
                    searchView.clearFocus()
                    return@launch
                }
            }
            exploreKinds?.map { it.title }?.let { exploreKindTitles ->
                binding.textFx.onLongClick {
                    selector("选择发现", exploreKindTitles) { _, index ->
                        val explore = exploreKinds[index]
                        binding.textFx.text = "${explore.title}::${explore.url}"
                        searchView.setQuery(binding.textFx.text, true)
                    }
                }
            }
        }
    }

    /**
     * 打开关闭历史界面
     */
    private fun openOrCloseHelp(open: Boolean) {
        if (open) {
            binding.help.visibility = View.VISIBLE
        } else {
            binding.help.visibility = View.GONE
        }
    }

    private fun startSearch(key: String) {
        adapter.clearItems()
        viewModel.startDebug(key, {
            binding.rotateLoading.show()
        }, {
            toastOnUi("未获取到书源")
        })
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_source_debug, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> qrCodeResult.launch()
            R.id.menu_search_src -> showDialogFragment(TextDialog(viewModel.searchSrc))
            R.id.menu_book_src -> showDialogFragment(TextDialog(viewModel.bookSrc))
            R.id.menu_toc_src -> showDialogFragment(TextDialog(viewModel.tocSrc))
            R.id.menu_content_src -> showDialogFragment(TextDialog(viewModel.contentSrc))
            R.id.menu_help -> showHelp()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun showHelp() {
        val text = String(assets.open("help/debugHelp.md").readBytes())
        showDialogFragment(TextDialog(text, TextDialog.Mode.MD))
    }

}