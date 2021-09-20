package com.diegomsilva.starwarsrnc.model.networking

import retrofit2.http.GET
import retrofit2.http.Path
import io.reactivex.rxjava3.core.Observable

interface StarWarsApiDef {

    @GET("films")
    fun listMovies() : Observable<FilmResult>

    @GET("people/{personId}")
    fun loadPerson(@Path("personId") personId: String?) : Observable<Person>

}