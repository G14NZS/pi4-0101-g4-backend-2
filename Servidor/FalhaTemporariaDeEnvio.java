/**
 * O envio nao aconteceu, mas pode dar certo depois (credencial errada, 20003; servico fora do ar;
 * sem resposta em 10 segundos). Nada e registrado e o lembrete e tentado no ciclo seguinte.
 */
public class FalhaTemporariaDeEnvio extends Exception
{
    public FalhaTemporariaDeEnvio (String mensagem)
    {
        super (mensagem);
    }
}
