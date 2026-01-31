package com.digitalturbine.promptnews.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitalturbine.promptnews.data.Article
import com.digitalturbine.promptnews.data.UserLocation
import com.digitalturbine.promptnews.data.net.Http
import com.digitalturbine.promptnews.data.serpapi.SerpApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.roundToInt

data class LocalNewsState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasFetched: Boolean = false,
    val hasLoadedMore: Boolean = false,
    val items: List<Article> = emptyList()
)

data class WeatherData(
    val city: String,
    val temperature: String,
    val condition: String,
    val highLow: String
)

data class WeatherState(
    val isLoading: Boolean = false,
    val hasFetched: Boolean = false,
    val data: WeatherData? = null
)

class HomeViewModel(
    private val serpApiRepository: SerpApiRepository = SerpApiRepository()
) : ViewModel() {
    private val _localNewsState = MutableStateFlow(LocalNewsState())
    val localNewsState: StateFlow<LocalNewsState> = _localNewsState.asStateFlow()

    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private var lastLocation: UserLocation? = null

    fun loadForLocation(location: UserLocation?) {
        if (location == null) {
            lastLocation = null
            _localNewsState.value = LocalNewsState()
            _weatherState.value = WeatherState()
            return
        }
        if (location == lastLocation && _localNewsState.value.hasFetched) return
        lastLocation = location
        loadLocalNews(location)
        loadWeather(location)
    }

    fun loadMoreLocalNews(location: UserLocation) {
        val currentState = _localNewsState.value
        if (currentState.isLoadingMore || currentState.hasLoadedMore) return
        viewModelScope.launch {
            _localNewsState.value = currentState.copy(isLoadingMore = true)
            val query = "${location.city} ${location.state} local news"
            val moreItems = serpApiRepository.fetchLocalNewsByOffset(
                location = "${location.city}, ${location.state}",
                query = query,
                limit = MORE_LIMIT,
                offset = currentState.items.size
            )
            _localNewsState.value = currentState.copy(
                isLoadingMore = false,
                hasLoadedMore = true,
                items = currentState.items + moreItems
            )
        }
    }

    private fun loadLocalNews(location: UserLocation) {
        viewModelScope.launch {
            _localNewsState.value = LocalNewsState(isLoading = true)
            val query = "${location.city} ${location.state} local news"
            val items = serpApiRepository.fetchLocalNewsByOffset(
                location = "${location.city}, ${location.state}",
                query = query,
                limit = INITIAL_LIMIT,
                offset = 0
            )
            _localNewsState.value = LocalNewsState(
                isLoading = false,
                hasFetched = true,
                items = items
            )
        }
    }

    private fun loadWeather(location: UserLocation) {
        viewModelScope.launch {
            _weatherState.value = WeatherState(isLoading = true)
            val data = fetchWeather(location)
            _weatherState.value = WeatherState(
                isLoading = false,
                hasFetched = true,
                data = data
            )
        }
    }

    private suspend fun fetchWeather(location: UserLocation): WeatherData? {
        val cityEncoded = URLEncoder.encode(location.city, "UTF-8")
        val stateEncoded = URLEncoder.encode(location.state, "UTF-8")
        val geoUrl = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=$cityEncoded&count=1&language=en&format=json&state=$stateEncoded&country=US"
        val geoResult = Http.client.newCall(Http.req(geoUrl)).execute().use { response ->
            val geoBody = response.body?.string().orEmpty()
            if (!response.isSuccessful || geoBody.isBlank()) return null
            val geoJson = JSONObject(geoBody)
            val results = geoJson.optJSONArray("results") ?: JSONArray()
            results.optJSONObject(0)
        } ?: return null
        val latitude = geoResult.optDouble("latitude")
        val longitude = geoResult.optDouble("longitude")
        val cityName = geoResult.optString("name").ifBlank { location.city }

        val weatherUrl = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude&current=temperature_2m,weathercode" +
            "&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
        return Http.client.newCall(Http.req(weatherUrl)).execute().use { response ->
            val weatherBody = response.body?.string().orEmpty()
            if (!response.isSuccessful || weatherBody.isBlank()) return null
            val weatherJson = JSONObject(weatherBody)
            val current = weatherJson.optJSONObject("current") ?: return null
            val daily = weatherJson.optJSONObject("daily") ?: return null
            val highs = daily.optJSONArray("temperature_2m_max") ?: JSONArray()
            val lows = daily.optJSONArray("temperature_2m_min") ?: JSONArray()
            val temp = current.optDouble("temperature_2m")
            val code = current.optInt("weathercode")
            val high = highs.optDouble(0)
            val low = lows.optDouble(0)
            val temperature = "${temp.roundToInt()}°"
            val highLow = "H ${high.roundToInt()}° / L ${low.roundToInt()}°"
            WeatherData(
                city = cityName,
                temperature = temperature,
                condition = conditionForWeatherCode(code),
                highLow = highLow
            )
        }
    }

    private fun conditionForWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Cloudy"
        }
    }

    companion object {
        private const val INITIAL_LIMIT = 4
        private const val MORE_LIMIT = 7
    }
}
