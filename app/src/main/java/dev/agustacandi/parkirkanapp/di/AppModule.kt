package dev.agustacandi.parkirkanapp.di

import com.google.firebase.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.agustacandi.parkirkanapp.data.auth.AuthRepositoryImpl
import dev.agustacandi.parkirkanapp.data.auth.network.AuthService
import dev.agustacandi.parkirkanapp.data.broadcast.BroadcastRepositoryImpl
import dev.agustacandi.parkirkanapp.data.broadcast.network.BroadcastService
import dev.agustacandi.parkirkanapp.data.parking.ParkingRepositoryImpl
import dev.agustacandi.parkirkanapp.data.parking.network.ParkingService
import dev.agustacandi.parkirkanapp.data.vehicle.VehicleRepositoryImpl
import dev.agustacandi.parkirkanapp.data.vehicle.network.VehicleService
import dev.agustacandi.parkirkanapp.domain.auth.repository.AuthRepository
import dev.agustacandi.parkirkanapp.domain.broadcast.BroadcastRepository
import dev.agustacandi.parkirkanapp.domain.parking.repository.ParkingRepository
import dev.agustacandi.parkirkanapp.domain.vehicle.VehicleRepository
import dev.agustacandi.parkirkanapp.util.FCMTokenManager
import dev.agustacandi.parkirkanapp.util.UserPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    // FCM
    @Provides
    @Singleton
    fun provideFCMTokenManager(): FCMTokenManager =
        FCMTokenManager()

    // Network
    @Provides
    @Singleton
    fun provideMoshi(): Moshi =
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://replication-fans-spent-debate.trycloudflare.com/api/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        userPreferences: UserPreferences
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                userPreferences.getAuthToken()?.let { token ->
                    request.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(request.build())
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideHttpLogger(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level =
                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

    // Service
    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService =
        retrofit.create(AuthService::class.java)

    @Provides
    @Singleton
    fun provideParkingService(retrofit: Retrofit): ParkingService =
        retrofit.create(ParkingService::class.java)

    @Provides
    @Singleton
    fun provideVehicleService(retrofit: Retrofit): VehicleService =
        retrofit.create(VehicleService::class.java)

    @Provides
    @Singleton
    fun provideBroadcastService(retrofit: Retrofit): BroadcastService =
        retrofit.create(BroadcastService::class.java)

    // Repository
    @Provides
    @Singleton
    fun provideAuthRepository(
        authService: AuthService,
        userPreferences: UserPreferences
    ): AuthRepository =
        AuthRepositoryImpl(authService, userPreferences)

    @Provides
    @Singleton
    fun provideParkingRepository(
        parkingService: ParkingService,
    ): ParkingRepository =
        ParkingRepositoryImpl(parkingService)

    @Provides
    @Singleton
    fun provideVehicleRepository(
        vehicleService: VehicleService,
    ): VehicleRepository =
        VehicleRepositoryImpl(vehicleService)

    @Provides
    @Singleton
    fun provideBroadcastRepository(broadcastService: BroadcastService): BroadcastRepository =
        BroadcastRepositoryImpl(broadcastService)

}