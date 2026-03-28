/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/di/AppModule.kt
    Descrição: Módulo Hilt de injeção de dependências, provendo repositório, serviços e banco de dados.
    Autor: Miguel Nischor <miguel@nischor.com.br>

    AVISO DE LICENÇA – USO DEMONSTRATIVO

    Este software é propriedade exclusiva de seu(s) autor(es) e está protegido pelas leis de
    direitos autorais e demais legislações aplicáveis.

    Sua utilização está estritamente limitada para fins demonstrativos no contexto do evento
    “Prêmio Educador Transformador” do SEBRAE. Qualquer uso fora desse escopo, incluindo, mas
    não se limitando a, reprodução, distribuição, modificação, engenharia reversa,
    sublicenciamento, comercialização ou qualquer outra forma de exploração, é expressamente
    proibido sem autorização prévia e por escrito do(s) detentor(es) dos direitos.

    Este licenciamento não concede quaisquer direitos de propriedade intelectual ao usuário,
    sendo permitido apenas o acesso e uso temporário para apresentação e avaliação durante o
    referido evento.

    O descumprimento destes termos poderá resultar em medidas legais cabíveis.

    Todos os direitos reservados.
*/
package tech.datatower.sebrae.desafio.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import tech.datatower.sebrae.desafio.BuildConfig
import tech.datatower.sebrae.desafio.R
import tech.datatower.sebrae.desafio.data.connectivity.ConnectivityObserver
import tech.datatower.sebrae.desafio.data.connectivity.NetworkConnectivityObserver
import tech.datatower.sebrae.desafio.data.local.AppDao
import tech.datatower.sebrae.desafio.data.local.AppDatabase
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseDataConnectService
import tech.datatower.sebrae.desafio.data.remote.firebase.FirebaseSeedCredentialStore
import tech.datatower.sebrae.desafio.data.repository.AppRepository
import javax.inject.Qualifier
import javax.inject.Singleton

/** Qualificador para o [MutableStateFlow] que guarda o rótulo da fonte de dados. */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class DataSourceLabelFlow

/** Módulo Hilt que fornece as dependências singleton da camada de dados. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Provides
  @Singleton
  fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
      Room.databaseBuilder(context, AppDatabase::class.java, "sebrae_local.db")
          .fallbackToDestructiveMigration(true)
          .build()

  @Provides @Singleton fun provideAppDao(database: AppDatabase): AppDao = database.appDao()

  @Provides
  @Singleton
  @DataSourceLabelFlow
  fun provideDataSourceLabelFlow(): MutableStateFlow<Int> =
      MutableStateFlow(R.string.stat_data_source)

  @Provides
  @Singleton
  fun provideAppRepository(
      database: AppDatabase,
      dao: AppDao,
      @DataSourceLabelFlow labelFlow: MutableStateFlow<Int>,
  ): AppRepository =
      AppRepository(database = database, dao = dao, dataSourceLabelResFlow = labelFlow)

  @Provides
  @Singleton
  fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver =
      NetworkConnectivityObserver(context)

  @Provides
  @Singleton
  fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

  @Provides
  @Singleton
  fun provideFirebaseSeedCredentialStore(
      @ApplicationContext context: Context
  ): FirebaseSeedCredentialStore = FirebaseSeedCredentialStore(context)

  @Provides
  @Singleton
  fun provideFirebaseDataConnectService(
      firestore: FirebaseFirestore,
      dao: AppDao,
      credentialStore: FirebaseSeedCredentialStore,
      @ApplicationContext context: Context,
  ): FirebaseDataConnectService =
      FirebaseDataConnectService(
          firestore = firestore,
          dao = dao,
          credentialStore = credentialStore,
          context = context,
          seedEmail = BuildConfig.FIREBASE_SEED_EMAIL,
          seedPassword = BuildConfig.FIREBASE_SEED_PASSWORD,
      )
}
