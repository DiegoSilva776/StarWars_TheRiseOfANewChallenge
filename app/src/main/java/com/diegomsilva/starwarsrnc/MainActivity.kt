package com.diegomsilva.starwarsrnc

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import com.diegomsilva.starwarsrnc.model.networking.StarWarsApi
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

    var listView : ListView? = null
    var movies = mutableListOf<String>()
    var moviesAdapter : ArrayAdapter<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listView = ListView(this)
        setContentView(listView)

        movies = mutableListOf<String>()

        moviesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, movies)
        listView?.adapter = moviesAdapter

        val api = StarWarsApi()
        api.loadMoviesFull()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    movie ->
                    movies.add("${movie.title} - ${movie.episodeId} \n ${movie.characters.toString()}")
                },
                {
                    e -> e.printStackTrace()
                },
                {
                    moviesAdapter?.notifyDataSetChanged()
                }
            )
    }

}