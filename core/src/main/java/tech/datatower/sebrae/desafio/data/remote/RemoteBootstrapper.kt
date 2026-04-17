/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/data/remote/RemoteBootstrapper.kt
    Descrição: Interface para inicialização e sincronização de dados remotos ao iniciar a aplicação.
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
package tech.datatower.sebrae.desafio.data.remote

/**
 * Contract for bootstrapping local cache from a remote backend.
 *
 * Returns `true` when remote data was fetched and written to local storage.
 */
interface RemoteBootstrapper {
  /**
   * Busca dados remotos e os persiste no cache local (Room).
   *
   * @return `true` quando os dados remotos foram obtidos e gravados com sucesso.
   */
  suspend fun bootstrapIntoLocalCache(): Boolean
}

/** Fallback bootstrapper used when no remote backend is configured. */
object NoOpRemoteBootstrapper : RemoteBootstrapper {
  /** Retorna `false` imediatamente — nenhum backend remoto está configurado. */
  override suspend fun bootstrapIntoLocalCache(): Boolean = false
}
