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

    private val service : StarWarsApiDef
    private val peopleCache = mutableMapOf<String, Person>()

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
                            Observable.concat(
                                getCache(personUrl),
                                service.loadPerson(Uri.parse(personUrl).lastPathSegment).doOnNext { person ->
                                    peopleCache.put(personUrl, person)
                                },
                            )
                                .firstElement()
                                .toObservable()
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

    /**
     * People cache
     */
    private fun getCache(personUrl : String) : Observable<Person> {
        return Observable.fromIterable(peopleCache.keys)
            .filter { key ->
                key == personUrl
            }
            .flatMap { key ->
                Observable.just(peopleCache[personUrl])
            }
    }

}