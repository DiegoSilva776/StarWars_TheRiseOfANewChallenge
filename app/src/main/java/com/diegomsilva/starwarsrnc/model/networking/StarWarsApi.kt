package com.diegomsilva.starwarsrnc.model.networking

import android.net.Uri
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.core.Observable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

import com.diegomsilva.starwarsrnc.model.Movie
import com.diegomsilva.starwarsrnc.model.Character

class StarWarsApi {

    val service : StarWarsApiDef

    init {
        var logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        var httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logging)

        var gson = GsonBuilder().setLenient().create()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://swapi.dev/api/")
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient.build())
            .build()

        service = retrofit.create<StarWarsApiDef>(StarWarsApiDef::class.java)
    }

    fun loadMovies() : Observable<Movie> {
        return service.listMovies()
            .flatMap { filmResult -> Observable.fromIterable(filmResult.results) }
            .flatMap { film -> Observable.just(Movie(film.title, film.episodeId, ArrayList<Character>())) }
    }

    fun loadMoviesFull() : Observable<Movie> {
        return service.listMovies()
            .flatMap { filmResult -> Observable.fromIterable(filmResult.results) }
            .flatMap { film ->
                Observable.zip(
                    Observable.just(Movie(film.title, film.episodeId, ArrayList<Character>())),
                    Observable.fromIterable(film.personUrls)
                        .flatMap { personUrl ->
                            Uri.parse(personUrl).lastPathSegment?.let { service.loadPerson(it) }
                        }
                        .flatMap { person ->
                            Observable.just(Character(person.name, person.gender))
                        }
                        .toList()
                        .toObservable(),
                    {   movie, characters ->
                        movie.characters.addAll(characters)
                        movie
                    }
                )
            }
    }

}