/** CT19: quem manda o SMS. Na producao e o TwilioSms; nos testes, um servico simulado. */
public interface ServicoDeMensagens
{
    /** Envia o SMS e devolve o sid da mensagem. */
    public String envie (String para, String texto)
    throws FalhaDefinitivaDeEnvio, FalhaTemporariaDeEnvio;
}
