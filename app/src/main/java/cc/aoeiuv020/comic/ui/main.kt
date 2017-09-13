package cc.aoeiuv020.comic.ui

import android.content.Context
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import cc.aoeiuv020.comic.R
import cc.aoeiuv020.comic.api.ComicGenre
import cc.aoeiuv020.comic.api.ComicListItem
import cc.aoeiuv020.comic.api.ComicSite
import cc.aoeiuv020.comic.di.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.comic_list_item.view.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.site_list_item.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error

/**
 * 主页，展示网站，分类，漫画列表，
 * Created by AoEiuV020 on 2017.09.12-19:04:44.
 */

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private val GROUP_ID: Int = 1

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.groupId) {
            GROUP_ID -> {
                val loadingDialog = loading()
                DaggerListComponent.builder()
                        .listModule(ListModule(genres[item.order]))
                        .build()
                        .getComicList()
                        .async()
                        .toList()
                        .subscribe({ comicList ->
                            setComicList(comicList)
                            loadingDialog.dismiss()
                        }, { e ->
                            error("加载漫画列表失败", e)
                            loadingDialog.dismiss()
                        })
            }
            else -> when (item.itemId) {
                R.id.select_sites -> showSites()
                R.id.settings -> {
                    Snackbar.make(drawer_layout, "没实现", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setComicList(comicList: List<ComicListItem>) {
        listView.run {
            adapter = ComicListAdapter(this@MainActivity, comicList)
            setOnItemClickListener { _, _, position, _ ->
            }
        }
    }

    private fun showSites() {
        DaggerSiteComponent.create()
                .getSites()
                .async()
                .toList()
                .subscribe { sites ->
                    AlertDialog.Builder(this@MainActivity).setAdapter(SiteListAdapter(this@MainActivity, sites)) { alertDialog, index ->
                        drawer_layout.openDrawer(GravityCompat.START)
                        val site = sites[index]
                        val loadingDialog = loading()
                        DaggerGenreComponent.builder()
                                .genreModule(GenreModule(site))
                                .build()
                                .getGenre()
                                .async()
                                .toList()
                                .subscribe({ genres ->
                                    setGenres(genres)
                                    loadingDialog.dismiss()
                                }, { e ->
                                    error("加载网站列表失败", e)
                                    loadingDialog.dismiss()
                                    alertDialog.dismiss()
                                })
                    }.show()
                }
    }

    private lateinit var genres: List<ComicGenre>

    private fun setGenres(genres: List<ComicGenre>) {
        this.genres = genres
        nav_view.menu.run {
            removeGroup(GROUP_ID)
            genres.forEachIndexed { index, comicGenre ->
                add(GROUP_ID, index, index, comicGenre.name)
            }
        }
    }
}

class SiteListAdapter(val ctx: Context, val sites: List<ComicSite>) : BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: View.inflate(ctx, R.layout.site_list_item, null)
        val site = getItem(position)
        view.apply {
            site_name.text = site.name
            asyncLoadImage(site_logo, site.logo)
        }
        return view
    }

    override fun getItem(position: Int) = sites[position]

    override fun getItemId(position: Int) = 0L

    override fun getCount() = sites.size
}

class ComicListAdapter(val ctx: Context, val items: List<ComicListItem>) : BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View
            = convertView ?: View.inflate(ctx, R.layout.comic_list_item, null).apply {
        val comic = getItem(position)
        comic_name.text = comic.name
        asyncLoadImage(comic_icon, comic.img)
    }

    override fun getItem(position: Int) = items[position]

    override fun getItemId(position: Int) = 0L

    override fun getCount() = items.size
}